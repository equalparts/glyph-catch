package dev.equalparts.glyph_catch.debug

import android.content.Context
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Helper for logging exceptions using the [DebugCaptureManager].
 */
object DebugExceptionTracker {

    private val installed = AtomicBoolean(false)
    private val loggingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    /**
     * Ensures uncaught exceptions in the current thread get logged.
     */
    fun install(context: Context) {
        if (installed.compareAndSet(false, true)) {
            val applicationContext = context.applicationContext
            defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                Log.e(TAG, "Uncaught exception on ${thread.name}", throwable)
                logInternal(
                    context = applicationContext,
                    throwable = throwable,
                    threadName = thread.name,
                    source = SOURCE_THREAD_DEFAULT_HANDLER
                )
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    /**
     * Log a caught exception.
     */
    fun log(
        context: Context,
        throwable: Throwable,
        snapshot: DebugSnapshot = DebugSnapshot.EMPTY,
        source: String? = null
    ) {
        logInternal(
            context = context.applicationContext,
            throwable = throwable,
            snapshot = snapshot,
            source = source
        )
    }

    private fun logInternal(
        context: Context,
        throwable: Throwable,
        snapshot: DebugSnapshot = DebugSnapshot.EMPTY,
        threadName: String? = null,
        source: String? = null
    ) {
        loggingScope.launch {
            runCatching {
                val manager = DebugCaptureManager.shared(context)
                manager.log(EVENT_TYPE, snapshot) {
                    buildJsonObject {
                        put("message", JsonPrimitive(throwable.message ?: "unknown_error"))
                        put("type", JsonPrimitive(throwable::class.qualifiedName ?: throwable::class.java.name))
                        threadName?.let { put("thread", JsonPrimitive(it)) }
                        source?.let { put("source", JsonPrimitive(it)) }
                        put("stacktrace", JsonPrimitive(throwable.stackTraceToString()))
                        throwable.cause?.let { cause ->
                            put("cause", JsonPrimitive(cause::class.qualifiedName ?: cause::class.java.name))
                            cause.message?.let { causeMessage ->
                                put("causeMessage", JsonPrimitive(causeMessage))
                            }
                        }
                    }
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to record debug exception event", error)
            }
        }
    }

    private const val TAG = "DebugExceptionTracker"
    private const val EVENT_TYPE = "exception"
    private const val SOURCE_THREAD_DEFAULT_HANDLER = "thread_default_handler"
}
