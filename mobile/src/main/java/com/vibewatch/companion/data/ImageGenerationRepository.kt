package com.vibewatch.companion.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

class ImageGenerationRepository(private val context: Context) {
    private val geminiApi = GeminiApiService.create()
    
    /**
     * Generate a watch face with complete customization based on a text prompt.
     * Uses Gemini to analyze the prompt and generate appropriate visual parameters,
     * including watch type (analog/digital), colors, fonts, and styling.
     */
    suspend fun generateWatchFaceImage(prompt: String, apiKey: String): Result<WatchFaceData> {
        return withContext(Dispatchers.IO) {
            try {
                // Use Gemini to analyze the prompt and extract design parameters
                val designParams = analyzePromptWithGemini(prompt, apiKey)
                
                // Generate the watch face based on parameters
                val bitmap = if (designParams.isAnalog) {
                    generateAnalogWatchFace(designParams)
                } else {
                    generateDigitalWatchFace(designParams)
                }
                
                // Convert bitmap to byte array
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val imageBytes = outputStream.toByteArray()
                
                bitmap.recycle()
                
                Result.success(WatchFaceData(imageBytes, designParams))
            } catch (e: Exception) {
                Log.e(TAG, "Error generating image", e)
                Result.failure(e)
            }
        }
    }
    
