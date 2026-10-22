package com.bernaferrari.changedetection.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The in-app brand artwork as Kotlin vectors, the same paths as `artwork/app-icon/brand_*.xml`.
 * [Mark] is the scanned page; [Change] is the highlighted change drawn over it in the signal colour.
 */
object BrandArt {
    private fun ImageVector.Builder.stroke(path: String, width: Float) = addPath(
        PathParser().parsePathString(path).toNodes(),
        stroke = SolidColor(Color.White),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )

    private fun ImageVector.Builder.fill(path: String) = addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.White))

    /** The scanned page: viewfinder corners, the page, a picture, text lines and the change frame. */
    val Mark: ImageVector by lazy {
        ImageVector.Builder("Mark", 24.dp, 24.dp, 24f, 24f).apply {
            stroke("M2,6 V3.5 Q2,2 3.5,2 H6 M18,2 H20.5 Q22,2 22,3.5 V6 M22,18 V20.5 Q22,22 20.5,22 H18 M6,22 H3.5 Q2,22 2,20.5 V18", 1.8f)
            stroke("M6,6.5 H18 Q20,6.5 20,8.5 V17.5 Q20,19.5 18,19.5 H6 Q4,19.5 4,17.5 V8.5 Q4,6.5 6,6.5 Z", 1.2f)
            stroke("M6.3,9 H11.6 V13.5 H6.3 Z M6.6,13.2 L8.2,11.2 L10.3,13.2", 0.8f)
            stroke("M6.5,16 H11.5 M6.5,17.5 H17.2", 1.1f)
            stroke("M13.5,9.6 V8.8 Q13.5,8.3 14,8.3 H14.7 M17.5,8.3 H18.2 Q18.7,8.3 18.7,8.8 V9.6 M18.7,13 V13.8 Q18.7,14.3 18.2,14.3 H17.5 M14.7,14.3 H14 Q13.5,14.3 13.5,13.8 V13", 0.8f)
            stroke("M14.6,10.2 H17.7 M14.6,12.5 H17.7", 1.3f)
            fill("M10.8,10.1 A0.65,0.65 0,1 1,9.5,10.1 A0.65,0.65 0,1 1,10.8,10.1 Z")
        }.build()
    }

    /** The highlighted change, drawn over [Mark] in the signal colour. */
    val Change: ImageVector by lazy {
        ImageVector.Builder("Change", 24.dp, 24.dp, 24f, 24f).apply {
            stroke("M14.6,10.2 H17.7 M14.6,12.5 H17.7", 1.3f)
        }.build()
    }
}
