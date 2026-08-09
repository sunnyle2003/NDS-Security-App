package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.ProposalDao
import com.example.data.local.UserDao
import com.example.data.local.ViolationDao
import com.example.data.model.Proposal
import com.example.data.model.User
import com.example.data.model.Violation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

data class ConnectionTestResult(
    val isSuccess: Boolean = false,
    val statusCode: Int = 0,
    val latencyMs: Long = 0,
    val message: String = "",
    val detail: String = ""
)

class FirebaseSyncService(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("firebase_sync_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "FirebaseSyncService"
        private const val PREF_FIREBASE_URL = "firebase_database_url"
        const val DEFAULT_FIREBASE_URL =
            "https://gen-lang-client-0615295150-default-rtdb.firebaseio.com"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow("Đã sẵn sàng đồng bộ trực tuyến")
    val syncStatusMessage: StateFlow<String> = _syncStatusMessage.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(prefs.getLong("last_sync_timestamp", 0L))
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private fun getInitialUrl(): String {
        val saved = prefs.getString(PREF_FIREBASE_URL, null)
        return if (saved.isNullOrBlank() || saved.contains("security-nda-system-default-rtdb")) {
            prefs.edit().putString(PREF_FIREBASE_URL, DEFAULT_FIREBASE_URL).apply()
            DEFAULT_FIREBASE_URL
        } else {
            saved
        }
    }

    private val _firebaseUrl = MutableStateFlow(getInitialUrl())
    val firebaseUrl: StateFlow<String> = _firebaseUrl.asStateFlow()

    private val _testResult = MutableStateFlow<ConnectionTestResult?>(null)
    val testResult: StateFlow<ConnectionTestResult?> = _testResult.asStateFlow()

    fun updateFirebaseUrl(url: String) {
        val cleanUrl = url.trim().removeSuffix("/")
        prefs.edit().putString(PREF_FIREBASE_URL, cleanUrl).apply()
        _firebaseUrl.value = cleanUrl
        _testResult.value = null
    }

    private fun getEndpoint(path: String): String {
        val base = _firebaseUrl.value.ifBlank { DEFAULT_FIREBASE_URL }.removeSuffix("/")
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "$base$cleanPath.json"
    }

    // =========================================================================
    // CONNECTION TEST & DIAGNOSTICS
    // =========================================================================
    suspend fun testConnection(targetUrl: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        val cleanUrl = targetUrl.trim().removeSuffix("/")
        if (cleanUrl.isBlank() || !cleanUrl.startsWith("http")) {
            val res = ConnectionTestResult(
                isSuccess = false,
                statusCode = 0,
                latencyMs = 0,
                message = "URL không hợp lệ",
                detail = "URL Firebase Realtime Database phải bắt đầu bằng https:// (ví dụ: https://du-an-cua-ban-default-rtdb.firebaseio.com)"
            )
            _testResult.value = res
            return@withContext res
        }

        val startTime = System.currentTimeMillis()
        val testUrl = "$cleanUrl/health_check.json"

        try {
            // Step 1: Try writing a test ping timestamp
            val pingJson = "{\"ping\": ${System.currentTimeMillis()}, \"app\": \"SecurityNDASystem\"}"
            val request = Request.Builder()
                .url(testUrl)
                .put(pingJson.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime

            val result = when (response.code) {
                200 -> ConnectionTestResult(
                    isSuccess = true,
                    statusCode = 200,
                    latencyMs = latency,
                    message = "Kết nối Đám Mây Thành Công (${latency}ms)",
                    detail = "Firebase Realtime Database đang hoạt động hoàn hảo và có quyền Đọc/Ghi trực tuyến!"
                )
                401 -> ConnectionTestResult(
                    isSuccess = false,
                    statusCode = 401,
                    latencyMs = latency,
                    message = "Lỗi 401: Bị Khóa Quyền Đọc/Ghi (Permission Denied)",
                    detail = "Database của bạn đang bị khóa Rules. Vui lòng vào Firebase Console > Realtime Database > Tab 'Rules' và đổi thành:\n{\n  \"rules\": {\n    \".read\": true,\n    \".write\": true\n  }\n}\nsau đó bấm Publish."
                )
                404 -> ConnectionTestResult(
                    isSuccess = false,
                    statusCode = 404,
                    latencyMs = latency,
                    message = "Lỗi 404: Database Không Tồn Tại",
                    detail = "Không tìm thấy cơ sở dữ liệu tại URL này. Vui lòng kiểm tra lại chính xác URL Firebase Database."
                )
                else -> ConnectionTestResult(
                    isSuccess = false,
                    statusCode = response.code,
                    latencyMs = latency,
                    message = "Mã phản hồi từ máy chủ: ${response.code}",
                    detail = "Máy chủ phản hồi với mã lỗi ${response.code}: ${response.message}"
                )
            }
            _testResult.value = result
            return@withContext result
        } catch (e: UnknownHostException) {
            val res = ConnectionTestResult(
                isSuccess = false,
                statusCode = 0,
                latencyMs = 0,
                message = "Không Thể Kết Nối Mạng (DNS Error)",
                detail = "Không tìm thấy tên miền máy chủ. Vui lòng kiểm tra kết nối Wifi / 4G hoặc kiểm tra lại đường dẫn URL."
            )
            _testResult.value = res
            return@withContext res
        } catch (e: SocketTimeoutException) {
            val res = ConnectionTestResult(
                isSuccess = false,
                statusCode = 0,
                latencyMs = 0,
                message = "Hết Thời Gian Chờ (Timeout)",
                detail = "Máy chủ phản hồi quá lâu (>6 giây). Kiểm tra chất lượng mạng của bạn."
            )
            _testResult.value = res
            return@withContext res
        } catch (e: ConnectException) {
            val res = ConnectionTestResult(
                isSuccess = false,
                statusCode = 0,
                latencyMs = 0,
                message = "Từ Chối Kết Nối",
                detail = "Không thể thiết lập kết nối tới máy chủ đám mây: ${e.message}"
            )
            _testResult.value = res
            return@withContext res
        } catch (e: Exception) {
            val res = ConnectionTestResult(
                isSuccess = false,
                statusCode = 0,
                latencyMs = 0,
                message = "Lỗi Kết Nối",
                detail = "${e.javaClass.simpleName}: ${e.message}"
            )
            _testResult.value = res
            return@withContext res
        }
    }

    // =========================================================================
    // USER SYNC
    // =========================================================================

    suspend fun syncUsers(userDao: UserDao): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = getEndpoint("/users")
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: "{}"
                if (body != "null" && body.isNotBlank()) {
                    if (body.trim().startsWith("{")) {
                        val rootJson = JSONObject(body)
                        val keys = rootJson.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val userJson = rootJson.optJSONObject(key)
                            if (userJson != null) {
                                val user = parseUser(userJson)
                                if (user.cccd.isNotBlank()) {
                                    userDao.insertUser(user)
                                }
                            }
                        }
                    } else if (body.trim().startsWith("[")) {
                        val rootArray = JSONArray(body)
                        for (i in 0 until rootArray.length()) {
                            val userJson = rootArray.optJSONObject(i)
                            if (userJson != null) {
                                val user = parseUser(userJson)
                                if (user.cccd.isNotBlank()) {
                                    userDao.insertUser(user)
                                }
                            }
                        }
                    }
                }

                // Push all local users to Cloud so everything is in sync
                val localUsers = userDao.getAllUsers().first()
                for (user in localUsers) {
                    pushUserDirect(user)
                }
                return@withContext true
            } else {
                Log.w(TAG, "Sync users response error: ${response.code}")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync users: ${e.message}")
            return@withContext false
        }
    }

    suspend fun fetchUserDirect(cccd: String): User? = withContext(Dispatchers.IO) {
        try {
            val cleanCccd = cccd.filter { it.isDigit() }.trim()
            if (cleanCccd.isBlank()) return@withContext null
            val url = getEndpoint("/users/$cleanCccd")
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                if (body != "null" && body.isNotBlank() && body.trim().startsWith("{")) {
                    val json = JSONObject(body)
                    val user = parseUser(json)
                    if (user.cccd.isNotBlank()) {
                        return@withContext user
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to direct fetch user $cccd: ${e.message}")
        }
        return@withContext null
    }

    suspend fun pushUserDirect(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanCccd = user.cccd.filter { it.isDigit() }.trim()
            if (cleanCccd.isBlank()) return@withContext false
            val url = getEndpoint("/users/$cleanCccd")
            val json = userToJson(user).toString()
            val body = json.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push user ${user.cccd}: ${e.message}")
            false
        }
    }

    suspend fun deleteUserDirect(cccd: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanCccd = cccd.filter { it.isDigit() }.trim()
            if (cleanCccd.isBlank()) return@withContext false
            val url = getEndpoint("/users/$cleanCccd")
            val request = Request.Builder().url(url).delete().build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete user $cccd from cloud: ${e.message}")
            false
        }
    }

    // =========================================================================
    // PROPOSAL SYNC
    // =========================================================================

    suspend fun syncProposals(proposalDao: ProposalDao): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = getEndpoint("/proposals")
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: "{}"
                if (body != "null" && body.isNotBlank()) {
                    val proposalsToInsert = mutableListOf<Proposal>()
                    if (body.trim().startsWith("{")) {
                        val rootJson = JSONObject(body)
                        val keys = rootJson.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val propJson = rootJson.optJSONObject(key)
                            if (propJson != null) {
                                val proposal = parseProposal(propJson)
                                if (proposal.proposerCccd.isNotBlank()) {
                                    proposalsToInsert.add(proposal)
                                }
                            }
                        }
                    } else if (body.trim().startsWith("[")) {
                        val rootArray = JSONArray(body)
                        for (i in 0 until rootArray.length()) {
                            val propJson = rootArray.optJSONObject(i)
                            if (propJson != null) {
                                val proposal = parseProposal(propJson)
                                if (proposal.proposerCccd.isNotBlank()) {
                                    proposalsToInsert.add(proposal)
                                }
                            }
                        }
                    }

                    for (proposal in proposalsToInsert) {
                        val existing = proposalDao.getProposalByProposerAndTimestamp(proposal.proposerCccd, proposal.timestamp)
                        if (existing != null) {
                            proposalDao.updateProposal(proposal.copy(id = existing.id))
                        } else {
                            proposalDao.insertProposal(proposal.copy(id = 0))
                        }
                    }
                }

                // Push all local proposals to Cloud
                val localProposals = proposalDao.getAllProposals().first()
                for (prop in localProposals) {
                    pushProposalDirect(prop)
                }
                return@withContext true
            } else {
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync proposals: ${e.message}")
            return@withContext false
        }
    }

    suspend fun pushProposalDirect(proposal: Proposal): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = "prop_${proposal.proposerCccd}_${proposal.timestamp}"
            val url = getEndpoint("/proposals/$key")
            val json = proposalToJson(proposal).toString()
            val body = json.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push proposal: ${e.message}")
            false
        }
    }

    suspend fun deleteProposalDirect(proposal: Proposal): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = "prop_${proposal.proposerCccd}_${proposal.timestamp}"
            val url = getEndpoint("/proposals/$key")
            val request = Request.Builder().url(url).delete().build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete proposal from cloud: ${e.message}")
            false
        }
    }

    // =========================================================================
    // VIOLATION SYNC
    // =========================================================================

    suspend fun syncViolations(violationDao: ViolationDao): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = getEndpoint("/violations")
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: "{}"
                if (body != "null" && body.isNotBlank()) {
                    val violationsToInsert = mutableListOf<Violation>()
                    if (body.trim().startsWith("{")) {
                        val rootJson = JSONObject(body)
                        val keys = rootJson.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val violJson = rootJson.optJSONObject(key)
                            if (violJson != null) {
                                val violation = parseViolation(violJson)
                                if (violation.reporterCccd.isNotBlank()) {
                                    violationsToInsert.add(violation)
                                }
                            }
                        }
                    } else if (body.trim().startsWith("[")) {
                        val rootArray = JSONArray(body)
                        for (i in 0 until rootArray.length()) {
                            val violJson = rootArray.optJSONObject(i)
                            if (violJson != null) {
                                val violation = parseViolation(violJson)
                                if (violation.reporterCccd.isNotBlank()) {
                                    violationsToInsert.add(violation)
                                }
                            }
                        }
                    }

                    for (violation in violationsToInsert) {
                        val existing = violationDao.getViolationByReporterAndTimestamp(violation.reporterCccd, violation.timestamp)
                        if (existing != null) {
                            violationDao.updateViolation(violation.copy(id = existing.id))
                        } else {
                            violationDao.insertViolation(violation.copy(id = 0))
                        }
                    }
                }

                // Push all local violations to Cloud
                val localViolations = violationDao.getAllViolations().first()
                for (viol in localViolations) {
                    pushViolationDirect(viol)
                }
                return@withContext true
            } else {
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync violations: ${e.message}")
            return@withContext false
        }
    }

    suspend fun pushViolationDirect(violation: Violation): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = "viol_${violation.reporterCccd}_${violation.timestamp}"
            val url = getEndpoint("/violations/$key")
            val json = violationToJson(violation).toString()
            val body = json.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push violation: ${e.message}")
            false
        }
    }

    suspend fun deleteViolationDirect(violation: Violation): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = "viol_${violation.reporterCccd}_${violation.timestamp}"
            val url = getEndpoint("/violations/$key")
            val request = Request.Builder().url(url).delete().build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete violation from cloud: ${e.message}")
            false
        }
    }

    // =========================================================================
    // FULL BIDIRECTIONAL SYNC
    // =========================================================================

    suspend fun syncAll(
        userDao: UserDao,
        proposalDao: ProposalDao,
        violationDao: ViolationDao
    ): Boolean = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.SYNCING
        _syncStatusMessage.value = "Đang đồng bộ dữ liệu đám mây Firebase..."

        val usersOk = syncUsers(userDao)
        val propsOk = syncProposals(proposalDao)
        val violsOk = syncViolations(violationDao)

        val success = usersOk || propsOk || violsOk
        val now = System.currentTimeMillis()

        if (success) {
            _syncState.value = SyncState.SUCCESS
            _syncStatusMessage.value = "Đã đồng bộ trực tuyến thành công! (Tất cả máy đều nhận dữ liệu)"
            _lastSyncTime.value = now
            prefs.edit().putLong("last_sync_timestamp", now).apply()
        } else {
            _syncState.value = SyncState.ERROR
            _syncStatusMessage.value = "Chưa kết nối được Cloud Firebase. Kiểm tra Wifi/4G hoặc Rules Database."
        }

        return@withContext success
    }

    // =========================================================================
    // JSON SERIALIZATION HELPERS
    // =========================================================================

    private fun userToJson(user: User): JSONObject {
        return JSONObject().apply {
            put("cccd", user.cccd)
            put("fullName", user.fullName)
            put("role", user.role)
            put("password", user.password)
            put("assignedLocation", user.assignedLocation)
        }
    }

    private fun parseUser(json: JSONObject): User {
        return User(
            cccd = json.optString("cccd", ""),
            fullName = json.optString("fullName", ""),
            role = json.optString("role", "CAPTAIN"),
            password = json.optString("password", ""),
            assignedLocation = json.optString("assignedLocation", "")
        )
    }

    private fun proposalToJson(proposal: Proposal): JSONObject {
        return JSONObject().apply {
            put("id", proposal.id)
            put("proposerCccd", proposal.proposerCccd)
            put("proposerName", proposal.proposerName)
            put("employeeName", proposal.employeeName)
            put("type", proposal.type)
            if (proposal.leaveType != null) put("leaveType", proposal.leaveType)
            if (proposal.leaveDate != null) put("leaveDate", proposal.leaveDate)
            if (proposal.leaveEndDate != null) put("leaveEndDate", proposal.leaveEndDate)
            if (proposal.imagePath != null) put("imagePath", proposal.imagePath)
            if (proposal.currentSalary != null) put("currentSalary", proposal.currentSalary)
            if (proposal.proposedSalary != null) put("proposedSalary", proposal.proposedSalary)
            if (proposal.salaryEffectiveDate != null) put("salaryEffectiveDate", proposal.salaryEffectiveDate)
            put("reason", proposal.reason)
            put("status", proposal.status)
            if (proposal.officerCccd != null) put("officerCccd", proposal.officerCccd)
            if (proposal.officerName != null) put("officerName", proposal.officerName)
            if (proposal.rejectReason != null) put("rejectReason", proposal.rejectReason)
            put("timestamp", proposal.timestamp)
        }
    }

    private fun parseProposal(json: JSONObject): Proposal {
        val curSalary = if (json.has("currentSalary") && !json.isNull("currentSalary")) json.optDouble("currentSalary") else null
        val propSalary = if (json.has("proposedSalary") && !json.isNull("proposedSalary")) json.optDouble("proposedSalary") else null
        
        return Proposal(
            id = json.optInt("id", 0),
            proposerCccd = json.optString("proposerCccd", ""),
            proposerName = json.optString("proposerName", ""),
            employeeName = json.optString("employeeName", ""),
            type = json.optString("type", "LEAVE"),
            leaveType = if (json.has("leaveType") && !json.isNull("leaveType")) json.optString("leaveType") else null,
            leaveDate = if (json.has("leaveDate") && !json.isNull("leaveDate")) json.optString("leaveDate") else null,
            leaveEndDate = if (json.has("leaveEndDate") && !json.isNull("leaveEndDate")) json.optString("leaveEndDate") else null,
            imagePath = if (json.has("imagePath") && !json.isNull("imagePath")) json.optString("imagePath") else null,
            currentSalary = curSalary,
            proposedSalary = propSalary,
            salaryEffectiveDate = if (json.has("salaryEffectiveDate") && !json.isNull("salaryEffectiveDate")) json.optString("salaryEffectiveDate") else null,
            reason = json.optString("reason", ""),
            status = json.optString("status", "RECEIVED"),
            officerCccd = if (json.has("officerCccd") && !json.isNull("officerCccd")) json.optString("officerCccd") else null,
            officerName = if (json.has("officerName") && !json.isNull("officerName")) json.optString("officerName") else null,
            rejectReason = if (json.has("rejectReason") && !json.isNull("rejectReason")) json.optString("rejectReason") else null,
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }

    private fun violationToJson(violation: Violation): JSONObject {
        return JSONObject().apply {
            put("id", violation.id)
            put("reporterCccd", violation.reporterCccd)
            put("reporterName", violation.reporterName)
            put("targetType", violation.targetType)
            put("targetName", violation.targetName)
            put("violationType", violation.violationType)
            if (violation.imagePath != null) put("imagePath", violation.imagePath)
            put("status", violation.status)
            if (violation.penalty != null) put("penalty", violation.penalty)
            if (violation.penaltyNote != null) put("penaltyNote", violation.penaltyNote)
            if (violation.officerCccd != null) put("officerCccd", violation.officerCccd)
            if (violation.officerName != null) put("officerName", violation.officerName)
            put("timestamp", violation.timestamp)
        }
    }

    private fun parseViolation(json: JSONObject): Violation {
        return Violation(
            id = json.optInt("id", 0),
            reporterCccd = json.optString("reporterCccd", ""),
            reporterName = json.optString("reporterName", ""),
            targetType = json.optString("targetType", "TARGET"),
            targetName = json.optString("targetName", ""),
            violationType = json.optString("violationType", ""),
            imagePath = if (json.has("imagePath") && !json.isNull("imagePath")) json.optString("imagePath") else null,
            status = json.optString("status", "RECEIVED"),
            penalty = if (json.has("penalty") && !json.isNull("penalty")) json.optString("penalty") else null,
            penaltyNote = if (json.has("penaltyNote") && !json.isNull("penaltyNote")) json.optString("penaltyNote") else null,
            officerCccd = if (json.has("officerCccd") && !json.isNull("officerCccd")) json.optString("officerCccd") else null,
            officerName = if (json.has("officerName") && !json.isNull("officerName")) json.optString("officerName") else null,
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }
}
