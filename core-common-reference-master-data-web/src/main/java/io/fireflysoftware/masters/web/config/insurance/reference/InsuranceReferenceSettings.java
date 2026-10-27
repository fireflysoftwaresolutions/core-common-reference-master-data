/* Copyright 2026 Firefly Software Foundation. Licensed under Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.config.insurance.reference;

import io.r2dbc.spi.ConnectionFactoryOptions;
import java.net.URI;
import java.util.Objects;
import java.util.Set;

/** Bound only by the explicitly enabled configuration, never global properties scanning. */
record InsuranceReferenceSettings(Set<String> issuers, Connection writer, Connection reader) {
    InsuranceReferenceSettings {
        try {
            issuers = Set.copyOf(issuers);
            if (issuers.isEmpty() || issuers.size() > 16) throw invalid();
            for (var issuer : issuers) {
                var uri = URI.create(issuer);
                if (issuer.length() > 500 || !"https".equals(uri.getScheme()) || uri.getHost() == null
                        || uri.getRawUserInfo() != null || uri.getRawFragment() != null || uri.getRawQuery() != null) throw invalid();
            }
            Objects.requireNonNull(writer);
            Objects.requireNonNull(reader);
            if (!writer.url().equals(reader.url()) || writer.username().equals(reader.username())) throw invalid();
        } catch (RuntimeException ignored) {
            throw invalid();
        }
    }

    @Override
    public String toString() {
        return "InsuranceReferenceSettings";
    }

    record Connection(String url, String username, String password) {
        Connection {
            try {
                if (url == null || url.isBlank() || username == null || username.isBlank()
                        || password == null || password.isBlank()) throw invalid();
                var options = ConnectionFactoryOptions.parse(url);
                if (!"postgresql".equals(options.getRequiredValue(ConnectionFactoryOptions.DRIVER))
                        || options.hasOption(ConnectionFactoryOptions.USER) || options.hasOption(ConnectionFactoryOptions.PASSWORD)) throw invalid();
            } catch (RuntimeException ignored) {
                throw invalid();
            }
        }

        @Override
        public String toString() {
            return "InsuranceReferenceConnectionSettings";
        }
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("INSURANCE_REFERENCE_CONFIGURATION_REQUIRED");
    }
}
