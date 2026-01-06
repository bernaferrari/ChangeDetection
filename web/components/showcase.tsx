"use client";

import { motion } from "framer-motion";
import Image from "next/image";
import { Plus, Minus, ArrowLeftRight } from "lucide-react";

export function Showcase() {
  return (
    <section className="py-24 px-6 border-t border-border overflow-hidden">
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
            Visual Comparison
          </motion.span>
          <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold mb-4">
            See exactly what{" "}
            <span className="text-gradient">changed</span>
          </h2>
          <p className="text-muted-foreground text-lg max-w-xl mx-auto">
            Our intelligent diff engine highlights additions, removals, and modifications
            so you can instantly understand what's different.
          </p>
        </motion.div>

        {/* Diff Demo */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="glass p-6 md:p-8 mb-12"
        >
          <div className="flex items-center justify-between mb-6">
            <h3 className="font-semibold">Text Diff Comparison</h3>
            <div className="flex items-center gap-4 text-sm">
              <span className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded-full bg-emerald-500/30" />
                <Plus size={14} className="text-emerald-500" />
                Added
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded-full bg-red-500/30" />
                <Minus size={14} className="text-red-500" />
                Removed
              </span>
            </div>
          </div>

          <div className="grid md:grid-cols-2 gap-4">
            {/* Before */}
            <div className="rounded-lg bg-card border border-border overflow-hidden">
              <div className="px-4 py-2 bg-muted/50 border-b border-border text-sm font-medium flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-muted-foreground/30" />
                Before
              </div>
              <div className="p-4 font-mono text-sm space-y-1">
                <div className="text-muted-foreground">1. Product XYZ</div>
                <div className="diff-removed px-2 py-0.5 rounded">2. Price: $299.99</div>
                <div className="text-muted-foreground">3. In Stock: Yes</div>
                <div className="diff-removed px-2 py-0.5 rounded">4. Shipping: Standard</div>
              </div>
            </div>

            {/* After */}
            <div className="rounded-lg bg-card border border-border overflow-hidden">
              <div className="px-4 py-2 bg-muted/50 border-b border-border text-sm font-medium flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-emerald-500" />
                After
              </div>
              <div className="p-4 font-mono text-sm space-y-1">
                <div className="text-muted-foreground">1. Product XYZ</div>
                <div className="diff-added px-2 py-0.5 rounded">2. Price: $199.99</div>
                <div className="text-muted-foreground">3. In Stock: Yes</div>
                <div className="diff-added px-2 py-0.5 rounded">4. Shipping: Free Express</div>
              </div>
            </div>
          </div>

          {/* Change Summary */}
          <div className="mt-6 pt-6 border-t border-border flex flex-wrap items-center justify-center gap-6 text-sm">
            <div className="flex items-center gap-2">
              <ArrowLeftRight size={16} className="text-primary" />
              <span className="text-muted-foreground">2 lines changed</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="font-mono text-emerald-500">-$100</span>
              <span className="text-muted-foreground">Price dropped!</span>
            </div>
          </div>
        </motion.div>

        {/* App Screenshots Grid */}
        <div className="grid md:grid-cols-2 gap-6">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            className="glass overflow-hidden group"
          >
            <div className="p-4 border-b border-border">
              <h3 className="font-semibold">Dashboard View</h3>
              <p className="text-sm text-muted-foreground">
                All your tracked websites in one place
              </p>
            </div>
            <div className="p-3 bg-muted/30">
              <div className="screenshot-container">
                <div className="screenshot-inner">
                  <Image
                    src="/assets/main-screen.png"
                    alt="Change Detection Dashboard"
                    width={600}
                    height={400}
                    className="w-full h-auto group-hover:scale-105 transition-transform duration-500"
                  />
                </div>
              </div>
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.1 }}
            className="glass overflow-hidden group"
          >
            <div className="p-4 border-b border-border">
              <h3 className="font-semibold">Version History</h3>
              <p className="text-sm text-muted-foreground">
                Browse through all captured snapshots
              </p>
            </div>
            <div className="p-3 bg-muted/30">
              <div className="screenshot-container">
                <div className="screenshot-inner">
                  <Image
                    src="/assets/header.png"
                    alt="Change Detection Version History"
                    width={600}
                    height={400}
                    className="w-full h-auto group-hover:scale-105 transition-transform duration-500"
                  />
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  );
}
