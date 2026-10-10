import type { Metadata } from "next"

import { site } from "@/lib/site"

export const metadata: Metadata = {
  title: "Privacy",
  description: "Change Detection has no account and no tracking. Pages you watch and their screenshots stay on your phone.",
  alternates: { canonical: "/privacy" },
}

export default function PrivacyPage() {
  return (
    <div className="mx-auto max-w-2xl px-5 py-16 [&_h2]:mt-10 [&_h2]:font-heading [&_h2]:text-xl [&_h2]:font-bold [&_p]:mt-3 [&_p]:leading-relaxed [&_p]:text-muted-foreground">
      <h1 className="font-heading text-4xl font-bold tracking-tight">Privacy</h1>
      <p>The short version: {site.name} has no account, no analytics and no servers. Everything happens on your phone.</p>
      <h2>What the app stores</h2>
      <p>
        The addresses you watch, their saved versions (text or screenshots) and your settings are stored in the app&apos;s private storage on your
        device. Deleting a page or the app deletes them.
      </p>
      <h2>Network requests</h2>
      <p>
        To check a page, your phone loads it directly from that website, like a browser would. Page icons are loaded from the website they belong
        to. No data is sent to us or to third parties by the app.
      </p>
      <h2>Signed-in pages</h2>
      <p>If you sign in to a website in the app&apos;s built-in browser, its cookies are kept on your device so later checks can load the page as you.</p>
      <h2>Notifications</h2>
      <p>Change notifications are created on your phone. No push service is involved.</p>
      <h2>Source code</h2>
      <p>
        The app is open source, so all of this can be verified: <a className="text-primary underline" href={site.githubUrl}>{site.githubUrl}</a>.
      </p>
    </div>
  )
}
