package io.fireflysoftware.masters.web.config.currency.observation;
import java.net.URI;
import java.util.*;
record CurrencyPolicyCurrentSettings(Set<String> issuers,Reader reader) {
    CurrencyPolicyCurrentSettings{
        issuers=Set.copyOf(issuers);Objects.requireNonNull(reader);
        if(issuers.isEmpty()||issuers.size()>16)throw invalid();
        for(String value:issuers){URI uri=URI.create(value);if(value.length()>500||!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null)throw invalid();}
    }
    record Reader(String url,String username,String password){
        Reader{if(url==null||!url.startsWith("r2dbc:postgresql://")||!"masters_currency_current_reader".equals(username)||password==null||password.isBlank())throw invalid();}
        @Override public String toString(){return "CurrencyPolicyCurrentReaderSettings";}
    }
    @Override public String toString(){return "CurrencyPolicyCurrentSettings";}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("CURRENCY_POLICY_CURRENT_CONFIGURATION_REQUIRED");}
}
