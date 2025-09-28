package com.KTA.QSclean.hook

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import com.github.kyuubiran.ezxhelper.utils.*

object PixelLauncherInsetsHook : BaseHook() {

    private const val LAUNCHER_ACTIVITY_CLASS = "com.google.android.apps.nexuslauncher.NexusLauncherActivity"

    override fun init() {
        // Sử dụng runCatching để xử lý lỗi một cách an toàn
        runCatching {
            // Hook vào phương thức onCreate của Activity chính trong Pixel Launcher
            findMethod(LAUNCHER_ACTIVITY_CLASS) {
                name == "onCreate" && parameterTypes.contentEquals(arrayOf(Bundle::class.java))
            }.hookAfter { param ->
                Log.i("Launcher onCreate detected. Applying edge-to-edge fix.")
                val activity = param.thisObject as Activity
                val window: Window = activity.window

                // Bước 1: Yêu cầu ứng dụng vẽ nội dung ra toàn màn hình (edge-to-edge)
                // Đây là bước quan trọng nhất để loại bỏ khoảng đen ở dưới
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.setDecorFitsSystemWindows(false)
                } else {
                    @Suppress("DEPRECATION")
                    var flags = window.decorView.systemUiVisibility
                    flags = flags or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE 
                                  or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN 
                                  or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
                    window.decorView.systemUiVisibility = flags
                }

                // Bước 2: Tìm DragLayer, là View gốc của launcher, và lắng nghe WindowInsets
                val dragLayer = activity.getObjectFieldAs<View>("mDragLayer")
                if (dragLayer == null) {
                    Log.e("Could not find mDragLayer view.")
                    return@hookAfter
                }

                dragLayer.setOnApplyWindowInsetsListener { view, insets ->
                    try {
                        // Lấy giá trị đệm (insets) thực tế từ hệ thống
                        val statusBarHeight = insets.systemWindowInsetTop
                        val navBarHeight = insets.systemWindowInsetBottom

                        // Tìm Workspace (khu vực chính) và Hotseat (dock)
                        val workspace = activity.getObjectFieldAs<View>("mWorkspace")
                        val hotseat = activity.getObjectFieldAs<View>("mHotseat")

                        // Áp dụng padding cho Workspace để không bị thanh trạng thái che
                        workspace?.setPadding(
                            workspace.paddingLeft,
                            statusBarHeight,
                            workspace.paddingRight,
                            workspace.paddingBottom
                        )
                        
                        // Áp dụng padding cho Hotseat để không bị thanh điều hướng che
                        hotseat?.setPadding(
                            hotseat.paddingLeft,
                            hotseat.paddingTop,
                            hotseat.paddingRight,
                            navBarHeight
                        )

                        Log.i("Applied insets successfully. Top: $statusBarHeight, Bottom: $navBarHeight")

                    } catch (e: Throwable) {
                        Log.e("Failed to apply insets to Workspace/Hotseat", e)
                    }
                    
                    // Trả về insets đã được xử lý để hệ thống không tự động áp dụng lại
                    insets.consumeSystemWindowInsets()
                }

                // Yêu cầu View áp dụng lại insets sau khi đã thiết lập listener
                dragLayer.requestApplyInsets()
            }
        }.logexIfThrow("Failed to hook $LAUNCHER_ACTIVITY_CLASS")
    }
}
