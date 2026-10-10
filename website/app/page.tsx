import Link from "next/link"
import { ArrowRight, Eye, Fingerprint, Layers, Lock, MousePointerClick, Timer } from "lucide-react"

import { DiffCard } from "@/components/diff-card"
import { Faq } from "@/components/faq"
import { JsonLd } from "@/components/json-ld"
import { Phone } from "@/components/phone"
import { StoreButtons } from "@/components/store-buttons"
import { screens, site } from "@/lib/site"
import { useCases } from "@/lib/use-cases"

const faqs = [
  { q: "Do I need an account?", a: "No. Install the app and watch a page. There is no sign-up, and nothing to log in to." },
  { q: "Where are my pages and screenshots stored?", a: "On your phone. Pages are opened and compared on your device; nothing is uploaded to our servers, because there are none." },
  { q: "Does it work with pages that need a login?", a: "Yes. Pages open in a built-in browser, so you can sign in once. Later checks reuse that session from your phone." },
  { q: "How often are pages checked?", a: "On a schedule you choose, in the background. On iPhone, the system decides exactly when background checks run. Turbo checks every 30 seconds while the app is open." },
  { q: "Screenshots or text: which should I use?", a: "Screenshots compare how the page looks and catch images, layout and anything drawn by JavaScript. Text compares the words and is lighter on battery, great for prices, dates and lists. You can switch any time." },
  { q: "Won't a long news page always look \"changed\"?", a: "Content that only moved, like stories pushed down by a new banner, is recognised as moved, not changed. You can also mark noisy parts, like carousels or ad slots, to be ignored." },
  { q: "Is it open source?", a: "Yes. The app is open source under the Apache 2.0 license; the code is on GitHub." },
]

const steps = [
  { icon: MousePointerClick, title: "Open the page", body: "Type an address or share it from your browser. It opens in a real browser, so you can sign in, pick a size or set a filter first." },
  { icon: Fingerprint, title: "Pick what matters", body: "Tap the price, a list or a status message, and widen it with a slider. Or mark the noisy parts to ignore. Or just watch the whole page." },
  { icon: Eye, title: "See exactly what changed", body: "Get a notification and open a before/after of just the change: the old price struck through, the new one highlighted." },
]

