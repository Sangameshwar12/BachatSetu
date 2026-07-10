package in.bachatsetu.backend.invitation.interfaces.rest.dto;

interface ValidationPatterns {

    String UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    String INVITATION_TYPE = "^(QR|CODE|LINK)$";
}
