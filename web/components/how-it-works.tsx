"use client";

import { motion } from "framer-motion";
import { Globe, Scan, Bell, ArrowRight } from "lucide-react";

const steps = [
  {
    number: "01",
    icon: Globe,
    title: "Add a Website",
    description:
      "Paste any URL or use our built-in browser to navigate to the page you want to monitor. Works with any public webpage.",
    color: "from-primary/20 to-primary/5",
  },
  {
    number: "02",
    icon: Scan,
    title: "We Monitor It",
    description:
      "Change Detection checks your websites in the background at your preferred interval. No manual refreshing needed.",
    color: "from-emerald-500/20 to-emerald-500/5",
  },
  {
    number: "03",
    icon: Bell,
    title: "Get Notified",
    description:
      "When a check detects an update, you get a notification. Open the app to see exactly what’s different.",
    color: "from-amber-500/20 to-amber-500/5",
  },
];

export function HowItWorks() {
  return (
    <section id="how-it-works" className="py-24 px-6 border-t border-border relative overflow-hidden">
      {/* Background decoration */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[800px] h-[400px] bg-gradient-radial from-primary/5 to-transparent blur-3xl" />

      <div className="relative max-w-6xl mx-auto">
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
            How It Works
          </motion.span>
          <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold mb-4">
            Simple as{" "}
            <span className="text-gradient">1-2-3</span>
          </h2>
          <p className="text-muted-foreground text-lg max-w-xl mx-auto">
            Set it up once and forget about it. We handle the monitoring so you don’t have to.
          </p>
        </motion.div>

        <div className="grid md:grid-cols-3 gap-8 relative">
          {/* Connection line */}
          <div className="hidden md:block absolute top-24 left-[20%] right-[20%] h-0.5">
            <div className="w-full h-full bg-gradient-to-r from-primary/30 via-emerald-500/30 to-amber-500/30" />
          </div>

          {steps.map((step, index) => (
            <motion.div
              key={index}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: index * 0.15 }}
              className="relative"
            >
              <div className="glass p-8 text-center relative overflow-hidden group">
                {/* Gradient background */}
                <div
                  className={`absolute inset-0 bg-gradient-to-br ${step.color} opacity-0 group-hover:opacity-100 transition-opacity duration-500`}
                />

                {/* Step number */}
                <div className="relative">
                  <span className="text-6xl font-bold text-muted-foreground/10 absolute -top-2 left-1/2 -translate-x-1/2">
                    {step.number}
                  </span>

                  {/* Icon */}
                  <div className="relative w-16 h-16 mx-auto mb-6 rounded-2xl bg-card border border-border flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
                    <step.icon size={28} className="text-primary" strokeWidth={1.5} />
                  </div>
                </div>

                <h3 className="relative text-xl font-semibold mb-3">{step.title}</h3>
                <p className="relative text-sm text-muted-foreground leading-relaxed">
                  {step.description}
                </p>
              </div>

              {/* Arrow for mobile */}
              {index < steps.length - 1 && (
                <div className="md:hidden flex justify-center my-4">
                  <ArrowRight className="text-muted-foreground/30 rotate-90" size={24} />
                </div>
              )}
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
