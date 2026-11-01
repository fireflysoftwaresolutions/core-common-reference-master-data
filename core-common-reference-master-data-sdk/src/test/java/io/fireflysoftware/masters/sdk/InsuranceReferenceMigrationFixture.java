package io.fireflysoftware.masters.sdk;

import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.DriverManager;

/** Explicit administrator prerequisites for the complete migration chain in a fresh test container. */
final class InsuranceReferenceMigrationFixture {
    private InsuranceReferenceMigrationFixture() {}

    public static void prepare(PostgreSQLContainer postgres) throws Exception {
        if (!postgres.isRunning()) {
            throw new IllegalStateException("Fresh running test container required");
        }
        try (var connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("""
                    REVOKE CREATE ON SCHEMA public FROM PUBLIC;
                    CREATE ROLE masters_ins_ref_owner NOLOGIN NOINHERIT NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION;
                    CREATE ROLE masters_ins_ref_guard NOLOGIN NOINHERIT NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION;
                    CREATE ROLE masters_ins_ref_writer LOGIN NOINHERIT NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION;
                    CREATE ROLE masters_ins_ref_reader LOGIN NOINHERIT NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION;
                    CREATE ROLE masters_ins_ref_source_registrar LOGIN NOINHERIT NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION;
                    CREATE ROLE masters_ins_ref_source_reader LOGIN NOINHERIT NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION;
                    """);
        }
    }
}
