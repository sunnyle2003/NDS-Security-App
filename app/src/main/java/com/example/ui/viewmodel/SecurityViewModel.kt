package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SecurityDatabase
import com.example.data.model.Proposal
import com.example.data.model.User
import com.example.data.repository.SecurityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SecurityViewModel(application: Application) : AndroidViewModel(application) {
    private val database = SecurityDatabase.getDatabase(application, viewModelScope)
    private val repository = SecurityRepository(database.userDao(), database.proposalDao())

    // Authentication States
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Registration-Free Setup States
    private val _pendingRegistrationCccd = MutableStateFlow<String?>(null)
    val pendingRegistrationCccd: StateFlow<String?> = _pendingRegistrationCccd.asStateFlow()

    private val _pendingRegistrationRole = MutableStateFlow<String?>(null)
    val pendingRegistrationRole: StateFlow<String?> = _pendingRegistrationRole.asStateFlow()

    // Filter and Search States
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow("ALL") // "ALL", "LEAVE", "SALARY"
    val filterStatus = MutableStateFlow("ALL") // "ALL", "RECEIVED", "APPROVED", "REJECTED"

    // List of Users (for Admin management)
    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactive Proposals list with search and filters applied
    val filteredProposals: StateFlow<List<Proposal>> = combine(
        repository.allProposals,
        searchQuery,
        filterType,
        filterStatus,
        _currentUser
    ) { proposals, query, type, status, user ->
        val baseList = if (user?.role == "CAPTAIN") {
            // Captains can only see their own proposals
            proposals.filter { p -> p.proposerCccd == user.cccd }
        } else {
            // Officers and Admin can see all proposals
            proposals
        }

        baseList.filter { p ->
            val matchesQuery = p.employeeName.contains(query, ignoreCase = true) ||
                    p.proposerName.contains(query, ignoreCase = true) ||
                    p.reason.contains(query, ignoreCase = true)
            
            val matchesType = type == "ALL" || p.type == type
            val matchesStatus = status == "ALL" || p.status == status

            matchesQuery && matchesType && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Clear login errors
    fun clearLoginError() {
        _loginError.value = null
    }

    // Cancel registration state
    fun cancelFirstTimeSetup() {
        _pendingRegistrationCccd.value = null
        _pendingRegistrationRole.value = null
    }

    // Log out user
    fun logout() {
        _currentUser.value = null
        _pendingRegistrationCccd.value = null
        _pendingRegistrationRole.value = null
        clearLoginError()
    }

    // Login process
    fun login(cccd: String, passwordEntered: String) {
        clearLoginError()
        val cleanedCccd = cccd.trim()
        
        // Validation: 12 digits, only numbers
        if (cleanedCccd.length != 12 || !cleanedCccd.all { it.isDigit() }) {
            _loginError.value = "Số CCCD phải nhập đủ 12 số không dư hoặc không thiếu, chỉ nhập số!"
            return
        }

        viewModelScope.launch {
            val user = repository.getUserByCccd(cleanedCccd)
            if (user != null) {
                // Pre-authorized CCCD exists!
                // If profile is not set up yet (fullName is empty or password is empty), trigger self-setup
                if (user.fullName.isBlank() || user.password.isBlank()) {
                    _pendingRegistrationCccd.value = cleanedCccd
                    _pendingRegistrationRole.value = user.role
                } else {
                    // Profile already set up, verify password
                    if (user.password == passwordEntered) {
                        _currentUser.value = user
                    } else {
                        _loginError.value = "Sai mật khẩu! Vui lòng kiểm tra lại."
                    }
                }
            } else {
                // CCCD does not exist in DB: unauthorized!
                _loginError.value = "Số CCCD này chưa được Admin cấp quyền! Vui lòng liên hệ Admin để phân quyền."
            }
        }
    }

    // Complete registration-free login
    fun completeFirstTimeSetup(cccd: String, fullName: String, role: String, passwordEntered: String) {
        clearLoginError()
        val trimmedName = fullName.trim()
        if (trimmedName.isEmpty()) {
            _loginError.value = "Vui lòng nhập Họ và tên nhân viên!"
            return
        }
        val trimmedPassword = passwordEntered.trim()
        if (trimmedPassword.isEmpty()) {
            _loginError.value = "Vui lòng nhập mật khẩu đăng nhập!"
            return
        }

        viewModelScope.launch {
            val newUser = User(
                cccd = cccd,
                fullName = trimmedName,
                role = role,
                password = trimmedPassword
            )
            repository.insertUser(newUser)
            _currentUser.value = newUser
            _pendingRegistrationCccd.value = null
            _pendingRegistrationRole.value = null
        }
    }

    // Submit a Leave/Resignation Proposal (Captain role)
    fun submitLeaveProposal(
        employeeName: String,
        leaveType: String, // "LEAVE", "RESIGNATION"
        leaveDate: String, // YYYY-MM-DD
        reason: String,
        imagePath: String? // Optional base64-like attached image details
    ) {
        val Proposer = _currentUser.value ?: return
        if (employeeName.trim().isEmpty() || leaveDate.trim().isEmpty() || reason.trim().isEmpty()) {
            return
        }

        viewModelScope.launch {
            val newProposal = Proposal(
                proposerCccd = Proposer.cccd,
                proposerName = Proposer.fullName,
                employeeName = employeeName.trim(),
                type = "LEAVE",
                leaveType = leaveType,
                leaveDate = leaveDate,
                reason = reason.trim(),
                imagePath = imagePath,
                status = "RECEIVED" // Received/Đã tiếp nhận by default
            )
            repository.insertProposal(newProposal)
        }
    }

    // Submit a Salary Adjustment Proposal (Captain role)
    fun submitSalaryProposal(
        employeeName: String,
        currentSalary: Double,
        proposedSalary: Double,
        reason: String
    ) {
        val Proposer = _currentUser.value ?: return
        if (employeeName.trim().isEmpty() || currentSalary <= 0 || proposedSalary <= 0 || reason.trim().isEmpty()) {
            return
        }

        viewModelScope.launch {
            val newProposal = Proposal(
                proposerCccd = Proposer.cccd,
                proposerName = Proposer.fullName,
                employeeName = employeeName.trim(),
                type = "SALARY",
                currentSalary = currentSalary,
                proposedSalary = proposedSalary,
                reason = reason.trim(),
                status = "RECEIVED"
            )
            repository.insertProposal(newProposal)
        }
    }

    // Process/Approve or Reject Proposal (Officer role)
    fun processProposal(proposalId: Int, isApproved: Boolean, rejectReason: String?) {
        val officer = _currentUser.value ?: return
        viewModelScope.launch {
            val existing = repository.getProposalById(proposalId)
            if (existing != null) {
                val updated = existing.copy(
                    status = if (isApproved) "APPROVED" else "REJECTED",
                    officerCccd = officer.cccd,
                    officerName = officer.fullName,
                    rejectReason = rejectReason?.trim()
                )
                repository.updateProposal(updated)
            }
        }
    }

    // Admin Feature: Update a User's role
    fun updateUserRole(userCccd: String, newRole: String) {
        viewModelScope.launch {
            val user = repository.getUserByCccd(userCccd)
            if (user != null) {
                // Update default passwords when changing roles if password was default
                val currentPassword = user.password
                val updatedPassword = if (currentPassword == "nds123" || currentPassword == "nds456") {
                    if (newRole == "CAPTAIN") "nds123" else "nds456"
                } else {
                    currentPassword
                }

                val updatedUser = user.copy(role = newRole, password = updatedPassword)
                repository.updateUser(updatedUser)
            }
        }
    }

    // Admin Feature: Delete a User
    fun deleteUser(user: User) {
        viewModelScope.launch {
            repository.deleteUser(user)
        }
    }

    // Admin Feature: Add a new preconfigured User with full details
    fun addNewUser(cccd: String, fullName: String, role: String, password: String, assignedLocation: String) {
        val cleanedCccd = cccd.trim()
        if (cleanedCccd.length != 12 || !cleanedCccd.all { it.isDigit() }) return
        viewModelScope.launch {
            repository.insertUser(
                User(
                    cccd = cleanedCccd,
                    fullName = fullName.trim(),
                    role = role,
                    password = password.trim(),
                    assignedLocation = assignedLocation.trim()
                )
            )
        }
    }
}

// Simple Factory for ViewModel
class SecurityViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SecurityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SecurityViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
