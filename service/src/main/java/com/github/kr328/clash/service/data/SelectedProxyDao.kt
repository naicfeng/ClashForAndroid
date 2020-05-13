package com.github.kr328.clash.service.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SelectedProxyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSelectedForProfile(item: SelectedProxyEntity)

    @Query("SELECT * FROM selected_proxies WHERE profile_id = :id")
    suspend fun querySelectedForProfile(id: Long): List<SelectedProxyEntity>

    @Query("DELETE FROM selected_proxies WHERE profile_id = :id AND proxy in (:selected)")
    suspend fun removeSelectedForProfile(id: Long, selected: List<String>)

    companion object : SelectedProxyDao by Database.database.openSelectedProxyDao()
}