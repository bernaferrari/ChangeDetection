"use client";

import { Github, Menu, X } from "lucide-react";
import { useEffect, useState } from "react";
import { cn } from "@/lib/utils";
import Link from "next/link";
import Image from "next/image";
import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui/button";

export function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20);
    };
    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const navLinks = [
    { href: "#features", label: "Features" },
    { href: "#how-it-works", label: "How It Works" },
    { href: "#use-cases", label: "Use Cases" },
    { href: "/docs", label: "Docs" },
  ];

  return (
    <nav
      className={cn(
        "fixed top-0 left-0 right-0 z-50 transition-all duration-300 border-b border-transparent",
        scrolled && "bg-background/80 backdrop-blur-md border-border"
      )}
    >
      <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
        {/* Logo */}
        <Link href="/" className="flex items-center gap-2.5 font-semibold text-lg">
          <div className="w-8 h-8 rounded-lg overflow-hidden">
            <Image
              src="/assets/logo.png"
              alt=""
              width={32}
              height={32}
              priority
              className="w-full h-full object-cover"
            />
          </div>
          <span className="hidden sm:inline">Change Detection</span>
        </Link>

        {/* Desktop Navigation */}
        <div className="hidden md:flex items-center gap-6">
          {navLinks.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className="text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
              {link.label}
            </a>
          ))}
        </div>

        {/* Right side actions */}
        <div className="flex items-center gap-2">
          <a
            href="https://github.com/bernaferrari/ChangeDetection"
            target="_blank"
            rel="noreferrer"
            aria-label="Open Change Detection on GitHub"
            className="p-3 text-muted-foreground hover:text-foreground transition-colors"
          >
            <Github size={18} />
          </a>
          <ThemeToggle />

          {/* Mobile menu button */}
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden p-3 text-muted-foreground hover:text-foreground transition-colors"
            aria-label={mobileMenuOpen ? "Close navigation menu" : "Open navigation menu"}
            aria-expanded={mobileMenuOpen}
          >
            {mobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
          </button>

          {/* CTA Button - Desktop only */}
          <Button size="sm" className="hidden md:inline-flex" asChild>
            <a
              href="https://play.google.com/store/apps/details?id=com.bernaferrari.changedetection"
              target="_blank"
              rel="noreferrer"
            >
              Download
            </a>
          </Button>
        </div>
      </div>

      {/* Mobile Navigation Menu */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-border bg-background/95 backdrop-blur-md">
          <div className="px-6 py-4 space-y-3">
            {navLinks.map((link) => (
              <a
                key={link.href}
                href={link.href}
                onClick={() => setMobileMenuOpen(false)}
                className="block py-2 text-muted-foreground hover:text-foreground transition-colors"
              >
                {link.label}
              </a>
            ))}
            <div className="pt-3 border-t border-border">
              <Button className="w-full" asChild>
                <a
                  href="https://play.google.com/store/apps/details?id=com.bernaferrari.changedetection"
                  target="_blank"
                  rel="noreferrer"
                >
                  Download App
                </a>
              </Button>
            </div>
          </div>
        </div>
      )}
    </nav>
  );
}
