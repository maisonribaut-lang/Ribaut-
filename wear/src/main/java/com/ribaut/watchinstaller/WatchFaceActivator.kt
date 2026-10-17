package com.ribaut.watchinstaller

import android.content.Context
import androidx.wear.watchfacepush.WatchFacePushManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object WatchFaceActivator {
    private const val WATCH_FACE_PACKAGE = "com.ribaut.watchdesigner.watchfacepush.ribauttest"

    @JvmStatic
    fun activate(context: Context, callback: Callback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val manager = WatchFacePushManagerFactory.createWatchFacePushManager(context.applicationContext)
                val response = manager.listWatchFaces()
                val details = response.installedWatchFaceDetails.firstOrNull { it.packageName == WATCH_FACE_PACKAGE }
                    ?: response.installedWatchFaceDetails.firstOrNull()

                if (details == null) {
                    withContext(Dispatchers.Main) { callback.onError("Cadran Ribaut non trouvé dans Watch Face Push") }
                    return@launch
                }

                manager.setWatchFaceAsActive(details.slotId)
                withContext(Dispatchers.Main) { callback.onActivated(details.packageName, details.slotId) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback.onError(e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    interface Callback {
        fun onActivated(packageName: String, slotId: String)
        fun onError(message: String)
    }
}
