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
import io.fireflysoftware.domain.insurance.financials.core.commands.UpdatePremiumScheduleCommand;
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
 * Verifies a field update keeps the current status and can be restored.
 */
@ExtendWith(MockitoExtension.class)
class UpdatePremiumScheduleSagaTest {

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    private UpdatePremiumScheduleSaga saga;

    @BeforeEach
    void setUp() {
        saga = new UpdatePremiumScheduleSaga(commandBus, queryBus);
    }

    @Test
    void update_keepsTheCurrentStatus() {
        UUID id = UUID.randomUUID();
        PremiumScheduleDTO current = new PremiumScheduleDTO(id);
        current.setScheduleStatus(PremiumScheduleDTO.ScheduleStatusEnum.ACTIVE);
        PremiumScheduleDTO incoming = new PremiumScheduleDTO();
        incoming.setScheduleStatus(PremiumScheduleDTO.ScheduleStatusEnum.COMPLETED);
        when(commandBus.send(any(ApplyPremiumScheduleStatusCommand.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0, ApplyPremiumScheduleStatusCommand.class).record()));

        ExecutionContext ctx = ExecutionContext.forSaga(PremiumScheduleSagaConstants.SAGA_UPDATE_RECORD, "test");
        ctx.putVariable(PremiumScheduleSagaConstants.CTX_RECORD, current);

        StepVerifier.create(saga.updatePremiumSchedule(new UpdatePremiumScheduleCommand(id, incoming), ctx))
                .assertNext(saved -> assertThat(saved.getScheduleStatus()).isEqualTo(PremiumScheduleDTO.ScheduleStatusEnum.ACTIVE))
                .verifyComplete();

        ArgumentCaptor<ApplyPremiumScheduleStatusCommand> captor = ArgumentCaptor.forClass(ApplyPremiumScheduleStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().premiumScheduleId()).isEqualTo(id);
        assertThat(captor.getValue().record().getScheduleStatus()).isEqualTo(PremiumScheduleDTO.ScheduleStatusEnum.ACTIVE);
        assertThat(ctx.getVariableAs(PremiumScheduleSagaConstants.CTX_PREVIOUS_RECORD, PremiumScheduleDTO.class)).isSameAs(current);
    }

    @Test
    void restore_writesThePreviousRecordBack() {
        UUID id = UUID.randomUUID();
        PremiumScheduleDTO previous = new PremiumScheduleDTO(id);
        previous.setScheduleStatus(PremiumScheduleDTO.ScheduleStatusEnum.ACTIVE);
        PremiumScheduleDTO updated = new PremiumScheduleDTO(id);
        updated.setScheduleStatus(PremiumScheduleDTO.ScheduleStatusEnum.COMPLETED);
        when(commandBus.send(any(ApplyPremiumScheduleStatusCommand.class))).thenReturn(Mono.just(previous));

        ExecutionContext ctx = ExecutionContext.forSaga(PremiumScheduleSagaConstants.SAGA_UPDATE_RECORD, "test");
        ctx.putVariable(PremiumScheduleSagaConstants.CTX_PREVIOUS_RECORD, previous);

        StepVerifier.create(saga.restorePremiumSchedule(updated, ctx))
                .verifyComplete();

        ArgumentCaptor<ApplyPremiumScheduleStatusCommand> captor = ArgumentCaptor.forClass(ApplyPremiumScheduleStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().record()).isSameAs(previous);
    }
}
