package io.fireflysoftware.masters.core.services.insurance.source;

import java.time.*;
import java.util.*;
import java.util.function.LongSupplier;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.fireflyframework.security.spi.*;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.Type;
import reactor.core.publisher.Mono;

/** Actual shared caller/current PDP boundary, separate from durable fact content and DB roles. */
public final class InsuranceSandboxFactAuthority {
    public record Access(SecurityPrincipal actor,R2dbcOperationBudget budget,Instant until) {
        @Override public String toString(){return "InsuranceSandboxFactAccess";}
    }
    private final SecurityContextPort callers;
    private final PolicyDecisionPort policies;
    private final Set<String> issuers;
    private final Clock clock;
    private final LongSupplier nanos;
    public InsuranceSandboxFactAuthority(SecurityContextPort callers,PolicyDecisionPort policies,Set<String> issuers,Clock clock,LongSupplier nanos){this.callers=Objects.requireNonNull(callers);this.policies=Objects.requireNonNull(policies);this.issuers=Set.copyOf(issuers);if(this.issuers.isEmpty())throw denied();this.clock=Objects.requireNonNull(clock);this.nanos=Objects.requireNonNull(nanos);}
    public boolean usesTimeSources(Clock clock,LongSupplier nanos){return this.clock==clock&&this.nanos==nanos;}
    public Mono<Access> authorize(Type type,String operation,UUID id,Map<String,Object> facts){return Mono.defer(()->{
        long before=nanos.getAsLong();Instant start=clock.instant();
        return callers.currentPrincipal().switchIfEmpty(Mono.error(denied())).flatMap(actor->{
            Instant until=Collections.min(List.of(start.plus(InsuranceSandboxFactPersistence.LIMIT),trusted(actor)));
            var budget=R2dbcOperationBudget.observed(start,until,before,InsuranceSandboxFactPersistence.LIMIT,clock,nanos);
            return decision(actor,type,operation,id,facts).timeout(budget.remaining()).then(Mono.fromSupplier(()->{budget.remaining();return new Access(actor,budget,until);}));
        }).timeout(InsuranceSandboxFactPersistence.LIMIT);
    }).onErrorMap(error->denied());}
    public Mono<Void> current(Access original,Type type,String operation,UUID id,Map<String,Object> facts,R2dbcOperationBudget bounded){return Mono.defer(()->{
        bounded.remaining();return callers.currentPrincipal().switchIfEmpty(Mono.error(denied())).flatMap(actor->{
            trusted(actor);if(!actor.equals(original.actor()))return Mono.error(denied());
            return decision(actor,type,operation,id,facts).then(Mono.<Void>fromRunnable(bounded::remaining));
        }).timeout(bounded.remaining());
    });}
    private Mono<Void> decision(SecurityPrincipal actor,Type type,String operation,UUID id,Map<String,Object> facts){
        if(!Set.of("register","withdraw","read").contains(operation))return Mono.error(denied());
        var context=new HashMap<String,Object>(facts);context.put("scope","SANDBOX");context.put("factType",type.name());context.put("factId",id.toString());
        return policies.authorize(actor,"masters.insurance-reference.sandbox-source."+type.name().toLowerCase(Locale.ROOT)+"."+operation,
                "insurance-sandbox-source/"+type+"/"+id,Map.copyOf(context))
            .filter(value->value.granted()&&value.obligations().isEmpty()).switchIfEmpty(Mono.error(denied())).then();
    }
    private Instant trusted(SecurityPrincipal actor){
        if(actor==null||actor.issuer()==null||!issuers.contains(actor.issuer())||actor.subject()==null||actor.subject().isBlank()||actor.subject().length()>255||actor.tenantId()==null||actor.tenantId().isBlank())throw denied();
        Instant expiry=actor.claim("exp",Instant.class);if(expiry==null||!expiry.isAfter(clock.instant()))throw denied();return expiry;
    }
    private static InsuranceSandboxFactFailure denied(){return new InsuranceSandboxFactFailure(InsuranceSandboxFactFailure.Kind.FORBIDDEN);}
}
