package com.example.kosmos.features.demo

import com.example.kosmos.core.database.dao.ChatRoomDao
import com.example.kosmos.core.database.dao.MessageDao
import com.example.kosmos.core.database.dao.ProjectDao
import com.example.kosmos.core.database.dao.ProjectInviteDao
import com.example.kosmos.core.database.dao.ProjectJoinRequestDao
import com.example.kosmos.core.database.dao.ProjectMemberDao
import com.example.kosmos.core.database.dao.TaskActivityDao
import com.example.kosmos.core.database.dao.TaskDao
import com.example.kosmos.core.database.dao.TaskDependencyDao
import com.example.kosmos.core.database.dao.TimeEntryDao
import com.example.kosmos.core.database.dao.UserConnectionDao
import com.example.kosmos.core.database.dao.UserDao
import com.example.kosmos.core.models.ActivityActionType
import com.example.kosmos.core.models.ChatRoom
import com.example.kosmos.core.models.ChatRoomType
import com.example.kosmos.core.models.ConnectionStatus
import com.example.kosmos.core.models.DependencyType
import com.example.kosmos.core.models.InviteStatus
import com.example.kosmos.core.models.JoinRequestStatus
import com.example.kosmos.core.models.Message
import com.example.kosmos.core.models.MessageType
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectCategory
import com.example.kosmos.core.models.ProjectInvite
import com.example.kosmos.core.models.ProjectJoinRequest
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.ProjectStatus
import com.example.kosmos.core.models.ProjectVisibility
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskActivity
import com.example.kosmos.core.models.TaskDependency
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.core.models.TimeEntry
import com.example.kosmos.core.models.User
import com.example.kosmos.core.models.UserConnection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the Room database with realistic offline demo data.
 *
 * All entities use fixed UUIDs so referential integrity holds across
 * projects → members → rooms → messages → tasks → activity/entries.
 * Runs once, idempotently (DAOs use INSERT OR REPLACE).
 */
