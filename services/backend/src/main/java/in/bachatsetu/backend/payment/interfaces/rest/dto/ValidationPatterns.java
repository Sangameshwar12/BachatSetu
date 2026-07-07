package in.bachatsetu.backend.payment.interfaces.rest.dto;

interface ValidationPatterns {

    String PAYMENT_METHOD = "^(UPI|BANK_TRANSFER|CARD|CASH|CHEQUE)$";
    String UPDATABLE_PAYMENT_STATUS = "^(PENDING_PROVIDER|VERIFIED|FAILED)$";
    String UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
}
