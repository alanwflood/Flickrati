package com.example.flickrati.utils

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.util.LruCache
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.example.flickrati.api.FlickrFetch
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "ImageDownloader"
private const val MESSAGE_DOWNLOAD = 0

class ImageDownloader<in T>(
    private val responseHandler: Handler,
    private val onImageDownloaded: (T, Bitmap) -> Unit
) : HandlerThread(TAG) {
    private var hasQuit = false
    private lateinit var requestHandler: Handler
    private val requestMap = ConcurrentHashMap<T, String>()
    private val flickrFetch = FlickrFetch()
    private lateinit var memoryCache: LruCache<String, Bitmap>

    val fragmentLifecycle = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun setup() {
            Log.i(TAG, "Starting background thread")
            // Get max available VM memory, exceeding this amount will throw an
            // OutOfMemory exception. Stored in kilobytes as LruCache takes an
            // int in its constructor.
            val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()

            // Use 1/8th of the available memory for this memory cache.
            val cacheSize = maxMemory / 8

            memoryCache = object : LruCache<String, Bitmap>(cacheSize) {

                override fun sizeOf(key: String, bitmap: Bitmap): Int {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.byteCount / 1024
                }
            }

            start()
            looper
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun teardown() {
            Log.i(TAG, "Destroying background thread")
            quit()
        }
    }

    val viewLifecycleObserver: LifecycleObserver = object: LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun clearQueue() {
            Log.i(TAG, "Clearing all requests from queue")
            requestHandler.removeMessages(MESSAGE_DOWNLOAD)
            requestMap.clear()
        }
    }

    @Suppress("UNCHECKED_CAST")
    @SuppressLint("HandlerLeak")
    override fun onLooperPrepared() {

        requestHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    val target = msg.obj as T
                    Log.i(TAG, "Got a request for URL: ${requestMap[target]}")
                    handleRequest(target)
                }
            }
        }
    }

    override fun quit(): Boolean {
        hasQuit = true
        return super.quit()
    }

    fun queueImage(target: T, url: String) {
        Log.i(TAG, "Got a URL: $url")
        requestMap[target] = url

        requestHandler
            .obtainMessage(MESSAGE_DOWNLOAD, target)
            .sendToTarget()
    }

    private fun handleRequest(target: T) {
        val url = requestMap[target] ?: return

        val bitmap  = memoryCache[url]
            ?: flickrFetch.fetchPhoto(url)?.apply {
                cacheImage(url, this)
            }
            ?: return


        responseHandler.post(Runnable {
            if (requestMap[target] != url || hasQuit) {
                return@Runnable
            }
            requestMap.remove(target)
            onImageDownloaded(target, bitmap)
        })
    }

    private fun cacheImage(url: String, bitmap: Bitmap) {

        memoryCache.put(url, bitmap)
    }
}