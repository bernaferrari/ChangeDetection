"use client";

import { motion } from "framer-motion";
import { Smartphone, Github, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";

export function CTA() {
  return (
    <section className="py-24 px-6 relative overflow-hidden">
      {/* Background effects */}
      <div className="absolute inset-0 hero-gradient" />
      <div className="absolute inset-0 grid-pattern opacity-30" />

      <div className="relative max-w-4xl mx-auto text-center">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
        >
          {/* Icon */}
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-primary/10 mb-8">
            <Sparkles className="w-8 h-8 text-primary" />
          </div>

          <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold mb-6">
            Ready to stop{" "}
            <span className="text-gradient">refreshing?</span>
          </h2>

          <p className="text-lg md:text-xl text-muted-foreground max-w-2xl mx-auto mb-10">
            Join thousands of users who've automated their website monitoring.
            Free, open source, and respects your privacy.
          </p>

          {/* Stats */}
          <div className="grid grid-cols-3 gap-4 max-w-lg mx-auto mb-10">
            <div className="glass p-4 stats-card">
              <div className="text-2xl md:text-3xl font-bold text-primary">10K+</div>
              <div className="text-xs text-muted-foreground">Downloads</div>
            </div>
            <div className="glass p-4 stats-card">
              <div className="text-2xl md:text-3xl font-bold text-primary">4.5</div>
              <div className="text-xs text-muted-foreground">Rating</div>
            </div>
            <div className="glass p-4 stats-card">
              <div className="text-2xl md:text-3xl font-bold text-primary">100%</div>
              <div className="text-xs text-muted-foreground">Free</div>
            </div>
          </div>

          {/* CTAs */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Button size="lg" className="glow min-w-[200px]" asChild>
              <a
                href="https://play.google.com/store/apps/details?id=com.bernaferrari.changedetection"
                target="_blank"
              >
                <Smartphone className="mr-2" size={18} />
                Get it on Google Play
              </a>
            </Button>
            <Button variant="outline" size="lg" className="min-w-[200px]" asChild>
              <a
                href="https://github.com/niceperson/changedetection"
                target="_blank"
              >
                <Github className="mr-2" size={18} />
                Star on GitHub
              </a>
            </Button>
          </div>

          <p className="text-sm text-muted-foreground mt-6">
            No account required · No ads · No tracking
          </p>
        </motion.div>
      </div>
    </section>
  );
}
