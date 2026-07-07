package in.bachatsetu.backend.draw.interfaces.rest.dto;

interface ValidationPatterns {

    String DRAW_TYPE = "^(RANDOM|FIXED_ROTATION|AUCTION)$";
    String UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
}
