# Bank Transaction Fraud Detection (Java + .NET + PostgreSQL)

| Service | Tech | Port | Role |
|---|---|---|---|
| `fraud-engine-java` | Java 17, Spring Boot 3 | 8090 | Receives transactions, applies fraud rules, raises alerts, simulates a live stream |
| `case-api-dotnet` | .NET 8, ASP.NET Core | 8091 | Case management API + analyst dashboard |
| `db` | PostgreSQL 16 | 5433 | Transactions and alerts |

## Fraud rules (Java)

1. `HIGH_AMOUNT` - amount above 5000
2. `LOCATION_CHANGE` - country differs from the account's last transaction in the past hour
3. `HIGH_VELOCITY` - 5 or more transactions in the last minute

## Run with Docker (recommended)

```bash
docker compose up --build
```

- Analyst dashboard: http://localhost:8091
- Java engine health: http://localhost:8090/api/health

Transactions are generated every 2 seconds; alerts start appearing within a minute.

## Try it manually

```bash
curl -X POST http://localhost:8090/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountId":42,"amount":9000,"country":"US"}'
```

Review an alert:

```bash
curl -X POST http://localhost:8091/api/alerts/1/decision \
  -H "Content-Type: application/json" -d '{"decision":"BLOCKED"}'
```

## .NET endpoints

- `GET /api/alerts?status=PENDING|APPROVED|BLOCKED|ALL`
- `POST /api/alerts/{id}/decision`
- `GET /api/stats`

## Run without Docker

1. Start PostgreSQL, create DB `fraud`, run `db/init.sql`.
2. Java: `cd fraud-engine-java && mvn spring-boot:run`
3. .NET: `cd case-api-dotnet && dotnet run --urls http://localhost:8091`

(Defaults assume PostgreSQL on port 5433; set `DB_URL` / `DB_CONN` to change.)

## Ideas to extend

- Put Kafka between ingestion and the rules engine (Kafka Streams)
- Add an ML fraud score (ML.NET or a Python model)
- Push live alerts with SignalR instead of polling
- Add authentication and roles for analysts
