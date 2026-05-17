# Jira Clone V1

Minimaler Spring Boot 4 / PostgreSQL / Plain HTML5-ES6-CSS3 Jira-Klon.

## Start

```bash
docker compose up -d
mvn spring-boot:run
```

UI: http://localhost:8080/

## API

- POST /api/users
- POST /api/session
- GET /api/session
- DELETE /api/session
- POST /api/issues
- GET /api/issues?sort=id|author|priority&direction=asc|desc
- GET /api/issues/{id}
- PUT /api/issues/{id}
