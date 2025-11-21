package com.vibewatch.companion.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.vibewatch.companion.data.ImageGenerationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class WatchFaceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ImageGenerationRepository(application)
    private val dataClient: DataClient = Wearable.getDataClient(application)
    private val sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _generatedImage = MutableStateFlow<Bitmap?>(null)
    val generatedImage: StateFlow<Bitmap?> = _generatedImage.asStateFlow()
    
    fun getApiKey(): String? {
        return sharedPreferences.getString(KEY_API_KEY, null)
    }
    
    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit().putString(KEY_API_KEY, apiKey).apply()
    }
    
    fun generateWatchFace(prompt: String) {
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            _uiState.value = UiState.Error("Please set your Gemini API key in settings")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                val result = repository.generateWatchFaceImage(prompt, apiKey)
                
                result.onSuccess { watchFaceData ->
                    val bitmap = BitmapFactory.decodeByteArray(
                        watchFaceData.imageBytes, 
                        0, 
                        watchFaceData.imageBytes.size
                    )
                    _generatedImage.value = bitmap
                    
                    val watchType = if (watchFaceData.designParams.isAnalog) "analog" else "digital"
                    _uiState.value = UiState.Success(
                        "Watch face generated successfully! ($watchType style)"
                    )
                }.onFailure { error ->
                    Log.e(TAG, "Generation failed", error)
                    _uiState.value = UiState.Error("Failed to generate image: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                _uiState.value = UiState.Error("Unexpected error: ${e.message}")
            }
        }
    }
    
    fun syncToWatch() {
        val bitmap = _generatedImage.value
        if (bitmap == null) {
            _uiState.value = UiState.Error("No image to sync. Generate a watch face first.")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                val imageAsset = createAssetFromBitmap(bitmap)
                
                val putDataMapRequest = PutDataMapRequest.create(WATCH_FACE_PATH).apply {
                    dataMap.putAsset(KEY_IMAGE, imageAsset)
                    dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                }
                
                val putDataRequest = putDataMapRequest.asPutDataRequest()
                putDataRequest.setUrgent()
                
                dataClient.putDataItem(putDataRequest).await()
                
                _uiState.value = UiState.Success("Watch face synced to watch successfully!")
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
                _uiState.value = UiState.Error("Failed to sync: ${e.message}")
            }
        }
    }
    
    private fun createAssetFromBitmap(bitmap: Bitmap): Asset {
        val byteStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream)
        return Asset.createFromBytes(byteStream.toByteArray())
    }
    
    fun clearError() {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.Idle
        }
    }
    
    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }
    
    companion object {
        private const val TAG = "WatchFaceViewModel"
        private const val PREFS_NAME = "watch_face_prefs"
        private const val KEY_API_KEY = "gemini_api_key"
        private const val WATCH_FACE_PATH = "/watch_face_image"
        private const val KEY_IMAGE = "image"
        private const val KEY_TIMESTAMP = "timestamp"
    }
}
