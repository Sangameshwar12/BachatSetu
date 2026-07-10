import { apiClient } from "@/services/api-client";
import type { CompleteOnboardingRequest, OnboardingCompletedResponse } from "@/types/auth";

/** `POST /api/v1/users/me/onboarding` — authenticated; acts on the caller's own profile. */
export async function completeOnboarding(
  payload: CompleteOnboardingRequest
): Promise<OnboardingCompletedResponse> {
  const { data } = await apiClient.post<OnboardingCompletedResponse>(
    "/api/v1/users/me/onboarding",
    payload
  );
  return data;
}
