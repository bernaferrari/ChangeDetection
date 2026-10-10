import type { Metadata } from "next"
import Link from "next/link"
import { notFound } from "next/navigation"
import { ArrowLeft, Check } from "lucide-react"

import { DiffCard } from "@/components/diff-card"
import { Faq } from "@/components/faq"
import { JsonLd } from "@/components/json-ld"
import { Phone } from "@/components/phone"
import { StoreButtons } from "@/components/store-buttons"
import { site } from "@/lib/site"
import { findUseCase, useCases } from "@/lib/use-cases"

export const dynamicParams = false

export function generateStaticParams() {
  return useCases.map((it) => ({ slug: it.slug }))
}

export async function generateMetadata({ params }: PageProps<"/use-cases/[slug]">): Promise<Metadata> {
  const { slug } = await params
  const useCase = findUseCase(slug)
  if (!useCase) return {}
  return {
    title: useCase.title,
    description: useCase.description,
    alternates: { canonical: `/use-cases/${useCase.slug}` },
    openGraph: { title: useCase.title, description: useCase.description, url: `/use-cases/${useCase.slug}` },
  }
}

export default async function UseCasePage({ params }: PageProps<"/use-cases/[slug]">) {
  const { slug } = await params
  const useCase = findUseCase(slug)
  if (!useCase) notFound()
  const others = useCases.filter((it) => it.slug !== useCase.slug).slice(0, 3)
  return (
    <article>
      <JsonLd
        data={{
          "@context": "https://schema.org",
          "@type": "HowTo",
          name: useCase.h1,
          description: useCase.description,
          step: useCase.steps.map((s, i) => ({ "@type": "HowToStep", position: i + 1, name: s.title, text: s.body })),
        }}
      />
      <JsonLd
        data={{
          "@context": "https://schema.org",
          "@type": "BreadcrumbList",
          itemListElement: [
            { "@type": "ListItem", position: 1, name: "Home", item: site.url },
            { "@type": "ListItem", position: 2, name: "Use cases", item: `${site.url}/use-cases` },
            { "@type": "ListItem", position: 3, name: useCase.title, item: `${site.url}/use-cases/${useCase.slug}` },
          ],
        }}
      />
      <div className="mx-auto grid max-w-6xl items-center gap-12 px-5 pt-10 pb-16 lg:grid-cols-[1.2fr_1fr]">
        <div>
          <Link href="/use-cases" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
            <ArrowLeft className="size-4" /> Use cases
          </Link>
          <h1 className="mt-6 font-heading text-4xl leading-tight font-bold tracking-tight text-balance sm:text-5xl">{useCase.h1}</h1>
          <p className="mt-5 text-lg leading-relaxed text-muted-foreground text-pretty">{useCase.intro}</p>
          <DiffCard {...useCase.example} className="mt-8 max-w-md" />
          <StoreButtons className="mt-8" />
        </div>
        <Phone screen={useCase.screen} priority className="mx-auto w-full max-w-[300px]" />
      </div>

      <section className="border-y bg-muted/30">
        <div className="mx-auto max-w-6xl px-5 py-16">
          <h2 className="font-heading text-3xl font-bold tracking-tight">How to set it up</h2>
          <ol className="mt-8 grid gap-5 md:grid-cols-3">
            {useCase.steps.map((step, index) => (
              <li key={step.title} className="rounded-3xl border bg-card p-6">
                <span className="text-sm font-semibold text-primary">Step {index + 1}</span>
                <h3 className="mt-2 text-lg font-semibold">{step.title}</h3>
                <p className="mt-2 leading-relaxed text-muted-foreground">{step.body}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section className="mx-auto grid max-w-6xl gap-12 px-5 py-16 md:grid-cols-2">
        <div>
          <h2 className="font-heading text-2xl font-bold tracking-tight">Tips</h2>
          <ul className="mt-6 space-y-4">
            {useCase.tips.map((tip) => (
              <li key={tip} className="flex gap-3 leading-relaxed">
                <Check className="mt-1 size-5 shrink-0 text-primary" />
                <span>{tip}</span>
              </li>
            ))}
          </ul>
        </div>
        <div>
          <h2 className="mb-6 font-heading text-2xl font-bold tracking-tight">Questions</h2>
          <Faq items={useCase.faqs} />
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-5 pb-20">
        <h2 className="font-heading text-2xl font-bold tracking-tight">More ways to use it</h2>
        <div className="mt-6 grid gap-5 sm:grid-cols-3">
          {others.map((it) => (
            <Link key={it.slug} href={`/use-cases/${it.slug}`} className="rounded-3xl border bg-card p-5 transition-colors hover:border-primary/40">
              <h3 className="font-semibold">{it.title}</h3>
              <p className="mt-2 line-clamp-2 text-sm text-muted-foreground">{it.description}</p>
            </Link>
          ))}
        </div>
      </section>
    </article>
  )
}
