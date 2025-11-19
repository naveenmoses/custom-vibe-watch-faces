package com.vibewatch.watchface.renderer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CustomWatchFaceRenderer(
    private val context: android.content.Context,
    surfaceHolder: android.view.SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<CustomWatchFaceRenderer.CustomSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    FRAME_PERIOD_MS_DEFAULT,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {
    
    private val timePaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }
    
    private val datePaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
        setShadowLayer(2f, 0f, 0f, Color.BLACK)
    }
    
    private val backgroundPaint = Paint().apply {
        color = Color.BLACK
    }
    
    var backgroundBitmap: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }
    
    class CustomSharedAssets : SharedAssets {
        override fun onDestroy() {}
    }
    
    override suspend fun createSharedAssets(): CustomSharedAssets {
        return CustomSharedAssets()
    }
    
    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: CustomSharedAssets
    ) {
        // Draw background
        if (backgroundBitmap != null) {
            val scaledBitmap = Bitmap.createScaledBitmap(
                backgroundBitmap!!,
                bounds.width(),
                bounds.height(),
                true
            )
            canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
            if (scaledBitmap != backgroundBitmap) {
                scaledBitmap.recycle()
            }
        } else {
            canvas.drawRect(bounds, backgroundPaint)
        }
        
        // Draw time
        val timeText = if (renderParameters.drawMode == DrawMode.AMBIENT) {
            zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        }
        
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        
        canvas.drawText(
            timeText,
            centerX,
            centerY,
            timePaint
        )
        
        // Draw date
        val dateText = zonedDateTime.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        canvas.drawText(
            dateText,
            centerX,
            centerY + 40f,
            datePaint
        )
        
        // Draw complications if not in ambient mode
        if (renderParameters.drawMode != DrawMode.AMBIENT) {
            drawComplications(canvas, zonedDateTime)
        }
    }
    
    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: CustomSharedAssets
    ) {
        // Render highlight layer for complications
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)
        
        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
        }
    }
    
    private fun drawComplications(canvas: Canvas, zonedDateTime: ZonedDateTime) {
        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }
    }
    
    companion object {
        private const val TAG = "CustomWatchFaceRenderer"
        private const val FRAME_PERIOD_MS_DEFAULT: Long = 16L
    }
}
