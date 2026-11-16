package com.lamnian.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// ============================== HOME ==============================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppVM, onProfile: (String) -> Unit, openWeather: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val base = Config.COLS.filter { it != "directory" }.flatMap { vm.posts[it].orEmpty() }
    val q = query.trim().lowercase()
    val filtered = base
        .filter { filter == "all" || filter == "top" || it.col == filter }
        .filter { q.isEmpty() || (it.title + " " + it.body + " " + it.author).lowercase().contains(q) }
    val shown = if (filter == "top") filtered.sortedByDescending { it.likeCount } else filtered.sortedByDescending { it.created }

    PullToRefreshBox(isRefreshing = vm.wxLoading, onRefresh = { vm.refreshWeather() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp)) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(vm.myPhoto(), if (vm.user != null) vm.myName() else "L", 40.dp)
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).clickable { vm.startCompose("news") }
                            ) {
                                Text(
                                    if (vm.user != null) "What's happening, ${vm.myName().substringBefore(' ')}?" else "What's happening in Lamnian?",
                                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = muted
                                )
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            QuickPost(Icons.AutoMirrored.Filled.Article, "News", Color(0xFF1877F2)) { vm.startCompose("news") }
                            QuickPost(Icons.Filled.Image, "Photo", Color(0xFF2EA44F)) { vm.startCompose("gallery") }
                            QuickPost(Icons.Filled.Event, "Event", Color(0xFFE0592A)) { vm.startCompose("events") }
                            QuickPost(Icons.AutoMirrored.Filled.Assignment, "Notice", Color(0xFF8A5CF6)) { vm.startCompose("notices") }
                        }
                    }
                }
            }
            item { WeatherMini(vm, openWeather) }
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text("Search posts") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
            }
            item {
                LazyRow(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        listOf(
                            Triple("all", "All", null as ImageVector?), Triple("news", "News", Icons.AutoMirrored.Filled.Article),
                            Triple("gallery", "Photos", Icons.Filled.Image), Triple("events", "Events", Icons.Filled.Event),
                            Triple("notices", "Notices", Icons.AutoMirrored.Filled.Assignment), Triple("top", "Top", Icons.Filled.Whatshot)
                        )
                    ) { (key, label, icon) ->
                        if (icon != null) FilterChip(
                            selected = filter == key, onClick = { filter = key }, label = { Text(label) },
                            leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) }
                        ) else FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label) })
                    }
                }
            }
            if (!vm.loaded && shown.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (shown.isEmpty()) {
                item { Text("Nothing here yet — be the first to post!", Modifier.fillMaxWidth().padding(24.dp), color = muted, textAlign = TextAlign.Center) }
            } else {
                items(shown, key = { it.col + it.id }) { p -> PostCard(p, vm, onProfile) }
            }
        }
    }
}

@Composable
private fun QuickPost(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WeatherMini(vm: AppVM, open: () -> Unit) {
    val w = vm.weather
    val colors = if (w != null) skyColors(w.code, w.isDay) else listOf(Color(0xFF3F86D3), Color(0xFF8FC1EE))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(colors))
            .clickable { open() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (w == null) {
            Text("Loading weather…", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        } else {
            val (label, icon, tint) = wxInfo(w.code, w.isDay)
            Icon(icon, null, tint = tint, modifier = Modifier.size(38.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${w.temp.roundToInt()}° · $label", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val d = w.days.firstOrNull()
                Text(
                    "Lamnian" + (if (d != null) " · H ${d.max.roundToInt()}° L ${d.min.roundToInt()}° · ${d.rain}% rain" else ""),
                    color = Color.White.copy(alpha = 0.92f), fontSize = 12.5.sp
                )
            }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = Color.White)
    }
}

// ============================== DIRECTORY ==============================
@Composable
fun DirectoryScreen(vm: AppVM) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("All") }
    val entries = vm.posts["directory"].orEmpty()
    val cats = listOf("All") + entries.map { it.cat.ifBlank { "General" } }.distinct()
    val q = query.trim().lowercase()
    val list = entries.filter { (cat == "All" || it.cat.ifBlank { "General" } == cat) && (q.isEmpty() || (it.title + it.body + it.cat).lowercase().contains(q)) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Useful numbers", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                Button(onClick = { vm.startCompose("directory") }) { Text("Add business") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Rescue" to "1122", "Police" to "15", "Ambulance" to "115", "Fire" to "16").forEach { (n, num) ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFFB4331A))
                            .clickable { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))) }.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(num, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(n, color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("Search shops, clinics, services") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cats) { c -> FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c) }) }
            }
        }
        if (list.isEmpty()) item { Text("No matches yet.", Modifier.fillMaxWidth().padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
        items(list, key = { it.id }) { p -> DirRow(p, vm) }
    }
}

