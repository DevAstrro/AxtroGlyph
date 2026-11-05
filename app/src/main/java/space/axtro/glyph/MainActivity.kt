package space.axtro.glyph

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
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

private val Background = Color(0xFF08090B)
private val Panel = Color(0xFF17191E)
private val Muted = Color(0xFF9CA3AF)
private val Accent = Color(0xFFB8D7FF)

class MainActivity : ComponentActivity() {
    private var batteryPercent by mutableIntStateOf(0)
    private var charging by mutableStateOf(false)
    private var batteryReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readBattery(null)
        registerBatteryUpdates()
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
                            Text("Charging progress for Nothing Phone (2a)", color = Muted, fontSize = 14.sp)
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
                            Text("Not connected yet", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Battery monitoring is live. Actual rear LED control still needs the compatible Nothing Glyph SDK and on-device validation.", color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("LIGHTWEIGHT BY DESIGN  ·  NO ANALYTICS  ·  NO ADS", color = Color(0xFF656B76), fontSize = 10.sp, letterSpacing = 1.sp)
                        Text("Prototype 0.2.0", color = Color(0xFF656B76), fontSize = 11.sp)
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
        super.onDestroy()
    }
}
