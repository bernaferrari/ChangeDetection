"use client";

import { motion } from "framer-motion";
import {
  GraduationCap,
  ShoppingCart,
  Briefcase,
  Newspaper,
  Ticket,
  TrendingDown,
} from "lucide-react";

const useCases = [
  {
    icon: GraduationCap,
    title: "Exam Results & Grades",
    description:
      "Students and teachers can stop refreshing university portals. Get notified when grades are posted or schedules change.",
    example: "\"I got notified about my exam results 2 hours before my classmates!\"",
  },
  {
    icon: TrendingDown,
    title: "Price Drops",
    description:
      "Monitor product pages for price changes. Perfect for catching sales, restocks, or price drops on items you want.",
    example: "Track that gadget you've been eyeing and buy it when it drops.",
  },
  {
    icon: Briefcase,
    title: "Job Postings",
    description:
      "Be the first to apply. Monitor company career pages or job boards and get notified when new positions open.",
    example: "Apply within minutes of a job posting going live.",
  },
  {
    icon: Ticket,
    title: "Event Tickets",
    description:
      "Never miss ticket releases or restocks. Monitor ticket pages for concerts, sports, or limited events.",
    example: "Got Taylor Swift tickets because I knew the moment they went on sale.",
  },
  {
    icon: Newspaper,
    title: "News & Updates",
    description:
      "Track news sites, blogs, or documentation pages. Know immediately when important content is published.",
    example: "Follow your favorite blog without checking it daily.",
  },
  {
    icon: ShoppingCart,
    title: "Stock Availability",
    description:
      "Monitor out-of-stock products. Get notified the moment limited items become available again.",
    example: "Finally got that sold-out sneaker drop.",
  },
];

export function UseCases() {
  return (
    <section id="use-cases" className="py-24 px-6 bg-muted/30 border-t border-border">
      <div className="max-w-6xl mx-auto">
        <motion.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          className="text-center mb-16"
        >
          <motion.span
            initial={{ opacity: 0, y: 10 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="inline-block text-sm font-medium text-primary mb-4"
          >
            Use Cases
          </motion.span>
          <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold mb-4">
            What will you{" "}
            <span className="text-gradient">monitor</span>?
          </h2>
          <p className="text-muted-foreground text-lg max-w-xl mx-auto">
            From students to shoppers, professionals to hobbyists — everyone has
            a website they're tired of refreshing.
          </p>
        </motion.div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {useCases.map((useCase, index) => (
            <motion.div
              key={index}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.4, delay: index * 0.08 }}
              className="glass p-6 card-hover group"
            >
              <div className="icon-container mb-4 group-hover:scale-110 transition-transform">
                <useCase.icon size={24} strokeWidth={1.5} />
              </div>

              <h3 className="font-semibold text-lg mb-2">{useCase.title}</h3>
              <p className="text-sm text-muted-foreground mb-4 leading-relaxed">
                {useCase.description}
              </p>

              <div className="pt-4 border-t border-border">
                <p className="text-xs text-muted-foreground italic">
                  {useCase.example}
                </p>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
