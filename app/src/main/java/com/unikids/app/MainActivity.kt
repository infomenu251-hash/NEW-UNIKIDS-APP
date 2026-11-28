
package com.unikids.app

import android.os.Bundle
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// NEW UNIKIDS APP - LIVE BACKEND
private const val API_URL =
    "https://script.google.com/macros/s/AKfycby-0V1PqZVNbhlTCB8hIt9ef4ieLiqpJFaGe4CTvN8fhzkIJeecHDXkaMCW8XG9VH2h/exec"

private val Purple = Color(0xFF291442)
private val DarkPurple = Color(0xFF1B0D2C)
private val Yellow = Color(0xFFFFC928)
private val Red = Color(0xFFD93636)
private val Blue = Color(0xFF1976D2)
private val White = Color.White

private enum class Page {
    Splash, Login, Register, StudentHome, TeacherHome,
    Classes, Schedule, Zoom, Lessons, Materials,
    Payments, Notifications, Profile, Progress, RegistrationSuccess
}

private enum class UserType { STUDENT, TEACHER }
private enum class AppLanguage { ENGLISH, SINHALA }

private data class ApiResult(
    val success: Boolean,
    val message: String,
    val token: String = "",
    val userId: String = "",
    val userName: String = "",
    val profile: JSONObject? = null,
    val response: JSONObject? = null
)

private data class RegistrationData(
    val type: UserType, val fullName: String, val phone: String, val address: String,
    val password: String, val email: String, val dateOfBirth: String, val school: String,
    val grade: String, val classIds: List<String>, val subject: String,
    val photoBase64: String, val photoMimeType: String, val photoFileName: String
)

private suspend fun sendApiRequest(data: JSONObject): ApiResult {
    return withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            connection = URL(API_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.doOutput = true
            connection.instanceFollowRedirects = true
            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=utf-8"
            )
            connection.setRequestProperty("Accept", "application/json")

            connection.outputStream.use { output ->
                output.write(data.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = responseStream?.bufferedReader()?.use {
                it.readText()
            } ?: throw Exception("Empty server response.")

            val json = JSONObject(responseText)
            val user = json.optJSONObject("user")

            ApiResult(
                success = json.optBoolean("success", false),
                message = json.optString(
                    "message",
                    "Unknown server response."
                ),
                token = json.optString("token", ""),
                userId = user?.optString("userId", "") ?: "",
                userName = user?.optString("fullName", "") ?: "",
                profile = user,
                response = json
            )
        } catch (e: Exception) {
            ApiResult(
                success = false,
                message = e.message ?: "Unable to connect to server."
            )
        } finally {
            connection?.disconnect()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Purple,
                    secondary = Yellow,
                    background = White,
                    surface = White
                )
            ) {
                UnikidsApp()
            }
        }
    }
}

