package com.spotlight.spotlightapp.utilities.viewutils

import android.view.View
import androidx.databinding.BindingAdapter

object BindingAdapters {
    @BindingAdapter("willShow")
    @JvmStatic
    fun toggleVisibility(view: View, willShow: Boolean) {
        view.visibility = if (willShow) View.VISIBLE else View.GONE
    }
}