package com.rahul.vibetube1.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

object DynamicStatusBarUtils {
    private val gradientCache = ConcurrentHashMap<String, List<Color>>()

    suspend fun extractGradientColors(thumbnailUrl: String): List<Color>? = withContext(Dispatchers.IO) {
        if (gradientCache.containsKey(thumbnailUrl)) {
            return@withContext gradientCache[thumbnailUrl]
        }

        try {
            val connection = URL(thumbnailUrl).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val inputStream = connection.getInputStream()
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (originalBitmap == null) return@withContext null
            
            val scaled = Bitmap.createScaledBitmap(originalBitmap, 64, 64, true)
            originalBitmap.recycle()
            
            val pixels = IntArray(64 * 64)
            scaled.getPixels(pixels, 0, 64, 0, 0, 64, 64)
            scaled.recycle()
            
            val colorCounts = HashMap<Int, Int>()
            for (pixel in pixels) {
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)
                
                // Group by 4 bits per channel to cluster similar colors
                val rQuant = r and 0xF0
                val gQuant = g and 0xF0
                val bQuant = b and 0xF0
                val quantized = android.graphics.Color.rgb(rQuant, gQuant, bQuant)
                
                colorCounts[quantized] = (colorCounts[quantized] ?: 0) + 1
            }
            
            val sortedColors = colorCounts.entries.sortedByDescending { it.value }.map { it.key }
            
            val selectedColors = mutableListOf<Int>()
            for (color in sortedColors) {
                if (selectedColors.size >= 3) break
                
                var distinct = true
                for (selected in selectedColors) {
                    if (colorDistance(color, selected) < 80.0) {
                        distinct = false
                        break
                    }
                }
                
                if (distinct) {
                    selectedColors.add(color)
                }
            }
            
            if (selectedColors.isEmpty()) return@withContext null
            
            // Pad to 3 colors if needed by slightly altering the last selected color
            while (selectedColors.size < 3) {
                val lastColor = selectedColors.last()
                val r = android.graphics.Color.red(lastColor)
                val g = android.graphics.Color.green(lastColor)
                val b = android.graphics.Color.blue(lastColor)
                selectedColors.add(android.graphics.Color.rgb((r * 0.8).toInt(), (g * 0.8).toInt(), (b * 0.8).toInt()))
            }
            
            // Sort by hue for a smoother gradient
            selectedColors.sortBy { 
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(it, hsv)
                hsv[0] 
            }
            
            val result = selectedColors.map { c -> 
                val r = android.graphics.Color.red(c)
                val g = android.graphics.Color.green(c)
                val b = android.graphics.Color.blue(c)
                
                // Darken colors to be suitable and readable for a status bar
                val factor = 0.4f
                val darkR = (r * factor).toInt().coerceIn(0, 255)
                val darkG = (g * factor).toInt().coerceIn(0, 255)
                val darkB = (b * factor).toInt().coerceIn(0, 255)
                Color(darkR, darkG, darkB)
            }
            
            gradientCache[thumbnailUrl] = result
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun colorDistance(c1: Int, c2: Int): Double {
        val r1 = android.graphics.Color.red(c1)
        val g1 = android.graphics.Color.green(c1)
        val b1 = android.graphics.Color.blue(c1)
        val r2 = android.graphics.Color.red(c2)
        val g2 = android.graphics.Color.green(c2)
        val b2 = android.graphics.Color.blue(c2)
        
        return sqrt(((r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2)).toDouble())
    }
}
