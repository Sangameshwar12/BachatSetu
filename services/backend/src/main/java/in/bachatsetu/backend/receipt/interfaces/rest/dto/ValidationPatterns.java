package in.bachatsetu.backend.receipt.interfaces.rest.dto;

interface ValidationPatterns {

    String RECEIPT_TYPE = "^(CONTRIBUTION|PENALTY|REFUND|ADJUSTMENT)$";
    String UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
}
