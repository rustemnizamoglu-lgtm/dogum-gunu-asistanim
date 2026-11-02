@file:OptIn(ExperimentalMaterial3Api::class)

package com.dogumgunu.asistanim.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dogumgunu.asistanim.util.ShareUtil
import java.time.LocalDate

@Composable
fun AppRoot(vm: AppViewModel) {
    val ctx = LocalContext.current

    val stack by vm.stack.collectAsStateWithLifecycle()
    val people by vm.people.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    var today by remember { mutableStateOf(LocalDate.now()) }
    var permTick by remember { mutableIntStateOf(0) }
    var askNotifications by remember { mutableStateOf(false) }

    // Uygulamaya her dönüşte: tarih ve izin durumları güncellensin (gece yarısı geçmiş olabilir).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        today = LocalDate.now()
        permTick++
    }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    // Android 13+: ilk açılışta bildirim izni için nazik bir açıklama göster.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            !vm.settingsStore.askedNotificationPermission &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications = true
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permTick++ }

    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.importContacts()
        else vm.say("Rehber izni verilmediği için kişiler aktarılamadı.")
    }

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportBackup(uri) }

    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importBackup(uri) }

    fun startContactsImport() {
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) vm.importContacts() else contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    BackHandler(enabled = stack.size > 1 || tab != 0) {
        if (stack.size > 1) vm.pop() else vm.setTab(0)
    }

    when (val screen = stack.last()) {
        is Screen.Main -> {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                when (tab) {
                                    0 -> "Doğum Günü Asistanım 🎂"
                                    1 -> "Takvim"
                                    else -> "Ayarlar"
                                }
                            )
                        }
                    )
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { vm.setTab(0) },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("Ana sayfa") }
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { vm.setTab(1) },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            label = { Text("Takvim") }
                        )
                        NavigationBarItem(
                            selected = tab == 2,
                            onClick = { vm.setTab(2) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text("Ayarlar") }
                        )
                    }
                },
                floatingActionButton = {
                    if (tab == 0) {
                        FloatingActionButton(onClick = { vm.openEdit(null) }) {
                            Icon(Icons.Default.Add, contentDescription = "Yeni kişi ekle")
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) }
            ) { padding ->
                val content = Modifier.padding(padding)
                when (tab) {
                    0 -> HomeTab(
                        people = people,
                        loaded = loaded,
                        today = today,
                        query = query,
                        onQuery = vm::setQuery,
                        category = category,
                        onCategory = vm::setCategory,
                        onOpen = { vm.openDetail(it.id) },
                        onImportContacts = { startContactsImport() },
                        modifier = content
                    )
                    1 -> CalendarTab(
                        people = people,
                        today = today,
                        onOpen = { vm.openDetail(it.id) },
                        modifier = content
                    )
                    else -> SettingsTab(
                        settings = settings,
                        permTick = permTick,
                        peopleCount = people.size,
                        onChange = { change -> vm.updateSettings(change) },
                        onNotificationSettings = { ShareUtil.openNotificationSettings(ctx) },
                        onBatterySettings = { ShareUtil.openBatterySettings(ctx) },
                        onExactAlarmSettings = { ShareUtil.openExactAlarmSettings(ctx) },
                        onTestNotification = { vm.sendTestNotification() },
                        onImportContacts = { startContactsImport() },
                        onExport = { createBackup.launch("dogum-gunu-yedek-${LocalDate.now()}.json") },
                        onImport = { openBackup.launch(arrayOf("*/*")) },
                        modifier = content
                    )
                }
            }
        }

        is Screen.Detail -> {
            val person = people.firstOrNull { it.id == screen.id }
            if (person == null) {
                // Kayıt silinmiş ya da henüz yüklenmedi.
                LaunchedEffect(loaded, screen.id) { if (loaded) vm.pop() }
            } else {
                DetailScreen(
                    person = person,
                    today = today,
                    settings = settings,
                    snackbar = snackbar,
                    onBack = vm::pop,
                    onEdit = { vm.openEdit(person.id) },
                    onDelete = { vm.delete(person) }
                )
            }
        }

        is Screen.Edit -> {
            val existing = screen.id?.let { id -> people.firstOrNull { it.id == id } }
            if (screen.id != null && existing == null) {
                LaunchedEffect(loaded, screen.id) { if (loaded) vm.pop() }
            } else {
                EditScreen(
                    existing = existing,
                    snackbar = snackbar,
                    onSave = vm::save,
                    onBack = vm::pop
                )
            }
        }
    }

    if (askNotifications) {
        AlertDialog(
            onDismissRequest = { askNotifications = false },
            title = { Text("Bildirim izni") },
            text = {
                Text(
                    "Doğum günlerini zamanında hatırlatabilmem için bildirim göndermeme izin vermen gerekiyor."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    askNotifications = false
                    vm.settingsStore.askedNotificationPermission = true
                    if (Build.VERSION.SDK_INT >= 33) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) { Text("İzin ver") }
            },
            dismissButton = {
                TextButton(onClick = {
                    askNotifications = false
                    vm.settingsStore.askedNotificationPermission = true
                }) { Text("Şimdi değil") }
            }
        )
    }
}