@Composable
private fun DirRow(p: Post, vm: AppVM) {
    val ctx = LocalContext.current
    val me = vm.user
    val own = me != null && me.uid == p.uid
    val icon = when (p.cat.lowercase()) {
        "clinics", "clinic", "hospital" -> Icons.Filled.LocalHospital
        "shops", "shop", "store" -> Icons.Filled.ShoppingCart
        "transport", "taxi" -> Icons.Filled.LocalTaxi
        else -> Icons.Filled.Place
    }
    var confirm by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Blue)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(p.title, fontWeight = FontWeight.Bold)
                Text(
                    listOf(p.cat.ifBlank { "General" }, p.body).filter { it.isNotBlank() }.joinToString(" · "),
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (p.phone.isNotBlank()) {
                Button(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${p.phone}"))) }) {
                    Icon(Icons.Filled.Phone, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Call")
                }
            }
            if (own || vm.isAdmin()) IconButton(onClick = { confirm = true }) { Icon(Icons.Filled.Delete, "Delete", tint = Color(0xFFC0392B)) }
            else IconButton(onClick = { if (!vm.needSignIn()) vm.reportPost = p }) { Icon(Icons.Filled.Flag, "Report", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    if (confirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirm = false }, title = { Text("Delete this entry?") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    scope.launch { try { vm.deletePost(p); vm.toast = "Deleted" } catch (e: Exception) { vm.toast = "Could not delete" } }
                }) { Text("Delete", color = Color(0xFFC0392B)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
        )
    }
}

// ============================== ASK AI ==============================
@Composable
fun AiScreen() {
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Blue)
                    Spacer(Modifier.width(8.dp))
                    Text("Lamnian AI", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
                Text("Ask about the weather, the roads, local events or anything else. Coming soon.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• Weather-aware — knows today's forecast for Lamnian.")
                Text("• Local news and events — can summarise what's happening.")
                Text("• Speaks your language — English, Urdu or Roman Urdu.")
                Text(
                    "Not connected yet", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        OutlinedTextField(
            value = "", onValueChange = {}, enabled = false, placeholder = { Text("Ask Lamnian AI…") },
            shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================== PROFILE ==============================
@Composable
fun ProfileScreen(vm: AppVM, uid: String, onBack: (() -> Unit)?, onProfile: (String) -> Unit) {
    if (uid.isBlank()) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Sign in to see and edit your profile.", textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.showAuth = true }) { Text("Sign in") }
        }
        return
    }
    val me = vm.user?.uid == uid
    val doc by produceState<Profile?>(if (me) vm.profile else null, uid, vm.profile) {
        value = if (me) vm.profile else runCatching { vm.userDoc(uid) }.getOrNull()
    }
    var tab by remember { mutableIntStateOf(0) }
    val all = Config.COLS.flatMap { vm.posts[it].orEmpty() }
    val mine = all.filter { it.uid == uid }.sortedByDescending { it.created }
    val list = if (tab == 0) mine else all.filter { vm.saved.contains(it.id) }.sortedByDescending { it.created }
    val name = doc?.name ?: mine.firstOrNull()?.author ?: if (me) vm.myName() else "Member"
    val photo = doc?.photo?.takeIf { it.isNotBlank() } ?: mine.firstOrNull()?.photo ?: if (me) vm.myPhoto() else ""
    val likes = mine.sumOf { it.likeCount }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp)) {
        item {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.padding(bottom = 10.dp)) {
                Column {
                    Box(
                        Modifier.fillMaxWidth().height(120.dp)
                            .background(Brush.linearGradient(listOf(Color(0xFF1877F2), Color(0xFF6CB2FF), Color(0xFFF0B24A))))
                    ) {
                        if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        if (me) IconButton(onClick = { vm.showSettings = true }, modifier = Modifier.align(Alignment.TopEnd)) {
                            Icon(Icons.Filled.Settings, "Settings", tint = Color.White)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(44.dp)) {
                        Avatar(
                            photo, name, 88.dp,
                            Modifier.align(Alignment.TopCenter).offset(y = (-44).dp)
                        )
                    }
                    Column(Modifier.fillMaxWidth().padding(14.dp, 0.dp, 14.dp, 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        val bio = doc?.bio.orEmpty()
                        if (bio.isNotBlank()) Text(bio, Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
                        else if (me) Text("Add a bio in Settings", Modifier.padding(top = 4.dp), color = muted)
                        val joined = doc?.joined.orEmpty()
                        if (joined.length >= 7) Text("Member since ${joined.take(7)}", fontSize = 12.sp, color = muted, modifier = Modifier.padding(top = 2.dp))
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${mine.size}", fontWeight = FontWeight.Bold, fontSize = 19.sp); Text("Posts", fontSize = 13.sp, color = muted) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$likes", fontWeight = FontWeight.Bold, fontSize = 19.sp); Text("Likes", fontSize = 13.sp, color = muted) }
                        }
                        if (me) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Posts") })
                            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Saved") })
                        }
                    }
                }
            }
        }
        if (list.isEmpty()) item {
            Text(if (tab == 1) "Nothing saved yet. Use the three-dots menu on a post to save it." else "No posts yet.", Modifier.fillMaxWidth().padding(24.dp), color = muted, textAlign = TextAlign.Center)
        }
        items(list, key = { "p" + it.col + it.id }) { p -> PostCard(p, vm, onProfile) }
    }
}

