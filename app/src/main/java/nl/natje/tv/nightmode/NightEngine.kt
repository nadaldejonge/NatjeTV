package nl.natje.tv.nightmode

import android.animation.ValueAnimator
import android.app.*
import android.content.*
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.view.*
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import nl.natje.tv.R
import nl.natje.tv.settings.ConfigStore
import nl.natje.tv.settings.Configuration
import nl.natje.tv.settings.SettingsActivity
import java.time.LocalTime
import kotlin.math.roundToInt

/** Parameter parity with NatjeTube 607a15a: amber #FFAB50, maximum blend .60; dim 0..80%. */
object FilterMath {
    fun warmthAlpha(value:Int, strength:Float=1f) = (value.coerceIn(0,100) / 100f * .60f * strength.coerceIn(0f,1f))
    fun dimAlpha(value:Int, strength:Float=1f) = value.coerceIn(0,80) / 100f * strength.coerceIn(0f,1f)
}

class FilterView(context:Context) : View(context) {
    var warmth=45
    var dim=30
    var strength=0f
        set(value) { field=value; invalidate() }
    init { importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO; isFocusable=false }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.argb((FilterMath.warmthAlpha(warmth,strength)*255).roundToInt(),255,171,80))
        canvas.drawColor(Color.argb((FilterMath.dimAlpha(dim,strength)*255).roundToInt(),0,0,0))
    }
}

/** Renders only on change/during the five second fade; never captures content. */
class OverlayHost(private val context:Context, private val type:Int) {
    private val wm=context.getSystemService(WindowManager::class.java)
    private var view:FilterView?=null
    private var animator:ValueAnimator?=null
    private var target:Triple<Boolean,Int,Int>?=null
    fun apply(c:Configuration, immediate:Boolean=false) {
        val active=c.nightActive()
        val next=Triple(active,c.warmth,c.dim)
        if(target==next && !immediate) return
        target=next
        NightController.status=if(active) "Filtervenster aangevraagd · video nog te testen" else if(c.nightMode=="schedule") "Uit · buiten het tijdschema" else "Uit"
        if(!active && view==null) return
        try {
            val filter=view ?: FilterView(context).also {
                val params=WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT)
                params.gravity=Gravity.TOP or Gravity.START
                // Full window alpha preserves NatjeTube's actual .60/.80 pixel blend. This TV-only
                // surface is non-focusable and never captures D-pad input. Android 12+ may restrict
                // touchscreen passthrough above its obscuring-opacity threshold; no bypass is used.
                params.alpha=1f
                wm.addView(it,params); view=it
            }
            val oldWarmth=filter.warmth; val oldDim=filter.dim
            filter.warmth=c.warmth; filter.dim=c.dim
            animator?.cancel()
            if(immediate) { filter.strength=if(active)1f else 0f; if(!active) close() }
            else {
                val initial=filter.strength
                animator=ValueAnimator.ofFloat(0f,1f).apply {
                    duration=5000
                    addUpdateListener {
                        val f=it.animatedValue as Float
                        filter.warmth=(oldWarmth+(c.warmth-oldWarmth)*f).toInt()
                        filter.dim=(oldDim+(c.dim-oldDim)*f).toInt()
                        filter.strength=initial+((if(active)1f else 0f)-initial)*f
                        if(f==1f && !active) close()
                    }
                    start()
                }
            }
            if(active) NightController.status="Filtervenster actief · zichtbaarheid boven video nog niet bewezen"
        } catch(_:SecurityException) { close(); NightController.status="Overlay niet toegestaan; controleer toestemming" }
          catch(_:WindowManager.BadTokenException) { close(); NightController.status="Overlay geweigerd door Android" }
    }
    fun close() {
        animator?.cancel(); animator=null
        view?.let { runCatching { wm.removeViewImmediate(it) } }; view=null; target=null
    }
}

object NightController {
    @Volatile var status="Uit · nog niet op echte stick getest"
    fun refresh(context:Context) {
        val c=ConfigStore(context).load()
        NightAccessibilityService.current?.refresh()
        if(c.backend=="accessibility" && (NightAccessibilityService.current!=null || !Settings.canDrawOverlays(context))) {
            context.stopService(Intent(context,NightService::class.java))
            status=if(NightAccessibilityService.current==null) "Toegankelijkheidsservice niet ingeschakeld" else "Toegankelijkheidsfilter geselecteerd · video nog te testen"
            return
        }
        if(c.nightMode=="off") {
            // If running, let the service perform the fade before stopping.
            if(NightService.running) context.startService(Intent(context,NightService::class.java))
            else status="Uit"
            return
        }
        if(!Settings.canDrawOverlays(context)) { status="Toestemming voor schermoverlay ontbreekt"; return }
        try { context.startForegroundService(Intent(context,NightService::class.java)) }
        catch(_:RuntimeException) { status="Android blokkeert achtergrondstart; open NatjeNightMode opnieuw" }
    }
    fun emergencyOff(context:Context) {
        ConfigStore(context).update { it.copy(nightMode="off") }
        NightService.emergency?.invoke()
        NightAccessibilityService.current?.emergency()
        status="Noodstop: filter direct uitgeschakeld"
    }
}

