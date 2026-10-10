/**
 * One place for everything the site says about the app. Set NEXT_PUBLIC_SITE_URL to the production
 * domain before deploying: canonical URLs, the sitemap and social cards are all built from it.
 */
export const site = {
  name: "Change Detection",
  tagline: "Stop refreshing. We'll tell you what changed.",
  description:
    "Watch any web page and get told exactly what changed: prices, restocks, dates, job posts and results. Visual screenshot diffs that ignore content that only moved, works with pages that need a login, and everything stays on your phone.",
  url: (process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000").replace(/\/$/, ""),
  playStoreUrl: "https://play.google.com/store/apps/details?id=com.bernaferrari.changedetection",
  /** Set once the iPhone app is live on the App Store. */
  appStoreUrl: process.env.NEXT_PUBLIC_APP_STORE_URL ?? null,
  githubUrl: "https://github.com/bernaferrari/ChangeDetection",
  keywords: [
    "website change detection",
    "web page monitor",
    "price drop alert",
    "restock alert",
    "website change alert app",
    "visual website diff",
    "page change notification",
    "monitor website for changes",
  ],
} as const

export type Screen = { src: string; alt: string; width: number; height: number }

const phone = { width: 1080, height: 2340 }

/** Real renders of the app (example data, no third-party content). */
export const screens = {
  home: { src: "/screens/home.png", alt: "Home screen listing three pages with their latest changes, including a price drop shown before and after", ...phone },
  changes: { src: "/screens/compare-changes.png", alt: "Comparison of two screenshots showing only the changed price, before and after", ...phone },
  page: { src: "/screens/compare-page.png", alt: "Full page screenshot with the changed price highlighted in yellow", ...phone },
  text: { src: "/screens/compare-text.png", alt: "Text comparison showing an application deadline moved from 31 October to 14 November", ...phone },
  versions: { src: "/screens/versions.png", alt: "List of saved versions grouped by day, each with a thumbnail of what changed", ...phone },
  turbo: { src: "/screens/turbo.png", alt: "Turbo mode checking pages every 30 seconds with a countdown to the next check", ...phone },
  detail: { src: "/screens/detail.png", alt: "Page detail with the latest change, a 30 day calendar and saved versions", ...phone },
} satisfies Record<string, Screen>
