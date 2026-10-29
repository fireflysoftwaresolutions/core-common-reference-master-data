/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.Test;

/** Source closure regression only; actual PostgreSQL resolution still needs its reviewed native gate. */
class DocumentReferenceHelperClosureTest {
    static final String PRIVATE="masters_document_current_private";
    static final Set<String> NAMES=Set.of("document_layout_utf16_key","document_layout_quote","document_layout_canonical","document_layout_shape","document_layout_text","document_layout_uuid","document_layout_sha","document_layout_master_reference_valid","document_layout_profile_valid","document_layout_notice_schema_valid","document_layout_definition_valid","document_layout_reference_matches");
    record Helper(String name,String arguments,String returns,String language,boolean strict,String body){}
    String resource(String name)throws Exception{try(var stream=getClass().getResourceAsStream(name)){assertNotNull(stream);return new String(stream.readAllBytes(),StandardCharsets.UTF_8);}}
    String legacy()throws Exception{return resource("/db/migration/V30__Add_Immutable_Document_Layout_Publications.sql").split("CREATE TABLE document_layout_publication",2)[0];}
    String current()throws Exception{return resource("/db/optional/document-reference-current/V38__Document_Reference_Finite_Current.sql");}
    Map<String,Helper> helpers(String source){var pattern=Pattern.compile("CREATE FUNCTION (document_layout_[a-z0-9_]+)\\((.*?)\\) RETURNS (.*?) LANGUAGE (sql|plpgsql) IMMUTABLE( STRICT)? AS \\$\\$(.*?)\\$\\$;",Pattern.DOTALL);var matcher=pattern.matcher(source);var result=new TreeMap<String,Helper>();while(matcher.find()){var h=new Helper(matcher.group(1),matcher.group(2),matcher.group(3),matcher.group(4),matcher.group(5)!=null,matcher.group(6));assertNull(result.put(h.name(),h));}return result;}
    String digest(String value)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
    String body(Helper h){int count=h.arguments().split(",").length;var args=new ArrayList<String>();for(int i=1;i<=count;i++)args.add("$"+i);return " SELECT public."+h.name()+"("+String.join(",",args)+") ";}
    void verifyAliases(String legacy,String current)throws Exception{
        var helpers=helpers(legacy);assertEquals(NAMES,helpers.keySet());
        for(var h:helpers.values()){
            String types=Arrays.stream(h.arguments().split(",")).map(s->s.trim().substring(s.trim().lastIndexOf(' ')+1)).map(s->s.equals("document_template_revision")?"public.document_template_revision":s).reduce((a,b)->a+","+b).orElseThrow();
            String header="CREATE FUNCTION "+PRIVATE+"."+h.name()+"("+types+") RETURNS "+h.returns()+" LANGUAGE sql IMMUTABLE"+(h.strict()?" STRICT":"")+" SECURITY INVOKER SET search_path=pg_catalog,"+PRIVATE+" AS $$"+body(h)+"$$;";
            assertTrue(current.contains(header),h.name());
            assertTrue(current.contains("'public."+h.name()+"("+types+")'"),h.name()+" signature");
            assertTrue(current.contains("'"+digest(h.body())+"'"),h.name()+" legacy fingerprint");
            assertTrue(current.contains("'"+digest(body(h))+"'"),h.name()+" private fingerprint");
        }
        var definitions=Pattern.compile("CREATE FUNCTION "+PRIVATE+"\\.(document_layout_[a-z0-9_]+)\\(").matcher(current);var found=new HashSet<String>();while(definitions.find())assertTrue(found.add(definitions.group(1)));assertEquals(NAMES,found);
    }
    @Test void exactTransitivePureLegacyClosureIsTwelve()throws Exception{var map=helpers(legacy());assertEquals(NAMES,map.keySet());var calls=Pattern.compile("\\b(document_layout_[a-z0-9_]+)\\s*\\(");for(var h:map.values()){var m=calls.matcher(h.body());while(m.find())assertTrue(NAMES.contains(m.group(1)),h.name()+" -> "+m.group(1));}assertTrue(map.get("document_layout_canonical").body().contains("document_layout_canonical(value)"));}
    @Test void everyAliasDelegatesExactExistingAlgorithmWithTrustedPrivatePath()throws Exception{verifyAliases(legacy(),current());}
    @Test void predecessorPgCatalogOnlyAliasPlanIsRejected()throws Exception{assertThrows(AssertionError.class,()->verifyAliases(legacy(),current().replace("SET search_path=pg_catalog,"+PRIVATE,"SET search_path=pg_catalog")));}
    @Test void missingTransitiveAliasAndChangedLegacyBodyAreRejected()throws Exception{String source=current();var h=helpers(legacy()).get("document_layout_quote");assertThrows(AssertionError.class,()->verifyAliases(legacy(),source.replace("CREATE FUNCTION "+PRIVATE+".document_layout_quote(","CREATE FUNCTION "+PRIVATE+".missing_quote(")));assertThrows(AssertionError.class,()->verifyAliases(legacy().replace("DECLARE result text:=to_jsonb(value)::text;","DECLARE result text:=to_jsonb(value)::text; "),source));}
    @Test void definerPathNeverAddsPublicAndRuntimeChecksExactClosure()throws Exception{String source=current();assertFalse(source.lines().filter(line->!line.startsWith("CREATE FUNCTION "+PRIVATE+".document_layout_")&&!line.stripLeading().startsWith("('public.document_layout_")).anyMatch(line->Pattern.compile("public\\.(?:"+String.join("|",NAMES)+")\\(").matcher(line).find()),"Native callers must enter the trusted private aliases");assertFalse(Pattern.compile("SET search_path=[^$\\n]*\\bpublic\\b").matcher(source).find());assertTrue(source.contains("AND masters_document_current_private.helper_closure_allowed()"));assertTrue(source.contains("NOT legacy.prosecdef"));assertTrue(source.contains("legacy.proconfig IS NULL"));assertTrue(source.contains("legacy.proowner=business.relowner"));assertTrue(source.contains("NOT alias.prosecdef"));assertTrue(source.contains("alias.proconfig=ARRAY['search_path=pg_catalog, masters_document_current_private']"));assertTrue(source.contains("a.grantee<>alias.proowner"));}
    Map<String,List<String>> relations(String sql,Set<String> tables){
        var result=new TreeMap<String,List<String>>();for(String kind:List.of("GRANT","FROM","JOIN","ROWTYPE"))result.put(kind,new ArrayList<>());
        var grants=Pattern.compile("GRANT SELECT ON (.*?) TO [a-z_]+;",Pattern.DOTALL).matcher(sql);
        while(grants.find())for(String relation:grants.group(1).split(",")){relation=relation.trim();if(tables.contains(relation.substring(relation.lastIndexOf('.')+1)))result.get("GRANT").add(relation);}
        for(String kind:List.of("FROM","JOIN")){var matcher=Pattern.compile("\\b"+kind+"\\s+((?:public|"+PRIVATE+")\\.([a-z_]+))").matcher(sql);while(matcher.find())if(tables.contains(matcher.group(2)))result.get(kind).add(matcher.group(1));}
        var row=Pattern.compile("((?:public|"+PRIVATE+")\\.([a-z_]+))%ROWTYPE").matcher(sql);while(row.find())if(tables.contains(row.group(2)))result.get("ROWTYPE").add(row.group(1));return result;
    }
    void verifyLegacyRelations(String sql)throws Exception{
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var baseline=json.readTree(resource("/document-reference-current/source554-relations.json"));var tables=new HashSet<String>();baseline.path("legacyTables").forEach(n->tables.add(n.textValue()));var expected=new TreeMap<String,List<String>>();baseline.path("inventory").fields().forEachRemaining(entry->{var values=new ArrayList<String>();entry.getValue().forEach(n->values.add(n.textValue()));expected.put(entry.getKey(),values);});assertEquals(expected,relations(sql,tables));
    }
    @Test void allLegacyGrantFromJoinAndRowTypesStayExactSource554PublicRelations()throws Exception{verifyLegacyRelations(current());}
    @Test void privateRelationPrefixMutationFailsWhileExactHelperAliasesStayPrivate()throws Exception{String sql=current();verifyLegacyRelations(sql);assertThrows(AssertionError.class,()->verifyLegacyRelations(sql.replace("public.document_layout_publication","masters_document_current_private.document_layout_publication")));assertThrows(AssertionError.class,()->verifyLegacyRelations(sql.replace("public.document_layout_notice","masters_document_current_private.document_layout_notice")));verifyAliases(legacy(),sql);}

}
