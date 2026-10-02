package example;

import java.util.Objects;

public final class AppointmentWorkflow {
    public enum Decision { QUEUED, NEEDS_REVIEW }

    public Decision decide(boolean patientConfirmed, boolean clinicianAvailable) {
        return patientConfirmed && clinicianAvailable ? Decision.QUEUED : Decision.NEEDS_REVIEW;
    }

    public String payload(String patientId, String appointmentId) {
        Objects.requireNonNull(patientId);
        Objects.requireNonNull(appointmentId);
        return "{\"patient_id\":\"" + patientId + "\",\"appointment_id\":\"" + appointmentId + "\"}";
    }
}
