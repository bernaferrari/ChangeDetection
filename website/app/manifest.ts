import type { MetadataRoute } from "next"

import { site } from "@/lib/site"

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: site.name,
    short_name: site.name,
    description: site.description,
    start_url: "/",
    display: "browser",
    background_color: "#ffffff",
    theme_color: "#4F5BD5",
    icons: [{ src: "/icon.png", sizes: "1024x1024", type: "image/png" }],
  }
}
