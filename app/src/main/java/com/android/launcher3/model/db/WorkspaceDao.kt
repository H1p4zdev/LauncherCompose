package com.android.launcher3.model.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspace_items WHERE screenId >= 0 ORDER BY screenId, cellY, cellX")
    fun getWorkspaceItems(): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_items WHERE screenId = -1 ORDER BY cellX")
    fun getHotseatItems(): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_items WHERE itemType = 'folder'")
    fun getFolderItems(): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_items WHERE folderId = :folderId")
    fun getItemsInFolder(folderId: Long): Flow<List<WorkspaceItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WorkspaceItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<WorkspaceItemEntity>)

    @Query("DELETE FROM workspace_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("DELETE FROM workspace_items")
    suspend fun deleteAll()

    @Query("UPDATE workspace_items SET screenId = :screenId, cellX = :cellX, cellY = :cellY WHERE id = :itemId")
    suspend fun updatePosition(itemId: Long, screenId: Long, cellX: Int, cellY: Int)

    @Query("UPDATE workspace_items SET folderId = :folderId WHERE id = :itemId")
    suspend fun moveToFolder(itemId: Long, folderId: Long)

    @Transaction
    suspend fun replaceAll(items: List<WorkspaceItemEntity>) {
        deleteAll()
        insertItems(items)
    }
}
