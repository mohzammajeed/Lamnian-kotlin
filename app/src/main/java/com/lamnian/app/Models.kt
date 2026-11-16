package com.lamnian.app

import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

data class Post(
    val id: String, val col: String, val title: String, val body: String, val cat: String,
    val date: String, val phone: String, val image: String, val author: String, val uid: String,
    val photo: String, val created: String, val edited: Boolean, val likeCount: Int
)

data class Comment(
    val id: String, val postId: String, val text: String, val author: String,
    val uid: String, val photo: String, val created: String
)

data class Profile(val uid: String, val name: String, val photo: String, val bio: String, val joined: String)

fun DocumentSnapshot.toPost(col: String) = Post(
    id = id, col = col,
    title = getString("title") ?: "", body = getString("body") ?: "", cat = getString("cat") ?: "",
    date = getString("date") ?: "", phone = getString("phone") ?: "", image = getString("image") ?: "",
    author = getString("author") ?: "Lamnian Community", uid = getString("uid") ?: "",
    photo = getString("photo") ?: "", created = getString("created") ?: "",
    edited = contains("edited"), likeCount = (getLong("likeCount") ?: 0L).toInt()
)

fun DocumentSnapshot.toComment() = Comment(
    id = id, postId = getString("postId") ?: "", text = getString("text") ?: "",
    author = getString("author") ?: "Member", uid = getString("uid") ?: "",
    photo = getString("photo") ?: "", created = getString("created") ?: ""
)

fun DocumentSnapshot.toProfile() = Profile(
    uid = id, name = getString("name") ?: "Member", photo = getString("photo") ?: "",
    bio = getString("bio") ?: "", joined = getString("joined") ?: ""
)

fun nowIso(): String = Instant.now().truncatedTo(ChronoUnit.MILLIS).toString()

fun ago(iso: String): String {
    if (iso.isBlank()) return ""
    return try {
        val t = Instant.parse(iso)
        val m = (System.currentTimeMillis() - t.toEpochMilli()) / 60000
        when {
            m < 1 -> "Just now"
            m < 60 -> "${m}m"
            m < 1440 -> "${m / 60}h"
            m < 10080 -> "${m / 1440}d"
            else -> t.atZone(ZoneOffset.UTC).toLocalDate().toString()
        }
    } catch (e: Exception) {
        iso
    }
}

fun tagOf(col: String) = when (col) {
    "news" -> "News"
    "gallery" -> "Photo"
    "events" -> "Event"
    "notices" -> "Notice"
    "directory" -> "Business"
    else -> ""
}
