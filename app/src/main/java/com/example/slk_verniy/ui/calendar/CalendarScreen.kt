package com.example.slk_verniy.ui.calendar

import android.Manifest
import android.os.Build
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.slk_verniy.data.MonthlySalaryResult
import com.example.slk_verniy.ui.components.AnimatedCatView
import com.example.slk_verniy.worker.PaydayWorker
import com.example.slk_verniy.worker.ShiftReminderWorker
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val catPhrases = listOf(
    "Мяу! Чего тебе? Надеюсь, ты с едой, а не с тупыми вопросами.",
    "Я тут вообще-то спал. У тебя есть веская причина меня будить?",
    "Лапки не для работы, лапки — чтобы ими красоваться! 🐾",
    "Мур-р-р... Ладно, так и быть, почеши меня за ушком, пока я добрый.",
    "Человек, ты сегодня подозрительно долго залипаешь в экран. Иди насыпь корма!",
    "Моя миска наполовину пуста! Это открытое неуважение к хозяюшке... то есть к хозяину!",
    "Опять работаешь? Лучше бы коробку мне новую нашел, я в нее залезть хочу.",
    "Мяу. Система работает стабильно, шерсть летит, всё под контролем.",
    "Ты кто такой? Я тебя не звал, иди отсюда, кожаный. ...Шучу-шучу, дай пять!",
    "Ладно, давай пообщаемся, но только чур без глупых шуток про тапки.",
    "Мой внутренний голос подсказывает, что пора сделать перерыв и поспать часиков этак пятнадцать.",
    "Хватит тыкать в меня пальцем! Нажимай аккуратнее, у меня стресс.",
    "Мр-р-мяу! А ты неплох для двуногого. Уважение заслужил.",
    "Если долго смотреть на телефон, в нем появится рыба. Проверь, я проверил — пока нет.",
    "Я сегодня весь день продуктивно лежал на диване. А чего добился ты?",
    "Осторожнее с кнопками, а то я сейчас лапой случайно режим самоуничтожения активирую!",
    "Слушай, а давай забросим все дела и пойдем поедим влажного корма?",
    "Мой внутренний голос подсказывает, что ты опять забыл про обновление статуса. Исправляйся!",
    "Мяу--у... Жизнь боль, когда миска пуста, а человек занят ерундой.",
    "Я кот простой: вижу кнопку — нажимаю, вижу еду — ем. Бери пример!"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory),
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            }
        }
    }

    val selectedDate by viewModel.selectedDate.collectAsState()
    val shiftStartDate by viewModel.shiftStartDate.collectAsState()
    val extraShifts by viewModel.extraShifts.collectAsState()
    val salarySettings by viewModel.salarySettings.collectAsState()
    val avatarUri by viewModel.avatarUri.collectAsState()
    val profileName by viewModel.name.collectAsState()
    val context = LocalContext.current
    
    val currentMonth = remember { YearMonth.now() }
    val centerIndex = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = centerIndex) { Int.MAX_VALUE }

    val visibleMonth = remember(pagerState.currentPage) {
        currentMonth.plusMonths((pagerState.currentPage - centerIndex).toLong())
    }

    val monthlySalary = remember(visibleMonth, shiftStartDate, extraShifts, salarySettings) {
        viewModel.calculateMonthlySalary(visibleMonth, shiftStartDate, salarySettings)
    }

    var showMenu by remember { mutableStateOf(false) }
    var showShiftRateDialog by remember { mutableStateOf(false) }
    var dateForExtraShiftDialog by remember { mutableStateOf<LocalDate?>(null) }
    val localeRu = remember { Locale.forLanguageTag("ru") }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Верный График",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    Box(
                        modifier = Modifier.wrapContentSize(Alignment.TopEnd)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { showMenu = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val currentAvatarUri = avatarUri
                            if (!currentAvatarUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = currentAvatarUri,
                                    contentDescription = "Аватар сотрудника",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = "Аватар сотрудника",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            val displayName = profileName.ifBlank { "Сотрудник" }
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            val monthName = visibleMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, localeRu)
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeRu) else it.toString() }

                            Column(
                                modifier = Modifier
                                    .widthIn(min = 280.dp, max = 320.dp)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Зарплата за $monthName ${visibleMonth.year}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))

                                if (shiftStartDate == null && extraShifts.isEmpty()) {
                                    Text(
                                        text = "Выберите дату первой смены в календаре для расчета зарплаты.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    val formattedSalary = String.format(localeRu, "%,.2f ₽", monthlySalary.totalSalary)
                                    Text(
                                        text = formattedSalary,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Рабочие смены",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = monthlySalary.workDaysCount.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Ставка за смену",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${salarySettings.shiftRate} ₽",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("Настройка ставки за смену") },
                                onClick = {
                                    showMenu = false
                                    showShiftRateDialog = true
                                }
                            )

                            HorizontalDivider()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Тест уведомлений",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            showMenu = false
                                            ShiftReminderWorker.sendTestNotification(context)
                                            Toast.makeText(context, "Тест смены отправлен", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Смена", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            showMenu = false
                                            PaydayWorker.sendTestNotification(context, true)
                                            Toast.makeText(context, "Тест зарплаты отправлен", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Зарплата", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            showMenu = false
                                            PaydayWorker.sendTestNotification(context, false)
                                            Toast.makeText(context, "Тест аванса отправлен", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Аванс", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            HorizontalDivider()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Разработчик: SLK",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = 0.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
            ) {
                MonthSalaryHeader(
                    visibleMonth = visibleMonth,
                    monthlySalary = monthlySalary,
                    localeRu = Locale.forLanguageTag("ru")
                )

                DaysOfWeekHeader()
                
                HorizontalPager(
                    state = pagerState,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    val month = currentMonth.plusMonths((page - centerIndex).toLong())
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        MonthView(
                            month = month,
                            selectedDate = selectedDate,
                            getShiftType = viewModel::getShiftTypeForDate,
                            onDateSelected = { viewModel.onDateSelected(it) },
                            onDateLongClick = { dateForExtraShiftDialog = it }
                        )
                    }
                }

                var tts by remember { mutableStateOf<TextToSpeech?>(null) }
                DisposableEffect(context) {
                    var textToSpeech: TextToSpeech? = null
                    textToSpeech = TextToSpeech(context) { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            textToSpeech?.language = Locale.forLanguageTag("ru")
                        }
                    }
                    tts = textToSpeech
                    onDispose {
                        textToSpeech?.stop()
                        textToSpeech?.shutdown()
                    }
                }

                AnimatedCatView(
                    onClick = {
                        val phrase = catPhrases.random()
                        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                )
            }
        }
    }

    if (showShiftRateDialog) {
        var shiftRateInput by remember { mutableStateOf(if (salarySettings.shiftRate > 0f) salarySettings.shiftRate.toString() else "") }
        AlertDialog(
            onDismissRequest = { showShiftRateDialog = false },
            title = { Text("Настройка ставки за смену") },
            text = {
                OutlinedTextField(
                    value = shiftRateInput,
                    onValueChange = { shiftRateInput = it },
                    label = { Text("Ставка за смену (₽)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = shiftRateInput.toFloatOrNull()
                        if (rate != null && rate >= 0f) {
                            viewModel.updateSalarySettings(rate)
                            showShiftRateDialog = false
                            Toast.makeText(context, "Ставка обновлена", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Введите корректную сумму", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShiftRateDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (dateForExtraShiftDialog != null) {
        val date = dateForExtraShiftDialog!!
        val isExtra = extraShifts.contains(date)
        val localeRu = Locale.forLanguageTag("ru")
        val formattedDate = date.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", localeRu))

        AlertDialog(
            onDismissRequest = { dateForExtraShiftDialog = null },
            title = {
                Text(
                    text = if (isExtra) "Удалить доп. смену на $formattedDate?" else "Добавить доп. смену на $formattedDate?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleExtraShift(date)
                        dateForExtraShiftDialog = null
                    }
                ) {
                    Text(if (isExtra) "Удалить" else "Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { dateForExtraShiftDialog = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun DaysOfWeekHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        val daysOfWeek = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        )
        daysOfWeek.forEach { dayOfWeek ->
            val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
            val textColor = if (isWeekend) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("ru")).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MonthView(
    month: YearMonth,
    selectedDate: LocalDate?,
    getShiftType: (LocalDate) -> ShiftType,
    onDateSelected: (LocalDate) -> Unit,
    onDateLongClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    
    // DayOfWeek starts at MONDAY (1) to SUNDAY (7)
    val firstDayOfWeekIndex = firstDayOfMonth.dayOfWeek.value - 1
    val totalCells = daysInMonth + firstDayOfWeekIndex
    val totalRows = (totalCells + 6) / 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (row in 0 until totalRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - firstDayOfWeekIndex + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = month.atDay(dayNumber)
                        val shiftType = getShiftType(date)
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()

                        DayCell(
                            date = date,
                            shiftType = shiftType,
                            isSelected = isSelected,
                            isToday = isToday,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                            onClick = { onDateSelected(date) },
                            onLongClick = { onDateLongClick(date) }
                        )
                    } else {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    date: LocalDate,
    shiftType: ShiftType,
    isSelected: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    val isPayday = date.dayOfMonth == 10 || date.dayOfMonth == 25

    val backgroundColor = when (shiftType) {
        ShiftType.WORK_DAY -> MaterialTheme.colorScheme.primaryContainer
        ShiftType.EXTRA_SHIFT -> MaterialTheme.colorScheme.tertiaryContainer
        ShiftType.REST_DAY -> MaterialTheme.colorScheme.surfaceContainerHighest
        ShiftType.NONE -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when (shiftType) {
        ShiftType.WORK_DAY -> MaterialTheme.colorScheme.onPrimaryContainer
        ShiftType.EXTRA_SHIFT -> MaterialTheme.colorScheme.onTertiaryContainer
        ShiftType.REST_DAY -> MaterialTheme.colorScheme.onSurfaceVariant
        ShiftType.NONE -> if (isWeekend) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    }

    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else backgroundColor,
        animationSpec = tween(durationMillis = 200),
        label = "cell_bg"
    )

    val animatedContentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        contentColor
    }

    val cellBorder = when {
        isToday -> BorderStroke(2.dp, Color.White)
        isPayday -> BorderStroke(2.dp, Color(0xFF2E7D32))
        else -> null
    }

    val cellShape = RoundedCornerShape(8.dp)

    Surface(
        modifier = modifier
            .clip(cellShape),
        color = animatedBgColor,
        shape = cellShape,
        border = cellBorder,
        tonalElevation = if (isToday) 4.dp else 0.dp,
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = animatedContentColor
                )
                
                when (shiftType) {
                    ShiftType.WORK_DAY -> {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(animatedContentColor)
                        )
                    }
                    ShiftType.EXTRA_SHIFT -> {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Доп. смена",
                            modifier = Modifier.size(10.dp),
                            tint = animatedContentColor
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarScreenPreview() {
    MaterialTheme {
        CalendarScreen()
    }
}

@Composable
fun MonthSalaryHeader(
    visibleMonth: YearMonth,
    monthlySalary: MonthlySalaryResult,
    localeRu: Locale,
    modifier: Modifier = Modifier
) {
    val monthName = visibleMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, localeRu)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeRu) else it.toString() }

    val formattedSalary = if (monthlySalary.totalSalary % 1f == 0f) {
        String.format(localeRu, "%,d ₽", monthlySalary.totalSalary.toInt())
    } else {
        String.format(localeRu, "%,.2f ₽", monthlySalary.totalSalary)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$monthName ${visibleMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "• $formattedSalary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (monthlySalary.workDaysCount > 0) {
            Text(
                text = "${monthlySalary.workDaysCount} смен.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
