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

package io.fireflysoftware.domain.insurance.financials.web.controllers;

import io.fireflysoftware.core.insurance.financials.sdk.model.PremiumScheduleDTO;
import io.fireflysoftware.domain.insurance.financials.core.commands.CreatePremiumScheduleCommand;
import io.fireflysoftware.domain.insurance.financials.core.commands.UpdatePremiumScheduleCommand;
import io.fireflysoftware.domain.insurance.financials.core.commands.UpdatePremiumScheduleStatusCommand;
import io.fireflysoftware.domain.insurance.financials.core.services.FinancialsService;
import io.fireflysoftware.domain.insurance.financials.interfaces.dtos.ReasonRequest;
import io.fireflysoftware.domain.insurance.financials.interfaces.dtos.StatusChangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Domain API for premium schedule records.
 */
@RestController
@RequestMapping("/api/v1/premium-schedules")
@Tag(name = "Financials", description = "Domain orchestration for premium schedule records")
public class PremiumSchedulesController {

    private final FinancialsService service;

    public PremiumSchedulesController(FinancialsService service) {
        this.service = service;
    }

    @Operation(summary = "Create premium schedule", description = "Runs the register saga against core-insurance-financials.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PremiumScheduleDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<PremiumScheduleDTO>> create(@RequestBody PremiumScheduleDTO body) {
        return service.create(new CreatePremiumScheduleCommand(body))
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }

    @Operation(summary = "Get premium schedule", description = "Reads one record from core-insurance-financials.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PremiumScheduleDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @GetMapping(value = "/{premiumScheduleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<PremiumScheduleDTO>> get(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID premiumScheduleId) {
        return service.get(premiumScheduleId)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Update status", description = "Runs the status saga against core-insurance-financials.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PremiumScheduleDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PutMapping(value = "/{premiumScheduleId}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<PremiumScheduleDTO>> updateStatus(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID premiumScheduleId,
            @RequestBody StatusChangeRequest request) {
        return service.updateStatus(new UpdatePremiumScheduleStatusCommand(premiumScheduleId, request.getStatus(), request.getStatusReason()))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Update premium schedule", description = "Runs the update saga against core-insurance-financials. Status is left unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PremiumScheduleDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PutMapping(value = "/{premiumScheduleId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<PremiumScheduleDTO>> update(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID premiumScheduleId,
            @RequestBody PremiumScheduleDTO body) {
        return service.update(new UpdatePremiumScheduleCommand(premiumScheduleId, body))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Cancel premium schedule", description = "Cancels a premium schedule through the financials core.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PremiumScheduleDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PostMapping(value = "/{premiumScheduleId}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<PremiumScheduleDTO>> cancel(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID premiumScheduleId,
            @RequestBody ReasonRequest request) {
        return service.cancel(premiumScheduleId, request.getStatusReason())
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Complete premium schedule", description = "Completes a premium schedule through the financials core.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PremiumScheduleDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PostMapping(value = "/{premiumScheduleId}/complete", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<PremiumScheduleDTO>> complete(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID premiumScheduleId,
            @RequestBody ReasonRequest request) {
        return service.complete(premiumScheduleId, request.getStatusReason())
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Remove premium schedule", description = "Deletes the record through core-insurance-financials when that core allows removal.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removed"),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @DeleteMapping("/{premiumScheduleId}")
    public Mono<ResponseEntity<Void>> remove(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID premiumScheduleId) {
        return service.remove(premiumScheduleId)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
