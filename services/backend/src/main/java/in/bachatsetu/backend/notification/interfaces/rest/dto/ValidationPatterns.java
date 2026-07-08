package in.bachatsetu.backend.notification.interfaces.rest.dto;

interface ValidationPatterns {

    String UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    String CHANNEL = "^(EMAIL|SMS|WHATSAPP|IN_APP)$";
    String CATEGORY = "^(VERIFICATION|PAYMENT_RECEIPT|CONTRIBUTION_REMINDER|GROUP_UPDATE|DRAW_RESULT|SECURITY_ALERT)$";
}
