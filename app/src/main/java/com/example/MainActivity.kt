package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.Proposal
import com.example.data.model.User
import com.example.data.model.Violation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.WarningColor
import com.example.ui.theme.InfoColor
import com.example.ui.viewmodel.SecurityViewModel
import com.example.ui.viewmodel.SecurityViewModelFactory
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

import com.example.data.remote.SyncState

class MainActivity : ComponentActivity() {
    private val viewModel: SecurityViewModel by viewModels {
        SecurityViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    SecurityAppMain(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SecurityAppMain(
    viewModel: SecurityViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val pendingRegistrationCccd by viewModel.pendingRegistrationCccd.collectAsStateWithLifecycle()
    val isCloudConfigOpen by viewModel.isCloudConfigOpen.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = if (currentUser != null) ScreenState.DASHBOARD else ScreenState.LOGIN,
            transitionSpec = {
                fadeIn() + slideInHorizontally() togetherWith fadeOut() + slideOutHorizontally()
            },
            label = "ScreenTransition"
        ) { targetState ->
            when (targetState) {
                ScreenState.LOGIN -> {
                    LoginScreen(viewModel = viewModel)
                }
                ScreenState.DASHBOARD -> {
                    val user = currentUser
                    if (user != null) {
                        when (user.role) {
                            "ADMIN" -> AdminDashboardScreen(user = user, viewModel = viewModel)
                            "CAPTAIN" -> CaptainDashboardScreen(user = user, viewModel = viewModel)
                            "OFFICER" -> OfficerDashboardScreen(user = user, viewModel = viewModel)
                            "DISCIPLINE" -> DisciplineDashboardScreen(user = user, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

enum class ScreenState {
    LOGIN,
    DASHBOARD
}

// -----------------------------------------------------------------------------
// LOGIN SCREEN
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: SecurityViewModel) {
    var cccd by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Custom drawn elegant company logo badge
            SecurityBadgeLogo(modifier = Modifier.size(120.dp))
            
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "NGÀY & ĐÊM SECURITY",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Hệ thống Quản lý Vận hành Nghiệp vụ",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "ĐĂNG NHẬP HỆ THỐNG",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    // CCCD input field
                    OutlinedTextField(
                        value = cccd,
                        onValueChange = { input ->
                            // Allow only digits and limit to 12
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 12) {
                                cccd = digits
                            }
                        },
                        label = { Text("Số CCCD (12 chữ số)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = "CCCD Icon"
                            )
                        },
                        placeholder = { Text("Nhập đủ 12 chữ số") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cccd_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Password input field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mật khẩu") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password Icon"
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Show validation error
                    if (loginError != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = loginError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.login(cccd, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ĐĂNG NHẬP",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Discreet notice for users
                    Text(
                        text = "ℹ️ Tài khoản nhân viên do Quản trị viên hệ thống cấp. Vui lòng liên hệ Admin nếu chưa có tài khoản hoặc quên mật khẩu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Official company logo badge using embedded/network image with graceful vector fallback
@Composable
fun SecurityBadgeLogo(modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = "https://i.ibb.co/fzhxZv5H/z7997808820598-c1f98aab385f7f198efebdd2a8b84471.jpg",
        contentDescription = "Night Day Security Logo",
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)),
        contentScale = ContentScale.Fit,
        loading = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Security Shield",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

// -----------------------------------------------------------------------------
// CLOUD / FIREBASE SYNCHRONIZATION UI COMPONENTS
// -----------------------------------------------------------------------------
@Composable
fun CloudSyncStatusBar(
    viewModel: SecurityViewModel,
    modifier: Modifier = Modifier
) {
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()

    val timeFormatted = if (lastSyncTime > 0) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.format(Date(lastSyncTime))
    } else "Chưa có"

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (syncState) {
                SyncState.SYNCING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                SyncState.SUCCESS -> SuccessColor.copy(alpha = 0.12f)
                SyncState.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                SyncState.IDLE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (syncState) {
                    SyncState.SYNCING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    SyncState.SUCCESS -> {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloud Synced",
                            tint = SuccessColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    SyncState.ERROR -> {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Cloud Offline",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    SyncState.IDLE -> {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Cloud Ready",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = when (syncState) {
                            SyncState.SYNCING -> "Đang đồng bộ dữ liệu đám mây..."
                            SyncState.SUCCESS -> "🟢 Đã đồng bộ Cloud/Firebase ($timeFormatted)"
                            SyncState.ERROR -> "⚠️ Ngoại tuyến / Lỗi kết nối đám mây"
                            SyncState.IDLE -> "☁️ Đồng bộ Cloud/Firebase ($timeFormatted)"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = syncMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { viewModel.triggerManualSync() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync Now",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleCloudConfigDialog(true) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Cloud Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CloudSyncBadge(viewModel: SecurityViewModel, modifier: Modifier = Modifier) {
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    
    Surface(
        onClick = { viewModel.toggleCloudConfigDialog(true) },
        shape = RoundedCornerShape(12.dp),
        color = when (syncState) {
            SyncState.SYNCING -> MaterialTheme.colorScheme.primaryContainer
            SyncState.SUCCESS -> SuccessColor.copy(alpha = 0.15f)
            SyncState.ERROR -> MaterialTheme.colorScheme.errorContainer
            SyncState.IDLE -> MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (syncState) {
                SyncState.SYNCING -> CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                SyncState.SUCCESS -> Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Cloud Synced",
                    tint = SuccessColor,
                    modifier = Modifier.size(14.dp)
                )
                SyncState.ERROR -> Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Cloud Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
                SyncState.IDLE -> Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Cloud Ready",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = when (syncState) {
                    SyncState.SYNCING -> "Đang đồng bộ..."
                    SyncState.SUCCESS -> "Cloud: Đã đồng bộ"
                    SyncState.ERROR -> "Cloud: Ngoại tuyến"
                    SyncState.IDLE -> "Cloud: Sẵn sàng"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = when (syncState) {
                    SyncState.SUCCESS -> SuccessColor
                    SyncState.ERROR -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun CloudConfigDialog(viewModel: SecurityViewModel) {
    val currentUrl by viewModel.firebaseUrl.collectAsStateWithLifecycle()
    var inputUrl by remember(currentUrl) { mutableStateOf(currentUrl) }
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()
    var isTestingConnection by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(true) }

    val timeFormatted = if (lastSyncTime > 0) {
        val sdf = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
        sdf.format(Date(lastSyncTime))
    } else "Chưa đồng bộ"

    Dialog(onDismissRequest = { viewModel.toggleCloudConfigDialog(false) }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Cloud Sync Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Đồng Bộ Đám Mây Trực Tuyến",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { viewModel.toggleCloudConfigDialog(false) }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Current Sync Status Box
                Surface(
                    color = when (syncState) {
                        SyncState.SUCCESS -> SuccessColor.copy(alpha = 0.12f)
                        SyncState.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        SyncState.SYNCING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        SyncState.IDLE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            when (syncState) {
                                SyncState.SUCCESS -> Icon(Icons.Default.CloudDone, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(16.dp))
                                SyncState.ERROR -> Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                SyncState.SYNCING -> CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                SyncState.IDLE -> Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "Trạng thái: $syncMessage",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Lần đồng bộ gần nhất: $timeFormatted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // URL Input Field
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text("Firebase Realtime Database URL") },
                    placeholder = { Text("https://gen-lang-client-0615295150-default-rtdb.firebaseio.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            inputUrl = "https://gen-lang-client-0615295150-default-rtdb.firebaseio.com"
                        }) {
                            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset default")
                        }
                    }
                )

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            inputUrl = "https://gen-lang-client-0615295150-default-rtdb.firebaseio.com"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text("📍 Server Firebase Mặc Định (gen-lang-client)", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Test Connection Button & Diagnostic
                OutlinedButton(
                    onClick = {
                        isTestingConnection = true
                        viewModel.testFirebaseConnection(inputUrl)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = "Test", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🔍 KIỂM TRA KẾT NỐI TRỰC TUYẾN (PING)")
                }

                // Test Connection Result Display
                if (testResult != null) {
                    val res = testResult!!
                    Surface(
                        color = if (res.isSuccess) SuccessColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (res.isSuccess) SuccessColor else MaterialTheme.colorScheme.error)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (res.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (res.isSuccess) SuccessColor else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = res.message,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (res.isSuccess) SuccessColor else MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = res.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Collapsible Step-by-Step Setup Guide
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showInstructions = !showInstructions },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "📖 Hướng Dẫn Mở Quyền Online 100% (30s)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (showInstructions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (showInstructions) {
                            Text(
                                text = "Để app hoạt động online giữa tất cả các máy điện thoại, bạn cần mở quyền Realtime Database như sau:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "1️⃣ Vào https://console.firebase.google.com -> Tạo project mới (hoặc chọn project có sẵn).\n" +
                                        "2️⃣ Vào mục Build > Realtime Database > Create Database (chọn Singapore hoặc US).\n" +
                                        "3️⃣ Vào Tab \"Rules\" và đổi thành:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "{\n  \"rules\": {\n    \".read\": true,\n    \".write\": true\n  }\n}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Text(
                                text = "4️⃣ Nhấn nút \"Publish\". Sau đó copy URL Database và dán vào ô bên trên rồi bấm Lưu & Đồng Bộ!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.triggerManualSync()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đồng bộ ngay")
                    }

                    Button(
                        onClick = {
                            viewModel.updateFirebaseUrl(inputUrl)
                            viewModel.toggleCloudConfigDialog(false)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Lưu & Kích hoạt")
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// REGISTRATION-FREE WIZARD (FIRST TIME SETUP & SELF-ACTIVATION)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstTimeSetupScreen(cccd: String, viewModel: SecurityViewModel) {
    val preassignedRole by viewModel.pendingRegistrationRole.collectAsStateWithLifecycle()
    var selectedRole by remember(preassignedRole) { mutableStateOf(preassignedRole ?: "CAPTAIN") }

    var cccdInput by remember(cccd) { mutableStateOf(cccd) }
    var fullName by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("Trụ sở Ngày & Đêm Security") }
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = "Setup Profile",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(68.dp)
            )

            Text(
                text = "KÍCH HOẠT & THIẾT LẬP CCCD",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Vui lòng nhập đầy đủ thông tin nhân viên và phân quyền để đăng nhập ngay trên thiết bị này.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // CCCD Input
                    OutlinedTextField(
                        value = cccdInput,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 12) {
                                cccdInput = digits
                            }
                        },
                        label = { Text("Số CCCD (12 chữ số)") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Badge, contentDescription = "CCCD") },
                        placeholder = { Text("Nhập đủ 12 chữ số") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Full Name
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Họ và Tên Nhân Viên") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Name") },
                        placeholder = { Text("Ví dụ: Lê Duy Tèo / Nguyễn Văn A") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Role Picker
                    Text(
                        text = "Chọn Phân Quyền Vận Hành:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val rolesList = listOf(
                        Triple("ADMIN", "👑 Quản trị viên (Admin)", "Toàn quyền quản trị phân quyền và duyệt hồ sơ"),
                        Triple("CAPTAIN", "🛡️ Đội trưởng Mục tiêu", "Nộp đơn xin nghỉ phép, đề xuất tăng lương"),
                        Triple("OFFICER", "📋 Cán bộ Nghiệp vụ", "Phê duyệt đơn đề xuất, xử lý chế tài vi phạm"),
                        Triple("DISCIPLINE", "⚖️ Cán bộ Điều lệnh", "Kiểm tra và lập biên bản vi phạm tác phong")
                    )

                    rolesList.forEach { (rCode, rTitle, rDesc) ->
                        val isSelected = selectedRole == rCode
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = rCode },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedRole = rCode }
                                )
                                Column {
                                    Text(
                                        text = rTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = rDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Assigned location
                    OutlinedTextField(
                        value = locationInput,
                        onValueChange = { locationInput = it },
                        label = { Text("Mục tiêu / Nơi công tác") },
                        leadingIcon = { Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Location") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Password entry
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Mật khẩu Đăng nhập") },
                        leadingIcon = { Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Password") },
                        placeholder = { Text("Nhập mật khẩu (ví dụ: 2 hoặc nds123)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Error text
                    if (loginError != null) {
                        Text(
                            text = loginError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.cancelFirstTimeSetup() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("HỦY")
                        }

                        Button(
                            onClick = {
                                viewModel.completeFirstTimeSetup(
                                    cccd = cccdInput,
                                    fullName = fullName,
                                    role = selectedRole,
                                    passwordEntered = passwordInput,
                                    location = locationInput
                                )
                            },
                            modifier = Modifier.weight(1.6f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("KÍCH HOẠT NGAY", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// CAPTAIN DASHBOARD (ĐỘI TRƯỞNG)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptainDashboardScreen(user: User, viewModel: SecurityViewModel) {
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showSalaryDialog by remember { mutableStateOf(false) }
    var selectedProposalDetail by remember { mutableStateOf<Proposal?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Đề xuất, 1: Vi phạm

    val proposals by viewModel.filteredProposals.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val filterStatus by viewModel.filterStatus.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Upper Status Card Hero
        CaptainHeroSection(user = user, viewModel = viewModel, onLogout = { viewModel.logout() })

        // Quick action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showLeaveDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("submit_leave_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.EventNote, contentDescription = "Leave")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Xin Nghỉ Phép/Việc", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            }

            Button(
                onClick = { showSalaryDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("submit_salary_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(imageVector = Icons.Default.Payments, contentDescription = "Salary")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đề Xuất Lương", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            }
        }

        // Tab Row switcher for Captain
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Đề xuất của Đội", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) },
                icon = { Icon(imageVector = Icons.Default.Description, contentDescription = "Proposals") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Tiến trình Vi phạm", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) },
                icon = { Icon(imageVector = Icons.Default.Gavel, contentDescription = "Violations") }
            )
        }

        if (selectedTab == 0) {
            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            // History list & Filter header
            ProposalListSection(
                title = "Lịch sử Đề xuất của Đội",
                proposals = proposals,
                searchQuery = searchQuery,
                onSearchChange = { viewModel.searchQuery.value = it },
                filterType = filterType,
                onTypeChange = { viewModel.filterType.value = it },
                filterStatus = filterStatus,
                onStatusChange = { viewModel.filterStatus.value = it },
                onProposalClick = { selectedProposalDetail = it }
            )
        } else {
            val violations by viewModel.allViolations.collectAsStateWithLifecycle()
            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            ViolationListSection(
                title = "Lỗi Vi phạm & Tiến trình xử lý",
                violations = violations,
                isOfficer = false,
                onProcessClick = {}
            )
        }
    }

    // Submit dialogues
    if (showLeaveDialog) {
        SubmitLeaveDialog(
            onDismiss = { showLeaveDialog = false },
            onSubmit = { name, type, startDate, endDate, reason, sigPoints, attachedImage ->
                // Save signature description or pass mock letter details
                val sigDesc = if (sigPoints.isNotEmpty()) "HAS_SIGNATURE" else "NO_SIGNATURE"
                // If we have an attached image, we can store it. Otherwise store sigDesc
                val finalImagePath = attachedImage ?: sigDesc
                viewModel.submitLeaveProposal(name, type, startDate, endDate, reason, finalImagePath)
                showLeaveDialog = false
            }
        )
    }

    if (showSalaryDialog) {
        SubmitSalaryDialog(
            onDismiss = { showSalaryDialog = false },
            onSubmitMultiple = { entries, reason ->
                entries.forEach { entry ->
                    viewModel.submitSalaryProposal(
                        entry.employeeName,
                        entry.currentSalary,
                        entry.proposedSalary,
                        entry.salaryEffectiveDate,
                        reason
                    )
                }
                showSalaryDialog = false
            }
        )
    }

    if (selectedProposalDetail != null) {
        ProposalDetailDialog(
            proposal = selectedProposalDetail!!,
            onDismiss = { selectedProposalDetail = null },
            isOfficer = false,
            onApprove = { _, _, _ -> },
            onReject = { _ -> }
        )
    }
}

@Composable
fun CaptainHeroSection(user: User, viewModel: SecurityViewModel, onLogout: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SubcomposeAsyncImage(
                            model = "https://i.ibb.co/fzhxZv5H/z7997808820598-c1f98aab385f7f198efebdd2a8b84471.jpg",
                            contentDescription = "Captain Avatar",
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            },
                            error = {
                                Box(
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Captain Badge",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        )
                        Column {
                            Text(
                                text = "Đội Trưởng",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Đăng xuất",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("CCCD Đội trưởng", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(user.cccd, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Mục tiêu vận hành", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(user.assignedLocation.ifBlank { "Ngày & Đêm Security" }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), overflow = TextOverflow.Ellipsis, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// OFFICER DASHBOARD (CÁN BỘ PHÒNG NGHIỆP VỤ)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficerDashboardScreen(user: User, viewModel: SecurityViewModel) {
    val proposals by viewModel.filteredProposals.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val filterStatus by viewModel.filterStatus.collectAsStateWithLifecycle()

    var selectedProposalDetail by remember { mutableStateOf<Proposal?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Đề xuất, 1: Vi phạm
    var selectedViolationPenalty by remember { mutableStateOf<Violation?>(null) }

    // Aggregate statistics for Proposals
    val pendingCount = proposals.count { it.status == "RECEIVED" }
    val approvedCount = proposals.count { it.status == "APPROVED" || it.status == "OFFICER_APPROVED" }
    val rejectedCount = proposals.count { it.status == "REJECTED" || it.status == "ADMIN_REJECTED" }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Upper Profile Bar
        OfficerHeader(user = user, viewModel = viewModel, onLogout = { viewModel.logout() })

        // Tab Row switcher
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Đề xuất Vận hành", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) },
                icon = { Icon(imageVector = Icons.Default.Description, contentDescription = "Proposals") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Xử lý Vi phạm", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) },
                icon = { Icon(imageVector = Icons.Default.Gavel, contentDescription = "Violations") }
            )
        }

        if (selectedTab == 0) {
            // Quick Stats Cards Rows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsCard(
                    title = "Đang chờ duyệt",
                    count = pendingCount,
                    color = WarningColor,
                    icon = Icons.Default.HourglassEmpty,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    title = "Đã phê duyệt",
                    count = approvedCount,
                    color = SuccessColor,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    title = "Từ chối",
                    count = rejectedCount,
                    color = ErrorColor,
                    icon = Icons.Default.Cancel,
                    modifier = Modifier.weight(1f)
                )
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // Proposals filterable list
            ProposalListSection(
                title = "Danh sách Đề xuất Vận hành",
                proposals = proposals,
                searchQuery = searchQuery,
                onSearchChange = { viewModel.searchQuery.value = it },
                filterType = filterType,
                onTypeChange = { viewModel.filterType.value = it },
                filterStatus = filterStatus,
                onStatusChange = { viewModel.filterStatus.value = it },
                onProposalClick = { selectedProposalDetail = it }
            )
        } else {
            val violations by viewModel.allViolations.collectAsStateWithLifecycle()
            val totalVi = violations.size
            val pendingVi = violations.count { it.status == "RECEIVED" }
            val resolvedVi = violations.count { it.status == "PROCESSED" }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsCard(
                    title = "Tổng vi phạm",
                    count = totalVi,
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.Assessment,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    title = "Chờ chế tài",
                    count = pendingVi,
                    color = WarningColor,
                    icon = Icons.Default.HourglassEmpty,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    title = "Đã xử phạt",
                    count = resolvedVi,
                    color = SuccessColor,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            ViolationListSection(
                title = "Danh sách Biên bản Vi phạm",
                violations = violations,
                isOfficer = true,
                onProcessClick = { selectedViolationPenalty = it }
            )
        }
    }

    if (selectedProposalDetail != null) {
        ProposalDetailDialog(
            proposal = selectedProposalDetail!!,
            onDismiss = { selectedProposalDetail = null },
            isOfficer = true,
            onApprove = { adjustedDate, adjustedSalary, adjustedSalaryDate ->
                viewModel.processProposal(
                    selectedProposalDetail!!.id,
                    isApproved = true,
                    rejectReason = null,
                    adjustedLeaveDate = adjustedDate,
                    adjustedProposedSalary = adjustedSalary,
                    adjustedSalaryEffectiveDate = adjustedSalaryDate
                )
                selectedProposalDetail = null
            },
            onReject = { reason ->
                viewModel.processProposal(selectedProposalDetail!!.id, isApproved = false, rejectReason = reason)
                selectedProposalDetail = null
            }
        )
    }

    if (selectedViolationPenalty != null) {
        SelectPenaltyDialog(
            violation = selectedViolationPenalty!!,
            onDismiss = { selectedViolationPenalty = null },
            onSubmit = { penalty, note ->
                viewModel.selectPenaltyForViolation(selectedViolationPenalty!!.id, penalty, note)
                selectedViolationPenalty = null
            }
        )
    }
}

@Composable
fun DisciplineDashboardScreen(user: User, viewModel: SecurityViewModel) {
    val violations by viewModel.allViolations.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    // Filter to violations created by this discipline officer
    val myViolations = violations.filter { it.reporterCccd == user.cccd }
    val pendingCount = myViolations.count { it.status == "RECEIVED" }
    val processedCount = myViolations.count { it.status == "PROCESSED" }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Upper Header
        DisciplineHeader(user = user, viewModel = viewModel, onLogout = { viewModel.logout() })

        // Quick Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsCard(
                title = "Đã tiếp nhận",
                count = pendingCount,
                color = WarningColor,
                icon = Icons.Default.HourglassEmpty,
                modifier = Modifier.weight(1f)
            )
            StatsCard(
                title = "Đã xử lý phạt",
                count = processedCount,
                color = SuccessColor,
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
        }

        // Action Button: Lập biên bản vi phạm
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(54.dp)
                .testTag("create_violation_btn"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(imageVector = Icons.Default.AddAlert, contentDescription = "Report Alert")
            Spacer(modifier = Modifier.width(8.dp))
            Text("LẬP BIÊN BẢN VI PHẠM MỚI", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }

        Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        // Violation list
        ViolationListSection(
            title = "Biên bản Vi phạm đã lập",
            violations = myViolations,
            isOfficer = false,
            onProcessClick = {}
        )
    }

    if (showCreateDialog) {
        CreateViolationDialog(
            onDismiss = { showCreateDialog = false },
            onSubmit = { targetType, targetName, violationType, imageUri ->
                viewModel.submitViolationReport(targetType, targetName, violationType, imageUri)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun DisciplineHeader(user: User, viewModel: SecurityViewModel, onLogout: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Discipline Officer",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Column {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "CÁN BỘ ĐIỀU LỆNH",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            IconButton(
                onClick = onLogout,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateViolationDialog(
    onDismiss: () -> Unit,
    onSubmit: (targetType: String, targetName: String, violationType: String, imageUri: String?) -> Unit
) {
    var targetType by remember { mutableStateOf("TARGET") } // "TARGET" or "EMPLOYEE"
    var targetName by remember { mutableStateOf("") }
    var selectedViolation by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<String?>(null) }

    val targetErrors = listOf(
        "Chụp sai quy định",
        "Không chụp báo",
        "Sai giày",
        "Sai nón",
        "Sai cà vạt",
        "Sai đồng phục"
    )

    val employeeErrors = listOf(
        "Ngủ",
        "Sử dụng ĐT",
        "Bỏ vị trí",
        "Sai đồng phục",
        "Mất đoàn kết"
    )

    val errors = if (targetType == "TARGET") targetErrors else employeeErrors

    // When targetType changes, clear selected violation
    LaunchedEffect(targetType) {
        selectedViolation = ""
    }

    val context = LocalContext.current

    // File selection launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri.toString()
        }
    }

    // Camera capture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val cacheFile = File(context.cacheDir, "violation_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(cacheFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
                attachedImageUri = Uri.fromFile(cacheFile).toString()
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Lỗi lưu ảnh: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission launcher for Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            android.widget.Toast.makeText(context, "Cần cấp quyền Camera để chụp ảnh", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "LẬP BIÊN BẢN VI PHẠM",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Divider()

                // Target Type selection
                Text(
                    text = "Đối tượng vi phạm:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ElevatedFilterChip(
                        selected = targetType == "TARGET",
                        onClick = { targetType = "TARGET" },
                        label = { Text("Mục tiêu vi phạm") },
                        modifier = Modifier.weight(1f)
                    )
                    ElevatedFilterChip(
                        selected = targetType == "EMPLOYEE",
                        onClick = { targetType = "EMPLOYEE" },
                        label = { Text("Nhân viên vi phạm") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Name input
                OutlinedTextField(
                    value = targetName,
                    onValueChange = { targetName = it },
                    label = { Text(if (targetType == "TARGET") "Tên Mục Tiêu Vi Phạm" else "Họ & Tên Nhân Viên Vi Phạm") },
                    placeholder = { Text(if (targetType == "TARGET") "Ví dụ: Mục tiêu Landmark 81..." else "Ví dụ: Nguyễn Văn Hải...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Error Selection Grid
                Text(
                    text = "Lỗi vi phạm điều lệnh:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    errors.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { err ->
                                val isSelected = selectedViolation == err
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedViolation = err },
                                    label = { Text(err, style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                )
                            }
                            if (pair.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Attached image section
                Text(
                    text = "Hình ảnh minh chứng vi phạm (Bắt buộc):",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (attachedImageUri == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chụp ảnh")
                        }
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Photo, contentDescription = "Gallery")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chọn ảnh")
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = attachedImageUri,
                            contentDescription = "Attached proof",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { attachedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove image", tint = Color.White)
                        }
                    }
                }

                // Submit and cancel buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("HỦY")
                    }

                    val isEnabled = targetName.isNotBlank() && selectedViolation.isNotBlank() && attachedImageUri != null
                    Button(
                        onClick = {
                            if (isEnabled) {
                                onSubmit(targetType, targetName, selectedViolation, attachedImageUri)
                            }
                        },
                        enabled = isEnabled,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("GỬI BIÊN BẢN", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectPenaltyDialog(
    violation: Violation,
    onDismiss: () -> Unit,
    onSubmit: (penalty: String, note: String) -> Unit
) {
    var selectedPenalty by remember { mutableStateOf("Trừ tiền mặt") }
    var note by remember { mutableStateOf("") }

    val options = listOf(
        "Trừ tiền mặt",
        "Nhắc nhở",
        "Cộng gộp lần 2",
        "Trừ thưởng",
        "Đề nghị sa thải"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "ÁP DỤNG CHẾ TÀI XỬ LÝ",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Divider()

                Text(
                    text = "Biên bản của: ${violation.targetName}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Lỗi vi phạm: ${violation.violationType}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )

                Divider()

                Text(
                    text = "Chọn chế tài xử lý:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPenalty = opt }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedPenalty == opt,
                                onClick = { selectedPenalty = opt }
                            )
                            Text(text = opt, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Lý do / Quyết định xử lý chi tiết") },
                    placeholder = { Text("Nhập lý do chi tiết hoặc căn cứ xử phạt...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("HỦY")
                    }

                    Button(
                        onClick = {
                            if (selectedPenalty.isNotBlank() && note.isNotBlank()) {
                                onSubmit(selectedPenalty, note)
                            }
                        },
                        enabled = selectedPenalty.isNotBlank() && note.isNotBlank(),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("XÁC NHẬN")
                    }
                }
            }
        }
    }
}

@Composable
fun ViolationListSection(
    title: String,
    violations: List<Violation>,
    isOfficer: Boolean,
    onProcessClick: (Violation) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        if (violations.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = "No Violations",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chưa ghi nhận biên bản vi phạm nào",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(violations) { violation ->
                    ViolationItemCard(
                        violation = violation,
                        isOfficer = isOfficer,
                        onProcessClick = { onProcessClick(violation) }
                    )
                }
            }
        }
    }
}

@Composable
fun ViolationItemCard(
    violation: Violation,
    isOfficer: Boolean,
    onProcessClick: () -> Unit
) {
    val isProcessed = violation.status == "PROCESSED"
    val badgeColor = if (isProcessed) SuccessColor else WarningColor
    val badgeText = if (isProcessed) "ĐÃ XỬ LÝ" else "CHỜ XỬ LÝ"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (violation.targetType == "TARGET") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (violation.targetType == "TARGET") "Mục tiêu vi phạm" else "Nhân viên vi phạm",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (violation.targetType == "TARGET") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = violation.targetName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReportProblem,
                        contentDescription = "Violation detail",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Lỗi vi phạm: ${violation.violationType}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = "Reporter icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Bởi: ${violation.reporterName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val df = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
                Text(
                    text = df.format(Date(violation.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!violation.imagePath.isNullOrBlank()) {
                var isExpandedImg by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clickable { isExpandedImg = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    SubcomposeAsyncImage(
                        model = violation.imagePath,
                        contentDescription = "Violation photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                if (isExpandedImg) {
                    Dialog(onDismissRequest = { isExpandedImg = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                ) {
                                    SubcomposeAsyncImage(
                                        model = violation.imagePath,
                                        contentDescription = "Violation photo full",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                    IconButton(
                                        onClick = { isExpandedImg = false },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isProcessed) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = "Sanction icon",
                                tint = SuccessColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "CHẾ TÀI ÁP DỤNG: ${violation.penalty}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = SuccessColor
                            )
                        }
                        Text(
                            text = "Chi tiết: ${violation.penaltyNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Xử lý bởi: ${violation.officerName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isOfficer && !isProcessed) {
                Button(
                    onClick = onProcessClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Gavel, contentDescription = "Process penalty", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ÁP DỤNG CHẾ TÀI XỬ LÝ", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun OfficerHeader(user: User, viewModel: SecurityViewModel, onLogout: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SubcomposeAsyncImage(
                    model = "https://i.ibb.co/fzhxZv5H/z7997808820598-c1f98aab385f7f198efebdd2a8b84471.jpg",
                    contentDescription = "Officer Avatar",
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupervisorAccount,
                                contentDescription = "Officer Badge",
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                )
                Column {
                    Text(
                        text = "Cán bộ Nghiệp vụ",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            IconButton(
                onClick = onLogout,
                modifier = Modifier.background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Đăng xuất",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.Gray
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ADMIN DASHBOARD (QUẢN TRỊ VIÊN)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(user: User, viewModel: SecurityViewModel) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val proposals by viewModel.filteredProposals.collectAsStateWithLifecycle()

    var showAddUserDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0: Users, 1: All Proposals

    Column(modifier = Modifier.fillMaxSize()) {
        // Admin header card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Badge",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Hệ thống Quản trị",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Admin Ngày & Đêm",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Đăng xuất",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Simple Tab Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { activeTab = 0 }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Quản lý Phân Quyền",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (activeTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { activeTab = 1 }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Tổng hợp Đề xuất",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (activeTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeTab == 0) {
            // User role assignment management list
            var editingUser by remember { mutableStateOf<User?>(null) }

            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Danh Sách Tài Khoản Nhân Viên (${users.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Quản lý và cấp quyền đăng nhập toàn hệ thống",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showAddUserDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Thêm mới", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(users) { usr ->
                            UserRoleItemCard(
                                user = usr,
                                onRoleChange = { newRole ->
                                    viewModel.updateUserRole(usr.cccd, newRole)
                                },
                                onEdit = {
                                    editingUser = usr
                                },
                                onDelete = {
                                    viewModel.deleteUser(usr)
                                }
                            )
                        }
                    }
                }
            }

            if (editingUser != null) {
                EditUserDialog(
                    user = editingUser!!,
                    onDismiss = { editingUser = null },
                    onSubmit = { updated ->
                        viewModel.updateUserDetails(updated)
                        editingUser = null
                    }
                )
            }
        } else {
            // Comprehensive history overview of all system proposals
            var searchQueryAdmin by remember { mutableStateOf("") }
            var filterTypeAdmin by remember { mutableStateOf("ALL") }
            var filterStatusAdmin by remember { mutableStateOf("ALL") }
            var selectedProposalDetailAdmin by remember { mutableStateOf<Proposal?>(null) }

            val filteredAdminProposals = proposals.filter { p ->
                val matchesQuery = p.employeeName.contains(searchQueryAdmin, ignoreCase = true) ||
                        p.proposerName.contains(searchQueryAdmin, ignoreCase = true) ||
                        p.reason.contains(searchQueryAdmin, ignoreCase = true)
                val matchesType = filterTypeAdmin == "ALL" || p.type == filterTypeAdmin
                val matchesStatus = when (filterStatusAdmin) {
                    "ALL" -> true
                    "RECEIVED" -> p.status == "RECEIVED" || p.status == "OFFICER_APPROVED"
                    "APPROVED" -> p.status == "APPROVED"
                    "REJECTED" -> p.status == "REJECTED" || p.status == "ADMIN_REJECTED"
                    else -> p.status == filterStatusAdmin
                }
                matchesQuery && matchesType && matchesStatus
            }

            Box(modifier = Modifier.weight(1f)) {
                ProposalListSection(
                    title = "Tổng hợp Lịch sử Vận hành",
                    proposals = filteredAdminProposals,
                    searchQuery = searchQueryAdmin,
                    onSearchChange = { searchQueryAdmin = it },
                    filterType = filterTypeAdmin,
                    onTypeChange = { filterTypeAdmin = it },
                    filterStatus = filterStatusAdmin,
                    onStatusChange = { filterStatusAdmin = it },
                    onProposalClick = { selectedProposalDetailAdmin = it }
                )
            }

            if (selectedProposalDetailAdmin != null) {
                val canAdminApprove = selectedProposalDetailAdmin!!.type == "SALARY" && selectedProposalDetailAdmin!!.status == "OFFICER_APPROVED"
                ProposalDetailDialog(
                    proposal = selectedProposalDetailAdmin!!,
                    onDismiss = { selectedProposalDetailAdmin = null },
                    isOfficer = canAdminApprove,
                    onApprove = { adjustedDate, adjustedSalary, adjustedSalaryDate ->
                        viewModel.processProposal(
                            selectedProposalDetailAdmin!!.id,
                            isApproved = true,
                            rejectReason = null,
                            adjustedLeaveDate = adjustedDate,
                            adjustedProposedSalary = adjustedSalary,
                            adjustedSalaryEffectiveDate = adjustedSalaryDate
                        )
                        selectedProposalDetailAdmin = null
                    },
                    onReject = { reason ->
                        viewModel.processProposal(selectedProposalDetailAdmin!!.id, isApproved = false, rejectReason = reason)
                        selectedProposalDetailAdmin = null
                    }
                )
            }
        }
    }

    if (showAddUserDialog) {
        AddNewUserDialog(
            onDismiss = { showAddUserDialog = false },
            onSubmit = { cccd, fullName, role, password, assignedLocation ->
                viewModel.addNewUser(cccd, fullName, role, password, assignedLocation)
                showAddUserDialog = false
            }
        )
    }
}

@Composable
fun UserRoleItemCard(
    user: User,
    onRoleChange: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (user.role != "ADMIN") {
                        SubcomposeAsyncImage(
                            model = "https://i.ibb.co/fzhxZv5H/z7997808820598-c1f98aab385f7f198efebdd2a8b84471.jpg",
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 1.5.dp
                                    )
                                }
                            },
                            error = {
                                Box(
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = when (user.role) {
                                        "CAPTAIN" -> Icons.Default.Security
                                        "OFFICER" -> Icons.Default.SupervisorAccount
                                        "DISCIPLINE" -> Icons.Default.AdminPanelSettings
                                        else -> Icons.Default.Person
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "User Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Icon",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Column {
                        val displayName = if (user.fullName.isBlank()) {
                            "⚠️ Chưa thiết lập Họ & Tên"
                        } else {
                            user.fullName
                        }
                        val nameColor = if (user.fullName.isBlank()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = nameColor
                        )
                        Text(
                            text = "CCCD: ${user.cccd}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        val displayPassword = if (user.password.isBlank()) {
                            "Mật khẩu: Chưa tạo (Tự tạo khi đăng nhập)"
                        } else {
                            "Mật khẩu: ${user.password}"
                        }
                        val pwdColor = if (user.password.isBlank()) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = displayPassword,
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold),
                            color = pwdColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val displayLocation = when (user.role) {
                            "ADMIN" -> "Bộ phận: Quản trị hệ thống (Trụ sở chính)"
                            "OFFICER" -> "Bộ phận: Phòng Nghiệp vụ"
                            "DISCIPLINE" -> "Bộ phận: Phòng Điều lệnh"
                            else -> if (user.assignedLocation.isBlank()) {
                                "Mục tiêu quản lý: Chưa thiết lập"
                            } else {
                                "Mục tiêu: ${user.assignedLocation}"
                            }
                        }
                        Text(
                            text = displayLocation,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Chỉnh sửa", tint = MaterialTheme.colorScheme.primary)
                    }

                    if (user.cccd != "000000000000" && user.cccd != "087095015873") {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Text("Phân quyền vai trò trực tiếp:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))

            if (user.cccd == "000000000000" || user.cccd == "087095015873") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "👑 Quản trị viên Tối cao Toàn quyền",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Captain Role Toggle
                    ElevatedFilterChip(
                        selected = user.role == "CAPTAIN",
                        onClick = { onRoleChange("CAPTAIN") },
                        label = { Text("Đội Trưởng", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f)
                    )

                    // Officer Role Toggle
                    ElevatedFilterChip(
                        selected = user.role == "OFFICER",
                        onClick = { onRoleChange("OFFICER") },
                        label = { Text("Nghiệp Vụ", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1.1f)
                    )

                    // Discipline Officer Role Toggle
                    ElevatedFilterChip(
                        selected = user.role == "DISCIPLINE",
                        onClick = { onRoleChange("DISCIPLINE") },
                        label = { Text("Điều Lệnh", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1.1f)
                    )

                    // Admin Role Toggle
                    ElevatedFilterChip(
                        selected = user.role == "ADMIN",
                        onClick = { onRoleChange("ADMIN") },
                        label = { Text("Admin", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(0.9f)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// PROPOSAL LIST SECTION (SHARED UI REUSED ACROSS CAPTAIN, OFFICER, ADMIN)
// -----------------------------------------------------------------------------
@Composable
fun ProposalListSection(
    title: String,
    proposals: List<Proposal>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterType: String,
    onTypeChange: (String) -> Unit,
    filterStatus: String,
    onStatusChange: (String) -> Unit,
    onProposalClick: (Proposal) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        // Title and filters layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Tìm theo tên NV, người đề xuất...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Filtering Row (Leave vs Salary) & (Pending vs Active)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Type filters
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("ALL" to "Tất cả", "LEAVE" to "Phép/Nghỉ", "SALARY" to "Lương").forEach { (key, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (filterType == key) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onTypeChange(key) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (filterType == key) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Status Row filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "ALL" to "Tất cả Trạng thái",
                    "RECEIVED" to "Đã Tiếp nhận",
                    "APPROVED" to "Đã Phê duyệt",
                    "REJECTED" to "Từ chối"
                ).forEach { (key, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (filterStatus == key) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onStatusChange(key) }
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (filterStatus == key) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Lazy List of proposals
        if (proposals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AssignmentLate,
                        contentDescription = "Empty",
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Không tìm thấy đề xuất phù hợp",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(proposals) { proposal ->
                    ProposalItemCard(proposal = proposal, onClick = { onProposalClick(proposal) })
                }
            }
        }
    }
}

@Composable
fun ProposalItemCard(proposal: Proposal, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header: Type badge and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isLeave = proposal.type == "LEAVE"
                val typeLabel = if (isLeave) {
                    if (proposal.leaveType == "RESIGNATION") "Nghỉ việc" else "Nghỉ phép"
                } else {
                    "Điều chỉnh lương"
                }

                Surface(
                    color = if (isLeave) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLeave) Icons.Default.EventNote else Icons.Default.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isLeave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isLeave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                // Status Badge
                StatusBadge(status = proposal.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Employee name & proposer
            Text(
                text = "Nhân viên: ${proposal.employeeName}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đề xuất bởi: ${proposal.proposerName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                // If Leave/Resignation: Show live remaining days countdown badge!
                if (proposal.type == "LEAVE" && proposal.leaveDate != null) {
                    val daysRemaining = calculateDaysRemaining(proposal.leaveDate)
                    val countdownText = if (daysRemaining > 0) "Còn $daysRemaining ngày" else "Đến hạn nghỉ"
                    val countdownBg = if (daysRemaining <= 3) ErrorColor.copy(alpha = 0.15f) else SuccessColor.copy(alpha = 0.15f)
                    val countdownTint = if (daysRemaining <= 3) ErrorColor else SuccessColor

                    Surface(
                        color = countdownBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = "Timer", tint = countdownTint, modifier = Modifier.size(12.dp))
                            Text(
                                text = countdownText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = countdownTint
                            )
                        }
                    }
                }
            }

            // Reason truncated
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Lý do: ${proposal.reason}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Date submitted
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(proposal.timestamp))
                Text(
                    text = "Thời gian: $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Attachment, contentDescription = "Attached", modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Text(
                        text = if (proposal.type == "LEAVE") "Đơn đính kèm" else "Đề xuất số",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (label, bg, tint) = when (status) {
        "RECEIVED" -> Triple("Chờ Nghiệp vụ", WarningColor.copy(alpha = 0.15f), WarningColor)
        "OFFICER_APPROVED" -> Triple("Chờ Giám đốc", InfoColor.copy(alpha = 0.15f), InfoColor)
        "APPROVED" -> Triple("Đã Phê duyệt", SuccessColor.copy(alpha = 0.15f), SuccessColor)
        "REJECTED" -> Triple("Nghiệp vụ từ chối", ErrorColor.copy(alpha = 0.15f), ErrorColor)
        "ADMIN_REJECTED" -> Triple("Giám đốc từ chối", ErrorColor.copy(alpha = 0.15f), ErrorColor)
        else -> Triple("Không rõ", Color.LightGray, Color.Gray)
    }

    Surface(
        color = bg,
        shape = CircleShape
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = tint,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

fun calculateDaysRemaining(leaveDateStr: String?): Long {
    if (leaveDateStr.isNullOrEmpty()) return 0
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val leaveDate = sdf.parse(leaveDateStr) ?: return 0
        val today = sdf.parse(sdf.format(Date())) ?: return 0
        val diff = leaveDate.time - today.time
        val days = diff / (1000 * 60 * 60 * 24)
        if (days < 0) 0 else days
    } catch (e: Exception) {
        0
    }
}

fun calculateLeaveDurationDays(startDateStr: String?, endDateStr: String?): Long {
    if (startDateStr.isNullOrEmpty() || endDateStr.isNullOrEmpty()) return 0
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val start = sdf.parse(startDateStr) ?: return 0
        val end = sdf.parse(endDateStr) ?: return 0
        val diff = end.time - start.time
        val days = diff / (1000 * 60 * 60 * 24) + 1 // Inclusive
        if (days < 1) 1 else days
    } catch (e: Exception) {
        1
    }
}

// -----------------------------------------------------------------------------
// DYNAMIC MOCK RESIGNATION DOCUMENT COMPOSE DRAWINGS WITH STAMP & SIGNATURE
// -----------------------------------------------------------------------------
@Composable
fun ResignationLetterDoc(
    employeeName: String,
    leaveType: String,
    leaveDateStr: String,
    leaveEndDateStr: String? = null,
    reason: String,
    captainName: String,
    status: String,
    officerName: String?,
    rejectReason: String?,
    modifier: Modifier = Modifier
) {
    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val formattedLeaveDate = try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(leaveDateStr)
        date?.let { df.format(it) } ?: leaveDateStr
    } catch (e: Exception) {
        leaveDateStr
    }

    val formattedLeaveEndDate = try {
        if (!leaveEndDateStr.isNullOrEmpty()) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(leaveEndDateStr)
            date?.let { df.format(it) } ?: leaveEndDateStr
        } else null
    } catch (e: Exception) {
        leaveEndDateStr
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Grid-breaking notebook lines background decoration
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Official Header
                Text(
                    text = "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Độc lập - Tự do - Hạnh phúc",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "---------- o0o ----------",
                    color = Color.Gray,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                val docTitle = if (leaveType == "RESIGNATION") "ĐƠN XIN NGHỈ VIỆC" else "ĐƠN XIN NGHỈ PHÉP"
                Text(
                    text = docTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )

                // Show countdown inside the letter document itself
                val letterDaysRemaining = calculateDaysRemaining(leaveDateStr)
                val countdownBadgeText = if (letterDaysRemaining > 0) {
                    "⏳ (Đếm ngược: Còn $letterDaysRemaining ngày nữa đến ngày nghỉ)"
                } else {
                    "⌛ (Đã đến ngày nghỉ/thôi việc)"
                }
                Text(
                    text = countdownBadgeText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                    color = if (letterDaysRemaining <= 3) Color(0xFFEF4444) else Color(0xFF1E3A8A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Letter body text
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Kính gửi: Ban Giám đốc & Phòng Nghiệp vụ Công ty Bảo vệ Ngày & Đêm.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.DarkGray
                    )

                    Text(
                        text = "Tên tôi là: $employeeName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )

                    Text(
                        text = "Chức vụ: Nhân viên Bảo vệ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )

                    val actionText = if (leaveType == "RESIGNATION") "xin thôi việc kể từ ngày" else "xin nghỉ phép từ ngày"
                    val dateSpanText = if (leaveType == "LEAVE" && !formattedLeaveEndDate.isNullOrEmpty()) {
                        "Tôi làm đơn này kính trình Ban lãnh đạo xin nghỉ phép từ ngày: $formattedLeaveDate đến ngày: $formattedLeaveEndDate"
                    } else {
                        "Tôi làm đơn này kính trình Ban lãnh đạo $actionText: $formattedLeaveDate"
                    }
                    Text(
                        text = dateSpanText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )

                    if (leaveType == "LEAVE" && !leaveEndDateStr.isNullOrEmpty()) {
                        val duration = calculateLeaveDurationDays(leaveDateStr, leaveEndDateStr)
                        Text(
                            text = "Tổng số ngày nghỉ phép đề xuất: $duration ngày.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.DarkGray
                        )
                    }

                    Text(
                        text = "Lý do xin nghỉ: $reason",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )

                    Text(
                        text = "Tôi xin cam đoan sẽ bàn giao đầy đủ công cụ hỗ trợ và sổ sách trực ca tại mục tiêu bàn giao cho đội trưởng.",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Signatures row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Người làm đơn", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.DarkGray)
                        Text("(Ký, ghi rõ họ tên)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        // Draw a stylish handwriting font or custom canvas vector signature
                        Text(
                            text = employeeName.split(" ").lastOrNull() ?: employeeName,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E3A8A)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Xác nhận Đội trưởng", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.DarkGray)
                        Text("(Duyệt gửi hệ thống)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = captainName.split(" ").lastOrNull() ?: captainName,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E40AF)
                        )
                    }
                }
            }

            // RED OFFICIAL COMPANY STAMP overlay if Approved/Rejected!
            if (status == "APPROVED" || status == "REJECTED" || status == "ADMIN_REJECTED") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-30).dp, y = (-20).dp)
                        .size(110.dp)
                ) {
                    OfficialRedStamp(
                        statusText = if (status == "APPROVED") "ĐÃ DUYỆT" else "TỪ CHỐI",
                        officerName = officerName ?: "Nghiệp Vụ"
                    )
                }
            }
        }
    }
}

// Draw a beautiful circular traditional Vietnamese stamp in Canvas
@Composable
fun OfficialRedStamp(statusText: String, officerName: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        val stampRed = Color(0xFFEF4444)

        // Outer red stamp circle
        drawCircle(
            color = stampRed,
            radius = radius * 0.95f,
            style = Stroke(width = 3.dp.toPx())
        )

        // Inner circular separator
        drawCircle(
            color = stampRed,
            radius = radius * 0.7f,
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw a central star
        val starPath = Path().apply {
            val cx = center.x
            val cy = center.y
            val outerR = radius * 0.22f
            val innerR = radius * 0.09f
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) outerR else innerR
                val angle = (i * Math.PI / 5) - Math.PI / 2
                val x = (cx + r * Math.cos(angle)).toFloat()
                val y = (cy + r * Math.sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(path = starPath, color = stampRed)

        // We can draw curved text inside, but simplified: let's draw direct text on stamp
        // Representing official review stamps
    }

    // Direct texts stacked using Composable layering
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = "CÔNG TY BẢO VỆ",
                color = Color(0xFFEF4444),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                fontSize = 8.sp
            )
            Text(
                text = "NGÀY & ĐÊM",
                color = Color(0xFFEF4444),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = statusText,
                color = Color(0xFFEF4444),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                fontSize = 11.sp,
                modifier = Modifier
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(2.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
            Text(
                text = officerName.split(" ").lastOrNull() ?: "CB Nghiệp vụ",
                color = Color(0xFFEF4444),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic),
                fontSize = 8.sp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// SUBMIT LEAVE/RESIGNATION DIALOGUE
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitLeaveDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, type: String, startDate: String, endDate: String?, reason: String, sigPoints: List<Offset>, attachedImage: String?) -> Unit
) {
    var empName by remember { mutableStateOf("") }
    var leaveType by remember { mutableStateOf("LEAVE") } // "LEAVE" or "RESIGNATION"
    var leaveDate by remember { mutableStateOf("") } // YYYY-MM-DD
    var leaveEndDate by remember { mutableStateOf("") } // YYYY-MM-DD
    var reason by remember { mutableStateOf("") }

    // Attached Image State
    var attachedImageUri by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri.toString()
        }
    }

    // Camera capture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val cacheFile = File(context.cacheDir, "captured_resignation_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(cacheFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
                attachedImageUri = Uri.fromFile(cacheFile).toString()
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Lỗi lưu ảnh: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission launcher for Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Không thể mở camera: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(context, "Cần cấp quyền Camera để chụp ảnh đơn", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Multi-stroke signature capture points
    var currentPathPoints = remember { mutableStateOf<List<Offset>>(emptyList()) }
    val completedPaths = remember { mutableStateListOf<List<Offset>>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "TẠO ĐỀ XUẤT NGHỈ PHÉP/VIỆC",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Employee Name
                OutlinedTextField(
                    value = empName,
                    onValueChange = { empName = it },
                    label = { Text("Họ và Tên Nhân viên Xin nghỉ") },
                    placeholder = { Text("Ví dụ: Nguyễn Văn C") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_name_leave"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Leave Type toggle
                Text("Chọn Loại Vận Hành:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("LEAVE" to "Nghỉ Phép", "RESIGNATION" to "Nghỉ Việc").forEach { (type, label) ->
                        ElevatedFilterChip(
                            selected = leaveType == type,
                            onClick = { leaveType = type },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Leave Date with Calendar Picker
                val context = LocalContext.current
                val localizedContext = remember(context) {
                    val locale = java.util.Locale("vi", "VN")
                    java.util.Locale.setDefault(locale)
                    
                    // Unwrap context to find the Activity context to avoid BadTokenException
                    var baseContext = context
                    while (baseContext is android.content.ContextWrapper) {
                        if (baseContext is android.app.Activity) break
                        baseContext = baseContext.baseContext
                    }
                    
                    val config = android.content.res.Configuration(baseContext.resources.configuration)
                    config.setLocale(locale)
                    val wrapper = android.view.ContextThemeWrapper(baseContext, 0)
                    wrapper.applyOverrideConfiguration(config)
                    wrapper
                }
                val calendar = Calendar.getInstance()
                
                // Parse existing leaveDate if possible, to start the picker at that date
                val initialYear: Int
                val initialMonth: Int
                val initialDay: Int
                if (leaveDate.trim().length == 10 && leaveDate.contains("-")) {
                    val parts = leaveDate.split("-")
                    initialYear = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR)
                    initialMonth = (parts.getOrNull(1)?.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1
                    initialDay = parts.getOrNull(2)?.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
                } else {
                    initialYear = calendar.get(Calendar.YEAR)
                    initialMonth = calendar.get(Calendar.MONTH)
                    initialDay = calendar.get(Calendar.DAY_OF_MONTH)
                }

                val datePickerDialog = remember(localizedContext) {
                    android.app.DatePickerDialog(
                        localizedContext,
                        { _, year, monthOfYear, dayOfMonth ->
                            val formattedMonth = String.format("%02d", monthOfYear + 1)
                            val formattedDay = String.format("%02d", dayOfMonth)
                            leaveDate = "$year-$formattedMonth-$formattedDay"
                        },
                        initialYear,
                        initialMonth,
                        initialDay
                    )
                }

                val endDatePickerDialog = remember(localizedContext) {
                    android.app.DatePickerDialog(
                        localizedContext,
                        { _, year, monthOfYear, dayOfMonth ->
                            val formattedMonth = String.format("%02d", monthOfYear + 1)
                            val formattedDay = String.format("%02d", dayOfMonth)
                            leaveEndDate = "$year-$formattedMonth-$formattedDay"
                        },
                        initialYear,
                        initialMonth,
                        initialDay
                    )
                }

                // Update datePickerDialog's date when initial changes or field changes
                LaunchedEffect(leaveDate) {
                    if (leaveDate.trim().length == 10 && leaveDate.contains("-")) {
                        val parts = leaveDate.split("-")
                        val y = parts.getOrNull(0)?.toIntOrNull()
                        val m = parts.getOrNull(1)?.toIntOrNull()
                        val d = parts.getOrNull(2)?.toIntOrNull()
                        if (y != null && m != null && d != null) {
                            datePickerDialog.updateDate(y, m - 1, d)
                        }
                    }
                }

                // Update endDatePickerDialog's date when initial changes or field changes
                LaunchedEffect(leaveEndDate) {
                    if (leaveEndDate.trim().length == 10 && leaveEndDate.contains("-")) {
                        val parts = leaveEndDate.split("-")
                        val y = parts.getOrNull(0)?.toIntOrNull()
                        val m = parts.getOrNull(1)?.toIntOrNull()
                        val d = parts.getOrNull(2)?.toIntOrNull()
                        if (y != null && m != null && d != null) {
                            endDatePickerDialog.updateDate(y, m - 1, d)
                        }
                    }
                }

                val startLabel = if (leaveType == "RESIGNATION") "Ngày thôi việc dự kiến" else "Ngày bắt đầu nghỉ phép"
                val startPlaceholder = if (leaveType == "RESIGNATION") "Nhấp chọn ngày thôi việc" else "Nhấp chọn ngày bắt đầu nghỉ"

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = leaveDate,
                        onValueChange = { },
                        label = { Text(startLabel) },
                        placeholder = { Text(startPlaceholder) },
                        singleLine = true,
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select Start Date",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Open Start Calendar Picker",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("leave_date_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    // Transparent clickable overlay that intercepts all clicks
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { datePickerDialog.show() }
                    )
                }

                if (leaveType == "LEAVE") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = leaveEndDate,
                            onValueChange = { },
                            label = { Text("Ngày kết thúc nghỉ phép") },
                            placeholder = { Text("Nhấp chọn ngày kết thúc nghỉ") },
                            singleLine = true,
                            readOnly = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Select End Date",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Open End Calendar Picker",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("leave_end_date_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { endDatePickerDialog.show() }
                        )
                    }

                    // Display expected leave duration days if both start and end dates are specified
                    if (leaveDate.trim().length == 10 && leaveEndDate.trim().length == 10) {
                        val duration = calculateLeaveDurationDays(leaveDate.trim(), leaveEndDate.trim())
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Duration",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Số ngày nghỉ phép dự kiến: $duration ngày",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // Live countdown display
                val parsedDays = remember(leaveDate) {
                    if (leaveDate.trim().length == 10) {
                        calculateDaysRemaining(leaveDate.trim())
                    } else {
                        -1L
                    }
                }
                if (parsedDays >= 0) {
                    val label = if (leaveType == "RESIGNATION") {
                        "Đếm ngược: Còn $parsedDays ngày nữa sẽ đến ngày xin thôi việc."
                    } else {
                        "Đếm ngược: Còn $parsedDays ngày nữa sẽ đến ngày bắt đầu nghỉ phép."
                    }
                    val badgeBg = if (parsedDays <= 3) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    val badgeText = if (parsedDays <= 3) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = badgeBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Countdown",
                                tint = badgeText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = badgeText
                            )
                        }
                    }
                }

                // Reason for Leave
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Lý do xin nghỉ phép / nghỉ việc") },
                    placeholder = { Text("Nhập lý do chi tiết để Cán bộ duyệt...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )

                // Attached document section
                if (leaveType == "RESIGNATION") {
                    Text(
                        text = "Đính kèm hình ảnh đơn nghỉ việc (Tùy chọn):",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (attachedImageUri == null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Upload",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Chưa có hình ảnh đơn được đính kèm",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Hãy chọn ảnh từ thư viện hoặc chụp ảnh để đính kèm đơn xin nghỉ việc",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(Color.Black, RoundedCornerShape(8.dp))
                                ) {
                                    SubcomposeAsyncImage(
                                        model = attachedImageUri,
                                        contentDescription = "Attached Document Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        loading = {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(strokeWidth = 2.dp)
                                            }
                                        }
                                    )
                                    // Delete/Clear button
                                    FloatingActionButton(
                                        onClick = { attachedImageUri = null },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(36.dp),
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = Color.White,
                                        shape = CircleShape
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove attached image", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            galleryLauncher.launch("image/*")
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            android.widget.Toast.makeText(context, "Không thể mở thư viện: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Photo, contentDescription = "Gallery", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Thư viện", style = MaterialTheme.typography.labelMedium)
                                }

                                Button(
                                    onClick = {
                                        val cameraPermission = android.Manifest.permission.CAMERA
                                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(context, cameraPermission) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) {
                                            try {
                                                cameraLauncher.launch(null)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                android.widget.Toast.makeText(context, "Không thể mở camera: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            cameraPermissionLauncher.launch(cameraPermission)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Chụp ảnh", style = MaterialTheme.typography.labelMedium)
                                }

                            }
                        }
                    }
                }

                // Signature Panel - interactive canvas
                Text(
                    text = "Chữ ký nhân viên (Đính kèm đơn trực tuyến):",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPathPoints.value = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    val newPoints = currentPathPoints.value.toMutableList()
                                    newPoints.add(change.position)
                                    currentPathPoints.value = newPoints
                                },
                                onDragEnd = {
                                    if (currentPathPoints.value.isNotEmpty()) {
                                        completedPaths.add(currentPathPoints.value)
                                        currentPathPoints.value = emptyList()
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw all completed stroke paths
                        completedPaths.forEach { stroke ->
                            if (stroke.size > 1) {
                                for (i in 0 until stroke.size - 1) {
                                    drawLine(
                                        color = Color(0xFF1E3A8A),
                                        start = stroke[i],
                                        end = stroke[i + 1],
                                        strokeWidth = 3.dp.toPx()
                                    )
                                }
                            }
                        }
                        // Draw current drawing stroke path
                        val currentStroke = currentPathPoints.value
                        if (currentStroke.size > 1) {
                            for (i in 0 until currentStroke.size - 1) {
                                drawLine(
                                    color = Color(0xFF1E3A8A),
                                    start = currentStroke[i],
                                    end = currentStroke[i + 1],
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                        }
                    }

                    if (completedPaths.isEmpty() && currentPathPoints.value.isEmpty()) {
                        Text(
                            text = "Ký tên bằng ngón tay vào đây...",
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
                        )
                    }

                    // Clear signature button
                    IconButton(
                        onClick = { completedPaths.clear() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Sig", tint = Color.Gray)
                    }
                }

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("HỦY")
                    }

                    Button(
                        onClick = {
                            if (empName.isNotBlank() && leaveDate.isNotBlank() && (leaveType == "RESIGNATION" || leaveEndDate.isNotBlank()) && reason.isNotBlank()) {
                                val allPointsCollected = completedPaths.flatten()
                                onSubmit(
                                    empName,
                                    leaveType,
                                    leaveDate,
                                    if (leaveType == "LEAVE") leaveEndDate else null,
                                    reason,
                                    allPointsCollected,
                                    if (leaveType == "RESIGNATION") attachedImageUri else null
                                )
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        enabled = empName.isNotBlank() && leaveDate.isNotBlank() && (leaveType == "RESIGNATION" || leaveEndDate.isNotBlank()) && reason.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("GỬI PHÊ DUYỆT")
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// SUBMIT SALARY PROPOSAL DIALOGUE
// -----------------------------------------------------------------------------
data class SalaryAdjustmentRequest(
    val employeeName: String,
    val currentSalary: Double,
    val proposedSalary: Double,
    val salaryEffectiveDate: String
)

data class SalaryEntryState(
    val id: Int,
    val name: String = "",
    val currentSalary: String = "",
    val proposedSalary: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitSalaryDialog(
    onDismiss: () -> Unit,
    onSubmitMultiple: (entries: List<SalaryAdjustmentRequest>, reason: String) -> Unit
) {
    var entries by remember { mutableStateOf(listOf(SalaryEntryState(id = 1))) }
    var nextId by remember { mutableStateOf(2) }
    var reason by remember { mutableStateOf("") }
    
    val calendar = Calendar.getInstance()
    val todayMonthStr = remember {
        val y = calendar.get(Calendar.YEAR)
        val m = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        "$y-$m"
    }
    var effectiveDate by remember { mutableStateOf(todayMonthStr) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val isFormValid = reason.isNotBlank() && effectiveDate.isNotBlank() && entries.isNotEmpty() && entries.all { entry ->
        entry.name.isNotBlank() &&
        (entry.currentSalary.toDoubleOrNull() ?: 0.0) > 0.0 &&
        (entry.proposedSalary.toDoubleOrNull() ?: 0.0) > 0.0
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ĐỀ XUẤT ĐIỀU CHỈNH LƯƠNG",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Subtitle / Guide
                Text(
                    text = "Nhập thông tin điều chỉnh lương cho một hoặc nhiều nhân viên cùng lúc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // List of Employee Rows
                entries.forEachIndexed { index, entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nhân viên #${index + 1}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (entries.size > 1) {
                                    IconButton(
                                        onClick = {
                                            entries = entries.filter { it.id != entry.id }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Xóa nhân viên này",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Employee Name
                            OutlinedTextField(
                                value = entry.name,
                                onValueChange = { newName ->
                                    entries = entries.map {
                                        if (it.id == entry.id) it.copy(name = newName) else it
                                    }
                                },
                                label = { Text("Họ và Tên Nhân viên Bảo vệ") },
                                placeholder = { Text("Ví dụ: Nguyễn Văn C") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("employee_name_salary_${index}"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Current Salary
                                OutlinedTextField(
                                    value = entry.currentSalary,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() }) {
                                            entries = entries.map {
                                                if (it.id == entry.id) it.copy(currentSalary = input) else it
                                            }
                                        }
                                    },
                                    label = { Text("Lương cũ (VNĐ)") },
                                    placeholder = { Text("8000000") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("current_salary_input_${index}"),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                // Proposed Salary
                                OutlinedTextField(
                                    value = entry.proposedSalary,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() }) {
                                            entries = entries.map {
                                                if (it.id == entry.id) it.copy(proposedSalary = input) else it
                                            }
                                        }
                                    },
                                    label = { Text("Lương mới (VNĐ)") },
                                    placeholder = { Text("9500000") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("proposed_salary_input_${index}"),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }

                // Add Row button
                OutlinedButton(
                    onClick = {
                        entries = entries + SalaryEntryState(id = nextId)
                        nextId++
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm nhân viên", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("THÊM NHÂN VIÊN ĐIỀU CHỈNH")
                }

                // Proposed Effective Date (tháng đề xuất áp dụng mức lương mới)
                val initialYear: Int
                val initialMonth: Int
                if (effectiveDate.contains("-")) {
                    val parts = effectiveDate.split("-")
                    initialYear = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR)
                    initialMonth = parts.getOrNull(1)?.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)
                } else {
                    initialYear = calendar.get(Calendar.YEAR)
                    initialMonth = calendar.get(Calendar.MONTH) + 1
                }

                if (showMonthPicker) {
                    MonthYearPickerDialog(
                        initialYear = initialYear,
                        initialMonth = initialMonth,
                        onDismiss = { showMonthPicker = false },
                        onDateSelected = { year, month ->
                            val formattedMonth = String.format("%02d", month)
                            effectiveDate = "$year-$formattedMonth"
                            showMonthPicker = false
                        }
                    )
                }

                val displayEffectiveDate = try {
                    if (effectiveDate.contains("-")) {
                        val parts = effectiveDate.split("-")
                        val y = parts.getOrNull(0) ?: ""
                        val m = parts.getOrNull(1) ?: ""
                        if (y.isNotEmpty() && m.isNotEmpty()) "Tháng $m/$y" else effectiveDate
                    } else {
                        effectiveDate
                    }
                } catch (e: Exception) {
                    effectiveDate
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = displayEffectiveDate,
                        onValueChange = { },
                        label = { Text("Tháng đề xuất áp dụng mức lương mới") },
                        placeholder = { Text("Nhấp chọn tháng áp dụng") },
                        singleLine = true,
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select Month",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Open Month Picker",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("salary_effective_date_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showMonthPicker = true }
                    )
                }

                // Reason for Salary adjustment
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Lý do điều chỉnh lương chung") },
                    placeholder = { Text("Nêu lý do chi tiết (ví dụ: Đạt hiệu quả trực xuất sắc tại mục tiêu, Đảm nhiệm thêm ca trực...)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("HỦY")
                    }

                    Button(
                        onClick = {
                            if (isFormValid) {
                                val mapped = entries.map {
                                    SalaryAdjustmentRequest(
                                        employeeName = it.name.trim(),
                                        currentSalary = it.currentSalary.toDoubleOrNull() ?: 0.0,
                                        proposedSalary = it.proposedSalary.toDoubleOrNull() ?: 0.0,
                                        salaryEffectiveDate = effectiveDate
                                    )
                                }
                                onSubmitMultiple(mapped, reason)
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        enabled = isFormValid,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("GỬI ĐỀ XUẤT")
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TIMELINE COMPONENT FOR SALARY PROPOSAL APPROVALS (CAPTAIN, OFFICER, ADMIN)
// -----------------------------------------------------------------------------
enum class StepState {
    COMPLETED, PENDING, REJECTED, DISABLED
}

@Composable
fun TimelineStep(
    title: String,
    statusText: String,
    descText: String,
    state: StepState,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon / Line Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            val (icon, color) = when (state) {
                StepState.COMPLETED -> Icons.Default.CheckCircle to SuccessColor
                StepState.PENDING -> Icons.Default.HourglassEmpty to WarningColor
                StepState.REJECTED -> Icons.Default.Cancel to ErrorColor
                StepState.DISABLED -> Icons.Default.RadioButtonUnchecked to Color.Gray
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            if (!isLast) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(color.copy(alpha = 0.5f))
                )
            }
        }

        // Info Column
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (state == StepState.DISABLED) Color.Gray else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = when (state) {
                    StepState.COMPLETED -> SuccessColor
                    StepState.PENDING -> WarningColor
                    StepState.REJECTED -> ErrorColor
                    StepState.DISABLED -> Color.Gray
                }
            )
            if (descText.isNotBlank()) {
                Text(
                    text = descText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ProposalApprovalTimeline(proposal: Proposal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "TIẾN TRÌNH PHÊ DUYỆT ĐỀ XUẤT",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary
            )

            // Step 1: Initiated by Captain
            TimelineStep(
                title = "Bước 1: Đội trưởng đề xuất",
                statusText = "Đã gửi đề xuất",
                descText = "Đội trưởng: ${proposal.proposerName}",
                state = StepState.COMPLETED,
                isLast = false
            )

            // Step 2: Officer Approval
            val step2State = when (proposal.status) {
                "RECEIVED" -> StepState.PENDING
                "OFFICER_APPROVED", "APPROVED", "ADMIN_REJECTED" -> StepState.COMPLETED
                "REJECTED" -> StepState.REJECTED
                else -> StepState.PENDING
            }
            val step2StatusText = when (proposal.status) {
                "RECEIVED" -> "Đang chờ Nghiệp vụ duyệt"
                "OFFICER_APPROVED", "APPROVED", "ADMIN_REJECTED" -> "Nghiệp vụ đã duyệt"
                "REJECTED" -> "Nghiệp vụ đã từ chối"
                else -> "Chờ xử lý"
            }
            val step2Desc = if (proposal.officerName != null) {
                if (proposal.status == "REJECTED" && !proposal.rejectReason.isNullOrBlank()) {
                    "Cán bộ: ${proposal.officerName}\nLý do từ chối: ${proposal.rejectReason}"
                } else {
                    "Cán bộ: ${proposal.officerName}"
                }
            } else {
                "Chờ phân phối hồ sơ"
            }
            TimelineStep(
                title = "Bước 2: Cán bộ phòng Nghiệp vụ",
                statusText = step2StatusText,
                descText = step2Desc,
                state = step2State,
                isLast = false
            )

            // Step 3: Admin / Director Approval
            val step3State = when (proposal.status) {
                "RECEIVED" -> StepState.DISABLED
                "OFFICER_APPROVED" -> StepState.PENDING
                "APPROVED" -> StepState.COMPLETED
                "ADMIN_REJECTED" -> StepState.REJECTED
                "REJECTED" -> StepState.DISABLED
                else -> StepState.DISABLED
            }
            val step3StatusText = when (proposal.status) {
                "RECEIVED" -> "Chưa đến lượt (Chờ bước 2)"
                "OFFICER_APPROVED" -> "Đang chờ Giám đốc duyệt"
                "APPROVED" -> "Giám đốc đã phê duyệt"
                "ADMIN_REJECTED" -> "Giám đốc đã từ chối"
                "REJECTED" -> "Hồ sơ đã bị từ chối ở bước 2"
                else -> "Chờ xử lý"
            }
            val step3Desc = when (proposal.status) {
                "APPROVED" -> "Đơn đề xuất lương đã được áp dụng"
                "ADMIN_REJECTED" -> if (!proposal.rejectReason.isNullOrBlank()) "Lý do: ${proposal.rejectReason}" else "Giám đốc từ chối phê duyệt"
                "OFFICER_APPROVED" -> "Hồ sơ đang ở bàn làm việc Giám đốc"
                else -> ""
            }
            TimelineStep(
                title = "Bước 3: Giám đốc (Admin) phê duyệt",
                statusText = step3StatusText,
                descText = step3Desc,
                state = step3State,
                isLast = true
            )
        }
    }
}

// -----------------------------------------------------------------------------
// DETAIL VIEWER AND REVIEW DIALOGUE (OFFICER ACTION SUPPORT)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalDetailDialog(
    proposal: Proposal,
    onDismiss: () -> Unit,
    isOfficer: Boolean,
    onApprove: (adjustedDate: String?, adjustedProposedSalary: Double?, adjustedSalaryEffectiveDate: String?) -> Unit,
    onReject: (String) -> Unit
) {
    var rejectReason by remember { mutableStateOf("") }
    var showRejectReasonInput by remember { mutableStateOf(false) }
    var adjustedLeaveDate by remember { mutableStateOf(proposal.leaveDate ?: "") }
    var adjustedProposedSalary by remember { mutableStateOf(proposal.proposedSalary?.toLong()?.toString() ?: "") }
    var adjustedSalaryEffectiveDate by remember { mutableStateOf(proposal.salaryEffectiveDate ?: "") }
    var showAdjustedSalaryMonthPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header of detail
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CHI TIẾT ĐỀ XUẤT",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider()

                // Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Proposer Information
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = "Proposer", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Người Đề Xuất: ${proposal.proposerName}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text("Số CCCD Đội Trưởng: ${proposal.proposerCccd}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                        Spacer(modifier = Modifier.height(4.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(4.dp))

                        // Target employee
                        Text(
                            text = "Nhân viên: ${proposal.employeeName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )

                        // If Leave type, show remaining days
                        if (proposal.type == "LEAVE" && proposal.leaveDate != null) {
                            val daysRemaining = calculateDaysRemaining(proposal.leaveDate)
                            val isLeaveNotResign = proposal.leaveType != "RESIGNATION"
                            val leaveLabel = if (isLeaveNotResign) "Bắt đầu nghỉ phép" else "Ngày thôi việc dự kiến"
                            val countdownDesc = if (daysRemaining > 0) "CÒN LẠI: $daysRemaining NGÀY" else "ĐÃ ĐẾN HẠN NGHỈ"
                            val accentColor = if (daysRemaining <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassBottom,
                                        contentDescription = "Timer",
                                        tint = accentColor
                                    )
                                    Text(
                                        text = "$leaveLabel: ${proposal.leaveDate} ($countdownDesc)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold, color = accentColor)
                                    )
                                }
                                if (isLeaveNotResign && proposal.leaveEndDate != null) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = "End Date",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Kết thúc nghỉ phép: ${proposal.leaveEndDate}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        )
                                    }
                                    
                                    val duration = calculateLeaveDurationDays(proposal.leaveDate, proposal.leaveEndDate)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = "Duration",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Tổng số ngày nghỉ: $duration ngày",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                        )
                                    }
                                }
                            }
                        }

                        // If Salary adjustment type, show current and proposed salary side by side
                        if (proposal.type == "SALARY" && proposal.currentSalary != null && proposal.proposedSalary != null) {
                            val formatter = DecimalFormat("#,###")
                            val currStr = formatter.format(proposal.currentSalary) + " VNĐ"
                            val propStr = formatter.format(proposal.proposedSalary) + " VNĐ"
                            val diffStr = "+" + formatter.format(proposal.proposedSalary - proposal.currentSalary) + " VNĐ"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Lương hiện tại", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(currStr, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }

                                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Arrow", tint = Color.LightGray)

                                Column {
                                    Text("Lương đề xuất mới", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(propStr, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = SuccessColor))
                                }
                            }

                            Surface(
                                color = SuccessColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Mức chênh lệch tăng: $diffStr",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = SuccessColor),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            proposal.salaryEffectiveDate?.let { effDate ->
                                val formattedDate = try {
                                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(effDate)
                                    date?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: effDate
                                } catch (e: Exception) {
                                    effDate
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Effective Date",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Ngày đề xuất áp dụng: $formattedDate",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Lý do đề xuất: ${proposal.reason}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // If LEAVE, show document view representing the letter
                if (proposal.type == "LEAVE") {
                    Text(
                        text = "ĐƠN ĐÍNH KÈM HỆ THỐNG:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    ResignationLetterDoc(
                        employeeName = proposal.employeeName,
                        leaveType = proposal.leaveType ?: "LEAVE",
                        leaveDateStr = proposal.leaveDate ?: "",
                        leaveEndDateStr = proposal.leaveEndDate,
                        reason = proposal.reason,
                        captainName = proposal.proposerName,
                        status = proposal.status,
                        officerName = proposal.officerName,
                        rejectReason = proposal.rejectReason,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // If an image was uploaded/attached, display it with zoom option
                    if (!proposal.imagePath.isNullOrBlank() && proposal.imagePath.startsWith("http")) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "HÌNH ẢNH ĐƠN ĐÍNH KÈM THỰC TẾ:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        var isZoomed by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isZoomed = true }
                                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                SubcomposeAsyncImage(
                                    model = proposal.imagePath,
                                    contentDescription = "Attached Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                    loading = {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(strokeWidth = 2.dp)
                                        }
                                    }
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "🔍 Chạm để phóng to đơn",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (isZoomed) {
                            Dialog(onDismissRequest = { isZoomed = false }) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "CHI TIẾT ẢNH ĐƠN ĐÍNH KÈM",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                            )
                                            IconButton(
                                                onClick = { isZoomed = false },
                                                modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                                            ) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(400.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SubcomposeAsyncImage(
                                                model = proposal.imagePath,
                                                contentDescription = "Zoomed Attached Image",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // If SALARY proposal, show the gorgeous multi-step progress timeline
                if (proposal.type == "SALARY") {
                    ProposalApprovalTimeline(proposal = proposal)
                } else if (proposal.status != "RECEIVED") {
                    // Original review feedback card for leave proposals
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (proposal.status == "APPROVED") SuccessColor.copy(alpha = 0.08f) else ErrorColor.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, if (proposal.status == "APPROVED") SuccessColor.copy(alpha = 0.4f) else ErrorColor.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (proposal.status == "APPROVED") Icons.Default.VerifiedUser else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (proposal.status == "APPROVED") SuccessColor else ErrorColor
                                )
                                Text(
                                    text = if (proposal.status == "APPROVED") "ĐÃ PHÊ DUYỆT" else "BỊ TỪ CHỐI",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                                    color = if (proposal.status == "APPROVED") SuccessColor else ErrorColor
                                )
                            }
                            Text(
                                text = "Xử lý bởi: ${proposal.officerName ?: "Cán bộ phòng"}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (proposal.status == "REJECTED" && !proposal.rejectReason.isNullOrBlank()) {
                                Text(
                                    text = "Lý do từ chối: ${proposal.rejectReason}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ErrorColor
                                )
                            }
                        }
                    }
                }

                // If logged in as OFFICER/ADMIN and state is PENDING, show Action workflows!
                if (isOfficer && (proposal.status == "RECEIVED" || proposal.status == "OFFICER_APPROVED")) {
                    if (proposal.type == "LEAVE" && proposal.status == "RECEIVED" && !showRejectReasonInput) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Phê duyệt ngày khác đề xuất (nếu có)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Mặc định lấy ngày đề xuất của Đội trưởng (${proposal.leaveDate}). Cán bộ có thể nhấp chọn ngày khác nếu cần điều chỉnh ngày chính thức.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                val context = LocalContext.current
                                val calendar = Calendar.getInstance()
                                val initialYear: Int
                                val initialMonth: Int
                                val initialDay: Int
                                if (adjustedLeaveDate.trim().length == 10 && adjustedLeaveDate.contains("-")) {
                                    val parts = adjustedLeaveDate.split("-")
                                    initialYear = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR)
                                    initialMonth = (parts.getOrNull(1)?.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1
                                    initialDay = parts.getOrNull(2)?.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
                                } else {
                                    initialYear = calendar.get(Calendar.YEAR)
                                    initialMonth = calendar.get(Calendar.MONTH)
                                    initialDay = calendar.get(Calendar.DAY_OF_MONTH)
                                }

                                val datePickerDialog = remember(context) {
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, monthOfYear, dayOfMonth ->
                                            val formattedMonth = String.format("%02d", monthOfYear + 1)
                                            val formattedDay = String.format("%02d", dayOfMonth)
                                            adjustedLeaveDate = "$year-$formattedMonth-$formattedDay"
                                        },
                                        initialYear,
                                        initialMonth,
                                        initialDay
                                    )
                                }

                                LaunchedEffect(adjustedLeaveDate) {
                                    if (adjustedLeaveDate.trim().length == 10 && adjustedLeaveDate.contains("-")) {
                                        val parts = adjustedLeaveDate.split("-")
                                        val y = parts.getOrNull(0)?.toIntOrNull()
                                        val m = parts.getOrNull(1)?.toIntOrNull()
                                        val d = parts.getOrNull(2)?.toIntOrNull()
                                        if (y != null && m != null && d != null) {
                                            datePickerDialog.updateDate(y, m - 1, d)
                                        }
                                    }
                                }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = adjustedLeaveDate,
                                        onValueChange = { },
                                        label = { Text("Ngày phê duyệt quyết định") },
                                        placeholder = { Text("Nhấp chọn ngày") },
                                        singleLine = true,
                                        readOnly = true,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = "Select Date",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = "Open Calendar Picker",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("officer_adjusted_date_input"),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { datePickerDialog.show() }
                                    )
                                }
                            }
                        }
                    }

                    if (proposal.type == "SALARY" && !showRejectReasonInput) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Phê duyệt mức lương & tháng áp dụng khác (nếu có)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val formattedOriginalDate = try {
                                    val effDate = proposal.salaryEffectiveDate ?: ""
                                    if (effDate.length == 7 && effDate.contains("-")) {
                                        val parts = effDate.split("-")
                                        "Tháng ${parts.getOrNull(1)}/${parts.getOrNull(0)}"
                                    } else {
                                        effDate
                                    }
                                } catch(e: Exception) {
                                    proposal.salaryEffectiveDate ?: ""
                                }
                                Text(
                                    text = "Mặc định lấy mức lương đề xuất (${proposal.proposedSalary?.let { DecimalFormat("#,###").format(it) } ?: ""} VNĐ) và tháng áp dụng ($formattedOriginalDate). Cán bộ có thể điều chỉnh lại bên dưới.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Salary Input
                                OutlinedTextField(
                                    value = adjustedProposedSalary,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() }) {
                                            adjustedProposedSalary = input
                                        }
                                    },
                                    label = { Text("Mức lương quyết định (VNĐ)") },
                                    placeholder = { Text("Ví dụ: 9500000") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("officer_adjusted_salary_input"),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Month/Year Picker instead of DatePickerDialog
                                val calendar = Calendar.getInstance()
                                val initialYear: Int
                                val initialMonth: Int
                                if (adjustedSalaryEffectiveDate.contains("-")) {
                                    val parts = adjustedSalaryEffectiveDate.split("-")
                                    initialYear = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR)
                                    initialMonth = parts.getOrNull(1)?.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)
                                } else {
                                    initialYear = calendar.get(Calendar.YEAR)
                                    initialMonth = calendar.get(Calendar.MONTH) + 1
                                }

                                if (showAdjustedSalaryMonthPicker) {
                                    MonthYearPickerDialog(
                                        initialYear = initialYear,
                                        initialMonth = initialMonth,
                                        onDismiss = { showAdjustedSalaryMonthPicker = false },
                                        onDateSelected = { year, month ->
                                            val formattedMonth = String.format("%02d", month)
                                            adjustedSalaryEffectiveDate = "$year-$formattedMonth"
                                            showAdjustedSalaryMonthPicker = false
                                        }
                                    )
                                }

                                val displayAdjustedDate = try {
                                    if (adjustedSalaryEffectiveDate.contains("-")) {
                                        val parts = adjustedSalaryEffectiveDate.split("-")
                                        val y = parts.getOrNull(0) ?: ""
                                        val m = parts.getOrNull(1) ?: ""
                                        if (y.isNotEmpty() && m.isNotEmpty()) {
                                            if (parts.size == 3) {
                                                // If it is old yyyy-MM-dd format, display dd/MM/yyyy
                                                val d = parts.getOrNull(2) ?: ""
                                                "$d/$m/$y"
                                            } else {
                                                "Tháng $m/$y"
                                            }
                                        } else {
                                            adjustedSalaryEffectiveDate
                                        }
                                    } else {
                                        adjustedSalaryEffectiveDate
                                    }
                                } catch (e: Exception) {
                                    adjustedSalaryEffectiveDate
                                }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = displayAdjustedDate,
                                        onValueChange = { },
                                        label = { Text("Tháng áp dụng quyết định") },
                                        placeholder = { Text("Nhấp chọn tháng áp dụng") },
                                        singleLine = true,
                                        readOnly = true,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = "Select Month",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = "Open Month Picker",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("officer_adjusted_salary_date_input"),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { showAdjustedSalaryMonthPicker = true }
                                    )
                                }
                            }
                        }
                    }

                    if (!showRejectReasonInput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showRejectReasonInput = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("reject_proposal_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Cancel, contentDescription = "Reject")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("TỪ CHỐI", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            Button(
                                onClick = {
                                    if (proposal.type == "SALARY") {
                                        onApprove(
                                            null,
                                            adjustedProposedSalary.toDoubleOrNull(),
                                            adjustedSalaryEffectiveDate
                                        )
                                    } else {
                                        onApprove(adjustedLeaveDate, null, null)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(50.dp)
                                    .testTag("approve_proposal_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Approve")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PHÊ DUYỆT", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    } else {
                        // Prompt for Reject Reason
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = rejectReason,
                                onValueChange = { rejectReason = it },
                                label = { Text("Lý do từ chối giải quyết") },
                                placeholder = { Text("Kính trình lý do từ chối (bắt buộc)...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reject_reason_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showRejectReasonInput = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("QUAY LẠI")
                                }

                                Button(
                                    onClick = {
                                        if (rejectReason.trim().isNotBlank()) {
                                            onReject(rejectReason)
                                        }
                                    },
                                    modifier = Modifier.weight(1.5f),
                                    enabled = rejectReason.trim().isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("XÁC NHẬN TỪ CHỐI", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ADD NEW PRECONFIGURED USER DIALOGUE (ADMIN FLOW)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewUserDialog(
    onDismiss: () -> Unit,
    onSubmit: (cccd: String, fullName: String, role: String, password: String, assignedLocation: String) -> Unit
) {
    var cccd by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var assignedLocation by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("nds123") }
    var role by remember { mutableStateOf("CAPTAIN") }

    val isFormValid = cccd.length == 12 && fullName.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "THÊM & PHÂN QUYỀN TÀI KHOẢN",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = "Nhập thông tin nhân viên mới để cấp quyền đăng nhập hệ thống.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // CCCD
                OutlinedTextField(
                    value = cccd,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 12) cccd = input
                    },
                    label = { Text("Số CCCD (12 chữ số) *") },
                    placeholder = { Text("Ví dụ: 123456789012") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("add_user_cccd"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ và Tên Nhân Viên *") },
                    placeholder = { Text("Ví dụ: Nguyễn Văn C") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_user_fullname"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Vai trò phân quyền:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ElevatedFilterChip(
                        selected = role == "CAPTAIN",
                        onClick = { role = "CAPTAIN" },
                        label = { Text("Đội Trưởng", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )

                    ElevatedFilterChip(
                        selected = role == "OFFICER",
                        onClick = { role = "OFFICER" },
                        label = { Text("Nghiệp Vụ", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )

                    ElevatedFilterChip(
                        selected = role == "DISCIPLINE",
                        onClick = { role = "DISCIPLINE" },
                        label = { Text("Điều Lệnh", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )

                    ElevatedFilterChip(
                        selected = role == "ADMIN",
                        onClick = { role = "ADMIN" },
                        label = { Text("Admin", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(0.9f)
                    )
                }

                // Assigned Location - Only for Captain
                if (role == "CAPTAIN") {
                    OutlinedTextField(
                        value = assignedLocation,
                        onValueChange = { assignedLocation = it },
                        label = { Text("Mục tiêu quản lý / Nơi làm việc") },
                        placeholder = { Text("Ví dụ: Ngày & Đêm Security, Landmark 81...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_user_location"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu Đăng nhập") },
                    placeholder = { Text("Mặc định: nds123 (hoặc tùy chỉnh)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_user_password"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("HỦY")
                    }

                    Button(
                        onClick = {
                            if (isFormValid) {
                                val finalLocation = when (role) {
                                    "ADMIN" -> "Trụ sở chính Ngày & Đêm"
                                    "OFFICER" -> "Phòng Nghiệp vụ"
                                    "DISCIPLINE" -> "Phòng Điều lệnh"
                                    else -> assignedLocation.trim().ifBlank { "Ngày & Đêm Security" }
                                }
                                onSubmit(cccd, fullName, role, password.trim().ifBlank { "nds123" }, finalLocation)
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        enabled = isFormValid,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("XÁC NHẬN")
                    }
                }
            }
        }
    }
}

@Composable
fun EditUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onSubmit: (User) -> Unit
) {
    var fullName by remember { mutableStateOf(user.fullName) }
    var assignedLocation by remember { mutableStateOf(user.assignedLocation) }
    var password by remember { mutableStateOf(user.password) }
    var role by remember { mutableStateOf(user.role) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "CHỈNH SỬA TÀI KHOẢN",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = "CCCD: ${user.cccd}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ và Tên Nhân Viên *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Vai trò phân quyền:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ElevatedFilterChip(
                        selected = role == "CAPTAIN",
                        onClick = { role = "CAPTAIN" },
                        label = { Text("Đội Trưởng", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )

                    ElevatedFilterChip(
                        selected = role == "OFFICER",
                        onClick = { role = "OFFICER" },
                        label = { Text("Nghiệp Vụ", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )

                    ElevatedFilterChip(
                        selected = role == "DISCIPLINE",
                        onClick = { role = "DISCIPLINE" },
                        label = { Text("Điều Lệnh", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )

                    ElevatedFilterChip(
                        selected = role == "ADMIN",
                        onClick = { role = "ADMIN" },
                        label = { Text("Admin", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(0.9f)
                    )
                }

                // Assigned Location
                OutlinedTextField(
                    value = assignedLocation,
                    onValueChange = { assignedLocation = it },
                    label = { Text("Mục tiêu / Nơi công tác") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu Đăng nhập") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("HỦY")
                    }

                    Button(
                        onClick = {
                            val updated = user.copy(
                                fullName = fullName.trim(),
                                role = role,
                                password = password.trim(),
                                assignedLocation = assignedLocation.trim()
                            )
                            onSubmit(updated)
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("LƯU THAY ĐỔI")
                    }
                }
            }
        }
    }
}

@Composable
fun MonthYearPickerDialog(
    initialYear: Int,
    initialMonth: Int, // 1-indexed (1 to 12)
    onDismiss: () -> Unit,
    onDateSelected: (year: Int, month: Int) -> Unit
) {
    var selectedYear by remember { mutableStateOf(initialYear) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Chọn Tháng & Năm",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                // Year selector row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous Year")
                    }
                    Text(
                        text = "$selectedYear",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next Year")
                    }
                }

                // Grid of 12 months
                val monthsList = listOf(
                    "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4",
                    "Tháng 5", "Tháng 6", "Tháng 7", "Tháng 8",
                    "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 3) {
                                val monthIndex = row * 3 + col // 0-indexed
                                val monthNum = monthIndex + 1
                                val isSelected = monthNum == initialMonth
                                
                                Button(
                                    onClick = {
                                        onDateSelected(selectedYear, monthNum)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Text(
                                        text = monthsList[monthIndex],
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("ĐÓNG", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
