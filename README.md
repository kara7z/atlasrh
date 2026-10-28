# AtlasRH

Spring Boot application using Java 21, Maven, Spring Web MVC, Spring Data JDBC, and MariaDB.

## Requirements

- Java 21 for running or building locally
- Docker Engine or Docker Desktop with Docker Compose v2
- Internet access for the first dependency and image downloads

The Maven wrapper (`./mvnw`) is included; installing Maven separately is not necessary.
Run the commands below from the project root. On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Run locally

Start Docker, then run only MariaDB and launch the app locally:

```bash
docker compose up -d --wait mariadb
DB_URL=jdbc:mariadb://localhost:3307/atlasrh ./mvnw spring-boot:run
```

Containers are managed manually with `docker compose`; the application does not
start them automatically. Stop the Docker app first with `docker compose stop app`
if it is already running, to avoid a port conflict.

The application listens on `http://localhost:8080`. The `/` endpoint returns placeholder text.
Department CRUD is available at `/api/departments`.
Employee create/update/view endpoints are available at `/api/employees`.

## Build locally

```bash
./mvnw clean package -DskipTests
```

The executable JAR is created at `target/atlasrh-0.0.1-SNAPSHOT.jar`.

To run it against MariaDB manually:

```bash
docker compose up -d --wait mariadb
DB_URL=jdbc:mariadb://localhost:3307/atlasrh java -jar target/atlasrh-0.0.1-SNAPSHOT.jar
```

Start the database separately before running the JAR. The explicit URL is necessary because `compose.yaml` exposes
port **3307**, while the fallback in `application.yaml` uses **3306**.

## Run development in Docker

Java does not need to be installed on the host for this option.

```bash
docker compose up --build -d
docker compose logs -f app
```

Compose uses the `build` stage of `Dockerfile` and runs Maven with DevTools after MariaDB is
healthy. Containers connect using the service name `mariadb` and its
internal port **3306**, not the host port **3307** or `localhost`.

The single `compose.yaml` is development-focused: it uses the Java 21 JDK stage,
mounts your source files, and runs `spring-boot:run`. Tests are skipped during image
builds; run them separately. No override file is needed.

The Dockerfile still provides a separate lightweight, non-root runtime stage for
packaged deployments. `docker build -t atlasrh .` builds that final image; Compose
deliberately uses the development stage instead.

Both the application and database run in Docker. Open `http://localhost:8080`.
Stop any locally running application or old standalone app container first to free port 8080.

## DevTools inside Docker

DevTools is enabled by the single Compose file. Source files are mounted read-only;
compiled files stay inside the container. Java does not need to be installed on
your computer.

```bash
docker compose up --build -d
```

After changing Java code or application resources, compile inside the running container:

```bash
docker compose exec app ./mvnw compile
```

DevTools detects changes in the compiled classpath and restarts Spring Boot.
It does **not** compile source files automatically. No image rebuild is needed for
these changes. Rebuild with the startup command after changing `pom.xml`, the Maven
wrapper, or `Dockerfile`, since those files are copied into the image.

View logs:

```bash
docker compose logs -f app
```

Rebuild the development app after dependency or Dockerfile changes:

```bash
docker compose up --build -d app
```

This replaces the app container without deleting the database volume. Compiled
classes inside the old container are discarded; your source files remain on your
computer and Maven recompiles them on startup.

## Database configuration

Development defaults:

| Setting           | Value               |
| ----------------- | ------------------- |
| Database          | `atlasrh`           |
| Username          | `atlasrh`           |
| Password          | `atlasrh_dev`       |
| Root password     | `root_dev_only`     |
| Host address      | jf `127.0.0.1:3307` |
| Container address | `mariadb:3306`      |

Environment variables supported by the application:

- `DB_URL`: JDBC connection URL
- `DB_USERNAME`: database username
- `DB_PASSWORD`: database password

