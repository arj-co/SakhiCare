package com.sakhicare.app.data

data class ApgarScore(
    val appearance: Int, // 0-2
    val pulse: Int,      // 0-2
    val grimace: Int,    // 0-2
    val activity: Int,   // 0-2
    val respiration: Int // 0-2
) {
    val totalScore: Int get() = appearance + pulse + grimace + activity + respiration

    val isNormal: Boolean get() = totalScore >= 7
    val isModerateDepression: Boolean get() = totalScore in 4..6
    val isSevereAsphyxia: Boolean get() = totalScore < 4
}
