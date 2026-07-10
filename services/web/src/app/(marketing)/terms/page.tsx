import type { Metadata } from "next";

import { siteConfig } from "@/constants/site";

export const metadata: Metadata = {
  title: "Terms of Service",
  description: `The terms that govern your use of ${siteConfig.name}.`,
  alternates: { canonical: "/terms" },
};

export default function TermsPage() {
  return (
    <section className="section-y">
      <div className="container-page max-w-3xl">
        <h1 className="text-3xl font-semibold text-foreground">Terms of Service</h1>
        <p className="mt-2 text-sm text-muted-foreground">Last updated 10 July 2026</p>

        <div className="mt-10 flex flex-col gap-6 text-sm text-muted-foreground">
          <p>
            By creating an account with {siteConfig.name}, you agree to the terms below. Please
            read them before joining or organizing a savings group.
          </p>

          <div>
            <h2 className="text-lg font-semibold text-foreground">Your account</h2>
            <p className="mt-2">
              You&apos;re responsible for keeping your mobile number and OTP codes confidential,
              and for the accuracy of the information you provide during signup and onboarding.
            </p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-foreground">Savings groups</h2>
            <p className="mt-2">
              {siteConfig.name} is a platform for organizing and recording group savings activity —
              contributions, draws, and payouts. Organizers are responsible for the rules,
              contribution schedule, and payout order of the groups they create; {siteConfig.name}{" "}
              does not guarantee, insure, or manage group funds directly.
            </p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-foreground">Payments</h2>
            <p className="mt-2">
              Contributions are processed through the payment provider configured for your group.
              You&apos;re responsible for verifying a payment was recorded correctly and for
              contacting your organizer about any discrepancy.
            </p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-foreground">Acceptable use</h2>
            <p className="mt-2">
              You agree not to use {siteConfig.name} for any unlawful purpose, to misrepresent your
              identity, or to attempt to disrupt or gain unauthorized access to the service.
            </p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-foreground">Changes</h2>
            <p className="mt-2">
              We may update these terms as the product evolves. Continued use of{" "}
              {siteConfig.name} after a change means you accept the updated terms.
            </p>
          </div>

          <p className="text-xs">
            This is a general summary for early access users, not a substitute for formal legal
            advice — it will be reviewed and expanded before general availability.
          </p>
        </div>
      </div>
    </section>
  );
}
