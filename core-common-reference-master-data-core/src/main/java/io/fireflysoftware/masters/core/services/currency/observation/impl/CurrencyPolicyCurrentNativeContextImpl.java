package io.fireflysoftware.masters.core.services.currency.observation.impl;

import java.time.*;
import java.util.*;
import io.fireflysoftware.masters.core.services.currency.observation.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.Target;
import io.fireflysoftware.masters.models.repositories.currency.policy.CurrencyPolicyCurrentContextRepository;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

public final class CurrencyPolicyCurrentNativeContextImpl implements CurrencyPolicyCurrentNativeContext {
    private final DatabaseClient client;
    private final CurrencyPolicyCurrentContextRepository repository;
    public CurrencyPolicyCurrentNativeContextImpl(R2dbcEntityTemplate operations){
        client=Objects.requireNonNull(operations).getDatabaseClient();repository=new R2dbcRepositoryFactory(operations).getRepository(CurrencyPolicyCurrentContextRepository.class);
    }
    @Override public DatabaseClient databaseClient(){return client;}
    @Override public Mono<Stamp> begin(Request request){return Mono.defer(()->{
        validate(request);var target=request.target();var ref=target.reference();
        return repository.runtimeAllowed().filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(unavailable()))
            .then(repository.begin(request.nonce(),UUID.fromString(ref.policyId()),UUID.fromString(ref.currencyId()),ref.isoCode(),ref.version(),ref.digest(),ref.encodingVersion(),target.postedScale(),
                    request.issuer(),request.subject(),request.tenant(),OffsetDateTime.ofInstant(request.authorityUntil(),ZoneOffset.UTC)))
            .switchIfEmpty(Mono.error(new CurrencyPolicyCurrentFailure(CurrencyPolicyCurrentFailure.Kind.NOT_CURRENT))).map(value->{
                var observed=new Target(new CurrencyPolicyReference(value.policyId().toString(),value.currencyId().toString(),value.isoCode(),value.version(),value.definitionDigest(),value.encodingVersion()),value.postedScale());
                if(!request.nonce().equals(value.contextNonce())||!target.equals(observed)||value.dbNowMillis()==null||value.recordedAt()==null||value.effectiveUntil()==null)throw unavailable();
                Instant recorded=value.recordedAt().toInstant(),until=value.effectiveUntil().toInstant();
                if(recorded.toEpochMilli()!=value.dbNowMillis()||!until.isAfter(recorded)||until.isAfter(request.authorityUntil())||Duration.between(recorded,until).compareTo(Duration.ofSeconds(5))>0)throw unavailable();
                return new Stamp(value.contextNonce(),observed,value.dbNowMillis(),recorded,until);
            });
    }).onErrorMap(error->error instanceof CurrencyPolicyCurrentFailure?error:unavailable());}
    @Override public Mono<Boolean> complete(Request request){return Mono.defer(()->{
        validate(request);return repository.complete(request.nonce()).filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(unavailable()));
    }).onErrorMap(error->unavailable());}
    private static void validate(Request value){
        if(value==null||value.nonce()==null||value.nonce().equals(new UUID(0,0))||value.target()==null||value.authorityUntil()==null)throw unavailable();
        for(String text:new String[]{value.issuer(),value.subject(),value.tenant()})if(text==null||text.isBlank()||text.length()>500||text.indexOf('\0')>=0)throw unavailable();
        if(value.subject().length()>255||value.tenant().length()>255)throw unavailable();
    }
    private static CurrencyPolicyCurrentFailure unavailable(){return new CurrencyPolicyCurrentFailure(CurrencyPolicyCurrentFailure.Kind.UNAVAILABLE);}
}
