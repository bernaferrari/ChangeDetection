import type { Metadata } from "next"
import Link from "next/link"

import { DiffCard } from "@/components/diff-card"
import { useCases } from "@/lib/use-cases"

export const metadata: Metadata = {
  title: "Use cases: price drops, restocks, job posts and more",
  description: "How people use Change Detection: price drop and restock alerts, job postings, exam results, deadline changes and visual website monitoring.",
  alternates: { canonical: "/use-cases" },
}

export default function UseCasesPage() {
  return (
    <div className="mx-auto max-w-6xl px-5 py-16">
      <h1 className="font-heading text-4xl font-bold tracking-tight sm:text-5xl">What will you watch?</h1>
      <p className="mt-4 max-w-2xl text-lg text-muted-foreground">Any page with something you care about. A few favourites, with how to set each one up.</p>
      <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {useCases.map((it) => (
          <Link key={it.slug} href={`/use-cases/${it.slug}`} className="group rounded-3xl border bg-card p-6 transition-colors hover:border-primary/40">
            <DiffCard label={it.example.label} before={it.example.before} after={it.example.after} className="mb-5 border-0 bg-muted/50 p-4 shadow-none" />
            <h2 className="font-semibold group-hover:text-primary">{it.title}</h2>
            <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{it.description}</p>
          </Link>
        ))}
      </div>
    </div>
  )
}
