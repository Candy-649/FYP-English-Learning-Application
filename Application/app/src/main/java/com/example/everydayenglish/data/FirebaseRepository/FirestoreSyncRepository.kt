package com.example.everydayenglish.data.FirebaseRepository

import com.example.everydayenglish.data.Repository.AttemptRepository
import com.example.everydayenglish.data.Repository.RecordRepository
import com.example.everydayenglish.data.Repository.SyncRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.entity.ExerciseRecord
import com.example.everydayenglish.data.entity.QuestionAttempt
import com.example.everydayenglish.data.entity.UserProfile
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreSyncRepository(
    private val userProfileRepository: UserProfileRepository,
    private val recordRepository: RecordRepository,
    private val attemptRepository: AttemptRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : SyncRepository {

    private fun userDoc(userId: String) = firestore.collection("users").document(userId)

    override suspend fun pushProfile(profile: UserProfile) {
        runCatching {
            userDoc(profile.userId)
                .collection("profile").document(profile.userId)
                .set(profile.toFirestoreMap())
                .awaitTask()
        }
        // 失败就算了：Firestore SDK 自带离线持久化队列，断网时这次 set() 会本地排队，
        // 一旦恢复网络自动重发，不用照搬 evaluationPending 那套手动重试。
    }

    override suspend fun pushRecord(record: ExerciseRecord) {
        runCatching {
            userDoc(record.userId)
                .collection("records").document(record.recordId)
                .set(record.toFirestoreMap())
                .awaitTask()
        }
    }

    override suspend fun pushAttempt(attempt: QuestionAttempt) {
        runCatching {
            userDoc(attempt.userId)
                .collection("attempts").document(attempt.id)
                .set(attempt.toFirestoreMap())
                .awaitTask()
        }
    }

    override suspend fun pullAndMerge(userId: String) {
        mergeProfile(userId)
        mergeRecords(userId)
        mergeAttempts(userId)
    }

    private suspend fun mergeProfile(userId: String) {
        val cloud = runCatching {
            userDoc(userId).collection("profile").document(userId).get().awaitTask()
        }.getOrNull()?.data?.toUserProfile()
        val local = userProfileRepository.getUserProfile(userId)

        when {
            cloud == null && local != null -> pushProfile(local)
            cloud != null && local == null -> userProfileRepository.insertUserProfile(cloud)
            cloud != null && local != null -> when {
                cloud.updatedAt > local.updatedAt -> userProfileRepository.insertUserProfile(cloud)
                local.updatedAt > cloud.updatedAt -> pushProfile(local)
                // 相等：两边一致，不用动
            }
        }
    }

    private suspend fun mergeRecords(userId: String) {
        val cloudById = runCatching {
            userDoc(userId).collection("records").get().awaitTask()
                .documents.mapNotNull { it.data?.toExerciseRecord() }
        }.getOrNull().orEmpty().associateBy { it.recordId }

        val localById = recordRepository.getAllByUser(userId).associateBy { it.recordId }

        for (id in cloudById.keys + localById.keys) {
            val cloud = cloudById[id]
            val local = localById[id]
            when {
                cloud == null && local != null -> pushRecord(local)
                cloud != null && local == null -> recordRepository.insertExerciseRecord(cloud)
                cloud != null && local != null -> when {
                    cloud.updatedAt > local.updatedAt -> recordRepository.insertExerciseRecord(cloud)
                    local.updatedAt > cloud.updatedAt -> pushRecord(local)
                }
            }
        }
    }

    private suspend fun mergeAttempts(userId: String) {
        val cloudById = runCatching {
            userDoc(userId).collection("attempts").get().awaitTask()
                .documents.mapNotNull { it.data?.toQuestionAttempt() }
        }.getOrNull().orEmpty().associateBy { it.id }

        val localById = attemptRepository.getAllByUser(userId).associateBy { it.id }

        for (id in cloudById.keys + localById.keys) {
            val cloud = cloudById[id]
            val local = localById[id]
            when {
                cloud == null && local != null -> pushAttempt(local)
                cloud != null && local == null -> attemptRepository.upsert(cloud)
                cloud != null && local != null -> when {
                    cloud.updatedAt > local.updatedAt -> attemptRepository.upsert(cloud)
                    local.updatedAt > cloud.updatedAt -> pushAttempt(local)
                }
            }
        }
    }
}
