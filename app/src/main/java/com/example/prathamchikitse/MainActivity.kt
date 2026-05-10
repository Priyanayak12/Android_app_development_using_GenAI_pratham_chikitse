package com.example.prathamchikitse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prathamchikitse.ui.theme.PrathamChikitseTheme
import kotlinx.coroutines.launch
import java.util.*

// --- DATA MODELS ---
enum class BottomTab { HOME, INFO, KIT, QUIZ }

enum class AudioState(val label: String, val color: Color) {
    OFF("AUDIO: OFF", Color.DarkGray),
    ENGLISH("AUDIO: ENGLISH", Color(0xFF1565C0)),
    KANNADA("AUDIO: KANNADA", Color(0xFF2E7D32)),
    BOTH("AUDIO: BOTH", Color(0xFF6A1B9A))
}

data class KitItem(val name: String, val imageResId: Int)
data class Hospital(val name: String, val distance: String, val status: String)

data class EmergencyItem(
    val id: Int, val name: String, val kannadaName: String, val color: Color, val imageResId: Int,
    val steps: List<FirstAidStep>, val dos: List<Pair<String, String>>, val donts: List<Pair<String, String>>
)

data class FirstAidStep(val stepNumber: Int, val instruction: String, val kannadaInstruction: String)

data class Question(
    val question: String,
    val kanQuestion: String,
    val options: List<String>,
    val kanOptions: List<String>,
    val correctAnswer: Int
)

