package in.bachatsetu.backend.member.interfaces.rest.dto;

interface ValidationPatterns {

    String GROUP_ROLE = "^(ORGANIZER|CO_ORGANIZER|MEMBER)$";
    String UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
}
