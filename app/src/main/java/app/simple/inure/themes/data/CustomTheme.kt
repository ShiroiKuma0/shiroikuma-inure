package app.simple.inure.themes.data

import android.graphics.Color
import androidx.annotation.ColorInt
import app.simple.inure.R
import app.simple.inure.preferences.SharedPreferences

/**
 * Fork (白い熊 Inure UI): backing store for the user-overridable [app.simple.inure.themes.manager.Theme.CUSTOM]
 * theme. The four theme data objects below are the SAME instances referenced by `Theme.CUSTOM`, so mutating
 * a field here and firing a theme refresh recolours the whole UI live.
 *
 * Every role is seeded from the DARK palette (the chosen base). A role is "set" only when its key is present
 * in [SharedPreferences]; an unset role falls back to its DARK default, so the Custom theme reproduces Dark
 * exactly until the user overrides something.
 */
object CustomTheme {

    private const val PREFIX = "custom_theme_"

    // ---- Dark-seeded defaults (literals copied from Theme.DARK) ------------------------------------------- //
    private const val D_HEADING = "#F1F1F1"
    private const val D_PRIMARY = "#E4E4E4"
    private const val D_SECONDARY = "#C8C8C8"
    private const val D_TERTIARY = "#AAAAAA"
    private const val D_QUATERNARY = "#9A9A9A"
    private const val D_BACKGROUND = "#171717"
    private const val D_VIEWER = "#404040"
    private const val D_HIGHLIGHT = "#404040"
    private const val D_SELECTED = "#242424"
    private const val D_DIVIDER = "#666666"
    private const val D_SWITCH_OFF = "#252525"
    private const val D_REGULAR_ICON = "#F8F8F8"
    private const val D_SECONDARY_ICON = "#E8E8E8"

    // ---- The live, mutable data instances Theme.CUSTOM points at ----------------------------------------- //
    val textViewTheme = TextViewTheme(
            headingTextColor = Color.parseColor(D_HEADING),
            primaryTextColor = Color.parseColor(D_PRIMARY),
            secondaryTextColor = Color.parseColor(D_SECONDARY),
            tertiaryTextColor = Color.parseColor(D_TERTIARY),
            quaternaryTextColor = Color.parseColor(D_QUATERNARY),
    )

    val viewGroupTheme = ViewGroupTheme(
            background = Color.parseColor(D_BACKGROUND),
            viewerBackground = Color.parseColor(D_VIEWER),
            highlightBackground = Color.parseColor(D_HIGHLIGHT),
            selectedBackground = Color.parseColor(D_SELECTED),
            dividerBackground = Color.parseColor(D_DIVIDER),
    )

    val switchViewTheme = SwitchViewTheme(switchOffColor = Color.parseColor(D_SWITCH_OFF))

    val iconTheme = IconTheme(
            regularIconColor = Color.parseColor(D_REGULAR_ICON),
            secondaryIconColor = Color.parseColor(D_SECONDARY_ICON),
    )

    /**
     * One overridable colour role. [default] is the literal DARK seed (captured explicitly, never read back
     * from the mutable instance, so it stays correct after [load] overlays user values).
     */
    class Role(
            val key: String,
            val labelRes: Int,
            @ColorInt val default: Int,
            val get: () -> Int,
            val set: (Int) -> Unit,
    )

    val roles: List<Role> by lazy {
        listOf(
                Role(PREFIX + "heading", R.string.custom_role_heading, Color.parseColor(D_HEADING),
                     { textViewTheme.headingTextColor }, { textViewTheme.headingTextColor = it }),
                Role(PREFIX + "primary", R.string.custom_role_primary, Color.parseColor(D_PRIMARY),
                     { textViewTheme.primaryTextColor }, { textViewTheme.primaryTextColor = it }),
                Role(PREFIX + "secondary", R.string.custom_role_secondary, Color.parseColor(D_SECONDARY),
                     { textViewTheme.secondaryTextColor }, { textViewTheme.secondaryTextColor = it }),
                Role(PREFIX + "tertiary", R.string.custom_role_tertiary, Color.parseColor(D_TERTIARY),
                     { textViewTheme.tertiaryTextColor }, { textViewTheme.tertiaryTextColor = it }),
                Role(PREFIX + "quaternary", R.string.custom_role_quaternary, Color.parseColor(D_QUATERNARY),
                     { textViewTheme.quaternaryTextColor }, { textViewTheme.quaternaryTextColor = it }),
                Role(PREFIX + "background", R.string.custom_role_background, Color.parseColor(D_BACKGROUND),
                     { viewGroupTheme.background }, { viewGroupTheme.background = it }),
                Role(PREFIX + "viewer", R.string.custom_role_viewer_background, Color.parseColor(D_VIEWER),
                     { viewGroupTheme.viewerBackground }, { viewGroupTheme.viewerBackground = it }),
                Role(PREFIX + "highlight", R.string.custom_role_highlight, Color.parseColor(D_HIGHLIGHT),
                     { viewGroupTheme.highlightBackground }, { viewGroupTheme.highlightBackground = it }),
                Role(PREFIX + "selected", R.string.custom_role_selected, Color.parseColor(D_SELECTED),
                     { viewGroupTheme.selectedBackground }, { viewGroupTheme.selectedBackground = it }),
                Role(PREFIX + "divider", R.string.custom_role_divider, Color.parseColor(D_DIVIDER),
                     { viewGroupTheme.dividerBackground }, { viewGroupTheme.dividerBackground = it }),
                Role(PREFIX + "regular_icon", R.string.custom_role_regular_icon, Color.parseColor(D_REGULAR_ICON),
                     { iconTheme.regularIconColor }, { iconTheme.regularIconColor = it }),
                Role(PREFIX + "secondary_icon", R.string.custom_role_secondary_icon, Color.parseColor(D_SECONDARY_ICON),
                     { iconTheme.secondaryIconColor }, { iconTheme.secondaryIconColor = it }),
                Role(PREFIX + "switch_off", R.string.custom_role_switch_off, Color.parseColor(D_SWITCH_OFF),
                     { switchViewTheme.switchOffColor }, { switchViewTheme.switchOffColor = it }),
        )
    }

    private fun prefs() = SharedPreferences.getSharedPreferences()

    /**
     * Overlay any stored overrides onto the live instances (unset roles fall back to their DARK default).
     * Call before activating the Custom theme. Safe to call repeatedly.
     */
    fun load() {
        val sp = prefs()
        roles.forEach { role ->
            role.set(if (sp.contains(role.key)) sp.getInt(role.key, role.default) else role.default)
        }
    }

    fun isSet(role: Role): Boolean = prefs().contains(role.key)

    /** The colour shown / used for a role: the stored override if set, else the DARK default. */
    @ColorInt
    fun effective(role: Role): Int = if (isSet(role)) prefs().getInt(role.key, role.default) else role.default

    fun set(role: Role, @ColorInt color: Int) {
        prefs().edit().putInt(role.key, color).apply()
        role.set(color)
    }

    fun reset(role: Role) {
        prefs().edit().remove(role.key).apply()
        role.set(role.default)
    }

    fun resetAll() {
        roles.forEach { reset(it) }
    }
}
