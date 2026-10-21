# AtlasRH

Spring Boot application using Java 21, Maven, Spring Web MVC, Spring Data JPA, and MariaDB.

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
SPRING_DOCKER_COMPOSE_ENABLED=false DB_URL=jdbc:mariadb://localhost:3307/atlasrh ./mvnw spring-boot:run
```

Automatic Compose management is disabled for this command so it does not also start
the containerized application and cause a port conflict. Stop the Docker app first
with `docker compose stop app` if it is already running.

The application listens on `http://localhost:8080`. There is no `/` endpoint yet,
so a 404 at that address does not necessarily mean startup failed. Check the startup logs.

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

Docker Compose support is excluded from the packaged JAR by default, so start the
database separately. The explicit URL is necessary because `compose.yaml` exposes
port **3307**, while the fallback in `application.yaml` uses **3306**.

## Build and run the Docker image

Java does not need to be installed on the host for this option.

```bash
docker compose up --build -d
docker compose logs -f app
```

Compose builds the application using `Dockerfile` and starts it after MariaDB is
healthy. Containers connect using the service name `mariadb` and its
internal port **3306**, not the host port **3307** or `localhost`.

The Dockerfile builds the JAR in a Java 21 JDK stage, then copies only the JAR into
an Alpine Java 21 JRE image. The application runs as a non-root user. Tests are
skipped during image builds; run them separately.

Both the application and database run in Docker. Open `http://localhost:8080`.
Stop any locally running application or old standalone app container first to free port 8080.

## DevTools inside Docker

Use the development override to run with Maven and DevTools instead of the
packaged JAR. Source files are mounted read-only; compiled files stay inside the
container. Java does not need to be installed on your computer.

```bash
docker compose -f compose.yaml -f compose.dev.yaml up --build -d
```

After changing Java code or application resources, compile inside the running container:

```bash
docker compose -f compose.yaml -f compose.dev.yaml exec app ./mvnw -B -ntp -DskipTests compile
```

DevTools detects changes in the compiled classpath and restarts Spring Boot.
It does **not** compile source files automatically. No image rebuild is needed for
these changes. Rebuild with the startup command after changing `pom.xml`, the Maven
wrapper, or `Dockerfile`, since those files are copied into the image.

View logs:

```bash
docker compose -f compose.yaml -f compose.dev.yaml logs -f app
```

Return to the normal packaged application:

```bash
docker compose up --build -d app
```

This replaces the development app container without deleting the database volume.
Development-only class changes inside the old container are discarded; your source
files remain on your computer.

## Database configuration

Development defaults:

| Setting | Value |
| --- | --- |
| Database | `atlasrh` |
| Username | `atlasrh` |
| Password | `atlasrh_dev` |
| Root password | `root_dev_only` |
| Host address | `127.0.0.1:3307` |
| Container address | `mariadb:3306` |

Environment variables supported by the application:

- `DB_URL`: JDBC connection URL
- `DB_USERNAME`: database username
- `DB_PASSWORD`: database password
- `DDL_AUTO`: Hibernate schema mode (default: `update`)

Compose also accepts `DB_USERNAME`, `DB_PASSWORD`, and `DB_ROOT_PASSWORD`.
If you override credentials, use matching values for the database and application.
MariaDB initialization variables only take effect with a new, empty data volume;
changing them does not update credentials in an existing database.

These passwords and automatic schema updates are for **local development only**.
Use secret management and versioned database migrations for production.

## Tests

Start the database and supply its host port explicitly:

```bash
docker compose up -d --wait mariadb
DB_URL=jdbc:mariadb://localhost:3307/atlasrh ./mvnw test
```

Spring Boot Docker Compose support is disabled during tests by default.

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

# Rebuild and recreate the app after code changes
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
