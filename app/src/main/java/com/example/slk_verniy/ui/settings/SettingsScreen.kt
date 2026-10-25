package com.example.slk_verniy.ui.settings

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.slk_verniy.BuildConfig
import com.example.slk_verniy.data.PreferencesRepository
import com.example.slk_verniy.data.SalaryPreferencesManager
import com.example.slk_verniy.data.SalarySettings
import com.example.slk_verniy.ui.main.UpdateDialog
import com.example.slk_verniy.worker.AlarmScheduler
import com.example.slk_verniy.worker.AppUpdateChecker
import com.example.slk_verniy.worker.ReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val salaryPreferencesManager = SalaryPreferencesManager(application)
    private val preferencesRepository = PreferencesRepository(application)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                SettingsViewModel(app)
            }
        }
    }

    val salarySettings: StateFlow<SalarySettings> = salaryPreferencesManager.salarySettings
        .catch { emit(SalarySettings(0f)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SalarySettings(0f)
        )

    val notificationHour: StateFlow<Int> = preferencesRepository.notificationHour
        .catch { emit(20) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 20
        )

    val notificationMinute: StateFlow<Int> = preferencesRepository.notificationMinute
        .catch { emit(0) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val shiftStartHour: StateFlow<Int> = preferencesRepository.shiftStartHour
        .catch { emit(11) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 11
        )

    val shiftStartMinute: StateFlow<Int> = preferencesRepository.shiftStartMinute
        .catch { emit(0) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val shiftEndHour: StateFlow<Int> = preferencesRepository.shiftEndHour
        .catch { emit(23) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 23
        )

    val shiftEndMinute: StateFlow<Int> = preferencesRepository.shiftEndMinute
        .catch { emit(0) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun updateSalarySettings(shiftRate: Float) {
        viewModelScope.launch {
            try {
                salaryPreferencesManager.updateSalarySettings(shiftRate)
            } catch (_: Exception) {
                // Handle exception
            }
        }
    }

    fun updateNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                preferencesRepository.saveNotificationTime(hour, minute)
                AlarmScheduler.scheduleAll(getApplication())
            } catch (_: Exception) {
                // Handle exception
            }
        }
    }

    fun updateShiftStartTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                preferencesRepository.saveShiftStartTime(hour, minute)
                AlarmScheduler.scheduleAll(getApplication())
            } catch (_: Exception) {
                // Handle exception
            }
        }
    }

    fun updateShiftEndTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                preferencesRepository.saveShiftEndTime(hour, minute)
                AlarmScheduler.scheduleAll(getApplication())
            } catch (_: Exception) {
                // Handle exception
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val context = LocalContext.current
    val salarySettings by viewModel.salarySettings.collectAsState()
    val notificationHour by viewModel.notificationHour.collectAsState()
    val notificationMinute by viewModel.notificationMinute.collectAsState()
    val shiftStartHour by viewModel.shiftStartHour.collectAsState()
    val shiftStartMinute by viewModel.shiftStartMinute.collectAsState()
    val shiftEndHour by viewModel.shiftEndHour.collectAsState()
    val shiftEndMinute by viewModel.shiftEndMinute.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val versionName = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME
        }
    }

    var shiftRateInput by remember(salarySettings.shiftRate) {
        mutableStateOf(if (salarySettings.shiftRate > 0f) salarySettings.shiftRate.toString() else "")
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Salary Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Расчет зарплаты",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Укажите вашу ставку оплаты за одну рабочую смену для автоматического расчета месячного заработка.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = shiftRateInput,
                        onValueChange = { shiftRateInput = it },
                        label = { Text("Оплата за смену (₽)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val rate = shiftRateInput.toFloatOrNull()
                            if (rate != null && rate >= 0f) {
                                viewModel.updateSalarySettings(rate)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Ставка успешно сохранена!")
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Введите корректную сумму")
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сохранить ставку")
                    }
                }
            }

            // Notification Time Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Время уведомлений",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Выберите время, когда вы хотите получать уведомления о сменах.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val formattedStartTime = String.format("%02d:%02d", shiftStartHour, shiftStartMinute)
                    val formattedEndTime = String.format("%02d:%02d", shiftEndHour, shiftEndMinute)
                    val formattedReminderTime = String.format("%02d:%02d", notificationHour, notificationMinute)

                    // 1. Shift Start Time
                    OutlinedCard(
                        onClick = {
                            android.app.TimePickerDialog(
                                context,
                                { _, selectedHour, selectedMinute ->
                                    viewModel.updateShiftStartTime(selectedHour, selectedMinute)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            String.format("Начало смены изменено на %02d:%02d", selectedHour, selectedMinute)
                                        )
                                    }
                                },
                                shiftStartHour,
                                shiftStartMinute,
                                true
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Начало смены",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = formattedStartTime,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. Shift End Time
                    OutlinedCard(
                        onClick = {
                            android.app.TimePickerDialog(
                                context,
                                { _, selectedHour, selectedMinute ->
                                    viewModel.updateShiftEndTime(selectedHour, selectedMinute)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            String.format("Окончание смены изменено на %02d:%02d", selectedHour, selectedMinute)
                                        )
                                    }
                                },
                                shiftEndHour,
                                shiftEndMinute,
                                true
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Окончание смены",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = formattedEndTime,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 3. Reminder Time
                    OutlinedCard(
                        onClick = {
                            android.app.TimePickerDialog(
                                context,
                                { _, selectedHour, selectedMinute ->
                                    viewModel.updateNotificationTime(selectedHour, selectedMinute)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            String.format("Время напоминания изменено на %02d:%02d", selectedHour, selectedMinute)
                                        )
                                    }
                                },
                                notificationHour,
                                notificationMinute,
                                true
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Время напоминания (на завтра)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = formattedReminderTime,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // App Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "О приложении",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "SLK Верный — график работы и расчет зарплаты для сотрудников сети «Верный».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    SettingRow(
                        icon = Icons.Rounded.Apps,
                        label = "Название",
                        value = "SLK Верный"
                    )

                    SettingRow(
                        icon = Icons.Rounded.Code,
                        label = "Разработчик",
                        value = "SLK"
                    )

                    SettingRow(
                        icon = Icons.Rounded.Verified,
                        label = "Версия",
                        value = versionName
                    )

                    SettingRow(
                        icon = Icons.Rounded.Folder,
                        label = "Пакет",
                        value = "com.example.slk_verniy"
                    )

                    HorizontalDivider()

                    var releaseInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
                    var isCheckingUpdate by remember { mutableStateOf(false) }

                    releaseInfo?.let { info ->
                        UpdateDialog(
                            releaseInfo = info,
                            onDismiss = { releaseInfo = null }
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (!isCheckingUpdate) {
                                isCheckingUpdate = true
                                scope.launch {
                                    val info: ReleaseInfo? = withContext(Dispatchers.IO) {
                                        AppUpdateChecker.checkForUpdate(versionName)
                                    }
                                    isCheckingUpdate = false
                                    if (info != null) {
                                        releaseInfo = info
                                    } else {
                                        snackbarHostState.showSnackbar("У вас установлена самая новая версия")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCheckingUpdate
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Проверка...")
                        } else {
                            Icon(Icons.Rounded.Update, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Проверить обновление")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen()
    }
}
