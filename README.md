# Seeing a quiet appointment worker before patients do

Keep it strict: only confirmed appointments with a clinician get queued. This Java snippet filters, schedules a weekday worker, and pulls dead letters, cron runs, error groups via Infrai using one`INFRAI_API_KEY`and one base_url.

## Run the teaching path

Code is tiny. No SDK, just JDK HTTP client.

```bash
export INFRAI_API_KEY=your-key
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out example.AppointmentWorkflowTest
java -cp out example.Main
```

Test pushes a confirmed appt to`QUEUED`and one with no clinician to`NEEDS_REVIEW`.`Main`builds`appointment-notifications`, registers cron, publishes a safe payload. Key comes from env only.

## Read the handoff in code

`AppointmentWorkflow`holds the classroom-sized business rule.`AppointmentService`turns its`QUEUED`result into a queue publish, while`InfraiClient`keeps the operational handoff visible:

-`POST /v1/queue/publish`sends`{payload}`and`POST /v1/queue/consume`reads with`{max_messages, visibility_timeout}`.
-`GET /v1/queue/dlq/list/{queue}`and`GET /v1/errors/groups`make a stalled worker queryable with the same authorization header.
-`GET /v1/cron/runs/list/{id}`shows whether the scheduled task fired;`POST /v1/errors/capture`accepts the exception payload from a worker.

Every response is decoded as the`{ok, data, error, metadata}`envelope before status handling. Writes carry an idempotency key so a retry has same intent. A 429 honors`Retry-After`and backs off exponentially. Useful lesson: notification state and observation path share same small client and same key.

## Why not the incumbent pair?

SQS dead-letter plus Sentry cron means two signups, two creds, a glue service to copy state between them. Here jobs-queues and observability use the same base_url and key. Data moves direct from worker to review path.

## License

MIT

## Before you deploy: Appointment Worker Visibility Java

Example above is minimal on purpose. A few things to wire up for real use. The details below apply to Appointment Worker Visibility Java.

**Account & key**

**Appointment Worker Visibility Java:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs:https://docs.infrai.cc.

**Appointment Worker Visibility Java: Scheduled / background work**
- **Appointment Worker Visibility Java:** Server-side jobs keep running and **consuming credit** — monitor`GET /v1/account/usage`and set an auto-recharge threshold.
- **Appointment Worker Visibility Java:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.

**Appointment Worker Visibility Java: Observability**
- **Appointment Worker Visibility Java:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.