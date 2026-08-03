# Pixel Backend
Spring Boot backend for Team Pixel portfolio management.

## Tech Stack
- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- MySQL
- Lombok
- springdoc-openapi (Swagger UI)

## Database
- MySQL database: `trading_db`
- Config file: `src/main/resources/application.properties`
- DDL strategy: `spring.jpa.hibernate.ddl-auto=update`

## Environment Variables
- `DB_USERNAME` (default: `root`)
- `DB_PASSWORD` (default: `password`)
- `GROQ_API_KEY` (optional for live AI responses)
- `GROQ_MODEL` (optional)
- `GROQ_BASE_URL` (optional)

## Run
```bash
mvn spring-boot:run
```

## Swagger
- UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
