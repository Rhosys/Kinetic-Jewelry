package ch.rhosys.lyra.data

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import java.io.File

object AppIconCache {

    private const val DIR = "app_icons"
    private const val ICON_SIZE_PX = 192

    fun loadIcon(context: Context, packageName: String): Drawable? {
        val file = iconFile(context, packageName)
        if (!file.exists()) return null
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            BitmapDrawable(context.resources, bitmap)
        } catch (_: Exception) {
            null
        }
    }

    fun saveIcon(context: Context, packageName: String, drawable: Drawable) {
        val file = iconFile(context, packageName)
        if (file.exists()) return
        try {
            file.parentFile?.mkdirs()
            val bitmap = drawable.toBitmap(ICON_SIZE_PX, ICON_SIZE_PX)
            file.outputStream().buffered().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (_: Exception) {
            file.delete()
        }
    }

    fun hasIcon(context: Context, packageName: String): Boolean = iconFile(context, packageName).exists()

    private fun iconFile(context: Context, packageName: String) =
        File(context.cacheDir, "$DIR/$packageName.png")
}
