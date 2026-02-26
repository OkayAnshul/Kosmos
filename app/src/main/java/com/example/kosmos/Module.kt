package com.example.kosmos

import android.content.Context
import androidx.room.Room
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ChatRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.TaskRepository
import com.example.kosmos.data.repository.UserRepository
import com.example.kosmos.data.repository.VoiceRepository
import com.example.kosmos.core.database.dao.ActionItemDao
import com.example.kosmos.core.database.dao.ChatRoomDao
import com.example.kosmos.core.database.KosmosDatabase
import com.example.kosmos.core.database.dao.MessageDao
import com.example.kosmos.core.database.dao.ProjectDao
import com.example.kosmos.core.database.dao.ProjectMemberDao
import com.example.kosmos.core.database.dao.TaskDao
import com.example.kosmos.core.database.dao.UserDao
import com.example.kosmos.core.database.dao.VoiceMessageDao
import com.example.kosmos.data.datasource.SupabaseProjectDataSource
import com.example.kosmos.data.datasource.SupabaseProjectMemberDataSource
// Firebase imports removed - migrated to Supabase
import com.example.kosmos.core.config.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import com.google.gson.Gson
import com.google.gson.GsonBuilder
// Voice services disabled for MVP - will be re-enabled in Phase 5
// import com.example.kosmos.features.voice.services.SpeechToTextService
// import com.example.kosmos.features.voice.services.TranscriptionService
import com.example.kosmos.features.smart.services.ActionDetectionService
import com.example.kosmos.features.smart.services.SmartReplyService
import com.example.kosmos.shared.utils.NetworkMonitor
import com.example.kosmos.shared.utils.NetworkMonitorImpl
import com.example.kosmos.core.config.AppConfigRepository
import com.example.kosmos.core.coroutines.DispatcherProvider
import com.example.kosmos.core.coroutines.DefaultDispatcherProvider
import com.example.kosmos.features.announcements.AnnouncementRepository

