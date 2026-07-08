package com.filmatube.app.domain.model

/** User-configurable subtitle appearance (persisted via DataStore). */
enum class SubtitleSize(val fraction: Float) {
    SMALL(0.040f),
    MEDIUM(0.0533f), // Media3 default
    LARGE(0.075f),
}

enum class SubtitleTextColor(val color: Int) {
    WHITE(0xFFFFFFFF.toInt()),
    YELLOW(0xFFFFEB3B.toInt()),
    CYAN(0xFF00E5FF.toInt()),
}

enum class SubtitleBackground(val color: Int) {
    NONE(0x00000000),
    DIM(0x99000000.toInt()),
    SOLID(0xCC000000.toInt()),
}

enum class SubtitleEdge { NONE, SHADOW, OUTLINE }

enum class SubtitlePosition(val bottomPaddingFraction: Float) {
    LOW(0.02f),
    NORMAL(0.08f),
    HIGH(0.20f),
}

data class SubtitleStyle(
    val size: SubtitleSize = SubtitleSize.MEDIUM,
    val textColor: SubtitleTextColor = SubtitleTextColor.WHITE,
    val background: SubtitleBackground = SubtitleBackground.NONE,
    val edge: SubtitleEdge = SubtitleEdge.SHADOW,
    val position: SubtitlePosition = SubtitlePosition.NORMAL,
)
