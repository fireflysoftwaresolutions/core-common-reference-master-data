package io.fireflysoftware.masters.core.services.insurance.reference;

import java.time.Instant;
import java.util.Objects;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCurrentContracts.Target;

/** Immutable catalog decoding is independent of expired historical command/approval horizons. */
public final class InsuranceReferenceCurrentValidator {
    private InsuranceReferenceCurrentValidator() {}
    public static InsuranceReferenceContracts.Revision checked(Target target,InsuranceReferenceCurrentReader.Snapshot snapshot) {
        Objects.requireNonNull(target);Objects.requireNonNull(snapshot);snapshot.budget().remaining();
        var identity=snapshot.identity();var row=snapshot.revision();var control=snapshot.control();var stamp=snapshot.stamp();
        var value=InsuranceReferenceCodec.decode(row.canonicalDefinition());
        if (!target.equals(stamp.target()) || !target.recordId().equals(identity.recordId()) || target.kind()!=identity.kind()
                || !InsuranceReferenceContracts.SCOPE.equals(identity.catalogScope())
                || !value.code().equals(identity.code()) || !target.recordId().equals(row.recordId()) || target.kind()!=row.kind()
                || !InsuranceReferenceContracts.SCOPE.equals(row.catalogScope()) || !InsuranceReferenceContracts.SCOPE.equals(control.catalogScope())
                || !target.publicationId().equals(row.publicationId()) || target.revision()!=row.revision()
                || !target.definitionDigest().equals(row.definitionDigest()) || !target.definitionDigest().equals(InsuranceReferenceCodec.digest(value))
                || !target.recordId().equals(value.recordId()) || target.kind()!=value.kind()
                || !target.publicationId().equals(value.publicationId()) || target.revision()!=value.revision()
                || !Objects.equals(row.effectiveFrom(),value.effectiveFrom()) || !Objects.equals(row.effectiveUntil(),value.effectiveUntil())
                || row.recordedAt()==null || row.recordedAt().isAfter(stamp.recordedAt())
                || !target.recordId().equals(control.recordId()) || target.kind()!=control.kind()
                || !target.publicationId().equals(control.publicationId()) || target.revision()!=control.revision()
                || target.generation()!=control.generation() || !"ACTIVE".equals(control.lifecycle())
                || stamp.recordedAt().isBefore(value.effectiveFrom())
                || value.effectiveUntil()!=null && !stamp.recordedAt().isBefore(value.effectiveUntil())) throw unavailable();
        return value;
    }
    public static InsuranceReferenceContracts.Control control(Target target) {
        return new InsuranceReferenceContracts.Control(target.recordId(),target.kind(),target.publicationId(),target.revision(),target.generation(),InsuranceReferenceContracts.State.ACTIVE);
    }
    private static InsuranceReferenceCurrentFailure unavailable(){return new InsuranceReferenceCurrentFailure(InsuranceReferenceCurrentFailure.Kind.NOT_CURRENT);}
}
