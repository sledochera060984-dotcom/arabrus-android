package com.david.qamus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.qamus.ui.theme.ArabrusBackground
import com.david.qamus.ui.theme.ArabrusBlueMark
import com.david.qamus.ui.theme.ArabrusGreen
import com.david.qamus.ui.theme.ArabrusLine
import com.david.qamus.ui.theme.ArabrusMuted
import com.david.qamus.ui.theme.ArabrusRedMark
import com.david.qamus.ui.theme.ArabrusSurface
import com.david.qamus.ui.theme.ArabrusText
import com.david.qamus.ui.theme.QamusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QamusTheme {
                ArabrusApp()
            }
        }
    }
}

data class DictionaryEntry(
    val id: Int,
    val arabic: String,
    val russian: String,
    val hint: String,
    val verbForm: String? = null,
)

enum class Screen(val title: String) {
    Dictionary("Словарь"),
    Favorites("Избранное"),
    Cards("Карточки"),
    Notes("Заметки"),
    Settings("Настройки"),
}

enum class FavoriteMark { None, Blue, Red }

private val demoWords = listOf(
    DictionaryEntry(1, "كَتَبَ", "писать; написал", "kataba", "يَكْتُبُ"),
    DictionaryEntry(2, "قَالَ", "сказать; сказал", "qāla", "يَقُولُ"),
    DictionaryEntry(3, "بَيْت", "дом", "bayt"),
    DictionaryEntry(4, "عِلْم", "знание; наука", "ʿilm"),
    DictionaryEntry(5, "سَلَام", "мир; приветствие", "salām"),
    DictionaryEntry(6, "طَلَبَ", "просить; требовать", "ṭalaba", "يَطْلُبُ"),
)

@Composable
fun ArabrusApp() {
    var currentScreen by remember { mutableStateOf(Screen.Dictionary) }
    var query by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val favoriteMarks = remember { mutableStateMapOf<Int, FavoriteMark>() }
    val flippedCards = remember { mutableStateMapOf<Int, Boolean>() }

    val filteredWords = if (query.trim().isBlank()) {
        demoWords
    } else {
        demoWords.filter { entry ->
            entry.arabic.contains(query.trim(), ignoreCase = true) ||
                entry.russian.contains(query.trim(), ignoreCase = true) ||
                entry.hint.contains(query.trim(), ignoreCase = true)
        }
    }

    val favoriteWords = demoWords.filter { favoriteMarks[it.id] != null && favoriteMarks[it.id] != FavoriteMark.None }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ArabrusBackground,
        bottomBar = {
            BottomTabs(
                currentScreen = currentScreen,
                onSelect = { currentScreen = it },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ArabrusBackground)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppShell {
                Header()
                StatusCard()

                when (currentScreen) {
                    Screen.Dictionary -> DictionaryScreen(
                        query = query,
                        onQueryChange = { query = it },
                        words = filteredWords,
                        favoriteMarks = favoriteMarks,
                        onToggleFavorite = { entry ->
                            val current = favoriteMarks[entry.id]
                            favoriteMarks[entry.id] = if (current == null || current == FavoriteMark.None) FavoriteMark.Blue else FavoriteMark.None
                        },
                        onSetMark = { entry, mark -> favoriteMarks[entry.id] = mark },
                    )

                    Screen.Favorites -> FavoritesScreen(
                        words = favoriteWords,
                        favoriteMarks = favoriteMarks,
                        onSetMark = { entry, mark -> favoriteMarks[entry.id] = mark },
                    )

                    Screen.Cards -> CardsScreen(
                        words = if (favoriteWords.isEmpty()) demoWords else favoriteWords,
                        flippedCards = flippedCards,
                    )

                    Screen.Notes -> NotesScreen(
                        note = note,
                        onNoteChange = { note = it },
                    )

                    Screen.Settings -> SettingsScreen()
                }
            }
        }
    }
}

