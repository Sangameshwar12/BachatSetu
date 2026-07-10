import { BenefitsSection } from "@/features/landing/benefits-section";
import { CtaSection } from "@/features/landing/cta-section";
import { FaqSection } from "@/features/landing/faq-section";
import { FeaturesSection } from "@/features/landing/features-section";
import { HeroSection } from "@/features/landing/hero-section";
import { HowItWorksSection } from "@/features/landing/how-it-works-section";
import { PricingSection } from "@/features/landing/pricing-section";
import { TestimonialsSection } from "@/features/landing/testimonials-section";

export default function HomePage() {
  return (
    <>
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