@Composable
private fun UnikidsApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var page by remember { mutableStateOf(Page.Splash) }
    var language by remember {
        mutableStateOf(
            if (context.getSharedPreferences("unikids_settings", android.content.Context.MODE_PRIVATE)
                    .getBoolean("sinhala_language", false)) AppLanguage.SINHALA else AppLanguage.ENGLISH
        )
    }
    var userType by remember { mutableStateOf(UserType.STUDENT) }

    var apiMessage by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var sessionToken by remember { mutableStateOf("") }
    var profileData by remember { mutableStateOf<JSONObject?>(null) }
    var profileLoading by remember { mutableStateOf(false) }
    var profileError by remember { mutableStateOf("") }
    var studentDashboardData by remember { mutableStateOf<JSONObject?>(null) }
    var studentNotifications by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var registrationSuccessType by remember { mutableStateOf(UserType.STUDENT) }
    var currentUserName by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val sinhala = language == AppLanguage.SINHALA

    LaunchedEffect(Unit) {
        delay(7000)
        page = Page.Login
    }

    LaunchedEffect(page, sessionToken) {
        if (page == Page.StudentHome && sessionToken.isNotBlank()) {
            val request = JSONObject().apply {
                put("action", "getStudentContent")
                put("token", sessionToken)
            }
            val result = sendApiRequest(request)
            studentDashboardData = if (result.success) result.response else null
            val notificationResult = sendApiRequest(JSONObject().apply {
                put("action", "getNotifications")
                put("token", sessionToken)
            })
            val notificationArray = notificationResult.response?.optJSONArray("notifications")
            val notificationList = mutableListOf<JSONObject>()
            if (notificationArray != null) {
                for (i in 0 until notificationArray.length()) notificationArray.optJSONObject(i)?.let { notificationList.add(it) }
            }
            studentNotifications = if (notificationResult.success) notificationList else emptyList()
        }
        if (page == Page.Profile && sessionToken.isNotBlank()) {
            profileLoading = true
            profileError = ""
            val request = JSONObject().apply {
                put("action", "getProfile")
                put("token", sessionToken)
            }
            val result = sendApiRequest(request)
            profileLoading = false
            if (result.success && result.profile != null) {
                profileData = result.profile
            } else {
                profileData = null
                profileError = result.message.ifBlank {
                    if (sinhala) "පැතිකඩ තොරතුරු ලබාගත නොහැක." else "Unable to load profile."
                }
            }
        }
    }

    fun setLanguage(selected: AppLanguage) {
        language = selected
        context.getSharedPreferences("unikids_settings", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("sinhala_language", selected == AppLanguage.SINHALA)
            .apply()
    }

    fun showError(message: String) {
        apiMessage = message
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = White
    ) {
        when (page) {
            Page.Splash -> SplashScreen(sinhala = sinhala)

            Page.Login -> LoginScreen(
                sinhala = sinhala,
                loading = loading,
                message = apiMessage,
                onLanguage = {
                    setLanguage(if (sinhala) AppLanguage.ENGLISH else AppLanguage.SINHALA)
                },
                onRegister = {
                    apiMessage = ""
                    page = Page.Register
                },
                onLogin = { type, mobile, password ->
                    if (mobile.isBlank() || password.isBlank()) {
                        showError(
                            if (sinhala) {
                                "දුරකථන අංකය සහ මුරපදය ඇතුළත් කරන්න."
                            } else {
                                "Enter your mobile number and password."
                            }
                        )
                    } else {
                        loading = true
                        apiMessage = ""

                        scope.launch {
                            val action = if (type == UserType.STUDENT) {
                                "loginStudent"
                            } else {
                                "loginTeacher"
                            }

                            val request = JSONObject().apply {
                                put("action", action)
                                put("phone", mobile.trim())
                                put("password", password)
                            }

                            val result = sendApiRequest(request)
                            loading = false

                            if (result.success && result.token.isNotBlank()) {
                                sessionToken = result.token
                                currentUserName = result.userName
                                userType = type
                                apiMessage = ""

                                page = if (type == UserType.STUDENT) {
                                    Page.StudentHome
                                } else {
                                    Page.TeacherHome
                                }
                            } else {
                                showError(result.message)
                            }
                        }
                    }
                }
            )

            Page.Register -> RegistrationScreen(
                sinhala = sinhala,
                loading = loading,
                message = apiMessage,
                onBack = {
                    apiMessage = ""
                    page = Page.Login
                },
                onSubmit = { registration ->
                    val type = registration.type
                    val name = registration.fullName
                    val mobile = registration.phone
                    val address = registration.address
                    val password = registration.password
                    if (
                        name.isBlank() ||
                        mobile.isBlank() ||
                        address.isBlank() ||
                        password.isBlank() ||
                        (type == UserType.STUDENT && registration.classIds.isEmpty())
                    ) {
                        showError(
                            if (sinhala) {
                                "සියලු තොරතුරු ඇතුළත් කරන්න."
                            } else {
                                "Please complete all fields."
                            }
                        )
                    } else if (password.length < 6) {
                        showError(
                            if (sinhala) {
                                "මුරපදය අවම වශයෙන් අකුරු 6ක් විය යුතුයි."
                            } else {
                                "Password must contain at least 6 characters."
                            }
                        )
                    } else {
                        loading = true
                        apiMessage = ""

                        scope.launch {
                            val action = if (type == UserType.STUDENT) {
                                "registerStudent"
                            } else {
                                "registerTeacher"
                            }

                            val request = JSONObject().apply {
                                put("action", action)
                                put("fullName", name.trim())
                                put("phone", mobile.trim())
                                put("address", address.trim())
                                put("password", password)
                                if (registration.email.isNotBlank()) put("email", registration.email)
                                if (type == UserType.STUDENT) {
                                    put("dateOfBirth", registration.dateOfBirth)
                                    put("school", registration.school)
                                    put("grade", registration.grade)
                                    put("classIds", org.json.JSONArray(registration.classIds))
                                } else if (registration.subject.isNotBlank()) put("subject", registration.subject)
                                if (registration.photoBase64.isNotBlank()) {
                                    put("photoBase64", registration.photoBase64)
                                    put("photoMimeType", registration.photoMimeType)
                                    put("photoFileName", registration.photoFileName)
                                }
                            }

                            val result = sendApiRequest(request)
                            loading = false

                            if (result.success) {
                                registrationSuccessType = type
                                val voiceFile = when {
                                    type == UserType.STUDENT && sinhala -> "sinhala_voiceover_forstudents (1).wav"
                                    type == UserType.STUDENT -> "english_voiceover_forstudents(1).wav"
                                    sinhala -> "sinhala_voiceover_forteachers(2).wav"
                                    else -> "english_voiceover_forteachers(2).wav"
                                }
                                playVoiceAsset(context, voiceFile)
                                page = Page.RegistrationSuccess
                            } else {
                                showError(result.message)
                            }
                        }
                    }
                }
            )

            Page.StudentHome -> StudentDashboardScreen(
                studentName = currentUserName.ifBlank { if (sinhala) "ශිෂ්‍යයා" else "Student" },
                availableClasses = studentDashboardData?.optJSONArray("classes")?.length() ?: 0,
                availableSchedules = studentDashboardData?.optJSONArray("schedules")?.length() ?: 0,
                availableVideos = studentDashboardData?.optJSONArray("videoLessons")?.length() ?: 0,
                availableMaterials = studentDashboardData?.optJSONArray("studyMaterials")?.length() ?: 0,
                latestNotificationTitle = studentNotifications.firstOrNull()?.optString("title").orEmpty(),
                latestNotificationMessage = studentNotifications.firstOrNull()?.optString("message").orEmpty(),
                isSinhala = sinhala,
                onLanguageChange = { selected ->
                    setLanguage(if (selected) AppLanguage.SINHALA else AppLanguage.ENGLISH)
                },
                onNavigate = { destination ->
                    page = when (destination) {
                        "home" -> Page.StudentHome
                        "classes" -> Page.Classes
                        "schedule" -> Page.Schedule
                        "videos" -> Page.Lessons
                        "zoom" -> Page.Zoom
                        "materials" -> Page.Materials
                        "notifications" -> Page.Notifications
                        "profile" -> Page.Profile
                        "progress" -> Page.Progress
                        else -> Page.StudentHome
                    }
                },
                onLogout = {
                    logoutFromApp(scope, sessionToken) {
                        sessionToken = ""
                        currentUserName = ""
                        profileData = null
                        profileError = ""
                        apiMessage = ""
                        page = Page.Login
                    }
                }
            )

            Page.TeacherHome -> DashboardScreen(
                title = if (sinhala) "ගුරු පුවරුව" else "Teacher Dashboard",
                sinhala = sinhala,
                student = false,
                userName = currentUserName,
                token = sessionToken,
                onLanguageChange = { selected ->
                    setLanguage(if (selected) AppLanguage.SINHALA else AppLanguage.ENGLISH)
                },
                onOpen = { page = it },
                onLogout = {
                    logoutFromApp(scope, sessionToken) {
                        sessionToken = ""
                        currentUserName = ""
                        profileData = null
                        profileError = ""
                        apiMessage = ""
                        page = Page.Login
                    }
                }
            )

            Page.RegistrationSuccess -> RegistrationSuccessScreen(
                sinhala = sinhala,
                userType = registrationSuccessType,
                onContinue = {
                    apiMessage = if (sinhala) {
                        "ලියාපදිංචිය සාර්ථකයි. පරිපාලක අනුමැතිය ලැබෙන තෙක් රැඳී සිටින්න."
                    } else {
                        "Registration successful. Please wait for Admin approval."
                    }
                    page = Page.Login
                }
            )

            Page.Profile -> ProfileScreen(
                profile = profileData,
                loading = profileLoading,
                message = profileError,
                sinhala = sinhala,
                onBack = {
                    page = if (userType == UserType.STUDENT) {
                        Page.StudentHome
                    } else {
                        Page.TeacherHome
                    }
                }
            )

            else -> ConnectedFeatureScreen(
                page = page,
                sinhala = sinhala,
                userType = userType,
                token = sessionToken,
                onBack = {
                    page = if (userType == UserType.STUDENT) {
                        Page.StudentHome
                    } else {
                        Page.TeacherHome
                    }
                }
            )
        }
    }
}


private fun playVoiceAsset(
    context: android.content.Context,
    fileName: String
) {
    runCatching {
        val mediaPlayer = MediaPlayer()
        val asset = context.assets.openFd("voices/$fileName")
        mediaPlayer.setDataSource(
            asset.fileDescriptor,
            asset.startOffset,
            asset.length
        )
        asset.close()
        mediaPlayer.setOnCompletionListener { player -> player.release() }
        mediaPlayer.setOnErrorListener { player, _, _ ->
            player.release()
            true
        }
        mediaPlayer.prepare()
        mediaPlayer.start()
    }
}

