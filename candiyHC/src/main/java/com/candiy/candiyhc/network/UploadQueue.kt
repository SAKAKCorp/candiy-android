package com.candiy.candiyhc.network


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

object UploadQueue {
    private val queue = ConcurrentLinkedQueue<suspend () -> Unit>()
    private var isRunning = false

    fun enqueue(task: suspend () -> Unit) {
        queue.add(task)
        processQueue()
    }

    private fun processQueue() {
        if (isRunning) return
        isRunning = true

        CoroutineScope(Dispatchers.IO).launch {
            while (queue.isNotEmpty()) {
                val task = queue.poll()
                try {
                    task?.invoke()
                } catch (e: Exception) {
                    // 로그 출력 또는 재시도 전략 적용 가능
                }
            }
            isRunning = false
        }
    }
}