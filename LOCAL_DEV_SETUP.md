# McWendyQueen Developer Workstation Setup

## Purpose
This document captures the working local developer setup for `McWendyQueen` on a fresh Windows machine so the environment can be recreated later with minimal guesswork.

## Current Local Development Model
- OS: Windows
- IDE: `IntelliJ IDEA Ultimate`
- App runtime: `McWendyQueen` runs from IntelliJ on the Windows host
- Database: `PostgreSQL` in Docker
- Event broker: `Kafka` in Docker
- Kafka inspection tool: `Offset Explorer`
- Data model: disposable learning data is acceptable for now
- Auto-start model: Docker Desktop starts after Windows login, and the local Postgres/Kafka containers restart automatically

## Core Tools

### Installed tools
- `Windows Terminal`
- `PowerShell 7`
- `Git for Windows`
- `GitHub CLI`
- `TortoiseGit`
- `JetBrains Toolbox`
- `IntelliJ IDEA Ultimate`
- `Eclipse Temurin JDK 21`
- `Eclipse Temurin JDK 17`
- `Docker Desktop`
- `DBeaver Community`
- `Offset Explorer`

### Optional later tools
- `pgAdmin`
- `kubectl`
- `Helm`
- `kind`
- `k9s`

## Base Machine Setup

### Windows and Docker prerequisites
- Run Windows Update until fully current.
- Enable virtualization in BIOS/UEFI.
- Install and verify `WSL 2`.
- In Docker Desktop:
  - use the `WSL 2` backend
  - enable Docker Desktop to start automatically on Windows login

### Install commands
Run these from an elevated PowerShell:

```powershell
winget install -e --id Microsoft.WindowsTerminal
winget install -e --id Microsoft.PowerShell
winget install -e --id 7zip.7zip
winget install -e --id Git.Git
winget install -e --id TortoiseGit.TortoiseGit
winget install -e --id GitHub.cli
winget install -e --id Microsoft.VisualStudioCode
winget install -e --id JetBrains.Toolbox
winget install -e --id EclipseAdoptium.Temurin.21.JDK
winget install -e --id EclipseAdoptium.Temurin.17.JDK
winget install -e --id dbeaver.dbeaver
winget install -e --id Docker.DockerDesktop
```

## Git and IntelliJ Setup

### Git
Configure Git identity:

```powershell
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
```

Generate an SSH key and add the full public key line to GitHub:

```powershell
ssh-keygen -t ed25519 -C "you@example.com"
gh auth login
```

Use `SSH` when `gh auth login` asks which protocol to use.

### IntelliJ
- Install `IntelliJ IDEA` from `JetBrains Toolbox`
- Sign in with the JetBrains account that has the Ultimate license
- Confirm IntelliJ sees:
  - `JDK 21`
  - `JDK 17`
- Set the project SDK to `JDK 21`
- Enable annotation processing if required by the project
- Enable Spring plugins:
  - `Spring`
  - `Spring Boot`
  - `Spring Data`

### IntelliJ Git integration
- Open `Settings` -> `Version Control` -> `Git`
- Make sure IntelliJ detects Git successfully
- Use the `Test` button if needed

## Clone the Repository
Choose a local source folder, for example:

```text
C:\dev\src
```

Clone with SSH:

```powershell
git clone <your-mcwendyqueen-ssh-url>
```

Open the project in IntelliJ and allow it to import the Maven model.

## Build Tool Rule
`McWendyQueen` currently syncs with Maven.

General rule for future recreation:
- if the repo has `mvnw` and `mvnw.cmd`, use the Maven wrapper
- if the repo has `gradlew` and `gradlew.bat`, use the Gradle wrapper
- only install global Maven or Gradle if the repository has no wrapper and actually requires a global install

## Local Infra Stack

### Folder
Create this folder for the reusable local stack:

```text
C:\dev\infra\mcwendyqueen-local
```

### Compose file
Create `docker-compose.yml` in that folder with the following contents:

```yaml
services:
  postgres:
    image: postgres:16
    container_name: learning-postgres
    restart: unless-stopped
    environment:
      POSTGRES_PASSWORD: admin
      POSTGRES_USER: postgres
      POSTGRES_DB: postgres
    ports:
      - "5434:5432"

  kafka:
    image: apache/kafka:3.9.2
    container_name: kafka-learning
    restart: unless-stopped
    ports:
      - "9092:9092"
    environment:
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_NODE_ID: 1
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
```

### Start the stack

```powershell
cd C:\dev\infra\mcwendyqueen-local
docker compose up -d
docker ps
```

Expected running containers:
- `learning-postgres`
- `kafka-learning`

### Auto-start behavior
- Docker Desktop starts automatically after Windows login
- both containers use `restart: unless-stopped`
- after login, the containers should restart automatically once Docker Desktop is ready

## Local App Configuration

### PostgreSQL settings
Use these Spring datasource values for the host-run app:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5434/postgres?sslmode=disable
    username: postgres
    password: admin
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

Notes:
- `localhost` is correct because the app runs on the Windows host
- host port `5434` maps to container port `5432`
- these settings are for local learning only

### Kafka settings
Use this for the host-run app:

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BROKER_HOST:localhost}:${KAFKA_BROKER_PORT:9092}
```

### Kafka topic property binding
Expected property shape:

```yaml
kafka-topics:
  prod:
    enabled: true
    orders-topic: order_events_topic
```

Important note:
- `orders-topic` binds to `ordersTopic`
- `orders:` will not bind correctly

## IntelliJ Database Setup
In IntelliJ Ultimate, add a PostgreSQL data source with:
- host: `localhost`
- port: `5434`
- database: `postgres`
- user: `postgres`
- password: `admin`

If IntelliJ prompts for the PostgreSQL JDBC driver, allow it to download the driver.

## Offset Explorer Setup
Use a connection with:
- bootstrap server: `localhost:9092`
- security: `PLAINTEXT`
- no SASL
- no SSL
- no JAAS properties

Expected topics may include:
- app topics such as `order_events_topic`
- Kafka internal topics such as `__consumer_offsets`

## Validation Checklist

### End-to-end app validation
1. Start the local stack with `docker compose up -d`
2. Start `McWendyQueen` from IntelliJ
3. Verify the app starts successfully, for example on `localhost:3000`
4. Use the public API or app flow to create an order
5. Verify PostgreSQL:
   - rows exist in expected tables
6. Verify Kafka:
   - a message appears in the expected topic in `Offset Explorer`

### Post-restart validation
1. Stop the Spring Boot app in IntelliJ
2. Do not run `docker compose down`
3. Reboot the machine or sign out/sign back in
4. Wait for Docker Desktop to start
5. Run:

```powershell
docker ps
```

6. Confirm `learning-postgres` and `kafka-learning` are running
7. Start `McWendyQueen` again
8. Create another order
9. Confirm:
   - existing DB data is still visible
   - new DB data is written
   - Kafka is still reachable
   - new Kafka messages appear

## Known Notes and Caveats

### Current architecture limitation
`McWendyQueen` currently runs on the host from IntelliJ. When it is later containerized:
- `localhost` values will need to change to service/container names
- Kafka will likely need a dual-listener configuration

### Kafka topic naming bug
A known bug was found where the Kafka listener and producer/topic creation were using different topic names:
- one path referenced `order_events_topic`
- another path referenced `order-events-topic`

That should be fixed later by making the topic name come from one shared configuration source.

### Data durability
This is still a disposable learning environment.

Current expectations:
- data usually survives reboot/login because the same containers restart
- data may be lost if containers are removed and recreated
- if stronger persistence is needed later, add named Docker volumes for Postgres and Kafka

## Recommended Future Improvements
- add named volumes for stronger persistence across container recreation
- add `Kafka UI` later if a browser-based Kafka view is useful
- fix the Kafka topic naming bug so listener, producer, and topic creation all use the same configured topic name
- eventually containerize `McWendyQueen` itself
