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
Department POST and PUT accept JSON, multipart form-data, or URL-encoded forms.
In Postman, choose **Body → form-data** and add text fields `name` (required) and
`description` (optional). Let Postman generate the Content-Type and multipart boundary.
For example: `curl -F 'name=Engineering' -F 'description=Development team' http://localhost:8080/api/departments`.
Employee create/update/view endpoints are available at `/api/employees`.
All current POST routes (`/api/departments` and `/api/employees`) support JSON,
multipart form-data, and URL-encoded forms. For employee form-data, use text fields
`employeeNumber`, `firstName`, `lastName`, `email`, `position`, `hireDate`
(`YYYY-MM-DD`), and `departmentId`. `managerId` is optional; omit it for no manager.
Contract creation (`POST /api/employees/{id}/contracts`) also supports JSON,
multipart form-data, and URL-encoded forms (see the contract section below).
Employee PUT also accepts multipart form-data and URL-encoded forms using the same
fields as POST. Send every required field on PUT; omit `managerId` to clear it
(do not send the literal text `null`). GET uses query parameters and DELETE uses
the resource ID in the URL; neither requires a form body.

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

No Compose watch process is needed: `./src` is bind-mounted directly into
`/workspace/src`. If switching from the previous watch-based setup, apply the
mount once with `docker compose up -d --force-recreate app`.

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

The master includes four initial table migrations, a contract-ending state migration,
and the leave-type allowance migration. The resulting tables are `departments`,
`employees`, `contracts`, `leave_requests`, and `leave_types`, including foreign keys and unique
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
omitting it on PUT clears the manager. Invalid fields return `400`.

## Employee list (ATLAS-14)

`GET /api/employees?page=0&size=20&sort=lastName,asc&name=sara&departmentId=1`
returns a paginated list using the existing MapStruct employee DTOs (no nested entities).

- `page`: zero-based, defaults to `0`.
- `size`: `1`–`100`, defaults to `20`.
- `sort`: employee field and optional `asc`/`desc`; defaults to `id,asc`.
  Repeat it for multiple fields, e.g. `sort=lastName,asc&sort=firstName,desc`.
  Allowed fields: `id`, `employeeNumber`, `firstName`, `lastName`, `email`,
  `position`, `hireDate`, `departmentId`, `managerId`. An ID tie-breaker is added
  when not explicitly sorted by ID, ensuring stable page boundaries.
- `name`: optional, trimmed, case-insensitive substring of first **or** last name;
  blank means no name filter. `%` and `_` are literal characters, not wildcards.
- `departmentId`: optional positive department ID; combined with `name` using AND.
  Unknown department IDs yield an empty page, not `404`.

The response has a stable JSON shape:

```json
{"content":[],"number":0,"size":20,"totalElements":0,"totalPages":0,"first":true,"last":true}
```

`content` contains employee records when matches exist. Out-of-range pages return
empty content but retain the matching total. Invalid page/size/filter values or
unsupported sort fields return `400`. Filtering, sorting, counting, and pagination
run in the database through Spring Data JDBC, rather than loading all employees.

## Contract creation (ATLAS-16)

`POST /api/employees/{id}/contracts` creates a contract for an existing employee
and returns `201` with the saved contract DTO and a `Location` header.

JSON example (replace `1` in the URL with an existing employee ID):

```json
{
  "type": "FIXED_TERM",
  "startDate": "2026-11-01",
  "endDate": "2027-10-31",
  "grossSalary": 12000.00
}
```

In Postman, you can instead use **Body → form-data**, with text fields `type`,
`startDate`, `endDate`, and `grossSalary`. Types are `PERMANENT`, `FIXED_TERM`,
or `INTERNSHIP` (uppercase). Dates use `YYYY-MM-DD`. For a permanent contract,
omit `endDate` for an open-ended contract; do not send the text `null`.

- Fixed-term and internship contracts require an end date (`400`).
- End date must be on or after start date (`400`). Same-day contracts are allowed.
- Salary is required, nonnegative, and has at most two decimal places (`400`).
- A missing employee returns `404`.
- Date intervals are inclusive: contracts for one employee must not overlap (`409`).
  This enforces one active contract at any date, including scheduled contracts.
  An open-ended contract blocks later overlapping contracts. A replacement can
  start the day after the previous contract ends.
- Employee-row locking serializes creation for the same employee so simultaneous
  requests cannot bypass the overlap check. All creation paths use the same transaction.

The Location identifies the created record; a single-contract GET endpoint is not
yet exposed. View the employee's full contract history with the route below.

## Contract ending and history (ATLAS-17)

- `GET /api/employees/{id}/contracts`: returns `200` with all contracts, newest
  start date first (ID descending breaks ties). Existing employees without contracts
  return `[]`; missing employees return `404`.
- `PUT /api/employees/{id}/contracts/{contractId}/end`: ends an active contract and
  returns `200` with the updated record, including `status: "ENDED"`.

