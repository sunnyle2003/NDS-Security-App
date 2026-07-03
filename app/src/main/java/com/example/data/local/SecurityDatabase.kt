package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Proposal
import com.example.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [User::class, Proposal::class], version = 1, exportSchema = false)
abstract class SecurityDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun proposalDao(): ProposalDao

    companion object {
        @Volatile
        private var INSTANCE: SecurityDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): SecurityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SecurityDatabase::class.java,
                    "security_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(SecurityDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SecurityDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val userDao = database.userDao()
                    val proposalDao = database.proposalDao()
                    
                    // Seed Admin Account
                    userDao.insertUser(
                        User(
                            cccd = "000000000000",
                            fullName = "Quản trị viên Hệ thống",
                            role = "ADMIN",
                            password = "admin123",
                            assignedLocation = "Trụ sở chính"
                        )
                    )
                    
                    // Seed Default Captain Account
                    userDao.insertUser(
                        User(
                            cccd = "111111111111",
                            fullName = "Đội trưởng Nguyễn Văn A",
                            role = "CAPTAIN",
                            password = "nds123",
                            assignedLocation = "Ngày & Đêm Security"
                        )
                    )
                    
                    // Seed Default Officer Account
                    userDao.insertUser(
                        User(
                            cccd = "222222222222",
                            fullName = "Cán bộ Nghiệp vụ Lê Văn B",
                            role = "OFFICER",
                            password = "nds456",
                            assignedLocation = "Phòng Nghiệp vụ"
                        )
                    )

                    // Seed some sample proposals for a rich visual experience out-of-the-box!
                    proposalDao.insertProposal(
                        Proposal(
                            proposerCccd = "111111111111",
                            proposerName = "Đội trưởng Nguyễn Văn A",
                            employeeName = "Trần Văn Nam",
                            type = "LEAVE",
                            leaveType = "LEAVE",
                            leaveDate = "2026-07-10",
                            reason = "Xin nghỉ phép 3 ngày về quê giải quyết việc gia đình",
                            status = "RECEIVED"
                        )
                    )
                    
                    proposalDao.insertProposal(
                        Proposal(
                            proposerCccd = "111111111111",
                            proposerName = "Đội trưởng Nguyễn Văn A",
                            employeeName = "Phạm Minh Tuấn",
                            type = "LEAVE",
                            leaveType = "RESIGNATION",
                            leaveDate = "2026-07-20",
                            reason = "Lý do cá nhân, xin nghỉ việc theo nguyện vọng",
                            status = "APPROVED",
                            officerCccd = "222222222222",
                            officerName = "Cán bộ Nghiệp vụ Lê Văn B"
                        )
                    )

                    proposalDao.insertProposal(
                        Proposal(
                            proposerCccd = "111111111111",
                            proposerName = "Đội trưởng Nguyễn Văn A",
                            employeeName = "Nguyễn Hoàng Nam",
                            type = "SALARY",
                            currentSalary = 8500000.0,
                            proposedSalary = 9800000.0,
                            reason = "Hoàn thành xuất sắc nhiệm vụ bảo vệ mục tiêu trọng điểm, tăng ca nhiệt tình",
                            status = "RECEIVED"
                        )
                    )
                }
            }
        }
    }
}
