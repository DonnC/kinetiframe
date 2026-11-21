// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Singleton holder, mirroring the standard Room "one instance per process" pattern. Every table
 * here is a rebuildable index cache over its own repository's real on-disk files, never the
 * source of truth -- a stale or missing file just means the row should be dropped and
 * re-derived, not that anything is corrupted. That's why [fallbackToDestructiveMigration] stays
 * the deliberately simple choice: there is no data in this database worth writing a real
 * migration to preserve, only a warm cache that rebuilds itself from disk the next time it's
 * found empty.
 */
@Database(
    entities = [
        ProjectEntity::class, ExportEntity::class,
        ActorEntity::class, PerformanceEntity::class, LibraryItemEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun exportDao(): ExportDao
    abstract fun actorDao(): ActorDao
    abstract fun performanceDao(): PerformanceDao
    abstract fun libraryItemDao(): LibraryItemDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "kinetiframe.db")
                .fallbackToDestructiveMigration(true)
                .build()
                .also { instance = it }
        }
    }
}
