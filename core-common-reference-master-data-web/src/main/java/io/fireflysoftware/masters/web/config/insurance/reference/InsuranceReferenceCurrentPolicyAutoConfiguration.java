package io.fireflysoftware.masters.web.config.insurance.reference;

import org.fireflyframework.security.rs.ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration(before=ResourceServerAutoConfiguration.class)
@ConditionalOnProperty(prefix="firefly.masters.insurance-reference.current",name="enabled",havingValue="true")
@Import(InsuranceReferencePolicyAutoConfiguration.DefaultPolicyBackend.class)
public class InsuranceReferenceCurrentPolicyAutoConfiguration {}
