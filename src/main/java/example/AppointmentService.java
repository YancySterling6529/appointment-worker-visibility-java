package example;

public final class AppointmentService {
    private final InfraiClient infrai;
    private final AppointmentWorkflow workflow = new AppointmentWorkflow();

    public AppointmentService(InfraiClient infrai) { this.infrai = infrai; }

    public String schedule(String patientId, String appointmentId, boolean confirmed, boolean clinicianAvailable) throws Exception {
        if (workflow.decide(confirmed, clinicianAvailable) == AppointmentWorkflow.Decision.NEEDS_REVIEW) return "needs-review";
        infrai.publish(workflow.payload(patientId, appointmentId));
        return "queued";
    }

    public void observe(String cronJobId) throws Exception {
        infrai.consume();
        infrai.deadLetters("appointment-notifications");
        infrai.errorGroups();
        infrai.cronRuns(cronJobId);
    }
}
