package com.sakhicare.app.data

/**
 * Demo Configuration & Isolation Control
 *
 * In production mode, isDemoEnabled is FALSE, and the database starts empty.
 * Demo fixtures are only available when explicitly enabled, and every demo
 * record is tagged with isDemo = true and a visible "Demo data" badge.
 */
object DemoConfig {
    var isDemoEnabled: Boolean = false

    fun setDemoMode(enabled: Boolean) {
        isDemoEnabled = enabled
    }
}
