package io.fireflysoftware.masters.core.services.currency.observation.impl;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.LongSupplier;
import io.fireflysoftware.masters.core.services.currency.observation.*;
import io.fireflysoftware.masters.core.services.currency.policy.CurrencyPolicyAuthority;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.*;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** Exact finite native observation; the original caller budget is never recreated downstream. */
public final class NativeCurrencyPolicyCurrentObservationServiceImpl implements CurrencyPolicyCurrentObservationService {
    private static final Duration LIMIT=Duration.ofSeconds(5);
    private final CurrencyPolicyCurrentReader reader;
    private final CurrencyPolicyAuthority authority;
    private final Clock clock;
    private final LongSupplier nanos;
    public NativeCurrencyPolicyCurrentObservationServiceImpl(CurrencyPolicyCurrentReader reader,CurrencyPolicyAuthority authority,Clock clock,LongSupplier nanos){
        this.reader=Objects.requireNonNull(reader);this.authority=Objects.requireNonNull(authority);this.clock=Objects.requireNonNull(clock);this.nanos=Objects.requireNonNull(nanos);
    }
    @Override public Mono<Observation> observe(CurrencyPolicyReference expected,int scale,R2dbcOperationBudget budget){
        return Mono.error(failure(CurrencyPolicyCurrentFailure.Kind.UNAVAILABLE));
    }
    @Override public Mono<CurrentObservation> observeCurrent(Target target){
        return Mono.defer(()->{long before=nanos.getAsLong();Instant start=clock.instant();Instant until=start.plus(LIMIT);
            return observeCurrent(target,new OriginalBudget(R2dbcOperationBudget.observed(start,until,before,LIMIT,clock,nanos),until));});
    }
    @Override public Mono<CurrentObservation> observeCurrent(Target target,OriginalBudget original){
        return Mono.defer(()->{
            Objects.requireNonNull(target);Objects.requireNonNull(original);original.budget().remaining();
            long before=nanos.getAsLong();Instant start=clock.instant();Instant until=minimum(original.until(),start.plus(LIMIT));
            var initial=original.budget().intersect(R2dbcOperationBudget.observed(start,until,before,LIMIT,clock,nanos));
            var reference=target.reference();var context=Map.<String,Object>of("policyId",reference.policyId(),"version",reference.version(),
                    "definitionDigest",reference.digest(),"encodingVersion",reference.encodingVersion(),"isoCode",reference.isoCode(),"postedScale",target.postedScale(),"stateVersion",0L);
            return authority.authorize("current",reference.currencyId(),context).switchIfEmpty(Mono.error(failure(CurrencyPolicyCurrentFailure.Kind.FORBIDDEN)))
                .timeout(initial.remaining()).flatMap(actor->{
                    Instant expiry=trusted(actor);Instant cap=minimum(until,expiry);
                    var authenticated=initial.intersect(R2dbcOperationBudget.observed(start,cap,before,LIMIT,clock,nanos));
                    return reader.read(target,actor,new OriginalBudget(authenticated,cap)).switchIfEmpty(Mono.error(failure(CurrencyPolicyCurrentFailure.Kind.NOT_CURRENT)))
                        .timeout(authenticated.remaining()).flatMap(snapshot->{
                            if(snapshot.stamp()==null||!target.equals(snapshot.stamp().target()))throw failure(CurrencyPolicyCurrentFailure.Kind.UNAVAILABLE);
                            CurrencyPolicyCurrentValidator.checked(reference,target.postedScale(),snapshot.publication(),clock.instant());
                            var retained=authenticated.intersect(snapshot.budget());
                            Instant absolute=minimum(cap,snapshot.absoluteUntil(),snapshot.stamp().effectiveUntil(),
                                    snapshot.publication().effectiveToEpochMillis()==null?null:Instant.ofEpochMilli(snapshot.publication().effectiveToEpochMillis()));
                            return authority.authorize("current",reference.currencyId(),context).switchIfEmpty(Mono.error(failure(CurrencyPolicyCurrentFailure.Kind.FORBIDDEN)))
                                .timeout(retained.remaining()).map(current->{
                                    trusted(current);if(!actor.equals(current))throw failure(CurrencyPolicyCurrentFailure.Kind.FORBIDDEN);
                                    Instant now=clock.instant();Instant delivered=minimum(absolute,now.plus(retained.remaining())).truncatedTo(ChronoUnit.MICROS);
                                    retained.remaining();if(!now.isBefore(delivered)||!snapshot.stamp().recordedAt().isBefore(delivered))throw failure(CurrencyPolicyCurrentFailure.Kind.UNAVAILABLE);
                                    return new CurrentObservation(new Current(target,snapshot.publication(),snapshot.stamp().recordedAt(),delivered),retained,absolute);
                                });
                        });
                }).timeout(initial.remaining());
        }).onErrorMap(error->error instanceof CurrencyPolicyCurrentFailure?error:
                error instanceof ResponseStatusException status&&status.getStatusCode().value()==403?failure(CurrencyPolicyCurrentFailure.Kind.FORBIDDEN):failure(CurrencyPolicyCurrentFailure.Kind.UNAVAILABLE));
    }
    private Instant trusted(SecurityPrincipal actor){
        if(actor==null||actor.issuer()==null||actor.subject()==null||actor.subject().isBlank()||actor.tenantId()==null||actor.tenantId().isBlank()
                ||!(actor.claims().get("exp") instanceof Instant expiry)||!expiry.isAfter(clock.instant()))throw failure(CurrencyPolicyCurrentFailure.Kind.FORBIDDEN);
        return expiry;
    }
    private static Instant minimum(Instant...values){return Arrays.stream(values).filter(Objects::nonNull).min(Instant::compareTo).orElseThrow();}
    private static CurrencyPolicyCurrentFailure failure(CurrencyPolicyCurrentFailure.Kind kind){return new CurrencyPolicyCurrentFailure(kind);}
}
