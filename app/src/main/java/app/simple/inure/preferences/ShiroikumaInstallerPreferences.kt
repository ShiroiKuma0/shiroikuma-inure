package app.simple.inure.preferences

/**
 * Fork (白い熊 Inure UI): installer-screen-scoped overrides. Independent of the global per-role fonts/colours —
 * each installer item (app name, package, version, action buttons) may override its font (family/weight/size)
 * and text colour, plus a screen background colour. Unset = the item keeps whatever the global theme/fonts give
 * it. Stored in the main [SharedPreferences].
 */
object ShiroikumaInstallerPreferences {

    const val NAME = "name"
    const val PACKAGE = "package"
    const val VERSION = "version"
    const val BUTTONS = "buttons"

    val ITEMS = listOf(NAME, PACKAGE, VERSION, BUTTONS)

    const val FILE_PREFIX = "file:"
    private const val PREFIX = "sinst_"
    private const val BACKGROUND = "sinst_background"

    private fun sp() = SharedPreferences.getSharedPreferences()
    private fun famKey(item: String) = "$PREFIX${item}_family"
    private fun weightKey(item: String) = "$PREFIX${item}_weight"
    private fun scaleKey(item: String) = "$PREFIX${item}_scale"
    private fun colorKey(item: String) = "$PREFIX${item}_color"

    // ---- per-item font ---------------------------------------------------------------------------------- //

    fun getFamily(item: String): String = sp().getString(famKey(item), "") ?: ""
    fun setFamily(item: String, value: String) = sp().edit().putString(famKey(item), value).apply()
    fun getWeight(item: String): Int = sp().getInt(weightKey(item), 0)
    fun setWeight(item: String, value: Int) = sp().edit().putInt(weightKey(item), value).apply()
    fun getScale(item: String): Int = sp().getInt(scaleKey(item), 0)
    fun setScale(item: String, value: Int) = sp().edit().putInt(scaleKey(item), value).apply()

    // ---- per-item colour -------------------------------------------------------------------------------- //

    fun isColorSet(item: String): Boolean = sp().contains(colorKey(item))
    fun getColor(item: String, def: Int): Int = sp().getInt(colorKey(item), def)
    fun setColor(item: String, color: Int) = sp().edit().putInt(colorKey(item), color).apply()
    fun resetColor(item: String) = sp().edit().remove(colorKey(item)).apply()

    // ---- screen background ------------------------------------------------------------------------------ //

    fun isBackgroundSet(): Boolean = sp().contains(BACKGROUND)
    fun getBackground(def: Int): Int = sp().getInt(BACKGROUND, def)
    fun setBackground(color: Int) = sp().edit().putInt(BACKGROUND, color).apply()
    fun resetBackground() = sp().edit().remove(BACKGROUND).apply()

    fun reset(item: String) {
        sp().edit().remove(famKey(item)).remove(weightKey(item)).remove(scaleKey(item)).remove(colorKey(item)).apply()
    }

    fun resetAll() {
        ITEMS.forEach { reset(it) }
        resetBackground()
    }
}
