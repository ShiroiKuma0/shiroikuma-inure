package app.simple.inure.dialogs.appearance

import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentManager
import app.simple.inure.R
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.extensions.fragments.ScopedBottomSheetFragment
import app.simple.inure.preferences.ShiroikumaFontPreferences
import app.simple.inure.util.TypeFace
import java.io.File

/**
 * Fork (白い熊 Inure UI): font-family chooser for a single role. Lists "Inherit", every bundled family (each
 * rendered in its own typeface), any imported fonts, and an "Add custom font…" entry that imports a `.ttf`/`.otf`
 * via SAF. Returns the chosen value through [onPicked]: "" (inherit), a bundled code, or "file:&lt;path&gt;".
 */
class FontFamilyPicker : ScopedBottomSheetFragment() {

    private lateinit var container: LinearLayout
    var onPicked: ((String) -> Unit)? = null

    private val openFont = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            val dir = File(requireContext().filesDir, "shiroikuma_fonts").apply { mkdirs() }
            // Keep the font's real filename so it shows properly in the picker / value labels.
            val name = (queryDisplayName(uri) ?: ("font_" + System.currentTimeMillis() + ".ttf"))
                .replace('/', '_').replace('\\', '_').ifBlank { "font.ttf" }
            val dest = File(dir, name)
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            dest.absolutePath
        }.onSuccess { path ->
            ShiroikumaFontPreferences.addImportedFont(path)
            TypeFace.clearFileTypefaceCache()
            onPicked?.invoke(ShiroikumaFontPreferences.FILE_PREFIX + path)
            dismiss()
        }.onFailure {
            showWarning("Could not import font")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_font_picker, c, false)
        container = view.findViewById(R.id.font_picker_container)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildList()
    }

    private fun buildList() {
        container.removeAllViews()

        addItem(getString(R.string.custom_font_inherit), null, null, onClick = { pick("") })

        TypeFace.list.forEach { model ->
            val tf = runCatching { ResourcesCompat.getFont(requireContext(), model.typeFaceResId) }.getOrNull()
            addItem(model.typefaceName, null, tf, onClick = { pick(model.name) })
        }

        ShiroikumaFontPreferences.getImportedFonts().forEach { path ->
            val tf = TypeFace.typefaceFromFile(path)
            addItem(File(path).name, getString(R.string.custom_font_remove_hint), tf,
                    onClick = { pick(ShiroikumaFontPreferences.FILE_PREFIX + path) },
                    onLongClick = {
                        ShiroikumaFontPreferences.removeImportedFont(path)
                        TypeFace.clearFileTypefaceCache()
                        buildList()
                    })
        }

        addItem(getString(R.string.custom_font_add), null, null, onClick = {
            openFont.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream", "*/*"))
        })
    }

    private fun pick(value: String) {
        onPicked?.invoke(value)
        dismiss()
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        requireContext().contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

    private fun addItem(name: String, caption: String?, preview: Typeface?, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
        val item = LayoutInflater.from(requireContext()).inflate(R.layout.adapter_shiroikuma_font_pick_item, container, false)
        val nameView = item.findViewById<TypeFaceTextView>(R.id.font_name)
        nameView.text = name
        if (preview != null) nameView.typeface = preview
        if (caption != null) {
            val fileView = item.findViewById<TypeFaceTextView>(R.id.font_file)
            fileView.text = caption
            fileView.visibility = View.VISIBLE
        }
        item.setOnClickListener { onClick() }
        if (onLongClick != null) {
            item.setOnLongClickListener { onLongClick(); true }
        }
        container.addView(item)
    }

    companion object {
        fun newInstance(): FontFamilyPicker {
            val args = Bundle()
            val fragment = FontFamilyPicker()
            fragment.arguments = args
            return fragment
        }

        fun FragmentManager.showFontFamilyPicker(onPicked: (String) -> Unit): FontFamilyPicker {
            val dialog = newInstance()
            dialog.onPicked = onPicked
            dialog.show(this, "font_family_picker")
            return dialog
        }
    }
}
