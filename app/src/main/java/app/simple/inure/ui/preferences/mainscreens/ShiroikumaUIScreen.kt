package app.simple.inure.ui.preferences.mainscreens

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.ContextCompat
import app.simple.inure.R
import app.simple.inure.constants.ThemeConstants
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.dialogs.appearance.FontFamilyPicker.Companion.showFontFamilyPicker
import app.simple.inure.dialogs.appearance.RoleColorPicker.Companion.showRoleColorPicker
import app.simple.inure.extensions.fragments.ScopedFragment
import app.simple.inure.preferences.AppearancePreferences
import app.simple.inure.preferences.ShiroikumaFontPreferences
import app.simple.inure.preferences.ShiroikumaInstallerPreferences
import app.simple.inure.themes.data.CustomTheme
import app.simple.inure.themes.manager.ThemeManager
import app.simple.inure.themes.manager.ThemeUtils
import app.simple.inure.util.ColorUtils.toHexColor
import app.simple.inure.util.TypeFace
import java.io.File

/**
 * Fork (白い熊 Inure UI): per-text-role fonts, the user-overridable Custom theme colours, and installer-screen
 * styling. Built programmatically with the grouped / deeply-indented treatment.
 */
class ShiroikumaUIScreen : ScopedFragment() {

    private lateinit var container: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.preferences_shiroikuma_ui, container, false)
        this.container = view.findViewById(R.id.shiroikuma_ui_container)
        startPostponedEnterTransition()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        render()
    }

    private fun render() {
        container.removeAllViews()

        // ---- Typeface (per text role) ------------------------------------------------------------------- //
        addGroupHeader(getString(R.string.custom_group_typeface))
        ShiroikumaFontPreferences.ROLES.forEach { role -> addFontElement(globalFontConfig(role)) }
        addAction(getString(R.string.custom_reset_fonts), null) {
            ShiroikumaFontPreferences.resetAll()
            TypeFace.clearFileTypefaceCache()
            render()
        }

        // ---- Colours (Custom theme) --------------------------------------------------------------------- //
        addAction(getString(R.string.custom_use_theme), activeDescription()) { useCustomTheme() }
        val roles = CustomTheme.roles
        addGroupHeader(getString(R.string.custom_group_text))
        roles.subList(0, 5).forEach { addCustomThemeRow(it) }
        addGroupHeader(getString(R.string.custom_group_surface))
        roles.subList(5, 10).forEach { addCustomThemeRow(it) }
        addGroupHeader(getString(R.string.custom_group_icons))
        roles.subList(10, 12).forEach { addCustomThemeRow(it) }
        addGroupHeader(getString(R.string.custom_group_other))
        roles.subList(12, roles.size).forEach { addCustomThemeRow(it) }
        addAccentRow()
        addAction(getString(R.string.custom_reset_all), null) {
            CustomTheme.resetAll()
            refreshIfActive()
            render()
        }

        // ---- Installer screen --------------------------------------------------------------------------- //
        addGroupHeader(getString(R.string.custom_group_installer))
        addColorRow(getString(R.string.custom_installer_background),
                    { ShiroikumaInstallerPreferences.getBackground(ThemeManager.theme.viewGroupTheme.background) },
                    { ShiroikumaInstallerPreferences.isBackgroundSet() },
                    { ShiroikumaInstallerPreferences.setBackground(it) },
                    { ShiroikumaInstallerPreferences.resetBackground() })
        addInstallerItem(ShiroikumaInstallerPreferences.NAME, R.string.custom_installer_name, ThemeManager.theme.textViewTheme.primaryTextColor)
        addInstallerItem(ShiroikumaInstallerPreferences.PACKAGE, R.string.custom_installer_package, ThemeManager.theme.textViewTheme.secondaryTextColor)
        addInstallerItem(ShiroikumaInstallerPreferences.VERSION, R.string.custom_installer_version, ThemeManager.theme.textViewTheme.secondaryTextColor)
        addInstallerItem(ShiroikumaInstallerPreferences.BUTTONS, R.string.custom_installer_buttons, AppearancePreferences.getAccentColor())
        addAction(getString(R.string.custom_reset_installer), null) {
            ShiroikumaInstallerPreferences.resetAll()
            render()
        }
    }

    // ---- Generic builders ------------------------------------------------------------------------------- //

    private fun inflate(layout: Int): View =
        LayoutInflater.from(requireContext()).inflate(layout, container, false)

    private fun addGroupHeader(title: String) {
        val header = inflate(R.layout.adapter_shiroikuma_group_header)
        header.findViewById<TypeFaceTextView>(R.id.group_title).text = title
        container.addView(header)
    }

    private fun addAction(label: String, description: String?, onClick: () -> Unit) {
        val row = inflate(R.layout.adapter_shiroikuma_action)
        row.findViewById<TypeFaceTextView>(R.id.action_label).text = label
        val desc = row.findViewById<TypeFaceTextView>(R.id.action_description)
        if (description != null) {
            desc.text = description
            desc.visibility = View.VISIBLE
        }
        row.setOnClickListener { onClick() }
        container.addView(row)
    }

    private fun addColorRow(
            label: String,
            effective: () -> Int,
            isSet: () -> Boolean,
            onPick: (Int) -> Unit,
            onReset: () -> Unit,
    ) {
        val row = inflate(R.layout.adapter_shiroikuma_color_row)
        row.findViewById<TypeFaceTextView>(R.id.label).text = label
        val swatch = row.findViewById<View>(R.id.swatch)
        val value = row.findViewById<TypeFaceTextView>(R.id.value)

        fun refresh() {
            val color = effective()
            swatch.background = swatch(color)
            value.text = if (isSet()) color.toHexColor() else getString(R.string.custom_default)
        }
        refresh()

        row.setOnClickListener {
            childFragmentManager.showRoleColorPicker(effective()) { picked ->
                onPick(picked)
                refresh()
            }
        }
        row.setOnLongClickListener {
            onReset()
            refresh()
            true
        }
        container.addView(row)
    }

    private fun addCustomThemeRow(role: CustomTheme.Role) {
        addColorRow(getString(role.labelRes),
                    { CustomTheme.effective(role) },
                    { CustomTheme.isSet(role) },
                    { CustomTheme.set(role, it); refreshIfActive() },
                    { CustomTheme.reset(role); refreshIfActive() })
    }

    /** Accent is global (not part of a [app.simple.inure.themes.manager.Theme]); recolours live via ACCENT_COLOR. */
    private fun addAccentRow() {
        addColorRow(getString(R.string.custom_role_accent),
                    { AppearancePreferences.getAccentColor() },
                    { true },
                    {
                        AppearancePreferences.setCustomColor(true)
                        AppearancePreferences.setAccentColor(it)
                        AppearancePreferences.setPickedAccentColor(it)
                    },
                    {
                        val inure = ContextCompat.getColor(requireContext(), R.color.inure)
                        AppearancePreferences.setAccentColor(inure)
                        AppearancePreferences.setPickedAccentColor(inure)
                    })
    }

    private fun addInstallerItem(item: String, labelRes: Int, colorDefault: Int) {
        val refreshFont = addFontElement(installerFontConfig(item, getString(labelRes), colorDefault))
        addColorRow(getString(R.string.custom_colour),
                    { ShiroikumaInstallerPreferences.getColor(item, colorDefault) },
                    { ShiroikumaInstallerPreferences.isColorSet(item) },
                    { ShiroikumaInstallerPreferences.setColor(item, it); refreshFont() },
                    { ShiroikumaInstallerPreferences.resetColor(item); refreshFont() })
    }

    // ---- Font element ----------------------------------------------------------------------------------- //

    private inner class FontElementConfig(
            val label: String,
            val getFamily: () -> String, val setFamily: (String) -> Unit,
            val getWeight: () -> Int, val setWeight: (Int) -> Unit,
            val getScale: () -> Int, val setScale: (Int) -> Unit,
            val effFamily: () -> String = getFamily,
            val effWeight: () -> Int = getWeight,
            val effScale: () -> Int = getScale,
            val previewColor: () -> Int = { ThemeManager.theme.textViewTheme.primaryTextColor },
    )

    private fun globalFontConfig(role: String) = FontElementConfig(
            label = fontRoleLabel(role),
            getFamily = { ShiroikumaFontPreferences.getFamily(role) }, setFamily = { ShiroikumaFontPreferences.setFamily(role, it) },
            getWeight = { ShiroikumaFontPreferences.getWeight(role) }, setWeight = { ShiroikumaFontPreferences.setWeight(role, it) },
            getScale = { ShiroikumaFontPreferences.getScale(role) }, setScale = { ShiroikumaFontPreferences.setScale(role, it) },
            effFamily = { ShiroikumaFontPreferences.effectiveFamily(role) },
            effWeight = { ShiroikumaFontPreferences.effectiveWeight(role) },
            effScale = { ShiroikumaFontPreferences.effectiveScale(role) },
            previewColor = { roleThemeColor(role) },
    )

    private fun installerFontConfig(item: String, label: String, colorDefault: Int) = FontElementConfig(
            label = label,
            getFamily = { ShiroikumaInstallerPreferences.getFamily(item) }, setFamily = { ShiroikumaInstallerPreferences.setFamily(item, it) },
            getWeight = { ShiroikumaInstallerPreferences.getWeight(item) }, setWeight = { ShiroikumaInstallerPreferences.setWeight(item, it) },
            getScale = { ShiroikumaInstallerPreferences.getScale(item) }, setScale = { ShiroikumaInstallerPreferences.setScale(item, it) },
            previewColor = { ShiroikumaInstallerPreferences.getColor(item, colorDefault) },
    )

    private fun roleThemeColor(role: String): Int = when (role) {
        ShiroikumaFontPreferences.HEADING -> ThemeManager.theme.textViewTheme.headingTextColor
        ShiroikumaFontPreferences.SECONDARY -> ThemeManager.theme.textViewTheme.secondaryTextColor
        ShiroikumaFontPreferences.TERTIARY -> ThemeManager.theme.textViewTheme.tertiaryTextColor
        ShiroikumaFontPreferences.QUATERNARY -> ThemeManager.theme.textViewTheme.quaternaryTextColor
        else -> ThemeManager.theme.textViewTheme.primaryTextColor
    }

    private val weightLabels by lazy {
        listOf(getString(R.string.custom_weight_inherit), "Thin 100", "Light 300", "Regular 400", "Medium 500", "Bold 700", "Black 900")
    }

    /** Builds a font element and returns a function that re-renders its value labels + preview. */
    private fun addFontElement(cfg: FontElementConfig): () -> Unit {
        val el = inflate(R.layout.adapter_shiroikuma_font_element)
        el.findViewById<TypeFaceTextView>(R.id.element_label).text = cfg.label
        val fontValue = el.findViewById<TypeFaceTextView>(R.id.font_value)
        val weightValue = el.findViewById<TypeFaceTextView>(R.id.weight_value)
        val sizeValue = el.findViewById<TypeFaceTextView>(R.id.size_value)
        val weightSeek = el.findViewById<AppCompatSeekBar>(R.id.weight_seek)
        val sizeSeek = el.findViewById<AppCompatSeekBar>(R.id.size_seek)
        val preview = el.findViewById<TextView>(R.id.preview)
        val weights = ShiroikumaFontPreferences.WEIGHTS

        fun renderElement() {
            fontValue.text = familyLabel(cfg.getFamily())
            weightValue.text = weightLabelFor(cfg.getWeight())
            weightSeek.progress = weights.indexOf(cfg.getWeight()).let { if (it < 0) 0 else it }
            val scale = cfg.getScale()
            sizeValue.text = if (scale > 0) "$scale%" else getString(R.string.custom_default)
            sizeSeek.progress = if (scale > 0) scale else 0

            preview.typeface = TypeFace.resolveRoleTypeface(cfg.effFamily(), cfg.effWeight(), TypeFaceTextView.REGULAR, requireContext())
            val effScale = cfg.effScale()
            preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (effScale > 0) 16f * effScale / 100f else 16f)
            preview.setTextColor(cfg.previewColor())
        }
        renderElement()

        el.findViewById<View>(R.id.row_font).setOnClickListener {
            childFragmentManager.showFontFamilyPicker { value ->
                cfg.setFamily(value)
                TypeFace.clearFileTypefaceCache()
                renderElement()
            }
        }
        el.findViewById<View>(R.id.row_font).setOnLongClickListener {
            cfg.setFamily("")
            renderElement()
            true
        }
        // Weight: a slider across [inherit, Thin, Light, Regular, Medium, Bold, Black]. Tap the label to reset.
        el.findViewById<View>(R.id.row_weight).setOnClickListener {
            cfg.setWeight(0)
            renderElement()
        }
        weightSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                cfg.setWeight(weights[progress.coerceIn(0, weights.size - 1)])
                renderElement()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        el.findViewById<View>(R.id.row_size).setOnClickListener {
            cfg.setScale(0)
            renderElement()
        }
        sizeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                cfg.setScale(if (progress < ShiroikumaFontPreferences.SCALE_MIN) 0 else progress)
                renderElement()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        container.addView(el)
        return ::renderElement
    }

    private fun fontRoleLabel(role: String): String = when (role) {
        ShiroikumaFontPreferences.DEFAULT -> getString(R.string.custom_font_role_default)
        ShiroikumaFontPreferences.HEADING -> getString(R.string.custom_role_heading)
        ShiroikumaFontPreferences.PRIMARY -> getString(R.string.custom_role_primary)
        ShiroikumaFontPreferences.SECONDARY -> getString(R.string.custom_role_secondary)
        ShiroikumaFontPreferences.TERTIARY -> getString(R.string.custom_role_tertiary)
        else -> getString(R.string.custom_role_quaternary)
    }

    private fun familyLabel(value: String): String = when {
        value.isEmpty() -> getString(R.string.custom_font_inherit)
        value.startsWith(ShiroikumaFontPreferences.FILE_PREFIX) -> File(value.removePrefix(ShiroikumaFontPreferences.FILE_PREFIX)).name
        else -> TypeFace.list.firstOrNull { it.name == value }?.typefaceName ?: value
    }

    private fun weightLabelFor(weight: Int): String {
        val idx = ShiroikumaFontPreferences.WEIGHTS.indexOf(weight)
        return if (idx >= 0) weightLabels[idx] else "$weight"
    }

    // ---- Helpers ---------------------------------------------------------------------------------------- //

    private fun useCustomTheme() {
        AppearancePreferences.setTheme(ThemeConstants.CUSTOM)
        AppearancePreferences.setLastDarkTheme(ThemeConstants.CUSTOM)
        ThemeUtils.setAppTheme(resources)
        render()
    }

    private fun refreshIfActive() {
        if (AppearancePreferences.getTheme() == ThemeConstants.CUSTOM) {
            ThemeManager.refreshTheme()
        }
    }

    private fun activeDescription(): String =
        if (AppearancePreferences.getTheme() == ThemeConstants.CUSTOM) {
            getString(R.string.custom_theme_active)
        } else {
            getString(R.string.custom_theme_inactive)
        }

    private fun swatch(color: Int): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = 8f * resources.displayMetrics.density
        d.setColor(color)
        d.setStroke((1f * resources.displayMetrics.density).toInt(), 0x55888888)
        return d
    }

    companion object {
        fun newInstance(): ShiroikumaUIScreen {
            val args = Bundle()
            val fragment = ShiroikumaUIScreen()
            fragment.arguments = args
            return fragment
        }

        const val TAG = "ShiroikumaUIScreen"
    }
}
