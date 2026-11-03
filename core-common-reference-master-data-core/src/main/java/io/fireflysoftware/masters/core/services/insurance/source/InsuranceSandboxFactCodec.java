package io.fireflysoftware.masters.core.services.insurance.source;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.*;
import io.fireflysoftware.masters.core.services.document.v1.DocumentTemplateCanonicalJson;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.*;
import io.fireflysoftware.masters.models.entities.insurance.source.InsuranceSandboxFactRows.*;

/** Closed versioned fact canonicalization; content hashes are not permissions. */
public final class InsuranceSandboxFactCodec {
    private static final JsonMapper JSON=JsonMapper.builder().findAndAddModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
    private static final String ENCODING="MASTERS_INSURANCE_SANDBOX_FACT_V1";
    private InsuranceSandboxFactCodec() {}
    public static String command(String operation,Object command){return digest(canonical(Map.of("operation",operation,"command",command)));}
    public static String canonical(Object value){try{return DocumentTemplateCanonicalJson.canonicalize(JSON.writeValueAsString(value));}catch(Exception error){throw InsuranceSandboxSourceRules.unavailable();}}
    public static String fact(Type type,UUID id,long version,long generation,Object payload,Instant until){return canonical(Map.of("encodingVersion",ENCODING,"scope","SANDBOX","type",type,"factId",id,"version",version,"generation",generation,"payload",payload,"effectiveUntil",until));}
    public static String digest(String canonical){return InsuranceSandboxSourceRules.sha("masters-insurance-sandbox-fact-v1\n"+canonical);}
    public static String localeEncoding(LocaleSource source){
        var data=new LinkedHashMap<String,Object>();data.put("localeId",source.localeId());data.put("languageCode",source.languageCode());data.put("countryCode",source.countryCode());data.put("localeCode",source.localeCode());data.put("languageName",source.languageName());data.put("nativeName",source.nativeName());data.put("regionName",source.regionName());data.put("rtl",source.rtl());data.put("sortOrder",source.sortOrder());data.put("status",source.status());data.put("dateCreated",source.dateCreated());data.put("dateUpdated",source.dateUpdated());data.put("sourceEpoch",source.sourceEpoch());return canonical(data);
    }
    public static Map<String,Object> localePayload(UUID locale,String code,long epoch,String encoding,String sourceDigest){return Map.of("localeId",locale,"localeCode",code,"sourceEpoch",epoch,"sourceEncoding",encoding,"sourceDigest",sourceDigest);}
    public static Map<String,Object> rightsPayload(UUID sourceId,String kind,String sourceDigest,Reference locale){var value=new LinkedHashMap<String,Object>();value.put("sourceId",sourceId);value.put("insuranceKind",kind);value.put("sourceDigest",sourceDigest);value.put("locale",locale);return value;}
    public static Map<String,Object> sourcePayload(String canonicalDefinition,String definitionDigest,String sourceDigest,Reference rights,Reference locale){var value=new LinkedHashMap<String,Object>();value.put("canonicalDefinition",canonicalDefinition);value.put("definitionDigest",definitionDigest);value.put("sourceDigest",sourceDigest);value.put("rights",rights);value.put("locale",locale);return value;}
    public static Reference reference(LocaleVersion row){return new Reference(Type.LOCALE,row.factId(),row.version(),row.admittedGeneration(),row.digest());}
    public static Reference reference(RightsVersion row){return new Reference(Type.RIGHTS,row.factId(),row.version(),row.admittedGeneration(),row.digest());}
    public static Reference reference(SourceVersion row){return new Reference(Type.SOURCE,row.factId(),row.version(),row.admittedGeneration(),row.digest());}
    public static Reference locale(UUID id,Long version,Long generation,String digest){
        boolean absent=id==null&&version==null&&generation==null&&digest==null;
        if(absent)return null;if(id==null||version==null||generation==null||digest==null)throw InsuranceSandboxSourceRules.unavailable();
        return new Reference(Type.LOCALE,id,version,generation,digest);
    }
    public static void validate(LocaleVersion row){String expected=fact(Type.LOCALE,row.factId(),row.version(),row.admittedGeneration(),localePayload(row.localeId(),row.localeCode(),row.sourceEpoch(),row.sourceEncoding(),row.sourceDigest()),row.effectiveUntil());check(row.catalogScope(),expected,row.canonicalFact(),row.digest());if(!InsuranceSandboxSourceRules.sha(row.sourceEncoding()).equals(row.sourceDigest()))throw InsuranceSandboxSourceRules.unavailable();}
    public static void validate(RightsVersion row){String expected=fact(Type.RIGHTS,row.factId(),row.version(),row.admittedGeneration(),rightsPayload(row.sourceId(),row.insuranceKind(),row.sourceDigest(),locale(row.localeFactId(),row.localeVersion(),row.localeGeneration(),row.localeDigest())),row.effectiveUntil());check(row.catalogScope(),expected,row.canonicalFact(),row.digest());if(!InsuranceSandboxSourceRules.sha(row.sourceText()).equals(row.sourceDigest()))throw InsuranceSandboxSourceRules.unavailable();}
    public static void validate(SourceVersion row){String expected=fact(Type.SOURCE,row.factId(),row.version(),row.admittedGeneration(),sourcePayload(row.canonicalDefinition(),row.definitionDigest(),row.sourceDigest(),new Reference(Type.RIGHTS,row.rightsFactId(),row.rightsVersion(),row.rightsGeneration(),row.rightsDigest()),locale(row.localeFactId(),row.localeVersion(),row.localeGeneration(),row.localeDigest())),row.effectiveUntil());check(row.catalogScope(),expected,row.canonicalFact(),row.digest());if(!InsuranceSandboxSourceRules.sha(row.sourceText()).equals(row.sourceDigest()))throw InsuranceSandboxSourceRules.unavailable();}
    private static void check(String scope,String expected,String actual,String digest){if(!"SANDBOX".equals(scope)||!expected.equals(actual)||!digest(actual).equals(digest))throw InsuranceSandboxSourceRules.unavailable();}
}
