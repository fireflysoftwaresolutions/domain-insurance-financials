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

Domain layer microservice for premium schedule records within the Firefly platform. This service orchestrates create, status change, and query against `core-insurance-financials` with compensating saga steps.

Repository: [https://github.com/fireflysoftwaresolutions/domain-insurance-financials](https://github.com/fireflysoftwaresolutions/domain-insurance-financials)

---

## Overview

`domain-insurance-financials` sits between experience services and `core-insurance-financials`. Write operations run through the FireflyFramework saga engine. Queries use the CQRS query bus. The domain depends on the core SDK only. It does not import another core's internal packages. Identifiers that point at parties, contracts, products, or policies stay UUIDs owned by those cores.

It records premium schedule status through the financials core. It does not disburse indemnity, refund premium, post commission, or certify IFRS 17 or Solvency II.

### Key features

- **Create** -- register a premium schedule through `core-insurance-financials`
- **Status change** -- read the current record and update its status through the core This core keeps status on the record itself, so the saga updates that field through the core and does not call a second service.
- **Query** -- read one premium schedule by identifier
- **Compensation** -- a failed later step asks the core to undo the work already accepted
- **CQRS** -- commands and queries travel on separate buses
- **Reactive I/O** -- Spring WebFlux with virtual threads enabled

## Architecture

### Module structure

| Module | Purpose |
|--------|---------|
| `domain-insurance-financials-core` | Saga workflows, commands, handlers, and the application service |
| `domain-insurance-financials-interfaces` | Request contract for a status change |
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
| `UpdatePremiumScheduleStatusSaga` | Reads the current record, then updates its status through `updatePremiumSchedule` | Writes the previous status back through the core |
| Query | `getPremiumScheduleById` on `PremiumSchedulesApi` | None. A read has nothing to undo. |

Status values accepted by the core: `ACTIVE`, `COMPLETED`, `CANCELLED`.

## Setup

### Prerequisites

- Java 25
- Apache Maven 3.9+
- `core-insurance-financials-sdk` 26.10.3
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

The status request body is `status` plus an optional `statusReason`.
