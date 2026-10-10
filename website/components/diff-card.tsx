import { cn } from "@/lib/utils"

/** The app's signature: the old value struck through, the new one highlighted. */
export function DiffCard({
  label,
  before,
  after,
  context,
  className,
}: {
  label: string
  before: string
  after: string
  context?: string
  className?: string
}) {
  return (
    <div className={cn("rounded-3xl border bg-card p-5 shadow-sm", className)}>
      {context ? <p className="mb-3 text-xs font-medium text-muted-foreground">{context}</p> : null}
      <p className="mb-1 text-xs font-semibold tracking-wide text-primary uppercase">{label}</p>
      <p className="text-lg leading-snug font-medium text-pretty">
        <del className="text-removed decoration-2">{before}</del>{" "}
        <ins className="rounded-md bg-highlight px-1 text-highlight-foreground no-underline [box-decoration-break:clone]">{after}</ins>
      </p>
    </div>
  )
}
