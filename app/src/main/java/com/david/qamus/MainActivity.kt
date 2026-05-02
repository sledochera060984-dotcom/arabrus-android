package com.david.qamus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import com.david.qamus.ui.theme.ArabrusBackground
import com.david.qamus.ui.theme.ArabrusGreen
import com.david.qamus.ui.theme.ArabrusLine
import com.david.qamus.ui.theme.ArabrusMuted
import com.david.qamus.ui.theme.ArabrusText
import com.david.qamus.ui.theme.ArabrusWarning
import com.david.qamus.ui.theme.QamusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { QamusTheme { ArabrusApp() } }
    }
}

data class DictionaryEntry(
    val id: Int,
    val arabic: String,
    val russian: String,
    val hint: String,
)

enum class FavoriteMark { None, Blue, Red }

private val demoWords = listOf(
    DictionaryEntry(1, "كَتَبَ", "писать; написал", "kataba"),
    DictionaryEntry(2, "قَالَ", "сказать; сказал", "qāla"),
    DictionaryEntry(3, "بَيْت", "дом", "bayt"),
    DictionaryEntry(4, "عِلْم", "знание; наука", "ʿilm"),
    DictionaryEntry(5, "سَلَام", "мир; приветствие", "salām"),
)

@Composable
fun ArabrusApp() {
    var query by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("Словарь") }
    var activeChip by remember { mutableStateOf("Все") }
    val favoriteMarks = remember { mutableStateMapOf<Int, FavoriteMark>() }

    val filteredWords = demoWords.filter { entry ->
        val matchesQuery = query.isBlank() ||
            entry.arabic.contains(query.trim(), true) ||
            entry.russian.contains(query.trim(), true) ||
            entry.hint.contains(query.trim(), true)
        val matchesChip = when (activeChip) {
            "Избранное" -> favoriteMarks[entry.id] != null && favoriteMarks[entry.id] != FavoriteMark.None
            "Глаголы" -> entry.russian.contains(";")
            else -> true
        }
        matchesQuery && matchesChip
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArabrusBackground)
            .padding(12.dp),
    ) {
        AppShell {
            Header()
            StatusOk("Нативная версия словаря • офлайн-режим")
            AuthRow()
            WebTabs(
                tabs = listOf("Словарь", "Избранное", "Карточки", "Заметки"),
                activeTab = activeTab,
                onSelect = { activeTab = it },
            )
            Spacer(Modifier.height(10.dp))
            WebSearchField(value = query, onValueChange = { query = it })
            Spacer(Modifier.height(10.dp))
            WebChips(
                chips = listOf("Все", "Глаголы", "Избранное", "Карточки"),
                active = activeChip,
                onSelect = { activeChip = it },
            )
            Spacer(Modifier.height(10.dp))
            DictionaryList(
                words = filteredWords,
                marks = favoriteMarks,
                onMarkChange = { id, mark -> favoriteMarks[id] = mark },
            )
            Spacer(Modifier.height(12.dp))
            ActionButtons()
            Spacer(Modifier.height(12.dp))
            LearningCards()
            Spacer(Modifier.height(8.dp))
            NotesTopbar()
        }
    }
}

@Composable
private fun AppShell(content: @Composable () -> Unit) = Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    color = Color.White,
) { Column(Modifier.padding(16.dp), content = { content() }) }

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(42.dp)
                .background(ArabrusGreen, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("ع", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.width(10.dp))
        Text("АРАБРУС", color = ArabrusGreen, fontSize = 24.sp, fontWeight = FontWeight.W800)
    }
}

@Composable
private fun StatusOk(text: String) {
    Spacer(Modifier.height(12.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFECFDF5), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(text, color = ArabrusGreen, fontSize = 13.sp, fontWeight = FontWeight.W800)
    }
}

@Composable
private fun AuthRow() {
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AuthButton("Войти")
        AuthButton("Регистрация")
    }
}

@Composable
private fun AuthButton(text: String) {
    Box(
        Modifier
            .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) { Text(text = text, color = ArabrusText, fontSize = 13.sp, fontWeight = FontWeight.W700) }
}

