# Development guide

[← Back to README](../README.md)

## Installation

### Requirements

- Java JDK 17+
- Maven
- Docker & Docker Compose

### Database Setup

The project uses PostgreSQL via Docker.

```bash
# From the project root
docker-compose up -d
```

Default credentials:

- **User:** `Sprint_user`
- **Password:** `Sprint_pass`
- **Database:** `Sprint_DB`
- **Port:** `5433`

## Configuration

Main application properties (development profile):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/Sprint_DB
spring.datasource.username=Sprint_user
spring.datasource.password=Sprint_pass

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

api.security.token.secret=${JWT_SECRET:my-secret-key}
```

### JWT secret

Tokens are signed with HMAC-SHA256 using `api.security.token.secret`, which is read from the
`JWT_SECRET` environment variable. The `my-secret-key` fallback exists **only for local
development**. Anyone who knows the secret can forge tokens for any user, so for any
shared, staging or production environment always set a long random value:

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
```

Changing the secret invalidates all previously issued tokens.

## Running the App

With the database running:

```bash
./mvnw spring-boot:run
```

The application will be available at:

```
http://localhost:8080/index.html
```

## Verification

```bash
./mvnw verify
```
