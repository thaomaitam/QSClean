package com.KTA.QSclean.hook

import android.app.Application
import android.content.Context
import android.util.Log
import com.github.kyuubiran.ezxhelper.utils.*
import de.robv.android.xposed.XC_MethodHook
import java.io.File

object QScleanHook : BaseHook() {
    private const val TAG = "Cleancache"

    override fun init() {
        // Hook vào Application.onCreate
        findMethod("android.app.Application") {
            name == "onCreate" && parameterTypes.isEmpty()
        }.hookAfter {
            val context = it.thisObject as? Context
            if (context != null) {
                cleanCache(context)
                Log.i(TAG, "Cleaned cache for: ${context.packageName}")
            } else {
                Log.e(TAG, "Context is null in Application.onCreate")
            }
        }
    }

    private fun cleanCache(context: Context) {
        deleteCacheDir(context.cacheDir, "internal")
        deleteCacheDir(context.externalCacheDir, "external")
        deleteCacheDir(context.codeCacheDir, "code")
    }

    private fun deleteCacheDir(dir: File?, type: String) {
        if (dir != null && dir.exists() && dir.isDirectory) {
            Log.i(TAG, "Clearing $type cache at: ${dir.absolutePath}")
            dir.deleteRecursively()
            dir.mkdirs()
        } else {
            Log.w(TAG, "Cache dir ($type) not found or invalid")
        }
    }
}