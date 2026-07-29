"use client";

import { motion, AnimatePresence } from "framer-motion";
import { useState } from "react";
import {
  Bell,
  Zap,
  Eye,
  History,
  Wifi,
  Palette,
  FileText,
  ImageIcon,
  Sparkles,
  Clock,
  Shield,
} from "lucide-react";
import { cn } from "@/lib/utils";

const categories = [
  { id: "monitoring", label: "Monitoring", icon: Eye },
  { id: "notifications", label: "Alerts", icon: Bell },
  { id: "comparison", label: "Comparison", icon: Sparkles },
];

const features = {
  monitoring: [
    {
      icon: Shield,
      title: "On-device Monitoring",
      description:
        "Checks, snapshots, and comparisons stay on your device. No account or cloud service required.",
    },
    {
      icon: Zap,
      title: "Turbo Mode",
      description:
        "High-frequency monitoring with intervals as short as 15 seconds for time-sensitive content.",
    },
    {
      icon: FileText,
      title: "Multiple Content Types",
      description:
        "Track HTML pages, PDFs, images, and more. Works with any content that has a URL.",
    },
  ],
  notifications: [
    {
      icon: Bell,
      title: "Change Notifications",
      description:
        "Get notified when a scheduled or Turbo check detects a website update.",
    },
    {
      icon: Wifi,
      title: "Smart Background Sync",
      description:
        "Intelligent syncing with constraints: WiFi-only, charging, or when device is idle.",
    },
    {
      icon: Clock,
      title: "Flexible Scheduling",
      description:
        "Choose a background sync interval, or start a short Turbo session when seconds matter.",
    },
  ],
  comparison: [
    {
      icon: Eye,
      title: "Visual Diff",
      description:
        "See exactly what changed with highlighted additions and removals in a clean side-by-side view.",
    },
    {
      icon: History,
      title: "Version History",
      description:
        "Browse through all captured snapshots. Complete timeline of changes for any website.",
    },
    {
      icon: ImageIcon,
      title: "Image & PDF Comparison",
      description:
        "Visual carousel for comparing images and documents. Spot differences at a glance.",
    },
  ],
};

export function Features() {
  const [activeCategory, setActiveCategory] = useState("monitoring");

  return (
    <section id="features" className="py-24 px-6 relative">
      <div className="absolute inset-0 grid-pattern opacity-30" />

      <div className="relative max-w-5xl mx-auto">
        <motion.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          className="text-center mb-12"
        >
          <motion.span
            initial={{ opacity: 0, y: 10 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="inline-block text-sm font-medium text-primary mb-4"
          >
            Features
          </motion.span>
          <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold mb-4">
            Everything you need
          </h2>
          <p className="text-muted-foreground text-lg max-w-xl mx-auto">
            Powerful yet simple. Pick a category to explore.
          </p>
        </motion.div>

        {/* Category Tabs */}
        <div className="flex justify-center mb-12">
          <div className="grid w-full max-w-md grid-cols-3 p-1 rounded-xl bg-muted/50 border border-border">
            {categories.map((category) => (
              <button
                key={category.id}
                onClick={() => setActiveCategory(category.id)}
                className={cn(
                  "relative flex items-center justify-center gap-2 px-2 py-2.5 rounded-lg text-sm font-medium transition-all duration-300",
                  activeCategory === category.id
                    ? "text-primary-foreground"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                {activeCategory === category.id && (
                  <motion.div
                    layoutId="activeTab"
                    className="absolute inset-0 bg-primary rounded-lg"
                    transition={{ type: "spring", duration: 0.5 }}
                  />
                )}
                <category.icon size={16} className="relative z-10" />
                <span className="relative z-10">{category.label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Feature Cards */}
        <AnimatePresence mode="wait">
          <motion.div
            key={activeCategory}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.3 }}
            className="grid md:grid-cols-3 gap-5"
          >
            {features[activeCategory as keyof typeof features].map((feature, index) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3, delay: index * 0.1 }}
                className="glass p-6 group hover:border-primary/30 transition-all duration-300"
              >
                <div className="icon-container mb-4 group-hover:scale-110 transition-transform duration-300">
                  <feature.icon size={22} strokeWidth={1.5} />
                </div>
                <h3 className="font-semibold text-lg mb-2">{feature.title}</h3>
                <p className="text-sm text-muted-foreground leading-relaxed">
                  {feature.description}
                </p>
              </motion.div>
            ))}
          </motion.div>
        </AnimatePresence>

        {/* Bottom highlights */}
        <motion.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          className="mt-12 flex flex-wrap justify-center gap-6 text-sm text-muted-foreground"
        >
          <div className="flex items-center gap-2">
            <Palette size={16} className="text-primary" />
            <span>Dark & Light themes</span>
          </div>
          <div className="flex items-center gap-2">
            <Shield size={16} className="text-primary" />
            <span>Local snapshots & history</span>
          </div>
          <div className="flex items-center gap-2">
            <Sparkles size={16} className="text-primary" />
            <span>No account required</span>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
