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
import io.fireflysoftware.domain.insurance.financials.core.commands.UpdatePremiumScheduleStatusCommand;
import io.fireflysoftware.domain.insurance.financials.core.commands.UpdatePremiumScheduleCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Application service for premium schedule orchestration.
 */
public interface FinancialsService {

    /**
     * Creates a premium schedule through the register saga.
     *
     * @param command the create command
     * @return the record returned by the core
     */
    Mono<PremiumScheduleDTO> create(CreatePremiumScheduleCommand command);

    /**
     * Moves a premium schedule to a new status through the core.
     *
     * @param command the status change
     * @return the updated record
     */
    Mono<PremiumScheduleDTO> updateStatus(UpdatePremiumScheduleStatusCommand command);

    /**
     * Reads one premium schedule.
     *
     * @param premiumScheduleId the core identifier
     * @return the record, or empty when the core has none
     */
    Mono<PremiumScheduleDTO> get(UUID premiumScheduleId);

    /**
     * Updates fields on a premium schedule and leaves its status unchanged.
     *
     * @param command the update
     * @return the record returned by the core
     */
    Mono<PremiumScheduleDTO> update(UpdatePremiumScheduleCommand command);

    /**
     * Cancels a premium schedule through the financials core.
     *
     * @param premiumScheduleId the core identifier
     * @param statusReason optional reason stored by the status saga when the core keeps one
     * @return the updated record
     */
    Mono<PremiumScheduleDTO> cancel(UUID premiumScheduleId, String statusReason);

    /**
     * Completes a premium schedule through the financials core.
     *
     * @param premiumScheduleId the core identifier
     * @param statusReason optional reason stored by the status saga when the core keeps one
     * @return the updated record
     */
    Mono<PremiumScheduleDTO> complete(UUID premiumScheduleId, String statusReason);

    /**
     * Deletes a premium schedule when the core lifecycle allows removal.
     *
     * @param premiumScheduleId the core identifier
     * @return completion when the core has deleted the record
     */
    Mono<Void> remove(UUID premiumScheduleId);
}
