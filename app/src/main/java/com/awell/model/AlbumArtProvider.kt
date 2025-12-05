package com.awell.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.awell.data.TrackInfo
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class AlbumArtProvider(private val context: Context) {

    companion object {
        private const val TAG = "AlbumArtProvider"
        private const val TEMP_FILE_PREFIX = "album_art_"
        private const val TEMP_FILE_SUFFIX = ".png"
        private const val DEFAULT_MAX_SIZE_DP = 600
        private const val COMPRESS_QUALITY = 100
        private const val DOWNLOAD_TIMEOUT = 10L // 秒
    }

    private var currentTempFile: File? = null
    private var currentArtUri: Uri? = null

    // 图片来源优先级
    private val sourcePriority = listOf(
        Source.MEDIA_METADATA,  // 最高优先级
        Source.SONG_ID,
        Source.ALBUM_ID,
        Source.FILE_EMBEDDED,
        Source.FILE_PATH      // 最低优先级
    )

    private enum class Source {
        MEDIA_METADATA,
        SONG_ID,
        ALBUM_ID,
        FILE_EMBEDDED,
        FILE_PATH
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(DOWNLOAD_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DOWNLOAD_TIMEOUT, TimeUnit.SECONDS)
            .connectionSpecs(listOf(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS))
            .hostnameVerifier { hostname, session -> true }
            .addInterceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
                    )
                    .build()
                chain.proceed(request)
            }
            .build()
    }


    /**
     * 更新当前播放的歌曲，并生成专辑图片临时文件
     * todo 如果保存了文件机器重启会来不及删除图片 需要清理
     * todo add carplay art
     */
    fun updateTrack(trackInfo: TrackInfo): Uri? {
        cleanup()  // 清理前一个资源

        // 按优先级获取专辑图片
        var bitmap: Bitmap? = null
        for (source in sourcePriority) {
            bitmap = when (source) {
                Source.MEDIA_METADATA -> getFromMediaMetadata(trackInfo.metadata)
                Source.SONG_ID -> trackInfo.songId?.let { getFromMediaStoreBySongId(it) }
                Source.ALBUM_ID -> trackInfo.albumId?.let { getFromMediaStoreByAlbumId(it) }
                Source.FILE_EMBEDDED -> trackInfo.fileMusicPath?.let { getFromFileEmbedded(it) }
                Source.FILE_PATH -> trackInfo.imagePath?.let { getFromPath(it) }
            }
            if (bitmap != null) break
        }

        // 保存为临时文件
        return bitmap?.let {
            saveAsTempFile(it, trackInfo.mediaId ?: "temp_")
        }?.also { uri ->
            currentArtUri = uri
        }
    }

    fun getCurrentArtUri(): Uri? = currentArtUri

    fun cleanup() {
        currentTempFile?.let { file ->
            if (file.exists()) file.delete()
        }
        currentTempFile = null
        currentArtUri = null
    }

    //
    private fun getFromMediaMetadata(metadata: MediaMetadata?): Bitmap? {
        if (metadata == null) return null

        // 1. 优先尝试从URI下载
        val uriBitmap = tryDownloadFromMetadata(metadata)
        if (uriBitmap != null) {
            Log.i(TAG, "getFromMediaMetadata: huang use uri bitmap=>")
            return uriBitmap
        }

        // 2. 如果URI下载失败，尝试直接获取Bitmap
        return getBitmapFromMetadata(metadata)

    }

    /**
     * 尝试从MediaMetadata中的URI下载专辑图片
     */
    private fun tryDownloadFromMetadata(metadata: MediaMetadata): Bitmap? {
        // 获取可能的URI键（按优先级）
        val uriKeys = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
        )

        // 查找第一个有效的URI
        val uriString = uriKeys.firstNotNullOfOrNull { key ->
            metadata.getString(key)?.takeIf { it.isNotBlank() }
        } ?: return null

        return try {
            Log.i(TAG, "tryDownloadFromMetadata: huang download uri=${uriString}")
            // 使用OkHttp下载
            downloadWithOkHttp(uriString)?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } ?: downloadWithHttpUrlConnection(uriString)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to download album art from URI: $uriString")
            null
        }
    }

    /**
     * 使用原生HttpURLConnection下载（备选方案）
     */
    private fun downloadWithHttpUrlConnection(urlString: String): Bitmap? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = DOWNLOAD_TIMEOUT.toInt() * 1000
            connection.readTimeout = DOWNLOAD_TIMEOUT.toInt() * 1000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "HTTP error: ${connection.responseCode} - $urlString")
                return null
            }

            connection.inputStream.use { inputStream ->
                return BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download with HttpURLConnection: $urlString", e)
            return null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * 使用OkHttp下载图片
     */
    private fun downloadWithOkHttp(url: String): ByteArray? {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            ) // 避免被某些服务器拒绝
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Download failed: HTTP ${response.code()} - $url")
                return null
            }
            return response.body()?.bytes()
        }
    }

    /**
     * 直接从MediaMetadata获取Bitmap
     */
    private fun getBitmapFromMetadata(metadata: MediaMetadata): Bitmap? {
        // 尝试的Bitmap键（按优先级）
        val bitmapKeys = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART,
        )

        return bitmapKeys.firstNotNullOfOrNull { key ->
            metadata.getBitmap(key)?.takeIf {
                Log.i(TAG, "getBitmapFromMetadata: huang use metadata bitmap=>")
                !it.isRecycled
            }
        }
    }

    // 通过歌曲ID获取
    private fun getFromMediaStoreBySongId(songId: Long): Bitmap? {
        // 添加ID有效性检查
        if (songId <= 0) {
            Log.w(TAG, "Invalid songId: $songId")
            return null
        }
        val uri = "content://media/external/audio/media/$songId/albumart".toUri()
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get album art by songId: $songId")
            null
        }
    }

    // 通过专辑ID获取
    private fun getFromMediaStoreByAlbumId(albumId: Long): Bitmap? {
        // 添加ID有效性检查
        if (albumId <= 0) {
            Log.w(TAG, "Invalid albumId: $albumId")
            return null
        }
        val uri = "content://media/external/audio/albumart/$albumId".toUri()
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                Log.i(TAG, "getFromMediaStoreByAlbumId: huang use album id bitmap=>")
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get album art by albumId: $albumId")
            null
        }
    }

    // 从文件内嵌数据获取
    private fun getFromFileEmbedded(filePath: String): Bitmap? {
        return try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(filePath)
                retriever.embeddedPicture?.let { bytes ->
                    Log.i(TAG, "getFromFileEmbedded: huang use file bitmap=>")
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get embedded album art from: $filePath")
            null
        }
    }

    inline fun <T> MediaMetadataRetriever.use(block: (MediaMetadataRetriever) -> T): T {
        return try {
            block(this)
        } finally {
            release() // 在 finally 中释放资源
        }
    }

    private fun getFromPath(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            Log.w(TAG, "getFromPath: file to get album art from: $path")
            null
        }
    }


    // 保存为临时文件
    private fun saveAsTempFile(bitmap: Bitmap, mediaId: String): Uri? {
        return try {
            // 创建临时文件
            val tempFile = File.createTempFile(
                "$TEMP_FILE_PREFIX$mediaId",
                TEMP_FILE_SUFFIX,
                context.cacheDir
            ).apply {
                deleteOnExit()
                currentTempFile = this
            }

            // 优化图片
            val optimized = optimizeBitmap(bitmap)

            // 保存到文件
            FileOutputStream(tempFile).use { out ->
                optimized.compress(Bitmap.CompressFormat.PNG, COMPRESS_QUALITY, out)
            }
            Log.i(TAG, "saveAsTempFile: huang save image path=>" + tempFile.absoluteFile)
            // 生成安全URI
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.mediaprovider",
                tempFile
            )
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create temp album art file")
            null
        }
    }

    // 优化图片尺寸
    private fun optimizeBitmap(original: Bitmap): Bitmap {
        val maxSizePx = (DEFAULT_MAX_SIZE_DP * context.resources.displayMetrics.density).toInt()

        // 无需调整的情况
        if (original.width <= maxSizePx && original.height <= maxSizePx) {
            return original
        }

        // 计算缩放比例
        val ratio = maxSizePx.toFloat() / maxOf(original.width, original.height)
        val newWidth = (original.width * ratio).toInt()
        val newHeight = (original.height * ratio).toInt()

        return original.scale(newWidth, newHeight)
    }
}