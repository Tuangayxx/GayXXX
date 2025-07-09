package com.Fullboys

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class FullboysProvider : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Fullboys())
    }
}
