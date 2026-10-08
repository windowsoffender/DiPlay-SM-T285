package com.shilapi.xcertplay.compat

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import android.widget.Switch

/**
 * [View.setForeground] needs API 23 except on a [FrameLayout]. Before that other views get no
 * foreground: the focus ring and ripple are cosmetic and the background still shows the state.
 */
fun View.setForegroundCompat(drawable: Drawable?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) foreground = drawable
    else (this as? FrameLayout)?.foreground = drawable
}

/** [Switch.setThumbTintList] and [Switch.setTrackTintList] need API 23; before that the drawables are tinted. */
fun Switch.setTintListsCompat(thumb: ColorStateList, track: ColorStateList) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        thumbTintList = thumb
        trackTintList = track
    } else {
        thumbDrawable?.mutate()?.setTintList(thumb)
        trackDrawable?.mutate()?.setTintList(track)
    }
}
