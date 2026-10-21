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

package io.fireflysoftware.domain.insurance.financials.core.services;

import io.fireflysoftware.core.insurance.financials.sdk.model.PremiumScheduleDTO;
import io.fireflysoftware.domain.insurance.financials.core.commands.CreatePremiumScheduleCommand;
import io.fireflysoftware.domain.insurance.financials.core.commands.GetPremiumScheduleQuery;
import io.fireflysoftware.domain.insurance.financials.core.commands.UpdatePremiumScheduleStatusCommand;
import io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Runs the premium schedule sagas and reads through the query bus.
 */
@Service
public class FinancialsServiceImpl implements FinancialsService {

    private final SagaEngine sagaEngine;
    private final QueryBus queryBus;

    public FinancialsServiceImpl(SagaEngine sagaEngine, QueryBus queryBus) {
        this.sagaEngine = sagaEngine;
        this.queryBus = queryBus;
    }

    @Override
    public Mono<PremiumScheduleDTO> create(CreatePremiumScheduleCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(PremiumScheduleSagaConstants.STEP_REGISTER, command)
                .build();
        return sagaEngine.execute(PremiumScheduleSagaConstants.SAGA_REGISTER, inputs)
                .flatMap(result -> outcome(result, PremiumScheduleSagaConstants.STEP_REGISTER, PremiumScheduleDTO.class));
    }

    @Override
    public Mono<PremiumScheduleDTO> updateStatus(UpdatePremiumScheduleStatusCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(PremiumScheduleSagaConstants.STEP_RETRIEVE, new GetPremiumScheduleQuery(command.premiumScheduleId()))
                .forStepId(PremiumScheduleSagaConstants.STEP_UPDATE_STATUS, command)
                .build();
        return sagaEngine.execute(PremiumScheduleSagaConstants.SAGA_UPDATE_STATUS, inputs)
                .flatMap(result -> outcome(result, PremiumScheduleSagaConstants.STEP_UPDATE_STATUS, PremiumScheduleDTO.class));
    }

    @Override
    public Mono<PremiumScheduleDTO> get(UUID premiumScheduleId) {
        return queryBus.query(new GetPremiumScheduleQuery(premiumScheduleId));
    }

    private static <T> Mono<T> outcome(SagaResult result, String stepId, Class<T> type) {
        if (result.isFailed()) {
            Throwable error = result.error().orElseGet(() ->
                    new IllegalStateException("Saga " + result.sagaName() + " failed"));
            return Mono.error(error);
        }
        return Mono.justOrEmpty(result.resultOf(stepId, type))
                .switchIfEmpty(Mono.error(new IllegalStateException("Saga step " + stepId + " produced no result")));
    }
}
