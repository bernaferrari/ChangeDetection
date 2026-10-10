import Image from "next/image"

import type { Screen } from "@/lib/site"
import { cn } from "@/lib/utils"

/** A real app screen in a simple phone frame. */
export function Phone({ screen, priority = false, className }: { screen: Screen; priority?: boolean; className?: string }) {
  return (
    <div className={cn("relative overflow-hidden rounded-[2.6rem] border-[6px] border-foreground/90 bg-background shadow-2xl shadow-primary/10", className)}>
      <Image
        src={screen.src}
        alt={screen.alt}
        width={screen.width}
        height={screen.height}
        priority={priority}
        sizes="(max-width: 768px) 70vw, 320px"
        className="h-auto w-full"
      />
    </div>
  )
}
