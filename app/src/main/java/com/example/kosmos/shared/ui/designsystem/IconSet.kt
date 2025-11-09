package com.example.kosmos.shared.ui.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icon Set for Kosmos App
 *
 * Centralized icon definitions using Material Icons.
 * Provides both filled and outlined variants for flexibility.
 *
 * Usage: IconSet.Navigation.home instead of Icons.Filled.Home
 * This allows easy icon replacement across the entire app.
 */
object IconSet {

    /**
     * Navigation Icons
     * For bottom navigation, tabs, and primary navigation
     */
    object Navigation {
        // Bottom Navigation
        val projects = Icons.Filled.Folder
        val projectsOutlined = Icons.Outlined.Folder
        val chats = Icons.Filled.Chat
        val chatsOutlined = Icons.Outlined.Chat
        val tasks = Icons.Filled.CheckCircle
        val tasksOutlined = Icons.Outlined.CheckCircle
        val more = Icons.Filled.MoreHoriz
        val moreOutlined = Icons.Outlined.MoreHoriz

        // Navigation Actions
        val back = Icons.AutoMirrored.Filled.ArrowBack
        val forward = Icons.AutoMirrored.Filled.ArrowForward
        val close = Icons.Filled.Close
        val menu = Icons.Filled.Menu
        val home = Icons.Filled.Home
        val homeOutlined = Icons.Outlined.Home
    }

    /**
     * Action Icons
     * For buttons, FABs, and action items
     */
    object Action {
        val add = Icons.Filled.Add
        val create = Icons.Filled.Create
        val edit = Icons.Filled.Edit
        val editOutlined = Icons.Outlined.Edit
        val delete = Icons.Filled.Delete
        val deleteOutlined = Icons.Outlined.Delete
        val save = Icons.Filled.Save
        val cancel = Icons.Filled.Cancel
        val done = Icons.Filled.Done
        val check = Icons.Filled.Check
        val clear = Icons.Filled.Clear
        val refresh = Icons.Filled.Refresh
        val sync = Icons.Filled.Sync
        val search = Icons.Filled.Search
        val filter = Icons.Filled.FilterList
        val sort = Icons.Filled.Sort
        val share = Icons.Filled.Share
        val copy = Icons.Filled.ContentCopy
        val download = Icons.Filled.Download
        val upload = Icons.Filled.Upload
        val expand = Icons.Filled.ExpandMore
        val collapse = Icons.Filled.ExpandLess
        val moreVert = Icons.Filled.MoreVert
        val moreHoriz = Icons.Filled.MoreHoriz
    }

    /**
     * Message/Chat Icons
     * For messaging features
     */
    object Message {
        val send = Icons.AutoMirrored.Filled.Send
        val sendOutlined = Icons.Outlined.Send
        val message = Icons.Filled.Message
        val messageOutlined = Icons.Outlined.Message
        val chat = Icons.Filled.Chat
        val chatOutlined = Icons.Outlined.Chat
        val chatBubble = Icons.Filled.ChatBubble
        val chatBubbleOutlined = Icons.Outlined.ChatBubble
        val reply = Icons.AutoMirrored.Filled.Reply
        val forward = Icons.AutoMirrored.Filled.Forward
        val reaction = Icons.Filled.AddReaction
        val emoji = Icons.Filled.EmojiEmotions
        val attach = Icons.Filled.AttachFile
        val image = Icons.Filled.Image
        val camera = Icons.Filled.CameraAlt
        val microphone = Icons.Filled.Mic
        val microphoneOff = Icons.Filled.MicOff
        val voice = Icons.Filled.VoiceChat
        val pin = Icons.Filled.PushPin
        val pinOutlined = Icons.Outlined.PushPin
        val archive = Icons.Filled.Archive
        val archiveOutlined = Icons.Outlined.Archive
        val unarchive = Icons.Filled.Unarchive
        val markRead = Icons.Filled.MarkEmailRead
        val markUnread = Icons.Filled.MarkEmailUnread
    }

    /**
     * Task Icons
     * For task management features
     */
    object Task {
        val task = Icons.Filled.Task
        val taskOutlined = Icons.Outlined.Task
        val checkCircle = Icons.Filled.CheckCircle
        val checkCircleOutlined = Icons.Outlined.CheckCircle
        val radioButtonUnchecked = Icons.Filled.RadioButtonUnchecked
        val assignment = Icons.Filled.Assignment
        val assignmentOutlined = Icons.Outlined.Assignment
        val assignmentTurnedIn = Icons.Filled.AssignmentTurnedIn
        val list = Icons.Filled.List
        val board = Icons.Filled.Dashboard
        val boardOutlined = Icons.Outlined.Dashboard
        val calendar = Icons.Filled.CalendarToday
        val calendarOutlined = Icons.Outlined.CalendarToday
        val priority = Icons.Filled.PriorityHigh
        val flag = Icons.Filled.Flag
        val flagOutlined = Icons.Outlined.Flag
    }

