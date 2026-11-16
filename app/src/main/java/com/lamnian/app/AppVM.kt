package com.lamnian.app

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AppVM(app: Application) : AndroidViewModel(app) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val prefs = app.getSharedPreferences("lamnian", Context.MODE_PRIVATE)

    // ---- state ----
    var user by mutableStateOf(auth.currentUser)
    var profile by mutableStateOf<Profile?>(null)
    val posts = mutableStateMapOf<String, List<Post>>()
    var loaded by mutableStateOf(false)
    val myLikes = mutableStateMapOf<String, Boolean>()
    var saved: Set<String> by mutableStateOf(prefs.getStringSet("saved", emptySet())?.toSet() ?: emptySet())
    var themeMode by mutableIntStateOf(prefs.getInt("theme", 0))
    var toast by mutableStateOf<String?>(null)

    // dialogs
    var showAuth by mutableStateOf(false)
    var composeType by mutableStateOf<String?>(null)
    var showSettings by mutableStateOf(false)
    var editPost by mutableStateOf<Post?>(null)
    var reportPost by mutableStateOf<Post?>(null)
    var commentsPost by mutableStateOf<Post?>(null)
    var viewImage by mutableStateOf<String?>(null)
    private var pendingCompose: String? = null

    // weather
    var weather by mutableStateOf<Weather?>(null)
    var air by mutableStateOf<Air?>(null)
    var prayer by mutableStateOf<Prayer?>(null)
    var wxLoading by mutableStateOf(false)

    private var likesReg: ListenerRegistration? = null

    init {
        Config.COLS.forEach { col ->
            db.collection(col).orderBy("created", Query.Direction.DESCENDING).limit(60)
                .addSnapshotListener { s, _ ->
                    if (s != null) posts[col] = s.documents.map { it.toPost(col) }
                    loaded = true
                }
        }
        auth.addAuthStateListener { a ->
            user = a.currentUser
            onUser(a.currentUser)
        }
        refreshWeather()
    }

    // ---- helpers ----
    fun nameOf(u: FirebaseUser): String =
        u.displayName?.takeIf { it.isNotBlank() } ?: (u.email ?: "Member").substringBefore("@")

    fun myName(): String = profile?.name ?: user?.let { nameOf(it) } ?: "Member"
    fun myPhoto(): String = profile?.photo ?: user?.photoUrl?.toString() ?: ""
    fun isAdmin(): Boolean = user?.email.equals(Config.ADMIN_EMAIL, ignoreCase = true)

    fun startCompose(type: String) {
        if (user == null) { pendingCompose = type; showAuth = true; toast = "Sign in to post" } else composeType = type
    }

    fun needSignIn(): Boolean {
        if (user == null) { showAuth = true; toast = "Sign in first"; return true }
        return false
    }

    private fun onUser(u: FirebaseUser?) {
        likesReg?.remove()
        likesReg = null
        myLikes.clear()
        profile = null
        if (u == null) return
        likesReg = db.collection("likes").whereEqualTo("uid", u.uid).addSnapshotListener { s, _ ->
            myLikes.clear()
            s?.documents?.forEach { d -> d.getString("postId")?.let { myLikes[it] = true } }
        }
        viewModelScope.launch { try { ensureProfile(u) } catch (e: Exception) { } }
        pendingCompose?.let { composeType = it; pendingCompose = null }
    }

    private suspend fun ensureProfile(u: FirebaseUser) {
        val ref = db.collection("users").document(u.uid)
        val d = ref.get().await()
        if (d.exists()) {
            profile = d.toProfile()
        } else {
            val m = mapOf("name" to nameOf(u), "photo" to (u.photoUrl?.toString() ?: ""), "bio" to "", "joined" to nowIso())
            ref.set(m).await()
            profile = Profile(u.uid, m["name"]!!, m["photo"]!!, "", m["joined"]!!)
        }
    }

    suspend fun userDoc(uid: String): Profile? {
        val d = db.collection("users").document(uid).get().await()
        return if (d.exists()) d.toProfile() else null
    }

    // ---- auth ----
    suspend fun signIn(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email.trim(), pass).await()
    }

    suspend fun signUp(name: String, email: String, pass: String) {
        val r = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
        val u = r.user ?: return
        u.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        u.reload().await()
        user = auth.currentUser
        try {
            ensureProfile(u)
            db.collection("users").document(u.uid).update("name", name.trim()).await()
            profile = profile?.copy(name = name.trim())
        } catch (e: Exception) { }
    }

    suspend fun googleCredential(activity: Activity): AuthCredential {
        val id = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        if (id == 0) throw IllegalStateException("Google sign-in is not set up yet (add the SHA-1 in Firebase and download google-services.json again).")
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(activity.getString(id))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = CredentialManager.create(activity).getCredential(activity, request)
        val cred = result.credential
        if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val g = GoogleIdTokenCredential.createFrom(cred.data)
            return GoogleAuthProvider.getCredential(g.idToken, null)
        }
        throw IllegalStateException("Google sign-in did not return an account.")
    }

    suspend fun signInGoogle(activity: Activity) {
        auth.signInWithCredential(googleCredential(activity)).await()
    }

    suspend fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun saveProfile(name: String, bio: String) {
        val u = user ?: return
        u.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        db.collection("users").document(u.uid).update(mapOf("name" to name.trim(), "bio" to bio.trim().take(160))).await()
        profile = (profile ?: Profile(u.uid, name, myPhoto(), bio, nowIso())).copy(name = name.trim(), bio = bio.trim().take(160))
    }

    // ---- posts ----
    suspend fun createPost(col: String, title: String, body: String, cat: String, extra: String, image: String?) {
        val u = user ?: return
        val m = hashMapOf<String, Any>(
            "title" to title.trim().take(120), "uid" to u.uid, "author" to myName(), "created" to nowIso()
        )
        if (body.isNotBlank()) m["body"] = body.trim().take(800)
        if ((col == "notices" || col == "directory")) m["cat"] = cat.trim().ifBlank { "General" }.take(40)
        if (col == "events" && extra.isNotBlank()) m["date"] = extra.trim().take(40)
        if (col == "directory" && extra.isNotBlank()) m["phone"] = extra.trim().take(40)
        if (!image.isNullOrBlank()) m["image"] = image
        myPhoto().takeIf { it.startsWith("http") }?.let { m["photo"] = it }
        db.collection(col).add(m).await()
    }

    suspend fun editPost(p: Post, title: String, body: String) {
        db.collection(p.col).document(p.id)
            .update(mapOf("title" to title.trim().take(120), "body" to body.trim().take(800), "edited" to nowIso())).await()
    }

    suspend fun deletePost(p: Post) {
        db.collection(p.col).document(p.id).delete().await()
    }

    fun toggleLike(p: Post) {
        val u = user
        if (u == null) { showAuth = true; toast = "Sign in to like posts"; return }
        val was = myLikes[p.id] == true
        viewModelScope.launch {
            try {
                val batch = db.batch()
                val postRef = db.collection(p.col).document(p.id)
                val likeRef = db.collection("likes").document("${p.id}_${u.uid}")
                batch.update(postRef, "likeCount", FieldValue.increment(if (was) -1L else 1L))
                if (was) batch.delete(likeRef)
                else batch.set(likeRef, mapOf("postId" to p.id, "uid" to u.uid, "col" to p.col))
                batch.commit().await()
            } catch (e: Exception) {
                toast = "Could not update like"
            }
        }
    }

    suspend fun report(p: Post, reason: String) {
        val u = user ?: return
        db.collection("reports").document("${p.id}_${u.uid}").set(
            mapOf(
                "col" to p.col, "postId" to p.id, "title" to p.title.take(120),
                "uid" to u.uid, "reason" to reason, "created" to nowIso()
            )
        ).await()
    }

    fun toggleSaved(id: String) {
        val adding = !saved.contains(id)
        saved = if (adding) saved + id else saved - id
        prefs.edit().putStringSet("saved", saved.toMutableSet()).apply()
        toast = if (adding) "Saved to your profile" else "Removed from saved"
    }

    // ---- comments ----
    suspend fun addComment(postId: String, text: String) {
        val u = user ?: return
        val m = hashMapOf<String, Any>(
            "postId" to postId, "text" to text.trim().take(500), "uid" to u.uid,
            "author" to myName(), "created" to nowIso()
        )
        myPhoto().takeIf { it.startsWith("http") }?.let { m["photo"] = it }
        db.collection("comments").add(m).await()
    }

    suspend fun deleteComment(id: String) {
        db.collection("comments").document(id).delete().await()
    }

    // ---- account / data ----
    private suspend fun purge(uid: String) {
        for (c in Config.COLS + "comments") {
            val docs = db.collection(c).whereEqualTo("uid", uid).get().await().documents
            docs.chunked(400).forEach { chunk ->
                val b = db.batch()
                chunk.forEach { b.delete(it.reference) }
                b.commit().await()
            }
        }
    }

    suspend fun deleteMyPosts() {
        val u = user ?: return
        purge(u.uid)
    }

    suspend fun deleteAccount(activity: Activity, password: String) {
        val u = auth.currentUser ?: return
        val google = u.providerData.any { it.providerId == "google.com" }
        val cred = if (google) googleCredential(activity) else EmailAuthProvider.getCredential(u.email ?: "", password)
        u.reauthenticate(cred).await()
        purge(u.uid)
        try { db.collection("users").document(u.uid).delete().await() } catch (e: Exception) { }
        u.delete().await()
    }

    fun setTheme(m: Int) {
        themeMode = m
        prefs.edit().putInt("theme", m).apply()
    }

    // ---- weather ----
    fun refreshWeather() {
        viewModelScope.launch {
            wxLoading = true
            val f = fetchCached("wx_f") { WeatherApi.forecastRaw() }
            weather = f?.let { runCatching { WeatherApi.parseWeather(it) }.getOrNull() } ?: weather
            val a = fetchCached("wx_a") { WeatherApi.airRaw() }
            air = a?.let { runCatching { WeatherApi.parseAir(it) }.getOrNull() } ?: air
            val p = fetchCached("wx_p") { WeatherApi.prayerRaw() }
            prayer = p?.let { runCatching { WeatherApi.parsePrayer(it) }.getOrNull() } ?: prayer
            wxLoading = false
        }
    }

    private suspend fun fetchCached(key: String, load: () -> String): String? {
        val fresh = withContext(Dispatchers.IO) { runCatching(load).getOrNull() }
        if (fresh != null) prefs.edit().putString(key, fresh).apply()
        return fresh ?: prefs.getString(key, null)
    }
}
