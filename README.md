# Currency Rates Service

A reactive Spring Boot service that fetches **fiat** and **crypto** currency rates from external mock APIs, persists them to **PostgreSQL** (append-only history), and returns a merged **latest snapshot**. The endpoint always responds with `200 OK` — if an upstream call fails, the service falls back to the most recent data stored in the database.

---

## 🎯 Features

- **Single endpoint**
    - `GET /currency-rates` →
      ```json
      {
        "fiat":   [{"currency":"USD","rate":40.5}],
        "crypto": [{"currency":"BTC","rate":65000.0}]
      }
      ```
- **Append-only storage**: every successful pull from upstream is saved; history is kept.
- **Smart fallback**: if an upstream list is empty/failed, the service returns the **latest-per-currency** snapshot from the DB; if upstream returns a partial list, it is **merged** with the DB snapshot so clients always see a complete current view.

---

## 📁 Architecture

- **controller** — REST API (WebFlux)
- **service** — orchestration: fetch + persist + merge + fallback
- **repository** — reactive repositories (Spring Data R2DBC)
- **model** — database entities
- **dto** — API contracts (Java records)
- **util** — mapping between entities and DTOs
- **resources** — configuration and SQL migrations
- **test** — unit and integration tests (JUnit 5, Mockito, Testcontainers)

---

## 🛠 Tech stack

- **Java 21**
- **Spring Boot 3.5.4** — WebFlux, Validation, Actuator
- **Spring Data R2DBC** (PostgreSQL)
- **JUnit 5**, **Mockito**, **Testcontainers**
- **Lombok**
- **Docker & Docker Compose**

---

## ⚙️ How to run

1. **Clone the repository to your local machine.**
2. **Configure Database Access, Docker Access:** Navigate to `.env`
   and replace the properties that you need. 
   `If there are no conflicts with your environment, 
   you can use the existing .env file.`
3. **From the project root, run:**

```bash
docker compose up -d
```

This brings up the database, mock services, and the application.
Use the Postman collection below to exercise the API.

**Postman collection:** [Postman](./currency-rates-service.postman_collection.json)

---

## Notes

- Response is always `200 OK` with two arrays: `fiat[]` and `crypto[]`, each item shaped as `{ "currency": "...", "rate": number }`.
- When upstreams are healthy, the response reflects live data. If any upstream fails, the service serves the latest stored snapshot for the missing pieces.
