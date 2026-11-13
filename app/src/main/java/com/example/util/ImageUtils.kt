package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Les photos sont réduites (640 px max, JPEG) et stockées en base64 dans le document Firestore.
 * Avantage : aucun service de stockage payant n'est nécessaire. Limite : à remplacer par
 * Firebase Storage si le volume de photos grandit.
 */
object ImageUtils {

    private const val MAX_SIDE = 640
    private const val QUALITY = 65

    fun loadScaledBitmap(context: Context, uri: Uri, maxSide: Int = MAX_SIDE): Bitmap? {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null

            var bitmap = decoded
            val longest = max(decoded.width, decoded.height)
            if (longest > maxSide) {
                val scale = maxSide.toFloat() / longest
                bitmap = Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            }
            rotateIfNeeded(context, uri, bitmap)
        } catch (e: Exception) {
            null
        }
    }

    private fun rotateIfNeeded(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                android.media.ExifInterface(stream).getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )
            } ?: android.media.ExifInterface.ORIENTATION_NORMAL
            val degrees = when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees == 0f) bitmap
            else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
        } catch (e: Exception) {
            bitmap
        }
    }

    fun toBase64(bitmap: Bitmap, quality: Int = QUALITY): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun fromBase64(b64: String): Bitmap? {
        if (b64.isBlank()) return null
        return try {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
