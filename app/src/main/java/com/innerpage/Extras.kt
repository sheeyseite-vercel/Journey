package com.innerpage

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.io.File
import java.time.YearMonth
import java.util.Calendar
import java.util.Locale

fun copyImage(ctx: Context, uri: Uri): String {
    val f = File(ctx.filesDir, "img_${System.currentTimeMillis()}.jpg")
    ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
    return f.absolutePath
}

@Composable fun Photo(path: String, h: Dp = 180.dp) {
    val bmp = remember(path) {
        runCatching { BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap() }.getOrNull()
    }
    if (bmp != null) Image(bmp, null, Modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
}

@Composable fun Player(path: String) {
    var playing by remember { mutableStateOf(false) }
    val mp = remember(path) { MediaPlayer() }
    DisposableEffect(path) { onDispose { mp.release() } }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Yel).clickable {
            if (playing) { runCatching { mp.stop(); mp.reset() }; playing = false }
            else runCatching { mp.reset(); mp.setDataSource(path); mp.prepare(); mp.setOnCompletionListener { playing = false }; mp.start(); playing = true }
        }, Alignment.Center) { Icon(if (playing) Icons.Filled.Close else Icons.Filled.PlayArrow, "Play", tint = Ink, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(10.dp))
        Row(Modifier.weight(1f).height(22.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(6, 12, 16, 9, 14, 7, 12, 8, 14, 6, 10, 5, 12, 7, 15, 9, 11, 6, 13, 8, 10, 5).forEach {
                Box(Modifier.width(3.dp).height(it.dp).background(if (playing) Yel else Color(0xFFD8D4CC)))
            }
        }
    }
}

@Composable fun Pill(t: String, c: Color, click: () -> Unit) =
    Text(t, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (c == Color.White) Ink else Color.White,
        modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(c).clickable { click() }.padding(horizontal = 16.dp, vertical = 9.dp))

@Composable fun Attachments(image: String, audio: String, setImage: (String) -> Unit, setAudio: (String) -> Unit) {
    val ctx = LocalContext.current
    var rec by remember { mutableStateOf<MediaRecorder?>(null) }
    var recPath by remember { mutableStateOf("") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> if (u != null) setImage(copyImage(ctx, u)) }
    fun start() {
        val f = File(ctx.filesDir, "rec_${System.currentTimeMillis()}.m4a")
        @Suppress("DEPRECATION") val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else MediaRecorder()
        runCatching {
            r.setAudioSource(MediaRecorder.AudioSource.MIC); r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); r.setOutputFile(f.absolutePath); r.prepare(); r.start()
            rec = r; recPath = f.absolutePath
        }
    }
    fun stop() { rec?.let { runCatching { it.stop() }; it.release() }; rec = null; setAudio(recPath) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) start() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (image.isNotEmpty()) Photo(image, 110.dp)
        if (audio.isNotEmpty() && rec == null) Player(audio)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(if (rec != null) "Stop recording" else "Voice note", if (rec != null) Color(0xFFD0402F) else Color.White) {
                if (rec != null) stop()
                else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) start()
                else perm.launch(Manifest.permission.RECORD_AUDIO)
            }
            Pill("Photo", Color.White) { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        }
    }
}

@Composable fun MonthCalendar(entries: List<Entry>) {
    var ym by remember { mutableStateOf(YearMonth.now()) }
    val byDay = remember(entries) { entries.groupBy { it.createdAt.day() } }
    val today = java.time.LocalDate.now()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Icon(Icons.Filled.KeyboardArrowLeft, "Previous", Modifier.clickable { ym = ym.minusMonths(1) })
            Text(ym.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) + " " + ym.year, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Icon(Icons.Filled.KeyboardArrowRight, "Next", Modifier.clickable { ym = ym.plusMonths(1) })
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) { "MTWTFSS".forEach { Text("$it", Modifier.weight(1f), fontSize = 11.sp, color = Grey, textAlign = TextAlign.Center) } }
        val offset = ym.atDay(1).dayOfWeek.value - 1
        val rows = (offset + ym.lengthOfMonth() + 6) / 7
        for (r in 0 until rows) Row(Modifier.fillMaxWidth()) {
            for (c in 0..6) {
                val n = r * 7 + c - offset + 1
                val day = if (n in 1..ym.lengthOfMonth()) ym.atDay(n) else null
                val mood = day?.let { byDay[it]?.first()?.mood }
                Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape)
                    .background(if (mood != null) moodColor(mood) else Color.Transparent)
                    .then(if (day == today) Modifier.border(1.dp, Ink, CircleShape) else Modifier), Alignment.Center) {
                    if (day != null) Text("$n", fontSize = 11.sp, color = if (mood == null || mood == "Happy") Ink else Color.White)
                }
            }
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("daily", "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT))
        val pi = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(1, Notification.Builder(c, "daily").setSmallIcon(android.R.drawable.ic_menu_edit).setContentTitle("Journey")
            .setContentText("Take a minute to write today's entry.").setContentIntent(pi).setAutoCancel(true).build())
    }
}

