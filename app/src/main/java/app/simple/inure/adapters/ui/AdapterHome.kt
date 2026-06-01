package app.simple.inure.adapters.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import app.simple.inure.R
import app.simple.inure.constants.Colors
import app.simple.inure.decorations.overscroll.VerticalListViewHolder
import app.simple.inure.decorations.ripple.DynamicRippleImageButton
import app.simple.inure.decorations.ripple.DynamicRippleLinearLayoutWithFactor
import app.simple.inure.decorations.theme.ThemeIcon
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.popups.home.PopupMenuLayout
import app.simple.inure.preferences.AccessibilityPreferences
import app.simple.inure.preferences.DevelopmentPreferences
import app.simple.inure.preferences.HomePreferences
import app.simple.inure.preferences.TrialPreferences
import app.simple.inure.util.ArrayUtils.circularGet
import app.simple.inure.util.ConditionUtils.isZero
import app.simple.inure.util.RecyclerViewUtils

class AdapterHome(private val list: List<Pair<Int, Int>>) : RecyclerView.Adapter<VerticalListViewHolder>() {

    private lateinit var adapterHomeMenuCallbacks: AdapterHomeMenuCallbacks

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerticalListViewHolder {
        return when (viewType) {
            RecyclerViewUtils.TYPE_ITEM -> {
                when (HomePreferences.getMenuLayout()) {
                    PopupMenuLayout.VERTICAL -> {
                        Holder(LayoutInflater.from(parent.context)
                                   .inflate(R.layout.adapter_home_menu_vertical, parent, false))
                    }
                    PopupMenuLayout.GRID -> {
                        Holder(LayoutInflater.from(parent.context)
                                   .inflate(R.layout.adapter_home_menu, parent, false))
                    }
                    else -> {
                        throw IllegalArgumentException("Invalid menu layout")
                    }
                }
            }
            RecyclerViewUtils.TYPE_DIVIDER -> {
                Divider(LayoutInflater.from(parent.context)
                            .inflate(R.layout.adapter_divider_preferences, parent, false))
            }
            RecyclerViewUtils.TYPE_HEADER -> {
                if (DevelopmentPreferences.get(DevelopmentPreferences.EXPAND_HOME_HEADER)) {
                    Header(LayoutInflater.from(parent.context)
                               .inflate(R.layout.adapter_header_home_expanded, parent, false))
                } else {
                    Header(LayoutInflater.from(parent.context)
                               .inflate(R.layout.adapter_header_home, parent, false))
                }
            }
            else -> {
                throw RuntimeException("there is no type that matches the type $viewType + make sure your using types correctly")
            }
        }
    }

