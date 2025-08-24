package com.example.kosmos

import android.content.Context
import androidx.room.Room
import com.example.kosmos.database.ActionItemDao
import com.example.kosmos.database.ChatRoomDao
import com.example.kosmos.database.KosmosDatabase
import com.example.kosmos.database.MessageDao
import com.example.kosmos.database.TaskDao
import com.example.kosmos.database.UserDao
import com.example.kosmos.database.VoiceMessageDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.GsonBuilder

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
        ).build()
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
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
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

    @Provides
    @Singleton
    fun provideSpeechToTextService(retrofit: Retrofit): SpeechToTextService =
        retrofit.create(SpeechToTextService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository = AuthRepository(firebaseAuth, firestore)

    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        firestore: FirebaseFirestore
    ): UserRepository = UserRepository(userDao, firestore)

    @Provides
    @Singleton
    fun provideChatRepository(
        chatRoomDao: ChatRoomDao,
        messageDao: MessageDao,
        firestore: FirebaseFirestore
    ): ChatRepository = ChatRepository(chatRoomDao, messageDao, firestore)

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        firestore: FirebaseFirestore
    ): TaskRepository = TaskRepository(taskDao, firestore)

    @Provides
    @Singleton
    fun provideVoiceRepository(
        voiceMessageDao: VoiceMessageDao,
        firebaseStorage: FirebaseStorage,
        @ApplicationContext context: Context
    ): VoiceRepository = VoiceRepository(voiceMessageDao, firebaseStorage, context)
}

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideTranscriptionService(
        speechToTextService: SpeechToTextService,
        voiceMessageDao: VoiceMessageDao,
        @ApplicationContext context: Context
    ): TranscriptionService = TranscriptionService(speechToTextService, voiceMessageDao, context)

    @Provides
    @Singleton
    fun provideActionDetectionService(
        actionItemDao: ActionItemDao
    ): ActionDetectionService = ActionDetectionService(actionItemDao)

    @Provides
    @Singleton
    fun provideSmartReplyService(): SmartReplyService = SmartReplyService()
}