// --- MAIN ACTIVITY ---
class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        setContent {
            PrathamChikitseTheme {
                var showSplash by remember { mutableStateOf(true) }
                var showHospitals by remember { mutableStateOf(false) }
                var selectedEmergency by remember { mutableStateOf<EmergencyItem?>(null) }
                var currentBottomTab by remember { mutableStateOf(BottomTab.HOME) }

                if (showSplash) {
                    FrontPageScreen(onEnter = { showSplash = false })
                } else {
                    Scaffold(
                        bottomBar = {
                            if (selectedEmergency == null && !showHospitals) {
                                NavigationBar(containerColor = Color.White, modifier = Modifier.shadow(8.dp)) {
                                    val tabs = listOf(
                                        Triple(BottomTab.HOME, Icons.Default.Home, "Home"),
                                        Triple(BottomTab.INFO, Icons.Default.Info, "Info"),
                                        Triple(BottomTab.KIT, Icons.Default.Favorite, "Kit"),
                                        Triple(BottomTab.QUIZ, Icons.Default.CheckCircle, "Quiz")
                                    )
                                    tabs.forEach { (tab, icon, label) ->
                                        NavigationBarItem(
                                            selected = currentBottomTab == tab,
                                            onClick = { currentBottomTab = tab },
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color(0xFFD32F2F),
                                                unselectedIconColor = Color.Gray,
                                                selectedTextColor = Color(0xFFD32F2F),
                                                indicatorColor = Color(0xFFFFEBEE)
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            if (selectedEmergency == null && !showHospitals && currentBottomTab == BottomTab.HOME) {
                                ExtendedFloatingActionButton(
                                    onClick = { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:108"))) },
                                    containerColor = Color(0xFFD32F2F), contentColor = Color.White,
                                    icon = { Icon(Icons.Default.Call, "Call SOS") },
                                    text = { Text("CALL 108", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                                )
                            }
                        }
                    ) { padding ->
                        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = Color(0xFFF8F9FA)) {
                            if (showHospitals) {
                                HospitalFinderScreen(onBack = { showHospitals = false })
                            } else if (selectedEmergency != null) {
                                DetailScreen(
                                    item = selectedEmergency!!,
                                    onBack = { selectedEmergency = null },
                                    onSpeak = { text -> tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) },
                                    tts = tts
                                )
                            } else {
                                when (currentBottomTab) {
                                    BottomTab.HOME -> HomeScreen(onItemClick = { selectedEmergency = it }, onFindHospitalsClick = { showHospitals = true })
                                    BottomTab.INFO -> InfoScreen()
                                    BottomTab.KIT -> KitScreen()
                                    BottomTab.QUIZ -> QuizScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("kn", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale("en", "IN")) // Fallback
            }
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}

// --- SCREENS & COMPONENTS ---

@Composable
fun QuizScreen() {
    val questions = remember {
        listOf(
            Question("What is the first step in CPR?", "ಸಿಪಿಆರ್ನಲ್ಲಿ ಮೊದಲ ಹಂತ ಯಾವುದು?", listOf("Check response & Call 108", "Give water", "Check pulse"), listOf("ಸ್ಪಂದನೆ ಪರೀಕ್ಷಿಸಿ ಮತ್ತು 108 ಗೆ ಕರೆ ಮಾಡಿ", "ನೀರು ಕೊಡಿ", "ನಾಡಿ ಪರೀಕ್ಷಿಸಿ"), 0),
            Question("Should you suck out snake venom?", "ಹಾವಿನ ವಿಷವನ್ನು ಬಾಯಿಂದ ಹೀರಬಹುದೇ?", listOf("Yes", "No", "Only for cobras"), listOf("ಹೌದು", "ಇಲ್ಲ", "ಕೇವಲ ನಾಗರಹಾವಿಗೆ"), 1),
            Question("SOS Number for ambulance in India?", "ಭಾರತದಲ್ಲಿ ಆಂಬ್ಯುಲೆನ್ಸ್ಗೆ ಕರೆ ಮಾಡಲು ಸಂಖ್ಯೆ ಯಾವುದು?", listOf("100", "101", "108"), listOf("100", "101", "108"), 2),
            Question("How to treat a minor burn?", "ಸಣ್ಣ ಸುಟ್ಟ ಗಾಯಕ್ಕೆ ಹೇಗೆ ಚಿಕಿತ್ಸೆ ನೀಡಬೇಕು?", listOf("Apply Butter", "Run Cool Water", "Apply Toothpaste"), listOf("ಬೆಣ್ಣೆ ಹಚ್ಚಿ", "ತಣ್ಣೀರು ಹಾಕಿ", "ಟೂತ್ಪೇಸ್ಟ್ ಹಚ್ಚಿ"), 1),
            Question("What to do if someone is choking?", "ಯಾರಿಗಾದರೂ ಉಸಿರು ಕಟ್ಟಿದರೆ ಏನು ಮಾಡಬೇಕು?", listOf("Give Water", "Abdominal Thrusts", "Make them lie down"), listOf("ನೀರು ಕೊಡಿ", "ಹೊಟ್ಟೆಯ ಭಾಗ ಒತ್ತಿರಿ (Thrusts)", "ಅವರನ್ನು ಮಲಗಿಸಿ"), 1),
            Question("How to stop severe bleeding?", "ತೀವ್ರ ರಕ್ತಸ್ರಾವ ನಿಲ್ಲಿಸುವುದು ಹೇಗೆ?", listOf("Direct Pressure", "Apply Oil", "Wash with hot water"), listOf("ನೇರ ಒತ್ತಡ ಹಾಕಿ", "ಎಣ್ಣೆ ಹಚ್ಚಿ", "ಬಿಸಿ ನೀರಿನಿಂದ ತೊಳೆಯಿರಿ"), 0),
            Question("Proper position for nosebleed?", "ಮೂಗಿನ ರಕ್ತಸ್ರಾವಕ್ಕೆ ಸರಿಯಾದ ಸ್ಥಿತಿ ಯಾವುದು?", listOf("Lean Back", "Lean Forward", "Lie down"), listOf("ಹಿಂದಕ್ಕೆ ಬಾಗಿ", "ಮುಂದಕ್ಕೆ ಬಾಗಿ", "ಮಲಗಿ"), 1),
            Question("Where to put a broken tooth?", "ಮುರಿದ ಹಲ್ಲನ್ನು ಎಲ್ಲಿ ಇಡಬೇಕು?", listOf("In Milk/Saliva", "In Hot Water", "In a Dry Tissue"), listOf("ಹಾಲು ಅಥವಾ ಲಾಲಾರಸದಲ್ಲಿ", "ಬಿಸಿ ನೀರಿನಲ್ಲಿ", "ಒಣ ಟಿಶ್ಯೂನಲ್ಲಿ"), 0),
            Question("What to do during a seizure?", "ಸೆಳೆತ (Fits) ಬಂದಾಗ ಏನು ಮಾಡಬೇಕು?", listOf("Put spoon in mouth", "Protect head & Stay calm", "Hold them tight"), listOf("ಬಾಯಲ್ಲಿ ಚಮಚ ಇಡಿ", "ತಲೆ ರಕ್ಷಿಸಿ ಮತ್ತು ಶಾಂತವಾಗಿರಿ", "ಬಲವಾಗಿ ಹಿಡಿಯಿರಿ"), 1),
            Question("What to give a conscious diabetic in shock?", "ಮಧುಮೇಹ ಆಘಾತವಾದಾಗ ಎಚ್ಚರವಿದ್ದವರಿಗೆ ಏನು ನೀಡಬೇಕು?", listOf("Insulin", "Sugar/Candy", "Water"), listOf("ಇನ್ಸುಲಿನ್", "ಸಕ್ಕರೆ/ಸಿಹಿ", "ನೀರು"), 1)
        )
    }

    var currentQuestionIdx by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var showResult by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope() // FIXED: Added missing CoroutineScope

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFFF5252), Color(0xFFD32F2F)))).padding(24.dp).padding(top = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("First Aid Quiz", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Test your readiness..", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                }
                Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(50.dp))
            }
        }

        if (showResult) {
            QuizResultScreen(score = score, total = questions.size) {
                currentQuestionIdx = 0
                score = 0
                showResult = false
            }
        } else {
            val q = questions[currentQuestionIdx]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { (currentQuestionIdx + 1).toFloat() / questions.size },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = Color(0xFFD32F2F),
                    trackColor = Color(0xFFFFEBEE),
                )
                Spacer(Modifier.height(12.dp))
                Text("Question ${currentQuestionIdx + 1} of ${questions.size}", fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(q.question, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.Black, textAlign = TextAlign.Center, lineHeight = 28.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(q.kanQuestion, fontSize = 18.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, lineHeight = 24.sp)
                    }
                }
                Spacer(Modifier.height(32.dp))

                q.options.forEachIndexed { index, option ->
                    Button(
                        onClick = {
                            if (index == q.correctAnswer) score++
                            if (currentQuestionIdx < questions.size - 1) {
                                currentQuestionIdx++
                                scope.launch { scrollState.scrollTo(0) }
                            } else {
                                showResult = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        elevation = ButtonDefaults.buttonElevation(4.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(option, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(q.kanOptions[index], fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 18.sp)
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun QuizResultScreen(score: Int, total: Int, onRestart: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(150.dp).background(Color(0xFFFFD700).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Text(if (score > 7) "🏆" else "💪", fontSize = 80.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text("Quiz Completed!", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text("Your Score: $score / $total", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
        Spacer(Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (score > 7) "Excellent! You are a First-Aid Hero." else "Good effort! Keep learning to be more prepared.", textAlign = TextAlign.Center, fontSize = 16.sp)
                if (score > 7) {
                    Spacer(Modifier.height(16.dp))
                    Text("CERTIFICATE OF READINESS EARNED", fontWeight = FontWeight.ExtraBold, color = Color(0xFFB8860B), fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(48.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))) {
            Text("RETRY QUIZ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun InfoScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Glossary")
    var selectedInfoTitle by remember { mutableStateOf<String?>(null) }
    var selectedInfoContent by remember { mutableStateOf<String?>(null) }

    if (selectedInfoTitle != null && selectedInfoContent != null) {
        AlertDialog(
            onDismissRequest = { selectedInfoTitle = null; selectedInfoContent = null },
            title = { Text(text = selectedInfoTitle!!, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
            text = { Text(text = selectedInfoContent!!, fontSize = 16.sp, lineHeight = 24.sp, color = Color.Black) },
            confirmButton = {
                TextButton(onClick = { selectedInfoTitle = null; selectedInfoContent = null }) {
                    Text("Close", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFFF5252), Color(0xFFD32F2F)))).padding(24.dp).padding(top = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Information", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("You Should Know..", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                }
                Icon(Icons.Default.Info, contentDescription = "Info Icon", tint = Color.White, modifier = Modifier.size(50.dp))
            }
        }

        TabRow(selectedTabIndex = selectedTab, containerColor = Color.White, contentColor = Color(0xFFFFC107)) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, color = if (selectedTab == index) Color(0xFFFFC107) else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                )
            }
        }

        if (selectedTab == 0) {
            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                val infoCards = listOf(
                    Triple("What is First Aid", Icons.Default.Add, "First aid is the first and immediate assistance given to any person suffering from either a minor or serious illness or injury..."),
                    Triple("Why Learn First Aid", Icons.Default.Person, "Learning first aid empowers you to help people in need. It can save lives and reduce recovery time..."),
                    Triple("Aims of First Aid", Icons.Default.CheckCircle, "The main aims of first aid are the 3 P's:\n\n• Preserve life\n• Prevent further injury\n• Promote recovery"),
                    Triple("First Aid Kit Info", Icons.Default.Favorite, "A well-stocked first-aid kit can help you respond effectively to common injuries and emergencies.")
                )
                items(infoCards.size) { index ->
                    val card = infoCards[index]
                    Card(
                        modifier = Modifier.fillMaxWidth().height(150.dp).clickable { selectedInfoTitle = card.first; selectedInfoContent = card.third },
                        colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp), shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(imageVector = card.second, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color(0xFFD32F2F))
                            Spacer(Modifier.height(12.dp))
                            Text(card.first, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black, fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            val glossaryTerms = listOf(
                "CPR" to "An emergency lifesaving procedure performed when the heart stops beating.",
                "AED" to "A portable device that checks the heart rhythm and can send an electric shock.",
                "Tourniquet" to "A device used to stop severe bleeding from a limb.",
                "Anaphylaxis" to "A severe, potentially life-threatening allergic reaction.",
                "Asphyxia" to "A condition where the body is deprived of oxygen.",
                "Shock" to "A critical condition caused by a sudden drop in blood flow."
            )
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                items(glossaryTerms.size) { index ->
                    val term = glossaryTerms[index]
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(term.first, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFD32F2F))
                            Spacer(Modifier.height(6.dp))
                            Text(term.second, fontSize = 15.sp, color = Color.Black, lineHeight = 22.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KitScreen() {
    val kitItems = listOf(
        KitItem("gauze", R.drawable.gauze),
        KitItem("adhesive tape", R.drawable.adhesive_tape),
        KitItem("adhesive bandages", R.drawable.adhesive_bandages),
        KitItem("elastic bandage", R.drawable.elastic_bandage),
        KitItem("a splint", R.drawable.a_splint),
        KitItem("wipes", R.drawable.wipes),
        KitItem("soap", R.drawable.soap),
        KitItem("antibiotic ointment", R.drawable.antibiotic_ointment),
        KitItem("antiseptic solution", R.drawable.antiseptic_solution),
        KitItem("hydrocortisone cream", R.drawable.hydrocortisone_cream),
        KitItem("acetaminophen & ibuprofen", R.drawable.acetaminophen_ibuprofen),
        KitItem("medications", R.drawable.medications),
        KitItem("tweezers", R.drawable.tweezers),
        KitItem("sharp scissors", R.drawable.sharp_scissors),
        KitItem("safety pins", R.drawable.safety_pins),
        KitItem("cold packs", R.drawable.cold_packs),
        KitItem("calamine lotion", R.drawable.calamine_lotion),
        KitItem("thermometer", R.drawable.thermometer),
        KitItem("tooth preservation kit", R.drawable.tooth_preservation_kit),
        KitItem("gloves", R.drawable.gloves),
        KitItem("flashlight and extra batteries", R.drawable.flashlight_and_batteries),
        KitItem("a blanket", R.drawable.a_blanket),
        KitItem("mouthpiece CPR", R.drawable.mouthpiece_cpr),
        KitItem("emergency numbers", R.drawable.emergency_numbers)
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFFF5252), Color(0xFFD32F2F)))).padding(24.dp).padding(top = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("First Aid Kit", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Always be prepared..", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                }
                Icon(Icons.Default.Favorite, contentDescription = "Kit Icon", tint = Color.White, modifier = Modifier.size(50.dp))
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
            items(kitItems.size) { index ->
                val item = kitItems[index]
                Card(modifier = Modifier.fillMaxWidth().height(180.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(painter = painterResource(id = item.imageResId), contentDescription = item.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))), contentAlignment = Alignment.BottomCenter) {
                            Text(text = item.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp, end = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MedicalIllustration(color: Color, imageResId: Int) {
    Box(
        modifier = Modifier.fillMaxWidth().height(220.dp).background(
            brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.15f), color.copy(alpha = 0.03f))),
            shape = RoundedCornerShape(20.dp)
        ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = "Medical Illustration",
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun FrontPageScreen(onEnter: () -> Unit) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFFD32F2F), Color(0xFF880E4F)))
    Box(modifier = Modifier.fillMaxSize().background(gradient), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(190.dp).shadow(24.dp, CircleShape).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.width(22.dp).height(70.dp).background(Color(0xFFD32F2F), RoundedCornerShape(6.dp)))
                        Box(modifier = Modifier.width(70.dp).height(22.dp).background(Color(0xFFD32F2F), RoundedCornerShape(6.dp)))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("PRATHAM", color = Color(0xFFB71C1C), fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("CHIKITSE", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
            Text("Emergency First-Aid Guide\nಪ್ರಥಮ ಚಿಕಿತ್ಸೆ ಮಾರ್ಗದರ್ಶಿ", color = Color.White.copy(alpha = 0.95f), fontSize = 20.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, lineHeight = 30.sp)
            Spacer(modifier = Modifier.height(72.dp))
            Button(
                onClick = onEnter, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFB71C1C)),
                shape = RoundedCornerShape(32.dp), modifier = Modifier.height(60.dp), elevation = ButtonDefaults.buttonElevation(12.dp)
            ) {
                Text("ENTER GUIDE  ➔", fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onItemClick: (EmergencyItem) -> Unit, onFindHospitalsClick: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredList = emergencies.filter { it.name.contains(searchQuery, true) || it.kannadaName.contains(searchQuery) }

    Column {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFD32F2F), Color(0xFFB71C1C))))) {
            Column(Modifier.padding(20.dp).padding(top = 16.dp)) {
                Text("Pratham-Chikitse", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("Select an Emergency Below", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(14.dp)),
                    placeholder = { Text("Search Emergency...", color = Color.Gray) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    textStyle = TextStyle(color = Color.Black, fontSize = 16.sp), shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent), singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onFindHospitalsClick, modifier = Modifier.fillMaxWidth().height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFB71C1C)),
                    shape = RoundedCornerShape(14.dp), elevation = ButtonDefaults.buttonElevation(6.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("FIND NEARBY HOSPITALS", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredList) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().height(180.dp).clickable { onItemClick(item) },
                    shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(item.color.copy(alpha = 0.75f), item.color)))) {
                        Image(
                            painter = painterResource(id = item.imageResId),
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.BottomEnd).offset(20.dp, 20.dp).size(90.dp).alpha(0.2f),
                            contentScale = ContentScale.Crop
                        )
                        Column(Modifier.padding(16.dp).fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(id = item.imageResId),
                                contentDescription = null,
                                modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.White).padding(4.dp),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(item.name, color = Color.White, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, fontSize = 15.sp, lineHeight = 18.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(item.kannadaName, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(item: EmergencyItem, onBack: () -> Unit, onSpeak: (String) -> Unit, tts: TextToSpeech) {
    val pagerState = rememberPagerState(pageCount = { item.steps.size })
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var audioState by remember { mutableStateOf(AudioState.OFF) }

    LaunchedEffect(pagerState.currentPage, audioState) {
        if (audioState != AudioState.OFF) {
            val currentStep = item.steps[pagerState.currentPage]
            if (audioState == AudioState.ENGLISH) tts.language = Locale("en", "IN") else tts.language = Locale("kn", "IN")
            val textToSpeak = when (audioState) {
                AudioState.ENGLISH -> currentStep.instruction
                AudioState.KANNADA -> currentStep.kannadaInstruction
                AudioState.BOTH -> "${currentStep.instruction}. ${currentStep.kannadaInstruction}"
                AudioState.OFF -> ""
            }
            onSpeak(textToSpeak)
        } else tts.stop()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(item.name, fontWeight = FontWeight.Black, color = Color.White, fontSize = 22.sp) },
                navigationIcon = { IconButton(onClick = { tts.stop(); onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = item.color)
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    audioState = when(audioState) {
                        AudioState.OFF -> AudioState.ENGLISH
                        AudioState.ENGLISH -> AudioState.KANNADA
                        AudioState.KANNADA -> AudioState.BOTH
                        AudioState.BOTH -> AudioState.OFF
                    }
                },
                containerColor = audioState.color, contentColor = Color.White,
                icon = { Icon(imageVector = if (audioState == AudioState.OFF) Icons.Default.Close else Icons.Default.PlayArrow, contentDescription = "Toggle Audio") },
                text = { Text(audioState.label, fontWeight = FontWeight.Black) },
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.fillMaxSize()) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().wrapContentHeight().heightIn(min = 440.dp)) { page ->
                        Column(Modifier.padding(24.dp).fillMaxWidth().wrapContentHeight(), Arrangement.Top, Alignment.CenterHorizontally) {
                            MedicalIllustration(color = item.color, imageResId = item.imageResId)
                            Spacer(Modifier.height(26.dp))
                            Text("STEP ${page + 1} OF ${item.steps.size}", color = item.color, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, fontSize = 14.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(item.steps[page].instruction, color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 30.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(item.steps[page].kannadaInstruction, fontSize = 20.sp, color = Color(0xFF2E7D32), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium, lineHeight = 28.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth().background(Color(0xFFF8F9FA)).padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }, enabled = pagerState.currentPage > 0) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, Modifier.size(36.dp), tint = if (pagerState.currentPage > 0) Color.Black else Color.LightGray)
                        }
                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            repeat(item.steps.size) { i ->
                                val isSelected = pagerState.currentPage == i
                                val width by animateDpAsState(targetValue = if (isSelected) 24.dp else 8.dp, label = "dot")
                                Box(Modifier.padding(4.dp).height(8.dp).width(width).background(if (isSelected) item.color else Color.LightGray, CircleShape))
                            }
                        }
                        IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }, enabled = pagerState.currentPage < item.steps.size - 1) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(36.dp), tint = if (pagerState.currentPage < item.steps.size - 1) Color.Black else Color.LightGray)
                        }
                    }
                }
            }
            EmergencyAdviceCard("Do's / ಮಾಡಿ", item.dos, Color(0xFF2E7D32))
            EmergencyAdviceCard("Don'ts / ಮಾಡಬೇಡಿ", item.donts, Color(0xFFB71C1C))
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun EmergencyAdviceCard(title: String, items: List<Pair<String, String>>, themeColor: Color) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.fillMaxHeight().width(10.dp).background(themeColor))
            Column(Modifier.padding(20.dp)) {
                Text(title.uppercase(), style = TextStyle(color = themeColor, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp))
                Spacer(Modifier.height(12.dp))
                items.forEach { (eng, kan) ->
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(if (themeColor == Color(0xFF2E7D32)) Icons.Default.Check else Icons.Default.Close, null, tint = themeColor, modifier = Modifier.size(20.dp).padding(top=2.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(eng, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(kan, color = Color.DarkGray, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalFinderScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nearby Hospitals", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFB71C1C))
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(Color(0xFFF8F9FA))) {
            Box(Modifier.fillMaxWidth().height(180.dp).background(Color(0xFFE0E0E0)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(64.dp), tint = Color(0xFFB71C1C))
                Text("Simulated Offline Map View", color = Color.Gray, modifier = Modifier.padding(top = 80.dp), fontWeight = FontWeight.Medium)
            }
            Column(Modifier.padding(16.dp)) {
                Text("Nearest Emergency Centers", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                Spacer(Modifier.height(16.dp))
                listOf(
                    Hospital("City General Hospital", "1.2 km away", "Open 24/7"),
                    Hospital("Sanjeevini Emergency Care", "3.5 km away", "Open 24/7"),
                    Hospital("Fortis Medical Center", "5.0 km away", "Open 24/7")
                ).forEach { hospital ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(hospital.name, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black)
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(hospital.distance, color = Color.DarkGray, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(16.dp))
                                    Text("• ${hospital.status}", color = Color(0xFF2E7D32), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- OFFLINE DATA REQUIRING DRAWABLE ASSETS -----------------
val emergencies = listOf(
    EmergencyItem(
        1, "CPR (Life Saving)", "ಸಿಪಿಆರ್ (ಜೀವ ರಕ್ಷಕ)", Color(0xFFD32F2F), R.drawable.cpr_illustration,
        listOf(
            FirstAidStep(1, "Check for responsiveness and call 108 immediately.", "ಸ್ಪಂದನೆ ಪರೀಕ್ಷಿಸಿ ಮತ್ತು ತಕ್ಷಣ 108 ಗೆ ಕರೆ ಮಾಡಿ."),
            FirstAidStep(2, "Lay the person flat on their back on a firm surface.", "ವ್ಯಕ್ತಿಯನ್ನು ಸಮತಟ್ಟಾದ ಗಟ್ಟಿಯಾದ ಮೇಲ್ಮೈಯಲ್ಲಿ ಮಲಗಿಸಿ."),
            FirstAidStep(3, "Place the heel of one hand in the center of their chest.", "ಅವರ ಎದೆಯ ಮಧ್ಯದಲ್ಲಿ ನಿಮ್ಮ ಒಂದು ಕೈಯನ್ನು ಇರಿಸಿ."),
            FirstAidStep(4, "Interlock your fingers and keep your elbows completely straight.", "ನಿಮ್ಮ ಬೆರಳುಗಳನ್ನು ಲಾಕ್ ಮಾಡಿ och ಮೊಣಕೈಗಳನ್ನು ನೇರವಾಗಿರಿಸಿ."),
            FirstAidStep(5, "Push hard and fast (2 inches deep, 100-120 pushes per minute).", "ಜೋರಾಗಿ ಮತ್ತು ವೇಗವಾಗಿ ಒತ್ತಿರಿ (ನಿಮಿಷಕ್ಕೆ 100-120 ಬಾರಿ).")
        ),
        listOf("Keep elbows straight" to "ಮೊಣಕೈಗಳನ್ನು ನೇರವಾಗಿ ಇರಿಸಿ", "Push hard" to "ಜೋರಾಗಿ ಒತ್ತಿ"),
        listOf("Don't lean on chest between pushes" to "ಎದೆಯ ಮೇಲೆ ಭಾರ ಹಾಕಬೇಡಿ", "Don't stop" to "ನಿಲ್ಲಿಸಬೇಡಿ")
    ),
    EmergencyItem(
        2, "Heart Attack", "ಹೃದಯಾಘಾತ", Color(0xFFB71C1C), R.drawable.heart_attack_illustration,
        listOf(
            FirstAidStep(1, "Make them sit down comfortably and keep them calm.", "ಅವರನ್ನು ಆರಾಮವಾಗಿ ಕುಳಿತುಕೊಳ್ಳುವಂತೆ ಮಾಡಿ ಶಾಂತಗೊಳಿಸಿ."),
            FirstAidStep(2, "Loosen any tight clothing around their neck and chest.", "ಕುತ್ತಿಗೆ och ಎದೆಯ ಸುತ್ತಲಿನ ಬಿಗಿಯಾದ ಬಟ್ಟೆಯನ್ನು ಸಡಿಲಗೊಳಿಸಿ."),
            FirstAidStep(3, "Ask if they take heart medicine and help them take it.", "ಅವರು ಹೃದಯದ ಔಷಧಿ ತೆಗೆದುಕೊಳ್ಳುತ್ತಿದ್ದರೆ ಅದನ್ನು ನೀಡಲು ಸಹಾಯ ಮಾಡಿ."),
            FirstAidStep(4, "Call 108 immediately for an ambulance.", "ತಕ್ಷಣವೇ 108 ಆಂಬ್ಯುಲೆನ್ಸ್ ಗೆ ಕರೆ ಮಾಡಿ."),
            FirstAidStep(5, "If they become unconscious and stop breathing, begin CPR.", "ಅವರು ಪ್ರಜ್ಞೆ ತಪ್ಪಿ ಉಸಿರಾಟ ನಿಲ್ಲಿಸಿದರೆ, ತಕ್ಷಣ ಸಿಪಿಆರ್ ಪ್ರಾರಂಭಿಸಿ.")
        ),
        listOf("Keep them awake" to "ಎಚ್ಚರದಿಂದಿರಲು ಹೇಳಿ", "Keep warm" to "ಬೆಚ್ಚಗೆ ಇರಿಸಿ"),
        listOf("Don't let them walk" to "ಅವರನ್ನು ನಡೆಯಲು ಬಿಡಬೇಡಿ", "Don't give heavy food" to "ಭಾರೀ ಆಹಾರ ನೀಡಬೇಡಿ")
    ),
    EmergencyItem(
        3, "Snake Bite", "ಹಾವಿನ ಕಡಿತ", Color(0xFF2E7D32), R.drawable.snake_bite_illustration,
        listOf(
            FirstAidStep(1, "Keep the person completely still to slow the venom spread.", "ವಿಷ ಹರಡುವುದನ್ನು ನಿಧಾನಗೊಳಿಸಲು ವ್ಯಕ್ತಿಯನ್ನು ಅಲುಗಾಡದಂತೆ ನೋಡಿಕೊಳ್ಳಿ."),
            FirstAidStep(2, "Wash the bite area gently with soap and water.", "ಕಚ್ಚಿದ ಭಾಗವನ್ನು ಸೋಪು ಮತ್ತು ನೀರಿನಿಂದ ನಿಧಾನವಾಗಿ ತೊಳೆಯಿರಿ."),
            FirstAidStep(3, "Keep the bitten area loosely supported below heart level.", "ಕಚ್ಚಿದ ಭಾಗವನ್ನು ಸಡಿಲವಾಗಿ ಹಿಡಿದುಕೊಳ್ಳಿ ಮತ್ತು ಹೃದಯಕ್ಕಿಂತ ಕೆಳಮಟ್ಟದಲ್ಲಿರಿಸಿ."),
            FirstAidStep(4, "Remove tight rings, watches, or clothing near the bite.", "ಕಚ್ಚಿದ ಭಾಗದ ಹತ್ತಿರವಿರುವ ಉಂಗುರ, ವಾಚ್, ಬಟ್ಟೆಗಳನ್ನು ತೆಗೆಯಿರಿ."),
            FirstAidStep(5, "Get to a hospital safely; do NOT attempt to suck the venom.", "ತಕ್ಷಣ ಆಸ್ಪತ್ರೆಗೆ ಹೋಗಿ; ವಿಷಯವನ್ನು ಬಾಯಿಂದ ಹೀರಲು ಒದ್ದಾಡಬೇಡಿ.")
        ),
        listOf("Note the snake's color" to "ಹಾವಿನ ಬಣ್ಣ ಗಮನಿಸಿ", "Stay calm" to "ಶಾಂತವಾಗಿರಿ"),
        listOf("Don't tie a tight tourniquet" to "ಬಿಗಿಯಾಗಿ ಕಟ್ಟಬೇಡಿ", "Don't cut wound" to "ಗಾಯವನ್ನು ಕೊಯ್ಯಬೇಡಿ")
    ),
    EmergencyItem(
        4, "Severe Bleeding", "ತೀವ್ರ ರಕ್ತಸ್ರಾವ", Color(0xFF880E4F), R.drawable.severe_bleeding_illustration,
        listOf(
            FirstAidStep(1, "Ensure safety and put on gloves if available.", "ಕೈಗವಸುಗಳಿದ್ದರೆ ಧರಿಸಿ, ಸುರಕ್ಷತೆ ಖಚಿತಪಡಿಸಿಕೊಳ್ಳಿ."),
            FirstAidStep(2, "Apply direct, firm pressure to the wound with a clean cloth.", "ಶುದ್ಧ ಬಟ್ಟೆಯಿಂದ ಗಾಯದ ಮೇಲೆ ನೇರವಾಗಿ ಬಲವಾಗಿ ಒತ್ತಿ ಹಿಡಿಯಿರಿ."),
            FirstAidStep(3, "Keep the pressure steady for at least 5 to 10 minutes.", "ಕನಿಷ್ಠ 5 ರಿಂದ 10 ನಿಮಿಷಗಳ ಕಾಲ ಒತ್ತಡವನ್ನು ಹಾಗೆಯೇ ಇರಿಸಿ."),
            FirstAidStep(4, "Elevate the bleeding limb above the heart if possible.", "ಸಾಧ್ಯವಾದರೆ ರಕ್ತಸ್ರಾವವಾಗುತ್ತಿರುವ ಭಾಗವನ್ನು ಹೃದಯಕ್ಕಿಂತ ಮೇಲೆತ್ತಿ."),
            FirstAidStep(5, "If blood soaks through, add another cloth on top; don't remove the first.", "ರಕ್ತ ನೆನೆದಿದ್ದರೆ ಮೊದಲ ಬಟ್ಟೆಯನ್ನು ತೆಗೆಯಬೇಡಿ, ಅದರ ಮೇಲೆಯೇ ಇನ್ನೊಂದು ಬಟ್ಟೆ ಇಡಿ.")
        ),
        listOf("Apply pressure" to "ಒತ್ತಡ ಹಾಕಿ", "Lie person down" to "ವ್ಯಕ್ತಿಯನ್ನು ಮಲಗಿಸಿ"),
        listOf("Don't remove original cloth" to "ಮೊದಲ ಬಟ್ಟೆ ತೆಗೆಯಬೇಡಿ", "Don't wash deep wounds" to "ಆಳವಾದ ಗಾಯ ತೊಳೆಯಬೇಡಿ")
    ),
    EmergencyItem(
        5, "Choking", "ಶ್ವಾಸ ತಡೆ", Color(0xFF1976D2), R.drawable.choking_illustration,
        listOf(
            FirstAidStep(1, "Ask 'Are you choking?' and encourage them to cough.", "'ನೀವು ಉಸಿರುಗಟ್ಟುತ್ತಿದ್ದೀರಾ?' ಎಂದು ಕೇಳಿ ಕೆಮ್ಮಲು ಹೇಳಿ."),
            FirstAidStep(2, "If they cannot breathe, stand behind and wrap arms around waist.", "ಅವರಿಗೆ ಉಸಿರಾಡಲು ಆಗದಿದ್ದರೆ, ಹಿಂದೆ ನಿಂತು ನಡುವನ್ನು ಹಿಡಿಯಿರಿ."),
            FirstAidStep(3, "Make a fist and place it just above their belly button.", "ಕೈಮುಷ್ಟಿ ಮಾಡಿ ಅವರ ಹೊಟ್ಟೆಯ ಭಾಗಕ್ಕೆ ಇರಿಸಿ."),
            FirstAidStep(4, "Grab your fist with the other hand and give quick upward thrusts.", "ಮತ್ತೊಂದು ಕೈಯಿಂದ ಮುಷ್ಟಿಯನ್ನು ಹಿಡಿದು ವೇಗವಾಗಿ ಮೇಲಕ್ಕೆ ಒತ್ತಿರಿ."),
            FirstAidStep(5, "Repeat thrusts until the object pops out or they faint.", "ವಸ್ತು ಹೊರಬರುವವರೆಗೆ ಅಥವಾ ಪ್ರಜ್ಞೆ ತಪ್ಪುವವರೆಗೆ ಪುನರಾವರ್ತಿಸಿ.")
        ),
        listOf("Encourage coughing if speaking" to "ಮಾತನಾಡುತ್ತಿದ್ದರೆ ಕೆಮ್ಮಲು ಹೇಳಿ", "Act quickly" to "ವೇಗವಾಗಿ ಕಾರ್ಯನಿರ್ವಹಿಸಿ"),
        listOf("Don't blindly poke mouth" to "ಬಾಯಿಯೊಳಗೆ ಬೆರಳು ಹಾಕಬೇಡಿ", "Don't give water" to "ನೀರು ಕೊಡಬೇಡಿ")
    ),
    EmergencyItem(
        6, "Drowning", "ನೀರಿನಲ್ಲಿ ಮುಳುಗುವುದು", Color(0xFF0288D1), R.drawable.drowning_illustration,
        listOf(
            FirstAidStep(1, "Take the person out of the water immediately and safely.", "ವ್ಯಕ್ತಿಯನ್ನು ತಕ್ಷಣವೇ ಮತ್ತು ಸುರಕ್ಷಿತವಾಗಿ ನೀರಿನಿಂದ ಹೊರತನ್ನಿ."),
            FirstAidStep(2, "Lay them flat on their back and check for breathing.", "ಅವರನ್ನು ಮಲಗಿಸಿ ಮತ್ತು ಉಸಿರಾಟವನ್ನು ಪರೀಕ್ಷಿಸಿ."),
            FirstAidStep(3, "If not breathing, slightly tilt their head back to open airway.", "ಉಸಿರಾಟವಿಲ್ಲದಿದ್ದರೆ, ತಲೆಯನ್ನು ಸ್ವಲ್ಪ ಹಿಂದಕ್ಕೆ ಬಾಗಿಸಿ ಶ್ವಾಸಮಾರ್ಗ ತೆರೆಯಿರಿ."),
            FirstAidStep(4, "Begin CPR immediately (chest compressions first).", "ತಕ್ಷಣವೇ ಸಿಪಿಆರ್ (ಎದೆಯೊತ್ತುವಿಕೆ) ಪ್ರಾರಂಭಿಸಿ."),
            FirstAidStep(5, "Cover them with a warm blanket to prevent severe chilling.", "ಅವರಿಗೆ ತಂಪಾಗದಂತೆ ತಡೆಯಲು ಬೆಚ್ಚಗಿನ ಹೊದಿಕೆ ಹೊದಿಸಿ.")
        ),
        listOf("Call for help" to "ಸಹಾಯಕ್ಕೆ ಕರೆ ಮಾಡಿ", "Perform CPR" to "ಸಿಪಿಆರ್ ಮಾಡಿ"),
        listOf("Don't push stomach to remove water" to "ನೀರು ತೆಗೆಯಲು ಹೊಟ್ಟೆ ಒತ್ತಬೇಡಿ", "Don't leave them alone" to "ಒಬ್ಬಂಟಿಯಾಗಿ ಬಿಡಬೇಡಿ")
    ),
    EmergencyItem(
        7, "Electric Shock", "ವಿದ್ಯುತ್ ಆಘಾತ", Color(0xFFFBC02D), R.drawable.electric_shock_illustration,
        listOf(
            FirstAidStep(1, "Do NOT touch the person if they are attached to the power source.", "ವಿದ್ಯುತ್ ಸಂಪರ್ಕದಲ್ಲಿದ್ದರೆ ವ್ಯಕ್ತಿಯನ್ನು ಮುಟ್ಟಬೇಡಿ."),
            FirstAidStep(2, "Turn off the main power switch or unplug the wire immediately.", "ತಕ್ಷಣವೇ ಮೇನ್ ಸ್ವಿಚ್ ಆಫ್ ಮಾಡಿ ಅಥವಾ ಪ್ಲಗ್ ತೆಗೆಯಿರಿ."),
            FirstAidStep(3, "Use a dry wooden stick to push them away from the source.", "ಒಣ ಮರದ ಕೋಲಿನಿಂದ ಅವರನ್ನು ವಿದ್ಯುತ್ ಮೂಲದಿಂದ ದೂರ ಸರಿಸಿ."),
            FirstAidStep(4, "Once safe, check if they are breathing and have a pulse.", "ಸುರಕ್ಷಿತವಾದ ನಂತರ, ಉಸಿರಾಟ och ನಾಡಿ ಪರೀಕ್ಷಿಸಿ."),
            FirstAidStep(5, "Call 108 and start CPR if they are unresponsive.", "ಅವರು ಸ್ಪಂದಿಸದಿದ್ದರೆ 108 ಕರೆ ಮಾಡಿ ಸಿಪಿಆರ್ ಪ್ರಾರಂಭಿಸಿ.")
        ),
        listOf("Turn off power" to "ವಿದ್ಯುತ್ ಸ್ಥಗಿತಗೊಳಿಸಿ", "Use dry objects" to "ಒಣ ವಸ್ತು ಬಳಸಿ"),
        listOf("Don't touch with bare hands" to "ಬರಿಗೈಯಿಂದ ಮುಟ್ಟಬೇಡಿ", "Don't use metal" to "ಕಬ್ಬಿಣ/ಲೋಹ ಬಳಸಬೇಡಿ")
    ),
    EmergencyItem(
        8, "Labor Signs", "ಹೆರಿಗೆಯ ಲಕ್ಷಣಗಳು", Color(0xFFC2185B), R.drawable.labor_signs_illustration,
        listOf(
            FirstAidStep(1, "Stay calm and try to record the timing between pain contractions.", "ಶಾಂತವಾಗಿರಿ och ನೋವಿನ ನಡುವಿನ ಸಮಯವನ್ನು ಗಮನಿಸಿ."),
            FirstAidStep(2, "Make the mother comfortable and encourage deep breathing.", "ತಾಯಿಯನ್ನು ಆರಾಮವಾಗಿರಿಸಿ ಮತ್ತು ದೀರ್ಘವಾಗಿ ಉಸಿರಾಡಲು ತಿಳಿಸಿ."),
            FirstAidStep(3, "Help her lie down on her left side if she feels dizzy.", "ತಲೆತಿರುಗುವಂತಿದ್ದರೆ ಅವಳನ್ನು ಎಡಭಾಗಕ್ಕೆ ಮಲಗಿಸಿ."),
            FirstAidStep(4, "Contact a doctor or call for an ambulance immediately.", "ತಕ್ಷಣ ವೈದ್ಯರನ್ನು ಭೇಟಿ ಮಾಡಿ ಅಥವಾ ಆಂಬ್ಯುಲೆನ್ಸ್ ಕರೆ ಮಾಡಿ."),
            FirstAidStep(5, "Gather clean towels, blankets, and pure water just in case.", "ತುರ್ತು ಪರಿಸ್ಥಿತಿಗಾಗಿ ಶುದ್ಧ ಟವೆಲ್, ಹೊದಿಕೆ ಮತ್ತು ನೀರು ಇಟ್ಟುಕೊಳ್ಳಿ.")
        ),
        listOf("Keep surroundings clean" to "ಪರಿಸರ ಶುಚಿಯಾಗಿಡಿ", "Track time" to "ಸಮಯ ಗಮನಿಸಿ"),
        listOf("Don't delay transport" to "ಆಸ್ಪತ್ರೆಗೆ ಹೋಗಲು ತಡಮಾಡಬೇಡಿ", "Don't panic" to "ಆತಂಕಪಡಬೇಡಿ")
    ),
    EmergencyItem(
        9, "Animal Bite", "ಪ್ರಾಣಿ ಕಡಿತ", Color(0xFFE64A19), R.drawable.animal_bite_illustration,
        listOf(
            FirstAidStep(1, "Move the person safely away from the animal.", "ಪ್ರಾಣಿಯಿಂದ ದೂರಕ್ಕೆ ವ್ಯಕ್ತಿಯನ್ನು ಸುರಕ್ಷಿತವಾಗಿ ಸರಿಸಿ."),
            FirstAidStep(2, "Wash the wound deeply under running water for 15 minutes.", "ಹರಿಯುವ ನೀರಿನಲ್ಲಿ ಗಾಯವನ್ನು ಕನಿಷ್ಠ 15 ನಿಮಿಷ ಶುಚಿಯಾಗಿ ತೊಳೆಯಿರಿ."),
            FirstAidStep(3, "Apply a clean cloth to stop any heavy bleeding.", "ರಕ್ತಸ್ರಾವ ನಿಲ್ಲಿಸಲು ಶುದ್ಧ ಬಟ್ಟೆಯನ್ನು ಇಡಿ."),
            FirstAidStep(4, "Cover the bite with a clean, sterile bandage.", "ಶುದ್ಧವಾದ ಬ್ಯಾಂಡೇಜ್ ಅನ್ನು ಗಾಯಕ್ಕೆ ಕಟ್ಟಿ."),
            FirstAidStep(5, "Visit a doctor within 24 hours for Rabies/Tetanus injections.", "ರೇಬಿಸ್ ಅಥವಾ ಟೆಟಾನಸ್ ಲಸಿಕೆಗಾಗಿ 24 ಗಂಟೆಯೊಳಗೆ ವೈದ್ಯರನ್ನು ಭೇಟಿ ಮಾಡಿ.")
        ),
        listOf("Observe animal if possible" to "ಪ್ರಾಣಿಯನ್ನು ಗಮನಿಸಿ", "Wash thoroughly" to "ಚೆನ್ನಾಗಿ ತೊಳೆಯಿರಿ"),
        listOf("Don't apply irritant chemicals" to "ರಾಸಾಯನಿಕ ಹಚ್ಚಬೇಡಿ", "Don't ignore small bites" to "ಸಣ್ಣ ಕಡಿತವೆಂದು ನಿರ್ಲಕ್ಷಿಸಬೇಡಿ")
    ),
    EmergencyItem(
        10, "Suffocation", "ಉಸಿರುಗಟ್ಟುವಿಕೆ", Color(0xFF5E35B1), R.drawable.suffocation_illustration,
        listOf(
            FirstAidStep(1, "Immediately move the person to an open area with fresh air.", "ತಕ್ಷಣವೇ ವ್ಯಕ್ತಿಯನ್ನು ಶುದ್ಧ ಗಾಳಿಯಿರುವ ತೆರೆದ ಸ್ಥಳಕ್ಕೆ ತನ್ನಿ."),
            FirstAidStep(2, "Loosen tight clothing around their neck and chest.", "ಕುತ್ತಿಗೆ och ಎದೆಯ ಬಿಗಿಯಾದ ಬಟ್ಟೆಯನ್ನು ಸಡಿಲಗೊಳಿಸಿ."),
            FirstAidStep(3, "Check their mouth and throat for any visible blockages.", "ಬಾಯಿ ಮತ್ತು ಗಂಟಲಿನಲ್ಲಿ ಏನಾದರೂ ಸಿಲುಕಿಕೊಂಡಿದೆಯೇ ಎಂಬುದನ್ನು ಪರೀಕ್ಷಿಸಿ."),
            FirstAidStep(4, "If they are unconscious but breathing, place them on their side.", "ಅವರು ಪ್ರಜ್ಞೆ ತಪ್ಪಿದ್ದರೆ, ಉಸಿರಾಡುತ್ತಿದ್ದರೆ ಪಕ್ಕಕ್ಕೆ ಮಲಗಿಸಿ."),
            FirstAidStep(5, "Call emergency services and perform CPR if breathing stops.", "ಉಸಿರಾಟ ನಿಂತರೆ ಆಂಬ್ಯುಲೆನ್ಸ್ ಕರೆ ಮಾಡಿ ಸಿಪಿಆರ್ ಮಾಡಿ.")
        ),
        listOf("Ensure ventilation" to "ಗಾಳಿಯಾಡುವಂತೆ ಮಾಡಿ", "Check airway" to "ಶ್ವಾಸಮಾರ್ಗ ಪರೀಕ್ಷಿಸಿ"),
        listOf("Don't crowd them" to "ಸುತ್ತಲೂ ಗುಂಪು ಕೂಡಬೇಡಿ", "Don't assume they are fine" to "ಚೆನ್ನಾಗಿದ್ದಾರೆಂದು ನಿರ್ಲಕ್ಷಿಸಬೇಡಿ")
    ),
    EmergencyItem(
        11, "Burns", "ಸುಟ್ಟ ಗಾಯಗಳು", Color(0xFFF57C00), R.drawable.burns_illustration,
        listOf(
            FirstAidStep(1, "Move away from the heat source and safely put out flames.", "ಬೆಂಕಿಯಿಂದ ದೂರ ಸರಿಸಿ och ಬೆಂಕಿಯನ್ನು ನಂದಿಸಿ."),
            FirstAidStep(2, "Run cool (not ice cold) water over the burn for 15 minutes.", "ಸುಟ್ಟ ಭಾಗದ ಮೇಲೆ ಕನಿಷ್ಠ 15 ನಿಮಿಷ ತಣ್ಣನೆಯ ನೀರು ಹರಿಯಲು ಬಿಡಿ."),
            FirstAidStep(3, "Remove tight jewelry or belts near the burn before swelling begins.", "ಊತ ಬರುವ ಮುನ್ನ ಸುಟ್ಟ ಭಾಗದ ಹತ್ತಿರವಿರುವ ಆಭರಣಗಳನ್ನು ತೆಗೆಯಿರಿ."),
            FirstAidStep(4, "Cover the burn loosely with a clean, non-fluffy cloth.", "ಗಾಯವನ್ನು ಸ್ವಚ್ಛವಾದ, ನಯವಾದ ಬಟ್ಟೆಯಿಂದ ಸಡಿಲವಾಗಿ ಮುಚ್ಚಿ."),
            FirstAidStep(5, "Do not pop blisters; seek medical help for severe burns.", "ಗುಳ್ಳೆಗಳನ್ನು ಒಡೆಯಬೇಡಿ; ಹೆಚ್ಚಿನ ಸುಟ್ಟ ಗಾಯಗಳಿಗೆ ವೈದ್ಯರನ್ನು ಸಂಪರ್ಕಿಸಿ.")
        ),
        listOf("Use cool water" to "ತಣ್ಣೀರು ಬಳಸಿ", "Cover loosely" to "ಸಡಿಲವಾಗಿ ಮುಚ್ಚಿ"),
        listOf("Don't apply ice/butter/paste" to "ಐಸ್ ಅಥವಾ ಪೇಸ್ಟ್ ಹಚ್ಚಬೇಡಿ", "Don't pull stuck clothes" to "ಅಂಟಿಕೊಂಡ ಬಟ್ಟೆಯನ್ನು ಎಳೆಯಬೇಡಿ")
    ),
    EmergencyItem(
        12, "Fracture", "ಮೂಳೆ ಮುರಿತ", Color(0xFF388E3C), R.drawable.fracture_illustration,
        listOf(
            FirstAidStep(1, "Tell the person not to move the injured area at all.", "ಗಾಯಗೊಂಡ ಭಾಗವನ್ನು ಅಲುಗಾಡಿಸದಂತೆ ವ್ಯಕ್ತಿಗೆ ಸೂಚಿಸಿ."),
            FirstAidStep(2, "Do not attempt to push the bone back into place.", "ಮೂಳೆಯನ್ನು ಸರಿಪಡಿಸಲು ಅಥವಾ ಒತ್ತಲು ಹೋಗಬೇಡಿ."),
            FirstAidStep(3, "Create a simple splint using a straight stick or folded magazine.", "ನೇರವಾದ ಕೋಲು ಬಳಸಿ ಗಾಯಗೊಂಡ ಭಾಗಕ್ಕೆ ಆಧಾರ (ಸ್ಪ್ಲಿಂಟ್) ನೀಡಿ."),
            FirstAidStep(4, "Apply an ice pack wrapped in cloth to reduce swelling.", "ಊತವನ್ನು ಕಡಿಮೆ ಮಾಡಲು ಐಸ್ ಪ್ಯಾಕ್ ಇಡಿ."),
            FirstAidStep(5, "Secure the injured limb gently and visit a hospital.", "ಗಾಯಗೊಂಡ ಭಾಗವನ್ನು ಭದ್ರಪಡಿಸಿ ಆಸ್ಪತ್ರೆಗೆ ಭೇಟಿ ನೀಡಿ.")
        ),
        listOf("Immobilize area" to "ಭಾಗವನ್ನು ಸ್ಥಿರವಾಗಿಡಿ", "Apply cold pack" to "ತಂಪಾದ ಪ್ಯಾಕ್ ಬಳಸಿ"),
        listOf("Don't try to align bone" to "ಮೂಳೆಯನ್ನು ನೀವೇ ಸರಿಪಡಿಸಬೇಡಿ", "Don't massage" to "ಮಸಾಜ್ ಮಾಡಬೇಡಿ")
    ),
    EmergencyItem(
        13, "Seizure/Fits", "ಸೆಳೆತ/ಫಿಟ್ಸ್", Color(0xFF512DA8), R.drawable.seizure_illustration,
        listOf(
            FirstAidStep(1, "Clear the area of hard/sharp objects to prevent injury.", "ಗಾಯವಾಗುವುದನ್ನು ತಡೆಯಲು ಸುತ್ತಲಿನ ಅಪಾಯಕಾರಿ ವಸ್ತುಗಳನ್ನು ಸರಿಸಿ."),
            FirstAidStep(2, "Gently place something soft, like a jacket, under their head.", "ಅವರ ತಲೆಯ ಕೆಳಗೆ ಮೃದುವಾದ ಬಟ್ಟೆಯನ್ನು ಇಡಿ."),
            FirstAidStep(3, "Do not try to hold them down or forcibly stop movements.", "ಅವರನ್ನು ಹಿಡಿದು ಒತ್ತುವ ಅಥವಾ ನಿಲ್ಲಿಸುವ ಪ್ರಯತ್ನ ಮಾಡಬೇಡಿ."),
            FirstAidStep(4, "Never put keys, spoons, or water inside their mouth.", "ಬಾಯಿಯಲ್ಲಿ ಕೀಲಿ ಕೈ, ಚಮಚ, ಅಥವಾ ನೀರು ಹಾಕಬೇಡಿ."),
            FirstAidStep(5, "Once shaking stops, gently roll them onto their side to breathe.", "ಸೆಳೆತ ನಿಂತ ಮೇಲೆ ಅವರನ್ನು ನಿಧಾನವಾಗಿ ಪಕ್ಕಕ್ಕೆ ಮಲಗಿಸಿ.")
        ),
        listOf("Time the seizure" to "ಸಮಯ ಗಮನಿಸಿ", "Stay present" to "ಅವರ ಜೊತೆಯಲ್ಲೇ ಇರಿ"),
        listOf("Don't put things in mouth" to "ಬಾಯಲ್ಲಿ ಏನನ್ನೂ ಇಡಬೇಡಿ", "Don't restrain" to "ಬಲವಂತವಾಗಿ ಹಿಡಿಯಬೇಡಿ")
    ),
    EmergencyItem(
        14, "Fainting", "ಪ್ರಜ್ಞೆ ತಪ್ಪುವುದು", Color(0xFF7B1FA2), R.drawable.fainting_illustration,
        listOf(
            FirstAidStep(1, "Lay the person flat on their back immediately.", "ತಕ್ಷಣ ಅವರನ್ನು ಸಮತಟ್ಟಾಗಿ ಮಲಗಿಸಿ."),
            FirstAidStep(2, "Elevate their legs about 12 inches above their heart level.", "ಅವರ ಕಾಲುಗಳನ್ನು ಹೃದಯಕ್ಕಿಂತ ಸ್ವಲ್ಪ ಮೇಲಕ್ಕೆ ಎತ್ತಿ ಇಡಿ."),
            FirstAidStep(3, "Loosen tight belts, collars, or other restrictive clothing.", "ಬಿಗಿಯಾದ ಬೆಲ್ಟ್, ಕಾಲರ್ ಬಟ್ಟೆಯನ್ನು ಸಡಿಲಗೊಳಿಸಿ."),
            FirstAidStep(4, "Wipe their face gently with a cool, damp cloth.", "ತಂಪಾದ, ಒದ್ದೆ ಬಟ್ಟೆಯಿಂದ ಮುಖವನ್ನು ಒರೆಸಿ."),
            FirstAidStep(5, "If they do not wake up within 1 minute, call emergency services.", "ಒಂದು ನಿಮಿಷದಲ್ಲಿ ಎಚ್ಚರವಾಗದಿದ್ದರೆ, ತಕ್ಷಣ ಆಂಬ್ಯುಲೆನ್ಸ್ ಕರೆ ಮಾಡಿ.")
        ),
        listOf("Ensure fresh air" to "ಗಾಳಿಯಾಡುವಂತೆ ಮಾಡಿ", "Check for pulse" to "ನಾಡಿ ಪರೀಕ್ಷಿಸಿ"),
        listOf("Don't make them sit fast" to "ತಕ್ಷಣ ಕುಳಿತುಕೊಳ್ಳಲು ಬಿಡಬೇಡಿ", "Don't splash excessive water" to "ಅತಿಯಾಗಿ ನೀರು ಸುರಿಯಬೇಡಿ")
    ),
    EmergencyItem(
        15, "Asthma Attack", "ಅಸ್ತಮಾ ದಾಳಿ", Color(0xFF00796B), R.drawable.asthma_attack_illustration,
        listOf(
            FirstAidStep(1, "Help the person sit comfortably upright and keep them calm.", "ಅವರನ್ನು ನೇರವಾಗಿ ಕುಳಿತುಕೊಳ್ಳಲು ಸಹಾಯ ಮಾಡಿ ಮತ್ತು ಸಮಾಧಾನಪಡಿಸಿ."),
            FirstAidStep(2, "Ask for their inhaler and carefully help them use it.", "ಅವರ ಇನ್ಹೇಲರ್ ಕೇಳಿ ಪಡೆಯಿರಿ och ಬಳಸಲು ಸಹಾಯ ಮಾಡಿ."),
            FirstAidStep(3, "Instruct them to take slow, steady and deep breaths.", "ನಿಧಾನವಾಗಿ ಮತ್ತು ಆಳವಾಗಿ ಉಸಿರಾಡಲು ಸೂಚಿಸಿ."),
            FirstAidStep(4, "If there is no improvement after 5 mins, use the inhaler again.", "5 ನಿಮಿಷದ ನಂತರಉಸಿರಾಟ ಸರಿಯಾಗದಿದ್ದರೆ ಮತ್ತೆ ಇನ್ಹೇಲರ್ ಬಳಸಿ."),
            FirstAidStep(5, "If lips turn blue or breathing worsens, call 108 immediately.", "ತುಟಿಗಳು ನೀಲಿಯಾದರೆ ಅಥವಾ ಉಸಿರಾಟ ಕಷ್ಟವಾದರೆ ತಕ್ಷಣ 108 ಕರೆ ಮಾಡಿ.")
        ),
        listOf("Sit them upright" to "ನೇರವಾಗಿ ಕುಳ್ಳಿರಿಸಿ", "Stay calm" to "ಶಾಂತವಾಗಿರಿ"),
        listOf("Don't let them lie down" to "ಅವರನ್ನು ಮಲಗಿಸಬೇಡಿ", "Don't ignore distress" to "ಕಷ್ಟಪಡುತ್ತಿರುವುದನ್ನು ನಿರ್ಲಕ್ಷಿಸಬೇಡಿ")
    ),
    EmergencyItem(
        16, "Allergic Reaction", "ಅಲರ್ಜಿ ಪ್ರತಿಕ್ರಿಯೆ", Color(0xFFD81B60), R.drawable.allergic_reaction_illustration,
        listOf(
            FirstAidStep(1, "Identify and remove the allergy source (like a bee stinger).", "ಅಲರ್ಜಿ ಕಾರಣವಾದ ವಸ್ತುವನ್ನು ಗುರುತಿಸಿ (ಉದಾ: ಜೇನುನೊಣದ ಮುಳ್ಳು) ತೆಗೆಯಿರಿ."),
            FirstAidStep(2, "Ask if they have an EpiPen and help them inject it into the thigh.", "EpiPen ಇದ್ದರೆ ಅದನ್ನು ಅವರ ತೊಡೆಗೆ ಚುಚ್ಚಲು ಸಹಾಯ ಮಾಡಿ."),
            FirstAidStep(3, "Have them sit perfectly still and keep calm to breathe easier.", "ಅವರು ಆರಾಮವಾಗಿ ಉಸಿರಾಡಲು ಶಾಂತವಾಗಿ ಕುಳಿತುಕೊಳ್ಳಲು ತಿಳಿಸಿ."),
            FirstAidStep(4, "If they feel dizzy/shocked, lay them flat and elevate legs.", "ತಲೆತಿರುಗುವಂತಿದ್ದರೆ ಅವರನ್ನು ಮಲಗಿಸಿ ಕಾಲುಗಳನ್ನು ಮೇಲೆತ್ತಿ."),
            FirstAidStep(5, "Seek immediate emergency care, even if symptoms improve.", "ಲಕ್ಷಣಗಳು ಕಡಿಮೆಯಾದರೂ ತಕ್ಷಣ ವೈದ್ಯರನ್ನು ಭೇಟಿ ಮಾಡಿ.")
        ),
        listOf("Use autoimmune pen" to "ಪೆನ್ ಔಷಧಿ ಬಳಸಿ", "Call for help" to "ಸಹಾಯಕ್ಕೆ ಕರೆ ಮಾಡಿ"),
        listOf("Don't ignore swelling throat" to "ಗಂಟಲು ಊತವನ್ನು ನಿರ್ಲಕ್ಷಿಸಬೇಡಿ", "Don't give food" to "ಆಹಾರ ನೀಡಬೇಡಿ")
    ),
    EmergencyItem(
        17, "Eye Injury", "ಕಣ್ಣಿನ ಗಾಯ", Color(0xFF0097A7), R.drawable.eye_injury_illustration,
        listOf(
            FirstAidStep(1, "Wash your hands thoroughly before touching near the eye.", "ಕಣ್ಣನ್ನು ಮುಟ್ಟುವ ಮೊದಲು ನಿಮ್ಮ ಕೈಗಳನ್ನು ಚೆನ್ನಾಗಿ ತೊಳೆಯಿರಿ."),
            FirstAidStep(2, "Do not rub the eye or apply direct pressure to the eyeball.", "ಕಣ್ಣನ್ನು ಉಜ್ಜಬೇಡಿ ಮತ್ತು ಕಣ್ಣುಗುಡ್ಡೆಯ ಮೇಲೆ ಒತ್ತಡ ಹಾಕಬೇಡಿ."),
            FirstAidStep(3, "For chemicals, flush eye continuously with clean water for 15 mins.", "ರಾಸಾಯನಿಕ ಬಿದ್ದಿದ್ದರೆ 15 ನಿಮಿಷ ಶುದ್ಧ ನೀರಿನಿಂದ ಕಣ್ಣನ್ನು ತೊಳೆಯಿರಿ."),
            FirstAidStep(4, "Cover the injured eye safely with a paper cup or shield.", "ಗಾಯಗೊಂಡ ಕಣ್ಣನ್ನು ರಕ್ಷಿಸಲು ಪೇಪರ್ ಕಪ್ ನಿಂದ ಮುಚ್ಚಿ."),
            FirstAidStep(5, "Visit an eye specialist immediately without removing stuck objects.", "ಚುಚ್ಚಿದ ವಸ್ತುವನ್ನು ತೆಗೆಯದೆ ತಕ್ಷಣ ಕಣ್ಣಿನ ವೈದ್ಯರನ್ನು ಭೇಟಿ ಮಾಡಿ.")
        ),
        listOf("Keep both eyes still" to "ಎರಡೂ ಕಣ್ಣುಗಳನ್ನು ಸ್ಥಿರವಾಗಿಡಿ", "Flush only for chemicals" to "ಕೆಮಿಕಲ್ ಇದ್ದರೆ ಮಾತ್ರ ತೊಳೆಯಿರಿ"),
        listOf("Don't rub eyes" to "ಕಣ್ಣನ್ನು ಉಜ್ಜಬೇಡಿ", "Don't use tweezers" to "ಚಿಮುಟ ಬಳಸಬೇಡಿ")
    ),
    EmergencyItem(
        18, "Broken Tooth", "ಹಲ್ಲು ಮುರಿಯುವುದು", Color(0xFF455A64), R.drawable.broken_tooth_illustration,
        listOf(
            FirstAidStep(1, "Find the broken tooth and pick it up only by the white top part.", "ಮುರಿದ ಹಲ್ಲನ್ನು ಹುಡುಕಿ ಮತ್ತು ಮೇಲ್ಭಾಗದಿಂದ (ಬಿಳಿ ಭಾಗ) ಮಾತ್ರ ಹಿಡಿಯಿರಿ."),
            FirstAidStep(2, "Do not touch the root or scrub off any tissue attached.", "ಹಲ್ಲಿನ ಬೇರನ್ನು ಮುಟ್ಟಬೇಡಿ ಅಥವಾ ಅದಕ್ಕಿರುವ ಮಾಂಸವನ್ನು ಉಜ್ಜಬೇಡಿ."),
            FirstAidStep(3, "Gently rinse the tooth in cold water for just a few seconds.", "ತಣ್ಣೀರಿನಲ್ಲಿ ಕೇವಲ ಕೆಲವು ಸೆಕೆಂಡುಗಳ ಕಾಲ ನಿಧಾನವಾಗಿ ತೊಳೆಯಿರಿ."),
            FirstAidStep(4, "Place the tooth in a small cup of milk or the person's saliva.", "ಹಲ್ಲನ್ನು ಹಾಲಿನಲ್ಲಿ ಅಥವಾ ಗಾಯಗೊಂಡವರ ಲಾಲಾರಸದಲ್ಲಿ ಇಡಿ."),
            FirstAidStep(5, "Reach a dentist immediately (within 30 mins) to save the tooth.", "ಹಲ್ಲನ್ನು ಉಳಿಸಲು 30 ನಿಮಿಷದೊಳಗೆ ತಕ್ಷಣ ದಂತವೈದ್ಯರನ್ನು ತಲುಪಿ.")
        ),
        listOf("Hold by crown" to "ಮೇಲ್ಭಾಗದಿಂದ ಹಿಡಿಯಿರಿ", "Stop bleeding with gauze" to "ರಕ್ತ ನಿಲ್ಲಿಸಲು ಹತ್ತಿ ಇಡಿ"),
        listOf("Don't wrap in tissue" to "ಟಿಶ್ಯೂ ಪೇಪರ್ ನಲ್ಲಿ ಸುತ್ತಬೇಡಿ", "Don't use soap" to "ಸೋಪು ಬಳಸಬೇಡಿ")
    ),
    EmergencyItem(
        19, "Food Poisoning", "ವಿಷಪ್ರಾಶನ (ಆಹಾರ)", Color(0xFF689F38), R.drawable.food_poisoning_illustration,
        listOf(
            FirstAidStep(1, "Tell the person to rest and avoid eating solid food for a few hours.", "ಕೆಲವು ಗಂಟೆಗಳ ಕಾಲ ಘನ ಆಹಾರ ಸೇವಿಸದೆ ವಿಶ್ರಾಂತಿ ಪಡೆಯಲು ತಿಳಿಸಿ."),
            FirstAidStep(2, "Drink plenty of safe water or ORS liquid to prevent dehydration.", "ನಿರ್ಜಲೀಕರಣ ತಡೆಯಲು ಹೆಚ್ಚು ನೀರು ಅಥವಾ ಒಆರ್ಎಸ್ ಕುಡಿಯಿರಿ."),
            FirstAidStep(3, "Take small, frequent sips rather than gulping large amounts.", "ಒಮ್ಮೆಲೇ ಹೆಚ್ಚು ಕುಡಿಯುವ ಬದಲು ಆಗಾಗ ಸ್ವಲ್ಪ ಸ್ವಲ್ಪವೇ ಕುಡಿಯಿರಿ."),
            FirstAidStep(4, "Do not take anti-diarrhea medications without a doctor's advice.", "ವೈದ್ಯರ ಸಲಹೆಯಿಲ್ಲದೆ ಭೇದಿ ನಿಲ್ಲಿಸುವ ಔಷಧಿಗಳನ್ನು ತೆಗೆದುಕೊಳ್ಳಬೇಡಿ."),
            FirstAidStep(5, "If vomiting persists for over 12 hours or shows blood, seek medical help.", "ವಾಂತಿ 12 ಗಂಟೆಗಳ ಕಾಲ ನಿಲ್ಲದಿದ್ದರೆ ಅಥವಾ ರಕ್ತ ಬಂದರೆ ವೈದ್ಯರನ್ನು ಭೇಟಿ ಮಾಡಿ.")
        ),
        listOf("Rest stomach" to "ಹೊಟ್ಟೆಗೆ ವಿಶ್ರಾಂತಿ ನೀಡಿ", "Stay hydrated" to "ದ್ರವಾಹಾರ ಸೇವಿಸಿ"),
        listOf("Don't drink milk/caffeine" to "ಹಾಲು/ಕಾಫಿ ಕುಡಿಯಬೇಡಿ", "Don't self-medicate" to "ಸ್ವಯಂ ಔಷಧಿ ಮಾಡಬೇಡಿ")
    ),
    EmergencyItem(
        20, "Nose Bleed", "ಮೂಗಿನ ರಕ್ತಸ್ರಾವ", Color(0xFFAD1457), R.drawable.nose_bleed_illustration,
        listOf(
            FirstAidStep(1, "Have the person sit upright and lean slightly forward.", "ವ್ಯಕ್ತಿಯನ್ನು ಕುಳ್ಳಿರಿಸಿ ಮತ್ತು ಸ್ವಲ್ಪ ಮುಂದಕ್ಕೆ ಬಾಗುವಂತೆ ಮಾಡಿ."),
            FirstAidStep(2, "Ask them to breathe exclusively through their mouth.", "ಕೇವಲ ಬಾಯಿಯ ಮೂಲಕ ಮಾತ್ರ ಉಸಿರಾಡಲು ಹೇಳಿ."),
            FirstAidStep(3, "Firmly pinch the soft part of the nose just below the bone.", "ಮೂಗಿನ ಮೂಳೆಯ ಕೆಳಗಿನ ಮೃದುವಾದ ಭಾಗವನ್ನು ಗಟ್ಟಿಯಾಗಿ ಒತ್ತಿ ಹಿಡಿಯಿರಿ."),
            FirstAidStep(4, "Keep the nose pinched continuously for 10 to 15 minutes.", "ಸತತವಾಗಿ 10 ರಿಂದ 15 ನಿಮಿಷಗಳ ಕಾಲ ಮೂಗನ್ನು ಬಿಡದೆ ಒತ್ತಿ ಹಿಡಿಯಿರಿ."),
            FirstAidStep(5, "Apply a cold pack to the bridge of the nose to shrink blood vessels.", "ರಕ್ತನಾಳಗಳನ್ನು ಕುಗ್ಗಿಸಲು ಮೂಗಿನ ಮೇಲೆ ಐಸ್ ಪ್ಯಾಕ್ ಇಡಿ.")
        ),
        listOf("Lean forward" to "ಮುಂದಕ್ಕೆ ಬಗ್ಗಿ", "Pinch soft part" to "ಮೆದು ಭಾಗವನ್ನು ಒತ್ತಿ"),
        listOf("Don't tilt head backward" to "ತಲೆಯನ್ನು ಹಿಂದಕ್ಕೆ ಬಾಗಿಸಬೇಡಿ", "Don't blow nose" to "ಮೂಗನ್ನು ಉದಬೇಡಿ")
    ),
    EmergencyItem(
        21, "Diabetic Shock", "ಮಧುಮೇಹ ಆಘಾತ", Color(0xFF2E7D32), R.drawable.diabetic_shock_illustration,
        listOf(
            FirstAidStep(1, "Ask the conscious person if they have diabetes or are feeling weak.", "ಮಧುಮೇಹವಿದೆಯೇ ಅಥವಾ ಸುಸ್ತಾಗುತ್ತಿದೆಯೇ ಎಂದು ಎಚ್ಚರವಿರುವ ವ್ಯಕ್ತಿಗೆ ಕೇಳಿ."),
            FirstAidStep(2, "If awake and able to swallow, give a sugary drink or hard candy.", "ಎಚ್ಚರವಿದ್ದರೆ ಮತ್ತು ನುಂಗಲು ಸಾಧ್ಯವಿದ್ದರೆ ಸಿಹಿ ಪಾನೀಯ ಅಥವಾ ಚಾಕೊಲೇಟ್ ನೀಡಿ."),
            FirstAidStep(3, "Wait exactly 15 minutes to see if their condition improves.", "ಅವರ ಸ್ಥಿತಿ ಸುಧಾರಿಸುತ್ತದೆಯೇ ಎಂದು ನೋಡಲು 15 ನಿಮಿಷ ಕಾಯಿರಿ."),
            FirstAidStep(4, "If they do not feel better after 15 mins, give them more sugar.", "ಸುಧಾರಿಸದಿದ್ದರೆ, ಅವರಿಗೆ ಇನ್ನಷ್ಟು ಸಿಹಿ ನೀಡಿ."),
            FirstAidStep(5, "If they are unconscious, do not feed them; call an ambulance immediately.", "ಅವರು ಪ್ರಜ್ಞೆ ತಪ್ಪಿದ್ದರೆ ಏನನ್ನೂ ತಿನ್ನಿಸಬೇಡಿ; ತಕ್ಷಣ ಆಂಬ್ಯುಲೆನ್ಸ್ ಕರೆ ಮಾಡಿ.")
        ),
        listOf("Give fast sugar" to "ವೇಗವಾಗಿ ಕರಗುವ ಸಿಹಿ ನೀಡಿ", "Check again in 15m" to "15 ನಿಮಿಷದ ನಂತರ ಪರೀಕ್ಷಿಸಿ"),
        listOf("Don't give diet drinks" to "ಶುಗರ್-ಫ್ರೀ ಡ್ರಿಂಕ್s ನೀಡಬೇಡಿ", "Don't force feed if sleepy" to "ನಿದ್ದೆ ಮಂಪರಿನಲ್ಲಿದ್ದರೆ ತಿನ್ನಿಸಬೇಡಿ")
    )
)
