# McWendyQueen - codebase reference

Snapshot of `master` at `6dd5007` (2026-10-16). A working reference for planning and
for priming agents; it describes what the code does today, including the rough edges.

## What it is

A Spring Boot learning/portfolio app modelling a fast-food restaurant: menu items,
condiments, recipes (menu item -> condiment links), and orders. Creating an order
publishes an event to Kafka, and a listener consumes it. There is no UI in use
(Thymeleaf is on the classpath but has no templates) and no authentication.

| Concern | Choice |
|---|---|
| Language / build | Java 17, Maven wrapper (`mvnw`, `mvnw.cmd`) |
| Framework | Spring Boot 3.5.0 (servlet stack, `spring-boot-starter-web`) |
| Persistence | Spring Data JPA, PostgreSQL 16 in Docker on host port 5434; H2 on the classpath |
| Messaging | Spring Kafka, Apache Kafka 3.9.2 (KRaft) in Docker on 9092 |
| API docs | springdoc-openapi 2.8.14: `/openapi`, `/swagger-ui.html`, gated by `app.debug-features-enabled` |
| Mapping | ModelMapper 3.2.0 (static instance in `ModelMapperUtils`) |
| Boilerplate | Lombok |
| Port | `3000` |

Local infra is not in the repo. `LOCAL_DEV_SETUP.md` holds the `docker-compose.yml` to
create under `C:\dev\infra\mcwendyqueen-local`.

## Layout

```
src/main/java/com/mcwendyqueen/
  ApiConstants.java            path constants: /api, /v1, /menu, /orders, /admin, /kafka ...
  McWendyQueenApplication.java @SpringBootApplication
  DataLoader.java              empty CommandLineRunner
  configuration/OpenApiConfig  OpenAPI metadata
  controller/                  5 REST controllers
  exception/                   global error handling + RequestIdFilter
  kafka/                       producer/consumer config, producer, placeholder consumer, study notes
  model/<domain>/              JPA entities, request/response DTOs, repositories
  service/<domain>/            interface + Impl per domain; eligibility client; order scheduler
src/main/resources/            application.yml, logback-spring.xml
src/test/java/                 @WebMvcTest controller tests + Mockito service tests
src/test/resources/            application.yml (replaces the main one for tests - see Known issues)
```

Package convention: `com.mcwendyqueen.<layer>.<domain>`. Constructor injection, `@Slf4j`,
`Optional` returns from services, and `ResponseStatusException(404, "... not found")` in
controllers when the `Optional` is empty.

## REST API

All under `/api/v1`. IDs are validated `@Positive`, names `@NotBlank`, names max 25 chars.

| Resource | Endpoints |
|---|---|
| Menu `/menu` | `GET /menu`, `GET /menu/{id}`, `GET /menu/name/{name}`, `POST /menu` (only `cheeseburger`, `fries`, `salad` accepted), `DELETE /menu` (body), `DELETE /menu/{id}`, `DELETE /menu/name/{name}` |
| Condiments `/condiment` | same shape as Menu; any name accepted on create |
| Recipes `/recipe` | `GET /recipe`, `GET /recipe/{id}`, `GET /recipe/name/{menu}/{condiment}`, `POST`, `DELETE` (body / id / names) |
| Orders `/orders` | `GET`, `GET /{id}`, `GET /name/{name}`, `POST` (optional `Idempotency-Key` header), `DELETE /{id}`, `DELETE /name/{name}`, `DELETE /all` |
| Order contents | `PATCH`/`DELETE /orders/condiments/{id}` and `/orders/condiments/name/{name}`; `PATCH`/`DELETE /orders/menu-item/{id}` and `/orders/menu-item/name/{name}` |
| Kafka admin `/admin/kafka` | `GET /admin/kafka/listeners`, `POST /admin/kafka/listeners/pause/{listenerId}`, `POST .../resume/{listenerId}` |