    override fun onBindViewHolder(holder: VerticalListViewHolder, position: Int) {
        if (holder is Holder) {
            holder.icon.transitionName = holder.itemView.context.getString(list[position].second)
            holder.icon.setImageResource(list[position].first)
            holder.text.text = holder.itemView.context.getString(list[position].second)

            // Fork (白い熊 Inure UI): main-screen icon size — scale the home menu icon (up to 3x), or the
            // designed size when unset. Recycling-safe (base captured at holder creation).
            run {
                val scale = app.simple.inure.preferences.ShiroikumaUIPreferences.getMainListIconScale()
                val lp = holder.icon.layoutParams
                lp.width = if (scale >= 0) holder.baseIconSize * scale / 100 else holder.baseIconSize
                lp.height = lp.width
                holder.icon.layoutParams = lp
            }

            if (AccessibilityPreferences.isColorfulIcons()) {
                holder.icon.imageTintList = ColorStateList(arrayOf(intArrayOf(
                        android.R.attr.state_enabled
                ), intArrayOf()), intArrayOf(
                        Colors.getColors().circularGet(position)!!,
                        Colors.getColors().circularGet(position)!!
                ))

                if (AccessibilityPreferences.isHighlightMode()) {
                    holder.container.setHighlightColor(Colors.getColors()[position])
                }
            }

            // Fork (白い熊 Inure UI): main-screen icon colour + label font/size/colour (overrides only when set).
            run {
                val prefs = app.simple.inure.preferences.ShiroikumaUIPreferences
                if (prefs.isIconColorSet()) {
                    holder.icon.imageTintList = ColorStateList.valueOf(prefs.getIconColor(0))
                }
                val family = prefs.getTextFamily()
                val weight = prefs.getTextWeight()
                if (family.isNotEmpty() || weight != 0) {
                    holder.text.typeface = app.simple.inure.util.TypeFace.resolveRoleTypeface(
                            family, weight, TypeFaceTextView.REGULAR, holder.itemView.context)
                }
                val textScale = prefs.getTextScale()
                if (textScale > 0) {
                    holder.text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, holder.baseTextSize * textScale / 100f)
                }
                if (prefs.isTextColorSet()) {
                    holder.text.setTextColor(prefs.getTextColor(0))
                }
            }

            holder.container.setOnClickListener {
                adapterHomeMenuCallbacks.onMenuItemClicked(list[position].second, holder.icon)
            }
        } else if (holder is Header) {
            if (TrialPreferences.isFullVersion()) {
                holder.trial.visibility = View.GONE
            } else {
                holder.trial.visibility = View.VISIBLE
            }

            holder.appIcon.setOnClickListener {
                adapterHomeMenuCallbacks.onMenuItemClicked(R.string.app_name, holder.appIcon)
            }

            holder.trial.setOnClickListener {
                adapterHomeMenuCallbacks.onMenuItemClicked(R.string.purchase, holder.trial)
            }

            holder.search.setOnClickListener {
                adapterHomeMenuCallbacks.onMenuItemClicked(R.string.search, holder.search)
            }

            holder.settings.setOnClickListener {
                adapterHomeMenuCallbacks.onMenuItemClicked(R.string.menus, holder.settings)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (list[position].first == 1) {
            RecyclerViewUtils.TYPE_HEADER
        } else {
            if (list[position].first.isZero()) {
                RecyclerViewUtils.TYPE_DIVIDER
            } else {
                RecyclerViewUtils.TYPE_ITEM
            }
        }
    }

    inner class Holder(itemView: View) : VerticalListViewHolder(itemView) {
        val icon: ThemeIcon = itemView.findViewById(R.id.adapter_app_info_menu_icon)
        val text: TypeFaceTextView = itemView.findViewById(R.id.adapter_app_info_menu_text)
        val container: DynamicRippleLinearLayoutWithFactor = itemView.findViewById(R.id.adapter_app_info_menu_container)

        // Fork: base icon size + text size captured at creation (before any scaling) for the main-screen styling.
        val baseIconSize: Int = icon.layoutParams.width
        val baseTextSize: Float = text.textSize

        init {
            text.isSelected = true

            if (AccessibilityPreferences.isColorfulIcons()) {
                icon.setTintType(3)
            }
        }
    }

    inner class Divider(itemView: View) : VerticalListViewHolder(itemView)

    inner class Header(itemView: View) : VerticalListViewHolder(itemView) {
        val appIcon: DynamicRippleImageButton = itemView.findViewById(R.id.app_icon)
        val trial: DynamicRippleImageButton = itemView.findViewById(R.id.home_purchase)
        val search: DynamicRippleImageButton = itemView.findViewById(R.id.search)
        val settings: DynamicRippleImageButton = itemView.findViewById(R.id.settings)
    }

    fun setOnAppInfoMenuCallback(adapterHomeMenuCallbacks: AdapterHomeMenuCallbacks) {
        this.adapterHomeMenuCallbacks = adapterHomeMenuCallbacks
    }

    interface AdapterHomeMenuCallbacks {
        fun onMenuItemClicked(source: Int, icon: ImageView)
    }
}
