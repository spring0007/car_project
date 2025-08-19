package com.awell.data

import android.media.MediaMetadata
import android.os.Parcel
import android.os.Parcelable

data class TrackInfo(
    val mediaId: String? = null,         // 唯一媒体ID
    val songId: Long? = null,            // 媒体库歌曲ID
    val albumId: Long? = null,           // 媒体库专辑ID
    val fileMusicPath: String? = null,        // 文件路径
    val metadata: MediaMetadata? = null,  // MediaMetadata 对象
    val imagePath: String? = null  // 图片路径
) : Parcelable {
    constructor(parcel: Parcel) : this(
        mediaId = parcel.readString(),
        songId = parcel.readValue(Long::class.java.classLoader) as? Long,
        albumId = parcel.readValue(Long::class.java.classLoader) as? Long,
        fileMusicPath = parcel.readString(),
        metadata = parcel.readParcelable(MediaMetadata::class.java.classLoader),
        imagePath = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(mediaId)
        parcel.writeValue(songId)
        parcel.writeValue(albumId)
        parcel.writeString(fileMusicPath)
        parcel.writeParcelable(metadata, flags)
        parcel.writeString(imagePath)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TrackInfo> {
        override fun createFromParcel(parcel: Parcel): TrackInfo {
            return TrackInfo(parcel)
        }

        override fun newArray(size: Int): Array<TrackInfo?> {
            return arrayOfNulls(size)
        }
    }
}