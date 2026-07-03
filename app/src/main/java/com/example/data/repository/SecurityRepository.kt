package com.example.data.repository

import com.example.data.local.ProposalDao
import com.example.data.local.UserDao
import com.example.data.model.Proposal
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

class SecurityRepository(
    private val userDao: UserDao,
    private val proposalDao: ProposalDao
) {
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val allProposals: Flow<List<Proposal>> = proposalDao.getAllProposals()

    fun getProposalsByProposer(proposerCccd: String): Flow<List<Proposal>> {
        return proposalDao.getProposalsByProposer(proposerCccd)
    }

    suspend fun getUserByCccd(cccd: String): User? {
        return userDao.getUserByCccd(cccd)
    }

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }

    suspend fun insertProposal(proposal: Proposal) {
        proposalDao.insertProposal(proposal)
    }

    suspend fun updateProposal(proposal: Proposal) {
        proposalDao.updateProposal(proposal)
    }

    suspend fun deleteProposal(proposal: Proposal) {
        proposalDao.deleteProposal(proposal)
    }

    suspend fun getProposalById(id: Int): Proposal? {
        return proposalDao.getProposalById(id)
    }
}