Compose also accepts `DB_USERNAME`, `DB_PASSWORD`, and `DB_ROOT_PASSWORD`.
If you override credentials, use matching values for the database and application.
MariaDB initialization variables only take effect with a new, empty data volume;
changing them does not update credentials in an existing database.

These passwords are for **local development only**. Use secret management for production.

## Database migrations (Liquibase)

Liquibase runs automatically at application startup using the configured MariaDB
datasource. The master changelog is:

```text
src/main/resources/db/changelog/db.changelog-master.xml
```

The master includes four ordered table migrations. The resulting tables are `departments`,
`employees`, `contracts`, and `leave_requests`, including foreign keys and unique
constraints on department names, employee numbers, and employee emails.
Managers and contract end dates are optional. Foreign keys prevent deleting
records that are still referenced; related HR records are not deleted automatically.

Add new versioned changesets for future schema changes. Spring Data JDBC does not
create, update, or validate the schema automatically; Liquibase manages schema changes.

Map models using Spring Data's `@Table` and `@Id` annotations, with `@Column` when
needed. Repositories can extend `CrudRepository` or `ListCrudRepository`. For
generated IDs, define auto-increment columns in the Liquibase changesets; no JPA
`@Entity` or `@GeneratedValue` annotations are needed.

Liquibase records applied changesets in `DATABASECHANGELOG`. Do not edit changesets
already applied to a shared database; add new changesets instead. Existing tables
are not automatically imported into the changelog and need a reviewed baseline
before adding migrations that would recreate them.

The application uses Spring Boot's managed Liquibase dependency versions. There is
no Maven Liquibase plugin or automatic entity-to-changelog generation configured.

## Tests

Run the mapper, service, controller, and architecture tests without a database:

```bash
./mvnw -Dtest='*Tests,!AtlasrhApplicationTests,!*IntegrationTests' test
```

For the full suite, start MariaDB and supply its host port explicitly:

```bash
docker compose up -d --wait mariadb
DB_URL=jdbc:mariadb://localhost:3307/atlasrh ./mvnw test
```

Tests use the database you started above; they do not start Docker Compose automatically.
The application integration test exercises all four JDBC repositories and DTO services;
its inserted records are rolled back after the test. Liquibase still applies pending
migrations at context startup, so use a development/test database, not production.

## Validation and API errors

Request bodies are validated with Jakarta Bean Validation. Required fields, email format,
positive reference IDs, string lengths, and salary precision are checked before controller
logic runs. Contract and leave DTO constraints are available for their future write endpoints.

`ApiExceptionHandler` handles errors centrally using `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/departments",
  "errors": [
    {"field": "name", "message": "Department name is required"}
  ]
}
```

| Condition | Status |
| --- | --- |
| Successful read/update | `200` |
| Department created | `201`, with a `Location` header |
| Employee created | `201`, with a `Location` header |
| Department deleted | `204` |
| Invalid fields, JSON, parameter types, or business operation | `400` |
| Missing resource | `404` |
| Duplicate department name or deletion with employees | `409` |
| Duplicate employee number or email | `409` |

Business exceptions use `BadRequestException`, `ResourceNotFoundException`, and
`ConflictException`; department-specific exceptions extend these shared types.
Non-validation errors have an empty `errors` array. Parsing errors never include
raw request bodies or internal exception details. Framework errors such as unsupported
methods (`405`) and content types (`415`) retain their status and headers.
Successful responses remain DTOs, not error envelopes.

## Employee records (ATLAS-13)

- `POST /api/employees`: persist an employee; returns `201` and a `Location` header.
- `PUT /api/employees/{id}`: replace an existing record; returns `200`.
- `GET /api/employees/{id}`: view a record; returns `200` or `404`.

POST and PUT accept the same body (IDs are generated by the server and the update
ID comes from the URL):

```json
{
  "employeeNumber": "EMP-001",
  "firstName": "Sara",
  "lastName": "Amrani",
  "email": "sara@example.com",
  "position": "Developer",
  "hireDate": "2026-01-01",
  "departmentId": 1,
  "managerId": null
}
```

