"use client";

import Link from "next/link";
import Image from "next/image";
import { Github, Heart } from "lucide-react";

export function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="border-t border-border py-12 px-6">
      <div className="max-w-6xl mx-auto">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-12">
          {/* Brand */}
          <div className="md:col-span-2">
            <Link href="/" className="flex items-center gap-2.5 mb-4">
              <div className="w-8 h-8 rounded-lg overflow-hidden">
                <Image
                  src="/assets/logo.png"
                  alt="Change Detection"
                  width={32}
                  height={32}
                  className="w-full h-full object-cover"
                />
              </div>
              <span className="font-semibold text-lg">Change Detection</span>
            </Link>
            <p className="text-sm text-muted-foreground max-w-sm mb-4">
              Stop refreshing websites manually. Get notified instantly when any
              webpage changes. Free, open source, and privacy-focused.
            </p>
            <div className="flex items-center gap-4">
              <a
                href="https://github.com/niceperson/changedetection"
                target="_blank"
                className="text-muted-foreground hover:text-foreground transition-colors"
              >
                <Github size={20} />
              </a>
            </div>
          </div>

          {/* Links */}
          <div>
            <h4 className="font-semibold mb-4">Product</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>
                <a href="#features" className="hover:text-foreground transition-colors">
                  Features
                </a>
              </li>
              <li>
                <a href="#how-it-works" className="hover:text-foreground transition-colors">
                  How It Works
                </a>
              </li>
              <li>
                <a href="#use-cases" className="hover:text-foreground transition-colors">
                  Use Cases
                </a>
              </li>
              <li>
                <a
                  href="https://play.google.com/store/apps/details?id=com.bernaferrari.changedetection"
                  target="_blank"
                  className="hover:text-foreground transition-colors"
                >
                  Google Play
                </a>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="font-semibold mb-4">Resources</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>
                <a
                  href="https://github.com/niceperson/changedetection"
                  target="_blank"
                  className="hover:text-foreground transition-colors"
                >
                  GitHub
                </a>
              </li>
              <li>
                <a
                  href="https://github.com/niceperson/changedetection/releases"
                  target="_blank"
                  className="hover:text-foreground transition-colors"
                >
                  Releases
                </a>
              </li>
              <li>
                <a
                  href="https://github.com/niceperson/changedetection/issues"
                  target="_blank"
                  className="hover:text-foreground transition-colors"
                >
                  Report Issue
                </a>
              </li>
              <li>
                <a
                  href="https://github.com/niceperson/changedetection/blob/main/LICENSE"
                  target="_blank"
                  className="hover:text-foreground transition-colors"
                >
                  License
                </a>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom */}
        <div className="pt-8 border-t border-border flex flex-col sm:flex-row justify-between items-center gap-4">
          <p className="text-xs text-muted-foreground">
            © {currentYear} Change Detection. Open source under Apache 2.0 License.
          </p>
          <p className="text-xs text-muted-foreground flex items-center gap-1">
            Made with <Heart size={12} className="text-red-500" /> using Kotlin Multiplatform
          </p>
        </div>
      </div>
    </footer>
  );
}
