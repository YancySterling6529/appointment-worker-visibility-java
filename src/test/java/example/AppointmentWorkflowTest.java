package example;

public final class AppointmentWorkflowTest {
    public static void main(String[] args) {
        AppointmentWorkflow workflow = new AppointmentWorkflow();
        if (workflow.decide(true, true) != AppointmentWorkflow.Decision.QUEUED) throw new AssertionError();
        if (workflow.decide(true, false) != AppointmentWorkflow.Decision.NEEDS_REVIEW) throw new AssertionError();
        System.out.println("workflow decisions verified");
    }
}
