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
import io.fireflysoftware.domain.insurance.financials.core.constants.PremiumScheduleSagaConstants;

import org.fireflyframework.cqrs.command.CommandBus;
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
 * Verifies the register saga stores the core result and can compensate by deleting it.
 */
@ExtendWith(MockitoExtension.class)
class RegisterPremiumScheduleSagaTest {

    @Mock
    private CommandBus commandBus;

    private RegisterPremiumScheduleSaga saga;

    @BeforeEach
    void setUp() {
        saga = new RegisterPremiumScheduleSaga(commandBus);
    }

    @Test
    void register_storesTheRecordReturnedByTheCore() {
        UUID id = UUID.randomUUID();
        PremiumScheduleDTO created = new PremiumScheduleDTO(id);
        created.setScheduleStatus(PremiumScheduleDTO.ScheduleStatusEnum.ACTIVE);
        when(commandBus.send(any(CreatePremiumScheduleCommand.class))).thenReturn(Mono.just(created));

        ExecutionContext ctx = ExecutionContext.forSaga(PremiumScheduleSagaConstants.SAGA_REGISTER, "test");
        PremiumScheduleDTO request = new PremiumScheduleDTO();
        request.setScheduleStatus(PremiumScheduleDTO.ScheduleStatusEnum.ACTIVE);

        StepVerifier.create(saga.registerPremiumSchedule(new CreatePremiumScheduleCommand(request), ctx))
                .expectNext(created)
                .verifyComplete();

        assertThat(ctx.getVariableAs(PremiumScheduleSagaConstants.CTX_RECORD, PremiumScheduleDTO.class).getPremiumScheduleId()).isEqualTo(id);
    }

    @Test
    void remove_sendsDeleteForTheCreatedRecord() {
        UUID id = UUID.randomUUID();
        when(commandBus.send(any(RemovePremiumScheduleCommand.class))).thenReturn(Mono.empty());

        StepVerifier.create(saga.removePremiumSchedule(new PremiumScheduleDTO(id)))
                .verifyComplete();

        ArgumentCaptor<RemovePremiumScheduleCommand> captor = ArgumentCaptor.forClass(RemovePremiumScheduleCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().premiumScheduleId()).isEqualTo(id);
    }

}
