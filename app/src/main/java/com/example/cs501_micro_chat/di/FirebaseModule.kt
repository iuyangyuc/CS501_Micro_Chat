/**
 * FirebaseModule.kt
 *
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
import com.example.cs501_micro_chat.data.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
     * Provides Firebase Authentication instance
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    /**
     * Provides Firebase Firestore instance
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    /**
     * Provides Firebase Storage instance
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
     * Provides AuthRepository instance
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
     * Provides FirebaseDataSource instance
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
     * Provides ChatRepository instance
     */
    @Provides
    @Singleton
    fun provideChatRepository(
        firebaseDataSource: FirebaseDataSource,
        auth: FirebaseAuth
    ): ChatRepository {
        return ChatRepository(firebaseDataSource, auth)
    }

    /**
     * Provides StorageRepository instance
     */
    @Provides
    @Singleton
    fun provideStorageRepository(
        storage: FirebaseStorage
    ): StorageRepository {
        return StorageRepository(storage)
    }
}
