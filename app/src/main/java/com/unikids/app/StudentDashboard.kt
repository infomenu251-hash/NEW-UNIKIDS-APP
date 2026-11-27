package com.unikids.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val UniPurple = Color(0xFF291442)
private val UniDarkPurple = Color(0xFF1B0D2C)
private val UniYellow = Color(0xFFFFC928)
private val UniRed = Color(0xFFD93636)
private val UniBlue = Color(0xFF1976D2)
private val UniWhite = Color(0xFFFFFFFF)

private val DashboardGradient = Brush.linearGradient(
    colors = listOf(
        UniDarkPurple,
        UniPurple,
        Color(0xFF702044),
        UniRed
    )
)

@Composable
private fun StudentAsset(
    fileName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(fileName) {
        runCatching {
            context.assets.open("images/$fileName").use {
                BitmapFactory.decodeStream(it).asImageBitmap()
            }
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun StudentDashboardScreen(
    studentName: String = "Student",
    isSinhala: Boolean = true,
    onLanguageChange: (Boolean) -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val greeting = if (isSinhala) "ආයුබෝවන්!" else "Hello!"
    val subtitle = if (isSinhala) "අදත් අපි අලුත් දෙයක් ඉගෙන ගනිමු." else "Let's learn something new today."
    val learningTitle = if (isSinhala) "මගේ ඉගෙනුම්" else "My Learning"
    val notificationTitle = if (isSinhala) "දැනුම්දීම්" else "Notifications"
    val seeAll = if (isSinhala) "සියල්ල බලන්න" else "See all"

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(containerColor = UniWhite) {
                NavigationBarItem(
                    selected = true,
                    onClick = { onNavigate("home") },
                    icon = { Text("🏠", fontSize = 21.sp) },
                    label = { Text(if (isSinhala) "මුල් පිටුව" else "Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigate("classes") },
                    icon = { Text("📚", fontSize = 21.sp) },
                    label = { Text(if (isSinhala) "පන්ති" else "Classes") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigate("notifications") },
                    icon = { Text("🔔", fontSize = 21.sp) },
                    label = { Text(if (isSinhala) "දැනුම්දීම්" else "Alerts") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigate("profile") },
                    icon = { Text("👤", fontSize = 21.sp) },
                    label = { Text(if (isSinhala) "මගේ විස්තර" else "Profile") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DashboardGradient)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            val headerShape = RoundedCornerShape(22.dp)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, headerShape)
                    .border(1.dp, UniWhite.copy(alpha = 0.28f), headerShape),
                shape = headerShape,
                colors = CardDefaults.cardColors(
                    containerColor = UniWhite.copy(alpha = 0.13f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "NEW UNIKIDS",
                            color = UniWhite,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            if (isSinhala) "ශිෂ්‍ය පාලක පුවරුව" else "Student Dashboard",
                            color = UniWhite.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                    }

                    TextButton(
                        onClick = { onLanguageChange(!isSinhala) }
                    ) {
                        Text(
                            if (isSinhala) "ENGLISH" else "සිංහල",
                            color = UniYellow,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(onClick = onLogout) {
                        Text(
                            if (isSinhala) "ඉවත් වන්න" else "Logout",
                            color = UniWhite,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            val greetingShape = RoundedCornerShape(26.dp)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, greetingShape)
                    .border(1.dp, UniPurple, greetingShape),
                shape = greetingShape,
                colors = CardDefaults.cardColors(containerColor = UniWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isSinhala) "ඔබ නැවතත් සාදරයෙන් පිළිගනිමු" else "WELCOME BACK",
                            color = UniRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            greeting,
                            color = UniPurple,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            studentName,
                            color = UniDarkPurple,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            subtitle,
                            color = UniDarkPurple,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                    StudentAsset(
                        "mascot_celebrating_3d.png",
                        Modifier.size(112.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Column {
                Text(
                    if (isSinhala) "ඉගෙනීමට සූදානම්ද?" else "YOUR LEARNING SPACE",
                    color = UniYellow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        learningTitle,
                        color = UniWhite,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    Card(
                        shape = RoundedCornerShape(50.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = UniWhite.copy(alpha = 0.16f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            if (isSinhala) "අංශ 06" else "06 MODULES",
                            color = UniYellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(15.dp))

            val items = listOf(
                DashboardItem(
                    "icon_books_3d.png",
                    if (isSinhala) "මගේ පන්ති" else "My Classes",
                    "classes",
                    UniPurple
                ),
                DashboardItem(
                    "icon_calendar_3d.png",
                    if (isSinhala) "කාලසටහන" else "Schedule",
                    "schedule",
                    UniBlue
                ),
                DashboardItem(
                    "icon_video_tile_3d.png",
                    if (isSinhala) "වීඩියෝ පාඩම්" else "Video Lessons",
                    "videos",
                    UniRed
                ),
                DashboardItem(
                    "icon_play_circle_3d.png",
                    if (isSinhala) "Zoom පන්ති" else "Zoom Classes",
                    "zoom",
                    Color(0xFF8E44AD)
                ),
                DashboardItem(
                    "icon_open_book_3d.png",
                    if (isSinhala) "අධ්‍යයන ද්‍රව්‍ය" else "Study Materials",
                    "materials",
                    Color(0xFF16865B)
                ),
                DashboardItem(
                    "icon_progress_chart_3d.png",
                    if (isSinhala) "මගේ ප්‍රගතිය" else "My Progress",
                    "progress",
                    UniYellow
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { item ->
                            DashboardTile(
                                item = item,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate(item.route) }
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isSinhala) "යාවත්කාලීනව සිටින්න" else "STAY UPDATED",
                        color = UniYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        notificationTitle,
                        color = UniWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                TextButton(onClick = { onNavigate("notifications") }) {
                    Text(seeAll, color = UniYellow, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(20.dp))
                    .border(1.dp, UniBlue, RoundedCornerShape(20.dp))
                    .clickable { onNavigate("notifications") },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = UniWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudentAsset(
                        "icon_achievement_3d.png",
                        Modifier.size(48.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (isSinhala) "ඔබේ අලුත් දැනුම්දීම් මෙහි පෙන්වයි." else "Your latest notifications will appear here.",
                            color = UniDarkPurple,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            if (isSinhala) "දැනුම්දීම් බැලීමට මෙතැන ඔබන්න." else "Tap here to view notifications.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Powered by SmartSolution PVT Ltd",
                color = UniWhite.copy(alpha = 0.85f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                "Developed by Menaka Nuwan kumarasinghe",
                color = UniWhite.copy(alpha = 0.85f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

private data class DashboardItem(
    val image: String,
    val title: String,
    val route: String,
    val background: Color
)

@Composable
private fun DashboardTile(
    item: DashboardItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tileShape = RoundedCornerShape(22.dp)

    Card(
        modifier = modifier
            .shadow(12.dp, tileShape)
            .border(1.dp, item.background, tileShape)
            .height(154.dp)
            .clickable(onClick = onClick),
        shape = tileShape,
        colors = CardDefaults.cardColors(containerColor = UniWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StudentAsset(
                item.image,
                Modifier.size(76.dp)
            )
            Spacer(Modifier.height(7.dp))
            Text(
                item.title,
                color = item.background,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
