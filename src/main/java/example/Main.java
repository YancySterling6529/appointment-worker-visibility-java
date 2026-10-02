package example;

public final class Main {
    public static void main(String[] args) throws Exception {
        InfraiClient client = new InfraiClient();
        client.createQueue("appointment-notifications");
        String cron = client.createCron("https://example.org/appointment-worker");
        String result = new AppointmentService(client).schedule("patient-42", "visit-9001", true, true);
        System.out.println("appointment decision: " + result);
        System.out.println("cron response: " + cron);
    }
}
