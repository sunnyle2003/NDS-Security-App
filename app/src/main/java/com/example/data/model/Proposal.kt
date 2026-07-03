package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proposals")
data class Proposal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val proposerCccd: String,
    val proposerName: String,
    val employeeName: String,
    val type: String, // "LEAVE", "SALARY"
    val leaveType: String? = null, // "LEAVE" (Nghỉ phép), "RESIGNATION" (Nghỉ việc)
    val leaveDate: String? = null, // YYYY-MM-DD
    val imagePath: String? = null, // Base64 or local description or drawing
    val currentSalary: Double? = null,
    val proposedSalary: Double? = null,
    val reason: String,
    val status: String, // "RECEIVED", "APPROVED", "REJECTED"
    val officerCccd: String? = null,
    val officerName: String? = null,
    val rejectReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
