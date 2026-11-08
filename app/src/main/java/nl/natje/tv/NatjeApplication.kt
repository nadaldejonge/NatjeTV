package nl.natje.tv

import android.app.Application
import java.io.File
import java.time.Instant

class NatjeApplication:Application() {
    override fun onCreate() {
        super.onCreate()
        val filters=java.util.WeakHashMap<android.app.Activity,nl.natje.tv.nightmode.ActivityNightFilter>()
        registerActivityLifecycleCallbacks(object:ActivityLifecycleCallbacks {
            override fun onActivityResumed(a:android.app.Activity) { filters.getOrPut(a) { nl.natje.tv.nightmode.ActivityNightFilter(a) }.start() }
            override fun onActivityPaused(a:android.app.Activity) { filters.remove(a)?.stop() }
            override fun onActivityCreated(a:android.app.Activity,b:android.os.Bundle?) {}
            override fun onActivityStarted(a:android.app.Activity) {}
            override fun onActivityStopped(a:android.app.Activity) {}
            override fun onActivitySaveInstanceState(a:android.app.Activity,b:android.os.Bundle) {}
            override fun onActivityDestroyed(a:android.app.Activity) { filters.remove(a)?.stop() }
        })
        val previous=Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread,error ->
            runCatching {
                // Never record exception messages, intents, URIs or account data. One bounded file.
                File(filesDir,"last-crash.txt").writeText(buildString {
                    appendLine(Instant.now().toString()); appendLine(error.javaClass.name)
                    error.stackTrace.take(18).forEach { appendLine("${it.className}.${it.methodName}:${it.lineNumber}") }
                })
            }
            previous?.uncaughtException(thread,error)
        }
    }
}
