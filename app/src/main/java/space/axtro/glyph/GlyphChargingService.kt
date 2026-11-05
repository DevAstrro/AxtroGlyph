package space.axtro.glyph

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphManager
import kotlin.math.abs

class GlyphChargingService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var manager: GlyphManager? = null
    private var registered = false
    private var sessionOpen = false
    private var screenOff = false
    private var charging = false
    private var percent = 0
    private var lastShown = -1
    private var animationToken = 0
    private var completedAtThisCharge = false
    private var receiver: BroadcastReceiver? = null

    private val callback = object : GlyphManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            try {
                val m = manager ?: return
                if (!Common.is23111()) {
                    Log.w(TAG, "Unsupported device; this build targets Phone (2a)")
                    return
                }
                registered = m.register(Glyph.DEVICE_23111)
                if (!registered) return
                m.openSession()
                sessionOpen = true
                refreshGlyph()
            } catch (e: Exception) {
                Log.e(TAG, "Glyph connection failed", e)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            sessionOpen = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        screenOff = !power.isInteractive
        registerEvents()
        readBattery(null)
        try {
            manager = GlyphManager.getInstance(applicationContext)
            manager?.init(callback)
        } catch (e: Exception) {
            Log.e(TAG, "Glyph SDK init failed", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        refreshGlyph()
        return START_STICKY
    }

    private fun registerEvents() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_BATTERY_CHANGED -> readBattery(intent)
                    Intent.ACTION_SCREEN_OFF -> {
                        screenOff = true
                        refreshGlyph()
                    }
                    Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                        screenOff = false
                        cancelAnimation()
                        turnOff()
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(receiver, filter)
    }

    private fun readBattery(intent: Intent?) {
        val i = intent ?: registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        if (level >= 0) percent = (level * 100 / scale).coerceIn(0, 100)
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val wasCharging = charging
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        if (!charging) completedAtThisCharge = false
        if (charging && percent >= 100 && !completedAtThisCharge) {
            completedAtThisCharge = true
            if (screenOff) finishAnimation()
        } else if (wasCharging != charging || charging) {
            refreshGlyph()
        }
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun refreshGlyph() {
        if (!screenOff || !charging || !sessionOpen || !registered) {
            if (!screenOff || !charging) turnOff()
            return
        }
        if (percent >= 100) {
            if (!completedAtThisCharge) {
                completedAtThisCharge = true
                finishAnimation()
            }
            return
        }
        val target = percent
        animateTo(target)
    }

    // Animate in 2% increments at 70 ms: deliberate, smooth, and bounded in work.
    private fun animateTo(target: Int) {
        cancelAnimation()
        val token = animationToken
        val start = if (lastShown < 0) 0 else lastShown
        val distance = abs(target - start)
        if (distance == 0) return
        val steps = (distance / 2).coerceAtLeast(1)
        var step = 0
        fun tick() {
            if (token != animationToken || !screenOff || !charging || !sessionOpen) return
            step++
            val value = (start + (target - start) * step / steps).coerceIn(0, 100)
            showProgress(value)
            if (step < steps) handler.postDelayed({ tick() }, 70L)
        }
        tick()
    }

    private fun finishAnimation() {
        cancelAnimation()
        if (!screenOff || !sessionOpen) return
        vibrate()
        val token = animationToken
        fun blink(count: Int) {
            if (token != animationToken || !screenOff || !sessionOpen) return
            if (count >= 4) {
                turnOff()
                return
            }
            if (count % 2 == 0) showProgress(100) else turnOff()
            handler.postDelayed({ blink(count + 1) }, 550L)
        }
        blink(0)
    }

    private fun showProgress(value: Int) {
        val m = manager ?: return
        try {
            val frame = m.glyphFrameBuilder.buildChannelC().buildPeriod(1000).buildCycles(1).build()
            m.displayProgress(frame, value)
            lastShown = value
        } catch (e: GlyphException) {
            Log.e(TAG, "Glyph progress failed", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected Glyph error", e)
        }
    }

    private fun turnOff() {
        if (!sessionOpen) return
        runCatching { manager?.turnOff() }.onFailure { Log.e(TAG, "Glyph off failed", it) }
        lastShown = -1
    }

    private fun cancelAnimation() {
        animationToken++
        handler.removeCallbacksAndMessages(null)
    }

    private fun vibrate() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else @Suppress("DEPRECATION") (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
            if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") vibrator.vibrate(180)
        } catch (e: Exception) {
            Log.w(TAG, "Vibration unavailable", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Charging Glyph", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Keeps the charging Glyph feature ready while the phone is locked"
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
        return builder.setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("AxtroGlyph is ready")
            .setContentText(if (charging) "Charging · $percent%" else "Waiting for charger")
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        cancelAnimation()
        receiver?.let { runCatching { unregisterReceiver(it) } }
        if (sessionOpen) runCatching { manager?.closeSession() }
        runCatching { manager?.unInit() }
        sessionOpen = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AxtroGlyphService"
        private const val CHANNEL_ID = "axtro_glyph_charging"
        private const val NOTIFICATION_ID = 701
    }
}
