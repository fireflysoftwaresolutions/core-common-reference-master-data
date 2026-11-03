/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.config.document.observation;
import java.net.URI;
import java.util.*;
record DocumentReferenceCurrentSettings(Set<String> issuers,Reader reader) {
    DocumentReferenceCurrentSettings{
        issuers=Set.copyOf(issuers);Objects.requireNonNull(reader);
        if(issuers.isEmpty()||issuers.size()>16)throw invalid();
        for(String value:issuers){URI uri=URI.create(value);if(value.length()>500||!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null)throw invalid();}
    }
    record Reader(String url,String username,String password){
        Reader{if(url==null||!url.startsWith("r2dbc:postgresql://")||!"masters_document_current_reader".equals(username)||password==null||password.isBlank())throw invalid();}
        @Override public String toString(){return "DocumentReferenceCurrentReaderSettings";}
    }
    @Override public String toString(){return "DocumentReferenceCurrentSettings";}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("DOCUMENT_REFERENCE_CURRENT_CONFIGURATION_REQUIRED");}
}
