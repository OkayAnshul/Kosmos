package com.example.kosmos.features.demo

import com.example.kosmos.features.notifications.SupabaseNotification

/**
 * Static notifications returned when the app runs in offline demo mode.
 * Mirrors the shape of Supabase rows so the Notifications screen renders fully.
 */
object DemoNotifications {

    private fun now(): Long = System.currentTimeMillis()
    private fun hoursAgo(h: Long): Long = now() - h * 60 * 60 * 1000L

    val all: List<SupabaseNotification> = listOf(
        SupabaseNotification(
            id = "90000000-0000-4000-8000-000000000001",
            userId = DemoMode.DEMO_USER_ID,
            title = "New comment on your task",
            body = "Sneha Kulkarni commented on \"Fix crash on API 26 during chat open\".",
            type = "task_comment",
            data = mapOf("task_id" to "b0000000-0000-4000-8000-000000000008"),
            isRead = false,
            createdAt = hoursAgo(1)
        ),
        SupabaseNotification(
            id = "90000000-0000-4000-8000-000000000002",
            userId = DemoMode.DEMO_USER_ID,
            title = "Connection request",
            body = "Meera Krishnan wants to connect with you.",
            type = "connection_request",
            data = mapOf("connection_id" to "d0000000-0000-4000-8000-000000000004"),
            isRead = false,
            createdAt = hoursAgo(3)
        ),
        SupabaseNotification(
            id = "90000000-0000-4000-8000-000000000003",
            userId = DemoMode.DEMO_USER_ID,
            title = "Mention in General",
            body = "Arjun Mehta mentioned you: \"@aravya can you check the release checklist?\"",
            type = "mention",
            data = mapOf("room_id" to "e0000000-0000-4000-8000-000000000001"),
            isRead = false,
            createdAt = hoursAgo(6)
        ),
        SupabaseNotification(
            id = "90000000-0000-4000-8000-000000000004",
            userId = DemoMode.DEMO_USER_ID,
            title = "Task assigned to you",
            body = "Priya Nair assigned you to \"Onboarding flow polish\".",
            type = "task_assigned",
            data = mapOf("task_id" to "b0000000-0000-4000-8000-000000000005"),
            isRead = false,
            createdAt = hoursAgo(24)
        ),
        SupabaseNotification(
            id = "90000000-0000-4000-8000-000000000005",
            userId = DemoMode.DEMO_USER_ID,
            title = "New join request",
            body = "Kavya Singh requested to join Kosmos App.",
            type = "join_request",
            data = mapOf("project_id" to "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
            isRead = true,
            createdAt = hoursAgo(30)
        ),
        SupabaseNotification(
            id = "90000000-0000-4000-8000-000000000006",
            userId = DemoMode.DEMO_USER_ID,
            title = "Sprint retrospective",
            body = "The retro for Sprint 12 is scheduled for Friday 3 PM.",
            type = "announcement",
            data = emptyMap(),
            isRead = true,
            createdAt = hoursAgo(48)
        )
    )
}
