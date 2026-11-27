import type { Metadata } from "next"

import { site } from "@/lib/site"

export const metadata: Metadata = {
  title: "Privacy",
  description: "How Change Detection handles watched pages, saved sign-ins, diagnostic reports and backup files.",
  alternates: { canonical: "/privacy" },
}

export default function PrivacyPage() {
  return (
    <div className="mx-auto max-w-2xl px-5 py-16 [&_h2]:mt-10 [&_h2]:font-heading [&_h2]:text-xl [&_h2]:font-bold [&_p]:mt-3 [&_p]:leading-relaxed [&_p]:text-muted-foreground">
      <h1 className="font-heading text-4xl font-bold tracking-tight">Privacy</h1>
      <p>{site.name} processes and stores watches on your device. The app has no accounts, analytics or Change Detection servers.</p>
      <h2>What the app stores</h2>
      <p>
        The addresses you watch, their saved versions (text or screenshots) and your settings are stored in the app&apos;s private storage on your
        device. Deleting a page removes its saved versions. Deleting the app removes its private data.
      </p>
      <h2>Network requests</h2>
      <p>
        Your device loads watched pages directly, including resources and page icons they refer to. The website and its infrastructure may receive
        your IP address, user agent and request time. This includes local or self-hosted addresses you choose to watch.
      </p>
      <h2>Signed-in pages</h2>
      <p>
        The built-in browser keeps cookies and site data in private storage on your device so later checks can reuse your session. On Android, you
        can clear them in Settings under Saved sign-ins.
      </p>
      <h2>Diagnostic reports</h2>
      <p>
        On Android, the app keeps a short record of recent checks, including timings and error categories. The report is designed to omit watched
        addresses and page content. When you choose Send report, the app shows the report before opening your email or sharing app.
      </p>
      <h2>Android backup and transfer</h2>
      <p>Watched pages, saved versions, settings and browser sessions are excluded from Android cloud backups and device-to-device transfer.</p>
      <h2>Backup files you create</h2>
      <p>
        In Android Settings, you can save a backup to a location you choose. A pages-and-settings backup contains watched addresses, page options,
        notes and app settings. A full backup also includes saved HTML and screenshots, which may contain private content from signed-in pages.
        Both files are unencrypted and exclude browser cookies and session data. Change Detection never uploads them.
      </p>
      <h2>Notifications</h2>
      <p>Change notifications are created on your phone. No push service is involved.</p>
      <h2>Source code</h2>
      <p>
        Read the <a className="text-primary underline" href={site.githubUrl}>Change Detection source code on GitHub</a>.
      </p>
    </div>
  )
}
