import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { SectionHeading } from "@/components/shared/section-heading";
import { faqItems } from "@/constants/landing";

export function FaqSection() {
  return (
    <section id="faq" className="section-y">
      <div className="container-page flex flex-col items-center gap-14">
        <SectionHeading eyebrow="FAQ" title="Common questions" />

        <Accordion className="w-full max-w-2xl">
          {faqItems.map((item, index) => (
            <AccordionItem key={item.question} value={`item-${index}`}>
              <AccordionTrigger className="text-left text-base font-medium">
                {item.question}
              </AccordionTrigger>
              <AccordionContent className="text-muted-foreground">{item.answer}</AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      </div>
    </section>
  );
}
