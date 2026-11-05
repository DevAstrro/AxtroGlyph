package space.axtro.glyph

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphManager

private const val TAG = "AxtroGlyph"
private val Background = Color(0xFF08090B)
private val Panel = Color(0xFF17191E)
private val Muted = Color(0xFF9CA3AF)
private val Accent = Color(0xFFB8D7FF)

class MainActivity : ComponentActivity() {
    private var batteryPercent by mutableIntStateOf(0)
    private var charging by mutableStateOf(false)
    private var glyphStatus by mutableStateOf("Connecting to Nothing Glyph service…")
    private var batteryReceiver: BroadcastReceiver? = null

    private var glyphManager: GlyphManager? = null
    private var glyphSessionOpen = false
    private var glyphRegistered = false

    private val glyphCallback = object : GlyphManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            val manager = glyphManager ?: return
            try {
                if (!Common.is23111()) {
                    glyphStatus = "This build targets Nothing Phone (2a)"
                    return
                }
                glyphRegistered = manager.register(Glyph.DEVICE_23111)
                if (!glyphRegistered) {
                    glyphStatus = "Glyph permission was not granted"
                    return
                }
                manager.openSession()
                glyphSessionOpen = true
                glyphStatus = "Connected · Phone (2a)"
                updateGlyph()
            } catch (error: Exception) {
                Log.e(TAG, "Glyph service setup failed", error)
                glyphStatus = "Glyph connection failed: ${error.message ?: "unknown error"}"
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            glyphSessionOpen = false
            glyphStatus = "Glyph service disconnected"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readBattery(null)
        registerBatteryUpdates()

        // Nothing's official SDK: initialise, register the supported model, then open a session.
        try {
            glyphManager = GlyphManager.getInstance(applicationContext)
            glyphManager?.init(glyphCallback)
        } catch (error: Exception) {
            Log.e(TAG, "Unable to initialise Glyph SDK", error)
            glyphStatus = "Glyph SDK unavailable: ${error.message ?: "unknown error"}"
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Background,
                surface = Panel,
                primary = Accent,
                onBackground = Color.White,
                onSurface = Color.White
            )) {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("AXTRO / LABS", color = Accent, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold)
                            Text("Glyph Meter", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                            Text("Live charging progress · Nothing Phone (2a)", color = Muted, fontSize = 14.sp)
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Panel).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("BATTERY LEVEL", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp)
                                Box(modifier = Modifier.clip(CircleShape).background(if (charging) Color(0xFF20372D) else Color(0xFF292C33)).padding(horizontal = 11.dp, vertical = 6.dp)) {
                                    Text(if (charging) "CHARGING" else "ON BATTERY", color = if (charging) Color(0xFF9BE3B5) else Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                                }
                            }
                            Text("$batteryPercent%", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Light)
                            val progress by animateFloatAsState(targetValue = batteryPercent / 100f, label = "battery-progress")
                            Box(modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape).background(Color(0xFF30343C))) {
                                Box(modifier = Modifier.fillMaxWidth(progress).height(9.dp).clip(CircleShape).background(Accent))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("0%", color = Muted, fontSize = 11.sp)
                                Text("100%", color = Muted, fontSize = 11.sp)
                            }
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Text("GLYPH CONNECTION", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(glyphStatus, color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                if (glyphSessionOpen) {
                                    if (charging) "The C-series Glyph progress is being updated from the current battery level." else "Connect a charger to show live progress on the rear Glyph."
                                } else {
                                    "Open this app in the foreground. Nothing's SDK restricts third-party Glyph control to foreground apps."
                                },
                                color = Muted, fontSize = 13.sp, lineHeight = 19.sp
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("NO ADS  ·  NO ANALYTICS  ·  EVENT-DRIVEN", color = Color(0xFF656B76), fontSize = 10.sp, letterSpacing = 1.sp)
                        Text("Prototype 0.3.0 · Glyph SDK integration", color = Color(0xFF656B76), fontSize = 11.sp)
                    }
                }
            }
        }
    }

    private fun readBattery(intent: Intent?) {
        val batteryIntent = intent ?: registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        batteryPercent = if (level >= 0) (level * 100 / scale).coerceIn(0, 100) else 0
        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        updateGlyph()
    }

    private fun updateGlyph() {
        val manager = glyphManager ?: return
        if (!glyphRegistered || !glyphSessionOpen) return
        try {
            if (charging) {
                // On Phone (2a), the official SDK maps displayProgress to the C1–C24 arc.
                val frame = manager.glyphFrameBuilder
                    .buildChannelC()
                    .buildPeriod(1000)
                    .buildCycles(1)
                    .build()
                manager.displayProgress(frame, batteryPercent)
            } else {
                manager.turnOff()
            }
        } catch (error: GlyphException) {
            Log.e(TAG, "Unable to update Glyph progress", error)
            glyphStatus = "Glyph update failed: ${error.message ?: "unknown error"}"
        } catch (error: Exception) {
            Log.e(TAG, "Unexpected Glyph update error", error)
            glyphStatus = "Glyph update failed: ${error.message ?: "unknown error"}"
        }
    }

    private fun registerBatteryUpdates() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = readBattery(intent)
        }
        @Suppress("DEPRECATION")
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onDestroy() {
        batteryReceiver?.let { runCatching { unregisterReceiver(it) } }
        batteryReceiver = null
        glyphManager?.let { manager ->
            if (glyphSessionOpen) runCatching { manager.closeSession() }
            runCatching { manager.unInit() }
        }
        glyphSessionOpen = false
        super.onDestroy()
    }
}
