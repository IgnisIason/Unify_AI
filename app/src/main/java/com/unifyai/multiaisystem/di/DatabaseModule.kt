package com.unifyai.multiaisystem.di

import android.content.Context
import com.unifyai.multiaisystem.data.database.AppDatabase
import com.unifyai.multiaisystem.data.database.AISystemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideAISystemDao(database: AppDatabase): AISystemDao {
        return database.aiSystemDao()
    }
}