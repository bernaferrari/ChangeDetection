import {
  BellRing,
  CheckCircle2,
  ChevronRight,
  CircleDotDashed,
  Globe2,
  History,
  Plus,
  ScanSearch,
  TrendingDown,
} from "lucide-react";

type ProductPreviewProps = {
  variant: "monitoring" | "comparison" | "history";
  compact?: boolean;
};

const monitoringRows = [
  {
    title: "Garden store",
    detail: "Price changed · 2 minutes ago",
    icon: TrendingDown,
    tone: "bg-emerald-500/15 text-emerald-700 dark:text-emerald-300",
  },
  {
    title: "City library",
    detail: "No changes · Today, 09:14",
    icon: CheckCircle2,
    tone: "bg-sky-500/15 text-sky-700 dark:text-sky-300",
  },
  {
    title: "Local news",
    detail: "Checking now",
    icon: CircleDotDashed,
    tone: "bg-amber-500/15 text-amber-700 dark:text-amber-300",
  },
];

export function ProductPreview({ variant, compact = false }: ProductPreviewProps) {
  const padding = compact ? "p-3" : "p-4 sm:p-5";

  if (variant === "comparison") {
    return (
      <div className={`preview-shell ${padding}`} aria-label="Illustration of a text comparison">
        <PreviewHeader icon={ScanSearch} title="Price watch" action="Compare" />
        <div className="mt-4 grid gap-3 text-left text-[11px] leading-5 sm:grid-cols-2 sm:text-xs">
          <DiffPane
            label="Previous"
            rows={[
              ["Product · Daily planner", "neutral"],
              ["Price · $29.00", "removed"],
              ["Delivery · Standard", "removed"],
            ]}
          />
          <DiffPane
            label="Latest"
            rows={[
              ["Product · Daily planner", "neutral"],
              ["Price · $21.00", "added"],
              ["Delivery · Free", "added"],
            ]}
          />
        </div>
        <div className="mt-3 flex items-center justify-between rounded-xl bg-muted/70 px-3 py-2 text-[11px] text-muted-foreground">
          <span className="inline-flex items-center gap-1.5"><History size={13} /> 2 changes found</span>
          <span className="font-medium text-emerald-700 dark:text-emerald-300">Save $8.00</span>
        </div>
      </div>
    );
  }

  if (variant === "history") {
    return (
      <div className={`preview-shell ${padding}`} aria-label="Illustration of snapshot history">
        <PreviewHeader icon={History} title="Garden store" action="History" />
        <div className="mt-4 space-y-2.5 text-left">
          {[
            ["Today, 10:42", "Price changed", "New"],
            ["Yesterday, 16:08", "No changes", "Saved"],
            ["Monday, 09:30", "Stock updated", "Saved"],
          ].map(([time, detail, state], index) => (
            <div key={time} className="flex items-center gap-3 rounded-xl border border-border/70 bg-card px-3 py-2.5">
              <div className={`flex size-8 shrink-0 items-center justify-center rounded-full ${index === 0 ? "bg-primary/15 text-primary" : "bg-muted text-muted-foreground"}`}>
                {index === 0 ? <BellRing size={15} /> : <History size={15} />}
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-medium text-foreground">{detail}</p>
                <p className="mt-0.5 text-[11px] text-muted-foreground">{time}</p>
              </div>
              <span className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${index === 0 ? "bg-primary/10 text-primary" : "bg-muted text-muted-foreground"}`}>{state}</span>
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className={`preview-shell ${padding}`} aria-label="Illustration of monitored websites">
      <PreviewHeader icon={Globe2} title="Tracked pages" action="3 active" />
      <div className="mt-4 space-y-2.5 text-left">
        {monitoringRows.map(({ title, detail, icon: Icon, tone }) => (
          <div key={title} className="flex items-center gap-3 rounded-xl border border-border/70 bg-card px-3 py-2.5 shadow-[0_1px_0_rgb(15_23_42/0.02)]">
            <div className={`flex size-8 shrink-0 items-center justify-center rounded-lg ${tone}`}>
              <Icon size={16} />
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-xs font-medium text-foreground">{title}</p>
              <p className="mt-0.5 truncate text-[11px] text-muted-foreground">{detail}</p>
            </div>
            <ChevronRight size={16} className="shrink-0 text-muted-foreground" />
          </div>
        ))}
      </div>
      <div className="mt-3 flex items-center justify-center gap-1.5 rounded-xl bg-primary px-3 py-2 text-xs font-medium text-primary-foreground">
        <Plus size={15} /> Add a page
      </div>
    </div>
  );
}

function PreviewHeader({
  icon: Icon,
  title,
  action,
}: {
  icon: typeof Globe2;
  title: string;
  action: string;
}) {
  return (
    <div className="flex items-center justify-between gap-3">
      <div className="flex min-w-0 items-center gap-2">
        <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/15 text-primary">
          <Icon size={16} />
        </div>
        <span className="truncate text-sm font-semibold text-foreground">{title}</span>
      </div>
      <span className="shrink-0 rounded-full bg-muted px-2.5 py-1 text-[10px] font-medium text-muted-foreground">{action}</span>
    </div>
  );
}

function DiffPane({
  label,
  rows,
}: {
  label: string;
  rows: [string, "neutral" | "added" | "removed"][];
}) {
  return (
    <div className="overflow-hidden rounded-xl border border-border/70 bg-card">
      <div className="border-b border-border/70 bg-muted/60 px-3 py-2 text-[10px] font-semibold uppercase tracking-[0.08em] text-muted-foreground">{label}</div>
      <div className="space-y-1.5 p-3 font-mono">
        {rows.map(([text, tone]) => (
          <p
            key={text}
            className={
              tone === "added"
                ? "rounded bg-emerald-500/15 px-1.5 text-emerald-800 dark:text-emerald-200"
                : tone === "removed"
                  ? "rounded bg-rose-500/15 px-1.5 text-rose-800 dark:text-rose-200"
                  : "px-1.5 text-muted-foreground"
            }
          >
            {text}
          </p>
        ))}
      </div>
    </div>
  );
}
