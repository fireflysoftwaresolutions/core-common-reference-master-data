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

package io.fireflysoftware.masters.web.controllers.currency.policy;

import io.fireflysoftware.masters.core.services.currency.policy.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.fireflyframework.web.idempotency.annotation.DisableIdempotency;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping(
        value = "/api/v1/currency-execution-policies",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(
        name = "Currency Execution Policies",
        description =
                "Exact immutable currency representation metadata and current publication"
                        + " lifecycle; no money execution authority")
public class CurrencyExecutionPolicyController {
    private final CurrencyExecutionPolicyService service;

    public CurrencyExecutionPolicyController(CurrencyExecutionPolicyService service) {
        this.service = service;
    }

    @DisableIdempotency
    @GetMapping("/currencies/{currencyId}/source-observation")
    @Operation(
            operationId = "observeCurrencyPolicySource",
            summary =
                    "Observe exact authorized current currency bytes and expected digest before"
                            + " publication")
    public Mono<CurrencyPolicySourceObservation> observeCurrencyPolicySource(
            @PathVariable String currencyId) {
        return Mono.defer(() -> service.observe(CurrencyPolicyRules.uuid(currencyId)));
    }

    @DisableIdempotency
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "publishCurrencyExecutionPolicy",
            summary =
                    "Publish an exact version after current source and head checks; original action"
                            + " replay reauthorizes")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "New committed currency policy",
                content = @Content(schema = @Schema(implementation = CurrencyPolicyView.class))),
        @ApiResponse(
                responseCode = "200",
                description = "Current-authorized retained action replay",
                content = @Content(schema = @Schema(implementation = CurrencyPolicyView.class)))
    })
    public Mono<ResponseEntity<CurrencyPolicyView>> publishCurrencyExecutionPolicy(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    CurrencyPolicyPublishRequest
                                                                            .class)))
                    @RequestBody
                    String raw) {
        return Mono.defer(() -> service.publish(CurrencyPolicyRequestDecoder.publish(raw)))
                .map(
                        o ->
                                ResponseEntity.status(
                                                o.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                                        .body(o.publication()));
    }

    @DisableIdempotency
    @GetMapping("/{policyId}")
    @Operation(
            operationId = "getCurrencyExecutionPolicy",
            summary =
                    "Read an exact retained policy using explicit currency context and current"
                            + " permission")
    public Mono<CurrencyPolicyView> getCurrencyExecutionPolicy(
            @PathVariable String policyId, @RequestParam String currencyId) {
        return Mono.defer(
                () ->
                        service.get(
                                CurrencyPolicyRules.uuid(policyId),
                                CurrencyPolicyRules.uuid(currencyId)));
    }

    @DisableIdempotency
    @GetMapping("/by-action/{actionId}")
    @Operation(
            operationId = "getCurrencyExecutionPolicyByAction",
            summary =
                    "Read an exact retained native receipt using explicit current-authorized"
                            + " currency context")
    public Mono<CurrencyPolicyView> getCurrencyExecutionPolicyByAction(
            @PathVariable String actionId, @RequestParam String currencyId) {
        return Mono.defer(
                () ->
                        service.byAction(
                                CurrencyPolicyRules.uuid(actionId),
                                CurrencyPolicyRules.uuid(currencyId)));
    }

    @DisableIdempotency
    @PostMapping(value = "/{policyId}/retire", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "retireCurrencyExecutionPolicy",
            summary =
                    "Append current-authorized retirement without changing historical policy bytes")
    public Mono<CurrencyPolicyView> retireCurrencyExecutionPolicy(
            @PathVariable String policyId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    CurrencyPolicyRetireRequest
                                                                            .class)))
                    @RequestBody
                    String raw) {
        return Mono.defer(
                () ->
                        service.retire(
                                CurrencyPolicyRules.uuid(policyId),
                                CurrencyPolicyRequestDecoder.retire(policyId, raw)));
    }
}
