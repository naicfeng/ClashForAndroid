package com.github.kr328.clash.service.data

import androidx.room.*

@Dao
interface ProfileDao {
    @Query("UPDATE profiles SET active = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setActive(id: Long)

    @Query("SELECT * FROM profiles WHERE active = 1 LIMIT 1")
    suspend fun queryActive(): ProfileEntity?

    @Query("SELECT * FROM profiles")
    suspend fun queryAll(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun queryById(id: Long): ProfileEntity?

    @Query("SELECT id FROM profiles")
    suspend fun queryAllIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: ProfileEntity): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun remove(id: Long)

    companion object : ProfileDao by Database.database.openProfileDao()
}