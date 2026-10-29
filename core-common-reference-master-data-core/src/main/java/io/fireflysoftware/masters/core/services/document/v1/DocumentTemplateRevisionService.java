package io.fireflysoftware.masters.core.services.document.v1;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fireflysoftware.masters.interfaces.dtos.document.v1.DocumentTemplatePublicationRequest;
import io.fireflysoftware.masters.interfaces.dtos.document.v1.DocumentTemplateRevisionDTO;
import io.fireflysoftware.masters.models.entities.document.v1.DocumentTemplateRevision;
import io.fireflysoftware.masters.models.repositories.document.v1.DocumentTemplateRevisionRepository;
import org.fireflyframework.validators.schema.OfflineJsonSchemaValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Publication and retirement commands are deliberately separate from legacy mutable draft CRUD. */
@Service
public class DocumentTemplateRevisionService {
    private final DocumentTemplateRevisionRepository repository;
    private final ObjectMapper mapper;
    private final TransactionalOperator transactions;
    private final DocumentTemplateAccessGuard access;
    private final OfflineJsonSchemaValidator schemas = new OfflineJsonSchemaValidator();

    public DocumentTemplateRevisionService(DocumentTemplateRevisionRepository repository, ObjectMapper mapper,
            ReactiveTransactionManager transactions, DocumentTemplateAccessGuard access) {
        this.repository = repository;
        this.mapper = mapper.copy().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        this.transactions = TransactionalOperator.create(transactions);
        this.access = access;
    }

    public Mono<DocumentTemplateRevisionDTO> publish(UUID templateId, DocumentTemplatePublicationRequest request) {
        return access.authorize("publish", "document-template:" + templateId).flatMap(principal -> Mono.defer(() -> {
            if (templateId == null || request == null) return Mono.error(badRequest("Publication identity is required"));
            validateIdentity(request.version(), request.localeCode());
            return repository.lockSource(templateId, request.localeCode())
                    .switchIfEmpty(Mono.error(notFound("Exact localized draft was not found")))
                    .flatMap(source -> {
                        var snapshot = snapshot(source, request, principal.subject());
                        return repository.findExact(snapshot.templateCode(), snapshot.version(), snapshot.localeCode())
                                .flatMap(existing -> existing.sourceDigest().equals(snapshot.sourceDigest())
                                        ? Mono.just(existing)
                                        : Mono.error(conflict("Publication identity already has a different source snapshot")))
                                .switchIfEmpty(Mono.defer(() -> repository.insert(snapshot)));
                    }).as(transactions::transactional).map(this::toDTO);
        })).onErrorMap(IncorrectResultSizeDataAccessException.class, error -> conflict("Localized draft identity is ambiguous"))
                .onErrorMap(DataIntegrityViolationException.class, error -> conflict("Publication integrity constraint rejected the operation"));
    }

    public Mono<DocumentTemplateRevisionDTO> get(UUID revisionId) {
        return access.authorize("read", "document-template-revision:" + revisionId)
                .then(Mono.defer(() -> repository.findById(revisionId)))
                .switchIfEmpty(Mono.error(notFound("Document template revision was not found"))).map(this::toDTO);
    }

    public Mono<DocumentTemplateRevisionDTO> resolve(String templateCode, String version, String localeCode) {
        return access.authorize("resolve", "document-template:" + templateCode + ":" + version + ":" + localeCode)
                .then(Mono.defer(() -> {
                    validateIdentity(version, localeCode);
                    if (templateCode == null || templateCode.isBlank() || templateCode.length() > 100) {
                        return Mono.error(badRequest("Template code is required"));
                    }
                    return repository.findExact(templateCode, version, localeCode)
                            .switchIfEmpty(Mono.error(notFound("Exact document template revision was not found")))
                            .flatMap(revision -> repository.isUsable(revision.revisionId()).flatMap(usable -> usable
                                    ? Mono.just(revision) : Mono.error(conflict("Revision is retired or its exact source identity is inactive"))));
                })).map(this::toDTO);
    }

    public Mono<DocumentTemplateRevisionDTO> retire(UUID revisionId) {
        return access.authorize("retire", "document-template-revision:" + revisionId)
                .flatMap(principal -> repository.retire(revisionId, principal.subject()))
                .switchIfEmpty(Mono.error(notFound("Document template revision was not found"))).map(this::toDTO);
    }

