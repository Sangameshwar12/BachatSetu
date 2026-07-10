package in.bachatsetu.backend.admin.application.configuration.service;

/** Whether the platform is currently in maintenance, and the message to show if so. */
public record MaintenanceStatus(boolean active, String message) {
}
