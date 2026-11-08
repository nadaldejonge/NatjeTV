package nl.natje.tv.discovery

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Uses only an installed market handler. The store chooses the right TV/device variant. */
object StoreAccess {
    fun marketIntent(query:String?=null):Intent = Intent(Intent.ACTION_VIEW,Uri.Builder().scheme("market").authority("search").appendQueryParameter("q",query ?: "Android TV").appendQueryParameter("c","apps").build())
    fun resolve(context:Context,query:String?=null):Intent? {
        val base=marketIntent(query)
        val handlers=context.packageManager.queryIntentActivities(base,0).filter { it.activityInfo?.let { a -> a.enabled && a.exported }==true }
        val chosen=handlers.firstOrNull { it.activityInfo.packageName=="com.android.vending" } ?: handlers.firstOrNull() ?: return null
        return base.setComponent(ComponentName(chosen.activityInfo.packageName,chosen.activityInfo.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    fun app(context:Context):TvApp? {
        val component=resolve(context)?.component ?: return null
        val pm=context.packageManager
        return runCatching { val info=pm.getApplicationInfo(component.packageName,0); TvApp(info.packageName,pm.getApplicationLabel(info).toString(),component,pm.getPackageInfo(info.packageName,0).lastUpdateTime) }.getOrNull()
    }
    fun open(context:Context,query:String?=null,fallback:TvApp?=null):Boolean = try {
        val intent=resolve(context,query)
        if(intent!=null) { context.startActivity(intent); true }
        else if(fallback!=null) AppDiscovery(context).launch(fallback) else false
    } catch(_:android.content.ActivityNotFoundException) { false } catch(_:SecurityException) { false }
}
