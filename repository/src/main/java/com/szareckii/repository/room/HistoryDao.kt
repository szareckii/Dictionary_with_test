package com.szareckii.repository.room

import androidx.room.*

// Построение методов должно быть вам знакомо. Единственное, чему стоит
// уделить внимание - ключевое слово suspend, которое намекает, что все
// запросы в БД будут асинхронными (корутины поддерживаются в Room изначально)
@Dao
interface HistoryDao {
    // Получить весь список слов
    @Query("SELECT * FROM HistoryEntity")
    suspend fun all(): List<HistoryEntity>
    // Получить конкретное слово
    @Query("SELECT * FROM HistoryEntity WHERE word LIKE :word LIMIT 1")
    suspend fun getDataByWord(word: String): HistoryEntity
    // Сохранить новое слово
    // onConflict = OnConflictStrategy.IGNORE означает, что дубликаты не будут
    // сохраняться
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: HistoryEntity)
    // Вставить список слов
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<HistoryEntity>)
    // Обновить слово
    @Update
    suspend fun update(entity: HistoryEntity)
    // Удалить слово
    @Delete
    suspend fun delete(entity: HistoryEntity)
}