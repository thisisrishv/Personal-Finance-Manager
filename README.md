# Personal Finance Manager API

Spring Boot 3 / Java 17 implementation of the Syfe backend intern assignment. The API uses session-cookie authentication, H2 persistence, per-user data isolation, and layered controller/service/repository code.

## Run Locally

```bash
mvn clean test
mvn spring-boot:run
```

The API starts at:

```text
http://localhost:8080/api
```

For compatibility with the provided shell test script, endpoints are also available without the `/api` prefix.

Protected endpoints require the `JSESSIONID` cookie returned by `POST /api/auth/login`.

## Deployment

This repository includes `render.yaml` and a `Dockerfile` for Render.

Important: an existing Render service created as a Node app cannot be fixed by redeploying the same service. Create a new Render Blueprint or Web Service configured for Docker runtime.

Recommended path:

1. Push the repo to GitHub.
2. In Render, create a new Blueprint from the repo so it reads `render.yaml`.
3. Confirm the service runtime is Docker.

If creating the service manually, choose Docker as the runtime and leave Build Command and Start Command blank. Do not choose Node, because Render will try `yarn start` and fail with `Couldn't find a package.json`.

The app reads `PORT` automatically and falls back to `8080` for local runs. The Docker image exposes `8080`, which matches the fallback port used by Spring Boot.

## Test Script

After deploying, run the assignment script against the deployed base URL:

```bash
bash financial_manager_tests.sh https://your-render-service.onrender.com/api
```

I did not find `financial_manager_tests.sh` in the provided workspace or Downloads folder, so the repository includes integration tests that exercise the same public API contract with MockMvc. Use the official script when you receive or download it.

## API Summary

### Auth

```http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
```

Register body:

```json
{
  "username": "user@example.com",
  "password": "password123",
  "fullName": "John Doe",
  "phoneNumber": "+1234567890"
}
```

Login body:

```json
{
  "username": "user@example.com",
  "password": "password123"
}
```

### Categories

```http
GET /api/categories
POST /api/categories
DELETE /api/categories/{name}
```

Default categories are seeded on startup:

```text
INCOME: Salary
EXPENSE: Food, Rent, Transportation, Entertainment, Healthcare, Utilities
```

Create category body:

```json
{
  "name": "SideBusinessIncome",
  "type": "INCOME"
}
```

### Transactions

```http
POST /api/transactions
GET /api/transactions?startDate=2024-01-01&endDate=2024-01-31&categoryId=1&type=INCOME
PUT /api/transactions/{id}
DELETE /api/transactions/{id}
```

Create transaction body:

```json
{
  "amount": 50000.00,
  "date": "2024-01-15",
  "category": "Salary",
  "description": "January Salary"
}
```

Transaction dates cannot be updated.

### Savings Goals

```http
POST /api/goals
GET /api/goals
GET /api/goals/{id}
PUT /api/goals/{id}
DELETE /api/goals/{id}
```

Create goal body:

```json
{
  "goalName": "Emergency Fund",
  "targetAmount": 5000.00,
  "targetDate": "2030-01-01",
  "startDate": "2024-01-01"
}
```

Progress is calculated as total income minus total expenses since the goal start date.

### Reports

```http
GET /api/reports/monthly/{year}/{month}
GET /api/reports/yearly/{year}
```

Reports return income totals by category, expense totals by category, and net savings.

## Design Notes

- Spring Security is used with a custom session-authentication filter so JSON login can set a standard server-side session.
- Users can only access their own transactions, custom categories, goals, and reports.
- Default categories are global and immutable.
- Known validation, conflict, authorization, and not-found scenarios are converted to JSON `4xx` responses by `@RestControllerAdvice`.
- H2 is in-memory by default, which is enough for assignment testing and free Render deployments. Environment variables can override the datasource settings.

