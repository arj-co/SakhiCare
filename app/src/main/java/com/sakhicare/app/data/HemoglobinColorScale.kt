package com.sakhicare.app.data

import androidx.compose.ui.graphics.Color

object HemoglobinColorScale {
    fun getColorForHb(hb: Double): Color = when {
        hb >= 11.0 -> Color(0xFFD32F2F) // Deep healthy red
        hb >= 9.0 -> Color(0xFFE57373)  // Moderate pink-red
        hb >= 7.0 -> Color(0xFFFFCDD2)  // Pale pink (Moderate anemia)
        else -> Color(0xFFFFEBEE)       // Very pale (Severe anemia alert)
    }
}
