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

import dagger.Module
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
        // Allow destructive migration for development
        // TODO: Implement proper migrations for production
        .fallbackToDestructiveMigration()
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
    fun provideSupabaseRealtimeManager(
        supabase: SupabaseClient,
        messageDao: MessageDao
    ): com.example.kosmos.data.realtime.SupabaseRealtimeManager {
        return com.example.kosmos.data.realtime.SupabaseRealtimeManager(supabase, messageDao)
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
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        supabase: SupabaseClient,
        userDao: UserDao
    ): AuthRepository = AuthRepository(supabase, userDao)

    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        supabase: SupabaseClient,
        supabaseUserDataSource: com.example.kosmos.data.datasource.SupabaseUserDataSource
    ): UserRepository = UserRepository(userDao, supabase, supabaseUserDataSource)

    @Provides
    @Singleton
    fun provideChatRepository(
        chatRoomDao: ChatRoomDao,
        messageDao: MessageDao,
        projectDao: ProjectDao,
        supabase: SupabaseClient,
        supabaseMessageDataSource: com.example.kosmos.data.datasource.SupabaseMessageDataSource,
        supabaseChatDataSource: com.example.kosmos.data.datasource.SupabaseChatDataSource,
        realtimeManager: com.example.kosmos.data.realtime.SupabaseRealtimeManager
    ): ChatRepository = ChatRepository(chatRoomDao, messageDao, projectDao, supabase, supabaseMessageDataSource, supabaseChatDataSource, realtimeManager)

    @Provides
    @Singleton
    fun provideProjectRepository(
        projectDao: ProjectDao,
        projectMemberDao: ProjectMemberDao,
        supabaseProjectDataSource: SupabaseProjectDataSource,
        supabaseProjectMemberDataSource: SupabaseProjectMemberDataSource,
        chatRoomDao: ChatRoomDao,
        taskDao: TaskDao
    ): ProjectRepository = ProjectRepository(
        projectDao,
        projectMemberDao,
        supabaseProjectDataSource,
        supabaseProjectMemberDataSource,
        chatRoomDao,
        taskDao
    )

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        projectDao: ProjectDao,
        projectMemberDao: ProjectMemberDao,
        supabaseTaskDataSource: com.example.kosmos.data.datasource.SupabaseTaskDataSource
    ): TaskRepository = TaskRepository(taskDao, projectDao, projectMemberDao, supabaseTaskDataSource)

    @Provides
    @Singleton
    fun provideVoiceRepository(
        voiceMessageDao: VoiceMessageDao
    ): VoiceRepository = VoiceRepository(voiceMessageDao)
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


