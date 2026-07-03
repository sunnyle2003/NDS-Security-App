package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val cccd: String, // 12 digits, only numbers
    val fullName: String,
    val role: String, // "ADMIN", "CAPTAIN", "OFFICER"
    val password: String,
    val assignedLocation: String = "" // "Mục tiêu quản lý"
)
