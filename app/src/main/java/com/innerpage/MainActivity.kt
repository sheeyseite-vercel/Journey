package com.innerpage

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.*
import java.util.Locale

val Bg = Color(0xFFF0EEEB); val Yel = Color(0xFFFDB927); val YelCard = Color(0xFFFFCB4F)
val Ink = Color(0xFF1A1A1A); val Grey = Color(0xFF8A8A8A)
val Moods = listOf("Happy" to Color(0xFFFDB927), "Sad" to Color(0xFF7A463C), "Calm" to Color(0xFF7C9A2D), "Anxious" to Color(0xFF7D7761))
val Prompts = listOf(
    Triple("Pause & reflect", "What are you grateful for today?", Color(0xFFF5C6B8)),
    Triple("Set Intentions", "How do you want to feel?", Color(0xFFD9CFF5)),
    Triple("Morning pages", "Write freely for five minutes.", YelCard),
    Triple("Evening review", "What went well today?", Color(0xFFCFE0A0)))

fun blank(t: String = "") = Entry(title = t, body = "", mood = "Happy", tag = "Personal", createdAt = System.currentTimeMillis())
fun Long.day(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
fun moodColor(m: String) = Moods.firstOrNull { it.first == m }?.second ?: Grey

class MainActivity : FragmentActivity() {
    override fun onCreate(s: Bundle?) { super.onCreate(s); setContent { MaterialTheme { Gate(this) } } }
}

@Composable fun App() {
    val ctx = LocalContext.current
    val dao = remember { Db.get(ctx).dao() }
    val entries by dao.all().collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("home") }
    var editing by remember { mutableStateOf<Entry?>(null) }
    var viewing by remember { mutableStateOf<Entry?>(null) }
    var emotions by remember { mutableStateOf(false) }
    BackHandler(editing != null || viewing != null || emotions) {
        when { editing != null -> editing = null; viewing != null -> viewing = null; else -> emotions = false }
    }
    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            Box(Modifier.weight(1f)) {
                val e = editing; val v = viewing
                when {
                    e != null -> Editor(e, { editing = null }) { x -> scope.launch { dao.save(x) }; editing = null; viewing = null }
                    v != null -> Detail(v, { viewing = null }, { editing = v }) { scope.launch { dao.delete(v) }; viewing = null }
                    emotions -> Emotions(entries, { emotions = false }) { editing = blank() }
                    tab == "home" -> Home(entries, { emotions = true }, { editing = blank(it) }) { viewing = it }
                    tab == "explore" -> Explore { editing = blank(it) }
                    tab == "journey" -> Journey(entries) { viewing = it }
                    else -> Profile(entries)
                }
            }
            if (editing == null && viewing == null && !emotions) Nav(tab, { tab = it }) { editing = blank() }
        }
    }
}

@Composable fun Nav(tab: String, on: (String) -> Unit, add: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        NavItem("Home", Icons.Filled.Home, tab == "home") { on("home") }
        NavItem("Explore", Icons.Filled.Search, tab == "explore") { on("explore") }
        Box(Modifier.size(42.dp).clip(CircleShape).background(Yel).clickable { add() }, Alignment.Center) { Icon(Icons.Filled.Add, "New", tint = Ink) }
        NavItem("Journey", Icons.Filled.DateRange, tab == "journey") { on("journey") }
        NavItem("Profile", Icons.Filled.Person, tab == "profile") { on("profile") }
    }
}
@Composable fun NavItem(l: String, i: ImageVector, sel: Boolean, c: () -> Unit) {
    Column(Modifier.clickable { c() }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(i, l, tint = if (sel) Ink else Grey); Text(l, fontSize = 11.sp, color = if (sel) Ink else Grey)
    }
}
@Composable fun Circle(i: ImageVector, d: String, c: () -> Unit) =
    Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable { c() }, Alignment.Center) { Icon(i, d, tint = Ink, modifier = Modifier.size(20.dp)) }
@Composable fun Head(t: String) = Text(t, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Ink)

