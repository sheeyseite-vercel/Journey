package com.innerpage

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val body: String, val mood: String, val tag: String, val createdAt: Long,
    val image: String = "", val audio: String = ""
)

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY createdAt DESC") fun all(): Flow<List<Entry>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(e: Entry)
    @Delete suspend fun delete(e: Entry)
}

@Database(entities = [Entry::class], version = 1, exportSchema = false)
abstract class Db : RoomDatabase() {
    abstract fun dao(): EntryDao
    companion object {
        @Volatile private var i: Db? = null
        fun get(c: Context): Db = i ?: synchronized(this) {
            i ?: Room.databaseBuilder(c.applicationContext, Db::class.java, "innerpage.db").build().also { i = it }
        }
    }
}
