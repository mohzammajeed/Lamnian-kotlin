package com.lamnian.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    private val vm: AppVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LamnianApp(vm, this) }
    }
}

@Composable
fun LamnianApp(vm: AppVM, activity: ComponentActivity) {
    val dark = when (vm.themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    LamnianTheme(dark) {
        val ctx = LocalContext.current
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var profileUid by rememberSaveable { mutableStateOf<String?>(null) }

        LaunchedEffect(vm.toast) {
            vm.toast?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show(); vm.toast = null }
        }
        BackHandler(enabled = profileUid != null || tab != 0) {
            if (profileUid != null) profileUid = null else tab = 0
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                    Row(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(painterResource(R.drawable.logo_icon), null, Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(8.dp))
                        Text("Lamnian", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.startCompose("news") }) { Icon(Icons.Filled.Add, "New post") }
                        if (vm.user != null) {
                            IconButton(onClick = { vm.showSettings = true }) { Icon(Icons.Filled.Settings, "Settings") }
                            Avatar(vm.myPhoto(), vm.myName(), 34.dp, Modifier.padding(start = 2.dp))
                        } else {
                            Button(onClick = { vm.showAuth = true }) { Text("Sign in") }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    val items = listOf(
                        "Home" to Icons.Filled.Home, "Weather" to Icons.Filled.WbSunny, "Directory" to Icons.Filled.Phone,
                        "Ask AI" to Icons.Filled.AutoAwesome, "Me" to Icons.Filled.Person
                    )
                    items.forEachIndexed { i, (label, icon) ->
                        NavigationBarItem(
                            selected = profileUid == null && tab == i,
                            onClick = { profileUid = null; tab = i },
                            icon = { Icon(icon, label) }, label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                val open = profileUid
                when {
                    open != null -> ProfileScreen(vm, open, { profileUid = null }, { profileUid = it })
                    tab == 0 -> HomeScreen(vm, { profileUid = it }, { tab = 1 })
                    tab == 1 -> WeatherScreen(vm)
                    tab == 2 -> DirectoryScreen(vm)
                    tab == 3 -> AiScreen()
                    else -> ProfileScreen(vm, vm.user?.uid ?: "", null, { profileUid = it })
                }
            }
        }

        if (vm.showAuth) AuthDialog(vm, activity)
        vm.composeType?.let { ComposeDialog(vm, it) }
        if (vm.showSettings) SettingsDialog(vm, activity)
        vm.editPost?.let { EditDialog(vm, it) }
        vm.reportPost?.let { ReportDialog(vm, it) }
        vm.commentsPost?.let { p -> CommentsDialog(vm, p) { uid -> vm.commentsPost = null; profileUid = uid } }
        vm.viewImage?.let { ImageViewer(it) { vm.viewImage = null } }
    }
}
