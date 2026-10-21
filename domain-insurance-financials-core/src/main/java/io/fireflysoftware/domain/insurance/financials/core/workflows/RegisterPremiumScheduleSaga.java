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
import io.fireflysoftware.domain.insurance.financials.core.commands.CreatePremiumScheduleCommand;
import io.fireflysoftware.domain.insurance.financials.core.commands.RemovePremiumScheduleCommand;

import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.COMPENSATE_REMOVE;
import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.COMPENSATE_REMOVE_INITIAL_STATUS;
import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.CTX_RECORD;
import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.EVENT_INITIAL_STATUS_RECORDED;
import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.EVENT_REGISTERED;
import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.SAGA_REGISTER;
import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.STEP_RECORD_INITIAL_STATUS;
import static io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants.STEP_REGISTER;

/**
 * Creates a premium schedule on {@code core-insurance-financials} and compensates by deleting it.
 */
@Saga(name = SAGA_REGISTER)
@Service
public class RegisterPremiumScheduleSaga {

    private final CommandBus commandBus;

    @Autowired
    public RegisterPremiumScheduleSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_REGISTER, compensate = COMPENSATE_REMOVE)
    @StepEvent(type = EVENT_REGISTERED)
    public Mono<PremiumScheduleDTO> registerPremiumSchedule(CreatePremiumScheduleCommand command, ExecutionContext ctx) {
        return commandBus.send(command)
                .doOnNext(created -> ctx.putVariable(CTX_RECORD, created));
    }

    public Mono<Void> removePremiumSchedule(PremiumScheduleDTO created) {
        if (created == null || created.getPremiumScheduleId() == null) {
            return Mono.empty();
        }
        return commandBus.send(new RemovePremiumScheduleCommand(created.getPremiumScheduleId()));
    }

}
