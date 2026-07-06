package in.bachatsetu.backend.group.interfaces.rest.dto;

interface ValidationPatterns {

    String GROUP_TYPE = "^(BHISHI|SELF_HELP_GROUP|SOCIETY_COLLECTION|COMMUNITY_FUND)$";
    String PAYOUT_METHOD = "^(FIXED_ROTATION|RANDOM_DRAW|AUCTION)$";
    String CONTRIBUTION_FREQUENCY = "^(WEEKLY|MONTHLY|QUARTERLY)$";
}