export default function Home() {
  return (
    <>
      <JsonLd
        data={{
          "@context": "https://schema.org",
          "@type": "MobileApplication",
          name: site.name,
          operatingSystem: "Android, iOS",
          applicationCategory: "UtilitiesApplication",
          description: site.description,
          url: site.url,
          downloadUrl: site.playStoreUrl,
          license: "https://www.apache.org/licenses/LICENSE-2.0",
        }}
      />
      <JsonLd
        data={{
          "@context": "https://schema.org",
          "@type": "FAQPage",
          mainEntity: faqs.map((f) => ({ "@type": "Question", name: f.q, acceptedAnswer: { "@type": "Answer", text: f.a } })),
        }}
      />

      {/* Hero */}
      <section className="relative overflow-hidden">
        <div aria-hidden className="pointer-events-none absolute inset-x-0 -top-40 h-[36rem] bg-[radial-gradient(60%_60%_at_50%_0%,var(--brand-soft),transparent)]" />
        <div className="relative mx-auto grid max-w-6xl items-center gap-14 px-5 pt-14 pb-20 md:pt-20 lg:grid-cols-[1.05fr_1fr]">
          <div className="space-y-7">
            <p className="inline-flex items-center gap-2 rounded-full border bg-background/70 px-3 py-1 text-xs font-medium text-muted-foreground">
              <span className="size-1.5 rounded-full bg-primary" /> Website change alerts for Android and iPhone
            </p>
            <h1 className="font-heading text-5xl leading-[1.02] font-bold tracking-tight text-balance sm:text-6xl">
              Stop refreshing. <span className="text-primary">We&apos;ll tell you what changed.</span>
            </h1>
            <p className="max-w-xl text-lg leading-relaxed text-muted-foreground text-pretty">
              Watch any web page for price drops, restocks, new dates, job posts or results. Change Detection checks it for you and shows exactly
              what changed, old next to new, right on the page.
            </p>
            <StoreButtons />
            <p className="text-sm text-muted-foreground">No account · Open source · Everything stays on your phone</p>
          </div>
          <div className="relative mx-auto w-full max-w-md">
            <div className="grid grid-cols-2 items-start gap-5">
              <Phone screen={screens.home} priority className="mt-10" />
              <Phone screen={screens.changes} priority />
            </div>
            <DiffCard
              label="Price"
              before="$1,599.00"
              after="$1,349.00"
              context="Studio Display · store.example"
              className="absolute -bottom-6 -left-4 w-64 rotate-[-3deg] shadow-xl sm:-left-10"
            />
          </div>
        </div>
      </section>

      {/* How it works */}
      <section className="border-y bg-muted/30">
        <div className="mx-auto max-w-6xl px-5 py-20">
          <h2 className="font-heading text-3xl font-bold tracking-tight sm:text-4xl">Three steps. Then put your phone down.</h2>
          <ol className="mt-10 grid gap-5 md:grid-cols-3">
            {steps.map((step, index) => (
              <li key={step.title} className="rounded-3xl border bg-card p-6">
                <div className="mb-5 flex items-center gap-3">
                  <span className="grid size-10 place-items-center rounded-2xl bg-primary/10 text-primary"><step.icon className="size-5" /></span>
                  <span className="text-sm font-semibold text-muted-foreground">Step {index + 1}</span>
                </div>
                <h3 className="text-lg font-semibold">{step.title}</h3>
                <p className="mt-2 leading-relaxed text-muted-foreground">{step.body}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="mx-auto max-w-6xl scroll-mt-20 space-y-24 px-5 py-24">
        <Feature
          eyebrow="Screenshot diffs"
          title="Only what really changed, even on busy pages"
          body="Pixel comparisons light up a whole page when a banner pushes everything down. Change Detection lines up the two screenshots first, so moved content stays quiet, while a single changed digit in a price is still caught. Step through each change, or see them all marked on the full page."
          points={["Moved content is recognised as moved", "One-digit edits still detected", "Ads and cookie banners hidden from captures"]}
          screen={screens.page}
        />
        <Feature
          reverse
          eyebrow="Text that reads like an edit"
          title="The date that moved, struck through and replaced"
          body="For prices, dates and lists, text mode shows edits word by word. New lines, removed lines and number-only updates are grouped, and a list that was only re-ordered is called out instead of flooding you with changes."
          points={["Word-level edits", "Number-only updates folded away", "Re-ordered lists recognised"]}
          screen={screens.text}
        />
        <Feature
          eyebrow="Visual history"
          title="Every version, with what changed in it"
          body="Each saved version shows a thumbnail of what changed and a short summary, grouped by day. Jump to any version and compare it with the one before, or step back and forth with the arrows."
          points={["Versions grouped by day", "Thumbnails of each change", "Mark as read, undo any time"]}
          screen={screens.versions}
        />
      </section>

      {/* Small features */}
      <section className="mx-auto max-w-6xl px-5 pb-24">
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          <MiniFeature icon={Layers} title="Pick parts, uBlock-style" body="Tap any part of the page and widen the selection with a slider. Watch only it, or ignore it." />
          <MiniFeature icon={Lock} title="Works behind logins" body="Sign in once in the built-in browser; checks reuse the session from your device." />
          <MiniFeature icon={Timer} title="Turbo: every 30 seconds" body="For drops and results day. A countdown shows when the next check runs." />
          <MiniFeature icon={Fingerprint} title="Private by design" body="No account, no tracking. Pages and screenshots never leave your phone." />
        </div>
      </section>

      {/* Use cases */}
      <section className="border-y bg-muted/30">
        <div className="mx-auto max-w-6xl px-5 py-20">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <h2 className="font-heading text-3xl font-bold tracking-tight sm:text-4xl">What people watch</h2>
            <Link href="/use-cases" className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline">
              All use cases <ArrowRight className="size-4" />
            </Link>
          </div>
          <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {useCases.map((it) => (
              <Link key={it.slug} href={`/use-cases/${it.slug}`} className="group rounded-3xl border bg-card p-6 transition-colors hover:border-primary/40">
                <DiffCard label={it.example.label} before={it.example.before} after={it.example.after} className="mb-5 border-0 bg-muted/50 p-4 shadow-none" />
                <h3 className="font-semibold group-hover:text-primary">{it.title}</h3>
                <p className="mt-2 line-clamp-3 text-sm leading-relaxed text-muted-foreground">{it.description}</p>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* FAQ */}
      <section id="faq" className="mx-auto max-w-3xl scroll-mt-20 px-5 py-24">
        <h2 className="mb-8 font-heading text-3xl font-bold tracking-tight sm:text-4xl">Questions</h2>
        <Faq items={faqs} />
      </section>

      {/* Final CTA */}
      <section className="px-5 pb-24">
        <div className="mx-auto max-w-6xl rounded-[2.5rem] bg-primary px-8 py-16 text-center text-primary-foreground">
          <h2 className="font-heading text-3xl font-bold tracking-tight text-balance sm:text-5xl">Let the page tell you when it changes.</h2>
          <p className="mx-auto mt-4 max-w-xl text-primary-foreground/80">Free your evenings from refreshing tabs.</p>
          <StoreButtons className="mt-8 justify-center [&_a]:bg-background [&_a]:text-foreground [&_span.border]:border-primary-foreground/40 [&_span.border]:text-primary-foreground/80" />
        </div>
      </section>
    </>
  )
}

function Feature({
  eyebrow,
  title,
  body,
  points,
  screen,
  reverse = false,
}: {
  eyebrow: string
  title: string
  body: string
  points: string[]
  screen: (typeof screens)[keyof typeof screens]
  reverse?: boolean
}) {
  return (
    <div className="grid items-center gap-12 md:grid-cols-2">
      <div className={reverse ? "md:order-2" : undefined}>
        <p className="text-sm font-semibold text-primary">{eyebrow}</p>
        <h2 className="mt-2 font-heading text-3xl font-bold tracking-tight text-balance sm:text-4xl">{title}</h2>
        <p className="mt-4 text-lg leading-relaxed text-muted-foreground text-pretty">{body}</p>
        <ul className="mt-6 space-y-2">
          {points.map((point) => (
            <li key={point} className="flex items-center gap-3">
              <span className="size-2 rounded-full bg-highlight ring-4 ring-highlight/25" />
              {point}
            </li>
          ))}
        </ul>
      </div>
      <div className={reverse ? "md:order-1" : undefined}>
        <Phone screen={screen} className="mx-auto w-full max-w-[300px]" />
      </div>
    </div>
  )
}

function MiniFeature({ icon: Icon, title, body }: { icon: typeof Lock; title: string; body: string }) {
  return (
    <div className="rounded-3xl border bg-card p-6">
      <span className="grid size-10 place-items-center rounded-2xl bg-primary/10 text-primary"><Icon className="size-5" /></span>
      <h3 className="mt-5 font-semibold">{title}</h3>
      <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{body}</p>
    </div>
  )
}
