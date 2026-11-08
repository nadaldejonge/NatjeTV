package nl.natje.tv.nightmode

import android.app.Activity
import android.os.*
import android.provider.Settings
import android.view.ViewGroup
import android.widget.FrameLayout
import nl.natje.tv.settings.ConfigStore

/** Always filters our own screens, even when Android has not granted a global backend. */
class ActivityNightFilter(private val activity:Activity) {
    private val handler=Handler(Looper.getMainLooper())
    private var view:FilterView?=null
    private val pulse=object:Runnable { override fun run() { refresh(); handler.postDelayed(this,500) } }
    fun start() { handler.removeCallbacksAndMessages(null); handler.post(pulse) }
    private fun refresh() {
        val c=ConfigStore(activity).load()
        val global=(NightService.running && Settings.canDrawOverlays(activity)) || (c.backend=="accessibility" && NightAccessibilityService.current!=null)
        if(global) { view?.let { (it.parent as? ViewGroup)?.removeView(it) }; view=null; return }
        if(!c.nightActive() && view==null)return
        val filter=view ?: FilterView(activity).also { view=it; activity.addContentView(it,FrameLayout.LayoutParams(-1,-1)) }
        filter.warmth=c.warmth; filter.dim=c.dim; filter.strength=1f
        val target=if(c.nightActive())1f else 0f
        if(filter.tag!=target) { filter.tag=target; filter.animate().alpha(target).setDuration(5000).start() }
        filter.invalidate()
    }
    fun stop() { handler.removeCallbacksAndMessages(null); view?.let { (it.parent as? ViewGroup)?.removeView(it) }; view=null }
}