@Composable
private fun RegistrationSuccessScreen(
    sinhala: Boolean,
    userType: UserType,
    onContinue: () -> Unit
) {
    val mascotTransition = rememberInfiniteTransition(label = "registrationMascot")
    val mascotScale by mascotTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascotScale"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AssetImage(
            fileName = "unikids_logo_concept.png",
            description = "UNIKIDS Logo",
            modifier = Modifier.size(width = 150.dp, height = 95.dp)
        )
        Spacer(Modifier.height(12.dp))
        AssetImage(
            fileName = "mascot_celebrating_3d.png",
            description = "Celebrating UNIKIDS mascot",
            modifier = Modifier.size(190.dp).graphicsLayer {
                scaleX = mascotScale
                scaleY = mascotScale
            }
        )
        Text(
            if (sinhala) "ලියාපදිංචිය සාර්ථකයි!" else "Registration Successful!",
            color = Purple,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (sinhala) {
                if (userType == UserType.STUDENT) "ආදරණීය ශිෂ්‍යයා, සාදරයෙන් පිළිගනිමු!" else "ආදරණීය ගුරුතුමනි, සාදරයෙන් පිළිගනිමු!"
            } else {
                if (userType == UserType.STUDENT) "Welcome to the UNIKIDS learning family!" else "Welcome to the UNIKIDS teaching family!"
            },
            color = DarkPurple,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8DF))
        ) {
            Text(
                if (sinhala) {
                    "ඔබගේ ලියාපදිංචි තොරතුරු අපට ලැබී ඇත. පරිපාලක අනුමැතිය ලැබෙන තෙක් රැඳී සිටින්න."
                } else {
                    "Your registration has been received. Your account will be available after Admin approval."
                },
                modifier = Modifier.padding(18.dp),
                color = DarkPurple,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text(if (sinhala) "Login පිටුවට යන්න" else "Continue to Login")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (sinhala) "Powered by SmartSolution PVT Ltd" else "Powered by SmartSolution PVT Ltd",
            color = Color.Gray,
            fontSize = 11.sp
        )
        Text(
            "Developed by Menaka Nuwan kumarasinghe",
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}


private fun logoutFromApp(
    scope: kotlinx.coroutines.CoroutineScope,
    token: String,
    onFinished: () -> Unit
) {
    scope.launch {
        try {
            if (token.isNotBlank()) {
                val request = JSONObject().apply {
                    put("action", "logout")
                    put("token", token)
                }
                sendApiRequest(request)
            }
        } finally {
            onFinished()
        }
    }
}

@Composable
private fun AssetImage(
    fileName: String,
    description: String,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val bitmap = remember(fileName) {
        runCatching {
            context.assets.open("images/$fileName").use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = description,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        fallback()
    }
}

@Composable
private fun SplashScreen(sinhala: Boolean) {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        repeat(70) {
            delay(100)
            progress = (it + 1) / 70f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AssetImage(
            fileName = "unikids_logo_concept.png",
            description = "UNIKIDS Logo",
            modifier = Modifier.size(180.dp),
            fallback = {
                Text(
                    "UNIKIDS",
                    color = Purple,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        )

        Text(
            if (sinhala) "ඉගෙනීම සතුටින්" else "Learning Made Fun",
            color = DarkPurple,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(35.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(72.dp)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .align(Alignment.BottomCenter),
                color = Yellow,
                trackColor = Color(0xFFECE7F1)
            )

            AssetImage(
                fileName = "mascot_waving_3d copy.png",
                description = "UNIKIDS mascot",
                modifier = Modifier
                    .size(54.dp)
                    .offset(x = (maxWidth - 54.dp) * progress)
                    .align(Alignment.TopStart),
                fallback = { Text("👋", fontSize = 30.sp) }
            )
        }

        Spacer(Modifier.height(15.dp))

        Text(
            "Loading... ${(progress * 100).toInt()}%",
            color = Purple,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun LoginScreen(
    sinhala: Boolean,
    loading: Boolean,
    message: String,
    onLanguage: () -> Unit,
    onRegister: () -> Unit,
    onLogin: (UserType, String, String) -> Unit
) {
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(UserType.STUDENT) }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onLanguage) {
                Text(
                    if (sinhala) "English" else "සිංහල",
                    color = Purple,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AssetImage(
            fileName = "unikids_logo_concept.png",
            description = "UNIKIDS Logo",
            modifier = Modifier.size(width = 145.dp, height = 92.dp),
            fallback = {
                Text(
                    "UNIKIDS",
                    color = Purple,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        )

        Text(
            if (sinhala) "ඉගෙනීම සතුටින්" else "Learning Made Fun",
            color = DarkPurple,
            fontSize = 14.sp
        )

        AssetImage(
            fileName = "mascot_hello_3d.png",
            description = "Friendly UNIKIDS mascot",
            modifier = Modifier.size(112.dp),
            fallback = { Text("👋", fontSize = 55.sp) }
        )

        Text(
            if (sinhala) "ආයුබෝවන්!" else "Welcome!",
            color = Purple,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            if (sinhala) "ඔබගේ ගිණුමට ඇතුල් වන්න"
            else "Login to your account",
            color = DarkPurple,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF8F5FC)
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (sinhala) "ඇතුල් වන්න" else "LOGIN",
                    color = Purple,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = selectedType == UserType.STUDENT,
                        onClick = { selectedType = UserType.STUDENT },
                        label = { Text(if (sinhala) "ශිෂ්‍ය" else "Student") }
                    )

                    FilterChip(
                        selected = selectedType == UserType.TEACHER,
                        onClick = { selectedType = UserType.TEACHER },
                        label = { Text(if (sinhala) "ගුරු" else "Teacher") }
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = {
                        Text(if (sinhala) "ජංගම දුරකථන අංකය" else "Mobile Number")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (sinhala) "මුරපදය" else "Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(
                                if (showPassword) {
                                    if (sinhala) "සඟවන්න" else "Hide"
                                } else {
                                    if (sinhala) "පෙන්වන්න" else "Show"
                                }
                            )
                        }
                    }
                )

                if (message.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        message,
                        color = Red,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { onLogin(selectedType, mobile, password) },
                    enabled = !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (sinhala) "ඇතුල් වන්න" else "LOGIN",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(onClick = onRegister, enabled = !loading) {
                    Text(
                        if (sinhala) "නව ශිෂ්‍ය / ගුරු ලියාපදිංචිය"
                        else "New Student / Teacher Registration",
                        color = Blue
                    )
                }
            }
        }

        Spacer(Modifier.height(25.dp))

        Text("Powered by", color = Color.Gray, fontSize = 11.sp)
        Text(
            "SmartSolution PVT Ltd",
            color = Purple,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            "Developed by Menaka Nuwan kumarasinghe",
            color = Color.Gray,
            fontSize = 10.sp
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun RegistrationScreen(
    sinhala: Boolean,
    loading: Boolean,
    message: String,
    onBack: () -> Unit,
    onSubmit: (RegistrationData) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf(UserType.STUDENT) }
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var availableClasses by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    val selectedClassIds = remember { mutableStateListOf<String>() }
    var photoBase64 by remember { mutableStateOf("") }
    var photoMimeType by remember { mutableStateOf("") }
    var photoFileName by remember { mutableStateOf("") }
    var photoMessage by remember { mutableStateOf("") }
    var classLoadMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val result = sendApiRequest(JSONObject().put("action", "getClasses"))
        if (result.success) {
            val array = result.response?.optJSONArray("classes")
            val list = mutableListOf<JSONObject>()
            if (array != null) {
                for (i in 0 until array.length()) array.optJSONObject(i)?.let { list.add(it) }
            }
            availableClasses = list
            if (list.isEmpty()) classLoadMessage = if (sinhala) "දැනට සක්‍රිය පන්ති නොමැත." else "No active classes are available."
        } else {
            classLoadMessage = result.message
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: throw IllegalStateException("Unable to read selected image.")
                        Triple(bytes, context.contentResolver.getType(uri) ?: "image/jpeg", uri.lastPathSegment ?: "profile-photo")
                    }.getOrNull()
                }
                if (result == null) {
                    photoMessage = if (sinhala) "රූපය කියවිය නොහැක." else "Could not read the selected image."
                } else if (result.first.size > 3 * 1024 * 1024) {
                    photoBase64 = ""
                    photoMessage = if (sinhala) "රූපය 3MB ට වඩා කුඩා විය යුතුයි." else "Photo must be 3MB or smaller."
                } else if (result.second !in listOf("image/jpeg", "image/jpg", "image/png", "image/webp")) {
                    photoBase64 = ""
                    photoMessage = if (sinhala) "JPG, PNG හෝ WEBP රූපයක් තෝරන්න." else "Choose a JPG, PNG or WEBP image."
                } else {
                    photoBase64 = Base64.encodeToString(result.first, Base64.NO_WRAP)
                    photoMimeType = result.second
                    photoFileName = result.third
                    photoMessage = if (sinhala) "රූපය තෝරාගෙන ඇත." else "Photo selected."
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(if (sinhala) "← ආපසු" else "← Back")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetImage(
                fileName = "mascot_hello_3d.png",
                description = "Friendly UNIKIDS mascot",
                modifier = Modifier.size(76.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    if (sinhala) "ලියාපදිංචිය" else "Registration",
                    fontSize = 26.sp,
                    color = Purple,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (sinhala) "UNIKIDS පවුලට එක්වන්න" else "Join the UNIKIDS family",
                    color = DarkPurple,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = type == UserType.STUDENT,
                onClick = { type = UserType.STUDENT },
                label = { Text(if (sinhala) "ශිෂ්‍ය" else "Student") }
            )
            FilterChip(
                selected = type == UserType.TEACHER,
                onClick = { type = UserType.TEACHER },
                label = { Text(if (sinhala) "ගුරු" else "Teacher") }
            )
        }

        FormField(
            if (sinhala) "සම්පූර්ණ නම" else "Full Name",
            name
        ) { name = it }

        FormField(
            if (sinhala) "ජංගම දුරකථන අංකය" else "Mobile Number",
            mobile
        ) { mobile = it }

        FormField(
            if (sinhala) "ලිපිනය" else "Address",
            address
        ) { address = it }

        FormField(
            if (sinhala) "මුරපදය" else "Password",
            password
        ) { password = it }

        FormField(
            if (sinhala) "විද්‍යුත් තැපෑල (අත්‍යවශ්‍ය නොවේ)" else "Email (optional)",
            email
        ) { email = it }

        if (type == UserType.STUDENT) {
            FormField(if (sinhala) "උපන් දිනය (YYYY-MM-DD)" else "Date of Birth (YYYY-MM-DD)", dateOfBirth) { dateOfBirth = it }
            FormField(if (sinhala) "පාසල" else "School", school) { school = it }
            FormField(if (sinhala) "ශ්‍රේණිය" else "Grade", grade) { grade = it }
            Spacer(Modifier.height(8.dp))
            Text(if (sinhala) "ඔබගේ පන්ති තෝරන්න" else "Select Your Classes", color = Purple, fontWeight = FontWeight.Bold)
            if (classLoadMessage.isNotBlank()) Text(classLoadMessage, color = Red, fontSize = 12.sp)
            availableClasses.forEach { classItem ->
                val classId = classItem.optString("ClassID", classItem.optString("classId"))
                val className = classItem.optString("ClassName", classItem.optString("className"))
                if (classId.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = classId in selectedClassIds,
                            onCheckedChange = { checked ->
                                if (checked && classId !in selectedClassIds) selectedClassIds.add(classId)
                                if (!checked) selectedClassIds.remove(classId)
                            }
                        )
                        Column {
                            Text(className.ifBlank { classId }, color = DarkPurple, fontSize = 14.sp)
                            val subjectName = classItem.optString("Subject")
                            if (subjectName.isNotBlank()) Text(subjectName, color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            FormField(if (sinhala) "විෂය / විශේෂඥතාව" else "Subject / Specialization", subject) { subject = it }
        }

        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { photoPicker.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (sinhala) "Profile Photo තෝරන්න" else "Choose Profile Photo")
        }
        if (photoMessage.isNotBlank()) Text(photoMessage, color = if (photoBase64.isNotBlank()) Blue else Red, fontSize = 12.sp)

        Spacer(Modifier.height(20.dp))

        Text(
            if (sinhala) {
                "ලියාපදිංචිය යොමු කළ පසු පරිපාලක අනුමැතිය අවශ්‍ය වේ."
            } else {
                "Admin approval is required after registration."
            },
            color = Red,
            fontSize = 13.sp
        )

        if (message.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(message, color = Red, fontSize = 13.sp)
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = {
                onSubmit(
                    RegistrationData(
                        type = type,
                        fullName = name,
                        phone = mobile,
                        address = address,
                        password = password,
                        email = email,
                        dateOfBirth = dateOfBirth,
                        school = school,
                        grade = grade,
                        classIds = selectedClassIds.toList(),
                        subject = subject,
                        photoBase64 = photoBase64,
                        photoMimeType = photoMimeType,
                        photoFileName = photoFileName
                    )
                )
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    if (sinhala) "ලියාපදිංචිය යොමු කරන්න"
                    else "SUBMIT REGISTRATION"
                )
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        singleLine = true
    )
}

@Composable
private fun DashboardScreen(
    title: String,
    sinhala: Boolean,
    student: Boolean,
    userName: String,
    token: String,
    onLanguageChange: (Boolean) -> Unit,
    onOpen: (Page) -> Unit,
    onLogout: () -> Unit
) {
    var dashboardData by remember(token) { mutableStateOf<JSONObject?>(null) }
    var dashboardLoading by remember(token) { mutableStateOf(true) }

    LaunchedEffect(token) {
        dashboardLoading = true
        val result = sendApiRequest(JSONObject().apply {
            put("action", "getTeacherDashboard")
            put("token", token)
        })
        dashboardData = if (result.success) result.response else null
        dashboardLoading = false
    }

    val items = if (student) {
        listOf(
            (if (sinhala) "මගේ පන්ති" else "My Classes") to Page.Classes,
            (if (sinhala) "පන්ති කාලසටහන" else "Class Schedule") to Page.Schedule,
            (if (sinhala) "Zoom පන්ති" else "Zoom Classes") to Page.Zoom,
            (if (sinhala) "වීඩියෝ පාඩම්" else "Video Lessons") to Page.Lessons,
            (if (sinhala) "අධ්‍යයන ද්‍රව්‍ය" else "Study Materials") to Page.Materials,
            (if (sinhala) "ගෙවීම්" else "Payments") to Page.Payments,
            (if (sinhala) "දැනුම්දීම්" else "Notifications") to Page.Notifications,
            (if (sinhala) "මගේ පැතිකඩ" else "My Profile") to Page.Profile
        )
    } else {
        listOf(
            (if (sinhala) "මගේ පන්ති" else "My Classes") to Page.Classes,
            (if (sinhala) "කාලසටහන් කළමනාකරණය" else "Manage Schedules") to Page.Schedule,
            (if (sinhala) "Zoom Links" else "Manage Zoom Links") to Page.Zoom,
            (if (sinhala) "වීඩියෝ පාඩම්" else "Manage Video Lessons") to Page.Lessons,
            (if (sinhala) "අධ්‍යයන ද්‍රව්‍ය" else "Manage Study Materials") to Page.Materials,
            (if (sinhala) "දැනුම්දීම්" else "Notifications") to Page.Notifications,
            (if (sinhala) "මගේ පැතිකඩ" else "My Profile") to Page.Profile
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetImage(
                fileName = "unikids_logo_concept.png",
                description = "UNIKIDS Logo",
                modifier = Modifier.size(58.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "NEW UNIKIDS",
                    color = Purple,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    if (sinhala) "ගුරු පාලක පුවරුව" else "Teacher Workspace",
                    color = DarkPurple,
                    fontSize = 12.sp
                )
            }
            TextButton(onClick = { onLanguageChange(!sinhala) }) {
                Text(if (sinhala) "ENGLISH" else "සිංහල", color = Blue, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            title,
            color = DarkPurple,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
        if (userName.isNotBlank()) {
            Text(
                userName,
                color = Blue,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(14.dp))

        if (dashboardLoading) {
            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Purple)
            }
        } else if (dashboardData != null) {
            val stats = listOf(
                (if (sinhala) "පන්ති" else "Classes") to (dashboardData?.optJSONArray("classes")?.length() ?: 0),
                (if (sinhala) "කාලසටහන්" else "Schedules") to (dashboardData?.optJSONArray("schedules")?.length() ?: 0),
                (if (sinhala) "Zoom Links" else "Zoom Links") to (dashboardData?.optJSONArray("zoomLinks")?.length() ?: 0),
                (if (sinhala) "වීඩියෝ පාඩම්" else "Video Lessons") to (dashboardData?.optJSONArray("videoLessons")?.length() ?: 0)
            )
            stats.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { (label, count) ->
                        Card(
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F5FC)),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(13.dp)) {
                                Text(count.toString(), color = Purple, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                                Text(label, color = DarkPurple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                if (sinhala) "දත්ත ලබාගත නොහැක." else "Dashboard data is currently unavailable.",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        items.forEachIndexed { index, item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (index % 3) {
                        0 -> Color(0xFFF3EFF8)
                        1 -> Color(0xFFFFF8DF)
                        else -> Color(0xFFEAF3FC)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val iconFile = when (item.second) {
                        Page.Schedule -> "icon_calendar_3d.png"
                        Page.Zoom -> "icon_play_circle_3d.png"
                        Page.Lessons -> "icon_video_tile_3d.png"
                        Page.Materials -> "icon_open_book_3d.png"
                        Page.Profile -> "icon_profile_3d.png"
                        else -> "icon_books_3d.png"
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssetImage(
                            fileName = iconFile,
                            description = item.first,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            item.first,
                            color = DarkPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = { onOpen(item.second) },
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Text(if (sinhala) "විවෘත කරන්න" else "Open")
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (sinhala) "ඉවත් වන්න" else "Logout")
        }
    }
}


@Composable
private fun ProfileScreen(
    profile: JSONObject?,
    loading: Boolean,
    message: String,
    sinhala: Boolean,
    onBack: () -> Unit
) {
    val fullName = profile?.optString("fullName", "").orEmpty()
    val photoUrl = profile?.optString("profileImageURL", "").orEmpty()
    val userType = profile?.optString("userType", "").orEmpty()
    val classes = profile?.optJSONArray("classes")
    val assignedClasses = profile?.optJSONArray("assignedClassIDs")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(if (sinhala) "← ආපසු" else "← Back", color = Purple)
        }

        Text(
            if (sinhala) "මගේ පැතිකඩ" else "My Profile",
            color = Purple,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(18.dp))

        if (loading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(35.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Purple)
            }
        } else if (profile == null) {
            Text(
                message.ifBlank {
                    if (sinhala) "පැතිකඩ තොරතුරු ලබාගත නොහැක." else "Profile information is unavailable."
                },
                color = Red
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F5FC))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RemoteProfileImage(
                        imageUrl = photoUrl,
                        displayName = fullName
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        fullName.ifBlank { if (sinhala) "නම සඳහන් නැත" else "Name not available" },
                        color = DarkPurple,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        userType.replaceFirstChar { it.uppercase() },
                        color = Blue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(14.dp))
                    ProfileInfoRow(if (sinhala) "දුරකථන අංකය" else "Mobile Number", profile.optString("phone"))
                    ProfileInfoRow(if (sinhala) "ලිපිනය" else "Address", profile.optString("address"))
                    ProfileInfoRow(if (sinhala) "විද්‍යුත් තැපෑල" else "Email", profile.optString("email"))
                    ProfileInfoRow(if (sinhala) "තත්ත්වය" else "Account Status", profile.optString("status"))

                    if (userType.equals("STUDENT", ignoreCase = true)) {
                        ProfileInfoRow(if (sinhala) "උපන් දිනය" else "Date of Birth", profile.optString("dateOfBirth"))
                        ProfileInfoRow(if (sinhala) "පාසල" else "School", profile.optString("school"))
                        ProfileInfoRow(if (sinhala) "ශ්‍රේණිය" else "Grade", profile.optString("grade"))
                        val classText = buildString {
                            if (classes != null) {
                                for (i in 0 until classes.length()) {
                                    if (isNotEmpty()) append(", ")
                                    val item = classes.opt(i)
                                    append(if (item is JSONObject) item.optString("className", item.toString()) else item.toString())
                                }
                            }
                        }
                        ProfileInfoRow(if (sinhala) "මගේ පන්ති" else "My Classes", classText)
                    } else {
                        val classText = buildString {
                            if (assignedClasses != null) {
                                for (i in 0 until assignedClasses.length()) {
                                    if (isNotEmpty()) append(", ")
                                    append(assignedClasses.optString(i))
                                }
                            }
                        }
                        ProfileInfoRow(if (sinhala) "පවරා ඇති පන්ති" else "Assigned Classes", classText)
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteProfileImage(
    imageUrl: String,
    displayName: String
) {
    var bitmap by remember(imageUrl) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }

    LaunchedEffect(imageUrl) {
        bitmap = if (imageUrl.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val connection = URL(imageUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.instanceFollowRedirects = true
                    try {
                        connection.inputStream.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                    } finally {
                        connection.disconnect()
                    }
                }.getOrNull()
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = "Profile photo",
            modifier = Modifier.size(112.dp).clip(RoundedCornerShape(56.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(56.dp))
                .background(Yellow),
            contentAlignment = Alignment.Center
        ) {
            Text(
                displayName.firstOrNull()?.uppercase() ?: "U",
                color = Purple,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String
) {
    if (value.isNotBlank() && value != "null") {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)
        ) {
            Text(label, color = Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = DarkPurple, fontSize = 15.sp)
            Divider(color = Color(0xFFE5DDED), thickness = 1.dp)
        }
    }
}


@Composable
private fun ConnectedFeatureScreen(
    page: Page,
    sinhala: Boolean,
    userType: UserType,
    token: String,
    onBack: () -> Unit
) {
    var loading by remember(page, token) { mutableStateOf(true) }
    var error by remember(page, token) { mutableStateOf("") }
    var payload by remember(page, token) { mutableStateOf<JSONObject?>(null) }
    var refreshKey by remember(page, token) { mutableIntStateOf(0) }
    var submissionMessage by remember(page, token) { mutableStateOf("") }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val title = when (page) {
        Page.Classes -> if (sinhala) "මගේ පන්ති" else "My Classes"
        Page.Schedule -> if (sinhala) "කාලසටහන්" else "Schedules"
        Page.Zoom -> if (sinhala) "Zoom පන්ති" else "Zoom Classes"
        Page.Lessons -> if (sinhala) "වීඩියෝ පාඩම්" else "Video Lessons"
        Page.Materials -> if (sinhala) "අධ්‍යයන ද්‍රව්‍ය" else "Study Materials"
        Page.Payments -> if (sinhala) "ගෙවීම්" else "Payments"
        Page.Notifications -> if (sinhala) "දැනුම්දීම්" else "Notifications"
        Page.Progress -> if (sinhala) "මගේ ප්‍රගතිය" else "My Progress"
        else -> "UNIKIDS"
    }

    val arrayKey = when (page) {
        Page.Classes -> "classes"
        Page.Schedule -> "schedules"
        Page.Zoom -> "zoomLinks"
        Page.Lessons -> "videoLessons"
        Page.Materials -> "studyMaterials"
        Page.Payments -> "payments"
        Page.Notifications -> "notifications"
        else -> ""
    }

    LaunchedEffect(page, token, refreshKey) {
        loading = true
        error = ""
        payload = null
        val action = when {
            page == Page.Notifications -> "getNotifications"
            page == Page.Payments -> "getMyPayments"
            userType == UserType.STUDENT -> "getStudentContent"
            else -> "getTeacherDashboard"
        }
        val request = JSONObject().apply {
            put("action", action)
            put("token", token)
        }
        val result = sendApiRequest(request)
        loading = false
        if (result.success) {
            payload = result.response
        } else {
            error = result.message.ifBlank {
                if (sinhala) "දත්ත ලබාගත නොහැක." else "Unable to load data."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(if (sinhala) "← ආපසු" else "← Back", color = Purple)
        }
        Text(title, color = Purple, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Purple)
            }
        } else if (error.isNotBlank()) {
            Text(error, color = Red, fontSize = 14.sp)
        } else if (page == Page.Progress) {
            Text(
                if (sinhala) "ඔබට ලබාදී ඇති ඉගෙනුම් සම්පත්" else "Your Learning Resources",
                color = DarkPurple,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            val metrics = listOf(
                (if (sinhala) "පන්ති" else "Classes") to (payload?.optJSONArray("classes")?.length() ?: 0),
                (if (sinhala) "කාලසටහන්" else "Schedules") to (payload?.optJSONArray("schedules")?.length() ?: 0),
                (if (sinhala) "වීඩියෝ පාඩම්" else "Video Lessons") to (payload?.optJSONArray("videoLessons")?.length() ?: 0),
                (if (sinhala) "අධ්‍යයන ද්‍රව්‍ය" else "Study Materials") to (payload?.optJSONArray("studyMaterials")?.length() ?: 0)
            )
            metrics.forEach { (label, count) ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F5FC)),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = DarkPurple, fontWeight = FontWeight.SemiBold)
                        Text(count.toString(), color = Purple, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Text(
                if (sinhala) "මෙම සංඛ්‍යා අනුමත කර ඔබට ලබාදී ඇති සම්පත් ගණන පෙන්වයි." else "These counts show approved learning resources currently available to your account.",
                color = Color.Gray,
                fontSize = 12.sp
            )
        } else {
            if (submissionMessage.isNotBlank()) {
                Text(submissionMessage, color = Blue, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }
            if (userType == UserType.TEACHER && page in listOf(Page.Schedule, Page.Zoom, Page.Lessons, Page.Materials)) {
                TeacherSubmissionForm(
                    page = page,
                    classes = payload?.optJSONArray("classes"),
                    token = token,
                    sinhala = sinhala,
                    onResult = { result ->
                        submissionMessage = result.message
                        if (result.success) refreshKey++
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
            if (userType == UserType.STUDENT && page == Page.Payments) {
                StudentPaymentForm(
                    token = token,
                    sinhala = sinhala,
                    onResult = { result ->
                        submissionMessage = result.message
                        if (result.success) refreshKey++
                    }
                )
                Spacer(Modifier.height(16.dp))
                val installments = payload?.optJSONArray("installments")
                if (installments != null && installments.length() > 0) {
                    Text(if (sinhala) "වාරික විස්තර" else "Installments", color = Purple, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    for (i in 0 until installments.length()) {
                        val installment = installments.optJSONObject(i) ?: continue
                        ConnectedRecordCard(installment, Page.Payments, sinhala) { }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            val array = payload?.optJSONArray(arrayKey)
            if (array == null || array.length() == 0) {
                Text(
                    if (sinhala) "දැනට පෙන්වීමට දත්ත නොමැත." else "No records are available yet.",
                    color = DarkPurple,
                    fontSize = 14.sp
                )
            } else {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    ConnectedRecordCard(
                        item = item,
                        page = page,
                        sinhala = sinhala,
                        onOpenLink = { url ->
                            runCatching { uriHandler.openUri(url) }
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

        }
    }
}



@Composable
private fun StudentPaymentForm(
    token: String,
    sinhala: Boolean,
    onResult: (ApiResult) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf("Course Fee") }
    var reference by remember { mutableStateOf("") }
    var installmentId by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F5FC))
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(if (sinhala) "ගෙවීමක් යොමු කරන්න" else "Submit a Payment", color = Purple, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(amount, { amount = it }, label = { Text(if (sinhala) "මුදල (LKR)" else "Amount (LKR)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(paymentType, { paymentType = it }, label = { Text(if (sinhala) "ගෙවීම් වර්ගය" else "Payment Type") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(reference, { reference = it }, label = { Text(if (sinhala) "බැංකු / ගනුදෙනු යොමුව" else "Bank / Transaction Reference") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(installmentId, { installmentId = it }, label = { Text(if (sinhala) "වාරික අංකය (තිබේ නම්)" else "Installment ID (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(notes, { notes = it }, label = { Text(if (sinhala) "සටහන්" else "Notes") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Text(if (sinhala) "ගෙවීම පරිපාලක විසින් තහවුරු කළ පසු යාවත්කාලීන වේ." else "Your payment remains pending until verified by Admin.", color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    val parsed = amount.toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) {
                        onResult(ApiResult(false, if (sinhala) "නිවැරදි මුදලක් ඇතුළත් කරන්න." else "Enter a valid positive amount."))
                    } else {
                        submitting = true
                        scope.launch {
                            val request = JSONObject().apply {
                                put("action", "submitPayment")
                                put("token", token)
                                put("amount", parsed)
                                if (paymentType.isNotBlank()) put("paymentType", paymentType)
                                if (reference.isNotBlank()) put("reference", reference)
                                if (installmentId.isNotBlank()) put("installmentId", installmentId)
                                if (notes.isNotBlank()) put("notes", notes)
                            }
                            val result = sendApiRequest(request)
                            submitting = false
                            onResult(result)
                        }
                    }
                },
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = White, strokeWidth = 2.dp)
                else Text(if (sinhala) "පරිපාලක තහවුරු කිරීම සඳහා යොමු කරන්න" else "Submit for Admin Verification")
            }
        }
    }
}


@Composable
private fun TeacherSubmissionForm(
    page: Page,
    classes: org.json.JSONArray?,
    token: String,
    sinhala: Boolean,
    onResult: (ApiResult) -> Unit
) {
    val classItems = remember(classes) {
        val items = mutableListOf<JSONObject>()
        if (classes != null) for (i in 0 until classes.length()) classes.optJSONObject(i)?.let { items.add(it) }
        items
    }
    var selectedClassId by remember(page) { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var date by remember(page) { mutableStateOf("") }
    var startTime by remember(page) { mutableStateOf("") }
    var endTime by remember(page) { mutableStateOf("") }
    var title by remember(page) { mutableStateOf("") }
    var subject by remember(page) { mutableStateOf("") }
    var description by remember(page) { mutableStateOf("") }
    var url by remember(page) { mutableStateOf("") }
    var thumbnailUrl by remember(page) { mutableStateOf("") }
    var materialType by remember(page) { mutableStateOf("PDF") }
    var submitting by remember(page) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val selectedClassName = classItems.firstOrNull {
        it.optString("ClassID", it.optString("classId")) == selectedClassId
    }?.optString("ClassName", selectedClassId) ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F5FC))
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(
                when (page) {
                    Page.Schedule -> if (sinhala) "නව කාලසටහනක් යොමු කරන්න" else "Submit a Schedule"
                    Page.Zoom -> if (sinhala) "නව Zoom Link එකක් යොමු කරන්න" else "Submit a Zoom Link"
                    Page.Lessons -> if (sinhala) "නව වීඩියෝ පාඩමක් යොමු කරන්න" else "Submit a Video Lesson"
                    else -> if (sinhala) "අධ්‍යයන ද්‍රව්‍ය යොමු කරන්න" else "Submit Study Material"
                },
                color = Purple,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (selectedClassId.isBlank()) {
                        if (sinhala) "පන්තිය තෝරන්න" else "Select Class"
                    } else selectedClassName.ifBlank { selectedClassId }
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                classItems.forEach { item ->
                    val id = item.optString("ClassID", item.optString("classId"))
                    val name = item.optString("ClassName", id)
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedClassId = id
                            expanded = false
                        }
                    )
                }
            }

            when (page) {
                Page.Schedule -> {
                    OutlinedTextField(date, { date = it }, label = { Text(if (sinhala) "දිනය (YYYY-MM-DD)" else "Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(startTime, { startTime = it }, label = { Text(if (sinhala) "ආරම්භක වේලාව" else "Start Time") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(endTime, { endTime = it }, label = { Text(if (sinhala) "අවසන් වේලාව" else "End Time") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(subject, { subject = it }, label = { Text(if (sinhala) "විෂය" else "Subject (optional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(description, { description = it }, label = { Text(if (sinhala) "විස්තර" else "Description (optional)") }, modifier = Modifier.fillMaxWidth())
                }
                Page.Zoom -> {
                    OutlinedTextField(url, { url = it }, label = { Text("Zoom URL (https://)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(date, { date = it }, label = { Text(if (sinhala) "දිනය" else "Date (optional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(startTime, { startTime = it }, label = { Text(if (sinhala) "ආරම්භක වේලාව" else "Start Time") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(endTime, { endTime = it }, label = { Text(if (sinhala) "අවසන් වේලාව" else "End Time") }, modifier = Modifier.fillMaxWidth())
                }
                Page.Lessons -> {
                    OutlinedTextField(title, { title = it }, label = { Text(if (sinhala) "පාඩමේ මාතෘකාව" else "Lesson Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(url, { url = it }, label = { Text(if (sinhala) "වීඩියෝ URL" else "Video URL") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(description, { description = it }, label = { Text(if (sinhala) "විස්තර" else "Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(thumbnailUrl, { thumbnailUrl = it }, label = { Text(if (sinhala) "Thumbnail URL (optional)" else "Thumbnail URL (optional)") }, modifier = Modifier.fillMaxWidth())
                }
                Page.Materials -> {
                    OutlinedTextField(title, { title = it }, label = { Text(if (sinhala) "මාතෘකාව" else "Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(url, { url = it }, label = { Text(if (sinhala) "ගොනු URL" else "File URL") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(materialType, { materialType = it }, label = { Text(if (sinhala) "ගොනු වර්ගය" else "Material Type") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(description, { description = it }, label = { Text(if (sinhala) "විස්තර" else "Description") }, modifier = Modifier.fillMaxWidth())
                }
                else -> Unit
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val action = when (page) {
                        Page.Schedule -> "submitSchedule"
                        Page.Zoom -> "submitZoomLink"
                        Page.Lessons -> "submitVideoLesson"
                        else -> "submitStudyMaterial"
                    }
                    val validUrl = url.startsWith("https://", true) || (page == Page.Lessons && url.startsWith("http://", true))
                    val valid = selectedClassId.isNotBlank() && when (page) {
                        Page.Schedule -> date.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()
                        Page.Zoom, Page.Lessons -> validUrl
                        else -> title.isNotBlank() && url.startsWith("http", true)
                    }
                    if (!valid) {
                        onResult(ApiResult(false, if (sinhala) "අවශ්‍ය තොරතුරු නිවැරදිව ඇතුළත් කරන්න." else "Please complete the required fields with valid values."))
                    } else {
                        submitting = true
                        scope.launch {
                            val request = JSONObject().apply {
                                put("action", action)
                                put("token", token)
                                put("classId", selectedClassId)
                                when (page) {
                                    Page.Schedule -> {
                                        put("classDate", date)
                                        put("startTime", startTime)
                                        put("endTime", endTime)
                                        if (subject.isNotBlank()) put("subject", subject)
                                        if (description.isNotBlank()) put("description", description)
                                    }
                                    Page.Zoom -> {
                                        put("zoomUrl", url)
                                        if (date.isNotBlank()) put("classDate", date)
                                        if (startTime.isNotBlank()) put("startTime", startTime)
                                        if (endTime.isNotBlank()) put("endTime", endTime)
                                    }
                                    Page.Lessons -> {
                                        put("videoUrl", url)
                                        if (title.isNotBlank()) put("title", title)
                                        if (description.isNotBlank()) put("description", description)
                                        if (thumbnailUrl.isNotBlank()) put("thumbnailUrl", thumbnailUrl)
                                    }
                                    Page.Materials -> {
                                        put("title", title)
                                        put("fileUrl", url)
                                        put("materialType", materialType)
                                        if (description.isNotBlank()) put("description", description)
                                    }
                                    else -> Unit
                                }
                            }
                            val result = sendApiRequest(request)
                            submitting = false
                            onResult(result)
                        }
                    }
                },
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = White, strokeWidth = 2.dp)
                else Text(if (sinhala) "පරිපාලක අනුමැතිය සඳහා යොමු කරන්න" else "Submit for Admin Approval")
            }
        }
    }
}



@Composable
private fun RemoteThumbnailImage(imageUrl: String) {
    var bitmap by remember(imageUrl) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    LaunchedEffect(imageUrl) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(imageUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.instanceFollowRedirects = true
                try {
                    connection.inputStream.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = "Video lesson thumbnail",
            modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    }
}


@Composable
private fun ConnectedRecordCard(
    item: JSONObject,
    page: Page,
    sinhala: Boolean,
    onOpenLink: (String) -> Unit
) {
    val preferredKeys = when (page) {
        Page.Classes -> listOf("ClassName", "className", "Subject", "subject", "Grade", "grade", "ClassID", "classId")
        Page.Schedule -> listOf("className", "ClassName", "classDate", "ClassDate", "ScheduleDate", "date", "startTime", "StartTime", "endTime", "EndTime", "description", "Description", "status", "Status")
        Page.Zoom -> listOf("className", "ClassName", "linkId", "LinkID", "classDate", "ClassDate", "startTime", "StartTime", "status", "Status")
        Page.Lessons -> listOf("title", "Title", "videoNumber", "VideoID", "subject", "Subject", "description", "Description", "status", "Status")
        Page.Materials -> listOf("title", "Title", "subject", "Subject", "description", "Description", "status", "Status")
        Page.Payments -> listOf("paymentDate", "PaymentDate", "amount", "Amount", "paymentType", "PaymentType", "reference", "Reference", "status", "Status", "receiptNumber", "ReceiptNumber", "PaymentID", "InstallmentID", "DueDate")
        Page.Notifications -> listOf("title", "Title", "message", "Message", "createdDate", "CreatedDate")
        else -> emptyList()
    }
    val values = preferredKeys.mapNotNull { key ->
        val value = item.optString(key, "").trim()
        if (value.isNotBlank() && value != "null") key to value else null
    }.distinctBy { it.second }
    val url = listOf("zoomUrl", "ZoomURL", "videoUrl", "VideoURL", "materialUrl", "MaterialURL", "fileUrl", "FileURL", "ReceiptURL", "receiptUrl", "url", "URL")
        .firstNotNullOfOrNull { key -> item.optString(key, "").takeIf { it.startsWith("http", true) } }
    val thumbnail = if (page == Page.Lessons) {
        listOf("thumbnailUrl", "ThumbnailURL", "thumbnailURL").firstNotNullOfOrNull { key ->
            item.optString(key, "").takeIf { it.startsWith("http", true) }
        }
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F5FC))
    ) {
        Column(Modifier.padding(15.dp)) {
            if (thumbnail != null) {
                RemoteThumbnailImage(thumbnail)
                Spacer(Modifier.height(10.dp))
            }
            Text(
                values.firstOrNull()?.second ?: if (sinhala) "විස්තර" else "Details",
                color = Purple,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            values.drop(1).forEach { (key, value) ->
                val label = key.replace(Regex("([a-z])([A-Z])"), "$1 $2")
                Text("$label: $value", color = DarkPurple, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
            }
            if (url != null) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onOpenLink(url) },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) {
                    Text(if (page == Page.Zoom) "Join Zoom" else if (sinhala) "විවෘත කරන්න" else "Open")
                }
            }
        }
    }
}
