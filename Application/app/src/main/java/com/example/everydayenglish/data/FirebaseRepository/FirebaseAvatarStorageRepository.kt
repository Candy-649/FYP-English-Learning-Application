package com.example.everydayenglish.data.FirebaseRepository

import android.net.Uri
import com.example.everydayenglish.data.Repository.AvatarStorageRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class FirebaseAvatarStorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : AvatarStorageRepository {

    override suspend fun uploadAvatar(userId: String, localUri: Uri): Result<Uri> =
        upload(storage.reference.child("avatars/$userId.jpg"), localUri)

    override suspend fun uploadBackground(userId: String, localUri: Uri): Result<Uri> =
        upload(storage.reference.child("backgrounds/$userId.jpg"), localUri)

    // Fixed path per user so re-uploads overwrite instead of piling up orphaned files.
    // Firebase Storage issues a new download token on each overwrite, so the returned
    // URL string changes automatically - no manual cache-busting needed for Coil.
    private suspend fun upload(ref: StorageReference, localUri: Uri): Result<Uri> = runCatching {
        ref.putFile(localUri).awaitResult()
        ref.downloadUrl.awaitResult()
    }

    // Same hand-rolled Task -> suspend adapter as FirebaseAuthRepository, to avoid
    // pulling in the kotlinx-coroutines-play-services dependency just for this.
    private suspend fun <T> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { result -> cont.resume(result) }
            addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
