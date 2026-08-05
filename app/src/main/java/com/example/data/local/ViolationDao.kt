package com.example.data.local

import androidx.room.*
import com.example.data.model.Violation
import kotlinx.coroutines.flow.Flow

@Dao
interface ViolationDao {
    @Query("SELECT * FROM violations ORDER BY timestamp DESC")
    fun getAllViolations(): Flow<List<Violation>>

    @Query("SELECT * FROM violations WHERE reporterCccd = :reporterCccd ORDER BY timestamp DESC")
    fun getViolationsByReporter(reporterCccd: String): Flow<List<Violation>>

    @Query("SELECT * FROM violations WHERE id = :id LIMIT 1")
    suspend fun getViolationById(id: Int): Violation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViolation(violation: Violation)

    @Update
    suspend fun updateViolation(violation: Violation)

    @Delete
    suspend fun deleteViolation(violation: Violation)
}
