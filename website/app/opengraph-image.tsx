import { readFile } from "node:fs/promises"
import { join } from "node:path"
import { ImageResponse } from "next/og"

export const alt = "Change Detection: stop refreshing, we'll tell you what changed"
export const size = { width: 1200, height: 630 }
export const contentType = "image/png"

export default async function Image() {
  const [font, icon] = await Promise.all([
    readFile(join(process.cwd(), "app/fonts/gsf_display_bold.ttf")),
    readFile(join(process.cwd(), "public/logo.png")),
  ])
  const iconSrc = `data:image/png;base64,${icon.toString("base64")}`
  return new ImageResponse(
    (
      <div style={{ width: "100%", height: "100%", display: "flex", flexDirection: "column", justifyContent: "space-between", padding: 72, background: "linear-gradient(135deg, #ffffff 0%, #eef0ff 100%)", fontFamily: "GSF" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
          <img src={iconSrc} width={72} height={72} style={{ borderRadius: 18 }} alt="" />
          <span style={{ fontSize: 36, color: "#1b1b21" }}>Change Detection</span>
        </div>
        <div style={{ display: "flex", flexDirection: "column", gap: 28 }}>
          <span style={{ fontSize: 76, lineHeight: 1.05, color: "#1b1b21", letterSpacing: -2 }}>Stop refreshing.</span>
          <span style={{ fontSize: 76, lineHeight: 1.05, color: "#4F5BD5", letterSpacing: -2 }}>We&apos;ll tell you what changed.</span>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 18, fontSize: 40 }}>
          <span style={{ color: "#b3261e", textDecoration: "line-through" }}>$1,599.00</span>
          <span style={{ background: "#FFE14A", color: "#1b1b21", padding: "4px 16px", borderRadius: 12 }}>$1,349.00</span>
          <span style={{ fontSize: 28, color: "#5d5e72", marginLeft: 12 }}>Website change alerts · Android & iPhone</span>
        </div>
      </div>
    ),
    { ...size, fonts: [{ name: "GSF", data: font, weight: 700, style: "normal" }] },
  )
}
