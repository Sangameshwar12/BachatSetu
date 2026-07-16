import type { ContributionFrequency, GroupType, PayoutMethod } from "@/types/group";

/** Matches the backend's `group.interfaces.rest.dto.ValidationPatterns.GROUP_TYPE` exactly. */
export const groupTypes: { value: GroupType; label: string }[] = [
  { value: "BHISHI", label: "Bhishi" },
  { value: "SELF_HELP_GROUP", label: "Self-Help Group" },
  { value: "SOCIETY_COLLECTION", label: "Society Collection" },
  { value: "COMMUNITY_FUND", label: "Community Fund" },
];

/** Matches the backend's `ValidationPatterns.CONTRIBUTION_FREQUENCY` exactly. */
export const contributionFrequencies: { value: ContributionFrequency; label: string }[] = [
  { value: "WEEKLY", label: "Weekly" },
  { value: "MONTHLY", label: "Monthly" },
  { value: "QUARTERLY", label: "Quarterly" },
];

/** Matches the backend's `ValidationPatterns.PAYOUT_METHOD` exactly. */
export const payoutMethods: { value: PayoutMethod; label: string }[] = [
  { value: "FIXED_ROTATION", label: "Fixed Rotation" },
  { value: "RANDOM_DRAW", label: "Random Draw" },
  { value: "AUCTION", label: "Auction" },
];