@Composable
private fun AppShell(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ArabrusSurface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(ArabrusGreen, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("ع", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "АРАБРУС",
            color = ArabrusGreen,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun StatusCard() {
    Spacer(Modifier.height(14.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFECFDF5), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(14.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Нативная версия словаря • офлайн-режим",
            color = ArabrusGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun DictionaryScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    words: List<DictionaryEntry>,
    favoriteMarks: Map<Int, FavoriteMark>,
    onToggleFavorite: (DictionaryEntry) -> Unit,
    onSetMark: (DictionaryEntry, FavoriteMark) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Поиск по арабскому, русскому или транскрипции") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
    )

    Spacer(Modifier.height(10.dp))
    ChipsRow(listOf("Все", "Глаголы", "Избранное", "Карточки"))
    Spacer(Modifier.height(8.dp))

    if (words.isEmpty()) {
        EmptyState("Ничего не найдено")
    } else {
        words.forEach { entry ->
            DictionaryCard(
                entry = entry,
                mark = favoriteMarks[entry.id] ?: FavoriteMark.None,
                onToggleFavorite = { onToggleFavorite(entry) },
                onSetBlue = { onSetMark(entry, FavoriteMark.Blue) },
                onSetRed = { onSetMark(entry, FavoriteMark.Red) },
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun FavoritesScreen(
    words: List<DictionaryEntry>,
    favoriteMarks: Map<Int, FavoriteMark>,
    onSetMark: (DictionaryEntry, FavoriteMark) -> Unit,
) {
    Text(
        text = "Избранное",
        color = ArabrusText,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
    )
    Spacer(Modifier.height(8.dp))
    ChipsRow(listOf("Все", "Синие", "Красные"))
    Spacer(Modifier.height(10.dp))

    if (words.isEmpty()) {
        EmptyState("Пока нет избранных слов. Добавь слово из вкладки Словарь.")
    } else {
        words.forEach { entry ->
            DictionaryCard(
                entry = entry,
                mark = favoriteMarks[entry.id] ?: FavoriteMark.None,
                onToggleFavorite = { onSetMark(entry, FavoriteMark.None) },
                onSetBlue = { onSetMark(entry, FavoriteMark.Blue) },
                onSetRed = { onSetMark(entry, FavoriteMark.Red) },
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CardsScreen(
    words: List<DictionaryEntry>,
    flippedCards: MutableMap<Int, Boolean>,
) {
    Text(
        text = "Карточки для заучивания",
        color = ArabrusText,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Нажми на карточку, чтобы перевернуть арабский и русский текст.",
        color = ArabrusMuted,
        fontSize = 13.sp,
    )
    Spacer(Modifier.height(12.dp))

    words.forEach { entry ->
        val isFlipped = flippedCards[entry.id] == true
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clickable { flippedCards[entry.id] = !isFlipped },
            colors = CardDefaults.cardColors(containerColor = if (isFlipped) Color(0xFFF8FAFC) else Color.White),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isFlipped) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(entry.russian, color = ArabrusText, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                        Text(entry.hint, color = ArabrusMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    Text(
                        text = entry.arabic,
                        color = ArabrusGreen,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun NotesScreen(
    note: String,
    onNoteChange: (String) -> Unit,
) {
    Text(
        text = "Заметки",
        color = ArabrusText,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        value = note,
        onValueChange = onNoteChange,
        placeholder = { Text("Напиши свою заметку по арабскому слову") },
        shape = RoundedCornerShape(14.dp),
    )
    Spacer(Modifier.height(10.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { },
        colors = ButtonDefaults.buttonColors(containerColor = ArabrusGreen),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text("Сохранить заметку")
    }
}

@Composable
private fun SettingsScreen() {
    Text(
        text = "Настройки",
        color = ArabrusText,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
    )
    Spacer(Modifier.height(10.dp))
    SettingRow("Тема", "Светлая тема как в GitHub-версии")
    SettingRow("База слов", "Сейчас тестовые данные, дальше подключим Room")
    SettingRow("Озвучка", "Дальше добавим нативный TextToSpeech для арабского")
    SettingRow("Синхронизация", "Позже сделаем резервную копию избранного")
}

@Composable
private fun DictionaryCard(
    entry: DictionaryEntry,
    mark: FavoriteMark,
    onToggleFavorite: () -> Unit,
    onSetBlue: () -> Unit,
    onSetRed: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val background = when (mark) {
        FavoriteMark.Blue -> ArabrusBlueMark
        FavoriteMark.Red -> ArabrusRedMark
        FavoriteMark.None -> Color.White
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.russian, color = ArabrusText, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(entry.hint, color = ArabrusMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = entry.arabic,
                        color = ArabrusGreen,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Right,
                    )
                    entry.verbForm?.let {
                        Text(
                            text = it,
                            color = ArabrusGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ArabrusLine)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Полный перевод: ${entry.russian}. Транскрипция: ${entry.hint}.",
                    color = ArabrusText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = onToggleFavorite, shape = RoundedCornerShape(12.dp)) {
                        Text(if (mark == FavoriteMark.None) "В избранное" else "Убрать")
                    }
                    Button(modifier = Modifier.weight(1f), onClick = onSetBlue, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)), shape = RoundedCornerShape(12.dp)) {
                        Text("Синий")
                    }
                    Button(modifier = Modifier.weight(1f), onClick = onSetRed, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)), shape = RoundedCornerShape(12.dp)) {
                        Text("Красный")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipsRow(chips: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEachIndexed { index, text ->
            Box(
                modifier = Modifier
                    .background(if (index == 0) ArabrusGreen else Color(0xFFE2E8F0), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    color = if (index == 0) Color.White else Color(0xFF475569),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun BottomTabs(
    currentScreen: Screen,
    onSelect: (Screen) -> Unit,
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Screen.values().forEach { screen ->
                val selected = currentScreen == screen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) ArabrusGreen else Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .clickable { onSelect(screen) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = screen.title,
                        color = if (selected) Color.White else ArabrusMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = ArabrusText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = ArabrusMuted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = ArabrusMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true)
@Composable
fun ArabrusPreview() {
    QamusTheme {
        ArabrusApp()
    }
}
