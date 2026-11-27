package com.unikids.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
private val UniBackground = Color(0xFFF8F6FC)

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
        containerColor = UniBackground,
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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "NEW UNIKIDS",
                        color = UniPurple,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        if (isSinhala) "ශිෂ්‍ය පාලක පුවරුව" else "Student Dashboard",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                TextButton(
                    onClick = { onLanguageChange(!isSinhala) }
                ) {
                    Text(
                        if (isSinhala) "ENGLISH" else "සිංහල",
                        color = UniPurple,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onLogout) {
                    Text(
                        if (isSinhala) "ඉවත් වන්න" else "Logout",
                        color = UniRed,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = UniPurple)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            greeting,
                            color = UniYellow,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            studentName,
                            color = UniWhite,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            subtitle,
                            color = UniWhite,
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

            Text(
                learningTitle,
                color = UniDarkPurple,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(13.dp))

            val items = listOf(
                DashboardItem(
                    "icon_books_3d.png",
                    if (isSinhala) "මගේ පන්ති" else "My Classes",
                    "classes",
                    Color(0xFFFFF3CC)
                ),
                DashboardItem(
                    "icon_calendar_3d.png",
                    if (isSinhala) "කාලසටහන" else "Schedule",
                    "schedule",
                    Color(0xFFE8F1FF)
                ),
                DashboardItem(
                    "icon_video_tile_3d.png",
                    if (isSinhala) "වීඩියෝ පාඩම්" else "Video Lessons",
                    "videos",
                    Color(0xFFFFE9E9)
                ),
                DashboardItem(
                    "icon_play_circle_3d.png",
                    if (isSinhala) "Zoom පන්ති" else "Zoom Classes",
                    "zoom",
                    Color(0xFFE9E6FF)
                ),
                DashboardItem(
                    "icon_open_book_3d.png",
                    if (isSinhala) "අධ්‍යයන ද්‍රව්‍ය" else "Study Materials",
                    "materials",
                    Color(0xFFE3F7EF)
                ),
                DashboardItem(
                    "icon_progress_chart_3d.png",
                    if (isSinhala) "මගේ ප්‍රගතිය" else "My Progress",
                    "progress",
                    Color(0xFFFFF0DF)
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
                Text(
                    notificationTitle,
                    color = UniDarkPurple,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onNavigate("notifications") }) {
                    Text(seeAll, color = UniBlue)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("notifications") },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = UniWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                "Developed by Menaka Nuwan kumarasinghe",
                color = Color.Gray,
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
    Card(
        modifier = modifier
            .height(142.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = item.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                color = UniDarkPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
