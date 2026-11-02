# Doğum Günü Asistanım 🎂

Reklamsız, internet izni istemeyen, tamamen Türkçe Android doğum günü hatırlatma uygulaması.
Kotlin • Jetpack Compose (Material 3) • MVVM • Room • WorkManager + AlarmManager • minSdk 26 (Android 8.0)

> Ücretsiz ve açık kaynak. Kişisel verileriniz yalnızca kendi telefonunuzda kalır.

## Telefonuna indir (kullanıcılar için)
1. Bu sayfanın sağındaki **Releases** bölümünden en son sürümü aç.
2. **Dogum-Gunu-Asistanim.apk** dosyasını Android telefonuna indir.
3. Dosyaya dokun; "bilinmeyen kaynak" uyarısı çıkarsa Ayarlar'a girip o uygulamaya (Chrome/Dosyalar) izin ver, sonra **Yükle**'ye dokun.
4. Uygulamayı aç, bildirim iznini ver, **Ayarlar** sekmesinden **Deneme bildirimi gönder**'e bas.

(Uygulama Play Store'da değildir; bu yüzden Android kurulum sırasında uyarı gösterir.)

---

## Geliştiriciler için

## APK nasıl elde edilir?

### Yol 1 — GitHub Actions (önerilen)
Depoya her `git push` yapıldığında akış APK'yı derler ve **Releases** altında yayımlar. İmza anahtarı için aşağıdaki "İmza anahtarı" bölümüne bakın.

### Yol 2 — Android Studio ile
1. Android Studio'yu (ücretsiz) kur, **Open** ile bu klasörü aç, Gradle eşitlemesinin bitmesini bekle.
2. Menüden **Build → Build Bundle(s) / APK(s) → Build APK(s)** de.
   Ya da terminalde: `./gradlew assembleRelease` → APK: `app/build/outputs/apk/release/app-release.apk`

## İmza anahtarı (kendi kopyanı derlemek istersen)
Anahtar depoda **yoktur**. GitHub'da kendi kopyanı (fork) derletmek için:
1. `keytool -genkeypair -keystore anahtar.jks -alias dogumgunu -keyalg RSA -keysize 3072 -validity 36500` ile kendi anahtarını üret.
2. Depo **Settings → Secrets and variables → Actions** altına şu 4 secret'i ekle:
   `KEYSTORE_BASE64` (`base64 -w0 anahtar.jks` çıktısı), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. **Actions → APK derle → Run workflow**.

Not: Farklı anahtarla imzalanan sürüm, başka anahtarla kurulmuş sürümün üstüne güncelleme olarak kurulamaz.
Release APK'larını yayımlayan kişi kendi anahtarını güvenle saklamalıdır.

## Özellikler
- Ad/soyad, gün-ay-yıl (yıl isteğe bağlı), fotoğraf, telefon, not, kategori (Aile/Arkadaş/İş/Diğer)
- Ekle / düzenle / sil (silmeden önce onay), isimle arama (Türkçe karakter duyarsız), kategori süzgeci
- Ana ekran: bugün, önümüzdeki 7 gün, bu ay, tüm kişiler, sıradaki doğum günü ve kalan gün
- Takvim görünümü, açık/koyu tema
- Hatırlatma: 7 gün / 3 gün / 1 gün önce, doğum günü sabahı, genel saat ve kişiye özel gün + saat
- Bildirime dokununca kişinin sayfası açılır; her yıl otomatik tekrar eder
- Yeniden başlatma, güncelleme, saat/saat dilimi değişimi sonrası hatırlatmalar yeniden kurulur;
  kapalıyken kaçan bildirimler (en fazla 2 gün) telafi edilir
- 29 Şubat doğumlular: artık olmayan yıllarda 28 Şubat ya da 1 Mart seçeneği
- WhatsApp ile kutlama (hazır mesajlar, düzenlenebilir), yeni yaş otomatik hesabı
- Rehberden içe aktarma (izin yalnızca bu düğmeye basınca istenir), yedekleme/geri yükleme (fotoğraflar dahil JSON)
- İnternet gerekmez; veriler yalnızca cihazdadır (`allowBackup=false`)

## Bildirim sistemi nasıl çalışır?
`ReminderScheduler`: (1) bir sonraki hatırlatma anına tek alarm kurar (izin varsa tam zamanlı);
alarm çalınca zamanı gelenleri gösterip sıradakini kurar. (2) WorkManager 6 saatte bir kontrol eder (güvenlik ağı).
(3) `BootReceiver` yeniden başlatma/güncelleme/saat değişiminde zinciri yeniler.
Tarih ve zaman hesapları `domain/` altındadır ve `app/src/test` içindeki birim testlerle denenir
(`./gradlew testDebugUnitTest`; GitHub akışı da çalıştırır).

## İzinler
`POST_NOTIFICATIONS` (bildirim), `RECEIVE_BOOT_COMPLETED` (yeniden başlatma sonrası), `SCHEDULE_EXACT_ALARM`
(tam zamanında bildirim; verilmezse birkaç dakika sapmalı çalışır), `READ_CONTACTS` (yalnızca rehberden aktarırken).
İnternet, konum, depolama izni **yoktur**; fotoğraflar sistemin fotoğraf seçicisiyle alınır.

## Sorun olursa
Derleme hatası çıkarsa Actions sayfasındaki kırmızı adıma tıklayıp hata metnini kopyala ve bana ilet;
hatayı düzeltip yeni sürümü hazırlarım.