import dagger.Module
import dagger.Binds
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideKosmosDatabase(@ApplicationContext context: Context): KosmosDatabase {
        return Room.databaseBuilder(
            context,
            KosmosDatabase::class.java,
            KosmosDatabase.DATABASE_NAME
        )
        // P0-04 FIX: All migrations defined - NO destructive fallback
        // Migrations ensure data persists across schema updates
        .addMigrations(
            KosmosDatabase.MIGRATION_1_2,
            KosmosDatabase.MIGRATION_2_3,
            KosmosDatabase.MIGRATION_3_4,
            KosmosDatabase.MIGRATION_4_5,  // P0-01 & P0-02: User version + task_activity table
            KosmosDatabase.MIGRATION_5_6,  // P0-05: Foreign key enforcement + indexes
            KosmosDatabase.MIGRATION_6_7,  // P0-08: Sync queue table
            KosmosDatabase.MIGRATION_7_8,  // Migration 7→8: Add is_pinned, estimated_hours, actual_hours
            KosmosDatabase.MIGRATION_8_9,  // Migration 8→9: Add sync_timestamps table for incremental sync
            KosmosDatabase.MIGRATION_9_10,  // Migration 9→10: Remove FK on task_activity.actorId
            KosmosDatabase.MIGRATION_10_11,  // Migration 10→11: Add time_entries and task_dependencies
            KosmosDatabase.MIGRATION_11_12   // Migration 11→12: Add project_invites, user_connections, project_join_requests
        )
        // P0-04 FIX: Removed .fallbackToDestructiveMigration() to prevent data loss
        // If a migration is missing, app will crash instead of silently wiping data
        // This forces developers to create proper migrations
        .build()
    }

    @Provides
    fun provideUserDao(database: KosmosDatabase): UserDao = database.userDao()

    @Provides
    fun provideChatRoomDao(database: KosmosDatabase): ChatRoomDao = database.chatRoomDao()

    @Provides
    fun provideMessageDao(database: KosmosDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideVoiceMessageDao(database: KosmosDatabase): VoiceMessageDao = database.voiceMessageDao()

    @Provides
    fun provideTaskDao(database: KosmosDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideActionItemDao(database: KosmosDatabase): ActionItemDao = database.actionItemDao()

    @Provides
    fun provideProjectDao(database: KosmosDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideProjectMemberDao(database: KosmosDatabase): ProjectMemberDao = database.projectMemberDao()

    @Provides
    fun provideTaskActivityDao(database: KosmosDatabase): com.example.kosmos.core.database.dao.TaskActivityDao = database.taskActivityDao()

    @Provides
    fun provideSyncQueueDao(database: KosmosDatabase): com.example.kosmos.core.database.dao.SyncQueueDao = database.syncQueueDao()  // P0-08 FIX

    @Provides
    fun provideSyncTimestampDao(database: KosmosDatabase): com.example.kosmos.core.database.dao.SyncTimestampDao = database.syncTimestampDao()  // Incremental sync

    @Provides
    fun provideTimeEntryDao(database: KosmosDatabase): com.example.kosmos.core.database.dao.TimeEntryDao = database.timeEntryDao()

    @Provides
    fun provideTaskDependencyDao(database: KosmosDatabase): com.example.kosmos.core.database.dao.TaskDependencyDao = database.taskDependencyDao()

    @Provides
    fun provideProjectInviteDao(database: KosmosDatabase): com.example.kosmos.core.database.dao.ProjectInviteDao = database.projectInviteDao()

    @Provides
    fun provideUserConnectionDao(database: KosmosDatabase): com.example.kosmos.core.database.dao.UserConnectionDao = database.userConnectionDao()

    @Provides
    fun provideProjectJoinRequestDao(database: KosmosDatabase): com.example.kosmos.core.database.dao.ProjectJoinRequestDao = database.projectJoinRequestDao()
}

// FirebaseModule removed - migrated to Supabase
// All Firebase services (Auth, Firestore, Storage, Messaging) replaced with Supabase equivalents

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return SupabaseConfig.client
    }

    @Provides
    @Singleton
    fun provideSupabaseProjectDataSource(supabase: SupabaseClient): SupabaseProjectDataSource {
        return SupabaseProjectDataSource(supabase)
    }

    @Provides
    @Singleton
    fun provideSupabaseProjectMemberDataSource(supabase: SupabaseClient): SupabaseProjectMemberDataSource {
        return SupabaseProjectMemberDataSource(supabase)
    }

    @Provides
    @Singleton
    fun provideSupabaseUserDataSource(supabase: SupabaseClient): com.example.kosmos.data.datasource.SupabaseUserDataSource {
        return com.example.kosmos.data.datasource.SupabaseUserDataSource(supabase)
    }

    @Provides
    @Singleton
    fun provideSupabaseMessageDataSource(supabase: SupabaseClient): com.example.kosmos.data.datasource.SupabaseMessageDataSource {
        return com.example.kosmos.data.datasource.SupabaseMessageDataSource(supabase)
    }

    @Provides
    @Singleton
    fun provideSupabaseTaskDataSource(supabase: SupabaseClient): com.example.kosmos.data.datasource.SupabaseTaskDataSource {
        return com.example.kosmos.data.datasource.SupabaseTaskDataSource(supabase)
    }

    @Provides
    @Singleton
    fun provideSupabaseChatDataSource(supabase: SupabaseClient): com.example.kosmos.data.datasource.SupabaseChatDataSource {
        return com.example.kosmos.data.datasource.SupabaseChatDataSource(supabase)
    }

    @Provides
    @Singleton
    fun provideSupabaseTaskActivityDataSource(supabase: SupabaseClient): com.example.kosmos.data.datasource.SupabaseTaskActivityDataSource {
        return com.example.kosmos.data.datasource.SupabaseTaskActivityDataSource(supabase)
    }

    @Provides
    @Singleton
    fun provideSupabaseProjectInviteDataSource(supabase: SupabaseClient): com.example.kosmos.data.datasource.SupabaseProjectInviteDataSource {
        return com.example.kosmos.data.datasource.SupabaseProjectInviteDataSource(supabase)
    }

    @Provides
    @Singleton
    fun provideSupabaseUserConnectionDataSource(supabase: SupabaseClient): com.example.kosmos.data.datasource.SupabaseUserConnectionDataSource {
        return com.example.kosmos.data.datasource.SupabaseUserConnectionDataSource(supabase)
    }

    @Provides
    @Singleton
    fun provideSupabaseProjectJoinRequestDataSource(supabase: SupabaseClient): com.example.kosmos.data.datasource.SupabaseProjectJoinRequestDataSource {
        return com.example.kosmos.data.datasource.SupabaseProjectJoinRequestDataSource(supabase)
    }

    @Provides
    @Singleton
    fun provideSupabaseRealtimeManager(
        supabase: SupabaseClient,
        messageDao: MessageDao,
        taskDao: TaskDao,
        taskActivityDao: com.example.kosmos.core.database.dao.TaskActivityDao,
        userConnectionDao: com.example.kosmos.core.database.dao.UserConnectionDao,
        projectInviteDao: com.example.kosmos.core.database.dao.ProjectInviteDao,
        projectMemberDao: ProjectMemberDao,
        projectDao: ProjectDao
    ): com.example.kosmos.data.realtime.SupabaseRealtimeManager {
        return com.example.kosmos.data.realtime.SupabaseRealtimeManager(supabase, messageDao, taskDao, taskActivityDao, userConnectionDao, projectInviteDao, projectMemberDao, projectDao)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .create()

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://speech.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    // Voice services disabled for MVP - will be re-enabled in Phase 5
    // @Provides
    // @Singleton
    // fun provideSpeechToTextService(retrofit: Retrofit): SpeechToTextService =
    //     retrofit.create(SpeechToTextService::class.java)

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitorImpl(context)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): android.content.SharedPreferences {
        return context.getSharedPreferences("kosmos_prefs", android.content.Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    @com.example.kosmos.data.sync.ApplicationScope
    fun provideApplicationScope(): kotlinx.coroutines.CoroutineScope {
        // P0-08 FIX: Application-scoped coroutine scope for SyncQueueManager
        // Survives configuration changes and activity lifecycle
        return kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        supabase: SupabaseClient,
        userDao: UserDao,
        sharedPreferences: android.content.SharedPreferences
    ): AuthRepository = AuthRepository(supabase, userDao, sharedPreferences)

    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        projectMemberDao: ProjectMemberDao,
        supabase: SupabaseClient,
        supabaseUserDataSource: com.example.kosmos.data.datasource.SupabaseUserDataSource,
        networkMonitor: NetworkMonitor,
        syncQueueDao: com.example.kosmos.core.database.dao.SyncQueueDao
    ): UserRepository = UserRepository(userDao, projectMemberDao, supabase, supabaseUserDataSource, networkMonitor, syncQueueDao)

    @Provides
    @Singleton
    fun provideChatRepository(
        chatRoomDao: ChatRoomDao,
        messageDao: MessageDao,
        projectDao: ProjectDao,
        supabase: SupabaseClient,
        supabaseMessageDataSource: com.example.kosmos.data.datasource.SupabaseMessageDataSource,
        supabaseChatDataSource: com.example.kosmos.data.datasource.SupabaseChatDataSource,
        realtimeManager: com.example.kosmos.data.realtime.SupabaseRealtimeManager,
        networkMonitor: NetworkMonitor,
        syncQueueDao: com.example.kosmos.core.database.dao.SyncQueueDao,
        fkRetryQueue: com.example.kosmos.data.sync.FKRetryQueue
    ): ChatRepository = ChatRepository(chatRoomDao, messageDao, projectDao, supabase, supabaseMessageDataSource, supabaseChatDataSource, realtimeManager, networkMonitor, syncQueueDao, fkRetryQueue)

    @Provides
    @Singleton
    fun provideProjectRepository(
        database: KosmosDatabase,  // BUG-011 FIX: Added for transaction support
        projectDao: ProjectDao,
        projectMemberDao: ProjectMemberDao,
        supabaseProjectDataSource: SupabaseProjectDataSource,
        supabaseProjectMemberDataSource: SupabaseProjectMemberDataSource,
        chatRoomDao: ChatRoomDao,
        taskDao: TaskDao,
        networkMonitor: NetworkMonitor,
        syncQueueDao: com.example.kosmos.core.database.dao.SyncQueueDao,
        dispatchers: DispatcherProvider,
        projectInviteDao: com.example.kosmos.core.database.dao.ProjectInviteDao,
        supabaseProjectInviteDataSource: com.example.kosmos.data.datasource.SupabaseProjectInviteDataSource,
        notificationService: com.example.kosmos.features.notifications.SupabaseNotificationService
    ): ProjectRepository = ProjectRepository(
        database,
        projectDao,
        projectMemberDao,
        supabaseProjectDataSource,
        supabaseProjectMemberDataSource,
        chatRoomDao,
        taskDao,
        networkMonitor,
        syncQueueDao,
        dispatchers,
        projectInviteDao,
        supabaseProjectInviteDataSource,
        notificationService
    )

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        projectDao: ProjectDao,
        projectMemberDao: ProjectMemberDao,
        userDao: UserDao,
        taskActivityDao: com.example.kosmos.core.database.dao.TaskActivityDao,
        supabaseTaskDataSource: com.example.kosmos.data.datasource.SupabaseTaskDataSource,
        supabaseTaskActivityDataSource: com.example.kosmos.data.datasource.SupabaseTaskActivityDataSource,
        notificationRulesEngine: com.example.kosmos.features.notifications.NotificationRulesEngine,
        reminderScheduler: com.example.kosmos.features.notifications.ReminderScheduler,
        networkMonitor: NetworkMonitor,
        syncQueueDao: com.example.kosmos.core.database.dao.SyncQueueDao,
        dispatchers: DispatcherProvider,
        fkRetryQueue: com.example.kosmos.data.sync.FKRetryQueue,
        timeEntryDao: com.example.kosmos.core.database.dao.TimeEntryDao,
        taskDependencyDao: com.example.kosmos.core.database.dao.TaskDependencyDao,
        supabaseTimeEntryDataSource: com.example.kosmos.data.datasource.SupabaseTimeEntryDataSource,
        supabaseDependencyDataSource: com.example.kosmos.data.datasource.SupabaseDependencyDataSource
    ): TaskRepository = TaskRepository(
        taskDao,
        projectDao,
        projectMemberDao,
        userDao,
        taskActivityDao,
        supabaseTaskDataSource,
        supabaseTaskActivityDataSource,
        notificationRulesEngine,
        reminderScheduler,
        networkMonitor,
        syncQueueDao,
        dispatchers,
        fkRetryQueue,
        timeEntryDao,
        taskDependencyDao,
        supabaseTimeEntryDataSource,
        supabaseDependencyDataSource
    )

    @Provides
    @Singleton
    fun provideVoiceRepository(
        voiceMessageDao: VoiceMessageDao
    ): VoiceRepository = VoiceRepository(voiceMessageDao)

    @Provides
    @Singleton
    fun provideProjectInviteRepository(
        inviteDao: com.example.kosmos.core.database.dao.ProjectInviteDao,
        projectMemberDao: ProjectMemberDao,
        supabaseDataSource: com.example.kosmos.data.datasource.SupabaseProjectInviteDataSource,
        projectRepository: ProjectRepository,
        notificationService: com.example.kosmos.features.notifications.SupabaseNotificationService,
        networkMonitor: NetworkMonitor,
        syncQueueDao: com.example.kosmos.core.database.dao.SyncQueueDao
    ): com.example.kosmos.data.repository.ProjectInviteRepository = com.example.kosmos.data.repository.ProjectInviteRepository(
        inviteDao, projectMemberDao, supabaseDataSource, projectRepository, notificationService, networkMonitor, syncQueueDao
    )

    @Provides
    @Singleton
    fun provideUserConnectionRepository(
        connectionDao: com.example.kosmos.core.database.dao.UserConnectionDao,
        supabaseDataSource: com.example.kosmos.data.datasource.SupabaseUserConnectionDataSource,
        notificationService: com.example.kosmos.features.notifications.SupabaseNotificationService,
        networkMonitor: NetworkMonitor,
        syncQueueDao: com.example.kosmos.core.database.dao.SyncQueueDao
    ): com.example.kosmos.data.repository.UserConnectionRepository = com.example.kosmos.data.repository.UserConnectionRepository(
        connectionDao, supabaseDataSource, notificationService, networkMonitor, syncQueueDao
    )

    @Provides
    @Singleton
    fun provideAppConfigRepository(
        supabase: SupabaseClient,
        sharedPreferences: android.content.SharedPreferences,
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): AppConfigRepository = AppConfigRepository(supabase, sharedPreferences, context)

    @Provides
    @Singleton
    fun provideAnnouncementRepository(
        supabase: SupabaseClient
    ): AnnouncementRepository = AnnouncementRepository(supabase)

    @Provides
    @Singleton
    fun provideProjectJoinRequestRepository(
        joinRequestDao: com.example.kosmos.core.database.dao.ProjectJoinRequestDao,
        projectMemberDao: ProjectMemberDao,
        supabaseDataSource: com.example.kosmos.data.datasource.SupabaseProjectJoinRequestDataSource,
        projectRepository: ProjectRepository,
        notificationService: com.example.kosmos.features.notifications.SupabaseNotificationService,
        networkMonitor: NetworkMonitor,
        syncQueueDao: com.example.kosmos.core.database.dao.SyncQueueDao
    ): com.example.kosmos.data.repository.ProjectJoinRequestRepository = com.example.kosmos.data.repository.ProjectJoinRequestRepository(
        joinRequestDao, projectMemberDao, supabaseDataSource, projectRepository, notificationService, networkMonitor, syncQueueDao
    )
}

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    // Voice services disabled for MVP - will be re-enabled in Phase 5
    // @Provides
    // @Singleton
    // fun provideTranscriptionService(
    //     speechToTextService: SpeechToTextService,
    //     voiceMessageDao: VoiceMessageDao,
    //     @ApplicationContext context: Context
    // ): TranscriptionService = TranscriptionService(speechToTextService, voiceMessageDao, context)

    @Provides
    @Singleton
    fun provideActionDetectionService(
        actionItemDao: ActionItemDao
    ): ActionDetectionService = ActionDetectionService(actionItemDao)

    @Provides
    @Singleton
    fun provideSmartReplyService(): SmartReplyService = SmartReplyService()
}

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideSupabaseNotificationService(
        supabase: SupabaseClient
    ): com.example.kosmos.features.notifications.SupabaseNotificationService {
        return com.example.kosmos.features.notifications.SupabaseNotificationService(supabase)
    }

    @Provides
    @Singleton
    fun provideNotificationRulesEngine(
        userDao: UserDao,
        supabaseNotificationService: com.example.kosmos.features.notifications.SupabaseNotificationService
    ): com.example.kosmos.features.notifications.NotificationRulesEngine {
        return com.example.kosmos.features.notifications.NotificationRulesEngine(
            userDao,
            supabaseNotificationService
        )
    }

    @Provides
    @Singleton
    fun provideReminderScheduler(
        @ApplicationContext context: Context
    ): com.example.kosmos.features.notifications.ReminderScheduler {
        return com.example.kosmos.features.notifications.ReminderScheduler(context)
    }

    @Provides
    @Singleton
    fun provideNotificationListener(
        @ApplicationContext context: Context,
        supabase: SupabaseClient
    ): com.example.kosmos.features.notifications.NotificationListener {
        return com.example.kosmos.features.notifications.NotificationListener(context, supabase)
    }
}

