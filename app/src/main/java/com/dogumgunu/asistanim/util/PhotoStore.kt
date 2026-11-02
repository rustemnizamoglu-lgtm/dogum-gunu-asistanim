package com.dogumgunu.asistanim.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/** Kişi fotoğrafları uygulamanın özel klasörüne küçültülerek kopyalanır; galeri izni gerekmez. */
object PhotoStore {
    private const val MAX_SIDE = 480

    private fun dir(ctx: Context): File = File(ctx.filesDir, "photos").also { it.mkdirs() }

    fun file(ctx: Context, name: String?): File? = if (name.isNullOrEmpty()) null else File(dir(ctx), name)

    fun load(ctx: Context, name: String?): Bitmap? {
        val f = file(ctx, name) ?: return null
        return if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }

    fun delete(ctx: Context, name: String?) {
        file(ctx, name)?.delete()
    }

    private fun newFile(ctx: Context): File =
        File(dir(ctx), "foto_${System.currentTimeMillis()}_${(0..99999).random()}.jpg")

    /** Seçilen görseli küçültüp kaydeder ve dosya adını döndürür; başarısızsa null. */
    fun saveFromUri(ctx: Context, uri: Uri): String? {
        return try {
            val cr = ctx.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null

            val rotation = try {
                cr.openInputStream(uri)?.use { stream ->
                    when (ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f
            } catch (e: Exception) {
                0f
            }

            val scale = MAX_SIDE.toFloat() / max(decoded.width, decoded.height)
            val matrix = Matrix()
            if (scale < 1f) matrix.postScale(scale, scale)
            if (rotation != 0f) matrix.postRotate(rotation)
            val finalBitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

            val out = newFile(ctx)
            FileOutputStream(out).use { finalBitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            out.name
        } catch (e: Exception) {
            null
        }
    }

    /** Yedekten gelen JPEG verisini yeni bir dosyaya yazar. */
    fun saveBytes(ctx: Context, bytes: ByteArray): String? = try {
        val out = newFile(ctx)
        out.writeBytes(bytes)
        out.name
    } catch (e: Exception) {
        null
    }

    /** Hiçbir kişiye bağlı olmayan ve bir günden eski fotoğrafları siler. */
    fun cleanOrphans(ctx: Context, keep: Set<String>) {
        val limit = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        dir(ctx).listFiles()?.forEach {
            if (it.name !in keep && it.lastModified() < limit) it.delete()
        }
    }
}