@Composable
private fun WebTabs(tabs: List<String>, activeTab: String, onSelect: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5F9), RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tabs.forEach { tab -> WebTab(tab, tab == activeTab) { onSelect(tab) } }
        }
    }
}

@Composable
private fun WebTab(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .background(if (active) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.W700, color = if (active) ArabrusGreen else ArabrusMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun WebSearchField(value: String, onValueChange: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .border(2.dp, Color(0xFFF1F5F9), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 16.sp, color = ArabrusText),
            singleLine = true,
            decorationBox = { inner -> if (value.isEmpty()) Text("Поиск по арабскому, русскому или транскрипции", color = ArabrusMuted, fontSize = 16.sp); inner() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WebChips(chips: List<String>, active: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { text ->
            val isActive = text == active
            Box(
                Modifier
                    .background(if (isActive) ArabrusGreen else Color(0xFFF1F5F9), RoundedCornerShape(999.dp))
                    .clickable { onSelect(text) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(text, fontSize = 13.sp, fontWeight = FontWeight.W700, color = if (isActive) Color.White else ArabrusMuted)
            }
        }
    }
}

@Composable
private fun DictionaryList(words: List<DictionaryEntry>, marks: Map<Int, FavoriteMark>, onMarkChange: (Int, FavoriteMark) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        words.forEach { word -> DictionaryItem(word, marks[word.id] ?: FavoriteMark.None, onMarkChange) }
    }
}

@Composable
private fun DictionaryItem(entry: DictionaryEntry, mark: FavoriteMark, onMarkChange: (Int, FavoriteMark) -> Unit) {
    val bg = when (mark) {
        FavoriteMark.Blue -> Color(0xFFEFF6FF)
        FavoriteMark.Red -> Color(0xFFFEF2F2)
        FavoriteMark.None -> Color.White
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFEEF2F7), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                RussianText(entry.russian)
                Spacer(Modifier.height(4.dp))
                HintText(entry.hint)
            }
            ArabicText(entry.arabic)
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FavoriteButton("Синий", mark == FavoriteMark.Blue) { onMarkChange(entry.id, FavoriteMark.Blue) }
            FavoriteButton("Красный", mark == FavoriteMark.Red) { onMarkChange(entry.id, FavoriteMark.Red) }
            FavoriteButton("Сброс", mark == FavoriteMark.None) { onMarkChange(entry.id, FavoriteMark.None) }
        }
    }
}

@Composable private fun RussianText(text: String) = Text(text, fontSize = 15.sp, fontWeight = FontWeight.W700, color = Color(0xFF334155))

@Composable
private fun ArabicText(text: String) = Text(
    text = text,
    fontSize = 28.sp,
    fontWeight = FontWeight.W800,
    color = ArabrusGreen,
    textAlign = TextAlign.Right,
    modifier = Modifier.width(120.dp),
    style = TextStyle(textDirection = TextDirection.Rtl),
)

@Composable private fun HintText(text: String) = Text(text, fontSize = 12.sp, color = ArabrusMuted)

@Composable
private fun FavoriteButton(text: String, active: Boolean, onClick: () -> Unit) {
    val bg = when {
        text == "Синий" && active -> Color(0xFF2563EB)
        text == "Красный" && active -> Color(0xFFDC2626)
        active -> ArabrusWarning
        else -> Color(0xFFF1F5F9)
    }
    Box(Modifier.background(bg, RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(text, color = if (active) Color.White else ArabrusText, fontSize = 13.sp, fontWeight = FontWeight.W700)
    }
}

@Composable private fun ActionButtons() { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AuthButton("Экспорт"); AuthButton("Импорт") } }
@Composable private fun LearningCards() { Text("Карточки обучения", color = ArabrusMuted, fontSize = 13.sp) }
@Composable private fun NotesTopbar() { Text("Заметки", color = ArabrusMuted, fontSize = 13.sp) }

@Preview(showBackground = true)
@Composable
fun ArabrusPreview() {
    QamusTheme { ArabrusApp() }
}
