/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.sdk;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Actual forward optional installation and exact target validation; no Main or financial/document admission. */
class DocumentReferenceQuotePurposeNativeIT {
    private static PostgreSQLContainer postgres;
    private static String originalOid,originalAcl;
    private static final String INSTALLER="masters_document_quote_installer",PASSWORD="synthetic-test-only";
    @BeforeAll static void install()throws Exception{
        postgres=new PostgreSQLContainer("postgres:17-alpine").withDatabaseName("quote_reference_native").withUsername("fixture_admin").withPassword(PASSWORD)
            .withCreateContainerCmdModifier(cmd->cmd.getHostConfig().withPortBindings(new com.github.dockerjava.api.model.PortBinding(com.github.dockerjava.api.model.Ports.Binding.bindIp("127.0.0.1"),new com.github.dockerjava.api.model.ExposedPort(5432))));
        try{
            postgres.start();InsuranceReferenceMigrationFixture.prepare(postgres);Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()).locations("classpath:db/migration").load().migrate();
            try(var c=admin();var s=c.createStatement()){
                s.execute("""
                    REVOKE CREATE,TEMPORARY ON DATABASE quote_reference_native FROM PUBLIC;
                    REVOKE CREATE ON SCHEMA public FROM PUBLIC;
                    CREATE ROLE masters_document_current_guard NOLOGIN NOINHERIT NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION;
                    CREATE ROLE masters_document_current_reader LOGIN NOINHERIT NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION;
                    CREATE ROLE masters_document_quote_installer LOGIN NOINHERIT NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD 'synthetic-test-only';
                    GRANT masters_document_current_guard TO masters_document_quote_installer WITH INHERIT TRUE,SET TRUE,ADMIN FALSE;
                    GRANT CREATE ON DATABASE quote_reference_native TO masters_document_quote_installer;
                    GRANT USAGE,CREATE ON SCHEMA public TO masters_document_quote_installer WITH GRANT OPTION;
                    GRANT SELECT ON public.document_layout_publication,public.document_layout_notice,public.document_template_revision,public.document_template_catalog,public.document_template_localization,public.document_template_type_catalog,public.language_locale TO masters_document_quote_installer WITH GRANT OPTION;
                    CREATE SCHEMA masters_document_current_migrations AUTHORIZATION masters_document_quote_installer;
                    """);
            }
            migration("38").migrate();originalOid=scalar("SELECT 'masters_document_current_private.target_valid(jsonb)'::regprocedure::oid::text");originalAcl=scalar("SELECT proacl::text FROM pg_proc WHERE oid='masters_document_current_private.target_valid(jsonb)'::regprocedure");migration(null).migrate();
        }catch(Exception|Error error){if(postgres!=null)postgres.stop();throw error;}
    }
    @AfterAll static void stop(){if(postgres!=null)postgres.stop();}
    private static Connection admin()throws Exception{return DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());}
    private static Flyway migration(String target){var c=Flyway.configure().dataSource(postgres.getJdbcUrl(),INSTALLER,PASSWORD).locations("classpath:db/optional/document-reference-current").defaultSchema("masters_document_current_migrations").schemas("masters_document_current_migrations").createSchemas(false).cleanDisabled(true).baselineOnMigrate(false).validateOnMigrate(true);if(target!=null)c.target(target);return c.load();}
    private static String scalar(String sql)throws Exception{try(var c=admin();var s=c.createStatement();var r=s.executeQuery(sql)){assertTrue(r.next());return r.getString(1);}}
    private static String target(String purpose){String id="10000000-0000-4000-8000-000000000001";return "{\"product\":{\"tenantId\":\""+id+"\",\"rootId\":\""+id+"\",\"productId\":\""+id+"\",\"revisionId\":\""+id+"\",\"publicationId\":\""+id+"\",\"definitionDigest\":\""+"a".repeat(64)+"\"},\"purpose\":\""+purpose+"\",\"documentType\":\"CONTRACT\",\"locale\":\"en-US\",\"template\":"+reference(id,"DOCUMENT_TEMPLATE")+",\"layout\":"+reference("10000000-0000-4000-8000-000000000002","DOCUMENT_LAYOUT")+"}";}
    private static String reference(String id,String kind){return "{\"owner\":\"core-common-reference-master-data\",\"kind\":\""+kind+"\",\"id\":\""+id+"\",\"versionFormat\":\"OPAQUE\",\"versionText\":\"release.01\",\"digest\":\""+"a".repeat(64)+"\"}";}
    private static boolean valid(String json)throws Exception{try(var c=admin();var s=c.createStatement()){s.execute("SET ROLE masters_document_current_guard");try(var q=c.prepareStatement("SELECT masters_document_current_private.target_valid(?::jsonb)")){q.setString(1,json);try(var r=q.executeQuery()){assertTrue(r.next());return r.getBoolean(1);}}}}
    @Test void bothExactNativePurposesPassWithoutBeingEquivalent()throws Exception{assertTrue(valid(target("DEPOSIT_ACCOUNT_ENROLLMENT")));assertTrue(valid(target("INSURANCE_TERM_PC_QUOTE")));assertNotEquals(target("DEPOSIT_ACCOUNT_ENROLLMENT"),target("INSURANCE_TERM_PC_QUOTE"));}
    @Test void unsupportedPurposeWrongVersionScopeAndKindsRefuse()throws Exception{for(var purpose:List.of("INSURANCE_POLICY_ISSUANCE","INSURANCE_CONTRACT_SIGNATURE","TERM_PC_INSURANCE_QUOTE",""))assertFalse(valid(target(purpose)));var t=target("INSURANCE_TERM_PC_QUOTE");assertFalse(valid(t.replace("\"purpose\":\"INSURANCE_TERM_PC_QUOTE\"","\"purpose\":null")));assertFalse(valid(t.replace("\"versionFormat\":\"OPAQUE\"","\"versionFormat\":\"NUMERIC\"")));assertFalse(valid(t.replace("DOCUMENT_TEMPLATE","DOCUMENT_LAYOUT")));assertFalse(valid(t.replace("\"product\":{","\"product\":{\"unknown\":true,")));}
    @Test void forwardMigrationPreservesFunctionOwnerOidAndAcl()throws Exception{assertEquals(originalOid,scalar("SELECT 'masters_document_current_private.target_valid(jsonb)'::regprocedure::oid::text"));assertEquals(originalAcl,scalar("SELECT proacl::text FROM pg_proc WHERE oid='masters_document_current_private.target_valid(jsonb)'::regprocedure"));assertEquals("masters_document_current_guard",scalar("SELECT pg_get_userbyid(proowner) FROM pg_proc WHERE oid='masters_document_current_private.target_valid(jsonb)'::regprocedure"));assertEquals("false",scalar("SELECT has_schema_privilege('masters_document_current_guard','public','CREATE')::text"));}
    @Test void runtimeReaderGainsNoPrivateOrMigrationAuthority()throws Exception{assertEquals("false",scalar("SELECT has_function_privilege('masters_document_current_reader','masters_document_current_private.target_valid(jsonb)','EXECUTE')::text"));assertEquals("false",scalar("SELECT has_schema_privilege('masters_document_current_reader','masters_document_current_private','USAGE,CREATE')::text"));assertEquals("false",scalar("SELECT has_schema_privilege('masters_document_current_reader','masters_document_current_migrations','USAGE,CREATE')::text"));assertEquals("false",scalar("SELECT pg_has_role('masters_document_current_reader','masters_document_current_guard','MEMBER')::text"));}
    @Test void dedicatedHistoryValidatesAcrossFreshInstallerConnections()throws Exception{migration(null).validate();assertEquals(0,migration(null).migrate().migrationsExecuted);assertEquals("2",scalar("SELECT count(*)::text FROM masters_document_current_migrations.flyway_schema_history WHERE success AND version IN ('38','39')"));assertEquals("36",scalar("SELECT count(*)::text FROM public.flyway_schema_history WHERE success AND version IS NOT NULL"));}
}
