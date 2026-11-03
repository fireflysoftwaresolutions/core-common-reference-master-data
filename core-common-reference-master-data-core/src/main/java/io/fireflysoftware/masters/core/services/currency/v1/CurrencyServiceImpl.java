/*
 * Copyright 2025 Firefly Software Foundation
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


package io.fireflysoftware.masters.core.services.currency.v1;

import org.fireflyframework.core.filters.FilterRequest;
import org.fireflyframework.core.filters.FilterUtils;
import org.fireflyframework.core.queries.PaginationRequest;
import org.fireflyframework.core.queries.PaginationResponse;
import org.fireflyframework.core.queries.PaginationUtils;
import io.fireflysoftware.masters.core.mappers.currency.v1.CurrencyMapper;
import io.fireflysoftware.masters.interfaces.dtos.country.v1.CountryDTO;
import io.fireflysoftware.masters.interfaces.dtos.currency.v1.CurrencyDTO;
import io.fireflysoftware.masters.models.entities.country.v1.Country;
import io.fireflysoftware.masters.models.entities.currency.v1.Currency;
import io.fireflysoftware.masters.models.repositories.currency.v1.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Service
@Transactional
public class CurrencyServiceImpl implements CurrencyService {

    @Autowired
    private CurrencyRepository repository;

    @Autowired
    private CurrencyMapper mapper;

    @Override
    public Mono<PaginationResponse<CurrencyDTO>> listCurrencies(FilterRequest<CurrencyDTO> filterRequest) {
        return FilterUtils
                .createFilter(
                        Currency.class,
                        mapper::toDTO
                )
                .filter(filterRequest);
    }

    @Override
    public Mono<CurrencyDTO> createCurrency(CurrencyDTO currencyDto) {
        Currency currency = mapper.toEntity(currencyDto);
        return repository.save(currency)
                .map(mapper::toDTO);
    }

    @Override
    public Mono<CurrencyDTO> getCurrency(UUID currencyId) {
        return repository.findById(currencyId)
                .map(mapper::toDTO);
    }

    @Override
    public Mono<CurrencyDTO> updateCurrency(UUID currencyId, CurrencyDTO currencyDto) {
        return repository.findById(currencyId)
                .flatMap(existingCurrency -> {
                    Currency updatedCurrency = mapper.toEntity(currencyDto);
                    updatedCurrency.setCurrencyId(currencyId); // Ensure ID matches the existing record
                    return repository.save(updatedCurrency);
                })
                .map(mapper::toDTO)
                .onErrorMap(this::policyConstraint);
    }

    @Override
    public Mono<Void> deleteCurrency(UUID currencyId) {
        return repository.deleteById(currencyId).onErrorMap(this::policyConstraint);
    }

    private Throwable policyConstraint(Throwable error) {
        if (!(error instanceof org.springframework.dao.DataIntegrityViolationException)) return error;
        Throwable cause = error;
        for (int depth = 0; cause != null && depth < 8; depth++, cause = cause.getCause()) {
            String message = java.util.Objects.toString(cause.getMessage(), "");
            if (message.contains("PUBLISHED_CURRENCY_IDENTITY_CANNOT_CHANGE_OR_DELETE")
                    || message.contains("CURRENCY_POLICY_CONCURRENT_SOURCE_CHANGE")
                    || message.contains("currency_policy_identity_currency_id_fkey")) {
                return new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Published currency identity cannot be retargeted or deleted");
            }
            if (cause.getCause() == cause) break;
        }
        return error;
    }
}
