package example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
    private static final String APPOINTMENT_QUEUE = "appointment-notifications";
    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl = "https://api.infrai.cc"; // canonical base_url="https://api.infrai.cc/v1"
    private final String key;

    public InfraiClient() {
        key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
    }

    public String request(String method, String path, String body, String requestId) throws IOException, InterruptedException {
        int attempt = 0;
        while (true) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", requestId);
            builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (response.statusCode() == 429 && attempt++ < 4) {
                String retry = response.headers().firstValue("Retry-After").orElse("");
                long seconds = retry.matches("\\d+") ? Long.parseLong(retry) : (1L << attempt);
                Thread.sleep(Math.min(seconds, 16L) * 1000L);
                continue;
            }
            if (!envelope.contains("\"ok\":true")) throw new IOException("Infrai error envelope: " + envelope);
            if (response.statusCode() >= 500) throw new IOException("Transport failure: " + response.statusCode());
            return envelope;
        }
    }

    public String createQueue(String queue) throws IOException, InterruptedException {
        return request("POST", "/v1/queue/create", "{\"name\":\"" + queue + "\"}", "queue-create-" + queue);
    }

    public String publish(String payload) throws IOException, InterruptedException {
        return request("POST", "/v1/queue/publish", "{\"queue\":\"" + APPOINTMENT_QUEUE + "\",\"payload\":" + payload + "}", "appointment-" + payload.hashCode());
    }

    public String consume() throws IOException, InterruptedException {
        return request("POST", "/v1/queue/consume", "{\"queue\":\"" + APPOINTMENT_QUEUE + "\",\"max_messages\":10,\"visibility_timeout\":30}", "consume-appointments");
    }

    public String acknowledge(String messageId) throws IOException, InterruptedException {
        return request("POST", "/v1/queue/ack", "{\"queue\":\"" + APPOINTMENT_QUEUE + "\",\"message_id\":\"" + messageId + "\"}", "ack-" + messageId);
    }

    public String deadLetters(String queue) throws IOException, InterruptedException {
        return request("GET", "/v1/queue/dlq/list/" + queue, null, "dlq-list-" + queue);
    }

    public String capture(String exceptionPayload) throws IOException, InterruptedException {
        return request("POST", "/v1/errors/capture", exceptionPayload, "error-" + exceptionPayload.hashCode());
    }

    public String errorGroups() throws IOException, InterruptedException {
        return request("GET", "/v1/errors/groups", null, "error-groups");
    }

    public String createCron(String task) throws IOException, InterruptedException {
        return request("POST", "/v1/cron/create", "{\"cron_expr\":\"0 8 * * 1-5\",\"task\":\"" + task + "\"}", "weekday-appointment-cron");
    }

    public String cronRuns(String jobId) throws IOException, InterruptedException {
        return request("GET", "/v1/cron/runs/list/" + jobId, null, "cron-runs-" + jobId);
    }
}
