package com.example.data.repository

import com.example.data.local.ProposalDao
import com.example.data.local.UserDao
import com.example.data.local.ViolationDao
import com.example.data.model.Proposal
import com.example.data.model.User
import com.example.data.model.Violation
import kotlinx.coroutines.flow.Flow

class SecurityRepository(
    private val userDao: UserDao,
    private val proposalDao: ProposalDao,
    private val violationDao: ViolationDao
) {
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val allProposals: Flow<List<Proposal>> = proposalDao.getAllProposals()
    val allViolations: Flow<List<Violation>> = violationDao.getAllViolations()

    fun getViolationsByReporter(reporterCccd: String): Flow<List<Violation>> {
        return violationDao.getViolationsByReporter(reporterCccd)
    }

    suspend fun getViolationById(id: Int): Violation? {
        return violationDao.getViolationById(id)
    }

    suspend fun insertViolation(violation: Violation) {
        violationDao.insertViolation(violation)
    }

    suspend fun updateViolation(violation: Violation) {
        violationDao.updateViolation(violation)
    }

    suspend fun deleteViolation(violation: Violation) {
        violationDao.deleteViolation(violation)
    }

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
