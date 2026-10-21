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

package io.fireflysoftware.domain.insurance.financials.core.workflows;

import io.fireflysoftware.core.insurance.financials.sdk.model.PremiumScheduleDTO;
import io.fireflysoftware.domain.insurance.financials.core.commands.ApplyPremiumScheduleStatusCommand;
import io.fireflysoftware.domain.insurance.financials.core.commands.GetPremiumScheduleQuery;
import io.fireflysoftware.domain.insurance.financials.core.commands.UpdatePremiumScheduleCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.COMPENSATE_RESTORE_RECORD;
import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.CTX_PREVIOUS_RECORD;
import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.CTX_RECORD;
import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.EVENT_RECORD_UPDATED;
import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.EVENT_RETRIEVED;
import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.SAGA_UPDATE_RECORD;
import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.STEP_RETRIEVE_FOR_UPDATE;
import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.STEP_UPDATE_RECORD;

/**
 * Updates a premium schedule on {@code core-insurance-financials} and compensates by writing the previous record back.
 * Status stays on the status saga.
 */
@Saga(name = SAGA_UPDATE_RECORD)
@Service
public class UpdatePremiumScheduleSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Autowired
    public UpdatePremiumScheduleSaga(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @SagaStep(id = STEP_RETRIEVE_FOR_UPDATE)
    @StepEvent(type = EVENT_RETRIEVED)
    public Mono<PremiumScheduleDTO> retrievePremiumScheduleForUpdate(GetPremiumScheduleQuery query, ExecutionContext ctx) {
        return queryBus.query(query)
                .doOnNext(record -> ctx.putVariable(CTX_RECORD, record));
    }

    @SagaStep(id = STEP_UPDATE_RECORD, dependsOn = STEP_RETRIEVE_FOR_UPDATE, compensate = COMPENSATE_RESTORE_RECORD)
    @StepEvent(type = EVENT_RECORD_UPDATED)
    public Mono<PremiumScheduleDTO> updatePremiumSchedule(UpdatePremiumScheduleCommand command, ExecutionContext ctx) {
        PremiumScheduleDTO current = ctx.getVariableAs(CTX_RECORD, PremiumScheduleDTO.class);
        if (current == null || current.getPremiumScheduleId() == null) {
            return Mono.error(new IllegalStateException("Premium schedule was not found"));
        }
        PremiumScheduleDTO incoming = command.record();
        if (incoming == null) {
            return Mono.error(new IllegalArgumentException("Premium schedule is required"));
        }
        if (current.getScheduleStatus() != null) {
            incoming.setScheduleStatus(current.getScheduleStatus());
        }
        ctx.putVariable(CTX_PREVIOUS_RECORD, current);
        return commandBus.send(new ApplyPremiumScheduleStatusCommand(current.getPremiumScheduleId(), incoming))
                .doOnNext(updated -> ctx.putVariable(CTX_RECORD, updated));
    }

    public Mono<Void> restorePremiumSchedule(PremiumScheduleDTO updated, ExecutionContext ctx) {
        PremiumScheduleDTO previous = ctx.getVariableAs(CTX_PREVIOUS_RECORD, PremiumScheduleDTO.class);
        if (previous == null || previous.getPremiumScheduleId() == null) {
            return Mono.empty();
        }
        return commandBus.send(new ApplyPremiumScheduleStatusCommand(previous.getPremiumScheduleId(), previous)).then();
    }
}
