import type { Metadata } from "next";

import { siteConfig } from "@/constants/site";

export const metadata: Metadata = {
  title: "Privacy Policy",
  description: `How ${siteConfig.name} collects, uses, and protects your data.`,
  alternates: { canonical: "/privacy" },
};

export default function PrivacyPage() {
  return (
    <section className="section-y">
      <div className="container-page max-w-3xl">
        <h1 className="text-3xl font-semibold text-foreground">Privacy Policy</h1>
        <p className="mt-2 text-sm text-muted-foreground">Last updated 10 July 2026</p>

        <div className="mt-10 flex flex-col gap-6 text-sm text-muted-foreground">
          <p>
            {siteConfig.name} runs Bhishi and chit-fund savings groups. This page explains what
            information we collect to do that, and how it&apos;s used.
          </p>

          <div>
            <h2 className="text-lg font-semibold text-foreground">What we collect</h2>
            <p className="mt-2">
              {siteConfig.name} stores your mobile number, name, and (optionally) email and profile
              photo to identify your account; the savings groups you join or organize, your
              contribution and payment records, and receipts, to run those groups; and device/app
              usage needed to keep the service secure, such as sign-in timestamps.
            </p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-foreground">How we use it</h2>
            <p className="mt-2">
              Your data is used only to operate your savings groups: verifying your identity via
              OTP, tracking contributions and draws, generating receipts, and sending you
              notifications about your groups. We never sell your data or use it for advertising.
            </p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-foreground">Who can see it</h2>
            <p className="mt-2">
              We never share your data with other members beyond what&apos;s needed to run the
              group you&apos;re part of — for example, an organizer can see your contribution
              status, but not your other groups. Payment and storage providers we integrate with
              process the minimum data required to complete a transaction or store a file on our
              behalf.
            </p>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-foreground">Your choices</h2>
            <p className="mt-2">
              You can review your profile and notification preferences from your account
              settings, and request account deletion by contacting{" "}
              <a href={`mailto:${siteConfig.supportEmail}`} className="underline underline-offset-4">
                {siteConfig.supportEmail}
              </a>
              .
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