    Mono<DocumentTemplateRevision> publishNewAuthorized(String sourceJson, DocumentTemplatePublicationRequest request, String actor) {
        return org.springframework.transaction.reactive.TransactionSynchronizationManager.forCurrentTransaction()
                .filter(org.springframework.transaction.reactive.TransactionSynchronizationManager::isActualTransactionActive)
                .switchIfEmpty(Mono.error(new IllegalStateException("New layout publication requires a local transaction")))
                .then(Mono.defer(() -> {
                    var revision = snapshot(sourceJson, request, actor);
                    return repository.findExact(revision.templateCode(), revision.version(), revision.localeCode())
                            .hasElement().flatMap(exists -> exists
                                    ? Mono.error(conflict("Publication identity already exists; a new layout cannot adopt it"))
                                    : repository.insert(revision));
                }));
    }

    DocumentTemplateRevision snapshot(String sourceJson, DocumentTemplatePublicationRequest request, String actor) {
        try {
            JsonNode source = mapper.readTree(sourceJson);
            for (String field : Set.of("catalogStatus", "localizationStatus", "localeStatus", "typeStatus")) {
                if (!"ACTIVE".equals(source.path(field).asText())) throw conflict("Publication requires active exact source records");
            }
            if (!request.version().equals(source.path("version").asText())) throw conflict("Requested version differs from the draft version");
            String content = source.path("templateContent").asText("");
            String name = source.path("templateName").asText("");
            String media = source.path("mediaType").asText("");
            String style = source.path("styleContent").asText("");
            if (content.isBlank() || name.isBlank() || content.getBytes(StandardCharsets.UTF_8).length > 262_144
                    || style.getBytes(StandardCharsets.UTF_8).length > 65_536) throw badRequest("Localized content, name or style is invalid or exceeds limits");
            if (!Set.of("text/html", "text/plain").contains(media)) throw badRequest("Supported publication media types are text/html and text/plain");
            JsonNode schema = source.path("inputSchema");
            String schemaJson = canonicalJson(schema);
            var validation = schemas.validateSchema(schemaJson);
            if (!validation.valid()) throw badRequest(validation.issues().getFirst().code() + ": Published input schema is invalid or unsupported");
            if (!"object".equals(schema.path("type").asText())) throw badRequest("Published input schema must declare type object");
            return new DocumentTemplateRevision(UUID.randomUUID(), UUID.fromString(source.path("templateId").asText()),
                    UUID.fromString(source.path("localizationId").asText()), UUID.fromString(source.path("localeId").asText()),
                    source.path("templateCode").asText(), request.version(), request.localeCode(), name, media, content,
                    schemaJson, style, canonicalJson(source.path("templateVariables")), canonicalJson(source),
                    digest(content), digest(schemaJson), digest(style), digest(canonicalJson(source)), Instant.now(), actor,
                    "PUBLISHED", null, null);
        } catch (JsonProcessingException error) {
            throw badRequest("Draft source JSON is invalid");
        } catch (IllegalArgumentException error) {
            throw badRequest("Draft source JSON exceeds supported canonical JSON limits");
        }
    }

    private String canonicalJson(JsonNode node) throws JsonProcessingException {
        return DocumentTemplateCanonicalJson.canonicalize(mapper.writeValueAsString(node));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(String json) {
        try { return mapper.readValue(json, Map.class); }
        catch (JsonProcessingException error) { throw new IllegalStateException("Published revision JSON cannot be read", error); }
    }

    private String canonicalStoredJson(String json) {
        try { return canonicalJson(mapper.readTree(json)); }
        catch (JsonProcessingException error) { throw new IllegalStateException("Published canonical JSON cannot be read", error); }
    }

    private DocumentTemplateRevisionDTO toDTO(DocumentTemplateRevision revision) {
        return new DocumentTemplateRevisionDTO(revision.revisionId(), revision.templateId(), revision.localizationId(), revision.localeId(),
                revision.templateCode(), revision.version(), revision.localeCode(), revision.templateName(), revision.mediaType(),
                revision.templateContent(), map(revision.inputSchema()), canonicalStoredJson(revision.inputSchema()),
                revision.styleContent(), map(revision.templateVariables()), map(revision.sourceSnapshot()),
                canonicalStoredJson(revision.sourceSnapshot()), "MASTERS_JSON_V1", revision.contentDigest(), revision.schemaDigest(), revision.styleDigest(), revision.sourceDigest(),
                revision.publishedAt(), revision.publishedBy(), revision.lifecycle(), revision.retiredAt(), revision.retiredBy());
    }

    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException("SHA-256 is unavailable", error); }
    }
    private static void validateIdentity(String version, String locale) {
        if (version == null || !version.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,19}")
                || locale == null || !locale.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,49}")) throw badRequest("Explicit version and locale are required");
    }
    private static ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
