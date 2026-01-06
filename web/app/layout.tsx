import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { cn } from "@/lib/utils";
import { ThemeProvider } from "@/components/theme-provider";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

export const metadata: Metadata = {
  title: "Change Detection – Never Miss a Website Update",
  description:
    "Stop refreshing websites manually. Get notified instantly when any website changes. Track prices, grades, news, job postings & more.",
  metadataBase: new URL("https://changedetection.app"),
  keywords: [
    "website monitor",
    "change detection",
    "price tracker",
    "website tracker",
    "page monitor",
    "website changes",
    "notification app",
  ],
  authors: [{ name: "Bernardo Ferrari" }],
  openGraph: {
    type: "website",
    url: "https://changedetection.app",
    title: "Change Detection – Never Miss a Website Update",
    description:
      "Stop refreshing websites manually. Get notified instantly when any website changes.",
    images: [{ url: "/assets/banner.png" }],
  },
  twitter: {
    card: "summary_large_image",
    title: "Change Detection – Never Miss a Website Update",
    description:
      "Stop refreshing websites manually. Get notified instantly when any website changes.",
    images: ["/assets/banner.png"],
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={cn(inter.variable, "antialiased min-h-screen")}>
        <ThemeProvider
          attribute="class"
          defaultTheme="dark"
          enableSystem
          disableTransitionOnChange
        >
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}
