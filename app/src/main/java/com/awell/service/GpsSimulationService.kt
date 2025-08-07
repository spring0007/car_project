package com.awell.service
import android.app.Service
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import kotlin.math.cos
import kotlin.math.sin

class GpsSimulationService : Service() {

    private val tag = "GpsSimulationService"
    private lateinit var locationManager: LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private var simulationActive = false

    // 模拟参数配置
    private val updateInterval = 1000L // 位置更新间隔 (ms)
    private var baseLatitude = 37.7749 // 初始纬度 (旧金山)
    private var baseLongitude = -122.4194 // 初始经度
    private var speedMps = 10.0 // 初始速度 (米/秒)
    private var direction = 0.0 // 方向 (度，0=正北)
    private val speedIncrement = 10 // 每次速度变化量
    private val directionIncrement = 30.0 // 方向变化量

    private val simulationRunnable = object : Runnable {
        override fun run() {
            if (simulationActive) {
                updateGpsPosition()
                handler.postDelayed(this, updateInterval)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        startSimulation()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startSimulation() {
        try {
            // 检查测试提供者是否已存在
            if (locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
            }

            // 兼容新旧版本的 addTestProvider 方法
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ 使用新API
                val properties = ProviderProperties.Builder()
                    .setHasAltitudeSupport(true)
                    .setHasSpeedSupport(true)
                    .setHasBearingSupport(true)
                    .setAccuracy(Criteria.ACCURACY_FINE)
                    .setPowerUsage(Criteria.POWER_HIGH)
                    .build()

                locationManager.addTestProvider(
                    LocationManager.GPS_PROVIDER,
                    properties,
                    emptySet<String>() // 空属性集合
                )
            } else {
                // 旧版本API (Android 10及以下)
                locationManager.addTestProvider(
                    LocationManager.GPS_PROVIDER,
                    false,  // 需要网络
                    false,  // 需要卫星
                    false,  // 需要蜂窝
                    true,   // 支持高度
                    true,   // 支持速度
                    true,   // 支持方位
                    true,   // 支持精度
                    Criteria.POWER_HIGH,
                    Criteria.ACCURACY_FINE
                )
            }

            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
            simulationActive = true
            handler.post(simulationRunnable)
            Log.d(tag, "GPS 模拟已启动")
        } catch (e: SecurityException) {
            Log.e(tag, "系统权限不足: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "初始化失败: ${e.message}")
        }
    }

    private fun updateGpsPosition() {
        try {
            // 更新位置参数 (简单圆周运动)
            direction = (direction + directionIncrement) % 360
            speedMps += speedIncrement

            // 计算新位置 (简化的圆周运动模型)
            val radius = 30.5 // 移动半径 (度)
            val radian = Math.toRadians(direction)
            val newLat = baseLatitude + radius * cos(radian)
            val newLon = baseLongitude + radius * sin(radian)

            // 创建模拟位置
            val mockLocation = Location(LocationManager.GPS_PROVIDER).apply {
                latitude = newLat
                longitude = newLon
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                accuracy = 1.0f  // 高精度
                bearing = direction.toFloat()
                speed = speedMps.toFloat()
                altitude = 50.0  // 海拔高度
            }

            // 注入位置
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLocation)
            Log.v(tag, "注入位置: $newLat, $newLon | 速度: ${"%.1f".format(speedMps)} m/s")
        } catch (e: Exception) {
            Log.e(tag, "位置更新失败: ${e.message}")
        }
    }

    private fun stopSimulation() {
        simulationActive = false
        handler.removeCallbacks(simulationRunnable)
        try {
            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false)
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
            Log.d(tag, "GPS 模拟已停止")
        } catch (e: Exception) {
            Log.e(tag, "清理失败: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSimulation()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}