For PUT, send JSON:

```json
{"endDate":"2026-10-30"}
```

Or use form-data / URL-encoded form with the text field `endDate` in `YYYY-MM-DD`.
The end date must be between the contract's start date and today (UTC); it cannot
extend the original end date. Invalid dates return `400`. Missing employees,
contracts, or contracts belonging to another employee return `404`. Ending an
already-ended, naturally expired, or not-yet-started contract returns `409`.
Repeated or concurrent ending requests cannot silently overwrite the first decision.

History and end responses expose contract fields plus a status:

- `SCHEDULED`: start date is in the future.
- `ACTIVE`: started, not explicitly ended, and not past its inclusive end date.
- `ENDED`: explicitly ended or its end date is before today.

An explicit ending is persisted in the new `contracts.ended` column so ending
today immediately returns `ENDED`. Existing rows default to false; expired records
are still recognized by their dates. Creation responses keep their existing DTO shape.
The original inclusive overlap rules still apply: a replacement starts no earlier
than the day after the previous contract's final end date.

## Manager team (ATLAS-15)

`GET /api/managers/{id}/team` returns `200` with an array of employee DTOs for
the manager's **direct reports** (`managerId` equals the requested ID). Reports
can belong to different departments; indirect reports and unrelated employees
are not included. No ordering is guaranteed.

An existing employee with no direct reports returns `[]`. A missing manager
returns `404`; a nonpositive or malformed ID returns `400`. Responses contain
relationship IDs, not nested manager/team objects. Reassigning or clearing an
employee's manager is reflected in subsequent team requests.

The repository uses Spring Data JDBC's derived `findByManagerId` query, not JPQL
or JPA navigation properties.

## Manager leave decisions (ATLAS-18)

- `GET /api/managers/{managerId}/leave-requests/pending`: pending leave requests
  from direct reports only, ordered by start date then ID (`200`). Indirect reports,
  unrelated employees, and the manager's own requests are excluded. A missing
  manager returns `404`; an existing employee without pending team requests gets `[]`.
- `PUT /api/leave-requests/{id}/decision`: approve or reject a direct report's
  pending request (`200`). JSON example:

```json
{"managerId":1,"decision":"REJECTED","comment":"Insufficient team coverage"}
```

PUT also accepts form-data / URL-encoded text fields `managerId`, `decision`, and
`comment`. `decision` must be `APPROVED` or `REJECTED`; rejection requires a nonblank
comment (`400`). Approval comments are optional. Comments are trimmed before saving.
Already processed or cancelled requests return `409`; simultaneous decisions
cannot overwrite each other. Missing managers, requests, or requests outside the
manager's direct team return `404`. Invalid IDs and malformed bodies return `400`.

**Development limitation:** authentication is not implemented. `managerId` identifies
the acting manager for team checks but is caller-supplied, not proof of identity.
This is not a production authorization boundary. A future authenticated API should
derive manager identity from the session/token.

Leave requests can be created through the ATLAS-19 endpoint below. Approval rechecks
the yearly balance under an employee lock: separate pending requests can each fit
the balance when submitted, but cannot both be approved if their combined days
exceed it. An insufficient balance at approval returns `400` and leaves the request
PENDING. Integration tests cover filtering, decisions and concurrent calls.

## Leave creation (ATLAS-19)

`POST /api/employees/{employeeId}/leave-requests` accepts JSON, multipart form-data,
or URL-encoded forms. Example JSON:

```json
{
  "leaveType": "ANNUAL",
  "startDate": "2026-11-06",
  "endDate": "2026-11-09",
  "reason": "Family holiday"
}
```

Use those same four text fields in Postman form-data; dates use `YYYY-MM-DD`.
The employee comes from the URL. ID, status, comment and working days are
server-controlled. The response is `201` with a Location header and a DTO:

```json
{
  "id": 3,
  "employeeId": 1,
  "leaveType": "ANNUAL",
  "startDate": "2026-11-06",
  "endDate": "2026-11-09",
  "reason": "Family holiday",
  "status": "PENDING",
  "comment": null,
  "workingDays": 2
}
```

Allowances confirmed for this project are **22 ANNUAL days** and **5 EXCEPTIONAL
days per calendar year**. Migration 006 seeds these in the `leave_types` entity
table; requests retain the `ANNUAL`/`EXCEPTIONAL` API codes backed by a foreign key.
These are project policy values, not statutory entitlements.

- Working days are Monday–Friday, inclusive. Weekends are excluded; public holidays
  are not separately excluded. Weekend-only requests return `400`.
- End date before start, blank reason, invalid type/date, overlapping dates, or
  exceeding the remaining balance return `400`. Missing employees return `404`.
- Following the brief's unqualified no-overlap rule, overlap is checked against
  **all existing requests**, including rejected/cancelled requests, across leave
  types. Date boundaries are inclusive. Other employees' requests do not conflict.
