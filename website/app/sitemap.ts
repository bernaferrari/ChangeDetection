import type { MetadataRoute } from "next"

import { site } from "@/lib/site"
import { useCases } from "@/lib/use-cases"

export default function sitemap(): MetadataRoute.Sitemap {
  const now = new Date()
  return [
    { url: site.url, lastModified: now, changeFrequency: "weekly", priority: 1 },
    { url: `${site.url}/use-cases`, lastModified: now, changeFrequency: "monthly", priority: 0.8 },
    ...useCases.map((it) => ({ url: `${site.url}/use-cases/${it.slug}`, lastModified: now, changeFrequency: "monthly" as const, priority: 0.7 })),
    { url: `${site.url}/privacy`, lastModified: now, changeFrequency: "yearly", priority: 0.3 },
  ]
}
