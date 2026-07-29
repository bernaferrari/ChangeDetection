import type { Metadata } from "next";
import "./globals.css";
import { ThemeProvider } from "@/components/theme-provider";
import { MotionProvider } from "@/components/motion-provider";

export const metadata: Metadata = {
  title: "Change Detection – Never Miss a Website Update",
  description:
    "Stop refreshing websites manually. Get notified when a page you monitor changes. Track prices, grades, news, job postings and more.",
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
      "Stop refreshing websites manually. Get notified when a page you monitor changes.",
  },
  twitter: {
    card: "summary_large_image",
    title: "Change Detection – Never Miss a Website Update",
    description:
      "Stop refreshing websites manually. Get notified when a page you monitor changes.",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="antialiased min-h-screen">
        <ThemeProvider
          attribute="class"
          defaultTheme="dark"
          enableSystem
          disableTransitionOnChange
        >
          <MotionProvider>{children}</MotionProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
