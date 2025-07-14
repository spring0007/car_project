package com.awell.model

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MediaViewModel : ViewModel() {

    private val _mediaState = MutableLiveData<MediaDataSelect>()
    val mediaState: LiveData<MediaDataSelect> get() = _mediaState

    private val _playStatus = MutableLiveData<PlayStatus>()
    val playStatus: LiveData<PlayStatus> get() = _playStatus

    private val _playInfo = MutableLiveData<PlayInfo>()
    val playInfo: LiveData<PlayInfo> get() = _playInfo

    private val _playTime = MutableLiveData<PlayTime>()
    val playTime: LiveData<PlayTime> get() = _playTime

    private val _playImage = MutableLiveData<PlayImage>()
    val playImage: LiveData<PlayImage> get() = _playImage

    private val _radioInfo = MutableLiveData<RadioInfo>()
    val radioInfo: LiveData<RadioInfo> get() = _radioInfo

    fun updateRadioInfo(bundle: Bundle, freq: String, unit: String, radioType: String) {
        _radioInfo.value =
            RadioInfo(bundle = bundle, freq = freq, unit = unit, radioType = radioType)
    }

    fun updatePlayImage(bundle: Bundle, songId: Long, albumId: Long) {
        _playImage.value = PlayImage(bundle = bundle, songId = songId, albumId = albumId)
    }

    fun updatePlayTime(bundle: Bundle, currentTime: Long, totalTime: Long, playType: Int) {
        _playTime.value = PlayTime(
            bundle = bundle, currentTime = currentTime, totalTime = totalTime, playType = playType
        )
    }


    fun updatePlayInfo(
        bundle: Bundle, songName: String, singerName: String, album: String, appType: Int
    ) {
        _playInfo.value = PlayInfo(
            bundle = bundle,
            songName = songName,
            singerName = singerName,
            album = album,
            appType = appType
        )
    }


    fun updatePlayStatus(bundle: Bundle, status: Boolean, appType: Int) {
        _playStatus.value = PlayStatus(bundle = bundle, status = status, playAppType = appType)
    }

    fun updateMediaState(
        bundle: Bundle, packName: String, status: String, mediaType: Int, curMedia: Int
    ) {
        _mediaState.value = MediaDataSelect(
            bundle = bundle,
            packName = packName,
            status = status,
            mediaType = mediaType,
            curMedia = curMedia
        )
    }
}

data class RadioInfo(
    val bundle: Bundle, val radioType: String, val freq: String, val unit: String
)

data class PlayImage(
    val bundle: Bundle, val songId: Long, val albumId: Long
)

data class PlayTime(
    val bundle: Bundle, val currentTime: Long, val totalTime: Long, val playType: Int
)

data class PlayInfo(
    val bundle: Bundle,
    val songName: String,
    val singerName: String,
    val album: String,
    val appType: Int
)

data class PlayStatus(
    val bundle: Bundle, val status: Boolean, val playAppType: Int
)

data class MediaDataSelect(
    val bundle: Bundle,
    val packName: String,
    val status: String,
    val mediaType: Int,
    val curMedia: Int
)