@Composable fun Home(entries: List<Entry>, seeAll: () -> Unit, start: (String) -> Unit, open: (Entry) -> Unit) {
    val today = LocalDate.now(); val mon = today.with(DayOfWeek.MONDAY)
    val hasEntry = entries.map { it.createdAt.day() }.toSet()
    LazyColumn(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Spacer(Modifier.height(8.dp)); Head("Hi, Jose Maria") }
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                for (n in 0..6) {
                    val d = mon.plusDays(n.toLong()); val sel = d == today
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH), fontSize = 11.sp, color = Grey)
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.size(38.dp).clip(CircleShape).background(if (sel) Yel else Color.White), Alignment.Center) {
                            Text("${d.dayOfMonth}", fontSize = 12.sp, fontWeight = if (hasEntry.contains(d)) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("My Journal", fontWeight = FontWeight.Medium); Text("See all", fontSize = 12.sp, modifier = Modifier.clickable { seeAll() }) } }
        item {
            Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(YelCard).clickable { start("Morning Reflection") }) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Let's start your day", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Begin with a mindful morning reflections.", fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                    androidx.compose.foundation.Canvas(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(90.dp)) {
                        drawCircle(Color(0xFFF08A24), 28.dp.toPx(), Offset(size.width / 2, 30.dp.toPx()))
                        drawCircle(Color(0xFF8FAE3A), size.width * 0.75f, Offset(size.width * 0.3f, size.height + size.width * 0.58f))
                        drawCircle(Color(0xFF6F9A2E), size.width * 0.7f, Offset(size.width * 0.85f, size.height + size.width * 0.62f))
                    }
                }
                Box(Modifier.width(56.dp).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(Color(0xFFCFC4B4)).clickable { start("Evening Reflection") }, Alignment.Center) {
                    Text("Evening", fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.wrapContentWidth(unbounded = true).rotate(-90f))
                }
            }
        }
        item { Text("Quick Journal", fontWeight = FontWeight.Medium) }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Prompts.forEach { (t, b, c) -> PromptCard(t, b, c, Modifier.width(150.dp)) { start(t) } }
            }
        }
        if (entries.isNotEmpty()) { item { Text("Recent", fontWeight = FontWeight.Medium) }; items(entries.take(3)) { EntryRow(it) { open(it) } } }
    }
}
@Composable fun PromptCard(t: String, b: String, c: Color, m: Modifier, click: () -> Unit) =
    Column(m.clip(RoundedCornerShape(16.dp)).background(c).clickable { click() }.padding(14.dp).heightIn(min = 100.dp)) {
        Text(t, fontWeight = FontWeight.Medium, fontSize = 13.sp); Spacer(Modifier.height(4.dp)); Text(b, fontSize = 11.sp)
    }
@Composable fun EntryRow(e: Entry, click: () -> Unit) =
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).clickable { click() }.padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(e.title.ifBlank { "Untitled" }, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1)
            Text(e.createdAt.day().toString(), fontSize = 11.sp, color = Grey)
        }
        Box(Modifier.size(10.dp).clip(CircleShape).background(moodColor(e.mood)))
    }

@Composable fun Emotions(entries: List<Entry>, back: () -> Unit, create: () -> Unit) {
    Column(Modifier.padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Circle(Icons.Filled.ArrowBack, "Back", back); Text("My Journal"); Spacer(Modifier.size(40.dp)) }
        Text("${entries.size}", fontSize = 52.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("Celebrate what made you smile today.", fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(14.dp)) {
            Text("Emotions", fontWeight = FontWeight.Medium); Text("Here are four core emotions for your journal", fontSize = 11.sp, color = Grey)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                Moods.forEach { (n, c) ->
                    val pct = if (entries.isEmpty()) 0 else entries.count { it.mood == n } * 100 / entries.size
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)).background(Bg), Alignment.BottomCenter) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight((pct / 100f).coerceAtLeast(0.08f)).clip(RoundedCornerShape(24.dp)).background(c), Alignment.BottomCenter) {
                                Text("$pct%", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (n == "Happy") Ink else Color.White, modifier = Modifier.padding(bottom = 8.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp)); Text(n, fontSize = 11.sp)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(create, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(Yel, Ink)) { Text("Create a New Journal", fontWeight = FontWeight.Medium) }
    }
}

@Composable fun Explore(start: (String) -> Unit) {
    Column(Modifier.padding(20.dp)) {
        Head("Explore"); Spacer(Modifier.height(12.dp))
        Text("Pick a prompt to start writing.", fontSize = 12.sp, color = Grey); Spacer(Modifier.height(12.dp))
        Prompts.chunked(2).forEach { r ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), Arrangement.spacedBy(8.dp)) { r.forEach { (t, b, c) -> PromptCard(t, b, c, Modifier.weight(1f)) { start(t) } } }
        }
    }
}

