# Personal Finance Manager

This project now uses:

- `Spring Boot` as the backend API
- `Nuxt 3` in [frontend](D:/Tools/Codex/Personal%20Finance%20Manager/frontend) as the frontend app

## Local infrastructure

Start the full stack:

```bash
docker compose up -d
```

Stop services:

```bash
docker compose down
```

If you want to remove persisted data too:

```bash
docker compose down -v
```

## Application env

Copy `.env.example` to `.env`, then adjust values if needed.

Spring Boot defaults in `src/main/resources/application.yml` already match the Docker Compose setup:

- PostgreSQL: `jdbc:postgresql://localhost:5432/pfm`
- Redis: `localhost:6379`
- Backend API: `http://localhost:8080`

Nuxt defaults:

- Frontend app: `http://localhost:3000`
- API base: `http://localhost:8080`

Compose defaults:

- Frontend container: `http://localhost:3000`
- Backend container: `http://localhost:8080`

## Run the backend

After the containers are healthy:

```bash
mvn spring-boot:run
```

## Run the Nuxt frontend

Install dependencies:

```bash
cd frontend
npm install
```

Start the dev server:

```bash
cd frontend
npm run dev
```

If your backend is not on `http://localhost:8080`, set:

```bash
NUXT_PUBLIC_API_BASE=http://your-backend-host:8080
```

The backend is configured to allow CORS from `localhost:3000`, `127.0.0.1:3000`, `localhost:3001`, and `127.0.0.1:3001`.

## Run Everything With Docker Compose

Build and run frontend, backend, PostgreSQL, and Redis together:

```bash
docker compose up -d --build
```

Open:

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`

Useful checks:

```bash
docker compose ps
docker compose logs -f frontend
docker compose logs -f app
```

If you need a different browser-facing API URL for the frontend, set:

```bash
NUXT_PUBLIC_API_BASE=http://localhost:8080
```

## Containerized Backend Only

If you only want the backend stack and prefer to run Nuxt locally:

```bash
docker compose up -d app postgres redis
```
