"use client"

import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"

export function Faq({ items }: { items: { q: string; a: string }[] }) {
  return (
    <Accordion className="rounded-3xl border bg-card px-5">
      {items.map((item) => (
        <AccordionItem key={item.q} value={item.q}>
          <AccordionTrigger className="py-4 text-base">{item.q}</AccordionTrigger>
          <AccordionContent className="pb-4 text-[15px] leading-relaxed text-muted-foreground">{item.a}</AccordionContent>
        </AccordionItem>
      ))}
    </Accordion>
  )
}
