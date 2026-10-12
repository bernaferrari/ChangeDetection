package com.bernaferrari.changedetection.repo

/** Paired light/dark tints stored in the page's existing colors column. */
enum class PageIconColor(val light: Int, val dark: Int) {
    AUTOMATIC(0, 0),
    ORANGE(0xFFFFDBCE.toInt(), 0xFF733E28.toInt()),
    AMBER(0xFFFFDCC2.toInt(), 0xFF6B3B12.toInt()),
    LIME(0xFFE0EDB7.toInt(), 0xFF435522.toInt()),
    GREEN(0xFFC9F0D8.toInt(), 0xFF1E5A3C.toInt()),
    TEAL(0xFFC4EEF0.toInt(), 0xFF1C5457.toInt()),
    CYAN(0xFFBCEBF5.toInt(), 0xFF175665.toInt()),
    BLUE(0xFFDDE1FF.toInt(), 0xFF353F8C.toInt()),
    INDIGO(0xFFDFDFFF.toInt(), 0xFF3C3D85.toInt()),
    PURPLE(0xFFE9DDFF.toInt(), 0xFF4F3A86.toInt()),
    PINK(0xFFFFD8E4.toInt(), 0xFF7A2949.toInt()),
    ;

    val colors: ColorGroup get() = light to dark

    companion object {
        fun from(colors: ColorGroup): PageIconColor = entries.firstOrNull { it.colors == colors } ?: AUTOMATIC
    }
}
