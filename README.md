# SmartServe

SmartServe is a Spring Boot backend for restaurant menu, ordering, kitchen workflow, and staff-role management.

## Tech Stack

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL
- Maven
- OpenAPI/Swagger

## Local Setup

1. Create a PostgreSQL database named `smartserve`.
2. Copy `.env.example` values into your local environment or shell profile.
3. Set real local values for database and JWT settings.
4. Run the application:

```bash
./mvnw spring-boot:run
```

5. Open Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

## Environment Variables

The application reads sensitive settings from environment variables:

```env
DB_URL=jdbc:postgresql://localhost:5432/smartserve
DB_USERNAME=postgres
DB_PASSWORD=change_me
JWT_SECRET=change_me_to_a_long_random_secret
SMARTSERVE_ADMIN_USERNAME=admin
SMARTSERVE_ADMIN_PASSWORD=change_me
SMARTSERVE_ADMIN_FULL_NAME=System Admin
```

Do not commit real passwords, production database credentials, JWT secrets, or API keys.

## Tests

Run:

```bash
./mvnw test
```

## GitHub Safety Notes

- `.env` files are ignored and should contain real local secrets.
- `.env.example` is committed and contains only placeholder values.
- Build output, logs, and IDE files are ignored.
- Keep the repository private until production secrets, deployment settings, and documentation are fully reviewed.