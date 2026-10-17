package com.ribaut.watchinstaller

import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import androidx.wear.watchfacepush.WatchFacePushManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object WatchFaceActivator {
    private const val WATCH_FACE_PACKAGE = "com.ribaut.watchdesigner.watchfacepush.ribauttest"
    private const val TOKEN_META = "com.google.android.wearable.marketplace.DEFAULT_WATCHFACE_VALIDATION_TOKEN"

    @JvmStatic
    fun activate(context: Context, callback: Callback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext
                val manager = WatchFacePushManagerFactory.createWatchFacePushManager(app)
                val info = app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
                val token = info.metaData?.getString(TOKEN_META)
                    ?: throw IllegalStateException("Jeton WFF absent")

                val apk = File(app.cacheDir, "default_watchface.apk")
                app.assets.open("default_watchface.apk").use { input ->
                    apk.outputStream().use { output -> input.copyTo(output) }
                }

                val current = manager.listWatchFaces()
                val existing = current.installedWatchFaceDetails.firstOrNull { it.packageName == WATCH_FACE_PACKAGE }
                val slotId = ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    if (existing == null) manager.addWatchFace(pfd, token).slotId
                    else {
                        manager.updateWatchFace(existing.slotId, pfd, token)
                        existing.slotId
                    }
                }
                manager.setWatchFaceAsActive(slotId)
                withContext(Dispatchers.Main) { callback.onActivated(WATCH_FACE_PACKAGE, slotId) }
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
