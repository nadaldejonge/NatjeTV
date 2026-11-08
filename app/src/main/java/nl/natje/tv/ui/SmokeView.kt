package nl.natje.tv.ui
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
/** Local MIT-licensed GPU smoke; keyboard focus belongs exclusively to native controls. */
class SmokeView(context:Context):FrameLayout(context) {
    private var web:WebView?=null
    var running=false
        set(value) {
            field=value
            web?.let { if(value)it.onResume() else it.onPause(); it.evaluateJavascript("if(window.smoke)window.smoke.${if(value)"resume" else "pause"}();",null) }
        }
    init {
        isFocusable=false; descendantFocusability=ViewGroup.FOCUS_BLOCK_DESCENDANTS
        importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        setBackgroundColor(Color.BLACK)
        postDelayed({
            if(!isAttachedToWindow)return@postDelayed
            web=WebView(context).apply {
                setBackgroundColor(Color.BLACK); isFocusable=false; isFocusableInTouchMode=false; isClickable=false
                settings.javaScriptEnabled=true; settings.domStorageEnabled=false
                settings.blockNetworkLoads=true; settings.allowContentAccess=false
                webChromeClient=object:WebChromeClient() { override fun onConsoleMessage(message:ConsoleMessage):Boolean { android.util.Log.d("NatjeSmoke",message.message()); return true } }
                webViewClient=object:WebViewClient() {
                    override fun onPageFinished(view:WebView,url:String) { if(!running)view.evaluateJavascript("if(window.smoke)window.smoke.pause();",null) }
                }
                loadUrl("file:///android_asset/smoke/index.html")
            }
            addView(web,LayoutParams(-1,-1))
        },350)
    }
    override fun onDetachedFromWindow() { web?.stopLoading(); web?.destroy(); web=null; super.onDetachedFromWindow() }
}
