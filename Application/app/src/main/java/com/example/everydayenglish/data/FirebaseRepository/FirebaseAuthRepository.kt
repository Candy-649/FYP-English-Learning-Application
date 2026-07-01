package com.example.everydayenglish.data.FirebaseRepository

import com.example.everydayenglish.data.Repository.AuthRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override val currentUserEmail: String?
        get() = auth.currentUser?.email

    override val isAnonymous: Boolean
        get() = auth.currentUser?.isAnonymous ?: false

    override suspend fun continueAsGuest(): Result<String> = runCatching {
        // 已经有 session（匿名或正式）就直接复用，不重复创建匿名账号
        auth.currentUser?.uid?.let { return@runCatching it }
        val result = auth.signInAnonymously().awaitResult()
        result.user?.uid ?: error("Anonymous sign-in succeeded but no user was returned.")
    }

    override suspend fun register(email: String, password: String): Result<String> =
        runCatching {
            val current = auth.currentUser
            val result = if (current != null && current.isAnonymous) {
                // 账号升级：匿名 -> 邮箱密码，uid 不变，本地/云端数据天然延续，不用手动搬
                val credential = EmailAuthProvider.getCredential(email, password)
                current.linkWithCredential(credential).awaitResult()
            } else {
                auth.createUserWithEmailAndPassword(email, password).awaitResult()
            }
            result.user?.uid ?: error("Registration succeeded but no user was returned.")
        }

    override suspend fun login(email: String, password: String): Result<String> =
        runCatching {
            val result = auth.signInWithEmailAndPassword(email, password).awaitResult()
            result.user?.uid ?: error("Login succeeded but no user was returned.")
        }

    override fun logout() {
        auth.signOut()
    }

    // 不引入 kotlinx-coroutines-play-services 依赖，手写一个最小的 Task -> suspend 适配器
    private suspend fun Task<AuthResult>.awaitResult(): AuthResult =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { result -> cont.resume(result) }
            addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
