/**
 * FirebaseModule.kt
 *
 * Hilt 依赖注入模块 - 提供 Firebase 相关的单例实例
 * Hilt Dependency Injection Module - Provides Firebase singleton instances
 *
 * @author CS501 Team
 */
package com.example.cs501_micro_chat.di

import android.app.Application
import com.example.cs501_micro_chat.data.remote.FirebaseDataSource
import com.example.cs501_micro_chat.data.repository.AuthRepository
import com.example.cs501_micro_chat.data.repository.ChatRepository
import com.example.cs501_micro_chat.data.repository.FirebaseProfileRepository
import com.example.cs501_micro_chat.data.repository.FirebaseAuthRepository
import com.example.cs501_micro_chat.data.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * 提供 Firebase Authentication 实例
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    /**
     * 提供 Firebase Firestore 实例
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance().apply {
            // 启用离线持久化
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        }
    }

    /**
     * 提供 Firebase Storage 实例
     */
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        storage: FirebaseStorage,
        application: Application
    ): ProfileRepository {
        return FirebaseProfileRepository(auth, firestore, storage, application)
    }

    /**
     * 提供 AuthRepository 实例
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository {
        return FirebaseAuthRepository(auth, firestore)
    }

    /**
     * 提供 FirebaseDataSource 实例
     */
    @Provides
    @Singleton
    fun provideFirebaseDataSource(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): FirebaseDataSource {
        return FirebaseDataSource(firestore, auth)
    }

    /**
     * 提供 ChatRepository 实例
     */
    @Provides
    @Singleton
    fun provideChatRepository(
        firebaseDataSource: FirebaseDataSource,
        auth: FirebaseAuth
    ): ChatRepository {
        return ChatRepository(firebaseDataSource, auth)
    }
}