// P0-08 FIX: Sync Module for offline-first sync queue
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideSyncQueueManager(
        syncQueueDao: com.example.kosmos.core.database.dao.SyncQueueDao,
        taskDao: com.example.kosmos.core.database.dao.TaskDao,
        networkMonitor: com.example.kosmos.shared.utils.NetworkMonitor,
        supabaseTaskDataSource: com.example.kosmos.data.datasource.SupabaseTaskDataSource,
        supabaseTaskActivityDataSource: com.example.kosmos.data.datasource.SupabaseTaskActivityDataSource,
        supabaseProjectDataSource: SupabaseProjectDataSource,
        supabaseProjectMemberDataSource: SupabaseProjectMemberDataSource,
        supabaseMessageDataSource: com.example.kosmos.data.datasource.SupabaseMessageDataSource,
        supabaseChatDataSource: com.example.kosmos.data.datasource.SupabaseChatDataSource,
        supabaseUserDataSource: com.example.kosmos.data.datasource.SupabaseUserDataSource,
        supabaseProjectInviteDataSource: com.example.kosmos.data.datasource.SupabaseProjectInviteDataSource,
        supabaseUserConnectionDataSource: com.example.kosmos.data.datasource.SupabaseUserConnectionDataSource,
        supabaseProjectJoinRequestDataSource: com.example.kosmos.data.datasource.SupabaseProjectJoinRequestDataSource,
        supabaseTimeEntryDataSource: com.example.kosmos.data.datasource.SupabaseTimeEntryDataSource,
        supabaseDependencyDataSource: com.example.kosmos.data.datasource.SupabaseDependencyDataSource,
        supabaseMilestoneDataSource: com.example.kosmos.data.datasource.SupabaseMilestoneDataSource,
        @com.example.kosmos.data.sync.ApplicationScope scope: kotlinx.coroutines.CoroutineScope
    ): com.example.kosmos.data.sync.SyncQueueManager {
        return com.example.kosmos.data.sync.SyncQueueManager(
            syncQueueDao = syncQueueDao,
            taskDao = taskDao,
            networkMonitor = networkMonitor,
            supabaseTaskDataSource = supabaseTaskDataSource,
            supabaseTaskActivityDataSource = supabaseTaskActivityDataSource,
            supabaseProjectDataSource = supabaseProjectDataSource,
            supabaseProjectMemberDataSource = supabaseProjectMemberDataSource,
            supabaseMessageDataSource = supabaseMessageDataSource,
            supabaseChatDataSource = supabaseChatDataSource,
            supabaseUserDataSource = supabaseUserDataSource,
            supabaseProjectInviteDataSource = supabaseProjectInviteDataSource,
            supabaseUserConnectionDataSource = supabaseUserConnectionDataSource,
            supabaseProjectJoinRequestDataSource = supabaseProjectJoinRequestDataSource,
            supabaseTimeEntryDataSource = supabaseTimeEntryDataSource,
            supabaseDependencyDataSource = supabaseDependencyDataSource,
            supabaseMilestoneDataSource = supabaseMilestoneDataSource,
            scope = scope
        )
    }
}



/**
 * P1-12: Dispatcher Provider Module
 * Provides proper threading for coroutines
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DispatcherModule {
    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(
        impl: DefaultDispatcherProvider
    ): DispatcherProvider
}

// === UserFeedbackManager EntryPoint (for KosmosApp composable) ===
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface UserFeedbackEntryPoint {
    fun userFeedbackManager(): com.example.kosmos.core.feedback.UserFeedbackManager
}
