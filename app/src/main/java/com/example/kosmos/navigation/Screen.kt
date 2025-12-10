package com.example.kosmos

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ProjectList : Screen("projectList")
    object ProjectDetail : Screen("project/{projectId}") {
        fun createRoute(projectId: String) = "project/$projectId"
    }
    object Chat : Screen("chat/{chatRoomId}") {
        fun createRoute(chatRoomId: String) = "chat/$chatRoomId"
    }
    object TaskBoard : Screen("taskBoard/{projectId}?chatRoomId={chatRoomId}") {
        fun createRoute(projectId: String, chatRoomId: String? = null) =
            if (chatRoomId != null) {
                "taskBoard/$projectId?chatRoomId=$chatRoomId"
            } else {
                "taskBoard/$projectId"
            }
    }
    object TaskEdit : Screen("taskEdit/{taskId}") {
        fun createRoute(taskId: String) = "taskEdit/$taskId"
    }
    object TaskDetail : Screen("taskDetail/{taskId}") {
        fun createRoute(taskId: String) = "taskDetail/$taskId"
    }
    object Profile : Screen("profile")
    object EditProfile : Screen("editProfile")
    object Settings : Screen("settings")
    object PrivacySettings : Screen("privacySettings")
    object NotificationSettings : Screen("notificationSettings")
    object NotificationList : Screen("notificationList")
    object About : Screen("about")
    object UserSearch : Screen("userSearch/{projectId}") {
        fun createRoute(projectId: String) = "userSearch/$projectId"
    }
    object InviteMembers : Screen("inviteMembers/{projectId}") {
        fun createRoute(projectId: String) = "inviteMembers/$projectId"
    }
    object MembersList : Screen("membersList/{projectId}") {
        fun createRoute(projectId: String) = "membersList/$projectId"
    }
    object UserProfile : Screen("userProfile/{userId}/{projectId}") {
        fun createRoute(userId: String, projectId: String) = "userProfile/$userId/$projectId"
    }
    object ActivityLog : Screen("activityLog/{projectId}") {
        fun createRoute(projectId: String) = "activityLog/$projectId"
    }
    object ProjectEdit : Screen("projectEdit/{projectId}") {
        fun createRoute(projectId: String) = "projectEdit/$projectId"
    }
    object MyTasks : Screen("myTasks")
    object ChatHub : Screen("chatHub")
    object More : Screen("more")
    object Discover : Screen("discover")
    object Connections : Screen("connections")
    object ProjectWorkspace : Screen("projectWorkspace/{projectId}") {
        fun createRoute(projectId: String) = "projectWorkspace/$projectId"
    }
    object Splash : Screen("splash")
    object GoogleProfileSetup : Screen("googleProfileSetup")
}
