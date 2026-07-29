import Link from "next/link";
import {
  ArrowLeft,
  CheckCircle2,
  Download,
  Zap,
  Bell,
  Settings,
  MousePointer2,
  Smartphone,
} from "lucide-react";
import { Navbar } from "@/components/navbar";
import { Footer } from "@/components/footer";
import { Button } from "@/components/ui/button";

export default function DocsPage() {
  return (
    <>
      <Navbar />
      <main className="pt-32 pb-24 px-6 min-h-screen">
        <div className="max-w-3xl mx-auto">
          <Link
            href="/"
            className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground mb-12 transition-colors group"
          >
            <ArrowLeft
              size={16}
              className="mr-2 group-hover:-translate-x-1 transition-transform"
            />
            Back to Home
          </Link>

          <header className="mb-16">
            <h1 className="text-4xl md:text-5xl font-bold mb-6 tracking-tight">
              Documentation
            </h1>
            <p className="text-xl text-muted-foreground leading-relaxed">
              Everything you need to know about monitoring websites and getting
              notified when they change.
            </p>
          </header>

          <article className="prose prose-neutral dark:prose-invert prose-lg max-w-none">
            <section className="mb-16">
              <div className="flex items-center gap-3 mb-6">
                <div className="p-2 rounded-lg bg-primary/10 text-primary">
                  <Download size={24} />
                </div>
                <h2 className="text-2xl font-bold m-0">Getting Started</h2>
              </div>

              <div className="glass p-6 not-prose mb-6">
                <div className="flex flex-col md:flex-row gap-4 items-start md:items-center justify-between">
                  <div>
                    <h3 className="text-lg font-semibold mb-1">
                      Download Change Detection
                    </h3>
                    <p className="text-sm text-muted-foreground">
                      Available on Google Play · Android 11+
                    </p>
                  </div>
                  <Button asChild>
                    <a href="https://play.google.com/store/apps/details?id=com.bernaferrari.changedetection">
                      <Smartphone size={16} className="mr-2" />
                      Google Play
                    </a>
                  </Button>
                </div>
              </div>

              <p>
                Change Detection is free, open source, and requires no account.
                Simply install the app, add your first website, and you’re ready
                to go.
              </p>
            </section>

            <section className="mb-16">
              <div className="flex items-center gap-3 mb-6">
                <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-500">
                  <MousePointer2 size={24} />
                </div>
                <h2 className="text-2xl font-bold m-0">Adding a Website</h2>
              </div>

              <p>
                Add a public website in two quick steps:
              </p>

              <ol className="space-y-2">
                <li>
                  <strong>Enter the URL:</strong> Paste the address and preview the
                  public page you want to monitor.
                </li>
                <li>
                  <strong>Name and save it:</strong> Give the site a recognizable
                  name and choose its color. The first snapshot starts immediately.
                </li>
              </ol>

              <div className="not-prose glass p-4 my-6 mt-8 flex gap-4 items-start border-l-4 border-l-emerald-500">
                <CheckCircle2
                  className="text-emerald-500 shrink-0 mt-0.5"
                  size={20}
                />
                <div>
                  <h4 className="font-semibold text-foreground mb-1">
                    Tip: Choose a stable URL
                  </h4>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Change Detection currently compares the complete public page.
                    Prefer a focused product, result, or article URL to reduce
                    unrelated changes from navigation and rotating content.
                  </p>
                </div>
              </div>
            </section>

            <section className="mb-16">
              <div className="flex items-center gap-3 mb-6">
                <div className="p-2 rounded-lg bg-amber-500/10 text-amber-500">
                  <Zap size={24} />
                </div>
                <h2 className="text-2xl font-bold m-0">Turbo Mode</h2>
              </div>

              <p>
                For time-sensitive monitoring, enable <strong>Turbo Mode</strong>{" "}
                to check websites more frequently:
              </p>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 not-prose my-6">
                <div className="p-3 rounded-lg border border-border bg-card text-center">
                  <span className="text-lg font-bold text-primary">15s</span>
                  <p className="text-xs text-muted-foreground mt-1">Minimum</p>
                </div>
                <div className="p-3 rounded-lg border border-border bg-card text-center">
                  <span className="text-lg font-bold text-primary">30s</span>
                  <p className="text-xs text-muted-foreground mt-1">Fast</p>
                </div>
                <div className="p-3 rounded-lg border border-border bg-card text-center">
                  <span className="text-lg font-bold text-primary">1m</span>
                  <p className="text-xs text-muted-foreground mt-1">Standard</p>
                </div>
                <div className="p-3 rounded-lg border border-border bg-card text-center">
                  <span className="text-lg font-bold text-primary">2m</span>
                  <p className="text-xs text-muted-foreground mt-1">Balanced</p>
                </div>
              </div>

              <p>
                Turbo Mode is perfect for catching flash sales, ticket drops, or
                any scenario where seconds matter.
              </p>
            </section>

            <section className="mb-16">
              <div className="flex items-center gap-3 mb-6">
                <div className="p-2 rounded-lg bg-blue-500/10 text-blue-500">
                  <Bell size={24} />
                </div>
                <h2 className="text-2xl font-bold m-0">Notifications</h2>
              </div>

              <p>
                When a change is detected, you’ll receive a notification with:
              </p>

              <ul className="space-y-2">
                <li>The website name and URL</li>
                <li>When the change was detected</li>
                <li>The site whose content changed</li>
              </ul>

              <p>
                Tap the notification to open the app and see a detailed
                side-by-side comparison of the changes.
              </p>
            </section>

            <section className="mb-16">
              <div className="flex items-center gap-3 mb-6">
                <div className="p-2 rounded-lg bg-violet-500/10 text-violet-500">
                  <Settings size={24} />
                </div>
                <h2 className="text-2xl font-bold m-0">Sync Constraints</h2>
              </div>

              <p>
                To preserve battery life, you can configure when background
                syncing should occur:
              </p>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 not-prose">
                <div className="p-4 rounded-lg border border-border bg-card">
                  <span className="text-sm font-bold">WiFi Only</span>
                  <p className="text-sm text-muted-foreground mt-1">
                    Only sync when connected to WiFi
                  </p>
                </div>
                <div className="p-4 rounded-lg border border-border bg-card">
                  <span className="text-sm font-bold">While Charging</span>
                  <p className="text-sm text-muted-foreground mt-1">
                    Only sync when device is charging
                  </p>
                </div>
                <div className="p-4 rounded-lg border border-border bg-card">
                  <span className="text-sm font-bold">Device Idle</span>
                  <p className="text-sm text-muted-foreground mt-1">
                    Only sync when you’re not using the device
                  </p>
                </div>
                <div className="p-4 rounded-lg border border-border bg-card">
                  <span className="text-sm font-bold">Battery Level</span>
                  <p className="text-sm text-muted-foreground mt-1">
                    Pause when Android reports the battery is low
                  </p>
                </div>
              </div>
            </section>

            <section>
              <h2 className="text-2xl font-bold mb-6">FAQ</h2>

              <div className="space-y-6 not-prose">
                <div className="glass p-6">
                  <h3 className="font-semibold text-foreground mb-2">
                    Does it work with login-required pages?
                  </h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Not currently. Monitoring supports public pages; authenticated
                    sessions and saved cookies are not included in this release.
                  </p>
                </div>

                <div className="glass p-6">
                  <h3 className="font-semibold text-foreground mb-2">
                    How much battery does it use?
                  </h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Very little for normal use. The app uses Android’s WorkManager
                    for efficient background scheduling. Turbo Mode uses more
                    battery but is designed for short-term intensive monitoring.
                  </p>
                </div>

                <div className="glass p-6">
                  <h3 className="font-semibold text-foreground mb-2">
                    Is my data private?
                  </h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Absolutely. All monitoring happens locally on your device.
                    We don’t have servers, we don’t collect data, and we don’t
                    track you. The app is 100% open source.
                  </p>
                </div>
              </div>
            </section>
          </article>
        </div>
      </main>
      <Footer />
    </>
  );
}
