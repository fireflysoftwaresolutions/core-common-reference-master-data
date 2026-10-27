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

package io.fireflysoftware.masters.core.services.currency.policy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.fireflysoftware.masters.core.mappers.currency.v1.CurrencyMapper;
import io.fireflysoftware.masters.core.services.currency.v1.CurrencyServiceImpl;
import io.fireflysoftware.masters.interfaces.dtos.currency.v1.CurrencyDTO;
import io.fireflysoftware.masters.models.entities.currency.v1.Currency;
import io.fireflysoftware.masters.models.repositories.currency.v1.CurrencyRepository;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import java.util.UUID;

class CurrencyPolicyLegacyConflictTest {
    @Test
    void identityUpdateAndDeleteFenceMapToStable409WithoutChangingOtherErrors() {
        var repository = mock(CurrencyRepository.class);
        var mapper = mock(CurrencyMapper.class);
        var service = new CurrencyServiceImpl();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "mapper", mapper);
        var id = UUID.randomUUID();
        var entity = new Currency();
        entity.setCurrencyId(id);
        var dto = new CurrencyDTO();
        when(repository.findById(id)).thenReturn(Mono.just(entity));
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(entity))
                .thenReturn(
                        Mono.error(
                                new DataIntegrityViolationException(
                                        "PUBLISHED_CURRENCY_IDENTITY_CANNOT_CHANGE_OR_DELETE")));
        when(repository.deleteById(id))
                .thenReturn(
                        Mono.error(
                                new DataIntegrityViolationException(
                                        "PUBLISHED_CURRENCY_IDENTITY_CANNOT_CHANGE_OR_DELETE")));
        for (var operation :
                java.util.List.of(
                        service.updateCurrency(id, dto).then(), service.deleteCurrency(id)))
            assertThatThrownBy(operation::block)
                    .isInstanceOfSatisfying(
                            ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        var unrelated = new DataIntegrityViolationException("unrelated legacy constraint");
        when(repository.deleteById(id)).thenReturn(Mono.error(unrelated));
        assertThatThrownBy(() -> service.deleteCurrency(id).block()).isSameAs(unrelated);
        var unavailable = new IllegalStateException("synthetic unavailable");
        when(repository.deleteById(id)).thenReturn(Mono.error(unavailable));
        assertThatThrownBy(() -> service.deleteCurrency(id).block()).isSameAs(unavailable);
    }
}
