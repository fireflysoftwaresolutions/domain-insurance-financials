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

package io.fireflysoftware.domain.insurance.financials.core.handlers;

import io.fireflysoftware.core.insurance.financials.sdk.api.PremiumSchedulesApi;
import io.fireflysoftware.core.insurance.financials.sdk.model.PremiumScheduleDTO;
import io.fireflysoftware.domain.insurance.financials.core.commands.GetPremiumScheduleQuery;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.query.QueryHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Loads one premium schedule from the core.
 */
@Slf4j
@QueryHandlerComponent
public class GetPremiumScheduleHandler extends QueryHandler<GetPremiumScheduleQuery, PremiumScheduleDTO> {

    private final PremiumSchedulesApi premiumSchedulesApi;

    public GetPremiumScheduleHandler(PremiumSchedulesApi premiumSchedulesApi) {
        this.premiumSchedulesApi = premiumSchedulesApi;
    }

    @Override
    protected Mono<PremiumScheduleDTO> doHandle(GetPremiumScheduleQuery query) {
        log.debug("Fetching premium schedule: premiumScheduleId={}", query.premiumScheduleId());
        return premiumSchedulesApi.getPremiumScheduleById(query.premiumScheduleId(), UUID.randomUUID().toString());
    }
}
