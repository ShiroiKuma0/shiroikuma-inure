package app.simple.inure.preferences

import java.io.File

/**
 * Fork (白い熊 Inure UI): per-text-role font configuration (family / weight / size-scale), plus a registry of
 * user-imported external font files. Roles mirror Inure's [app.simple.inure.decorations.typeface.TypeFaceTextView]
 * `textColorStyle` buckets; [DEFAULT] is the cascade fallback (role → DEFAULT → inherit the app font).
 *
 * Stored in the main [SharedPreferences]. A field is "inherit" when empty/0, so an all-inherit configuration
 * reproduces the stock app-font behaviour exactly.
 */
object ShiroikumaFontPreferences {

    const val DEFAULT = "default"
    const val HEADING = "heading"
    const val PRIMARY = "primary"
    const val SECONDARY = "secondary"
    const val TERTIARY = "tertiary"
    const val QUATERNARY = "quaternary"

    val ROLES = listOf(DEFAULT, HEADING, PRIMARY, SECONDARY, TERTIARY, QUATERNARY)

    /** All font preference keys start with this; [app.simple.inure.decorations.typeface.TypeFaceTextView] watches it. */
    const val PREFIX = "sfont_"
    private const val IMPORTED = "sfont_imported_fonts"

    const val FILE_PREFIX = "file:"
    const val SCALE_MIN = 50
    const val SCALE_MAX = 400

    /** Weight options for the picker (0 = inherit). */
    val WEIGHTS = intArrayOf(0, 100, 300, 400, 500, 700, 900)

    private fun sp() = SharedPreferences.getSharedPreferences()
    private fun famKey(role: String) = "$PREFIX${role}_family"
    private fun weightKey(role: String) = "$PREFIX${role}_weight"
    private fun scaleKey(role: String) = "$PREFIX${role}_scale"

    // ---- raw per-role accessors ------------------------------------------------------------------------- //

    fun getFamily(role: String): String = sp().getString(famKey(role), "") ?: ""
    fun setFamily(role: String, value: String) = sp().edit().putString(famKey(role), value).apply()

    fun getWeight(role: String): Int = sp().getInt(weightKey(role), 0)
    fun setWeight(role: String, value: Int) = sp().edit().putInt(weightKey(role), value).apply()

    /** 0 = inherit, else a percentage in [SCALE_MIN]..[SCALE_MAX]. */
    fun getScale(role: String): Int = sp().getInt(scaleKey(role), 0)
    fun setScale(role: String, value: Int) = sp().edit().putInt(scaleKey(role), value).apply()

    fun reset(role: String) {
        sp().edit().remove(famKey(role)).remove(weightKey(role)).remove(scaleKey(role)).apply()
    }

    fun resetAll() = ROLES.forEach { reset(it) }

    fun isRoleConfigured(role: String): Boolean =
        getFamily(role).isNotEmpty() || getWeight(role) != 0 || getScale(role) != 0

    // ---- effective (role -> DEFAULT -> inherit) cascade ------------------------------------------------- //

    fun effectiveFamily(role: String): String {
        val v = getFamily(role)
        if (v.isNotEmpty()) return v
        if (role != DEFAULT) {
            val d = getFamily(DEFAULT)
            if (d.isNotEmpty()) return d
        }
        return ""
    }

    fun effectiveWeight(role: String): Int {
        val v = getWeight(role)
        if (v != 0) return v
        if (role != DEFAULT) {
            val d = getWeight(DEFAULT)
            if (d != 0) return d
        }
        return 0
    }

    fun effectiveScale(role: String): Int {
        val v = getScale(role)
        if (v != 0) return v
        if (role != DEFAULT) {
            val d = getScale(DEFAULT)
            if (d != 0) return d
        }
        return 0
    }

    // ---- imported font registry ------------------------------------------------------------------------- //

    fun getImportedFonts(): List<String> {
        val set = sp().getStringSet(IMPORTED, emptySet()) ?: emptySet()
        return set.filter { File(it).exists() }.sorted()
    }

    fun addImportedFont(path: String) {
        val set = LinkedHashSet(sp().getStringSet(IMPORTED, emptySet()) ?: emptySet())
        set.add(path)
        sp().edit().putStringSet(IMPORTED, set).apply()
    }

    /** Drop an imported font from the registry and delete its file. Roles still pointing at it fall back to the app font. */
    fun removeImportedFont(path: String) {
        val set = LinkedHashSet(sp().getStringSet(IMPORTED, emptySet()) ?: emptySet())
        set.remove(path)
        sp().edit().putStringSet(IMPORTED, set).apply()
        runCatching { File(path).delete() }
    }
}
