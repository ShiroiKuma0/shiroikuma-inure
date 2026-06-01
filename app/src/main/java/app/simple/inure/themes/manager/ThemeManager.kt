package app.simple.inure.themes.manager

import app.simple.inure.themes.interfaces.ThemeChangedListener

object ThemeManager {

    private val listeners = mutableSetOf<ThemeChangedListener>()

    var theme = Theme.LIGHT
        set(value) {
            val bool = field != value
            field = value
            listeners.forEach { listener -> listener.onThemeChanged(value, bool) }
        }

    var accent = Accent.INURE
        set(value) {
            field = value
            listeners.forEach { it.onAccentChanged(accent) }
        }

    /**
     * Fork: force every listener to re-read the current [theme] even though the [Theme] reference is
     * unchanged. Used by the Custom theme — its colour fields are mutated in place (same instance), so the
     * normal `theme = …` setter would see no change and skip notifying.
     */
    fun refreshTheme(animate: Boolean = true) {
        val current = theme
        listeners.forEach { listener -> listener.onThemeChanged(current, animate) }
    }

    fun addListener(listener: ThemeChangedListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: ThemeChangedListener) {
        listeners.remove(listener)
    }
}