# McWendyQueen - agent rules

Full codebase reference (API, domain, Kafka, error handling, known issues):
`docs/CODEBASE.md`. Read it before planning; this file holds only the rules.

## Stack
- Java 17 (`maven.compiler.release=17` from the Boot parent, so a local JDK 21 still
  rejects post-17 APIs), Spring Boot 3.5.0 servlet stack, Maven wrapper.
- JPA + PostgreSQL (Docker, host port 5434), Spring Kafka (Docker, 9092), springdoc,
  Lombok, ModelMapper. JUnit 5 + Mockito + MockMvc via `spring-boot-starter-test`.
- No CI, formatter, linter, or static analysis in the repo: the loop below is the gate.

## Verify loop
- test:     `.\mvnw.cmd -q test`  (2026-10-22: 89 tests, 0 failures)
- build:    `.\mvnw.cmd -q package -DskipTests`
- behavior: with containers `learning-postgres` and `kafka-learning` up, run
  `.\mvnw.cmd spring-boot:run`, then `curl -s http://localhost:3000/api/v1/menu` ->
  200 with `cheeseburger`, `fries`, `salad`. Stop the process listening on :3000.
- Test output is very noisy. Judge by the Maven exit code (not piped through `tail`) or
  by `target/surefire-reports/*.xml`.
Keep this section current: a change to any command above updates it in the same change.

## Conventions
- Packages: `com.mcwendyqueen.<layer>.<domain>`. A service is an interface plus `<Name>Impl`.
- Constructor injection only. Do not use `this.` on injected collaborators outside the
  constructor.
- Controllers build paths from `ApiConstants` static imports. Do not write literal
  `/api/v1/...` strings in main code.
- Services return `Optional`. When it is empty, the controller throws
  `ResponseStatusException(HttpStatus.NOT_FOUND, "<thing> not found")`.
- Entity <-> DTO mapping goes through static methods in `model/ModelMapperUtils`. Add
  new mappings there.
- Error bodies come from `GlobalExceptionHandler` -> `ExceptionFactory`. Do not
  hand-roll a JSON error body inside the dispatcher.
- `catch`, `finally`, and `else` start on a new line after the closing `}`.
- Logging uses Lombok `@Slf4j`.
- Tests: controllers use `@WebMvcTest` + `@MockBean`; services use plain Mockito. Name
  test methods `method_condition_expectedResult`.

## Reuse map
- Paths: `ApiConstants`. Mapping: `ModelMapperUtils`. Error payloads: `exception/*`.
  Request correlation: `RequestIdFilter` (MDC `requestId`).
- Libraries on the classpath: Spring, Lombok, ModelMapper, Jackson, springdoc. There is
  no resilience, caching, or rate-limit library. Adding one is a dependency decision,
  so do not add one silently.

## Do not
- Assume test config merges with the main config. `src/test/resources/application.yml`
  replaces `src/main/resources/application.yml` for tests, so a property tests need goes
  in the test file.
- Throw from a servlet `Filter` and expect the standard error shape (see CODEBASE.md,
  "Errors and request correlation").
- Reorder imports or reformat code you did not change. Import order varies by file, so
  match the file you are in.
