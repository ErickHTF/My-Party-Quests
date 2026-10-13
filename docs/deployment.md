# AWS reference deployment

[← Back to README](../README.md)

> ⚠️ This AWS setup is documented as a reference architecture.
> Infrastructure was decommissioned after validation to avoid costs.

This setup demonstrates:

- A production-like AWS environment
- Secure configuration management
- Separation between local and production concerns

### Environment Overview

| Component | Technology              |
| --------- | ----------------------- |
| OS        | Amazon Linux 2023       |
| Runtime   | Spring Boot (JAR)       |
| Database  | PostgreSQL (Amazon RDS) |
| Profile   | `prod`                  |

### Secrets & Configuration

Sensitive credentials were **not hardcoded**.
Instead, environment variables were injected at runtime via a startup script on the server.

### Production Startup Script (Example)

> This script illustrates how environment variables were injected
> at runtime in a production environment.

```bash
export DB_URL='jdbc:postgresql://<rds-endpoint>:5432/<database>'
export DB_USERNAME='<db-username>'
export DB_PASSWORD='<db-password>'
export JWT_SECRET='<long-random-secret>'

nohup java -Dspring.profiles.active=prod \
           -Dspring.datasource.url=$DB_URL \
           -Dspring.datasource.username=$DB_USERNAME \
           -Dspring.datasource.password=$DB_PASSWORD \
           -jar app.jar > logs.txt 2>&1 &
```

### Architecture at a Glance

Client → EC2 (Spring Boot – prod) → RDS PostgreSQL
