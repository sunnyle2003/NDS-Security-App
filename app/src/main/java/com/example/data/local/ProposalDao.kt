package com.example.data.local

import androidx.room.*
import com.example.data.model.Proposal
import kotlinx.coroutines.flow.Flow

@Dao
interface ProposalDao {
    @Query("SELECT * FROM proposals ORDER BY timestamp DESC")
    fun getAllProposals(): Flow<List<Proposal>>

    @Query("SELECT * FROM proposals WHERE proposerCccd = :proposerCccd ORDER BY timestamp DESC")
    fun getProposalsByProposer(proposerCccd: String): Flow<List<Proposal>>

    @Query("SELECT * FROM proposals WHERE id = :id LIMIT 1")
    suspend fun getProposalById(id: Int): Proposal?

    @Query("SELECT * FROM proposals WHERE proposerCccd = :proposerCccd AND timestamp = :timestamp LIMIT 1")
    suspend fun getProposalByProposerAndTimestamp(proposerCccd: String, timestamp: Long): Proposal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProposal(proposal: Proposal)

    @Update
    suspend fun updateProposal(proposal: Proposal)

    @Delete
    suspend fun deleteProposal(proposal: Proposal)
}
