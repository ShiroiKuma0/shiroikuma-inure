package app.simple.inure.util

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.simple.inure.decorations.theme.ThemeConstraintLayout
import app.simple.inure.decorations.theme.ThemeLinearLayout
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.preferences.ShiroikumaInstallerPreferences

/**
 * Fork (白い熊 Inure UI): applies the installer-scoped overrides ([ShiroikumaInstallerPreferences]) onto the
 * installer screen. Only set values override; anything unset keeps the global theme/font styling.
 *
 * The background can't just be set on the fragment root — that root is transparent and the visible surfaces
 * (the tab's RecyclerView, the bottom ThemeLinearLayout bar, etc.) each paint the theme background over it.
 * So [tintBackground] walks the tree and recolours those surface containers. It must be re-run when a new tab
 * page is shown (ViewPager2 creates tab fragments lazily), hence the public method called from the installer.
 */
object ShiroikumaInstallerStyle {

    fun apply(root: View?, name: TextView?, packageId: TextView?, version: TextView?, buttons: List<TextView?>) {
        applyText(name, ShiroikumaInstallerPreferences.NAME)
        applyText(packageId, ShiroikumaInstallerPreferences.PACKAGE)
        applyText(version, ShiroikumaInstallerPreferences.VERSION)
        buttons.forEach { applyText(it, ShiroikumaInstallerPreferences.BUTTONS) }
        tintBackground(root)
    }

    /** Recolour every background-surface container under [root] to the installer background (if set). */
    fun tintBackground(root: View?) {
        root ?: return
        if (!ShiroikumaInstallerPreferences.isBackgroundSet()) return
        val color = ShiroikumaInstallerPreferences.getBackground(0)
        root.setBackgroundColor(color)
        recolour(root, color)
    }

    private fun recolour(view: View, color: Int) {
        if (view is RecyclerView || view is ThemeLinearLayout || view is ThemeConstraintLayout) {
            view.setBackgroundColor(color)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) recolour(view.getChildAt(i), color)
        }
    }

    private fun applyText(tv: TextView?, item: String) {
        tv ?: return
        val family = ShiroikumaInstallerPreferences.getFamily(item)
        val weight = ShiroikumaInstallerPreferences.getWeight(item)
        if (family.isNotEmpty() || weight != 0) {
            tv.typeface = TypeFace.resolveRoleTypeface(family, weight, TypeFaceTextView.REGULAR, tv.context)
        }
        val scale = ShiroikumaInstallerPreferences.getScale(item)
        if (scale > 0) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tv.textSize * scale / 100f)
        }
        if (ShiroikumaInstallerPreferences.isColorSet(item)) {
            tv.setTextColor(ShiroikumaInstallerPreferences.getColor(item, tv.currentTextColor))
        }
    }
}
