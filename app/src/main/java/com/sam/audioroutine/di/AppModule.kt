package com.sam.audioroutine.di

import android.content.Context
import androidx.room.Room
import com.sam.audioroutine.data.db.AudioRoutineDatabase
import com.sam.audioroutine.data.db.RoutineDao
import com.sam.audioroutine.data.repo.AlarmStateRepositoryImpl
import com.sam.audioroutine.data.repo.AppBackgroundRepositoryImpl
import com.sam.audioroutine.data.repo.AssetRoutineSeedSource
import com.sam.audioroutine.data.repo.RoutineRepositoryImpl
import com.sam.audioroutine.domain.repo.AlarmStateRepository
import com.sam.audioroutine.domain.repo.AppBackgroundRepository
import com.sam.audioroutine.domain.repo.RoutineRepository
import com.sam.audioroutine.domain.repo.RoutineSeedSource
import com.sam.audioroutine.feature.player.music.BlockMusicResolver
import com.sam.audioroutine.feature.player.music.BlockMusicResolverImpl
import com.sam.audioroutine.feature.player.music.DuckingMusicPromptPolicy
import com.sam.audioroutine.feature.player.music.FreeCatalogMusicProvider
import com.sam.audioroutine.feature.player.music.LocalFileMusicProvider
import com.sam.audioroutine.feature.player.music.MusicProvider
import com.sam.audioroutine.feature.player.music.MusicPromptPolicy
import com.sam.audioroutine.feature.player.music.SpotifyMusicProvider
import com.sam.audioroutine.feature.schedule.AlarmScheduler
import com.sam.audioroutine.feature.schedule.RoutineScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AudioRoutineDatabase {
        return Room.databaseBuilder(
            context,
            AudioRoutineDatabase::class.java,
            "audio_routine.db"
        ).addMigrations(
            AudioRoutineDatabase.MIGRATION_1_2,
            AudioRoutineDatabase.MIGRATION_2_3,
            AudioRoutineDatabase.MIGRATION_3_4,
            AudioRoutineDatabase.MIGRATION_4_5
        )
            .build()
    }

    @Provides
    fun provideRoutineDao(database: AudioRoutineDatabase): RoutineDao = database.routineDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository

    @Binds
    abstract fun bindRoutineSeedSource(impl: AssetRoutineSeedSource): RoutineSeedSource

    @Binds
    abstract fun bindAlarmStateRepository(impl: AlarmStateRepositoryImpl): AlarmStateRepository

    @Binds
    abstract fun bindAppBackgroundRepository(impl: AppBackgroundRepositoryImpl): AppBackgroundRepository
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerModule {
    @Binds
    abstract fun bindAlarmScheduler(impl: RoutineScheduler): AlarmScheduler
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MusicModule {
    @Binds
    abstract fun bindBlockMusicResolver(impl: BlockMusicResolverImpl): BlockMusicResolver

    @Binds
    abstract fun bindMusicPromptPolicy(impl: DuckingMusicPromptPolicy): MusicPromptPolicy

    @Binds
    @IntoSet
    abstract fun bindFreeCatalogMusicProvider(impl: FreeCatalogMusicProvider): MusicProvider

    @Binds
    @IntoSet
    abstract fun bindLocalFileMusicProvider(impl: LocalFileMusicProvider): MusicProvider

    @Binds
    @IntoSet
    abstract fun bindSpotifyMusicProvider(impl: SpotifyMusicProvider): MusicProvider
}

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
