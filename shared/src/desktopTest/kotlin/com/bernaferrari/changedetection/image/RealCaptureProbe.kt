package com.bernaferrari.changedetection.image

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

/** Manual probe: runs only when real captures are at /tmp/g1-a.png and /tmp/g1-b.png. Writes crops to /tmp/regions. */
class RealCaptureProbe {
    @Test fun probe() = runBlocking {
        val a = File("/tmp/g1-a.png")
        val b = File("/tmp/g1-b.png")
        assumeTrue(a.exists() && b.exists())
        val change = requireNotNull(computeVisualChange(a.readBytes(), b.readBytes()))
        println("share=${change.changedShare} heightDelta=${change.heightDelta} visible=${change.visible} moved=${change.moved} decision=${!change.moved || change.visible}")
        val before = ImageIO.read(a)
        val after = ImageIO.read(b)
        val out = File("/tmp/regions").apply {
            deleteRecursively()
            mkdirs()
        }
        change.regions.forEachIndexed { i, r ->
            println("$i ${r.kind} x=${r.left}-${r.right} before=${r.beforeTop}-${r.beforeBottom} after=${r.afterTop}-${r.afterBottom}")
            fun crop(img: java.awt.image.BufferedImage, top: Float, bottom: Float, name: String) {
                val y0 = (top * img.height).toInt().coerceIn(0, img.height - 1)
                val y1 = (bottom * img.height).toInt().coerceIn(y0 + 1, img.height)
                val x0 = (r.left * img.width).toInt().coerceIn(0, img.width - 1)
                val x1 = (r.right * img.width).toInt().coerceIn(x0 + 1, img.width)
                ImageIO.write(img.getSubimage(x0, y0, x1 - x0, y1 - y0), "png", File(out, "$i-$name.png"))
            }
            if (r.kind != VisualRegion.Kind.Added) crop(before, r.beforeTop, r.beforeBottom, "before")
            if (r.kind != VisualRegion.Kind.Removed) crop(after, r.afterTop, r.afterBottom, "after")
        }
    }
}