Adding a menu item to an order also adds that item's recipe condiments. Removing the
menu item clears the base item and all condiments.

## Domain and data

| Entity | Table | Notes |
|---|---|---|
| `MenuItem` | `menuitems` | unique `name`; one-to-one `MenuItemDuration` (`menuItemDuration`, `processingTime` ms, hard-coded 5000/2500/3000) |
| `CondimentItem` | `condiments` | unique `name`; seeded: lettuce, pickles, tomatoes, cheese, mustard, ketchup, onions |
| `RecipeItem` | `recipes` | `(menu_id, condiment_id)` unique, raw ids not relations; seeded for cheeseburger and salad |
| `Order` | `orders` | `name`, many-to-one `baseMenuItem`, many-to-many `condiments` (`order_condiments`), `createdTime`, `completedTime`, `orderStatus` |

Seed data is created in the **service constructors** (`addAllMenuItems`, `addAllCondiments`,
`addAllRecipeItems`), not in `DataLoader`. Schema is `ddl-auto: update`.

`OrderStatusEnum`: CREATED, PENDING, QUEUED, IN_PROCESS, FAILED, CANCELED, COMPLETED.
Only CREATED is ever set today.

## Order creation flow

`POST /api/v1/orders` -> `OrdersController.createOrder` -> `OrderServiceImpl.createOrder(dto, key)`:

1. `ResiliantOrderPolicyEligibilityClient.checkEligibility` wraps `OrderPolicyEligibilityClient`,
   which **approves at random 50% of the time**. On DENIED it retries with
   `Thread.sleep` backoff, and after 3 consecutive failures it returns MANUAL_REVIEW.
2. APPROVED -> `synchronized createOrder(name, key)`: if an `Idempotency-Key` is given,
   an in-memory `ConcurrentHashMap<String, IdempotencyOrder>` replays a COMPLETED order,
   returns 409 for IN_PROGRESS, and 400 when the same key is reused with a different name.
3. Save, then `sendCreatedOrder` publishes `OrderEventDTO` (envelope `BaseEventDTO<T>`:
   `eventId`, `eventType="order"`, `timestamp`, `source="/services/orders-service"`,
   payload `{id, name, orderStatus}`) to `kafka-topics.prod.orders-topic` if enabled.
4. MANUAL_REVIEW -> `fallback` throws `HttpClientErrorException(503)`.

## Kafka

- Topic: `order-events-topic`, created by the `NewTopic` bean in `KafkaConfig` (2 partitions, 1 replica).
- Producer: `KafkaProducerConfig` builds its own `ProducerFactory` with String/Json serializers.
- **Two listeners consume the same topic**:
  - `OrderScheduler.listen` - id `ordersListener`, the default container factory, group
    `order-event-group` (from yml). It looks up the order and logs it.
  - `OrderEventConsumer.onOrderEvent` - the custom `orderEventKafkaListenerContainerFactory`
    with a **hard-coded** `localhost:9092`, group `mcwendyqueen-order-consumers`. A placeholder log.
- `KafkaStudyLab`, `OrderEventConsumer`, and `KafkaConsumerConfig` carry TODO lists:
  manual ack, DLT, keying by orderId, outbox, and consumer idempotency.
- The admin controller exposes `KafkaListenerEndpointRegistry` for pause and resume.

## Errors and request correlation

- `RequestIdFilter` (a `@Component` servlet `Filter`) puts a random UUID in MDC `requestId`
  and clears MDC afterwards. The id is **not** returned as a response header.
- `GlobalExceptionHandler` (`@RestControllerAdvice`, handles `Exception`) ->
  `ExceptionFactory` picks a payload class:
  `SpringBindingExceptionMessage` (validation), `BindExceptionMessage`, `SpringHttpErrorMessage`
  (anything implementing `ErrorResponse`, e.g. `ResponseStatusException`), `NonHttpExceptionMessage`
  (maps ValidationException -> 400, DataIntegrity/OptimisticLock -> 409, EmptyResult -> 404, else 500).
