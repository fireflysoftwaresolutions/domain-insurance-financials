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
import io.fireflysoftware.domain.insurance.financials.core.commands.UpdatePremiumScheduleStatusCommand;
import io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies a status change is written through the core and can be restored.
 */
@ExtendWith(MockitoExtension.class)
class UpdatePremiumScheduleStatusSagaTest {

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    private UpdatePremiumScheduleStatusSaga saga;

    @BeforeEach
    void setUp() {
        saga = new UpdatePremiumScheduleStatusSaga(commandBus, queryBus);
    }

    @Test
    void updateStatus_sendsTheRequestedStatusToTheCore() {
        UUID id = UUID.randomUUID();
        PremiumScheduleDTO current = new PremiumScheduleDTO(id);
        current.setScheduleStatus(PremiumScheduleDTO.ScheduleStatusEnum.ACTIVE);
        when(commandBus.send(any(ApplyPremiumScheduleStatusCommand.class))).thenReturn(Mono.just(current));

        ExecutionContext ctx = ExecutionContext.forSaga(PremiumScheduleSagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(PremiumScheduleSagaConstants.CTX_RECORD, current);

        StepVerifier.create(saga.updatePremiumScheduleStatus(new UpdatePremiumScheduleStatusCommand(id, "COMPLETED", "Caller request"), ctx))
                .expectNext(current)
                .verifyComplete();

        ArgumentCaptor<ApplyPremiumScheduleStatusCommand> captor = ArgumentCaptor.forClass(ApplyPremiumScheduleStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().premiumScheduleId()).isEqualTo(id);
        assertThat(captor.getValue().record().getScheduleStatus()).isEqualTo(PremiumScheduleDTO.ScheduleStatusEnum.COMPLETED);
        assertThat(ctx.getVariableAs(PremiumScheduleSagaConstants.CTX_PREVIOUS_STATUS, String.class)).isEqualTo("ACTIVE");
    }

    @Test
    void restore_writesThePreviousStatusBack() {
        UUID id = UUID.randomUUID();
        PremiumScheduleDTO updated = new PremiumScheduleDTO(id);
        updated.setScheduleStatus(PremiumScheduleDTO.ScheduleStatusEnum.COMPLETED);
        when(commandBus.send(any(ApplyPremiumScheduleStatusCommand.class))).thenReturn(Mono.just(updated));

        ExecutionContext ctx = ExecutionContext.forSaga(PremiumScheduleSagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(PremiumScheduleSagaConstants.CTX_PREVIOUS_STATUS, "ACTIVE");

        StepVerifier.create(saga.restorePremiumScheduleStatus(updated, ctx))
                .verifyComplete();

        ArgumentCaptor<ApplyPremiumScheduleStatusCommand> captor = ArgumentCaptor.forClass(ApplyPremiumScheduleStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().record().getScheduleStatus()).isEqualTo(PremiumScheduleDTO.ScheduleStatusEnum.ACTIVE);
    }
}
