"use client";

import { motion } from "framer-motion";
import Image from "next/image";
import { Bell, Github, Smartphone } from "lucide-react";
import { Button } from "@/components/ui/button";

export function Hero() {
  return (
    <section className="relative hero-gradient overflow-hidden">
      {/* Subtle grid pattern */}
      <div className="absolute inset-0 grid-pattern opacity-50" />

      <div className="relative max-w-6xl mx-auto px-6 pt-32 pb-24">
        <div className="text-center">
          {/* Badge */}
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full border border-border bg-card/50 backdrop-blur-sm text-sm text-muted-foreground mb-8"
          >
            <span className="relative flex h-2 w-2">
              <span className="pulse-ring absolute inline-flex h-full w-full rounded-full bg-emerald-500 opacity-75" />
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500" />
            </span>
            Free & Open Source
          </motion.div>

          {/* Headline */}
          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-bold tracking-tight mb-6 leading-[1.1]"
          >
            Never miss a{" "}
            <span className="text-gradient-animated">website update</span>
            <br className="hidden sm:block" />
            <span className="text-muted-foreground font-normal text-3xl sm:text-4xl md:text-5xl lg:text-6xl">
              {" "}again
            </span>
          </motion.h1>

          {/* Description */}
          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="text-lg md:text-xl text-muted-foreground max-w-2xl mx-auto mb-10 leading-relaxed"
          >
            Stop refreshing websites endlessly. Change Detection monitors any webpage
            and notifies you the instant something changes. Track prices, grades,
            job postings, news & more.
          </motion.p>

          {/* CTAs */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.3 }}
            className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-4"
          >
            <Button size="lg" className="glow" asChild>
              <a
                href="https://play.google.com/store/apps/details?id=com.bernaferrari.changedetection"
                target="_blank"
              >
                <Smartphone className="mr-2" size={18} />
                Get it on Google Play
              </a>
            </Button>
            <Button variant="outline" size="lg" asChild>
              <a
                href="https://github.com/niceperson/changedetection"
                target="_blank"
              >
                <Github className="mr-2" size={18} />
                View on GitHub
              </a>
            </Button>
          </motion.div>

          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.4 }}
            className="text-sm text-muted-foreground mb-16"
          >
            Android & iOS (coming soon) · Built with Kotlin Multiplatform
          </motion.p>

          {/* Phone Mockup */}
          <motion.div
            initial={{ opacity: 0, y: 40 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.5 }}
            className="relative max-w-sm mx-auto float"
          >
            <div className="phone-mockup">
              <div className="phone-screen aspect-[9/19.5]">
                <Image
                  src="/assets/main-screen.png"
                  alt="Change Detection - Main Interface showing tracked websites"
                  width={400}
                  height={867}
                  priority
                  className="w-full h-full object-cover object-top"
                />
              </div>
            </div>

            {/* Floating notification badges */}
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.5, delay: 1 }}
              className="absolute -left-4 md:-left-16 top-1/4 glass p-3 rounded-lg shadow-lg"
            >
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-emerald-500/20 flex items-center justify-center">
                  <Bell size={14} className="text-emerald-500" />
                </div>
                <div className="text-left">
                  <p className="text-xs font-medium">Price dropped!</p>
                  <p className="text-[10px] text-muted-foreground">$299 → $199</p>
                </div>
              </div>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.5, delay: 1.2 }}
              className="absolute -right-4 md:-right-20 top-1/2 glass p-3 rounded-lg shadow-lg"
            >
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center">
                  <span className="text-xs font-bold text-primary">+3</span>
                </div>
                <div className="text-left">
                  <p className="text-xs font-medium">Content changed</p>
                  <p className="text-[10px] text-muted-foreground">3 new lines added</p>
                </div>
              </div>
            </motion.div>
          </motion.div>
        </div>
      </div>
    </section>
  );
}
