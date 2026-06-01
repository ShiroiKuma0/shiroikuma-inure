package app.simple.inure.themes.data

import androidx.annotation.ColorInt

data class ViewGroupTheme(
        @ColorInt
        var background: Int,

        @ColorInt
        var viewerBackground: Int,

        @ColorInt
        var highlightBackground: Int,

        @ColorInt
        var selectedBackground: Int,

        @ColorInt
        var dividerBackground: Int
)