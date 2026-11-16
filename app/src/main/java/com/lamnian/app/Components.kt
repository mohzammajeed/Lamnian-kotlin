package com.lamnian.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun Avatar(photo: String, name: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(Blue), contentAlignment = Alignment.Center) {
        if (photo.startsWith("http")) {
            AsyncImage(
                model = photo, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        } else {
            Text(
                name.take(1).uppercase().ifBlank { "L" }, color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp
            )
        }
    }
}

/** A centred panel used for every pop-up window. */
@Composable
fun Sheet(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 660.dp).imePadding()
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close") }
                }
                content()
            }
        }
    }
}

fun sharePost(ctx: Context, p: Post) {
    val text = p.title + "\n— shared from Lamnian" + (if (Config.API_BASE.isNotBlank()) "\n" + Config.API_BASE else "")
    val i = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(i, "Share"))
}

@Composable
fun PostCard(p: Post, vm: AppVM, onProfile: (String) -> Unit) {
    val ctx = LocalContext.current
    val liked = vm.myLikes[p.id] == true
    val me = vm.user
    val own = me != null && me.uid == p.uid
    val canDelete = own || vm.isAdmin()
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(Modifier.padding(12.dp, 10.dp, 4.dp, 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(p.photo, p.author, 40.dp, Modifier.clickable(enabled = p.uid.isNotBlank()) { onProfile(p.uid) })
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        p.author, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = p.uid.isNotBlank()) { onProfile(p.uid) }
                    )
                    Text(
                        listOf(ago(p.created), if (p.edited) "Edited" else "", tagOf(p.col))
                            .filter { it.isNotBlank() }.joinToString(" · "),
                        fontSize = 12.sp, color = muted
                    )
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreHoriz, "More options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        val isSaved = vm.saved.contains(p.id)
                        DropdownMenuItem(
                            text = { Text(if (isSaved) "Remove from saved" else "Save post") },
                            leadingIcon = { Icon(if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, null) },
                            onClick = { menu = false; vm.toggleSaved(p.id) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share post") },
                            leadingIcon = { Icon(Icons.Filled.Share, null) },
                            onClick = { menu = false; sharePost(ctx, p) }
                        )
                        if (own) DropdownMenuItem(
                            text = { Text("Edit post") },
                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                            onClick = { menu = false; vm.editPost = p }
                        )
                        if (canDelete) DropdownMenuItem(
                            text = { Text("Delete post", color = Color(0xFFC0392B)) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFC0392B)) },
                            onClick = { menu = false; confirmDelete = true }
                        )
                        if (!own) DropdownMenuItem(
                            text = { Text("Report post", color = Color(0xFFC0392B)) },
                            leadingIcon = { Icon(Icons.Filled.Flag, null, tint = Color(0xFFC0392B)) },
                            onClick = {
                                menu = false
                                if (!vm.needSignIn()) vm.reportPost = p
                            }
                        )
                    }
                }
            }

            Column(Modifier.padding(12.dp, 4.dp, 12.dp, 10.dp)) {
                Text(p.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (p.body.isNotBlank()) Text(p.body, Modifier.padding(top = 4.dp))
                if (p.col == "events" && p.date.isNotBlank())
                    Text("Date: ${p.date}", Modifier.padding(top = 4.dp), color = Blue, fontWeight = FontWeight.SemiBold)
                if (p.col == "notices" && p.cat.isNotBlank() && p.cat != "General")
                    Text("Category: ${p.cat}", Modifier.padding(top = 4.dp), color = Blue, fontSize = 13.sp)
            }

            if (p.image.isNotBlank()) {
                DataImage(
                    p.image,
                    Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 460.dp).clickable { vm.viewImage = p.image },
                    ContentScale.Crop
                )
            }

            if (p.likeCount > 0) {
                Row(Modifier.padding(12.dp, 8.dp, 12.dp, 0.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ThumbUp, null, tint = Blue, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("${p.likeCount}", fontSize = 13.sp, color = muted)
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                TextButton(onClick = { vm.toggleLike(p) }, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, null,
                        tint = if (liked) Blue else muted, modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (liked) "Liked" else "Like", color = if (liked) Blue else muted, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { vm.commentsPost = p }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.ChatBubbleOutline, null, tint = muted, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Comment", color = muted, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { sharePost(ctx, p) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Share, null, tint = muted, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share", color = muted, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this post?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        try { vm.deletePost(p); vm.toast = "Post deleted" }
                        catch (e: Exception) { vm.toast = "Could not delete the post" }
                    }
                }) { Text("Delete", color = Color(0xFFC0392B)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
