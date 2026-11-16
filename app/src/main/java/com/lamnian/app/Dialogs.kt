package com.lamnian.app

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Red = Color(0xFFC0392B)

private fun friendly(e: Throwable): String {
    if (e is FirebaseAuthException) {
        return when (e.errorCode) {
            "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD" -> "Wrong email or password."
            "ERROR_USER_NOT_FOUND" -> "No account with this email."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "This email already has an account. Try signing in."
            "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
            "ERROR_INVALID_EMAIL" -> "Enter a valid email."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many tries. Please wait and try again."
            "ERROR_REQUIRES_RECENT_LOGIN" -> "Please sign in again and retry."
            else -> e.message ?: "Something went wrong."
        }
    }
    val m = e.message ?: ""
    return when {
        m.contains("network", true) -> "No internet connection."
        m.contains("cancel", true) -> ""
        m.isBlank() -> "Something went wrong."
        else -> m
    }
}

// ============================== SIGN IN / CREATE ACCOUNT ==============================
@Composable
fun AuthDialog(vm: AppVM, activity: Activity) {
    var signUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun launchAuth(block: suspend () -> Unit) {
        scope.launch {
            busy = true; error = ""
            try { block(); vm.showAuth = false } catch (e: Exception) { error = friendly(e) } finally { busy = false }
        }
    }

    Sheet(if (signUp) "Create account" else "Sign in", { vm.showAuth = false }) {
        OutlinedButton(onClick = { launchAuth { vm.signInGoogle(activity) } }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("G", fontWeight = FontWeight.Bold, color = Color(0xFF4285F4), fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text("Continue with Google")
        }
        Text("or use email", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        if (signUp) OutlinedTextField(name, { name = it }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            pass, { pass = it }, label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
        )
        if (error.isNotBlank()) Text(error, color = Red, fontSize = 13.sp)
        Button(
            onClick = {
                if (email.isBlank() || pass.length < 6 || (signUp && name.isBlank())) {
                    error = if (signUp && name.isBlank()) "Enter your name." else "Enter your email and a password of at least 6 characters."
                } else launchAuth { if (signUp) vm.signUp(name, email, pass) else vm.signIn(email, pass) }
            },
            enabled = !busy, modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "Please wait…" else if (signUp) "Create account" else "Sign in") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { signUp = !signUp; error = "" }) { Text(if (signUp) "I already have an account" else "Create new account") }
            if (!signUp) TextButton(onClick = {
                if (email.isBlank()) error = "Type your email above first."
                else scope.launch {
                    try { vm.resetPassword(email); vm.toast = "Password reset email sent" } catch (e: Exception) { error = friendly(e) }
                }
            }) { Text("Forgot password?") }
        }
    }
}

