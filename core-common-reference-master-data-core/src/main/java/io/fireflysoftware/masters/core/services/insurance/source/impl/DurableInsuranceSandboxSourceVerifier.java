package io.fireflysoftware.masters.core.services.insurance.source.impl;

import java.time.*;
import java.util.*;
import java.util.function.LongSupplier;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import io.fireflysoftware.masters.core.services.insurance.source.*;
import io.fireflysoftware.masters.core.services.insurance.reference.*;
import io.fireflysoftware.masters.core.services.currency.observation.CurrencyPolicyCurrentObservationService;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.*;
import reactor.core.publisher.Mono;

/** Durable sandbox adapter. A sandbox label/reference constructor never establishes eligibility. */
public final class DurableInsuranceSandboxSourceVerifier implements InsuranceReferenceSourceVerifier {
    private final InsuranceSandboxFactReader source;
    private final CurrencyPolicyCurrentObservationService currency;
    private final Clock clock;
    private final LongSupplier nanos;
    public DurableInsuranceSandboxSourceVerifier(InsuranceSandboxFactReader source,CurrencyPolicyCurrentObservationService currency,Clock clock,LongSupplier nanos){this.source=Objects.requireNonNull(source);this.currency=Objects.requireNonNull(currency);this.clock=Objects.requireNonNull(clock);this.nanos=Objects.requireNonNull(nanos);}
    private record CurrencyChecked(R2dbcOperationBudget budget,Instant until) {}
    @Override public Mono<Evidence> verify(Revision expected){return Mono.defer(()->source.observe(expected).switchIfEmpty(Mono.error(InsuranceSandboxSourceRules.unavailable())).flatMap(first->{
        first.budget().remaining();String digest=InsuranceReferenceCodec.digest(expected);if(!digest.equals(first.definitionDigest()))throw InsuranceSandboxSourceRules.unavailable();
        Mono<CurrencyChecked> checked;Instant currencyUntil;
        if(expected.definition() instanceof PremiumTax tax){long before=nanos.getAsLong();Instant observed=clock.instant();currencyUntil=minimum(first.effectiveUntil(),observed.plusSeconds(5));
            checked=currency.observe(tax.currencyPolicy(),tax.postedScale(),first.budget()).switchIfEmpty(Mono.error(InsuranceSandboxSourceRules.unavailable())).timeout(first.budget().remaining()).map(value->{
                if(!tax.currencyPolicy().equals(value.policy().reference())||!Objects.equals(tax.postedScale(),value.policy().postedScale()))throw InsuranceSandboxSourceRules.unavailable();
                Instant until=minimum(currencyUntil,value.policy().effectiveToEpochMillis()==null?null:Instant.ofEpochMilli(value.policy().effectiveToEpochMillis()));
                return new CurrencyChecked(first.budget().intersect(value.budget()).intersect(R2dbcOperationBudget.observed(observed,until,before,Duration.ofSeconds(5),clock,nanos)),until);
            });
        }else{currencyUntil=first.effectiveUntil();checked=Mono.just(new CurrencyChecked(first.budget(),currencyUntil));}
        return checked.flatMap(window->source.observe(expected).switchIfEmpty(Mono.error(InsuranceSandboxSourceRules.unavailable())).timeout(window.budget().remaining()).map(second->{
            if(!first.actor().equals(second.actor())||!first.source().equals(second.source())||!first.rights().equals(second.rights())||!Objects.equals(first.locale(),second.locale())||!digest.equals(second.definitionDigest()))throw InsuranceSandboxSourceRules.unavailable();
            var budget=window.budget().intersect(second.budget());Instant until=minimum(first.effectiveUntil(),second.effectiveUntil(),window.until(),second.observedAt().plus(budget.remaining())).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            budget.remaining();
            if(!second.observedAt().isBefore(until))throw InsuranceSandboxSourceRules.unavailable();
            var tax=expected.definition() instanceof PremiumTax value?value:null;
            return new Evidence(digest,tax==null?null:tax.currencyPolicy(),tax==null?null:tax.postedScale(),second.observedAt(),until);
        }));
    })).onErrorMap(error->InsuranceSandboxSourceRules.unavailable());}
    private static Instant minimum(Instant...values){return Arrays.stream(values).filter(Objects::nonNull).min(Instant::compareTo).orElseThrow();}
}
