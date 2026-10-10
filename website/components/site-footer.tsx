import Link from "next/link"

import { site } from "@/lib/site"
import { useCases } from "@/lib/use-cases"

export function SiteFooter() {
  return (
    <footer className="border-t bg-muted/30">
      <div className="mx-auto grid max-w-6xl gap-10 px-5 py-14 sm:grid-cols-2 lg:grid-cols-4">
        <div className="space-y-3">
          <p className="font-heading text-lg font-bold">{site.name}</p>
          <p className="text-sm text-muted-foreground">{site.tagline}</p>
        </div>
        <div>
          <p className="mb-3 text-sm font-semibold">Use cases</p>
          <ul className="space-y-2 text-sm text-muted-foreground">
            {useCases.slice(0, 5).map((it) => (
              <li key={it.slug}><Link href={`/use-cases/${it.slug}`} className="hover:text-foreground">{it.title}</Link></li>
            ))}
          </ul>
        </div>
        <div>
          <p className="mb-3 text-sm font-semibold">Product</p>
          <ul className="space-y-2 text-sm text-muted-foreground">
            <li><Link href="/#features" className="hover:text-foreground">Features</Link></li>
            <li><Link href="/#faq" className="hover:text-foreground">FAQ</Link></li>
            <li><a href={site.playStoreUrl} className="hover:text-foreground">Google Play</a></li>
            <li><a href={site.githubUrl} className="hover:text-foreground">Source code</a></li>
          </ul>
        </div>
        <div>
          <p className="mb-3 text-sm font-semibold">Legal</p>
          <ul className="space-y-2 text-sm text-muted-foreground">
            <li><Link href="/privacy" className="hover:text-foreground">Privacy</Link></li>
          </ul>
        </div>
      </div>
      <p className="pb-10 text-center text-xs text-muted-foreground">Open source under the Apache 2.0 license.</p>
    </footer>
  )
}
