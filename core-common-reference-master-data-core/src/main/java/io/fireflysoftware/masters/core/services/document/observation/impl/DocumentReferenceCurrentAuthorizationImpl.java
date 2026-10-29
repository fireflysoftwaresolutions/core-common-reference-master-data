/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation.impl;
import java.time.*;
import java.util.*;
import java.net.URI;
import io.fireflysoftware.masters.core.services.document.observation.*;
import io.fireflysoftware.masters.interfaces.dtos.document.observation.DocumentReferenceCurrentContracts.Target;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.fireflyframework.security.spi.*;
import reactor.core.publisher.Mono;

/** Explicit current permission binds the complete exact caller intent, never caller actor headers. */
public final class DocumentReferenceCurrentAuthorizationImpl implements DocumentReferenceCurrentAuthorization {
    private final SecurityContextPort contexts;private final PolicyDecisionPort policies;private final Set<String> issuers;private final Clock clock;
    public DocumentReferenceCurrentAuthorizationImpl(SecurityContextPort contexts,PolicyDecisionPort policies,Set<String> issuers,Clock clock){this.contexts=Objects.requireNonNull(contexts);this.policies=Objects.requireNonNull(policies);this.issuers=Set.copyOf(issuers);this.clock=Objects.requireNonNull(clock);}
    @Override public Mono<SecurityPrincipal> authorize(Target target){return Mono.defer(()->contexts.currentPrincipal().flatMap(actor->{check(actor,target);var p=target.product();var safe=Map.<String,Object>of("scope","GLOBAL_MASTER","tenantId",p.tenantId().toString(),"rootId",p.rootId().toString(),"productId",p.productId().toString(),"revisionId",p.revisionId().toString(),"publicationId",p.publicationId().toString(),"definitionDigest",p.definitionDigest(),"purpose",target.purpose(),"documentType",target.documentType(),"target",Map.of("locale",target.locale(),"template",Map.of("id",target.template().id().toString(),"version",target.template().versionText(),"digest",target.template().digest()),"layout",Map.of("id",target.layout().id().toString(),"version",target.layout().versionText(),"digest",target.layout().digest())));
        return policies.authorize(actor,"masters.document-template.current","document-layout-revision:"+target.layout().id(),safe).filter(d->d.granted()&&d.obligations().isEmpty()).map(d->{check(actor,target);return actor;});
    }).switchIfEmpty(Mono.error(forbidden()))).onErrorMap(e->forbidden());}
    private void check(SecurityPrincipal p,Target target){if(p==null||!issuers.contains(p.issuer())||p.subject()==null||p.subject().isBlank()||p.subject().length()>255||!target.product().tenantId().toString().equals(p.tenantId())||!(p.claims().get("exp")instanceof Instant expiry)||!expiry.isAfter(clock.instant()))throw forbidden();URI uri=URI.create(p.issuer());if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null)throw forbidden();}
    private static DocumentReferenceCurrentFailure forbidden(){return new DocumentReferenceCurrentFailure(DocumentReferenceCurrentFailure.Kind.FORBIDDEN);}
}
