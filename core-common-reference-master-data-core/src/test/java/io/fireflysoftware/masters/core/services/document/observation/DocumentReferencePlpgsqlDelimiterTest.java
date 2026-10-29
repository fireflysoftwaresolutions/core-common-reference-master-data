/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** PL/pgSQL reads an IF SQL expression to its first unparenthesized THEN token. */
class DocumentReferencePlpgsqlDelimiterTest {
    static final String CASE="CASE k WHEN 'template' THEN 'DOCUMENT_TEMPLATE' ELSE 'DOCUMENT_LAYOUT' END";
    String source()throws Exception{try(var stream=getClass().getResourceAsStream("/db/optional/document-reference-current/V38__Document_Reference_Finite_Current.sql")){assertNotNull(stream);return new String(stream.readAllBytes(),StandardCharsets.UTF_8);}}
    static String condition(String source){
        int start=source.indexOf("IF masters_document_current_private.document_layout_shape(r,");if(start<0)throw new IllegalArgumentException("Exact owner target condition required");return expression(source.substring(start+2));
    }
    static String expression(String sql){
        int depth=0,cases=0;boolean quoted=false;
        for(int i=0;i<sql.length();){char ch=sql.charAt(i);
            if(quoted){if(ch=='\''){if(i+1<sql.length()&&sql.charAt(i+1)=='\''){i+=2;continue;}quoted=false;}i++;continue;}
            if(ch=='\''){quoted=true;i++;continue;}if(ch=='('){depth++;i++;continue;}if(ch==')'){if(--depth<0)throw new IllegalArgumentException();i++;continue;}
            if(Character.isLetter(ch)||ch=='_'){int end=i+1;while(end<sql.length()&&(Character.isLetterOrDigit(sql.charAt(end))||sql.charAt(end)=='_'))end++;String token=sql.substring(i,end);if(token.equals("CASE"))cases++;else if(token.equals("END"))cases--;else if(token.equals("THEN")&&depth==0){if(cases!=0)throw new IllegalArgumentException("IF delimiter truncates an unparenthesized SQL CASE");return sql.substring(0,i);}i=end;continue;}i++;
        }throw new IllegalArgumentException("Complete IF delimiter required");
    }
    @Test void realTargetConditionIncludesCompleteGroupedCaseBeforeOuterThen()throws Exception{String condition=condition(source());assertTrue(condition.contains("("+CASE+")"));assertTrue(condition.contains("document_layout_sha(r->'digest')"));}
    @Test void originalNative42601UnparenthesizedCaseIsARealNegativeControl()throws Exception{String broken=source().replace("("+CASE+")",CASE);assertThrows(IllegalArgumentException.class,()->condition(broken));}
    @Test void quotedThenAndNestedCaseCannotBecomeOuterDelimiter(){assertEquals(" ('THEN'='THEN') AND (CASE k WHEN 'template' THEN 'DOCUMENT_TEMPLATE' ELSE 'DOCUMENT_LAYOUT' END)='DOCUMENT_TEMPLATE' ",expression(" ('THEN'='THEN') AND ("+CASE+")='DOCUMENT_TEMPLATE' THEN RETURN false;"));}
}
