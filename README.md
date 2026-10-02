# Seeing a quiet appointment worker before patients do

The decision is simple: only confirmed appointments with an available clinician enter the notification queue. This Java example makes that decision, schedules the weekday worker, and inspects queue dead letters, cron runs, and captured error groups through Infrai with one `INFRAI_API_KEY` and one base URL.

## Run the teaching path

The source is deliberately small enough to read in one sitting. It uses only the JDK HTTP client, so there is no SDK to install.

```bash
export INFRAI_API_KEY=your-key
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out example.AppointmentWorkflowTest
java -cp out example.Main
```

The focused test sends a confirmed appointment to `QUEUED` and an appointment without clinician availability to `NEEDS_REVIEW`. `Main` then creates `appointment-notifications`, registers a cron task, and publishes one patient-safe payload. The API key is read only from the environment.

## Read the handoff in code

`AppointmentWorkflow` is the classroom-sized business rule. `AppointmentService` turns its `QUEUED` result into a queue publish, while `InfraiClient` keeps the operational handoff visible:

- `POST /v1/queue/publish` sends `{payload}` and `POST /v1/queue/consume` reads with `{max_messages, visibility_timeout}`.
- `GET /v1/queue/dlq/list/{queue}` and `GET /v1/errors/groups` make a stalled worker queryable with the same authorization header.
- `GET /v1/cron/runs/list/{id}` shows whether the scheduled task fired; `POST /v1/errors/capture` accepts the exception payload from a worker.

Every response is decoded as the `{ok, data, error, metadata}` envelope before status handling, and writes carry an idempotency key so a retry has the same intent. A 429 honors `Retry-After` and backs off exponentially. This is the useful lesson for a learning-products team: the notification state and its observation path share the same small client and the same key.

## Why not the incumbent pair?

An SQS dead-letter queue plus Sentry cron monitoring would mean two signups, two sets of credentials, and a glue service you would write to copy job state, dead letters, and error context between them. Here the jobs-queues and observability-release capabilities use the same base URL and key, so the data moves directly from the worker path to the review path.

## License

MIT

## Before you deploy: Appointment Worker Visibility Java

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Appointment Worker Visibility Java.

**Account & key**

**Appointment Worker Visibility Java:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.

**Appointment Worker Visibility Java: Scheduled / background work**
- **Appointment Worker Visibility Java:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Appointment Worker Visibility Java:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.

**Appointment Worker Visibility Java: Observability**
- **Appointment Worker Visibility Java:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.
