<a href="https://fireflysoftware.io/openplatform/">
<picture>
  <source media="(max-width: 640px) and (prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-dark.svg">
  <source media="(max-width: 640px)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-light.svg">
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-dark.svg">
  <img src="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-light.svg" alt="Firefly OpenPlatform - Domain Insurance - Financials" width="100%">
</picture>
</a>

# Firefly OpenPlatform - Domain Insurance - Financials

Developed by the Firefly Software Foundation. [dev@fireflysoftware.io](mailto:dev@fireflysoftware.io)

[Website](https://fireflysoftware.io/) · [OpenPlatform](https://fireflysoftware.io/openplatform/) · [Module catalogue](https://fireflysoftware.io/openplatform/modules/)

Domain layer microservice for premium schedule records within the Firefly platform. This service orchestrates create, update, status change, close, and query against `core-insurance-financials` with compensating saga steps.

Repository: [https://github.com/fireflysoftwaresolutions/domain-insurance-financials](https://github.com/fireflysoftwaresolutions/domain-insurance-financials)

---

## Overview

`domain-insurance-financials` sits between experience services and `core-insurance-financials`. Write operations run through the FireflyFramework saga engine. Queries use the CQRS query bus. The domain depends on the core SDK only. It does not import another core's internal packages. Identifiers that point at parties, contracts, products, or policies stay UUIDs owned by those cores.

It creates, updates, changes status, and closes premium schedules through the financials core. It does not disburse indemnity, refund premium, post commission, or certify IFRS 17 or Solvency II.

### Key features

- **Create** -- register a premium schedule through `core-insurance-financials`, with compensation that deletes the core write
- **Update** -- read the current premium schedule and write the new field values through `updatePremiumSchedule` on `core-insurance-financials`. Status is left unchanged. Compensation writes the previous record back.
- **Status change** -- read the current premium schedule and update its status through `core-insurance-financials`. This core keeps status on the record, so the status saga does not append a separate history row. Compensation writes the previous status back through the core.
- **Close** -- Cancel sets CANCELLED and complete sets COMPLETED through the status saga. The financials core does not restrict those moves or delete, so remove deletes the schedule whenever it exists.
- **Query** -- read one premium schedule by identifier
- **Compensation** -- a failed later step asks the core to undo the work already accepted
- **CQRS** -- commands and queries travel on separate buses
- **Reactive I/O** -- Spring WebFlux with virtual threads enabled

## Architecture

### Module structure

| Module | Purpose |
|--------|---------|
| `domain-insurance-financials-core` | Saga workflows, commands, handlers, and the application service |
| `domain-insurance-financials-interfaces` | Request contracts for a status change and a closing reason |
| `domain-insurance-financials-infra` | SDK client factory and downstream base-path configuration |
| `domain-insurance-financials-web` | Spring Boot application, REST API, actuator, and OpenAPI |
| `domain-insurance-financials-sdk` | Java client generated from the OpenAPI document |

### Dependency flow

```
web --> interfaces --> core --> infra --> core-insurance-financials-sdk
sdk (generated from openapi.yml)
```

### Saga map

| Saga | Core calls | Compensation |
|------|------------|--------------|
| `RegisterPremiumScheduleSaga` | `createPremiumSchedule` on `PremiumSchedulesApi` | Deletes the record through `deletePremiumSchedule` |
| `UpdatePremiumScheduleSaga` | Reads the current record, then writes field changes through `updatePremiumSchedule` on `PremiumSchedulesApi`. Status is left unchanged. | Writes the previous record back through `updatePremiumSchedule` |
| `UpdatePremiumScheduleStatusSaga` | Reads the current record, then updates its status through `updatePremiumSchedule` | Writes the previous status back through the core |
| Query | `getPremiumScheduleById` on `PremiumSchedulesApi` | None. A read has nothing to undo. |

Status values accepted by the core: `ACTIVE`, `COMPLETED`, `CANCELLED`.

## Setup

### Prerequisites

- Java 25
- Apache Maven 3.9+
- `core-insurance-financials-sdk` 26.10.6
- Apache Kafka on `localhost:9092` when event publishing is left at the default

### Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_ADDRESS` | `localhost` | Bind address |
| `SERVER_PORT` | `8074` | HTTP port |
| `ENDPOINT_CORE_INSURANCE_FINANCIALS` | `http://localhost:8080` | Base URL of `core-insurance-financials` |

### Build and test

```bash
mvn clean verify
```

### Run

```bash
ENDPOINT_CORE_INSURANCE_FINANCIALS=http://localhost:8080 mvn spring-boot:run -pl domain-insurance-financials-web
```

Swagger UI (non-production): [http://localhost:8074/swagger-ui.html](http://localhost:8074/swagger-ui.html)

## API

| Method | Path | Behavior |
|--------|------|----------|
| `POST` | `/api/v1/premium-schedules` | Runs `RegisterPremiumScheduleSaga` |
| `GET` | `/api/v1/premium-schedules/{premiumScheduleId}` | Queries the core |
| `PUT` | `/api/v1/premium-schedules/{premiumScheduleId}/status` | Runs `UpdatePremiumScheduleStatusSaga` |
| `PUT` | `/api/v1/premium-schedules/{premiumScheduleId}` | Runs `UpdatePremiumScheduleSaga` |
| `POST` | `/api/v1/premium-schedules/{premiumScheduleId}/cancel` | Cancel premium schedule. Runs `UpdatePremiumScheduleStatusSaga` and sets `CANCELLED`. |
| `POST` | `/api/v1/premium-schedules/{premiumScheduleId}/complete` | Complete premium schedule. Runs `UpdatePremiumScheduleStatusSaga` and sets `COMPLETED`. |
| `DELETE` | `/api/v1/premium-schedules/{premiumScheduleId}` | Deletes the record through `core-insurance-financials` when that core allows removal |

The status request body is `status` plus an optional `statusReason`.

Cancel sets CANCELLED and complete sets COMPLETED through the status saga. The financials core does not restrict those moves or delete, so remove deletes the schedule whenever it exists.
A closing command body is an optional `statusReason`.

