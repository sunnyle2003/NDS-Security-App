package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "violations")
data class Violation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reporterCccd: String, // CCCD of the Cán bộ Điều lệnh who reported
    val reporterName: String, // Name of the Cán bộ Điều lệnh
    val targetType: String, // "TARGET" (Mục tiêu vi phạm) or "EMPLOYEE" (Nhân viên vi phạm)
    val targetName: String, // Entered manually
    val violationType: String, // e.g. "Chụp sai quy định", "Ngủ", etc.
    val imagePath: String? = null, // Attached picture (Base64 string or description)
    val status: String = "RECEIVED", // "RECEIVED" (Đã tiếp nhận), "PROCESSED" (Đã xử lý)
    val penalty: String? = null, // "Trừ tiền mặt", "Nhắc nhở", "Cộng gộp lần 2", "Trừ thưởng", etc.
    val penaltyNote: String? = null, // Optional note
    val officerCccd: String? = null, // CCCD of the Cán bộ Nghiệp vụ who chose the penalty
    val officerName: String? = null, // Name of the Cán bộ Nghiệp vụ
    val timestamp: Long = System.currentTimeMillis()
)