class NightService:Service() {
    private val handler=Handler(Looper.getMainLooper())
    private lateinit var host:OverlayHost
    private val prefsListener=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,_ -> tick() }
    private val pulse=object:Runnable { override fun run() { tick(); handler.postDelayed(this,30000) } }
    override fun onCreate() {
        super.onCreate(); running=true
        host=OverlayHost(this,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        val nm=getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("night","NatjeNightMode",NotificationManager.IMPORTANCE_LOW))
        startForeground(12,notification())
        getSharedPreferences("natje_tv",MODE_PRIVATE).registerOnSharedPreferenceChangeListener(prefsListener)
        emergency={ host.close(); stopSelf() }
        handler.post(pulse)
    }
    private fun notification():Notification {
        val pi=PendingIntent.getActivity(this,0,Intent(this,SettingsActivity::class.java).putExtra("section","night"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val off=PendingIntent.getService(this,1,Intent(this,NightService::class.java).setAction("EMERGENCY_OFF"),PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this,"night").setSmallIcon(R.mipmap.ic_launcher).setContentTitle("NatjeNightMode")
            .setContentText("Schermfilter / tijdschema actief · instellingen en uitknop")
            .setContentIntent(pi).setOngoing(true).addAction(Notification.Action.Builder(null,"Direct uit",off).build()).build()
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action=="EMERGENCY_OFF") { NightController.emergencyOff(this); return START_NOT_STICKY }
        tick(); return START_STICKY
    }
    private fun tick() {
        val c=ConfigStore(this).load()
        if((c.backend=="accessibility" && NightAccessibilityService.current!=null) || !Settings.canDrawOverlays(this)) { host.close(); stopSelf(); return }
        host.apply(c)
        if(c.nightMode=="off") { handler.postDelayed({ if(ConfigStore(this).load().nightMode=="off") stopSelf() },5100) }
    }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); host.close(); getSharedPreferences("natje_tv",MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefsListener); running=false; emergency=null; super.onDestroy() }
    override fun onBind(intent:Intent?):IBinder?=null
    companion object { @Volatile var running=false; var emergency:(()->Unit)?=null }
}

/** User-enabled HOME recovery and optional filter; no screen text or node tree access. */
class NightAccessibilityService:AccessibilityService() {
    private var lastHomeRedirect=0L
    private lateinit var host:OverlayHost
    private val handler=Handler(Looper.getMainLooper())
    private lateinit var remote:RemoteChord
    private val pulse=object:Runnable { override fun run() { refresh(); handler.postDelayed(this,30000) } }
    override fun onServiceConnected() {
        current=this; host=OverlayHost(this,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY); remote=RemoteChord(this)
        NightController.refresh(this); handler.post(pulse)
    }
    fun refresh() {
        if(!::host.isInitialized) return
        val c=ConfigStore(this).load()
        if(c.backend=="accessibility") host.apply(c) else host.close()
    }
    fun emergency() { if(::host.isInitialized) host.close() }
    override fun onAccessibilityEvent(event:AccessibilityEvent?) {
        refresh()
        if(event?.eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.packageName?.toString()=="com.google.android.apps.tv.launcherx") {
            val now=SystemClock.elapsedRealtime()
            if(now-lastHomeRedirect>800) {
                lastHomeRedirect=now
                runCatching { startActivity(Intent(this,nl.natje.tv.launcher.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)) }
            }
        }
    }
    override fun onKeyEvent(event:KeyEvent):Boolean { if(::remote.isInitialized) remote.onKey(event); return false }
    override fun onInterrupt() { emergency(); NightController.status="Toegankelijkheidsfilter onderbroken" }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); if(::remote.isInitialized) remote.close(); emergency(); current=null; super.onDestroy() }
    companion object { var current:NightAccessibilityService?=null }
}

class BootReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        // No activity launch from a boot receiver. Foreground-service policy may require opening launcher.
        NightController.refresh(context)
        if(intent.action==Intent.ACTION_BOOT_COMPLETED) context.getSharedPreferences("boot",Context.MODE_PRIVATE).edit().putBoolean("pendingAutoStart",true).apply()
    }
}

/** Observes only key codes. Never consumes Back/Home; emergency works with a timed hold. */
class RemoteChord(private val context:Context) {
    private val pressed=mutableSetOf<Int>()
    private val handler=Handler(Looper.getMainLooper())
    private var emergencyArmed=false
    private var toggled=false
    private val reset=Runnable { if(emergencyArmed) NightController.emergencyOff(context) }
    fun onKey(e:KeyEvent) {
        if(e.action==KeyEvent.ACTION_DOWN) {
            pressed.add(e.keyCode)
            if(e.repeatCount==0 && e.keyCode !in ConfigStore(context).load().observedKeys) ConfigStore(context).update { it.copy(observedKeys=it.observedKeys+e.keyCode) }
        } else if(e.action==KeyEvent.ACTION_UP) { pressed.remove(e.keyCode); toggled=false }
        val emergency=KeyEvent.KEYCODE_BACK in pressed && KeyEvent.KEYCODE_DPAD_DOWN in pressed
        if(emergency && !emergencyArmed) { emergencyArmed=true; handler.postDelayed(reset,3000) }
        if(!emergency) { emergencyArmed=false; handler.removeCallbacks(reset) }
        val c=ConfigStore(context).load()
        val direction=when(c.chord) { "menu_down"->KeyEvent.KEYCODE_DPAD_DOWN; "menu_up"->KeyEvent.KEYCODE_DPAD_UP; else->-1 }
        if(!toggled && KeyEvent.KEYCODE_MENU in pressed && direction in pressed) {
            toggled=true; ConfigStore(context).update { it.copy(nightMode=if(it.nightActive())"off" else "on") }; NightController.refresh(context)
        }
    }
    fun close() { pressed.clear(); handler.removeCallbacksAndMessages(null) }
}
