import type { Metadata } from "next";

import { siteConfig } from "@/constants/site";
import { BenefitsSection } from "@/features/landing/benefits-section";
import { CtaSection } from "@/features/landing/cta-section";
import { FaqSection } from "@/features/landing/faq-section";
import { FeaturesSection } from "@/features/landing/features-section";
import { HeroSection } from "@/features/landing/hero-section";
import { HowItWorksSection } from "@/features/landing/how-it-works-section";
import { PricingSection } from "@/features/landing/pricing-section";
import { TestimonialsSection } from "@/features/landing/testimonials-section";

export const metadata: Metadata = {
  alternates: { canonical: "/" },
};

const organizationJsonLd = {
  "@context": "https://schema.org",
  "@type": "Organization",
  name: siteConfig.name,
  url: siteConfig.url,
  description: siteConfig.description,
};

export default function HomePage() {
  return (
    <>
      {/* Structured data for search engines; static and derived from siteConfig, no user input. */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(organizationJsonLd) }}
      />
      <HeroSection />
      <FeaturesSection />
      <HowItWorksSection />
      <BenefitsSection />
      <TestimonialsSection />
      <PricingSection />
      <FaqSection />
      <CtaSection />
    </>
  );
}
