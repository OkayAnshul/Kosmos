package com.example.kosmos.features.announcements

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnouncementRepository @Inject constructor(
    private val supabase: SupabaseClient
) {

    /**
     * Returns the newest active announcement the user hasn't seen yet, or null if none.
     */
    suspend fun fetchUnseenAnnouncement(userId: String): Announcement? =
        withContext(Dispatchers.IO) {
            try {
                // Fetch all active (non-expired) announcements — RLS filters is_active + expires_at
                val all = supabase.from("announcements")
                    .select()
                    .decodeList<Announcement>()

                if (all.isEmpty()) return@withContext null

                // Fetch which ones this user has already seen
                val seen = supabase.from("announcement_seen")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<AnnouncementSeen>()

                val seenIds = seen.map { it.announcementId }.toSet()

                // Return newest unseen
                all.filter { it.id !in seenIds }
                    .maxByOrNull { it.createdAt }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }

    /** Marks an announcement as seen for this user (idempotent on conflict). */
    suspend fun markSeen(announcementId: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabase.from("announcement_seen")
                    .upsert(AnnouncementSeen(announcementId = announcementId, userId = userId))
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
