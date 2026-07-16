import type { Metadata } from "next";
import { Suspense } from "react";

import { AuthShell } from "@/features/auth/auth-shell";
import { OnboardingForm } from "@/features/auth/onboarding-form";

export const metadata: Metadata = { title: "Complete your profile" };

export default function OnboardingPage() {
  return (
    <AuthShell title="Complete your profile" description="A few last details — all optional.">
      <Suspense>
        <OnboardingForm />
      </Suspense>
    </AuthShell>
  );
}
