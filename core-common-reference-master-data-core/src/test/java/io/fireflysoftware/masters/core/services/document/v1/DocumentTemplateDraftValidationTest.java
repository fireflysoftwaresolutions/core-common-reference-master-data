package io.fireflysoftware.masters.core.services.document.v1;

import io.fireflysoftware.masters.core.mappers.document.v1.DocumentTemplateCatalogMapper;
import io.fireflysoftware.masters.interfaces.dtos.document.v1.DocumentTemplateCatalogDTO;
import io.fireflysoftware.masters.models.repositories.document.v1.DocumentTemplateCatalogRepository;
import org.fireflyframework.security.api.domain.Decision;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.fireflyframework.security.spi.PolicyDecisionPort;
import org.fireflyframework.security.spi.SecurityContextPort;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DocumentTemplateDraftValidationTest {
    @ParameterizedTest
    @ValueSource(strings = {"1e-129", "1e128"})
    void excessiveCanonicalNumbersAreClientErrorsBeforeAnyDraftWrite(String number) {
        var beans = new StaticListableBeanFactory();
        beans.addBean("syntheticContext", (SecurityContextPort) () -> Mono.just(SecurityPrincipal.builder().subject("synthetic-editor").build()));
        beans.addBean("syntheticPolicy", (PolicyDecisionPort) (p,a,r,c) -> Mono.just(Decision.permit()));
        var repository = mock(DocumentTemplateCatalogRepository.class);
        var mapper = mock(DocumentTemplateCatalogMapper.class);
        var service = new DocumentTemplateCatalogServiceImpl();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "access", new DocumentTemplateAccessGuard(
                beans.getBeanProvider(SecurityContextPort.class), beans.getBeanProvider(PolicyDecisionPort.class)));
        var draft = DocumentTemplateCatalogDTO.builder().inputSchemaJson("""
                {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","properties":{"x":{"minimum":%s}}}
                """.formatted(number)).build();
        for (Mono<?> operation : java.util.List.of(service.createDocumentTemplate(draft),
                service.updateDocumentTemplate(UUID.randomUUID(), draft))) {
            StepVerifier.create(operation).expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(ResponseStatusException.class);
                assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            }).verify();
        }
        verifyNoInteractions(repository, mapper);
    }
}
