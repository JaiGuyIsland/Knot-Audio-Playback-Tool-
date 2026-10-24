package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AudioTrack
import com.example.data.model.Playlist
import com.example.data.model.PlaylistItem

@Database(
    entities = [
        AudioTrack::class,
        Playlist::class,
        PlaylistItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KnotDatabase : RoomDatabase() {
    abstract fun audioTrackDao(): AudioTrackDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: KnotDatabase? = null

        fun getInstance(context: Context): KnotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KnotDatabase::class.java,
                    "knot_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
