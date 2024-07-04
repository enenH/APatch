package me.bmax.apatch.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import me.bmax.apatch.Native
import me.bmax.apatch.R
import me.bmax.apatch.TAG
import me.bmax.apatch.ui.MainActivity
import me.bmax.apatch.ui.screen.antiRecord
import me.bmax.apatch.ui.screen.recordBlack
import me.bmax.apatch.util.RecorderFakeUtils
import java.util.UUID
import kotlin.concurrent.thread

class MyService : Service() {

    val HANDLER = Handler(Looper.getMainLooper())
    private val NOTIFICATION_ID = 1001 // 通知栏 ID
    private val CHANNEL_ID = "ForegroundServiceChannel" // 通知渠道 ID
    private var screenWidth = 0
    private var screenHeight = 0
    private lateinit var dv: View
    private lateinit var windowView: View
    private var threadStart = false

    private lateinit var manager: WindowManager
    private val windowlp = WindowManager.LayoutParams()
    private val surfacelp = WindowManager.LayoutParams()

    companion object {
        lateinit var service: MyService
    }

    override fun onCreate() {
        super.onCreate()
        service = this
        manager = getSystemService(WINDOW_SERVICE) as WindowManager
        showFloatWindow(antiRecord, recordBlack)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_ID,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(
            NotificationManager::class.java
        )
        notificationManager.createNotificationChannel(channel)
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(UUID.randomUUID().toString())
            .setContentText(UUID.randomUUID().toString())
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        }
        return START_STICKY
    }

    fun updateWindowSize() {
        val menuPos: FloatArray = Native.getWindowSize()
        val menuPosInt = menuPos.map { it.toInt() }.toIntArray()
        if (menuPosInt[0] != windowlp.x || menuPosInt[1] != windowlp.y || menuPosInt[2] != windowlp.width || menuPosInt[3] != windowlp.height) {
            windowlp.x = menuPosInt[0]
            windowlp.y = menuPosInt[1]
            windowlp.width = menuPosInt[2]
            windowlp.height = menuPosInt[3]
            manager.updateViewLayout(windowView, windowlp)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showFloatWindow(
        isHide: Boolean,
        isSecure: Boolean
    ) {
        val point = Point()
        manager.getDefaultDisplay().getRealSize(point)
        screenWidth = point.x.coerceAtLeast(point.y)
        screenHeight = point.x.coerceAtMost(point.y)

        windowlp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        windowlp.gravity = Gravity.TOP or Gravity.START
        windowlp.format = PixelFormat.TRANSPARENT
        windowlp.x = 0
        windowlp.y = 0
        windowlp.width = 0
        windowlp.height = 0
        windowlp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or  //硬件加速
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_ATTACHED_IN_DECOR //显示在状态栏上方(貌似高版本无效
        if (isSecure) windowlp.flags = windowlp.flags or WindowManager.LayoutParams.FLAG_SECURE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            windowlp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES //覆盖刘海
        }
        windowView = View(this)
        windowView.setBackgroundColor(Color.TRANSPARENT);
        windowView.alpha = 0.8f
        windowView.setOnTouchListener { _: View?, event: MotionEvent ->
            HANDLER.postDelayed({ updateWindowSize() }, 200)
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    Native.setTouch(event.rawX, event.rawY)
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_UP -> {
                    Native.setTouch(-1f, -1f)
                    return@setOnTouchListener true
                }
            }
            false
        }
        RecorderFakeUtils.setFakeRecorderWindowLayoutParams(windowlp)
        manager.addView(windowView, windowlp)

        surfacelp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        surfacelp.gravity = Gravity.TOP or Gravity.START
        surfacelp.format = PixelFormat.TRANSPARENT
        surfacelp.x = 0
        surfacelp.y = 0
        surfacelp.width = screenWidth
        surfacelp.height = WindowManager.LayoutParams.MATCH_PARENT //取巧 让他还会进行surfaceChanged回调

        surfacelp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or  //不接受触控
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or  //硬件加速
                WindowManager.LayoutParams.FLAG_FULLSCREEN or  //隐藏状态栏导航栏以全屏(貌似没什么用)
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or  //忽略屏幕边界
                WindowManager.LayoutParams.FLAG_LAYOUT_ATTACHED_IN_DECOR or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN //布局充满整个屏幕 忽略应用窗口限制
        if (isSecure) surfacelp.flags =
            surfacelp.flags or WindowManager.LayoutParams.FLAG_SECURE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            surfacelp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES //覆盖刘海
        }
        if (isHide) RecorderFakeUtils.setFakeRecorderWindowLayoutParams(surfacelp)
        dv = if (isHide) DrawViewTexture(this) else DrawView(this)
        manager.addView(dv, surfacelp)

        thread {
            threadStart = true
            while (threadStart) {
                HANDLER.post { updateWindowSize() }
                try {
                    Thread.sleep(500)
                } catch (_: InterruptedException) {
                }
            }
        }
    }

    fun hideFloatWindow(isAppDraw: Boolean) {
        Log.d(TAG, "hideFloatWindow: ")

        threadStart = false

        manager.removeViewImmediate(dv)
        manager.removeViewImmediate(windowView)

        if (!isAppDraw) {
            Destroyed()
        }
    }

    fun Created(surface: Surface?) {
        Native.surfaceCreated(surface, screenWidth, screenHeight)
    }

    fun Changed(width: Int, height: Int) {
        if (height in width - 200..width + 200) {
            if (height in screenWidth - 200..screenWidth + 200) {
                surfacelp.width = screenHeight
                manager.updateViewLayout(dv, surfacelp)
            } else if (height in screenHeight - 200..screenHeight + 200) {
                surfacelp.width = screenWidth
                manager.updateViewLayout(dv, surfacelp)
            }
        }
        updateWindowSize()
    }

    fun Destroyed() {
        Native.surfaceDestroyed()
    }

    inner class DrawView(context: Context?) : SurfaceView(context), SurfaceHolder.Callback {
        init {
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSPARENT)
            holder.addCallback(this)
        }

        override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
            Created(surfaceHolder.surface)
        }

        override fun surfaceChanged(
            surfaceHolder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            Changed(width, height)
        }

        override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
            Destroyed()
        }
    }


    inner class DrawViewTexture(context: Context) : TextureView(context),
        SurfaceTextureListener {
        init {
            surfaceTextureListener = this
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            Created(Surface(surface))
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            Changed(width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            Destroyed()
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        hideFloatWindow(true)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}