The department and optional manager must exist (`404`). Employee numbers and
emails must be unique (`409`); retaining your own values on update is allowed.
An employee cannot be their own manager (`400`). Setting `managerId` to null or
omitting it on PUT clears the manager. Invalid fields return `400`. List pagination,
sorting, and search are reserved for ATLAS-14.

## Layered architecture

All application packages are under `com.example.atlasrh`:

| Package | Responsibility |
| --- | --- |
| `controller` | HTTP handling; depends on service interfaces, never repositories or entities |
| `service` | Public service interfaces; exposes DTOs, not persistence entities |
| `service.impl` | Spring-managed implementations; transaction boundaries, repository access, and DTO conversion |
| `repository` | Spring Data JDBC `ListCrudRepository` interfaces for persistence |
| `entity` | The four JDBC-mapped entities: Department, Employee, Contract, and LeaveRequest |
| `dto` | Immutable Java records exchanged across the service boundary |
| `mapper` | MapStruct entity ↔ DTO mappings and shared configuration |
| `enums` | Contract types, leave types, and request statuses |

Dependencies flow from controller → service interface → service implementation →
repository. Spring injects implementations, repositories, and generated mappers
through constructors. Keeping controllers dependent on interfaces makes them
testable with a mock service, without loading JDBC or a database.

The four HR services provide `findAll()` and `findById(Long)` read operations
returning `List<...Dto>` and `Optional<...Dto>`. Mapping occurs inside the service's
read-only transaction; entities stay behind the service boundary. A missing record
is represented by `Optional.empty()`. Department CRUD and employee create/update/view
also provide HTTP error translation and write-side business rules; contract and leave
write operations belong to subsequent Jira stories.

MapStruct 1.6.3 generates Spring components during Maven compilation. Shared
`MappingConfig` selects the Spring component model, constructor injection, and
compile-time errors for unmapped target properties. Do not edit generated files
under `target/generated-sources/annotations`.

DTOs carry relationship IDs (`departmentId`, `managerId`, `employeeId`), not nested
entities. Reverse mappings deliberately ignore transient navigation properties
and collections, retaining foreign keys without recursively rebuilding a graph.
For example, an employee's manager/team cycle cannot cause recursive JSON output.
Future write services must validate relationship IDs and server-controlled fields
(such as IDs and leave status) before saving; these mappers are not validation.

`ArchitectureTests` guards the layer boundaries. `MapperTests` verifies Spring
mapper discovery, both mapping directions, nullable fields, enums, and cyclic
relationships. `ServiceTests` verifies DTO-only reads and missing/empty results.

## Useful Docker commands

```bash
# Database status and logs
docker compose ps
docker compose logs -f mariadb

# Application logs (Ctrl+C exits log viewing, not the container)
docker compose logs -f app

# Stop or restart the application
docker compose stop app
docker compose start app

# Recompile source changes for DevTools
docker compose exec app ./mvnw compile

# Rebuild after dependency or Dockerfile changes
docker compose up --build -d app

# Stop both services; keep the database data
docker compose down

# Open a database terminal; enter atlasrh_dev when prompted
docker compose exec mariadb mariadb -u atlasrh -p atlasrh
```

Database files persist in the `mariadb_data` named volume. **Adding `-v` to
`docker compose down` deletes that project's database volume and its data.**

## Troubleshooting

- **Docker unavailable:** run `docker info` and make sure the Docker daemon is running.
- **Port 8080 already in use:** stop the local application or map `8081:8080` when running the image.
- **Port 3307 already in use:** change the host port in `compose.yaml` and update manually supplied JDBC URLs.
- **Container name already exists:** stop and remove `atlasrh-app` before recreating it.
- **Database connection refused:** check database health and confirm the URL uses the correct host/container address.
- **Permission denied running the wrapper:** run `chmod +x mvnw`.
