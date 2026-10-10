import { site } from "@/lib/site"
import { cn } from "@/lib/utils"

function GooglePlayMark() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden className="size-6">
      <path fill="currentColor" d="M3.6 1.8c-.3.3-.5.8-.5 1.4v17.6c0 .6.2 1.1.5 1.4l.1.1 9.9-9.9v-.2L3.7 1.7l-.1.1Zm13.3 13.9-3.3-3.3v-.2l3.3-3.3.1.1 3.9 2.2c1.1.6 1.1 1.7 0 2.3l-3.9 2.2-.1 0Zm-.1.1-3.4-3.4L3.6 22.2c.4.4 1 .4 1.7.1l11.5-6.5Zm0-7.6L5.3 1.7C4.6 1.3 4 1.4 3.6 1.8l9.8 9.8 3.4-3.2Z" />
    </svg>
  )
}

function AppleMark() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden className="size-6">
      <path fill="currentColor" d="M16.4 12.6c0-2.6 2.1-3.8 2.2-3.9-1.2-1.8-3.1-2-3.7-2-1.6-.2-3.1.9-3.9.9-.8 0-2-.9-3.4-.9-1.7 0-3.3 1-4.2 2.6-1.8 3.1-.5 7.7 1.3 10.2.9 1.2 1.9 2.6 3.2 2.6 1.3-.1 1.8-.8 3.3-.8s2 .8 3.4.8c1.4 0 2.3-1.3 3.1-2.5 1-1.4 1.4-2.8 1.4-2.9 0 0-2.7-1-2.7-4.1ZM13.9 4.9c.7-.9 1.2-2 1-3.2-1 0-2.3.7-3 1.6-.7.8-1.3 2-1.1 3.1 1.2.1 2.3-.6 3.1-1.5Z" />
    </svg>
  )
}

const pill = "inline-flex h-14 items-center gap-3 rounded-2xl px-5 text-left transition-colors"

export function StoreButtons({ className }: { className?: string }) {
  return (
    <div className={cn("flex flex-wrap items-center gap-3", className)}>
      <a href={site.playStoreUrl} className={cn(pill, "bg-foreground text-background hover:bg-foreground/85")} aria-label="Get it on Google Play">
        <GooglePlayMark />
        <span className="leading-tight">
          <span className="block text-[11px] opacity-80">Get it on</span>
          <span className="block text-base font-semibold">Google Play</span>
        </span>
      </a>
      {site.appStoreUrl ? (
        <a href={site.appStoreUrl} className={cn(pill, "bg-foreground text-background hover:bg-foreground/85")} aria-label="Download on the App Store">
          <AppleMark />
          <span className="leading-tight">
            <span className="block text-[11px] opacity-80">Download on the</span>
            <span className="block text-base font-semibold">App Store</span>
          </span>
        </a>
      ) : (
        <span className={cn(pill, "border border-dashed text-muted-foreground")}>
          <AppleMark />
          <span className="leading-tight">
            <span className="block text-[11px]">iPhone</span>
            <span className="block text-base font-semibold">Coming soon</span>
          </span>
        </span>
      )}
    </div>
  )
}
