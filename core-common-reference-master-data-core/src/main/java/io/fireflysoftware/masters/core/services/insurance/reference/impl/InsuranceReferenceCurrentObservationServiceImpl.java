package io.fireflysoftware.masters.core.services.insurance.reference.impl;

import java.time.*;
import java.util.*;
import java.util.function.LongSupplier;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.fireflyframework.security.spi.PolicyDecisionPort;
import org.fireflyframework.security.spi.SecurityContextPort;
import io.fireflysoftware.masters.core.services.currency.observation.CurrencyPolicyCurrentObservationService;
import io.fireflysoftware.masters.core.services.insurance.reference.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCurrentContracts.*;
import reactor.core.publisher.Mono;

/** Exact current observation. Provider mocks and historical receipts cannot activate this service. */
public final class InsuranceReferenceCurrentObservationServiceImpl implements InsuranceReferenceCurrentObservationService {
    private static final Duration LIMIT=Duration.ofSeconds(30);
    private final SecurityContextPort principals;
    private final PolicyDecisionPort policies;
    private final Set<String> issuers;
    private final InsuranceReferenceCurrentReader reader;
    private final InsuranceReferenceSourceVerifier sources;
    private final CurrencyPolicyCurrentObservationService currencies;
    private final Clock clock;
    private final LongSupplier nanos;
    private record Access(SecurityPrincipal actor,R2dbcOperationBudget budget,Instant until) {}
    private record Checked(InsuranceReferenceContracts.Revision definition,R2dbcOperationBudget budget,Instant until) {}

