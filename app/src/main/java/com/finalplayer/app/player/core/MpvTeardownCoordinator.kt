package com.finalplayer.app.player.core

import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

object MpvTeardownCoordinator {

    private const val TAG = "MpvTeardownCoordinator"

    enum class CoreOwner {
        NONE,
        ACTIVITY,
        DETACHED_SERVICE,
        TEARING_DOWN
    }

    @Volatile
    var currentOwner: CoreOwner = CoreOwner.NONE
        private set

    private val teardownExecutor = Executors.newSingleThreadExecutor()
    private var activeTeardownFuture: Future<*>? = null

    fun markActivityCoreInitialized() {
        currentOwner = CoreOwner.ACTIVITY
        Log.d(TAG, "Core owner set to ACTIVITY")
    }

    fun markDetachedService() {
        currentOwner = CoreOwner.DETACHED_SERVICE
        Log.d(TAG, "Core owner set to DETACHED_SERVICE")
    }

    @Synchronized
    fun destroyActivityCoreAsync(reason: String, mpvView: MPVView?): Boolean {
        if (currentOwner == CoreOwner.TEARING_DOWN) {
            Log.w(TAG, "Teardown already in progress. Reason skipped: $reason")
            return false
        }

        currentOwner = CoreOwner.TEARING_DOWN
        Log.d(TAG, "Starting async teardown. Reason: $reason")

        activeTeardownFuture = teardownExecutor.submit {
            try {
                destroyNativeCore(mpvView)
            } catch (e: Throwable) {
                Log.e(TAG, "Error during native MPV teardown", e)
            } finally {
                currentOwner = CoreOwner.NONE
                Log.d(TAG, "Native MPV teardown complete. Reason: $reason")
            }
        }
        return true
    }

    fun awaitIdle(timeoutMs: Long): Boolean {
        val future = activeTeardownFuture ?: return true
        return try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Timeout or interruption waiting for teardown idle", e)
            false
        }
    }

    private fun destroyNativeCore(view: MPVView?) {
        if (view == null) return
        try {
            Log.d(TAG, "Pausing MPV player before teardown")
            view.pause()

            Log.d(TAG, "Setting VO to null")
            view.setOptionString("vo", "null")

            Log.d(TAG, "Detaching surface")
            view.detachSurface()

            Log.d(TAG, "Sending quit command to MPV")
            view.command(arrayOf("quit"))

            // Allow short window for commands to drain
            Thread.sleep(200)

            Log.d(TAG, "Calling MPV destroy")
            view.destroy()
        } catch (e: Throwable) {
            Log.e(TAG, "Exception in destroyNativeCore", e)
        }
    }
}