- Response shape: `{status, code, type, message, timeStamp, requestId, errors}`.
  5xx errors go to the `ExceptionLogger` logger (`logs/exceptions.log`, with stack trace). Lower codes get a `warn` without `errors`.
- Note for any new filter: an exception thrown from a servlet `Filter` is raised outside
  `DispatcherServlet`, so `@RestControllerAdvice` does not handle it. Anything that must
  return this error shape has to run inside the dispatcher (`HandlerInterceptor`) or write the
  response itself.

## Tests and verify loop

- Controller tests: `@WebMvcTest` + `MockMvc` + `@MockBean` services (`@Component` filters such as
  `RequestIdFilter` are part of the slice). Service tests: Mockito.
- Verify loop: `.\mvnw.cmd test` (unit), `.\mvnw.cmd -q package` (build). The repo configures
  no formatter, linter, or static analysis.
- Running the app needs Postgres + Kafka up; `@WebMvcTest` tests need neither.

## Known issues and rough edges

Line numbers are as of `6dd5007`.

| # | Where | Issue |
|---|---|---|
| 1 | `OrderServiceImpl.java:91-100`, `OrdersController.java:106` | DENIED returns `null`; the controller then maps `null`, which surfaces as a 500. Order creation is **non-deterministic** (random eligibility) and can block the request thread in `Thread.sleep`. |
| 2 | `ResiliantOrderPolicyEligibilityClient.java:25-27` | `failedCount` and the `LinkedHashMap` are shared mutable singleton state with no synchronization. |
| 3 | `application.yml:51-52` | producer `key-serializer`/`value-serializer` are set to *Deserializer* classes (masked because `KafkaProducerConfig` builds its own factory). |
| 4 | `KafkaConsumerConfig.java:38` | bootstrap servers hard-coded instead of read from `spring.kafka.bootstrap-servers`. |
| 5 | `OrderScheduler` + `OrderEventConsumer` | two listeners in two groups on one topic, so every event is processed twice. |
| 6 | `OrderServiceImpl.java:51` | idempotency store is in-memory and unbounded, and lost on restart. |
| 7 | `src/test/resources/application.yml` | **Fixed 2026-10-21.** It sat in `src/test/java/resources`, off the test classpath, with a leftover logger from another project. Now it loads, and it **replaces** the main `application.yml` for tests rather than merging with it. |
| 8 | `LOCAL_DEV_SETUP.md` "Kafka topic naming bug" | doc says the topic name should come from one config source; code now uses `kafka-topics.prod.orders-topic` in all three places, so the doc note looks stale. |
| 9 | Services | seed data inserted from constructors; the condiment create path returns the existing row silently (`// need to throw some problem or log`). |

## Data classification

Personal learning project, no markings. Scan on 2026-10-16 found no marking, SSN, or
cardholder hits (the card-shaped numbers in `System Design.excalidraw` are `updated`
timestamps). The datasource passwords in `application.yml`, the test `application.yml`, and
`LOCAL_DEV_SETUP.md` are local Docker/H2 defaults, documented on purpose.

## Landing zone for request rate limiting

Facts that a rate-limiting feature has to work with:

- Every API path shares the prefix `/api/v1`. Admin endpoints sit under `/api/v1/admin`;
  docs are at `/openapi` and `/swagger-ui.html`.
- There is no client identity (no auth), so the only client key available is the remote address
  or a caller-supplied header.
- The existing cross-cutting precedent is `RequestIdFilter`. For the error-shape constraint, see "Errors and request correlation".
- `GET /api/v1/menu` is deterministic and fast, which makes it a good target for showing throttling;
  `POST /api/v1/orders` is not (issue 1).
- Single instance, no Redis in the stack today. A distributed limiter would mean new infrastructure.
- `ResiliantOrderPolicyEligibilityClient` comments mention Resilience4j as the "production"
  choice. No resilience library is on the classpath.
