package com.example.coffeetimer.data

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Entity(tableName = "recipes")
data class Recipe (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Int, // 自動生成されたIDを後から設定するため
    @ColumnInfo(name = "beans_name") val beansName: String?,
    @ColumnInfo(name = "roast_level") val roastLevel: String?,
    @ColumnInfo(name = "grind") val grind: Int?,
    @ColumnInfo(name = "temperature") val temperature: Int?,
    @ColumnInfo(name = "memo") val memo: String?,
    @ColumnInfo(name = "modified_datetime") val modifiedDateTime: Instant?
)

// SQLiteに日時型は無いので、String型で保存する
object DateTimeConverter {
    @SuppressLint("NewApi")
    @TypeConverter
    fun stringToInstant(value: String): Instant =
        formatter.parse(value, Instant::from)

    @SuppressLint("NewApi")
    @TypeConverter
    fun instantToString(value: Instant): String =
        formatter.format(value)

    @SuppressLint("NewApi")
    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)
}

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes")
    suspend fun selectAll(): List<Recipe>

    // 付与されたRowIdを返す
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(recipe: Recipe) : Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun delete(id: Int)
}

@Database(entities = [Recipe::class], version = 1, exportSchema = true)
@TypeConverters(DateTimeConverter::class)
abstract class RecipeDatabase : RoomDatabase() { // アプリ内でインスタンスが1つしか存在しないシングルトンクラス

    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile
        private var Instance: RecipeDatabase? = null

        // このクラスのインスタンスがnullの時のみDBがビルドされる
        fun getDatabase(context: Context): RecipeDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, RecipeDatabase::class.java, "recipe_database")
                    .build()
                    .also { Instance = it }
            }
        }
    }
}