    /**
     * User/Profile Icons
     * For user-related features
     */
    object User {
        val person = Icons.Filled.Person
        val personOutlined = Icons.Outlined.Person
        val personAdd = Icons.Filled.PersonAdd
        val personAddOutlined = Icons.Outlined.PersonAdd
        val people = Icons.Filled.People
        val peopleOutlined = Icons.Outlined.People
        val group = Icons.Filled.Group
        val groupOutlined = Icons.Outlined.Group
        val account = Icons.Filled.AccountCircle
        val accountOutlined = Icons.Outlined.AccountCircle
        val profile = Icons.Filled.AccountBox
        val profileOutlined = Icons.Outlined.AccountBox
        val logout = Icons.AutoMirrored.Filled.Logout
        val login = Icons.AutoMirrored.Filled.Login
    }

    /**
     * Status Icons
     * For presence, connection, and status indicators
     */
    object Status {
        val online = Icons.Filled.Circle      // Will be colored green
        val offline = Icons.Outlined.Circle   // Will be colored gray
        val away = Icons.Filled.Circle        // Will be colored orange
        val busy = Icons.Filled.DoNotDisturb
        val typing = Icons.Filled.MoreHoriz
        val connected = Icons.Filled.Wifi
        val disconnected = Icons.Filled.WifiOff
        val syncing = Icons.Filled.Sync
        val syncProblem = Icons.Filled.SyncProblem
        val checkmark = Icons.Filled.Check
        val doubleCheck = Icons.Filled.DoneAll // For read receipts
        val error = Icons.Filled.Error
        val errorOutlined = Icons.Outlined.Error
        val warning = Icons.Filled.Warning
        val warningOutlined = Icons.Outlined.Warning
        val info = Icons.Filled.Info
        val infoOutlined = Icons.Outlined.Info
        val success = Icons.Filled.CheckCircle
    }

    /**
     * Settings Icons
     * For settings and configuration
     */
    object Settings {
        val settings = Icons.Filled.Settings
        val settingsOutlined = Icons.Outlined.Settings
        val notifications = Icons.Filled.Notifications
        val notificationsOutlined = Icons.Outlined.Notifications
        val notificationsOff = Icons.Filled.NotificationsOff
        val notificationsActive = Icons.Filled.NotificationsActive
        val privacy = Icons.Filled.Lock
        val privacyOutlined = Icons.Outlined.Lock
        val security = Icons.Filled.Security
        val theme = Icons.Filled.Palette
        val darkMode = Icons.Filled.DarkMode
        val lightMode = Icons.Filled.LightMode
        val language = Icons.Filled.Language
        val help = Icons.Filled.Help
        val helpOutlined = Icons.Outlined.Help
        val info = Icons.Filled.Info
        val infoOutlined = Icons.Outlined.Info
        val about = Icons.Outlined.Info
    }

    /**
     * Project Icons
     * For project management
     */
    object Project {
        val project = Icons.Filled.Folder
        val projectOutlined = Icons.Outlined.Folder
        val folderOpen = Icons.Filled.FolderOpen
        val star = Icons.Filled.Star
        val starOutlined = Icons.Outlined.Star
        val starBorder = Icons.Outlined.StarBorder
        val bookmark = Icons.Filled.Bookmark
        val bookmarkOutlined = Icons.Outlined.Bookmark
        val bookmarkBorder = Icons.Outlined.BookmarkBorder
        val label = Icons.Filled.Label
        val labelOutlined = Icons.Outlined.Label
    }

    /**
     * File/Attachment Icons
     * For file handling
     */
    object File {
        val file = Icons.Filled.InsertDriveFile
        val fileOutlined = Icons.Outlined.InsertDriveFile
        val folder = Icons.Filled.Folder
        val folderOutlined = Icons.Outlined.Folder
        val image = Icons.Filled.Image
        val imageOutlined = Icons.Outlined.Image
        val video = Icons.Filled.VideoFile
        val audio = Icons.Filled.AudioFile
        val document = Icons.Filled.Description
        val documentOutlined = Icons.Outlined.Description
        val pdf = Icons.Filled.PictureAsPdf
        val attachment = Icons.Filled.AttachFile
        val download = Icons.Filled.Download
        val upload = Icons.Filled.Upload
    }

    /**
     * Feedback Icons
     * For user feedback and states
     */
    object Feedback {
        val success = Icons.Filled.CheckCircle
        val error = Icons.Filled.Error
        val warning = Icons.Filled.Warning
        val info = Icons.Filled.Info
        val loading = Icons.Filled.Sync
        val empty = Icons.Filled.Inbox
        val emptyOutlined = Icons.Outlined.Inbox
        val noResults = Icons.Filled.SearchOff
        val offline = Icons.Filled.CloudOff
    }

    /**
     * Time/Date Icons
     * For temporal information
     */
    object Time {
        val clock = Icons.Filled.AccessTime
        val clockOutlined = Icons.Outlined.AccessTime
        val calendar = Icons.Filled.CalendarToday
        val calendarOutlined = Icons.Outlined.CalendarToday
        val schedule = Icons.Filled.Schedule
        val history = Icons.Filled.History
        val timer = Icons.Filled.Timer
        val alarm = Icons.Filled.Alarm
        val alarmOutlined = Icons.Outlined.Alarm
        val today = Icons.Filled.Today
    }

