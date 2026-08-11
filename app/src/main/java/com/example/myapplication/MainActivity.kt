package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.CategoryBlue
import com.example.myapplication.ui.theme.CategoryOrange
import com.example.myapplication.ui.theme.CategoryTeal
import com.example.myapplication.ui.theme.PhishingRed
import com.example.myapplication.ui.theme.PhishingRedContainer
import com.example.myapplication.ui.theme.SafeGreen
import com.example.myapplication.ui.theme.SafeGreenContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val requestPermissions =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissionsToRequest =
            mutableListOf(android.Manifest.permission.RECEIVE_SMS).apply {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    add(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        requestPermissions.launch(permissionsToRequest.toTypedArray())

        setContent {
            val context = LocalContext.current
            val systemDarkTheme = isSystemInDarkTheme()
            var darkModeEnabled by
                remember { mutableStateOf(ThemePrefs.isDarkModeEnabled(context, systemDarkTheme)) }

            MyApplicationTheme(darkTheme = darkModeEnabled) {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1000)
                    showSplash = false
                }

                if (showSplash) {
                    SplashScreen()
                } else {
                    PhishGuardScreen(
                        darkModeEnabled = darkModeEnabled,
                        onDarkModeChange = { enabled ->
                            darkModeEnabled = enabled
                            ThemePrefs.setDarkModeEnabled(context, enabled)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(96.dp),
        )
    }
}

enum class Verdict { PHISHING, SAFE, UNKNOWN }

fun parseVerdict(result: String): Verdict =
    when {
        result.contains("판정: 피싱") -> Verdict.PHISHING
        result.contains("판정: 정상") -> Verdict.SAFE
        else -> Verdict.UNKNOWN
    }

fun parseReason(result: String): String =
    result.substringAfter("근거:", "").trim().ifBlank { result.trim() }

private enum class Screen { HOME, HISTORY, SETTINGS }

@Composable
fun PhishGuardScreen(darkModeEnabled: Boolean, onDarkModeChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var downloadId by remember { mutableLongStateOf(-1L) }
    var progress by remember {
        mutableIntStateOf(if (ModelDownloader.isModelDownloaded(context)) 100 else 0)
    }
    var failureReason by remember { mutableStateOf<String?>(null) }
    var modelReady by remember { mutableStateOf(false) }

    var smsText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var classifying by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var historyVersion by remember { mutableIntStateOf(0) }
    var manualCheckExpanded by remember { mutableStateOf(false) }
    var scrollToBottomTrigger by remember { mutableIntStateOf(0) }
    val homeScrollState = rememberScrollState()

    LaunchedEffect(scrollToBottomTrigger) {
        if (scrollToBottomTrigger == 0) return@LaunchedEffect
        delay(100)
        homeScrollState.animateScrollTo(homeScrollState.maxValue)
    }

    LaunchedEffect(Unit) {
        if (progress != 100) {
            downloadId = ModelDownloader.enqueueDownload(context)
        }
    }

    LaunchedEffect(downloadId) {
        if (downloadId == -1L) return@LaunchedEffect
        failureReason = null
        while (progress in 0..99) {
            progress = ModelDownloader.queryProgress(context, downloadId)
            if (progress == -1) {
                failureReason = ModelDownloader.queryFailureReason(context, downloadId)
            }
            delay(500)
        }
    }

    LaunchedEffect(progress) {
        if (progress == 100 && !modelReady) {
            PhishingDetector.initialize(context)
            modelReady = true
        }
    }

    fun classify() {
        val textToClassify = smsText
        classifying = true
        scope.launch {
            result = PhishingDetector.classify(textToClassify)
            classifying = false
            val verdict = parseVerdict(result)
            if (verdict != Verdict.UNKNOWN) {
                ClassificationStats.record(context = context, isPhishing = verdict == Verdict.PHISHING)
                HistoryStore.add(
                    context = context,
                    verdict = verdict,
                    fullText = textToClassify,
                    reason = parseReason(result),
                )
                historyVersion++
            }
            scrollToBottomTrigger++
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                darkModeEnabled = darkModeEnabled,
                onDarkModeChange = onDarkModeChange,
                onClose = { scope.launch { drawerState.close() } },
            )
        },
    ) {
    Scaffold(
        bottomBar = {
            BottomNavBar(
                selectedScreen = currentScreen,
                onHomeClick = { currentScreen = Screen.HOME },
                onHistoryClick = { currentScreen = Screen.HISTORY },
                onReportClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.kisa.or.kr/118".toUri()))
                },
                onSettingsClick = { currentScreen = Screen.SETTINGS },
            )
        },
    ) { innerPadding ->
        when (currentScreen) {
            Screen.HOME ->
                Column(
                    modifier =
                        Modifier.fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(homeScrollState),
                ) {
                    HeroSection(
                        modelReady = modelReady,
                        progress = progress,
                        onMenuClick = { scope.launch { drawerState.open() } },
                    )

                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Spacer(modifier = Modifier.height(20.dp))
                        ManualCheckSection(
                            expanded = manualCheckExpanded,
                            onExpandToggle = { manualCheckExpanded = !manualCheckExpanded },
                            smsText = smsText,
                            onSmsTextChange = { smsText = it },
                            canClassify = modelReady && !classifying && smsText.isNotBlank(),
                            onClassify = ::classify,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        TrustSection()
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(
                            text = "이런 스미싱 수법도 있어요",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SmishingCaseCard(onStatsClick = { showStatsDialog = true }, historyVersion = historyVersion)
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(
                            text = "최근 검사 결과",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(12.dp))

        if (!modelReady) {
                            failureReason?.let { Text(text = it, color = PhishingRed) }
                        } else {
                            val history = remember(historyVersion) { HistoryStore.getAll(context) }
                            if (history.isEmpty()) {
                                Text(
                                    text = "아직 검사 기록이 없어요. 위에 문자 내용을 붙여넣고 판별해보세요.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    history.take(2).forEach { entry ->
                                        HistoryCard(
                                            entry = entry,
                                            onDelete = {
                                                HistoryStore.remove(context, entry.timestampMillis)
                                                historyVersion++
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            Screen.HISTORY -> {
                val history = remember(historyVersion) { HistoryStore.getAll(context) }
                HistoryScreen(
                    history = history,
                    modifier = Modifier.padding(innerPadding),
                    onDelete = { entry ->
                        HistoryStore.remove(context, entry.timestampMillis)
                        historyVersion++
                    },
                )
            }
            Screen.SETTINGS ->
                SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                    onResetData = {
                        HistoryStore.clear(context)
                        ClassificationStats.clear(context)
                        historyVersion++
                    },
                )
        }
    }
    }

    if (showStatsDialog) {
        val (phishingCount, safeCount) = ClassificationStats.getMonthlyStats(context)
        StatsDialog(
            phishingCount = phishingCount,
            safeCount = safeCount,
            onDismiss = { showStatsDialog = false },
        )
    }
}

@Composable
private fun HeroSection(
    modelReady: Boolean,
    progress: Int,
    onMenuClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "메뉴",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    text = "TextShield",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "알림",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (modelReady) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                            } else {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(
                            text = if (modelReady) "자동 감지 작동 중" else "AI 준비 중... $progress%",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text =
                                if (modelReady) "문자가 오면 자동으로 판별해서 알려드려요"
                                else "잠시만요, 모델을 불러오고 있어요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualCheckSection(
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    smsText: String,
    onSmsTextChange: (String) -> Unit,
    canClassify: Boolean,
    onClassify: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onExpandToggle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Sms,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "다른 메시지를 직접 확인하고 싶다면",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                OutlinedTextField(
                    value = smsText,
                    onValueChange = onSmsTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("카카오톡, 이메일 등 다른 메시지를 붙여넣어보세요") },
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        IconButton(onClick = onClassify, enabled = canClassify) {
                            Icon(imageVector = Icons.Filled.Send, contentDescription = "판별하기")
                        }
                    },
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        ),
                )
            }
        }
    }
}

private data class TrustPoint(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
    val color: androidx.compose.ui.graphics.Color,
)

@Composable
private fun TrustSection() {
    val points =
        listOf(
            TrustPoint(
                Icons.Filled.PhonelinkLock,
                "기기 내 처리",
                "문자가 기기 밖으로\n나가지 않아요",
                CategoryBlue,
            ),
            TrustPoint(Icons.Filled.MoneyOff, "완전 무료", "광고·결제 없이\n계속 사용해요", CategoryOrange),
            TrustPoint(
                Icons.Filled.NotificationsActive,
                "실시간 알림",
                "인터넷 없이도\n즉시 판별해요",
                CategoryTeal,
            ),
        )
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "왜 TextShield를 믿을 수 있나요",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            points.forEach { point ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = point.color.copy(alpha = 0.13f)),
                    modifier = Modifier.weight(1f).height(140.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = point.color, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = point.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Column {
                            Text(text = point.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = point.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmishingCaseCard(onStatsClick: () -> Unit, historyVersion: Int) {
    val context = LocalContext.current
    var index by remember { mutableIntStateOf(0) }
    val case = smishingCases[index]
    val (phishingCount, safeCount) = remember(historyVersion) { ClassificationStats.getMonthlyStats(context) }

    fun next() {
        index = (index + 1) % smishingCases.size
    }

    fun previous() {
        index = (index - 1 + smishingCases.size) % smishingCases.size
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier.size(200.dp)
                    .pointerInput(Unit) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                            onDragEnd = {
                                if (totalDrag < -80f) next() else if (totalDrag > 80f) previous()
                            },
                        )
                    },
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = index,
                        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.94f)) togetherWith fadeOut() },
                        label = "smishing-case",
                        modifier = Modifier.fillMaxSize(),
                    ) { animatedIndex ->
                        val animatedCase = smishingCases[animatedIndex]
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                                Text(
                                    text = animatedCase.tag,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = animatedCase.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 2,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = animatedCase.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 4,
                            )
                        }
                    }

                    Row(
                        modifier =
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        smishingCases.indices.forEach { i ->
                            Box(
                                modifier =
                                    Modifier.padding(horizontal = 2.dp)
                                        .size(if (i == index) 6.dp else 5.dp)
                                        .background(
                                            color =
                                                if (i == index) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(50),
                                        )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 3.dp,
                        modifier =
                            Modifier.align(Alignment.TopEnd).padding(8.dp).size(30.dp).clickable {
                                val query = java.net.URLEncoder.encode(case.newsQuery, "UTF-8")
                                val intent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "https://search.naver.com/search.naver?where=news&query=$query".toUri(),
                                    )
                                context.startActivity(intent)
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Article,
                                contentDescription = "네이버 기사 보기",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f).height(200.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            MiniStatCard(label = "이번 달 검사", value = "${phishingCount + safeCount}건", valueColor = MaterialTheme.colorScheme.onSurface)
            MiniStatCard(label = "차단된 피싱", value = "${phishingCount}건", valueColor = PhishingRed)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onStatsClick),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "전체 통계",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStatCard(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun StatsDialog(phishingCount: Int, safeCount: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("이번 달 검사 통계") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = PhishingRed,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(text = " 피싱 탐지", modifier = Modifier.padding(start = 4.dp))
                    }
                    Text(text = "${phishingCount}건", fontWeight = FontWeight.Bold, color = PhishingRed)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(text = " 정상 문자", modifier = Modifier.padding(start = 4.dp))
                    }
                    Text(text = "${safeCount}건", fontWeight = FontWeight.Bold, color = SafeGreen)
                }
                Text(
                    text = "총 ${phishingCount + safeCount}건의 문자를 검사했어요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("확인") } },
    )
}

private data class HistoryBadgeStyle(
    val containerColor: Color,
    val contentColor: Color,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun HistoryCard(entry: HistoryEntry, onDelete: (() -> Unit)? = null) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val badge =
        when (entry.verdict) {
            Verdict.PHISHING -> HistoryBadgeStyle(PhishingRedContainer, PhishingRed, "피싱", Icons.Filled.Warning)
            Verdict.SAFE -> HistoryBadgeStyle(SafeGreenContainer, SafeGreen, "정상", Icons.Filled.CheckCircle)
            Verdict.UNKNOWN ->
                HistoryBadgeStyle(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    "?",
                    Icons.Filled.Warning,
                )
        }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badge.containerColor,
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = badge.icon,
                            contentDescription = null,
                            tint = badge.contentColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(text = entry.snippet, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        text = formatRelativeTime(entry.timestampMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(shape = RoundedCornerShape(50), color = badge.containerColor) {
                    Text(
                        text = badge.label,
                        color = badge.contentColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
                if (onDelete != null) {
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(28.dp).padding(start = 4.dp)) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "삭제",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (entry.reason.isNotBlank()) {
                Text(
                    text = entry.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("이 기록을 삭제할까요?") },
            text = { Text("삭제하면 되돌릴 수 없어요.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete?.invoke()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PhishingRed),
                ) {
                    Text("삭제")
                }
            },
            dismissButton = { Button(onClick = { showDeleteConfirm = false }) { Text("취소") } },
        )
    }
}

private fun formatRelativeTime(timestampMillis: Long): String {
    val diffMinutes = (System.currentTimeMillis() - timestampMillis) / 60000
    return when {
        diffMinutes < 1 -> "방금 전"
        diffMinutes < 60 -> "${diffMinutes}분 전"
        diffMinutes < 60 * 24 -> "${diffMinutes / 60}시간 전"
        else -> "${diffMinutes / (60 * 24)}일 전"
    }
}

private data class BottomNavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun BottomNavBar(
    selectedScreen: Screen,
    onHomeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onReportClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val items =
        listOf(
            BottomNavItem("문자판별", Icons.Filled.Sms, selectedScreen == Screen.HOME, onHomeClick),
            BottomNavItem("검사기록", Icons.Filled.History, selectedScreen == Screen.HISTORY, onHistoryClick),
            BottomNavItem("신고하기", Icons.Filled.Report, false, onReportClick),
            BottomNavItem("설정", Icons.Filled.Settings, selectedScreen == Screen.SETTINGS, onSettingsClick),
        )

    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Row(
            modifier =
                Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { item ->
                val tint =
                    if (item.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = item.onClick,
                        ),
                ) {
                    Icon(imageVector = item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(20.dp))
                    Text(text = item.label, style = MaterialTheme.typography.labelSmall, color = tint)
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    history: List<HistoryEntry>,
    modifier: Modifier = Modifier,
    onDelete: (HistoryEntry) -> Unit,
) {
    val phishingCount = history.count { it.verdict == Verdict.PHISHING }
    val safeCount = history.count { it.verdict == Verdict.SAFE }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                    Text(
                        text = "검사 기록",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Text(
                    text = "지금까지 검사한 문자 ${history.size}건",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp, start = 34.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "피싱 탐지",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            )
                            Text(
                                text = "${phishingCount}건",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "정상 문자",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            )
                            Text(
                                text = "${safeCount}건",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }

        if (history.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "아직 검사 기록이 없어요",
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "문자판별 탭에서 첫 검사를 해보세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(history, key = { it.timestampMillis }) { entry ->
                    HistoryCard(entry = entry, onDelete = { onDelete(entry) })
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier = Modifier, onResetData: () -> Unit) {
    var showResetConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var notificationAccessEnabled by remember { mutableStateOf(MessageNotificationListener.isEnabled(context)) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    notificationAccessEnabled = MessageNotificationListener.isEnabled(context)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp),
                )
                Text(
                    text = "설정",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsSection(title = "권한") {
                SettingsActionRow(
                    icon = if (notificationAccessEnabled) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    label = if (notificationAccessEnabled) "알림 접근 허용됨" else "알림 접근 허용 필요",
                    description = "카카오톡·RCS(채팅+) 등 모든 메시지를 감지하려면 필요해요",
                    tint = if (notificationAccessEnabled) SafeGreen else PhishingRed,
                    onClick = {
                        context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                )
            }

            SettingsSection(title = "AI 모델") {
                SettingsInfoRow(label = "모델", value = "Gemma-4-E2B-it")
                SettingsInfoRow(label = "실행 방식", value = "온디바이스 (기기 내 처리)")
            }

            SettingsSection(title = "데이터") {
                SettingsActionRow(
                    icon = Icons.Filled.DeleteOutline,
                    label = "검사 기록 초기화",
                    description = "저장된 검사 기록과 통계를 모두 삭제해요",
                    tint = PhishingRed,
                    onClick = { showResetConfirm = true },
                )
            }

            SettingsSection(title = "앱 정보") {
                SettingsInfoRow(label = "앱 이름", value = "TextShield")
                SettingsInfoRow(label = "버전", value = "1.0.0")
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("검사 기록을 초기화할까요?") },
            text = { Text("저장된 검사 기록과 이번 달 통계가 모두 삭제되며, 되돌릴 수 없어요.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetData()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PhishingRed),
                ) {
                    Text("초기화")
                }
            },
            dismissButton = {
                Button(onClick = { showResetConfirm = false }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(4.dp), content = content)
        }
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = label, fontWeight = FontWeight.Bold, color = tint)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppDrawerContent(darkModeEnabled: Boolean, onDarkModeChange: (Boolean) -> Unit, onClose: () -> Unit) {
    var showAboutDialog by remember { mutableStateOf(false) }

    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "T",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                Text(
                    text = "TextShield",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Text(text = "다크 모드", modifier = Modifier.padding(start = 16.dp).weight(1f))
                Switch(checked = darkModeEnabled, onCheckedChange = onDarkModeChange)
            }

            HorizontalDivider()

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(text = "AI 모델 정보", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp))
                }
                Column(modifier = Modifier.padding(start = 38.dp, top = 8.dp)) {
                    DrawerInfoLine(label = "모델", value = "Gemma-4-E2B-it")
                    DrawerInfoLine(label = "실행 방식", value = "온디바이스 (LiteRT-LM)")
                    DrawerInfoLine(label = "버전", value = "1.0.0")
                }
            }

            HorizontalDivider()

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(text = "모델 정확도 정보", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp))
                }
                Row(
                    modifier = Modifier.padding(start = 38.dp, top = 10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    AccuracyStat(label = "정확도", value = "95%")
                    AccuracyStat(label = "오탐률", value = "0%")
                    AccuracyStat(label = "미탐률", value = "10%")
                }
                Text(
                    text = "자체 테스트 데이터셋 100건 기준",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 38.dp, top = 8.dp),
                )
            }

            HorizontalDivider()

            Row(
                modifier =
                    Modifier.fillMaxWidth().clickable { showAboutDialog = true }.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Text(text = "앱 정보 및 문의", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("TextShield") },
            text = {
                Column {
                    Text("버전 1.0.0")
                    Text(
                        text = "온디바이스 AI로 스미싱 문자를 판별하는 앱입니다.",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = { Button(onClick = { showAboutDialog = false }) { Text("확인") } },
        )
    }
}

@Composable
private fun DrawerInfoLine(label: String, value: String) {
    Text(
        text = "$label · $value",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun AccuracyStat(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
