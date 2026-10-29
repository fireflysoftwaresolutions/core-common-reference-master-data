/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.qualification.currency;

import org.fireflyframework.security.core.policy.PolicyRule;
import org.fireflyframework.security.rs.ResourceServerAutoConfiguration;
import org.fireflyframework.security.rs.ResourceServerProperties;
import org.fireflyframework.security.spi.PolicyDecisionPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** Private optional-stack fixture calls the exact native factory with no project policy rules. */
@Configuration(proxyBeanMethods = false)
public class CurrencyPolicyFrameworkFallbackConfiguration {
    @Bean
    PolicyDecisionPort nativeFrameworkPolicy(
            ObjectProvider<PolicyRule> rules, Environment environment) {
        if (rules.stream().findAny().isPresent())
            throw new IllegalStateException(
                    "Private empty-rule qualification requires zero project rules");
        var properties = new ResourceServerProperties();
        properties.setStrictPolicy(
                environment.getProperty(
                        "firefly.security.resource-server.strict-policy", Boolean.class, false));
        return new ResourceServerAutoConfiguration().fireflyPolicyDecisionPort(rules, properties);
    }
}
