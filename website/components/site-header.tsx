import Image from "next/image"
import Link from "next/link"

import { buttonVariants } from "@/components/ui/button"
import { site } from "@/lib/site"
import { cn } from "@/lib/utils"

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-40 border-b border-transparent bg-background/80 backdrop-blur-md supports-[backdrop-filter]:bg-background/70">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-5">
        <Link href="/" className="flex items-center gap-2.5 font-heading text-[17px] font-bold tracking-tight">
          <Image src="/logo.png" alt="" width={32} height={32} className="rounded-[9px]" />
          {site.name}
        </Link>
        <nav aria-label="Main" className="ml-auto hidden items-center gap-1 text-sm text-muted-foreground md:flex">
          <Link href="/#features" className="rounded-lg px-3 py-2 hover:text-foreground">Features</Link>
          <Link href="/use-cases" className="rounded-lg px-3 py-2 hover:text-foreground">Use cases</Link>
          <Link href="/#faq" className="rounded-lg px-3 py-2 hover:text-foreground">FAQ</Link>
          <a href={site.githubUrl} className="rounded-lg px-3 py-2 hover:text-foreground">GitHub</a>
        </nav>
        <a href={site.playStoreUrl} className={cn(buttonVariants({ size: "lg" }), "ml-auto h-10 rounded-full px-5 md:ml-0")}>
          Get the app
        </a>
      </div>
    </header>
  )
}