- Remaining balance = the type's yearly allowance minus approved working days
  within that calendar year. Pending/rejected/cancelled requests do not consume it.
- Cross-year requests split both requested and approved days at January 1; each
  year's portion must fit that year's allowance. No carry-over or prorating is applied.
- Employee locking prevents concurrent overlapping submissions, and approval
  revalidation prevents concurrent decisions from overspending an allowance.

The brief does not require authentication. Employee IDs in routes are development
identifiers, not proof of caller identity. The balance display endpoint is described
under ATLAS-21 below. Location identifies
the created request; a single-request GET is not yet exposed.

## Leave tracking and cancellation (ATLAS-20)

`GET /api/employees/{employeeId}/leave-requests` lists only that employee's requests.
Add `status=PENDING`, `APPROVED`, `REJECTED`, or `CANCELLED` to filter. For example:

```text
GET /api/employees/1/leave-requests?status=PENDING&page=0&size=10
```

Results use the same `content`, `number`, `size`, `totalElements`, `totalPages`,
`first`, and `last` pagination envelope as the employee list. Each content item is
a MapStruct leave-request DTO, not an entity. Defaults are page `0`, size `10`,
start date descending then ID descending. Size must be 1–100. Optional `sort`
supports `id`, `leaveType`, `startDate`, `endDate`, or `status`, with an ID tie-breaker
added when needed. Unknown status/sort fields and invalid pagination return `400`.
Missing employees return `404`; existing employees without matches get empty
content. Out-of-range pages preserve the matching total.

`PUT /api/employees/{employeeId}/leave-requests/{id}/cancel` cancels an employee's
own PENDING request, returning `200` and the saved DTO with `status: "CANCELLED"`.
The route IDs provide all required data, so no JSON or form fields are needed;
an empty form-data request is also accepted.

- Missing employees/requests and another employee's request return `404`.
- APPROVED, REJECTED, and already CANCELLED requests return `409`.
- Request dates, reason, type and comment are preserved; only status changes.
- Cancelled requests disappear from the manager's pending list. They do not consume
  balance. The existing no-overlap rule still includes cancelled request dates.
- Row locking serializes cancellation with manager approval/rejection and repeated
  cancellation: only one concurrent transition succeeds.

There is no authentication in this brief: the employee ID is a development actor
identifier. The ownership check prevents cancelling a request with a mismatched
employee ID, but is not a substitute for authenticated identity.

## Leave balance and seniority (ATLAS-21)

`GET /api/employees/{employeeId}/leave-balance` returns the current UTC calendar
year's allowance, approved usage, and remaining days for each leave type, plus
seniority since hire. Optionally select a year:

```text
GET /api/employees/1/leave-balance?year=2026
```

Example response (values depend on the employee's actual records):

```json
{
  "employeeId": 1,
  "year": 2026,
  "asOf": "2026-10-30",
  "hireDate": "2020-08-15",
  "seniority": {"years": 6, "months": 2},
  "balances": [
    {"leaveType": "ANNUAL", "daysPerYear": 22, "approvedDays": 7, "remainingDays": 15},
    {"leaveType": "EXCEPTIONAL", "daysPerYear": 5, "approvedDays": 0, "remainingDays": 5}
  ]
}
```

- Balance is calculated, never entered manually: configured yearly allowance minus
  approved Monday–Friday days within the selected year, inclusive.
- Cross-year approved requests are clipped to each calendar year. Pending,
  rejected, and cancelled requests do not consume days; other employees' requests
  and other leave types do not affect a type's balance.
- All approved days in the selected year count, including future approved leave.
  No carry-over or prorating is applied. Allowances come from the current
  `leave_types` configuration, not historical policy versions.
- Seniority uses `Period.between(hireDate, today)` in UTC, showing completed years
  and months and omitting leftover days. It is always as of `asOf`, even when a
  historical year is selected. Future hire dates yield zero years/months.
- No requests means the full allowance. Legacy/imported overdraw is shown as a
  negative remaining balance rather than silently hidden.
- Missing employee returns `404`; invalid IDs or a year outside 1000–9999 return
  `400`. This GET endpoint uses query parameters, not a form body.

## Layered architecture

All application packages are under `com.example.atlasrh`:

| Package | Responsibility |
| --- | --- |
| `controller` | HTTP handling; depends on service interfaces, never repositories or entities |
| `service` | Public service interfaces; exposes DTOs, not persistence entities |
| `service.impl` | Spring-managed implementations; transaction boundaries, repository access, and DTO conversion |
| `repository` | Spring Data JDBC `ListCrudRepository` interfaces for persistence |
| `entity` | Five JDBC-mapped entities: Department, Employee, Contract, LeaveType, and LeaveRequest |
| `dto` | Immutable Java records exchanged across the service boundary |
| `mapper` | MapStruct entity ↔ DTO mappings and shared configuration |
| `enums` | Contract types/statuses, stable leave-type API codes, and request statuses |

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
