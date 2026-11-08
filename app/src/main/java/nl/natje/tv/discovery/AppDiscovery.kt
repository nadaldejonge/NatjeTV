package nl.natje.tv.discovery

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import nl.natje.tv.settings.Configuration
import java.text.Normalizer

data class TvApp(val packageName: String, val label: String, val component: ComponentName, val lastUpdate:Long=0)
data class FavoriteSlot(val name: String, val aliases: List<String>, val knownPackages: Set<String>)

class AppDiscovery(private val context: Context) {
    fun discover(): List<TvApp> {
        val pm=context.packageManager
        val tv=pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),0)
        val side=pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
        return (tv+side).mapNotNull { info ->
            val a=info.activityInfo ?: return@mapNotNull null
            if(!a.enabled || !a.exported || a.packageName==context.packageName) return@mapNotNull null
            TvApp(a.packageName,info.loadLabel(pm).toString(),ComponentName(a.packageName,a.name),runCatching { pm.getPackageInfo(a.packageName,0).lastUpdateTime }.getOrDefault(0))
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }
    fun launch(app: TvApp): Boolean = try {
        // Re-resolve at launch: package may have been removed since discovery.
        val pm=context.packageManager
        val intent=pm.getLeanbackLaunchIntentForPackage(app.packageName) ?: pm.getLaunchIntentForPackage(app.packageName)
        if(intent==null) false else { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }
    } catch (_: android.content.ActivityNotFoundException) { false } catch (_: SecurityException) { false }
    companion object {
        val slots=listOf(
            FavoriteSlot("NatjeTube",listOf("natjetube"),setOf("nl.natje.natjetube")),
            FavoriteSlot("Natje IPTV",listOf("natjeiptv","natjesiptvplayer"),setOf("nl.natje.iptvplayer")),
            FavoriteSlot("Netflix",listOf("netflix"),setOf("com.netflix.ninja","com.netflix.mediaclient")),
            FavoriteSlot("Videoland",listOf("videoland"),emptySet()),
            FavoriteSlot("NPO Start",listOf("npostart"),emptySet()),
            FavoriteSlot("Prime Video",listOf("primevideo","amazonprimevideo"),setOf("com.amazon.amazonvideo.livingroom")),
            FavoriteSlot("Appwinkel",listOf("googleplaystore","playstore","aptoidetv","amazonappstore"),setOf("com.android.vending","cm.aptoidetv.pt","com.amazon.venezia"))
        )
        private fun normalize(s:String)=Normalizer.normalize(s,Normalizer.Form.NFD).lowercase().filter { it.isLetterOrDigit() }
        fun match(slot:FavoriteSlot, apps:List<TvApp>, config:Configuration):TvApp? {
            config.bindings[slot.name]?.let { binding -> return apps.firstOrNull { it.packageName==binding } }
            return apps.firstOrNull { it.packageName in slot.knownPackages } ?: apps.firstOrNull { app -> slot.aliases.any { normalize(app.label).contains(it) } }
        }
        fun ordered(apps:List<TvApp>, order:List<String>):List<TvApp> = apps.sortedWith(compareBy<TvApp> { order.indexOf(it.packageName).let { i -> if(i<0) Int.MAX_VALUE else i } }.thenBy { it.label.lowercase() })
    }
}
