package com.vibewatch.watchface.service

import android.graphics.BitmapFactory
import android.util.Log
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.vibewatch.watchface.renderer.CustomWatchFaceRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.InputStream

class CustomWatchFaceService : WatchFaceService(), DataClient.OnDataChangedListener {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var dataClient: DataClient
    private var renderer: CustomWatchFaceRenderer? = null
    
    override fun onCreate() {
        super.onCreate()
        dataClient = Wearable.getDataClient(this)
        dataClient.addListener(this)
        
        // Load existing background if available
        serviceScope.launch {
            loadExistingBackground()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        dataClient.removeListener(this)
        serviceScope.cancel()
    }
    
    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        renderer = CustomWatchFaceRenderer(
            context = this,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            complicationSlotsManager = complicationSlotsManager,
            currentUserStyleRepository = currentUserStyleRepository,
            canvasType = CanvasType.HARDWARE
        )
        
        return WatchFace(
            watchFaceType = WatchFaceType.DIGITAL,
            renderer = renderer!!
        )
    }
    
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                if (dataItem.uri.path == WATCH_FACE_PATH) {
                    serviceScope.launch {
                        try {
                            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                            val imageAsset = dataMap.getAsset(KEY_IMAGE)
                            
                            imageAsset?.let { asset ->
                                val bitmap = loadBitmapFromAsset(asset)
                                renderer?.backgroundBitmap = bitmap
                                Log.d(TAG, "Watch face background updated")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading image from data", e)
                        }
                    }
                }
            }
        }
    }
    
    private suspend fun loadExistingBackground() {
        try {
            val dataItems = dataClient.getDataItems(
                android.net.Uri.parse("wear://*$WATCH_FACE_PATH")
            ).await()
            
            if (dataItems.count > 0) {
                val dataItem = dataItems.first()
                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                val imageAsset = dataMap.getAsset(KEY_IMAGE)
                
                imageAsset?.let { asset ->
                    val bitmap = loadBitmapFromAsset(asset)
                    renderer?.backgroundBitmap = bitmap
                    Log.d(TAG, "Loaded existing background")
                }
            }
            
            dataItems.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading existing background", e)
        }
    }
    
    private suspend fun loadBitmapFromAsset(asset: Asset): android.graphics.Bitmap? {
        return try {
            val inputStream: InputStream = dataClient.getFdForAsset(asset).await().inputStream
            BitmapFactory.decodeStream(inputStream).also {
                inputStream.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding bitmap from asset", e)
            null
        }
    }
    
    companion object {
        private const val TAG = "CustomWatchFaceService"
        private const val WATCH_FACE_PATH = "/watch_face_image"
        private const val KEY_IMAGE = "image"
    }
}