@Singleton
class DemoDataSeeder @Inject constructor(
    private val userDao: UserDao,
    private val projectDao: ProjectDao,
    private val projectMemberDao: ProjectMemberDao,
    private val chatRoomDao: ChatRoomDao,
    private val messageDao: MessageDao,
    private val taskDao: TaskDao,
    private val taskActivityDao: TaskActivityDao,
    private val userConnectionDao: UserConnectionDao,
    private val projectInviteDao: ProjectInviteDao,
    private val projectJoinRequestDao: ProjectJoinRequestDao,
    private val timeEntryDao: TimeEntryDao,
    private val taskDependencyDao: TaskDependencyDao
) {

    companion object {
        val DEMO = DemoMode.DEMO_USER_ID

        // Team members (fixed UUIDs so memberships/tasks/messages resolve)
        val PRIYA = "22222222-2222-4222-8222-222222222222"
        val RAHUL = "33333333-3333-4333-8333-333333333333"
        val SNEHA = "44444444-4444-4444-8444-444444444444"
        val ARJUN = "55555555-5555-4555-8555-555555555555"
        val MEERA = "66666666-6666-4666-8666-666666666666"
        val KAVYA = "77777777-7777-4777-8777-777777777777"

        // Projects
        val KOSMOS = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        val NEBULA = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
        val ORBIT = "cccccccc-cccc-4ccc-8ccc-cccccccccccc"
        val VENTURE = "dddddddd-dddd-4ddd-8ddd-dddddddddddd"

        // Chat rooms
        val KOSMOS_GENERAL = "e0000000-0000-4000-8000-000000000001"
        val KOSMOS_DESIGN = "e0000000-0000-4000-8000-000000000002"
        val KOSMOS_ENGINEERING = "e0000000-0000-4000-8000-000000000003"
        val DM_PRIYA = "e0000000-0000-4000-8000-000000000004"

        private fun now(): Long = System.currentTimeMillis()
        private fun hoursAgo(h: Long): Long = now() - h * 60 * 60 * 1000L
        private fun daysAgo(d: Long): Long = now() - d * 24 * 60 * 60 * 1000L
        private fun daysFromNow(d: Long): Long = now() + d * 24 * 60 * 60 * 1000L
    }

    suspend fun seed() {
        seedUsers()
        seedProjects()
        seedMembers()
        seedChatRooms()
        seedMessages()
        seedTasks()
        seedTaskActivity()
        seedConnections()
        seedInvitesAndRequests()
        seedTimeEntries()
        seedDependencies()
    }

    // ========================================================================
    // USERS
    // ========================================================================
    private suspend fun seedUsers() {
        val users = listOf(
            DemoMode.DEMO_USER,
            User(
                id = PRIYA, email = "priya@kosmos.demo", username = "priyadesigns",
                displayName = "Priya Nair", role = "Product Designer", bio = "Designing pixel-perfect experiences.",
                createdAt = daysAgo(120), isOnline = true
            ),
            User(
                id = RAHUL, email = "rahul@kosmos.demo", username = "rahuldev",
                displayName = "Rahul Verma", role = "Backend Engineer", bio = "Distributed systems & APIs.",
                createdAt = daysAgo(110), isOnline = true
            ),
            User(
                id = SNEHA, email = "sneha@kosmos.demo", username = "snehaqa",
                displayName = "Sneha Kulkarni", role = "QA Engineer", bio = "Breaking things so you don't have to.",
                createdAt = daysAgo(95), isOnline = false
            ),
            User(
                id = ARJUN, email = "arjun@kosmos.demo", username = "arjunm",
                displayName = "Arjun Mehta", role = "Product Manager", bio = "Roadmaps, priorities, shipped features.",
                createdAt = daysAgo(130), isOnline = true
            ),
            User(
                id = MEERA, email = "meera@kosmos.demo", username = "meerak",
                displayName = "Meera Krishnan", role = "Marketing Lead", bio = "Growth & go-to-market.",
                createdAt = daysAgo(80), isOnline = true
            ),
            User(
                id = KAVYA, email = "kavya@kosmos.demo", username = "kavyas",
                displayName = "Kavya Singh", role = "Data Analyst", bio = "Turning data into decisions.",
                createdAt = daysAgo(70), isOnline = false
            )
        )
        userDao.insertUsers(users)
    }

    // ========================================================================
    // PROJECTS (cached counts set manually — no SQL triggers in Room)
    // ========================================================================
    private suspend fun seedProjects() {
        val projects = listOf(
            Project(
                id = KOSMOS, name = "Kosmos App", ownerId = DEMO,
                description = "Offline-first team collaboration platform for Android. Real-time chat, task boards, and project workspaces that work without a connection.",
                status = ProjectStatus.ACTIVE, visibility = ProjectVisibility.PRIVATE,
                category = ProjectCategory.TECH, color = "#6366F1",
                techStack = """["Kotlin","Jetpack Compose","Room","Supabase"]""",
                tags = """["android","offline-first","collaboration"]""",
                githubUrl = "https://github.com/OkayAnshul/Kosmos",
                deadline = daysFromNow(45),
                createdAt = daysAgo(90), updatedAt = hoursAgo(1),
                memberCount = 4, chatCount = 3, taskCount = 9, completedTaskCount = 4, pendingTaskCount = 5,
                lastActivityAt = hoursAgo(1)
            ),
            Project(
                id = NEBULA, name = "Nebula Website", ownerId = PRIYA,
                description = "Complete redesign of the corporate marketing site. New design system, component library, and CMS integration.",
                status = ProjectStatus.ACTIVE, visibility = ProjectVisibility.INTERNAL,
                category = ProjectCategory.TECH, color = "#0EA5E9",
                techStack = """["Figma","Next.js","Tailwind"]""",
                tags = """["web","design-system"]""",
                deadline = daysFromNow(30),
                createdAt = daysAgo(60), updatedAt = hoursAgo(5),
                memberCount = 3, chatCount = 1, taskCount = 5, completedTaskCount = 2, pendingTaskCount = 3,
                lastActivityAt = hoursAgo(5)
            ),
            Project(
                id = ORBIT, name = "Orbit Social Club", ownerId = DEMO,
                description = "Community meetup app connecting developers across the city. Monthly hackathons, study circles, and tech talks.",
                status = ProjectStatus.ACTIVE, visibility = ProjectVisibility.PUBLIC,
                category = ProjectCategory.SOCIAL, color = "#F59E0B",
                projectMotive = "Foster an inclusive local developer community.",
                targetAudience = "Students, engineers, and founders",
                tags = """["community","events"]""",
                deadline = daysFromNow(90),
                createdAt = daysAgo(40), updatedAt = daysAgo(1),
                memberCount = 3, chatCount = 1, taskCount = 4, completedTaskCount = 1, pendingTaskCount = 3,
                lastActivityAt = daysAgo(1)
            ),
            Project(
                id = VENTURE, name = "Venture Pitch Prep", ownerId = ARJUN,
                description = "Investor pitch deck, financial model, and demo preparation for the Series A round.",
                status = ProjectStatus.ON_HOLD, visibility = ProjectVisibility.PRIVATE,
                category = ProjectCategory.BUSINESS, color = "#10B981",
                businessModel = "B2B SaaS subscription with free tier",
                websiteUrl = "https://example.com",
                tags = """["fundraising","saas"]""",
                deadline = daysFromNow(21),
                createdAt = daysAgo(30), updatedAt = daysAgo(4),
                memberCount = 2, chatCount = 1, taskCount = 3, completedTaskCount = 0, pendingTaskCount = 3,
                lastActivityAt = daysAgo(4)
            )
        )
        projectDao.insertProjects(projects)
    }

    // ========================================================================
    // PROJECT MEMBERS
    // ========================================================================
    private suspend fun seedMembers() {
        val members = listOf(
            ProjectMember(id = "a0000000-0000-4000-8000-000000000001", projectId = KOSMOS, userId = DEMO, role = ProjectRole.ADMIN, invitedBy = null, joinedAt = daysAgo(90)),
            ProjectMember(id = "a0000000-0000-4000-8000-000000000002", projectId = KOSMOS, userId = PRIYA, role = ProjectRole.MANAGER, invitedBy = DEMO, joinedAt = daysAgo(85)),
            ProjectMember(id = "a0000000-0000-4000-8000-000000000003", projectId = KOSMOS, userId = RAHUL, role = ProjectRole.MANAGER, invitedBy = DEMO, joinedAt = daysAgo(84)),
            ProjectMember(id = "a0000000-0000-4000-8000-000000000004", projectId = KOSMOS, userId = SNEHA, role = ProjectRole.MEMBER, invitedBy = DEMO, joinedAt = daysAgo(60)),

            ProjectMember(id = "a0000000-0000-4000-8000-000000000005", projectId = NEBULA, userId = PRIYA, role = ProjectRole.ADMIN, invitedBy = null, joinedAt = daysAgo(60)),
            ProjectMember(id = "a0000000-0000-4000-8000-000000000006", projectId = NEBULA, userId = DEMO, role = ProjectRole.MEMBER, invitedBy = PRIYA, joinedAt = daysAgo(55)),
            ProjectMember(id = "a0000000-0000-4000-8000-000000000007", projectId = NEBULA, userId = ARJUN, role = ProjectRole.MANAGER, invitedBy = PRIYA, joinedAt = daysAgo(54)),

            ProjectMember(id = "a0000000-0000-4000-8000-000000000008", projectId = ORBIT, userId = DEMO, role = ProjectRole.ADMIN, invitedBy = null, joinedAt = daysAgo(40)),
            ProjectMember(id = "a0000000-0000-4000-8000-000000000009", projectId = ORBIT, userId = MEERA, role = ProjectRole.MANAGER, invitedBy = DEMO, joinedAt = daysAgo(38)),
            ProjectMember(id = "a0000000-0000-4000-8000-000000000010", projectId = ORBIT, userId = KAVYA, role = ProjectRole.MEMBER, invitedBy = DEMO, joinedAt = daysAgo(35)),

            ProjectMember(id = "a0000000-0000-4000-8000-000000000011", projectId = VENTURE, userId = ARJUN, role = ProjectRole.ADMIN, invitedBy = null, joinedAt = daysAgo(30)),
            ProjectMember(id = "a0000000-0000-4000-8000-000000000012", projectId = VENTURE, userId = DEMO, role = ProjectRole.MEMBER, invitedBy = ARJUN, joinedAt = daysAgo(29))
        )
        projectMemberDao.insertMembers(members)
    }

    // ========================================================================
    // CHAT ROOMS
    // ========================================================================
    private suspend fun seedChatRooms() {
        val rooms = listOf(
            ChatRoom(
                id = KOSMOS_GENERAL, projectId = KOSMOS, name = "General", description = "Team-wide announcements and discussion.",
                type = ChatRoomType.GENERAL, createdBy = DEMO, createdAt = daysAgo(90), updatedAt = hoursAgo(1),
                lastMessage = "Demo build is looking great 🔥", lastMessageTimestamp = hoursAgo(1),
                lastMessageId = "f0000000-0000-4000-8000-000000000004"
            ),
            ChatRoom(
                id = KOSMOS_DESIGN, projectId = KOSMOS, name = "Design", description = "UI/UX decisions and design reviews.",
                type = ChatRoomType.CHANNEL, createdBy = PRIYA, createdAt = daysAgo(80), updatedAt = hoursAgo(3),
                lastMessage = "Pushed the updated task card to Figma", lastMessageTimestamp = hoursAgo(3),
                lastMessageId = "f0000000-0000-4000-8000-000000000009"
            ),
            ChatRoom(
                id = KOSMOS_ENGINEERING, projectId = KOSMOS, name = "Engineering", description = "Code reviews, architecture, and release notes.",
                type = ChatRoomType.CHANNEL, createdBy = RAHUL, createdAt = daysAgo(80), updatedAt = hoursAgo(6),
                lastMessage = "CI is green again ✅", lastMessageTimestamp = hoursAgo(6),
                lastMessageId = "f0000000-0000-4000-8000-000000000014"
            ),
            ChatRoom(
                id = DM_PRIYA, projectId = KOSMOS, name = "Priya Nair", description = "",
                type = ChatRoomType.DIRECT, createdBy = DEMO, createdAt = daysAgo(70), updatedAt = hoursAgo(2),
                lastMessage = "Sounds good, let's ship it this week!", lastMessageTimestamp = hoursAgo(2),
                lastMessageId = "f0000000-0000-4000-8000-000000000018",
                participantIds = listOf(DEMO, PRIYA), isPrivate = true
            )
        )
        chatRoomDao.insertChatRooms(rooms)
    }

    // ========================================================================
    // MESSAGES (readBy variants for unread badges)
    // ========================================================================
    private suspend fun seedMessages() {
        val messages = listOf(
            // General room
            Message(id = "f0000000-0000-4000-8000-000000000001", chatRoomId = KOSMOS_GENERAL, senderId = ARJUN, senderName = "Arjun Mehta",
                content = "Welcome to the Kosmos team! Excited to have everyone on board 🚀", timestamp = daysAgo(6), type = MessageType.SYSTEM,
                readBy = listOf(DEMO, PRIYA, RAHUL, SNEHA)),
            Message(id = "f0000000-0000-4000-8000-000000000002", chatRoomId = KOSMOS_GENERAL, senderId = PRIYA, senderName = "Priya Nair",
                content = "Thanks Arjun! New mockups are in the Design channel.", timestamp = daysAgo(6), type = MessageType.TEXT,
                readBy = listOf(DEMO, RAHUL, SNEHA)),
            Message(id = "f0000000-0000-4000-8000-000000000003", chatRoomId = KOSMOS_GENERAL, senderId = DEMO, senderName = "Aravya Sharma",
                content = "Just merged the Room schema update. Offline sync should be rock solid now.", timestamp = daysAgo(2), type = MessageType.TEXT,
                readBy = listOf(PRIYA, RAHUL, SNEHA, ARJUN)),
            Message(id = "f0000000-0000-4000-8000-000000000004", chatRoomId = KOSMOS_GENERAL, senderId = SNEHA, senderName = "Sneha Kulkarni",
                content = "Demo build is looking great 🔥", timestamp = hoursAgo(1), type = MessageType.TEXT,
                readBy = emptyList()),

            // Design channel
            Message(id = "f0000000-0000-4000-8000-000000000005", chatRoomId = KOSMOS_DESIGN, senderId = PRIYA, senderName = "Priya Nair",
                content = "Here's the updated task board layout with the new color tokens.", timestamp = daysAgo(3), type = MessageType.TEXT,
                readBy = listOf(DEMO, RAHUL)),
            Message(id = "f0000000-0000-4000-8000-000000000006", chatRoomId = KOSMOS_DESIGN, senderId = DEMO, senderName = "Aravya Sharma",
                content = "Love the contrast on the priority badges. Can we make due dates more prominent?", timestamp = daysAgo(3), type = MessageType.TEXT,
                readBy = listOf(PRIYA)),
            Message(id = "f0000000-0000-4000-8000-000000000007", chatRoomId = KOSMOS_DESIGN, senderId = PRIYA, senderName = "Priya Nair",
                content = "Absolutely — I'll iterate on the due date pill this afternoon.", timestamp = daysAgo(2), type = MessageType.TEXT,
                readBy = listOf(DEMO)),
            Message(id = "f0000000-0000-4000-8000-000000000008", chatRoomId = KOSMOS_DESIGN, senderId = DEMO, senderName = "Aravya Sharma",
                content = "Nice, the onboarding flow is much smoother now.", timestamp = hoursAgo(5), type = MessageType.TEXT,
                readBy = listOf(PRIYA)),
            Message(id = "f0000000-0000-4000-8000-000000000009", chatRoomId = KOSMOS_DESIGN, senderId = PRIYA, senderName = "Priya Nair",
                content = "Pushed the updated task card to Figma", timestamp = hoursAgo(3), type = MessageType.TEXT,
                readBy = listOf(DEMO, RAHUL, SNEHA)),

            // Engineering channel
            Message(id = "f0000000-0000-4000-8000-000000000010", chatRoomId = KOSMOS_ENGINEERING, senderId = RAHUL, senderName = "Rahul Verma",
                content = "Supabase row-level security is now enforced on all tables.", timestamp = daysAgo(2), type = MessageType.TEXT,
                readBy = listOf(DEMO, SNEHA)),
            Message(id = "f0000000-0000-4000-8000-000000000011", chatRoomId = KOSMOS_ENGINEERING, senderId = DEMO, senderName = "Aravya Sharma",
                content = "Nice. Let me know when the realtime channel for chat is ready to test.", timestamp = daysAgo(2), type = MessageType.TEXT,
                readBy = listOf(RAHUL)),
            Message(id = "f0000000-0000-4000-8000-000000000012", chatRoomId = KOSMOS_ENGINEERING, senderId = RAHUL, senderName = "Rahul Verma",
                content = "Realtime is wired up. Pushing an OTA update shortly.", timestamp = daysAgo(1), type = MessageType.TEXT,
                readBy = listOf(DEMO)),
            Message(id = "f0000000-0000-4000-8000-000000000013", chatRoomId = KOSMOS_ENGINEERING, senderId = SNEHA, senderName = "Sneha Kulkarni",
                content = "Found a minor crash on API 26 — logging a ticket in Engineering.", timestamp = hoursAgo(8), type = MessageType.TEXT,
                readBy = listOf(DEMO, RAHUL)),
            Message(id = "f0000000-0000-4000-8000-000000000014", chatRoomId = KOSMOS_ENGINEERING, senderId = RAHUL, senderName = "Rahul Verma",
                content = "CI is green again ✅", timestamp = hoursAgo(6), type = MessageType.TEXT,
                readBy = listOf(DEMO, SNEHA)),

            // DM with Priya
            Message(id = "f0000000-0000-4000-8000-000000000015", chatRoomId = DM_PRIYA, senderId = DEMO, senderName = "Aravya Sharma",
                content = "Hey Priya, can you review the new project header mockup?", timestamp = daysAgo(1), type = MessageType.TEXT,
                readBy = listOf(PRIYA)),
            Message(id = "f0000000-0000-4000-8000-000000000016", chatRoomId = DM_PRIYA, senderId = PRIYA, senderName = "Priya Nair",
                content = "Just did — the gradient looks great. One tweak on spacing.", timestamp = hoursAgo(6), type = MessageType.TEXT,
                readBy = listOf(DEMO)),
            Message(id = "f0000000-0000-4000-8000-000000000017", chatRoomId = DM_PRIYA, senderId = DEMO, senderName = "Aravya Sharma",
                content = "Perfect. I'll apply it before the sprint demo.", timestamp = hoursAgo(3), type = MessageType.TEXT,
                readBy = listOf(PRIYA)),
            Message(id = "f0000000-0000-4000-8000-000000000018", chatRoomId = DM_PRIYA, senderId = PRIYA, senderName = "Priya Nair",
                content = "Sounds good, let's ship it this week!", timestamp = hoursAgo(2), type = MessageType.TEXT,
                readBy = emptyList())
        )
        messageDao.insertMessages(messages)
    }

    // ========================================================================
    // TASKS (assignees, due dates, statuses across all projects)
    // ========================================================================
    private suspend fun seedTasks() {
        val tasks = listOf(
            // Kosmos App (owner = demo)
            Task(id = "b0000000-0000-4000-8000-000000000001", projectId = KOSMOS, title = "Implement Room offline sync queue",
                description = "Queue writes locally and sync to Supabase in the background with retry and conflict resolution.",
                status = TaskStatus.DONE, priority = TaskPriority.HIGH, assignedToId = DEMO, assignedToName = "Aravya Sharma",
                assignedToRole = ProjectRole.ADMIN, createdById = ARJUN, createdByName = "Arjun Mehta", createdByRole = ProjectRole.MANAGER,
                createdAt = daysAgo(12), updatedAt = daysAgo(3), dueDate = daysAgo(5), tags = listOf("backend", "sync"),
                actualHours = 22f, estimatedHours = 24f, chatRoomId = KOSMOS_ENGINEERING),
            Task(id = "b0000000-0000-4000-8000-000000000002", projectId = KOSMOS, title = "Design system tokens for dark mode",
                description = "Add dark theme color and elevation tokens across all Material 3 components.",
                status = TaskStatus.DONE, priority = TaskPriority.MEDIUM, assignedToId = PRIYA, assignedToName = "Priya Nair",
                assignedToRole = ProjectRole.MANAGER, createdById = DEMO, createdByName = "Aravya Sharma", createdByRole = ProjectRole.ADMIN,
                createdAt = daysAgo(11), updatedAt = daysAgo(4), dueDate = daysAgo(6), tags = listOf("design", "theming"),
                actualHours = 10f, estimatedHours = 12f, chatRoomId = KOSMOS_DESIGN),
            Task(id = "b0000000-0000-4000-8000-000000000003", projectId = KOSMOS, title = "Realtime chat via Supabase channels",
                description = "Subscribe to chat_room postgres_changes events and emit to Compose state.",
                status = TaskStatus.IN_PROGRESS, priority = TaskPriority.HIGH, assignedToId = RAHUL, assignedToName = "Rahul Verma",
                assignedToRole = ProjectRole.MANAGER, createdById = DEMO, createdByName = "Aravya Sharma", createdByRole = ProjectRole.ADMIN,
                createdAt = daysAgo(8), updatedAt = hoursAgo(6), dueDate = daysFromNow(3), tags = listOf("backend", "realtime"),
                actualHours = 8f, estimatedHours = 16f, chatRoomId = KOSMOS_ENGINEERING),
            Task(id = "b0000000-0000-4000-8000-000000000004", projectId = KOSMOS, title = "QA pass: offline-to-online handoff",
                description = "Verify queued edits flush correctly after connectivity is restored.",
                status = TaskStatus.IN_PROGRESS, priority = TaskPriority.MEDIUM, assignedToId = SNEHA, assignedToName = "Sneha Kulkarni",
                assignedToRole = ProjectRole.MEMBER, createdById = DEMO, createdByName = "Aravya Sharma", createdByRole = ProjectRole.ADMIN,
                createdAt = daysAgo(5), updatedAt = hoursAgo(9), dueDate = daysFromNow(2), tags = listOf("qa", "offline"),
                actualHours = 3f, estimatedHours = 8f),
            Task(id = "b0000000-0000-4000-8000-000000000005", projectId = KOSMOS, title = "Onboarding flow polish",
                description = "Reduce signup friction; add social auth and demo-mode entry.",
                status = TaskStatus.TODO, priority = TaskPriority.HIGH, assignedToId = DEMO, assignedToName = "Aravya Sharma",
                assignedToRole = ProjectRole.ADMIN, createdById = PRIYA, createdByName = "Priya Nair", createdByRole = ProjectRole.MANAGER,
                createdAt = daysAgo(2), updatedAt = daysAgo(1), dueDate = daysFromNow(5), tags = listOf("auth", "ux"),
                estimatedHours = 12f, chatRoomId = KOSMOS_DESIGN),
            Task(id = "b0000000-0000-4000-8000-000000000006", projectId = KOSMOS, title = "Push notifications for mentions",
                description = "Deliver FCM push when a user is @mentioned in chat.",
                status = TaskStatus.TODO, priority = TaskPriority.LOW, assignedToId = RAHUL, assignedToName = "Rahul Verma",
                assignedToRole = ProjectRole.MANAGER, createdById = DEMO, createdByName = "Aravya Sharma", createdByRole = ProjectRole.ADMIN,
                createdAt = daysAgo(1), updatedAt = hoursAgo(2), dueDate = daysFromNow(12), tags = listOf("notifications"),
                estimatedHours = 10f),
            Task(id = "b0000000-0000-4000-8000-000000000007", projectId = KOSMOS, title = "Release 1.0 checklists",
                description = "Store checklist, crash reporting, and store listing assets.",
                status = TaskStatus.TODO, priority = TaskPriority.MEDIUM, assignedToId = ARJUN, assignedToName = "Arjun Mehta",
                assignedToRole = ProjectRole.MANAGER, createdById = DEMO, createdByName = "Aravya Sharma", createdByRole = ProjectRole.ADMIN,
                createdAt = daysAgo(1), updatedAt = hoursAgo(1), dueDate = daysFromNow(14), tags = listOf("release"),
                estimatedHours = 6f),
            Task(id = "b0000000-0000-4000-8000-000000000008", projectId = KOSMOS, title = "Fix crash on API 26 during chat open",
                description = "NPE in Room message query when voice message is null.",
                status = TaskStatus.TODO, priority = TaskPriority.URGENT, assignedToId = DEMO, assignedToName = "Aravya Sharma",
                assignedToRole = ProjectRole.ADMIN, createdById = SNEHA, createdByName = "Sneha Kulkarni", createdByRole = ProjectRole.MEMBER,
                createdAt = hoursAgo(8), updatedAt = hoursAgo(8), dueDate = daysFromNow(1), tags = listOf("bug"),
                estimatedHours = 3f),
            Task(id = "b0000000-0000-4000-8000-000000000009", projectId = KOSMOS, title = "Task board drag-and-drop",
                description = "Move tasks between status columns with optimistic local updates.",
                status = TaskStatus.TODO, priority = TaskPriority.MEDIUM, assignedToId = SNEHA, assignedToName = "Sneha Kulkarni",
                assignedToRole = ProjectRole.MEMBER, createdById = PRIYA, createdByName = "Priya Nair", createdByRole = ProjectRole.MANAGER,
                createdAt = hoursAgo(3), updatedAt = hoursAgo(3), dueDate = daysFromNow(10), tags = listOf("feature"),
                estimatedHours = 16f),

            // Nebula Website
            Task(id = "b0000000-0000-4000-8000-000000000010", projectId = NEBULA, title = "Design system Figma library",
                description = "Buttons, forms, typography scale, and color ramps.",
                status = TaskStatus.DONE, priority = TaskPriority.HIGH, assignedToId = PRIYA, assignedToName = "Priya Nair",
                assignedToRole = ProjectRole.ADMIN, createdById = PRIYA, createdByName = "Priya Nair", createdByRole = ProjectRole.ADMIN,
                createdAt = daysAgo(50), updatedAt = daysAgo(10), dueDate = daysAgo(15), tags = listOf("design"),
                actualHours = 20f, estimatedHours = 20f),
            Task(id = "b0000000-0000-4000-8000-000000000011", projectId = NEBULA, title = "Homepage hero section",
                description = "Build responsive hero with gradient background and CTA.",
                status = TaskStatus.IN_PROGRESS, priority = TaskPriority.HIGH, assignedToId = DEMO, assignedToName = "Aravya Sharma",
                assignedToRole = ProjectRole.MEMBER, createdById = PRIYA, createdByName = "Priya Nair", createdByRole = ProjectRole.ADMIN,
                createdAt = daysAgo(6), updatedAt = hoursAgo(5), dueDate = daysFromNow(7), tags = listOf("frontend"),
                actualHours = 4f, estimatedHours = 8f),
            Task(id = "b0000000-0000-4000-8000-000000000012", projectId = NEBULA, title = "CMS content model",
                description = "Define pages, sections, and blog content types.",
                status = TaskStatus.TODO, priority = TaskPriority.MEDIUM, assignedToId = ARJUN, assignedToName = "Arjun Mehta",
                assignedToRole = ProjectRole.MANAGER, createdById = PRIYA, createdByName = "Priya Nair", createdByRole = ProjectRole.ADMIN,
                createdAt = daysAgo(3), updatedAt = daysAgo(2), dueDate = daysFromNow(10), tags = listOf("cms"),
                estimatedHours = 6f),
            Task(id = "b0000000-0000-4000-8000-000000000013", projectId = NEBULA, title = "Migrate blog posts",
                description = "Import 120 legacy posts and verify SEO redirects.",
                status = TaskStatus.TODO, priority = TaskPriority.LOW, assignedToId = MEERA, assignedToName = "Meera Krishnan",
                assignedToRole = ProjectRole.MEMBER, createdById = ARJUN, createdByName = "Arjun Mehta", createdByRole = ProjectRole.MANAGER,
                createdAt = daysAgo(2), updatedAt = daysAgo(1), dueDate = daysFromNow(20), tags = listOf("content"),
                estimatedHours = 12f),

            // Orbit Social Club
            Task(id = "b0000000-0000-4000-8000-000000000014", projectId = ORBIT, title = "August hackathon planning",
                description = "Venue, sponsors, judges, and participant swag.",
                status = TaskStatus.IN_PROGRESS, priority = TaskPriority.HIGH, assignedToId = MEERA, assignedToName = "Meera Krishnan",
                assignedToRole = ProjectRole.MANAGER, createdById = DEMO, createdByName = "Aravya Sharma", createdByRole = ProjectRole.ADMIN,
                createdAt = daysAgo(20), updatedAt = daysAgo(1), dueDate = daysFromNow(15), tags = listOf("events"),
                actualHours = 6f, estimatedHours = 15f),
            Task(id = "b0000000-0000-4000-8000-000000000015", projectId = ORBIT, title = "Discord community onboarding",
                description = "Automate join flow with role assignment and rules.",
                status = TaskStatus.DONE, priority = TaskPriority.MEDIUM, assignedToId = DEMO, assignedToName = "Aravya Sharma",
                assignedToRole = ProjectRole.ADMIN, createdById = MEERA, createdByName = "Meera Krishnan", createdByRole = ProjectRole.MANAGER,
                createdAt = daysAgo(30), updatedAt = daysAgo(25), dueDate = daysAgo(26), tags = listOf("community"),
                actualHours = 5f, estimatedHours = 5f),
            Task(id = "b0000000-0000-4000-8000-000000000016", projectId = ORBIT, title = "Sponsorship proposal deck",
                description = "Tiered sponsorship packages with analytics summary.",
                status = TaskStatus.TODO, priority = TaskPriority.MEDIUM, assignedToId = KAVYA, assignedToName = "Kavya Singh",
                assignedToRole = ProjectRole.MEMBER, createdById = MEERA, createdByName = "Meera Krishnan", createdByRole = ProjectRole.MANAGER,
                createdAt = daysAgo(5), updatedAt = daysAgo(3), dueDate = daysFromNow(9), tags = listOf("marketing"),
                estimatedHours = 8f),
            Task(id = "b0000000-0000-4000-8000-000000000017", projectId = ORBIT, title = "Tech talk: Offline-first Android",
                description = "Prepare a 20-min talk on Room + Supabase offline sync for the August meetup.",
                status = TaskStatus.TODO, priority = TaskPriority.LOW, assignedToId = DEMO, assignedToName = "Aravya Sharma",
                assignedToRole = ProjectRole.ADMIN, createdById = DEMO, createdByName = "Aravya Sharma", createdByRole = ProjectRole.ADMIN,
                createdAt = daysAgo(4), updatedAt = daysAgo(2), dueDate = daysFromNow(21), tags = listOf("talks"),
                estimatedHours = 6f),

            // Venture Pitch Prep
            Task(id = "b0000000-0000-4000-8000-000000000018", projectId = VENTURE, title = "Series A pitch deck v2",
                description = "Narrative arc with market size, traction, and financial projections.",
                status = TaskStatus.TODO, priority = TaskPriority.HIGH, assignedToId = ARJUN, assignedToName = "Arjun Mehta",
                assignedToRole = ProjectRole.ADMIN, createdById = ARJUN, createdByName = "Arjun Mehta", createdByRole = ProjectRole.ADMIN,
                createdAt = daysAgo(12), updatedAt = daysAgo(4), dueDate = daysFromNow(7), tags = listOf("fundraising"),
                estimatedHours = 20f),
            Task(id = "b0000000-0000-4000-8000-000000000019", projectId = VENTURE, title = "Financial model for 3 scenarios",
                description = "Base, upside, and downside projections through FY27.",
                status = TaskStatus.TODO, priority = TaskPriority.MEDIUM, assignedToId = KAVYA, assignedToName = "Kavya Singh",
                assignedToRole = ProjectRole.MEMBER, createdById = ARJUN, createdByName = "Arjun Mehta", createdByRole = ProjectRole.ADMIN,
                createdAt = daysAgo(10), updatedAt = daysAgo(3), dueDate = daysFromNow(10), tags = listOf("finance"),
                estimatedHours = 12f),
            Task(id = "b0000000-0000-4000-8000-000000000020", projectId = VENTURE, title = "Product demo video",
                description = "3-minute walkthrough for investor outreach.",
                status = TaskStatus.TODO, priority = TaskPriority.MEDIUM, assignedToId = DEMO, assignedToName = "Aravya Sharma",
                assignedToRole = ProjectRole.MEMBER, createdById = ARJUN, createdByName = "Arjun Mehta", createdByRole = ProjectRole.ADMIN,
                createdAt = daysAgo(8), updatedAt = daysAgo(2), dueDate = daysFromNow(14), tags = listOf("demo"),
                estimatedHours = 6f)
        )
        taskDao.insertTasks(tasks)
    }

    // ========================================================================
    // TASK ACTIVITY (timeline feeds)
    // ========================================================================
    private suspend fun seedTaskActivity() {
        val activities = listOf(
            TaskActivity(id = "c0000000-0000-4000-8000-000000000001", taskId = "b0000000-0000-4000-8000-000000000003", projectId = KOSMOS,
                actorId = RAHUL, actorName = "Rahul Verma", actorRole = "MANAGER",
                actionType = ActivityActionType.STATUS_CHANGED, timestamp = hoursAgo(6),
                changes = listOf(com.example.kosmos.core.models.FieldChange("status", "TODO", "IN_PROGRESS", "To Do", "In Progress")),
                autoDescription = "changed status from To Do to In Progress"),
            TaskActivity(id = "c0000000-0000-4000-8000-000000000002", taskId = "b0000000-0000-4000-8000-000000000003", projectId = KOSMOS,
                actorId = DEMO, actorName = "Aravya Sharma", actorRole = "ADMIN",
                actionType = ActivityActionType.CREATED, timestamp = daysAgo(8),
                changes = emptyList(), autoDescription = "created this task"),
            TaskActivity(id = "c0000000-0000-4000-8000-000000000003", taskId = "b0000000-0000-4000-8000-000000000008", projectId = KOSMOS,
                actorId = SNEHA, actorName = "Sneha Kulkarni", actorRole = "MEMBER",
                actionType = ActivityActionType.CREATED, timestamp = hoursAgo(8),
                changes = emptyList(), autoDescription = "created this task",
                commitMessage = "Logging the API 26 crash for triage"),
            TaskActivity(id = "c0000000-0000-4000-8000-000000000004", taskId = "b0000000-0000-4000-8000-000000000005", projectId = KOSMOS,
                actorId = PRIYA, actorName = "Priya Nair", actorRole = "MANAGER",
                actionType = ActivityActionType.ASSIGNED, timestamp = daysAgo(1),
                changes = listOf(com.example.kosmos.core.models.FieldChange("assignedTo", "Unassigned", "Aravya Sharma", "None", "Aravya Sharma")),
                autoDescription = "assigned to Aravya Sharma"),
            TaskActivity(id = "c0000000-0000-4000-8000-000000000005", taskId = "b0000000-0000-4000-8000-000000000001", projectId = KOSMOS,
                actorId = DEMO, actorName = "Aravya Sharma", actorRole = "ADMIN",
                actionType = ActivityActionType.STATUS_CHANGED, timestamp = daysAgo(3),
                changes = listOf(com.example.kosmos.core.models.FieldChange("status", "IN_PROGRESS", "DONE", "In Progress", "Done")),
                autoDescription = "changed status from In Progress to Done")
        )
        taskActivityDao.insertActivities(activities)
    }

    // ========================================================================
    // USER CONNECTIONS (accepted + pending for connections UI)
    // ========================================================================
    private suspend fun seedConnections() {
        val connections = listOf(
            UserConnection(id = "d0000000-0000-4000-8000-000000000001", requesterId = DEMO, addresseeId = PRIYA,
                status = ConnectionStatus.ACCEPTED, createdAt = daysAgo(60), respondedAt = daysAgo(59)),
            UserConnection(id = "d0000000-0000-4000-8000-000000000002", requesterId = DEMO, addresseeId = RAHUL,
                status = ConnectionStatus.ACCEPTED, createdAt = daysAgo(58), respondedAt = daysAgo(57)),
            UserConnection(id = "d0000000-0000-4000-8000-000000000003", requesterId = SNEHA, addresseeId = DEMO,
                status = ConnectionStatus.ACCEPTED, createdAt = daysAgo(30), respondedAt = daysAgo(30)),
            UserConnection(id = "d0000000-0000-4000-8000-000000000004", requesterId = MEERA, addresseeId = DEMO,
                status = ConnectionStatus.PENDING, createdAt = daysAgo(1), respondedAt = null),
            UserConnection(id = "d0000000-0000-4000-8000-000000000005", requesterId = DEMO, addresseeId = KAVYA,
                status = ConnectionStatus.PENDING, createdAt = daysAgo(1), respondedAt = null)
        )
        userConnectionDao.insertAll(connections)
    }

    // ========================================================================
    // INVITES + JOIN REQUESTS
    // ========================================================================
    private suspend fun seedInvitesAndRequests() {
        projectInviteDao.insertAll(
            listOf(
                ProjectInvite(id = "d1000000-0000-4000-8000-000000000001", projectId = KOSMOS, inviteeId = MEERA, inviterId = DEMO,
                    role = "MEMBER", status = InviteStatus.PENDING, message = "Would love your marketing expertise on Kosmos!",
                    createdAt = daysAgo(2), expiresAt = daysFromNow(5))
            )
        )
        projectJoinRequestDao.insertAll(
            listOf(
                ProjectJoinRequest(id = "d2000000-0000-4000-8000-000000000001", projectId = KOSMOS, requesterId = KAVYA,
                    message = "I'd like to contribute analytics dashboards to Kosmos.",
                    status = JoinRequestStatus.PENDING, createdAt = daysAgo(1))
            )
        )
    }

    // ========================================================================
    // TIME ENTRIES
    // ========================================================================
    private suspend fun seedTimeEntries() {
        timeEntryDao.insertEntries(
            listOf(
                TimeEntry(id = "d3000000-0000-4000-8000-000000000001", taskId = "b0000000-0000-4000-8000-000000000001", projectId = KOSMOS,
                    userId = DEMO, startTime = daysAgo(6), endTime = daysAgo(6),
                    durationSeconds = 4 * 3600 + 30 * 60, description = "Implemented sync queue with WorkManager retry", isBillable = true, hourlyRate = 90f, isManual = true),
                TimeEntry(id = "d3000000-0000-4000-8000-000000000002", taskId = "b0000000-0000-4000-8000-000000000003", projectId = KOSMOS,
                    userId = RAHUL, startTime = hoursAgo(9), endTime = hoursAgo(2),
                    durationSeconds = 7 * 3600, description = "Realtime channel wiring", isBillable = true, hourlyRate = 85f, isManual = false),
                TimeEntry(id = "d3000000-0000-4000-8000-000000000003", taskId = "b0000000-0000-4000-8000-000000000005", projectId = KOSMOS,
                    userId = DEMO, startTime = hoursAgo(4), endTime = hoursAgo(2),
                    durationSeconds = 2 * 3600, description = "Onboarding polish — social auth buttons", isBillable = true, hourlyRate = 90f, isManual = false)
            )
        )
    }

    // ========================================================================
    // TASK DEPENDENCIES
    // ========================================================================
    private suspend fun seedDependencies() {
        taskDependencyDao.insertDependencies(
            listOf(
                TaskDependency(id = "d4000000-0000-4000-8000-000000000001", taskId = "b0000000-0000-4000-8000-000000000009",
                    dependsOnTaskId = "b0000000-0000-4000-8000-000000000001", dependencyType = DependencyType.BLOCKED_BY,
                    createdAt = daysAgo(2), createdBy = DEMO),
                TaskDependency(id = "d4000000-0000-4000-8000-000000000002", taskId = "b0000000-0000-4000-8000-000000000004",
                    dependsOnTaskId = "b0000000-0000-4000-8000-000000000003", dependencyType = DependencyType.BLOCKED_BY,
                    createdAt = daysAgo(1), createdBy = DEMO)
            )
        )
    }
}
