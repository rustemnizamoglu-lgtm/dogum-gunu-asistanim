package com.dogumgunu.asistanim.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dogumgunu.asistanim.data.AppDatabase
import com.dogumgunu.asistanim.data.Person
import com.dogumgunu.asistanim.data.SettingsStore
import com.dogumgunu.asistanim.domain.SettingsSnapshot
import com.dogumgunu.asistanim.notify.NotificationHelper
import com.dogumgunu.asistanim.notify.ReminderScheduler
import com.dogumgunu.asistanim.util.BackupManager
import com.dogumgunu.asistanim.util.ContactsImporter
import com.dogumgunu.asistanim.util.PhotoStore
import com.dogumgunu.asistanim.util.TurkishText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

sealed interface Screen {
    data object Main : Screen
    data class Detail(val id: Long) : Screen
    data class Edit(val id: Long?) : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val dao = AppDatabase.get(ctx).personDao()
    val settingsStore = SettingsStore(ctx)

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    val people: StateFlow<List<Person>> = dao.observeAll()
        .onEach { _loaded.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _settings = MutableStateFlow(settingsStore.snapshot())
    val settings: StateFlow<SettingsSnapshot> = _settings.asStateFlow()

    private val _stack = MutableStateFlow<List<Screen>>(listOf(Screen.Main))
    val stack: StateFlow<List<Screen>> = _stack.asStateFlow()

    private val _tab = MutableStateFlow(0)
    val tab: StateFlow<Int> = _tab.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _category = MutableStateFlow<String?>(null)
    val category: StateFlow<String?> = _category.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun say(text: String) {
        _messages.tryEmit(text)
    }

    // ---- Gezinme -------------------------------------------------------------------------

    fun setTab(index: Int) {
        _tab.value = index
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun setCategory(c: String?) {
        _category.value = c
    }

    fun openDetail(id: Long) {
        _stack.value = _stack.value + Screen.Detail(id)
    }

    fun openEdit(id: Long?) {
        _stack.value = _stack.value + Screen.Edit(id)
    }

    /** Bildirime dokunulunca: ana ekran + kişinin ayrıntı sayfası. */
    fun openFromNotification(id: Long) {
        _tab.value = 0
        _stack.value = listOf(Screen.Main, Screen.Detail(id))
    }

    fun pop() {
        val s = _stack.value
        if (s.size > 1) _stack.value = s.dropLast(1)
    }

    private fun popToMain() {
        _stack.value = listOf(Screen.Main)
    }

    // ---- Kişi işlemleri ------------------------------------------------------------------

    /**
     * Veri değişmeden önce zamanı gelmiş bildirimleri gösterir; böylece yeni eklenen/düzenlenen
     * bir kişi için geçmişe dönük sahte bildirim oluşmaz.
     */
    private suspend fun beforeChange() {
        ReminderScheduler.fireDue(ctx)
    }

    fun save(person: Person) {
        viewModelScope.launch {
            try {
                beforeChange()
                if (person.id == 0L) {
                    dao.insert(person)
                } else {
                    val old = dao.getById(person.id)
                    dao.update(person)
                    if (old?.photo != null && old.photo != person.photo) PhotoStore.delete(ctx, old.photo)
                }
                ReminderScheduler.scheduleNext(ctx)
                say("Kaydedildi ✓")
                if (person.id == 0L) popToMain() else pop()
            } catch (e: Exception) {
                say("Kaydedilemedi: ${e.message}")
            }
        }
    }

    fun delete(person: Person) {
        viewModelScope.launch {
            try {
                dao.delete(person)
                PhotoStore.delete(ctx, person.photo)
                ReminderScheduler.scheduleNext(ctx)
                popToMain()
                say("${person.fullName} silindi.")
            } catch (e: Exception) {
                say("Silinemedi: ${e.message}")
            }
        }
    }

    // ---- Ayarlar -------------------------------------------------------------------------

    fun updateSettings(change: (SettingsSnapshot) -> SettingsSnapshot) {
        val updated = change(_settings.value)
        _settings.value = updated
        settingsStore.save(updated)
        viewModelScope.launch { ReminderScheduler.scheduleNext(ctx) }
    }

    fun sendTestNotification() {
        if (!NotificationHelper.showTest(ctx)) {
            say("Bildirimler kapalı. Önce bildirim iznini açın.")
        }
    }

    // ---- Yedekleme / içe aktarma ---------------------------------------------------------

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    val all = dao.getAll()
                    val json = BackupManager.toJson(ctx, all)
                    val out = ctx.contentResolver.openOutputStream(uri, "wt")
                        ?: throw IllegalStateException("Dosya açılamadı.")
                    out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    all.size
                }
                say("Yedek kaydedildi ($count kişi).")
            } catch (e: Exception) {
                say("Yedek alınamadı: ${e.message}")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val incoming = withContext(Dispatchers.IO) {
                    val text = ctx.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: throw IllegalStateException("Dosya okunamadı.")
                    BackupManager.fromJson(ctx, text)
                }
                val (added, skipped) = addWithoutDuplicates(incoming)
                say("Yedek yüklendi: $added kişi eklendi, $skipped kişi zaten kayıtlıydı.")
            } catch (e: IllegalArgumentException) {
                say(e.message ?: "Yedek yüklenemedi.")
            } catch (e: Exception) {
                say("Yedek yüklenemedi: ${e.message}")
            }
        }
    }

    fun importContacts() {
        viewModelScope.launch {
            try {
                val found = ContactsImporter.read(ctx)
                val (added, skipped) = addWithoutDuplicates(found.people)
                val extra = if (found.withoutValidDate > 0) " ${found.withoutValidDate} kayıt okunamadı." else ""
                say("Rehberden $added kişi eklendi, $skipped kişi zaten kayıtlıydı.$extra")
            } catch (e: Exception) {
                say("Rehber okunamadı: ${e.message}")
            }
        }
    }

    /** Aynı ad + gün + ay olan kayıtları atlayarak ekler. (eklenen, atlanan) döndürür. */
    private suspend fun addWithoutDuplicates(incoming: List<Person>): Pair<Int, Int> {
        beforeChange()
        fun key(p: Person) = "${TurkishText.fold(p.fullName)}|${p.day}|${p.month}"
        val existing = dao.getAll().map { key(it) }.toMutableSet()
        val toAdd = ArrayList<Person>()
        var skipped = 0
        for (p in incoming) {
            if (existing.add(key(p))) toAdd.add(p) else {
                skipped++
                PhotoStore.delete(ctx, p.photo)
            }
        }
        if (toAdd.isNotEmpty()) dao.insertAll(toAdd)
        ReminderScheduler.scheduleNext(ctx)
        return toAdd.size to skipped
    }

    fun today(): LocalDate = LocalDate.now()
}
