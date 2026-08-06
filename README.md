# MindIO Backend API

Spring Boot backend for MindIO, a personal workspace for turning input into output.

## Tech Stack

- Spring Boot 3.2
- Java 17
- Spring Security + JWT
- Spring Data JPA / Hibernate
- MySQL for server deployments
- Maven

## Development

```powershell
.\mvnw.cmd spring-boot:run
```

The API is served at:

```text
http://localhost:8080/api
```

OpenAPI documentation:

```text
http://localhost:8080/api/swagger-ui.html
```

The default development database is MySQL `mindio_app`:

```text
jdbc:mysql://localhost:3306/mindio_app
```

Override it with `SPRING_DATASOURCE_URL`, `DB_USERNAME`, and `DB_PASSWORD` when needed. The older `worknotes` database name is reserved for the personal branch/database.

## Naming

Some internal package names and configuration keys still use `worknotes` for compatibility. New product deployments should use the `mindio_app` database, while the older `worknotes` database name is reserved for personal/legacy use. User-facing product content should use `MindIO`.