// ============================== NEW POST ==============================
@Composable
fun ComposeDialog(vm: AppVM, initial: String) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf(initial) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val r = withContext(Dispatchers.Default) { shrinkImage(ctx, uri) }
            if (r == null) vm.toast = "Could not read that photo" else image = r
        }
    }
    val types = listOf("news" to "News", "gallery" to "Photo", "events" to "Event", "notices" to "Notice", "directory" to "Business")

    Sheet("New post", { vm.composeType = null }) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(types) { (k, l) -> FilterChip(selected = type == k, onClick = { type = k }, label = { Text(l) }) }
        }
        OutlinedTextField(title, { if (it.length <= 120) title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(body, { if (it.length <= 800) body = it }, label = { Text("Details") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        if (type == "notices" || type == "directory")
            OutlinedTextField(cat, { cat = it }, label = { Text(if (type == "notices") "Category (Jobs, Lost & found, For sale)" else "Category (Shops, Clinics, Transport)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (type == "events") OutlinedTextField(extra, { extra = it }, label = { Text("Event date (for example 2026-12-05)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (type == "directory") OutlinedTextField(extra, { extra = it }, label = { Text("Phone number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (type == "news" || type == "gallery" || type == "notices") {
            OutlinedButton(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Image, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (image == null) (if (type == "gallery") "Choose a photo (required)" else "Add a photo") else "Change photo")
            }
            image?.let {
                Box(Modifier.fillMaxWidth()) {
                    DataImage(it, Modifier.fillMaxWidth().heightIn(max = 220.dp), ContentScale.Fit)
                    IconButton(onClick = { image = null }, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Filled.Close, "Remove photo", tint = Color.White) }
                }
            }
        }
        Button(
            onClick = {
                if (title.isBlank()) vm.toast = "Add a title"
                else if (type == "gallery" && image == null) vm.toast = "Choose a photo"
                else scope.launch {
                    busy = true
                    try {
                        vm.createPost(type, title, body, cat, extra, if (type == "events" || type == "directory") null else image)
                        vm.toast = "Posted!"
                        vm.composeType = null
                    } catch (e: Exception) { vm.toast = "Could not post. Please try again." } finally { busy = false }
                }
            },
            enabled = !busy, modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "Posting…" else "Post") }
    }
}

// ============================== EDIT ==============================
@Composable
fun EditDialog(vm: AppVM, p: Post) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(p.title) }
    var body by remember { mutableStateOf(p.body) }
    Sheet("Edit post", { vm.editPost = null }) {
        OutlinedTextField(title, { if (it.length <= 120) title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(body, { if (it.length <= 800) body = it }, label = { Text("Details") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                if (title.isBlank()) vm.toast = "Add a title"
                else scope.launch {
                    try { vm.editPost(p, title, body); vm.toast = "Post updated"; vm.editPost = null }
                    catch (e: Exception) { vm.toast = "Could not save changes" }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }
    }
}

// ============================== REPORT ==============================
@Composable
fun ReportDialog(vm: AppVM, p: Post) {
    val scope = rememberCoroutineScope()
    val reasons = listOf("Spam", "Scam or fraud", "Inappropriate content", "Harassment or hate", "False information", "Something else")
    var reason by remember { mutableStateOf(reasons[0]) }
    Sheet("Report post", { vm.reportPost = null }) {
        Text(p.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        reasons.forEach { r ->
            Row(Modifier.fillMaxWidth().clickable { reason = r }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = reason == r, onClick = { reason = r })
                Text(r)
            }
        }
        Button(
            onClick = {
                scope.launch {
                    try { vm.report(p, reason); vm.toast = "Thanks — we will review this post" }
                    catch (e: Exception) { vm.toast = "You already reported this post" }
                    vm.reportPost = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Report") }
    }
}

// ============================== COMMENTS ==============================
@Composable
fun CommentsDialog(vm: AppVM, p: Post, onProfile: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    DisposableEffect(p.id) {
        val reg = vm.db.collection("comments").whereEqualTo("postId", p.id).addSnapshotListener { s, _ ->
            list = s?.documents?.map { it.toComment() }?.sortedBy { it.created } ?: emptyList()
        }
        onDispose { reg.remove() }
    }
    Sheet("Comments", { vm.commentsPost = null }) {
        Text(p.title, fontWeight = FontWeight.SemiBold)
        HorizontalDivider()
        if (list.isEmpty()) Text("No comments yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        list.forEach { c ->
            Row(Modifier.fillMaxWidth()) {
                Avatar(c.photo, c.author, 34.dp, Modifier.clickable(enabled = c.uid.isNotBlank()) { onProfile(c.uid) })
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(10.dp, 6.dp)) {
                    Text(c.author, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(c.text)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(ago(c.created), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (vm.user?.uid == c.uid || vm.isAdmin()) TextButton(onClick = {
                            scope.launch { try { vm.deleteComment(c.id) } catch (e: Exception) { vm.toast = "Could not delete comment" } }
                        }) { Text("Delete", color = Red, fontSize = 12.sp) }
                    }
                }
            }
        }
        if (vm.user == null) {
            Button(onClick = { vm.commentsPost = null; vm.showAuth = true }, modifier = Modifier.fillMaxWidth()) { Text("Sign in to comment") }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    text, { if (it.length <= 500) text = it }, placeholder = { Text("Write a comment") },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(50)
                )
                IconButton(onClick = {
                    val t = text.trim()
                    if (t.isNotEmpty()) scope.launch {
                        try { vm.addComment(p.id, t); text = "" } catch (e: Exception) { vm.toast = "Could not post comment" }
                    }
                }) { Icon(Icons.Filled.Send, "Send", tint = Blue) }
            }
        }
    }
}

// ============================== SETTINGS (the gear) ==============================
@Composable
fun SettingsDialog(vm: AppVM, activity: Activity) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(vm.myName()) }
    var bio by remember { mutableStateOf(vm.profile?.bio ?: "") }
    var confirmPosts by remember { mutableStateOf(false) }
    var confirmAccount by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val u = vm.user
    val usesPassword = u?.providerData?.any { it.providerId == "password" } == true
    val usesGoogle = u?.providerData?.any { it.providerId == "google.com" } == true
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Sheet("Settings", { vm.showSettings = false }) {
        Text("PROFILE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = muted)
        OutlinedTextField(name, { if (it.length <= 40) name = it }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(bio, { if (it.length <= 160) bio = it }, label = { Text("Bio") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                if (name.isBlank()) vm.toast = "Enter a name"
                else scope.launch {
                    try { vm.saveProfile(name, bio); vm.toast = "Profile saved" } catch (e: Exception) { vm.toast = "Could not save profile" }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save profile") }

        Text("APPEARANCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = vm.themeMode == 0, onClick = { vm.setTheme(0) }, label = { Text("System") })
            FilterChip(selected = vm.themeMode == 1, onClick = { vm.setTheme(1) }, label = { Text("Light") }, leadingIcon = { Icon(Icons.Filled.LightMode, null, modifier = Modifier.size(16.dp)) })
            FilterChip(selected = vm.themeMode == 2, onClick = { vm.setTheme(2) }, label = { Text("Dark") }, leadingIcon = { Icon(Icons.Filled.DarkMode, null, modifier = Modifier.size(16.dp)) })
        }

        Text("ACCOUNT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = muted)
        Text(u?.email ?: "", color = muted)
        if (usesPassword) OutlinedButton(
            onClick = {
                scope.launch {
                    try { vm.resetPassword(u?.email ?: ""); vm.toast = "Password reset email sent" } catch (e: Exception) { vm.toast = "Could not send email" }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Icon(Icons.Filled.Lock, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Change password (email link)") }
        if (vm.isAdmin() && Config.API_BASE.isNotBlank()) OutlinedButton(
            onClick = { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(Config.API_BASE + "/admin"))) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Open admin panel") }
        OutlinedButton(onClick = { vm.signOut(); vm.showSettings = false; vm.toast = "Signed out" }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }

        Text("DANGER ZONE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Red)
        OutlinedButton(onClick = { confirmPosts = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete all my posts", color = Red) }
        OutlinedButton(onClick = { confirmAccount = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete my account", color = Red) }
        Text("Lamnian · Weather by Open-Meteo · Prayer times by Aladhan", fontSize = 11.sp, color = muted)
    }

    if (confirmPosts) AlertDialog(
        onDismissRequest = { confirmPosts = false },
        title = { Text("Delete all your posts?") }, text = { Text("Your posts and comments will be removed. This cannot be undone.") },
        confirmButton = {
            TextButton(onClick = {
                confirmPosts = false
                scope.launch {
                    try { vm.deleteMyPosts(); vm.toast = "Your posts were deleted" } catch (e: Exception) { vm.toast = "Could not delete posts" }
                }
            }) { Text("Delete", color = Red) }
        },
        dismissButton = { TextButton(onClick = { confirmPosts = false }) { Text("Cancel") } }
    )

    if (confirmAccount) AlertDialog(
        onDismissRequest = { if (!busy) confirmAccount = false },
        title = { Text("Delete your account?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This removes your login, profile, posts and comments. This cannot be undone.")
                if (!usesGoogle) OutlinedTextField(
                    password, { password = it }, label = { Text("Confirm your password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                ) else Text("You will be asked to confirm with Google.")
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                scope.launch {
                    busy = true
                    try {
                        vm.deleteAccount(activity, password)
                        confirmAccount = false; vm.showSettings = false; vm.toast = "Your account was deleted"
                    } catch (e: Exception) {
                        vm.toast = friendly(e).ifBlank { "Could not delete the account" }
                    } finally { busy = false }
                }
            }) { Text(if (busy) "Deleting…" else "Delete", color = Red) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = { confirmAccount = false }) { Text("Cancel") } }
    )
}

// ============================== FULL SCREEN PHOTO ==============================
@Composable
fun ImageViewer(data: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black).clickable { onClose() }, contentAlignment = Alignment.Center) {
            DataImage(data, Modifier.fillMaxSize(), ContentScale.Fit)
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) { Icon(Icons.Filled.Close, "Close", tint = Color.White) }
        }
    }
}
