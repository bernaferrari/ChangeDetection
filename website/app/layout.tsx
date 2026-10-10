import type { Metadata, Viewport } from "next"
import localFont from "next/font/local"

import "./globals.css"
import { SiteFooter } from "@/components/site-footer"
import { SiteHeader } from "@/components/site-header"
import { ThemeProvider } from "@/components/theme-provider"
import { site } from "@/lib/site"
import { cn } from "@/lib/utils"

// Google Sans Flex, the typeface the app uses (SIL Open Font License, see fonts/GoogleSansFlex-OFL.txt).
const sans = localFont({
  src: [
    { path: "./fonts/gsf_regular.ttf", weight: "400" },
    { path: "./fonts/gsf_medium.ttf", weight: "500" },
    { path: "./fonts/gsf_semibold.ttf", weight: "600" },
    { path: "./fonts/gsf_bold.ttf", weight: "700" },
  ],
  variable: "--font-sans",
  display: "swap",
})

const display = localFont({
  src: [{ path: "./fonts/gsf_display_bold.ttf", weight: "700" }],
  variable: "--font-display",
  display: "swap",
})

export const metadata: Metadata = {
  metadataBase: new URL(site.url),
  title: {
    default: `${site.name}: website change alerts with visual diffs`,
    template: `%s · ${site.name}`,
  },
  description: site.description,
  applicationName: site.name,
  keywords: [...site.keywords],
  alternates: { canonical: "/" },
  openGraph: {
    type: "website",
    siteName: site.name,
    title: `${site.name}: website change alerts with visual diffs`,
    description: site.description,
    url: "/",
  },
  twitter: { card: "summary_large_image" },
  robots: { index: true, follow: true },
  other: { "google-play-app": "app-id=com.bernaferrari.changedetection" },
}

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#ffffff" },
    { media: "(prefers-color-scheme: dark)", color: "#0b0b0f" },
  ],
}

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" suppressHydrationWarning className={cn("antialiased", sans.variable, display.variable, "font-sans")}>
      <body className="flex min-h-svh flex-col">
        <ThemeProvider>
          <SiteHeader />
          <main className="flex-1">{children}</main>
          <SiteFooter />
        </ThemeProvider>
      </body>
    </html>
  )
}
