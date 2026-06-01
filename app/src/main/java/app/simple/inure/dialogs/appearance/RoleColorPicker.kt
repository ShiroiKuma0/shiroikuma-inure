package app.simple.inure.dialogs.appearance

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentManager
import app.simple.inure.R
import app.simple.inure.adapters.preferences.AdapterPickedColors
import app.simple.inure.decorations.colorpicker.ColorPickerView
import app.simple.inure.decorations.corners.DynamicCornerAccentColor
import app.simple.inure.decorations.corners.DynamicCornerEditText
import app.simple.inure.decorations.overscroll.CustomHorizontalRecyclerView
import app.simple.inure.decorations.ripple.DynamicRippleTextView
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.extensions.fragments.ScopedBottomSheetFragment
import app.simple.inure.preferences.ColorPickerPreferences

/**
 * Fork (白い熊 Inure UI): full-page colour picker for an arbitrary role. A giant hue/saturation circle with the
 * previously-picked colours floating over its top, R/G/B/A sliders for precise control, a hex field and a live
 * preview swatch, and an OK button to accept. Returns the chosen ARGB through [onColorPicked].
 */
class RoleColorPicker : ScopedBottomSheetFragment() {

    private lateinit var colorPickerView: ColorPickerView
    private lateinit var colorsRecyclerView: CustomHorizontalRecyclerView
    private lateinit var hex: DynamicCornerEditText
    private lateinit var strip: DynamicCornerAccentColor
    private lateinit var set: DynamicRippleTextView
    private lateinit var cancel: DynamicRippleTextView

    private lateinit var seekR: AppCompatSeekBar
    private lateinit var seekG: AppCompatSeekBar
    private lateinit var seekB: AppCompatSeekBar
    private lateinit var seekA: AppCompatSeekBar
    private lateinit var valueR: TypeFaceTextView
    private lateinit var valueG: TypeFaceTextView
    private lateinit var valueB: TypeFaceTextView
    private lateinit var valueA: TypeFaceTextView

    private var updating = false

    var onColorPicked: ((Int) -> Unit)? = null

    private val initialColor: Int
        get() = requireArguments().getInt(BUNDLE_INITIAL_COLOR, Color.WHITE)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_role_color_picker, container, false)

        colorPickerView = view.findViewById(R.id.color_picker_view)
        colorsRecyclerView = view.findViewById(R.id.colors_recycler_view)
        hex = view.findViewById(R.id.color_hex_code)
        strip = view.findViewById(R.id.color_strip)
        set = view.findViewById(R.id.set)
        cancel = view.findViewById(R.id.cancel)

        bindSlider(view.findViewById(R.id.row_r), "R").let { (s, v) -> seekR = s; valueR = v }
        bindSlider(view.findViewById(R.id.row_g), "G").let { (s, v) -> seekG = s; valueG = v }
        bindSlider(view.findViewById(R.id.row_b), "B").let { (s, v) -> seekB = s; valueB = v }
        bindSlider(view.findViewById(R.id.row_a), "A").let { (s, v) -> seekA = s; valueA = v }

        return view
    }

    private fun bindSlider(row: View, label: String): Pair<AppCompatSeekBar, TypeFaceTextView> {
        row.findViewById<TypeFaceTextView>(R.id.slider_label).text = label
        return row.findViewById<AppCompatSeekBar>(R.id.slider_seek) to row.findViewById(R.id.slider_value)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        colorsRecyclerView.setBackgroundColor(Color.TRANSPARENT)

        colorsRecyclerView.adapter = AdapterPickedColors(ColorPickerPreferences.getColorHistory()) {
            runCatching { Color.parseColor(it) }.getOrNull()?.let { c -> refreshFromColor(c, setSliders = true) }
        }

        colorPickerView.setColorListener { color, _, isUser ->
            if (isUser && !updating) {
                refreshFromColor(Color.argb(seekA.progress, Color.red(color), Color.green(color), Color.blue(color)), setSliders = true)
            }
        }

        val sliderListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !updating) refreshFromColor(currentColor(), setSliders = false)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        seekR.setOnSeekBarChangeListener(sliderListener)
        seekG.setOnSeekBarChangeListener(sliderListener)
        seekB.setOnSeekBarChangeListener(sliderListener)
        seekA.setOnSeekBarChangeListener(sliderListener)

        hex.doOnTextChanged { text, _, _, _ ->
            if (updating) return@doOnTextChanged
            val s = text?.toString()?.trim().orEmpty()
            if (s.isEmpty()) return@doOnTextChanged
            runCatching { Color.parseColor(if (s.startsWith("#")) s else "#$s") }
                .getOrNull()?.let { refreshFromColor(it, setSliders = true) }
        }

        refreshFromColor(initialColor, setSliders = true)

        set.setOnClickListener {
            val color = currentColor()
            ColorPickerPreferences.setColorHistory(hexOf(color))
            onColorPicked?.invoke(color)
            dismiss()
        }
        cancel.setOnClickListener { dismiss() }
    }

    private fun currentColor(): Int =
        Color.argb(seekA.progress, seekR.progress, seekG.progress, seekB.progress)

    private fun refreshFromColor(color: Int, setSliders: Boolean) {
        updating = true
        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        if (setSliders) {
            seekR.progress = r; seekG.progress = g; seekB.progress = b; seekA.progress = a
        }
        valueR.text = r.toString(); valueG.text = g.toString(); valueB.text = b.toString(); valueA.text = a.toString()
        hex.setText(hexOf(color))
        applyStrip(color)
        colorPickerView.setColor(Color.rgb(r, g, b)) // move the pointer; does not fire the listener
        updating = false
    }

    private fun hexOf(color: Int): String =
        if (Color.alpha(color) == 255) String.format("#%06X", 0xFFFFFF and color)
        else String.format("#%08X", color)

    private fun applyStrip(color: Int) {
        strip.backgroundTintList = ColorStateList.valueOf(color)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            strip.outlineSpotShadowColor = color
            strip.outlineAmbientShadowColor = color
        }
    }

    companion object {
        private const val BUNDLE_INITIAL_COLOR = "initial_color"

        fun newInstance(initialColor: Int): RoleColorPicker {
            val args = Bundle()
            args.putInt(BUNDLE_INITIAL_COLOR, initialColor)
            val fragment = RoleColorPicker()
            fragment.arguments = args
            return fragment
        }

        fun FragmentManager.showRoleColorPicker(initialColor: Int, onColorPicked: (Int) -> Unit): RoleColorPicker {
            val dialog = newInstance(initialColor)
            dialog.onColorPicked = onColorPicked
            dialog.show(this, "role_color_picker")
            return dialog
        }
    }
}
