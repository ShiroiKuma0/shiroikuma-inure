package app.simple.inure.preferences

/**
 * Fork (白い熊 Inure UI): styling for the MAIN (Home) screen's feature menu items — applied in
 * [app.simple.inure.adapters.ui.AdapterHome]. Covers the icon (size + colour) and the label text
 * (font family/weight/size + colour). Each value is an independent override; unset = the stock styling.
 */
object ShiroikumaUIPreferences {

    // Icon
    const val MAIN_LIST_ICON_SCALE = "sui_main_list_icon_scale" // percent of designed size; -1 = inherit
    const val MAIN_ICON_COLOR = "sui_main_icon_color"

    // Text
    const val MAIN_TEXT_FAMILY = "sui_main_text_family"
    const val MAIN_TEXT_WEIGHT = "sui_main_text_weight"
    const val MAIN_TEXT_SCALE = "sui_main_text_scale"
    const val MAIN_TEXT_COLOR = "sui_main_text_color"

    const val SCALE_MIN = 50
    const val SCALE_MAX = 300
    const val SCALE_DEFAULT = 100

    /** Any of these changing should refresh the Home list. */
    const val PREFIX = "sui_main"

    private fun sp() = SharedPreferences.getSharedPreferences()

    // ---- icon size -------------------------------------------------------------------------------------- //
    fun getMainListIconScale(): Int = sp().getInt(MAIN_LIST_ICON_SCALE, -1)
    fun setMainListIconScale(percent: Int) = sp().edit().putInt(MAIN_LIST_ICON_SCALE, percent).apply()
    fun isMainListIconScaleSet(): Boolean = getMainListIconScale() >= 0
    fun resetMainListIconScale() = sp().edit().remove(MAIN_LIST_ICON_SCALE).apply()

    // ---- icon colour ------------------------------------------------------------------------------------ //
    fun isIconColorSet(): Boolean = sp().contains(MAIN_ICON_COLOR)
    fun getIconColor(def: Int): Int = sp().getInt(MAIN_ICON_COLOR, def)
    fun setIconColor(color: Int) = sp().edit().putInt(MAIN_ICON_COLOR, color).apply()
    fun resetIconColor() = sp().edit().remove(MAIN_ICON_COLOR).apply()

    // ---- text font -------------------------------------------------------------------------------------- //
    fun getTextFamily(): String = sp().getString(MAIN_TEXT_FAMILY, "") ?: ""
    fun setTextFamily(value: String) = sp().edit().putString(MAIN_TEXT_FAMILY, value).apply()
    fun getTextWeight(): Int = sp().getInt(MAIN_TEXT_WEIGHT, 0)
    fun setTextWeight(value: Int) = sp().edit().putInt(MAIN_TEXT_WEIGHT, value).apply()
    fun getTextScale(): Int = sp().getInt(MAIN_TEXT_SCALE, 0)
    fun setTextScale(value: Int) = sp().edit().putInt(MAIN_TEXT_SCALE, value).apply()

    // ---- text colour ------------------------------------------------------------------------------------ //
    fun isTextColorSet(): Boolean = sp().contains(MAIN_TEXT_COLOR)
    fun getTextColor(def: Int): Int = sp().getInt(MAIN_TEXT_COLOR, def)
    fun setTextColor(color: Int) = sp().edit().putInt(MAIN_TEXT_COLOR, color).apply()
    fun resetTextColor() = sp().edit().remove(MAIN_TEXT_COLOR).apply()
}