    /**
     * Priority/Importance Icons
     * For task priority and importance markers
     */
    object Priority {
        val urgent = Icons.Filled.PriorityHigh
        val high = Icons.Filled.ArrowUpward
        val medium = Icons.Filled.DragHandle
        val low = Icons.Filled.ArrowDownward
        val flag = Icons.Filled.Flag
        val flagOutlined = Icons.Outlined.Flag
    }

    /**
     * Media Icons
     * For media playback and control
     */
    object Media {
        val play = Icons.Filled.PlayArrow
        val pause = Icons.Filled.Pause
        val stop = Icons.Filled.Stop
        val record = Icons.Filled.FiberManualRecord
        val forward = Icons.Filled.FastForward
        val rewind = Icons.Filled.FastRewind
        val volumeUp = Icons.Filled.VolumeUp
        val volumeDown = Icons.Filled.VolumeDown
        val volumeOff = Icons.Filled.VolumeOff
        val microphone = Icons.Filled.Mic
        val microphoneOff = Icons.Filled.MicOff
    }

    /**
     * Visibility Icons
     * For show/hide functionality
     */
    object Visibility {
        val visible = Icons.Filled.Visibility
        val visibleOutlined = Icons.Outlined.Visibility
        val invisible = Icons.Filled.VisibilityOff
        val invisibleOutlined = Icons.Outlined.VisibilityOff
    }

    /**
     * Direction Icons
     * For directional indicators
     */
    object Direction {
        val up = Icons.Filled.KeyboardArrowUp
        val down = Icons.Filled.KeyboardArrowDown
        val left = Icons.Filled.KeyboardArrowLeft
        val right = Icons.Filled.KeyboardArrowRight
        val expandMore = Icons.Filled.ExpandMore
        val expandLess = Icons.Filled.ExpandLess
        val chevronRight = Icons.Filled.ChevronRight
        val chevronLeft = Icons.Filled.ChevronLeft
    }

    /**
     * Gesture Icons
     * For gesture indicators and tutorials
     */
    object Gesture {
        val swipe = Icons.Filled.SwipeRight
        val touch = Icons.Filled.TouchApp
        val drag = Icons.Filled.DragIndicator
        val pinch = Icons.Filled.ZoomIn
        val tap = Icons.Filled.TouchApp
    }

    /**
     * Special Icons
     * For special features and states
     */
    object Special {
        val quickActions = Icons.Filled.Bolt
        val shortcuts = Icons.Filled.Keyboard
        val command = Icons.Filled.KeyboardCommandKey
        val ai = Icons.Filled.AutoAwesome
        val smart = Icons.Filled.Lightbulb
        val smartOutlined = Icons.Outlined.Lightbulb
        val magic = Icons.Filled.AutoFixHigh
        val verified = Icons.Filled.Verified
        val verifiedOutlined = Icons.Outlined.Verified
    }

    /**
     * Helper function to get appropriate icon based on state
     */
    object Helper {
        /**
         * Get navigation icon variant based on selected state
         */
        fun getNavigationIcon(iconType: NavigationIconType, isSelected: Boolean): ImageVector {
            return when (iconType) {
                NavigationIconType.PROJECTS -> if (isSelected) Navigation.projects else Navigation.projectsOutlined
                NavigationIconType.CHATS -> if (isSelected) Navigation.chats else Navigation.chatsOutlined
                NavigationIconType.TASKS -> if (isSelected) Navigation.tasks else Navigation.tasksOutlined
                NavigationIconType.MORE -> if (isSelected) Navigation.more else Navigation.moreOutlined
            }
        }

        /**
         * Get task status icon
         */
        fun getTaskStatusIcon(status: String): ImageVector {
            return when (status.uppercase()) {
                "TODO" -> Task.radioButtonUnchecked
                "IN_PROGRESS" -> Task.assignment
                "DONE" -> Task.checkCircle
                "CANCELLED" -> Action.cancel
                else -> Task.task
            }
        }

        /**
         * Get priority icon
         */
        fun getPriorityIcon(priority: String): ImageVector {
            return when (priority.uppercase()) {
                "URGENT" -> Priority.urgent
                "HIGH" -> Priority.high
                "MEDIUM" -> Priority.medium
                "LOW" -> Priority.low
                else -> Priority.flag
            }
        }

        /**
         * Get online status icon
         */
        fun getStatusIcon(status: String): ImageVector {
            return when (status.uppercase()) {
                "ONLINE" -> Status.online
                "OFFLINE" -> Status.offline
                "AWAY" -> Status.away
                "BUSY" -> Status.busy
                else -> Status.offline
            }
        }
    }

    /**
     * Navigation icon types enum
     */
    enum class NavigationIconType {
        PROJECTS,
        CHATS,
        TASKS,
        MORE
    }
}
