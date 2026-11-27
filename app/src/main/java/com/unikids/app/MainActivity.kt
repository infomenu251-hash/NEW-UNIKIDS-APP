package com.unikids.app

import android.media.MediaPlayer
import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import import androidx.compose.ui.layout.ContentScale
import import androidx.compose.ui.graphics.asImageBitmap
import import androidx.compose.foundation.layout.BoxWithConstraints
import import androidx.compose.foundation.Image
import import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Purple = Color(0xFF291442)
private val DarkPurple = Color(0xFF1B0D2C)
private val Yellow = Color(0xFFFFC928)
private val Red = Color(0xFFD93636)
private val Blue = Color(0xFF1976D2)
private val White = Color.White

private enum class Page {
    Splash, Login, Register, StudentHome, TeacherHome,
    Classes, Schedule, Zoom, Lessons, Materials,
    Payments, Notifications, Profile
}

private enum class UserType { STUDENT, TEACHER }

private enum class AppLanguage { ENGLISH, SINHALA }

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
    var page by remember { mutableStateOf(Page.Splash) }
    var language by remember { mutableStateOf(AppLanguage.ENGLISH) }
    var userType by remember { mutableStateOf(UserType.STUDENT) }

    val sinhala = language == AppLanguage.SINHALA

    LaunchedEffect(Unit) {
        // Splash minimum duration: 7 seconds.
        // Backend initialization will be connected in the integration phase.
        delay(7000)
        page = Page.Login
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = White
    ) {
        when (page) {
            Page.Splash -> SplashScreen()
            Page.Login -> LoginScreen(
                sinhala = sinhala,
                onLanguage = {
                    language = if (sinhala) AppLanguage.ENGLISH
                    else AppLanguage.SINHALA
                },
                onRegister = { page = Page.Register },
                onLogin = { type ->
                    userType = type
                    page = if (type == UserType.STUDENT)
                        Page.StudentHome else Page.TeacherHome
                }
            )
            Page.Register -> RegistrationScreen(
                sinhala = sinhala,
                onBack = { page = Page.Login },
                onSubmit = {
                    // Backend registration and success voice are
                    // intentionally not triggered until server success.
                }
            )
            Page.StudentHome -> DashboardScreen(
                title = if (sinhala) "ශිෂ්‍ය පුවරුව" else "Student Dashboard",
                sinhala = sinhala,
                student = true,
                onOpen = { page = it },
                onLogout = { page = Page.Login }
            )
            Page.TeacherHome -> DashboardScreen(
                title = if (sinhala) "ගුරු පුවරුව" else "Teacher Dashboard",
                sinhala = sinhala,
                student = false,
                onOpen = { page = it },
                onLogout = { page = Page.Login }
            )
            else -> FeatureScreen(
                page = page,
                sinhala = sinhala,
                onBack = {
                    page = if (userType == UserType.STUDENT)
                        Page.StudentHome else Page.TeacherHome
                }
            )
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
private fun SplashScreen() {
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
            "Learning Made Fun",
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
                fallback = {
                    Text("👋", fontSize = 30.sp)
                }
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
    onLanguage: () -> Unit,
    onRegister: () -> Unit,
    onLogin: (UserType) -> Unit
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

        Spacer(Modifier.height(4.dp))

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
            fallback = {
                Text("👋", fontSize = 55.sp)
            }
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedType == UserType.STUDENT,
                        onClick = { selectedType = UserType.STUDENT },
                        label = {
                            Text(if (sinhala) "ශිෂ්‍ය" else "Student")
                        }
                    )

                    FilterChip(
                        selected = selectedType == UserType.TEACHER,
                        onClick = { selectedType = UserType.TEACHER },
                        label = {
                            Text(if (sinhala) "ගුරු" else "Teacher")
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = {
                        Text(
                            if (sinhala) "ජංගම දුරකථන අංකය"
                            else "Mobile Number"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(if (sinhala) "මුරපදය" else "Password")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                    visualTransformation = if (showPassword)
                        VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(
                            onClick = { showPassword = !showPassword }
                        ) {
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

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { onLogin(selectedType) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (sinhala) "ඇතුල් වන්න" else "LOGIN",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onRegister) {
                    Text(
                        if (sinhala) "නව ශිෂ්‍ය / ගුරු ලියාපදිංචිය"
                        else "New Student / Teacher Registration",
                        color = Blue
                    )
                }
            }
        }

        Spacer(Modifier.height(25.dp))

        Text(
            "Powered by",
            color = Color.Gray,
            fontSize = 11.sp
        )

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
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    var type by remember { mutableStateOf(UserType.STUDENT) }
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(if (sinhala) "← ආපසු" else "← Back")
        }
        Text(
            if (sinhala) "ලියාපදිංචිය" else "Registration",
            fontSize = 26.sp,
            color = Purple,
            fontWeight = FontWeight.Bold
        )
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

        FormField(if (sinhala) "සම්පූර්ණ නම" else "Full Name", name) { name = it }
        FormField(if (sinhala) "ජංගම දුරකථන අංකය" else "Mobile Number", mobile) { mobile = it }
        FormField(if (sinhala) "ලිපිනය" else "Address", address) { address = it }
        FormField(if (sinhala) "මුරපදය" else "Password", password) { password = it }

        Spacer(Modifier.height(20.dp))
        Text(
            if (sinhala)
                "ලියාපදිංචිය යොමු කළ පසු පරිපාලක අනුමැතිය අවශ්‍ය වේ."
            else
                "Admin approval is required after registration.",
            color = Red,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text(if (sinhala) "ලියාපදිංචිය යොමු කරන්න" else "SUBMIT REGISTRATION")
        }
        Text(
            if (sinhala)
                "සැබෑ ලියාපදිංචිය සඳහා Backend සම්බන්ධ කිරීම ඊළඟ සංවර්ධන අදියරේදී සිදු කෙරේ."
            else
                "Backend submission will be connected in the integration phase.",
            color = Color.Gray,
            fontSize = 11.sp
        )
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        singleLine = true
    )
}

@Composable
private fun DashboardScreen(
    title: String,
    sinhala: Boolean,
    student: Boolean,
    onOpen: (Page) -> Unit,
    onLogout: () -> Unit
) {
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
            (if (sinhala) "කාලසටහන් කළමනාකරණය" else "Manage Schedules") to Page.Schedule,
            (if (sinhala) "Zoom Links" else "Manage Zoom Links") to Page.Zoom,
            (if (sinhala) "වීඩියෝ පාඩම්" else "Manage Video Lessons") to Page.Lessons,
            (if (sinhala) "අධ්‍යයන ද්‍රව්‍ය" else "Manage Study Materials") to Page.Materials,
            (if (sinhala) "මගේ පැතිකඩ" else "My Profile") to Page.Profile
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("UNIKIDS", color = Purple, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text(title, color = DarkPurple, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        items.forEachIndexed { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
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
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.first, color = DarkPurple, fontWeight = FontWeight.SemiBold)
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
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text(if (sinhala) "ඉවත් වන්න" else "Logout")
        }
    }
}

@Composable
private fun FeatureScreen(
    page: Page,
    sinhala: Boolean,
    onBack: () -> Unit
) {
    val title = when (page) {
        Page.Classes -> if (sinhala) "මගේ පන්ති" else "My Classes"
        Page.Schedule -> if (sinhala) "කාලසටහන්" else "Schedules"
        Page.Zoom -> if (sinhala) "Zoom පන්ති" else "Zoom Classes"
        Page.Lessons -> if (sinhala) "වීඩියෝ පාඩම්" else "Video Lessons"
        Page.Materials -> if (sinhala) "අධ්‍යයන ද්‍රව්‍ය" else "Study Materials"
        Page.Payments -> if (sinhala) "ගෙවීම්" else "Payments"
        Page.Notifications -> if (sinhala) "දැනුම්දීම්" else "Notifications"
        Page.Profile -> if (sinhala) "මගේ පැතිකඩ" else "My Profile"
        else -> "UNIKIDS"
    }

    Column(
        modifier = Modifier.fillMaxSize().background(White).padding(22.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(if (sinhala) "← ආපසු" else "← Back")
        }
        Text(title, color = Purple, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text(
            if (sinhala)
                "මෙම විශේෂාංගයේ දත්ත සහ ක්‍රියාකාරකම් Backend සම්බන්ධ කිරීමේ අදියරේදී සක්‍රිය කෙරේ."
            else
                "This feature will be connected to the live backend and class-specific data in the integration phase.",
            color = DarkPurple
        )
    }
}