    private suspend fun analyzePromptWithGemini(prompt: String, apiKey: String): WatchFaceDesignParams {
        return try {
            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(
                                text = """Analyze this watch face design prompt and extract design parameters:
                                    "$prompt"
                                    
                                    Respond in this exact format (each parameter on a new line):
                                    TYPE: [ANALOG or DIGITAL]
                                    BACKGROUND_COLOR1: [hex without #]
                                    BACKGROUND_COLOR2: [hex without #]
                                    BACKGROUND_COLOR3: [hex without #]
                                    CLOCK_COLOR: [hex without #]
                                    FONT_STYLE: [BOLD, NORMAL, LIGHT, MONO, SERIF]
                                    DIAL_STYLE: [CLASSIC, MODERN, SWORD, MINIMAL, ORNATE]
                                    SHOW_NUMBERS: [YES or NO]
                                    BACKGROUND_STYLE: [GRADIENT, RADIAL, NATURE, METALLIC, ABSTRACT]
                                    
                                    Example response:
                                    TYPE: ANALOG
                                    BACKGROUND_COLOR1: C0C0C0
                                    BACKGROUND_COLOR2: 808080
                                    BACKGROUND_COLOR3: 404040
                                    CLOCK_COLOR: 000000
                                    FONT_STYLE: BOLD
                                    DIAL_STYLE: SWORD
                                    SHOW_NUMBERS: NO
                                    BACKGROUND_STYLE: METALLIC
                                    
                                    Analyze the prompt carefully and choose appropriate values.""".trimIndent()
                            )
                        )
                    )
                )
            )
            
            val response = geminiApi.generateContent(apiKey, request)
            
            if (response.isSuccessful && response.body()?.candidates?.isNotEmpty() == true) {
                val text = response.body()?.candidates?.first()?.content?.parts?.first()?.text ?: ""
                Log.d(TAG, "Gemini response: $text")
                parseDesignParams(text)
            } else {
                Log.w(TAG, "Failed to get design params from Gemini, using defaults")
                getDefaultDesignParams()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API", e)
            getDefaultDesignParams()
        }
    }
    
    private fun parseDesignParams(text: String): WatchFaceDesignParams {
        return try {
            val lines = text.lines().associate { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else {
                    "" to ""
                }
            }
            
            WatchFaceDesignParams(
                isAnalog = lines["TYPE"]?.uppercase()?.contains("ANALOG") == true,
                backgroundColors = listOf(
                    parseColor(lines["BACKGROUND_COLOR1"] ?: "6200EE"),
                    parseColor(lines["BACKGROUND_COLOR2"] ?: "03DAC5"),
                    parseColor(lines["BACKGROUND_COLOR3"] ?: "FF6B6B")
                ),
                clockColor = parseColor(lines["CLOCK_COLOR"] ?: "FFFFFF"),
                fontStyle = parseFontStyle(lines["FONT_STYLE"] ?: "BOLD"),
                dialStyle = parseDialStyle(lines["DIAL_STYLE"] ?: "CLASSIC"),
                showNumbers = lines["SHOW_NUMBERS"]?.uppercase()?.contains("YES") == true,
                backgroundStyle = parseBackgroundStyle(lines["BACKGROUND_STYLE"] ?: "GRADIENT")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing design params", e)
            getDefaultDesignParams()
        }
    }
    
    private fun parseColor(hex: String): Int {
        return try {
            val cleanHex = hex.replace("#", "").trim()
            Color.parseColor("#$cleanHex")
        } catch (e: Exception) {
            Color.parseColor("#FFFFFF")
        }
    }
    
    private fun parseFontStyle(style: String): FontStyle {
        return when (style.uppercase()) {
            "BOLD" -> FontStyle.BOLD
            "LIGHT" -> FontStyle.LIGHT
            "MONO" -> FontStyle.MONO
            "SERIF" -> FontStyle.SERIF
            else -> FontStyle.NORMAL
        }
    }
    
    private fun parseDialStyle(style: String): DialStyle {
        return when (style.uppercase()) {
            "MODERN" -> DialStyle.MODERN
            "SWORD" -> DialStyle.SWORD
            "MINIMAL" -> DialStyle.MINIMAL
            "ORNATE" -> DialStyle.ORNATE
            else -> DialStyle.CLASSIC
        }
    }
    
    private fun parseBackgroundStyle(style: String): BackgroundStyle {
        return when (style.uppercase()) {
            "RADIAL" -> BackgroundStyle.RADIAL
            "NATURE" -> BackgroundStyle.NATURE
            "METALLIC" -> BackgroundStyle.METALLIC
            "ABSTRACT" -> BackgroundStyle.ABSTRACT
            else -> BackgroundStyle.GRADIENT
        }
    }
    
    private fun getDefaultDesignParams(): WatchFaceDesignParams {
        return WatchFaceDesignParams(
            isAnalog = true,
            backgroundColors = listOf(
                Color.parseColor("#6200EE"),
                Color.parseColor("#03DAC5"),
                Color.parseColor("#FF6B6B")
            ),
            clockColor = Color.WHITE,
            fontStyle = FontStyle.BOLD,
            dialStyle = DialStyle.CLASSIC,
            showNumbers = true,
            backgroundStyle = BackgroundStyle.GRADIENT
        )
    }
    
    private fun generateDigitalWatchFace(params: WatchFaceDesignParams): Bitmap {
        val width = 512
        val height = 512
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background
        drawBackground(canvas, width, height, params)
        
        // Draw sample time (12:34:56)
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = params.clockColor
            textSize = 72f
            textAlign = Paint.Align.CENTER
            typeface = when (params.fontStyle) {
                FontStyle.BOLD -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                FontStyle.LIGHT -> Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                FontStyle.MONO -> Typeface.MONOSPACE
                FontStyle.SERIF -> Typeface.SERIF
                else -> Typeface.DEFAULT
            }
            setShadowLayer(8f, 0f, 0f, Color.BLACK)
        }
        
        canvas.drawText("12:34", width / 2f, height / 2f + 20f, timePaint)
        
        // Draw date if space allows
        val datePaint = Paint(timePaint).apply {
            textSize = 28f
        }
        canvas.drawText("MON, JAN 1", width / 2f, height / 2f + 70f, datePaint)
        
        return bitmap
    }
    
    private fun generateAnalogWatchFace(params: WatchFaceDesignParams): Bitmap {
        val width = 512
        val height = 512
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background
        drawBackground(canvas, width, height, params)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = width / 2f - 40f
        
        // Draw hour markers and numbers
        if (params.showNumbers) {
            val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = params.clockColor
                textSize = 32f
                textAlign = Paint.Align.CENTER
                typeface = when (params.fontStyle) {
                    FontStyle.BOLD -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    FontStyle.SERIF -> Typeface.SERIF
                    else -> Typeface.DEFAULT
                }
                setShadowLayer(4f, 0f, 0f, Color.BLACK)
            }
            
            for (i in 1..12) {
                val angle = (i * 30 - 90) * PI / 180
                val x = centerX + (radius - 30) * cos(angle).toFloat()
                val y = centerY + (radius - 30) * sin(angle).toFloat() + 10f
                canvas.drawText(i.toString(), x, y, numberPaint)
            }
        } else {
            // Draw hour markers as ticks
            val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = params.clockColor
                strokeWidth = 4f
                setShadowLayer(4f, 0f, 0f, Color.BLACK)
            }
            
            for (i in 0..11) {
                val angle = (i * 30 - 90) * PI / 180
                val startX = centerX + (radius - 20) * cos(angle).toFloat()
                val startY = centerY + (radius - 20) * sin(angle).toFloat()
                val endX = centerX + (radius - 5) * cos(angle).toFloat()
                val endY = centerY + (radius - 5) * sin(angle).toFloat()
                canvas.drawLine(startX, startY, endX, endY, markerPaint)
            }
        }
        
        // Draw clock hands (showing 10:10)
        drawClockHands(canvas, centerX, centerY, radius, params)
        
        // Draw center circle
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = params.clockColor
            style = Paint.Style.FILL
            setShadowLayer(4f, 0f, 0f, Color.BLACK)
        }
        canvas.drawCircle(centerX, centerY, 15f, centerPaint)
        
        return bitmap
    }
    
    private fun drawBackground(canvas: Canvas, width: Int, height: Int, params: WatchFaceDesignParams) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        when (params.backgroundStyle) {
            BackgroundStyle.RADIAL -> {
                paint.shader = RadialGradient(
                    width / 2f, height / 2f, width / 2f,
                    intArrayOf(params.backgroundColors[0], params.backgroundColors[1], params.backgroundColors[2]),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            BackgroundStyle.NATURE -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(params.backgroundColors[0], params.backgroundColors[1]),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                
                // Add nature-like decorative elements
                val naturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    alpha = 100
                }
                for (i in 0..5) {
                    naturePaint.color = params.backgroundColors[2]
                    val x = Random.nextFloat() * width
                    val y = height * 0.7f + Random.nextFloat() * height * 0.3f
                    canvas.drawCircle(x, y, Random.nextFloat() * 40 + 20, naturePaint)
                }
                return
            }
            BackgroundStyle.METALLIC -> {
                // Create metallic effect with multiple gradients
                paint.shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(params.backgroundColors[0], params.backgroundColors[1], params.backgroundColors[0]),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                
                // Add metallic shine
                val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    alpha = 30
                    shader = LinearGradient(
                        width * 0.3f, 0f, width * 0.7f, height.toFloat(),
                        intArrayOf(Color.TRANSPARENT, Color.WHITE, Color.TRANSPARENT),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shinePaint)
                return
            }
            BackgroundStyle.ABSTRACT -> {
                paint.color = params.backgroundColors[0]
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                
                // Add abstract shapes
                val abstractPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    alpha = 150
                }
                for (i in 0..4) {
                    abstractPaint.color = params.backgroundColors[i % params.backgroundColors.size]
                    val path = Path()
                    path.moveTo(Random.nextFloat() * width, Random.nextFloat() * height)
                    for (j in 0..3) {
                        path.lineTo(Random.nextFloat() * width, Random.nextFloat() * height)
                    }
                    path.close()
                    canvas.drawPath(path, abstractPaint)
                }
                return
            }
            else -> {
                // Default gradient
                paint.shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(params.backgroundColors[0], params.backgroundColors[1], params.backgroundColors[2]),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
        }
        
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
    
    private fun drawClockHands(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, params: WatchFaceDesignParams) {
        val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = params.clockColor
            style = Paint.Style.FILL
            setShadowLayer(6f, 0f, 0f, Color.BLACK)
        }
        
        // Hour hand (pointing at 10)
        val hourAngle = (10 * 30 - 90) * PI / 180
        when (params.dialStyle) {
            DialStyle.SWORD -> {
                drawSwordHand(canvas, centerX, centerY, radius * 0.5f, hourAngle.toFloat(), 12f, handPaint)
            }
            DialStyle.MODERN -> {
                drawModernHand(canvas, centerX, centerY, radius * 0.5f, hourAngle.toFloat(), 10f, handPaint)
            }
            DialStyle.MINIMAL -> {
                drawMinimalHand(canvas, centerX, centerY, radius * 0.5f, hourAngle.toFloat(), 6f, handPaint)
            }
            DialStyle.ORNATE -> {
                drawOrnateHand(canvas, centerX, centerY, radius * 0.5f, hourAngle.toFloat(), 12f, handPaint)
            }
            else -> {
                drawClassicHand(canvas, centerX, centerY, radius * 0.5f, hourAngle.toFloat(), 10f, handPaint)
            }
        }
        
        // Minute hand (pointing at 2)
        val minuteAngle = (10 * 6 - 90) * PI / 180
        when (params.dialStyle) {
            DialStyle.SWORD -> {
                drawSwordHand(canvas, centerX, centerY, radius * 0.75f, minuteAngle.toFloat(), 8f, handPaint)
            }
            DialStyle.MODERN -> {
                drawModernHand(canvas, centerX, centerY, radius * 0.75f, minuteAngle.toFloat(), 6f, handPaint)
            }
            DialStyle.MINIMAL -> {
                drawMinimalHand(canvas, centerX, centerY, radius * 0.75f, minuteAngle.toFloat(), 4f, handPaint)
            }
            DialStyle.ORNATE -> {
                drawOrnateHand(canvas, centerX, centerY, radius * 0.75f, minuteAngle.toFloat(), 8f, handPaint)
            }
            else -> {
                drawClassicHand(canvas, centerX, centerY, radius * 0.75f, minuteAngle.toFloat(), 6f, handPaint)
            }
        }
    }
    
    private fun drawClassicHand(canvas: Canvas, centerX: Float, centerY: Float, length: Float, angle: Float, width: Float, paint: Paint) {
        val path = Path()
        val endX = centerX + length * cos(angle.toDouble()).toFloat()
        val endY = centerY + length * sin(angle.toDouble()).toFloat()
        
        path.moveTo(centerX - width / 2 * sin(angle.toDouble()).toFloat(), centerY + width / 2 * cos(angle.toDouble()).toFloat())
        path.lineTo(endX, endY)
        path.lineTo(centerX + width / 2 * sin(angle.toDouble()).toFloat(), centerY - width / 2 * cos(angle.toDouble()).toFloat())
        path.close()
        
        canvas.drawPath(path, paint)
    }
    
    private fun drawSwordHand(canvas: Canvas, centerX: Float, centerY: Float, length: Float, angle: Float, width: Float, paint: Paint) {
        val path = Path()
        val endX = centerX + length * cos(angle.toDouble()).toFloat()
        val endY = centerY + length * sin(angle.toDouble()).toFloat()
        val tipLength = length * 0.2f
        val tipEndX = centerX + (length + tipLength) * cos(angle.toDouble()).toFloat()
        val tipEndY = centerY + (length + tipLength) * sin(angle.toDouble()).toFloat()
        
        // Draw sword blade with pointed tip
        path.moveTo(centerX - width / 2 * sin(angle.toDouble()).toFloat(), centerY + width / 2 * cos(angle.toDouble()).toFloat())
        path.lineTo(endX - width / 3 * sin(angle.toDouble()).toFloat(), endY + width / 3 * cos(angle.toDouble()).toFloat())
        path.lineTo(tipEndX, tipEndY)
        path.lineTo(endX + width / 3 * sin(angle.toDouble()).toFloat(), endY - width / 3 * cos(angle.toDouble()).toFloat())
        path.lineTo(centerX + width / 2 * sin(angle.toDouble()).toFloat(), centerY - width / 2 * cos(angle.toDouble()).toFloat())
        path.close()
        
        canvas.drawPath(path, paint)
    }
    
    private fun drawModernHand(canvas: Canvas, centerX: Float, centerY: Float, length: Float, angle: Float, width: Float, paint: Paint) {
        val endX = centerX + length * cos(angle.toDouble()).toFloat()
        val endY = centerY + length * sin(angle.toDouble()).toFloat()
        
        paint.strokeWidth = width
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(centerX, centerY, endX, endY, paint)
        paint.style = Paint.Style.FILL
    }
    
    private fun drawMinimalHand(canvas: Canvas, centerX: Float, centerY: Float, length: Float, angle: Float, width: Float, paint: Paint) {
        val endX = centerX + length * cos(angle.toDouble()).toFloat()
        val endY = centerY + length * sin(angle.toDouble()).toFloat()
        
        paint.strokeWidth = width
        paint.style = Paint.Style.STROKE
        canvas.drawLine(centerX, centerY, endX, endY, paint)
        paint.style = Paint.Style.FILL
    }
    
    private fun drawOrnateHand(canvas: Canvas, centerX: Float, centerY: Float, length: Float, angle: Float, width: Float, paint: Paint) {
        val path = Path()
        val endX = centerX + length * cos(angle.toDouble()).toFloat()
        val endY = centerY + length * sin(angle.toDouble()).toFloat()
        
        // Create ornate design with decorative elements
        val midX = centerX + (length * 0.5f) * cos(angle.toDouble()).toFloat()
        val midY = centerY + (length * 0.5f) * sin(angle.toDouble()).toFloat()
        
        path.moveTo(centerX - width / 2 * sin(angle.toDouble()).toFloat(), centerY + width / 2 * cos(angle.toDouble()).toFloat())
        path.lineTo(midX - width * 0.7f * sin(angle.toDouble()).toFloat(), midY + width * 0.7f * cos(angle.toDouble()).toFloat())
        path.lineTo(endX, endY)
        path.lineTo(midX + width * 0.7f * sin(angle.toDouble()).toFloat(), midY - width * 0.7f * cos(angle.toDouble()).toFloat())
        path.lineTo(centerX + width / 2 * sin(angle.toDouble()).toFloat(), centerY - width / 2 * cos(angle.toDouble()).toFloat())
        path.close()
        
        canvas.drawPath(path, paint)
        
        // Add decorative circle in the middle
        canvas.drawCircle(midX, midY, width * 0.6f, paint)
    }
    
    // Data classes
    data class WatchFaceData(
        val imageBytes: ByteArray,
        val designParams: WatchFaceDesignParams
    )
    
    data class WatchFaceDesignParams(
        val isAnalog: Boolean,
        val backgroundColors: List<Int>,
        val clockColor: Int,
        val fontStyle: FontStyle,
        val dialStyle: DialStyle,
        val showNumbers: Boolean,
        val backgroundStyle: BackgroundStyle
    )
    
    enum class FontStyle {
        NORMAL, BOLD, LIGHT, MONO, SERIF
    }
    
    enum class DialStyle {
        CLASSIC, MODERN, SWORD, MINIMAL, ORNATE
    }
    
    enum class BackgroundStyle {
        GRADIENT, RADIAL, NATURE, METALLIC, ABSTRACT
    }
    
    companion object {
        private const val TAG = "ImageGenRepository"
    }
}
