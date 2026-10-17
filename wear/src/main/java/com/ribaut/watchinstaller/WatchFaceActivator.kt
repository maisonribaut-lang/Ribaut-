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
    private const val DEFAULT_WATCH_FACE_PACKAGE = "com.ribaut.watchdesigner.watchfacepush.ribauttest"
    private const val TOKEN_META = "com.google.android.wearable.marketplace.DEFAULT_WATCHFACE_VALIDATION_TOKEN"

    @JvmStatic
    fun activate(context: Context, callback: Callback) {
        val app = context.applicationContext
        val apk = File(app.cacheDir, "default_watchface.apk")
        try {
            app.assets.open("default_watchface.apk").use { input ->
                apk.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            callback.onError("Préparation du cadran : ${e.message ?: e.javaClass.simpleName}")
            return
        }
        val info = app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
        val token = info.metaData?.getString(TOKEN_META)
        if (token.isNullOrBlank()) {
            callback.onError("Jeton WFF par défaut absent")
            return
        }
        installFile(context, apk, DEFAULT_WATCH_FACE_PACKAGE, token, callback)
    }

    @JvmStatic
    fun installReceived(context: Context, apk: File, packageName: String, validationToken: String, callback: Callback) {
        if (!apk.isFile || apk.length() == 0L) {
            callback.onError("Fichier de cadran reçu absent ou vide")
            return
        }
        if (validationToken.isBlank()) {
            callback.onError("Jeton de validation du cadran reçu absent")
            return
        }
        installFile(context, apk, packageName, validationToken, callback)
    }

    private fun installFile(context: Context, apk: File, packageName: String, validationToken: String, callback: Callback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext
                val manager = WatchFacePushManagerFactory.createWatchFacePushManager(app)
                val current = manager.listWatchFaces()
                val existing = current.installedWatchFaceDetails.firstOrNull { it.packageName == packageName }
                val slotId = ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    if (existing == null) manager.addWatchFace(pfd, validationToken).slotId
                    else {
                        manager.updateWatchFace(existing.slotId, pfd, validationToken)
                        existing.slotId
                    }
                }
                manager.setWatchFaceAsActive(slotId)
                withContext(Dispatchers.Main) { callback.onActivated(packageName, slotId) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("${e.javaClass.simpleName}: ${e.message ?: "erreur sans détail"}")
                }
            }
        }
    }

    interface Callback {
        fun onActivated(packageName: String, slotId: String)
        fun onError(message: String)
    }
}