@Composable fun Journey(entries: List<Entry>, open: (Entry) -> Unit) {
    val days = entries.map { it.createdAt.day() }.toSet(); var streak = 0; var d = LocalDate.now()
    while (days.contains(d)) { streak++; d = d.minusDays(1) }
    LazyColumn(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Head("Journey"); Spacer(Modifier.height(8.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("$streak", "Day streak", YelCard, Modifier.weight(1f)); Stat("${entries.size}", "Entries", Color(0xFFD9CFF5), Modifier.weight(1f))
            }
        }
        item { MonthCalendar(entries) }
        if (entries.isEmpty()) item { Text("No entries yet. Tap + to write your first one.", fontSize = 12.sp, color = Grey) }
        items(entries) { EntryRow(it) { open(it) } }
    }
}
@Composable fun Stat(v: String, l: String, c: Color, m: Modifier) =
    Column(m.clip(RoundedCornerShape(16.dp)).background(c).padding(14.dp)) { Text(v, fontSize = 22.sp, fontWeight = FontWeight.Medium); Text(l, fontSize = 11.sp) }

@Composable fun ProfileStats(entries: List<Entry>) {
    val words = entries.sumOf { it.body.trim().split(Regex("\\s+")).count { w -> w.isNotEmpty() } }
    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(80.dp).clip(CircleShape).background(Color(0xFFE8855A)), Alignment.Center) { Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(44.dp)) }
        Spacer(Modifier.height(8.dp)); Head("Jose Maria"); Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("${entries.size}" to "Entries", "${entries.map { it.createdAt.day() }.toSet().size}" to "Days", "$words" to "Words").forEach { (v, l) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(v, fontWeight = FontWeight.Medium, fontSize = 16.sp); Text(l, fontSize = 11.sp, color = Grey)
                }
            }
        }
    }
}

@Composable fun Detail(e: Entry, back: () -> Unit, edit: () -> Unit, del: () -> Unit) {
    Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
        Circle(Icons.Filled.ArrowBack, "Back", back); Spacer(Modifier.height(8.dp))
        Text(e.createdAt.day().format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)), color = Color(0xFFB5541F), fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(e.title.ifBlank { "Untitled" }, fontSize = 24.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), Arrangement.Center) { Chip(e.tag); Spacer(Modifier.width(6.dp)); Chip(e.mood) }
        if (e.image.isNotEmpty()) { Photo(e.image); Spacer(Modifier.height(10.dp)) }
        if (e.audio.isNotEmpty()) { Player(e.audio); Spacer(Modifier.height(10.dp)) }
        Text(e.body, fontSize = 14.sp, lineHeight = 21.sp); Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) { Circle(Icons.Filled.Edit, "Edit", edit); Spacer(Modifier.width(12.dp)); Circle(Icons.Filled.Delete, "Delete", del) }
    }
}
@Composable fun Chip(t: String) = Text(t, fontSize = 11.sp, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 4.dp))

@Composable fun Editor(e: Entry, close: () -> Unit, save: (Entry) -> Unit) {
    var title by remember { mutableStateOf(e.title) }; var body by remember { mutableStateOf(e.body) }
    var mood by remember { mutableStateOf(e.mood) }; var tag by remember { mutableStateOf(e.tag) }
    var image by remember { mutableStateOf(e.image) }; var audio by remember { mutableStateOf(e.audio) }
    val tf = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
    Column(Modifier.padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Circle(Icons.Filled.Close, "Close", close); Text("New Journal")
            Button({ save(e.copy(title = title.trim(), body = body.trim(), mood = mood, tag = tag, image = image, audio = audio)) }, colors = ButtonDefaults.buttonColors(Yel, Ink)) { Text("Save") }
        }
        Spacer(Modifier.height(10.dp))
        Text(e.createdAt.day().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)), color = Color(0xFFB5541F), fontSize = 12.sp)
        TextField(title, { title = it }, Modifier.fillMaxWidth(), placeholder = { Text("Title", fontSize = 22.sp) }, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium), colors = tf, singleLine = true)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Personal", "Family", "Work", "Gratitude", "Goals").forEach { t -> FilterChip(tag == t, { tag = t }, { Text(t, fontSize = 11.sp) }) }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(12.dp)) {
            Text("How do you feel?", fontWeight = FontWeight.Medium, fontSize = 12.sp); Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                Moods.forEach { (n, c) -> Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (mood == n) c else Bg).clickable { mood = n }.padding(vertical = 9.dp), Alignment.Center) {
                    Text(n, fontSize = 11.sp, color = if (mood == n && n != "Happy") Color.White else Ink) } }
            }
        }
        Spacer(Modifier.height(10.dp))
        TextField(body, { body = it }, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)), placeholder = { Text("Start writing about your day…", fontSize = 13.sp) },
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
        Spacer(Modifier.height(8.dp))
        Attachments(image, audio, { image = it }, { audio = it })
    }
}