    public InsuranceReferenceCurrentObservationServiceImpl(SecurityContextPort principals,PolicyDecisionPort policies,
            Set<String> issuers,InsuranceReferenceCurrentReader reader,InsuranceReferenceSourceVerifier sources,
            CurrencyPolicyCurrentObservationService currencies,Clock clock,LongSupplier nanos) {
        this.principals=Objects.requireNonNull(principals);this.policies=Objects.requireNonNull(policies);this.issuers=Set.copyOf(issuers);
        if(this.issuers.isEmpty())throw new IllegalArgumentException("Current trusted issuers required");
        this.reader=Objects.requireNonNull(reader);this.sources=Objects.requireNonNull(sources);this.currencies=Objects.requireNonNull(currencies);
        this.clock=Objects.requireNonNull(clock);this.nanos=Objects.requireNonNull(nanos);
    }
    @Override public Mono<Observation> observe(Target target){return observeInternal(target,null,null);}
    @Override public Mono<Observation> observe(Target target,OriginalBudget original){
        Objects.requireNonNull(original);
        return observeInternal(target,original.budget(),original.until());
    }
    private Mono<Observation> observeInternal(Target target,R2dbcOperationBudget inherited,Instant inheritedUntil) {
        return Mono.defer(()->{
            Objects.requireNonNull(target);long before=nanos.getAsLong();Instant observed=clock.instant();
            return principals.currentPrincipal().switchIfEmpty(Mono.error(forbidden())).flatMap(actor->{
                Instant expiry=trusted(actor);Instant until=Collections.min(inheritedUntil==null
                    ?List.of(observed.plus(LIMIT),expiry):List.of(observed.plus(LIMIT),expiry,inheritedUntil));
                var local=R2dbcOperationBudget.observed(observed,until,before,LIMIT,clock,nanos);
                var budget=inherited==null?local:local.intersect(inherited);var access=new Access(actor,budget,until);
                return authorize(actor,target).timeout(budget.remaining()).then(Mono.defer(()->reader.read(target,actor,budget,until)))
                    .switchIfEmpty(Mono.error(unavailable())).flatMap(first->{
                        var definition=InsuranceReferenceCurrentValidator.checked(target,first);
                        var firstBudget=budget.intersect(first.budget());Instant firstUntil=minimum(until,first.absoluteUntil(),definition.effectiveUntil());
                        return current(access,target,firstBudget).then(verify(definition,firstBudget,firstUntil)).flatMap(checked->
                            current(access,target,checked.budget()).then(Mono.defer(()->reader.read(target,actor,checked.budget(),checked.until())))
                            .switchIfEmpty(Mono.error(unavailable())).flatMap(second->{
                                var finalDefinition=InsuranceReferenceCurrentValidator.checked(target,second);
                                if(!definition.equals(finalDefinition))throw notCurrent();
                                var finalBudget=checked.budget().intersect(second.budget());
                                Instant finalUntil=minimum(checked.until(),second.absoluteUntil(),definition.effectiveUntil());
                                return current(access,target,finalBudget).then(Mono.fromSupplier(()->{
                                    finalBudget.remaining();Instant now=clock.instant();
                                    Instant responseUntil=minimum(finalUntil,now.plus(finalBudget.remaining())).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
                                    finalBudget.remaining();if(!now.isBefore(responseUntil))throw unavailable();
                                    var value=new InsuranceReferenceContracts.Observation(definition,
                                        InsuranceReferenceCurrentValidator.control(target),second.stamp().recordedAt(),responseUntil);
                                    return new Observation(new Current(target,value),finalBudget);
                                }));
                            }));
                    }).timeout(budget.remaining());
            }).timeout(LIMIT);
        }).onErrorMap(error->error instanceof InsuranceReferenceCurrentFailure?error:unavailable());
    }
    private Mono<Checked> verify(InsuranceReferenceContracts.Revision definition,R2dbcOperationBudget inherited,Instant until) {
        return Mono.defer(()->{
            inherited.remaining();long before=nanos.getAsLong();Instant observed=clock.instant();
            return sources.verify(definition).switchIfEmpty(Mono.error(unavailable())).timeout(inherited.remaining()).flatMap(evidence->{
                InsuranceReferenceContracts.requireTimestamp(evidence.observedAt());InsuranceReferenceContracts.requireTimestamp(evidence.effectiveUntil());
                long millis=Math.subtractExact(Math.subtractExact(evidence.effectiveUntil().toEpochMilli(),evidence.observedAt().toEpochMilli()),1L);
                if(!InsuranceReferenceCodec.digest(definition).equals(evidence.definitionDigest()) || evidence.observedAt().isAfter(clock.instant()) || millis<=0 || millis>29999)throw unavailable();
                Instant sourceUntil=minimum(until,evidence.effectiveUntil(),observed.plusMillis(millis));
                var bounded=inherited.intersect(R2dbcOperationBudget.observed(observed,sourceUntil,before,LIMIT,clock,nanos));
                if(definition.definition() instanceof InsuranceReferenceContracts.PremiumTax tax) {
                    if(!tax.currencyPolicy().equals(evidence.currencyPolicy()) || !Objects.equals(tax.postedScale(),evidence.postedScale()))throw unavailable();
                    long currencyBefore=nanos.getAsLong();Instant currencyStart=clock.instant();
                    return currencies.observeCurrent(new io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.Target(tax.currencyPolicy(),tax.postedScale()),
                            new CurrencyPolicyCurrentObservationService.OriginalBudget(bounded,sourceUntil)).switchIfEmpty(Mono.error(unavailable()))
                        .timeout(bounded.remaining()).map(value->{
                            if(!tax.currencyPolicy().equals(value.value().publication().reference()) || !Objects.equals(tax.postedScale(),value.value().publication().postedScale()))throw unavailable();
                            var retained=bounded.intersect(value.budget());Instant currencyUntil=minimum(sourceUntil,currencyStart.plusSeconds(5),
                                value.absoluteUntil(),value.value().effectiveUntil(),value.value().publication().effectiveToEpochMillis()==null?null:Instant.ofEpochMilli(value.value().publication().effectiveToEpochMillis()));
                            retained=retained.intersect(R2dbcOperationBudget.observed(currencyStart,currencyUntil,currencyBefore,Duration.ofSeconds(5),clock,nanos));
                            return new Checked(definition,retained,currencyUntil);
                        });
                }
                if(evidence.currencyPolicy()!=null || evidence.postedScale()!=null)throw unavailable();
                bounded.remaining();return Mono.just(new Checked(definition,bounded,sourceUntil));
            });
        });
    }
    private Mono<Void> current(Access access,Target target,R2dbcOperationBudget budget) {
        return Mono.defer(()->{
            budget.remaining();return principals.currentPrincipal().switchIfEmpty(Mono.error(forbidden())).flatMap(actor->{
                trusted(actor);if(!actor.equals(access.actor()))return Mono.<Void>error(forbidden());
                return authorize(actor,target).then(Mono.<Void>fromRunnable(budget::remaining));
            }).timeout(budget.remaining());
        });
    }
    private Mono<Void> authorize(SecurityPrincipal actor,Target target) {
        return policies.authorize(actor,"masters.insurance-reference.current","insurance-reference/"+target.kind()+"/"+target.recordId(),
            Map.of("scope",InsuranceReferenceContracts.SCOPE,"recordId",target.recordId().toString(),"kind",target.kind().name(),
                "publicationId",target.publicationId().toString(),"revision",target.revision(),"definitionDigest",target.definitionDigest(),"generation",target.generation()))
            .filter(decision->decision.granted()&&decision.obligations().isEmpty()).switchIfEmpty(Mono.error(forbidden())).then();
    }
    private Instant trusted(SecurityPrincipal actor) {
        if(actor==null || actor.issuer()==null || !issuers.contains(actor.issuer()) || actor.subject()==null || actor.subject().isBlank()
                || actor.tenantId()==null || actor.tenantId().isBlank())throw forbidden();
        Instant expiry=actor.claim("exp",Instant.class);if(expiry==null || !expiry.isAfter(clock.instant()))throw forbidden();return expiry;
    }
    private static Instant minimum(Instant...values){return Arrays.stream(values).filter(Objects::nonNull).min(Instant::compareTo).orElseThrow();}
    private static InsuranceReferenceCurrentFailure forbidden(){return new InsuranceReferenceCurrentFailure(InsuranceReferenceCurrentFailure.Kind.FORBIDDEN);}
    private static InsuranceReferenceCurrentFailure notCurrent(){return new InsuranceReferenceCurrentFailure(InsuranceReferenceCurrentFailure.Kind.NOT_CURRENT);}
    private static InsuranceReferenceCurrentFailure unavailable(){return new InsuranceReferenceCurrentFailure(InsuranceReferenceCurrentFailure.Kind.UNAVAILABLE);}
}
