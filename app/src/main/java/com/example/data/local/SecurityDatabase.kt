package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Proposal
import com.example.data.model.User
import com.example.data.model.Violation
import com.example.data.local.ViolationDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [User::class, Proposal::class, Violation::class], version = 4, exportSchema = false)
abstract class SecurityDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun proposalDao(): ProposalDao
    abstract fun violationDao(): ViolationDao

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
            seedDatabase()
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            seedDatabase()
        }

        private fun seedDatabase() {
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val userDao = database.userDao()
                    val proposalDao = database.proposalDao()
                    val violationDao = database.violationDao()
                    
                    val adminUser = userDao.getUserByCccd("000000000000")
                    if (adminUser == null) {
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

                        // Seed Default Discipline Officer Account
                        userDao.insertUser(
                            User(
                                cccd = "333333333333",
                                fullName = "Cán bộ Điều lệnh Trần Văn C",
                                role = "DISCIPLINE",
                                password = "nds789",
                                assignedLocation = "Phòng Điều lệnh"
                            )
                        )
                    }
                }
            }
        }
    }
}
