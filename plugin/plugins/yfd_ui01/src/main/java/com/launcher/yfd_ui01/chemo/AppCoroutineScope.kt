package com.launcher.yfd_ui01.chemo

import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineScope

/**
 * 简单的协程作用域管理器，不需要 lifecycle 依赖
 */
class AppCoroutineScope {
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /**
     * 启动协程
     */
    fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        return scope.launch { block() }
    }
    
    /**
     * 取消所有协程
     */
    fun cancelAll() {
        scope.cancel()
    }
    
    /**
     * 在 IO 线程执行并返回结果到主线程
     */
    suspend fun <T> io(block: suspend CoroutineScope.() -> T): T {
        return withContext(Dispatchers.IO) { block() }
    }
    
    /**
     * 在主线程执行
     */
    suspend fun <T> main(block: suspend CoroutineScope.() -> T): T {
        return withContext(Dispatchers.Main) { block() }
    }
}