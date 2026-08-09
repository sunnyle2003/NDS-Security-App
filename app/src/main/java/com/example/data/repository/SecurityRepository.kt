package com.example.data.repository

import com.example.data.local.ProposalDao
import com.example.data.local.UserDao
import com.example.data.local.ViolationDao
import com.example.data.model.Proposal
import com.example.data.model.User
import com.example.data.model.Violation
import com.example.data.remote.FirebaseSyncService
import com.example.data.remote.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class SecurityRepository(
    private val userDao: UserDao,
    private val proposalDao: ProposalDao,
    private val violationDao: ViolationDao,
    val syncService: FirebaseSyncService? = null
) {
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val allProposals: Flow<List<Proposal>> = proposalDao.getAllProposals()
    val allViolations: Flow<List<Violation>> = violationDao.getAllViolations()

    val syncState: StateFlow<SyncState>? = syncService?.syncState
    val syncStatusMessage: StateFlow<String>? = syncService?.syncStatusMessage
    val lastSyncTime: StateFlow<Long>? = syncService?.lastSyncTime
    val firebaseUrl: StateFlow<String>? = syncService?.firebaseUrl
    val testResult = syncService?.testResult

    suspend fun triggerSync(): Boolean {
        return syncService?.syncAll(userDao, proposalDao, violationDao) ?: false
    }

    suspend fun updateFirebaseUrl(newUrl: String) {
        syncService?.updateFirebaseUrl(newUrl)
        triggerSync()
    }

    suspend fun testFirebaseConnection(url: String) = syncService?.testConnection(url)

    fun getViolationsByReporter(reporterCccd: String): Flow<List<Violation>> {
        return violationDao.getViolationsByReporter(reporterCccd)
    }

    suspend fun getViolationById(id: Int): Violation? {
        return violationDao.getViolationById(id)
    }

    suspend fun insertViolation(violation: Violation) {
        violationDao.insertViolation(violation)
        syncService?.pushViolationDirect(violation)
    }

    suspend fun updateViolation(violation: Violation) {
        violationDao.updateViolation(violation)
        syncService?.pushViolationDirect(violation)
    }

    suspend fun deleteViolation(violation: Violation) {
        violationDao.deleteViolation(violation)
        syncService?.deleteViolationDirect(violation)
    }

    fun getProposalsByProposer(proposerCccd: String): Flow<List<Proposal>> {
        return proposalDao.getProposalsByProposer(proposerCccd)
    }

    suspend fun getUserByCccd(cccd: String): User? {
        return userDao.getUserByCccd(cccd)
    }

    suspend fun fetchUserDirect(cccd: String): User? {
        val cloudUser = syncService?.fetchUserDirect(cccd)
        if (cloudUser != null && cloudUser.cccd.isNotBlank()) {
            userDao.insertUser(cloudUser)
            return cloudUser
        }
        return null
    }

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
        syncService?.pushUserDirect(user)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
        syncService?.pushUserDirect(user)
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
        syncService?.deleteUserDirect(user.cccd)
    }

    suspend fun insertProposal(proposal: Proposal) {
        proposalDao.insertProposal(proposal)
        syncService?.pushProposalDirect(proposal)
    }

    suspend fun updateProposal(proposal: Proposal) {
        proposalDao.updateProposal(proposal)
        syncService?.pushProposalDirect(proposal)
    }

    suspend fun deleteProposal(proposal: Proposal) {
        proposalDao.deleteProposal(proposal)
        syncService?.deleteProposalDirect(proposal)
    }

    suspend fun getProposalById(id: Int): Proposal? {
        return proposalDao.getProposalById(id)
    }

    suspend fun seedDefaultsIfNeeded() {
        if (userDao.getUserByCccd("087095015873") == null) {
            val primaryAdmin = User(
                cccd = "087095015873",
                fullName = "Lê Duy Tèo (Quản trị viên)",
                role = "ADMIN",
                password = "2",
                assignedLocation = "Trụ sở chính Ngày & Đêm"
            )
            userDao.insertUser(primaryAdmin)
            syncService?.pushUserDirect(primaryAdmin)
        }
        if (userDao.getUserByCccd("000000000000") == null) {
            val adminUser = User(
                cccd = "000000000000",
                fullName = "Quản trị viên Hệ thống",
                role = "ADMIN",
                password = "admin123",
                assignedLocation = "Trụ sở chính"
            )
            userDao.insertUser(adminUser)
            syncService?.pushUserDirect(adminUser)
        }
        if (userDao.getUserByCccd("111111111111") == null) {
            val captainUser = User(
                cccd = "111111111111",
                fullName = "Đội trưởng Nguyễn Văn A",
                role = "CAPTAIN",
                password = "nds123",
                assignedLocation = "Ngày & Đêm Security"
            )
            userDao.insertUser(captainUser)
            syncService?.pushUserDirect(captainUser)
        }
        if (userDao.getUserByCccd("222222222222") == null) {
            val officerUser = User(
                cccd = "222222222222",
                fullName = "Cán bộ Nghiệp vụ Lê Văn B",
                role = "OFFICER",
                password = "nds456",
                assignedLocation = "Phòng Nghiệp vụ"
            )
            userDao.insertUser(officerUser)
            syncService?.pushUserDirect(officerUser)
        }
        if (userDao.getUserByCccd("333333333333") == null) {
            val disciplineUser = User(
                cccd = "333333333333",
                fullName = "Cán bộ Điều lệnh Trần Văn C",
                role = "DISCIPLINE",
                password = "nds789",
                assignedLocation = "Phòng Điều lệnh"
            )
            userDao.insertUser(disciplineUser)
            syncService?.pushUserDirect(disciplineUser)
        }
    }
}