fun setReminder(c: Context, on: Boolean) {
    val am = c.getSystemService(AlarmManager::class.java)
    val pi = PendingIntent.getBroadcast(c, 1, Intent(c, ReminderReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    if (on) {
        val t = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
        if (t.before(Calendar.getInstance())) t.add(Calendar.DAY_OF_YEAR, 1)
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, t.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
    } else am.cancel(pi)
}

val AUTH = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

@Composable fun Gate(a: FragmentActivity) {
    val p = remember { a.getSharedPreferences("ip", Context.MODE_PRIVATE) }
    val canAuth = remember { BiometricManager.from(a).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS }
    var ok by remember { mutableStateOf(!p.getBoolean("lock", false) || !canAuth) }
    fun ask() {
        val info = BiometricPrompt.PromptInfo.Builder().setTitle("Journey").setAllowedAuthenticators(AUTH).build()
        BiometricPrompt(a, ContextCompat.getMainExecutor(a), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) { ok = true }
        }).authenticate(info)
    }
    if (ok) App() else {
        LaunchedEffect(Unit) { ask() }
        Box(Modifier.fillMaxSize().background(Bg), Alignment.Center) {
            Button({ ask() }, colors = ButtonDefaults.buttonColors(Yel, Ink)) { Text("Unlock") }
        }
    }
}

@Composable fun SettingRow(l: String, on: Boolean, set: (Boolean) -> Unit) =
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(l, fontSize = 13.sp)
        Switch(on, set, colors = SwitchDefaults.colors(checkedTrackColor = Yel, checkedThumbColor = Color.White))
    }

@Composable fun SettingsCard(entries: List<Entry>) {
    val ctx = LocalContext.current
    val p = remember { ctx.getSharedPreferences("ip", Context.MODE_PRIVATE) }
    var rem by remember { mutableStateOf(p.getBoolean("rem", false)) }
    var lock by remember { mutableStateOf(p.getBoolean("lock", false)) }
    var msg by remember { mutableStateOf("") }
    val np = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 6.dp)) {
        SettingRow("Daily reminder (8 PM)", rem) {
            rem = it; p.edit().putBoolean("rem", it).apply(); setReminder(ctx, it)
            if (it && Build.VERSION.SDK_INT >= 33) np.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        HorizontalDivider(color = Bg)
        SettingRow("App lock", lock) {
            if (it && BiometricManager.from(ctx).canAuthenticate(AUTH) != BiometricManager.BIOMETRIC_SUCCESS) msg = "Set a screen lock or fingerprint in your phone settings first."
            else { msg = ""; lock = it; p.edit().putBoolean("lock", it).apply() }
        }
        HorizontalDivider(color = Bg)
        Row(Modifier.fillMaxWidth().clickable {
            val text = entries.joinToString("\n\n") { "${it.createdAt.day()} | ${it.title} | ${it.mood} | ${it.tag}\n${it.body}" }
            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Export entries"))
        }.padding(vertical = 14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Export entries", fontSize = 13.sp); Icon(Icons.Filled.Share, "Export", modifier = Modifier.size(18.dp))
        }
    }
    if (msg.isNotEmpty()) Text(msg, fontSize = 11.sp, color = Color(0xFFD0402F), modifier = Modifier.padding(top = 6.dp))
}

@Composable fun Profile(entries: List<Entry>) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ProfileStats(entries)
        Column(Modifier.padding(horizontal = 20.dp)) { SettingsCard(entries) }
    }
}
