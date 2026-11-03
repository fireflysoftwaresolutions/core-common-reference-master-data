package io.fireflysoftware.masters.core.services.insurance.source;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import io.fireflysoftware.masters.core.services.document.v1.DocumentTemplateCanonicalJson;
import io.fireflysoftware.masters.core.services.insurance.reference.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.*;

/** Exact synthetic fact bindings; digest equality is integrity, never current authority. */
public final class InsuranceSandboxSourceRules {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private InsuranceSandboxSourceRules() {}
    public static InsuranceReferenceContracts.Revision submitted(RegisterSource command,InsuranceReferenceCommands.Receipt retained) {
        var draft=command.draft();var input=command.submitted();
        if(retained==null||!"SUBMIT_PUBLICATION".equals(retained.operation())||!input.actionId().equals(retained.actionId())
                ||!input.intentDigest().equals(retained.intentDigest())||!input.publicationId().equals(retained.publicationId())
                ||input.revision()!=retained.revision()||!draft.recordId().equals(retained.recordId())||draft.kind()!=retained.kind())throw unavailable();
        long next=draft.expected().publicationId()==null?1:Math.addExact(draft.expected().revision(),1);
        if(next!=input.revision())throw unavailable();
        var value=new InsuranceReferenceContracts.Revision(draft.kind(),draft.recordId(),input.publicationId(),input.revision(),
                draft.code(),draft.effectiveFrom(),draft.effectiveUntil(),draft.definition(),draft.provenance());
        var intent=new InsuranceReferenceIntentCodec.Intent("PUBLISH",value,draft.expected());
        if(!InsuranceReferenceIntentCodec.digest(InsuranceReferenceIntentCodec.encode(intent)).equals(input.intentDigest()))throw unavailable();
        sourceBinding(command.factId(),Math.addExact(command.expectedVersion(),1),value,command.sourceText(),command.rights());
        return value;
    }
    public static void sourceBinding(UUID sourceId,long version,InsuranceReferenceContracts.Revision value,String sourceText,Reference rights) {
        var provenance=value.provenance();
        if(provenance.origin()!=InsuranceReferenceContracts.Origin.SANDBOX
                ||!InsuranceSandboxFacts.sourceUri(sourceId).equals(provenance.sourceReference())
                ||!("v"+version).equals(provenance.versionText())||!sha(sourceText).equals(provenance.sourceDigest())
                ||!InsuranceSandboxFacts.rightsUri(rights).equals(provenance.rightsReference())
                ||!sourceText(value).equals(sourceText))throw unavailable();
    }
    public static String sourceText(InsuranceReferenceContracts.Revision value) {
        if(value.definition() instanceof InsuranceReferenceContracts.Wording wording)return wording.content();
        try{return DocumentTemplateCanonicalJson.canonicalize(JSON.readTree(InsuranceReferenceCodec.encode(value)).get("definition").toString());}
        catch(Exception failure){throw unavailable();}
    }
    public static void requireWindow(Instant nativeNow,Instant until,Instant...dependencies) {
        InsuranceReferenceContracts.requireTimestamp(nativeNow);InsuranceReferenceContracts.requireTimestamp(until);
        if(!until.isAfter(nativeNow)||Duration.between(nativeNow,until).compareTo(Duration.ofDays(1))>0)throw unavailable();
        for(Instant bound:dependencies)if(bound!=null&&until.isAfter(bound))throw unavailable();
    }
    public static String sha(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception error){throw unavailable();}
    }
    public static IllegalStateException unavailable(){return new IllegalStateException("INSURANCE_SANDBOX_REGISTERED_FACT_UNAVAILABLE");}
}
