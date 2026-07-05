package in.bachatsetu.backend.auth.interfaces.rest.dto;

interface ValidationPatterns {

    String UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    String OTP_PURPOSE = "^(REGISTRATION|SIGN_IN|PASSWORD_RESET|MOBILE_CHANGE)$";
    String OTP_CODE = "^[0-9]{6}$";

}
