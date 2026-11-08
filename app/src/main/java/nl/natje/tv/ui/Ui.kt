package nl.natje.tv.ui

import android.app.Activity
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.service.dreams.DreamService
import android.view.*
import android.widget.*
import nl.natje.tv.R
import nl.natje.tv.settings.ConfigStore
import nl.natje.tv.settings.Configuration
import java.time.LocalTime
import java.util.concurrent.Executors

fun Context.dp(n:Int)=(n*resources.displayMetrics.density+.5f).toInt()
fun Context.dp(n:Float)=(n*resources.displayMetrics.density+.5f).toInt()
fun Activity.fullscreen() {
    window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
}
fun surface(color:Int=Color.rgb(24,26,31), focused:Boolean=false)=GradientDrawable().apply {
    shape=GradientDrawable.RECTANGLE; cornerRadius=22f*android.content.res.Resources.getSystem().displayMetrics.density
    setColor(color); setStroke(if(focused)3 else 1,if(focused)0xfff4f6fa.toInt() else 0x35d8dce3)
}
fun Context.label(value:String,size:Float=16f,color:Int=Color.WHITE)=TextView(this).apply {
    text=value; textSize=size; setTextColor(color); typeface=Typeface.create("sans-serif",Typeface.NORMAL)
}
fun Context.action(value:String, click:()->Unit)=Button(this).apply {
    text=value; isAllCaps=false; textSize=15f; minHeight=dp(48); setTextColor(Color.WHITE)
    background=surface(); setPadding(dp(16),dp(8),dp(16),dp(8)); isFocusable=true
    setOnClickListener { click() }
    setOnFocusChangeListener { _, focus -> background=surface(if(focus)0xff34373e.toInt() else 0xff181a1f.toInt(),focus) }
}
fun LinearLayout.row(view:View,height:Int=ViewGroup.LayoutParams.WRAP_CONTENT) {
    addView(view,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,height).apply { bottomMargin=context.dp(10) })
}

object Images {
    private val cache=object:android.util.LruCache<String,Bitmap>(12*1024*1024) { override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount }
    private val executor=Executors.newSingleThreadExecutor()
    private val main=Handler(Looper.getMainLooper())
    fun load(context:Context,key:String,width:Int,height:Int,view:ImageView,source:()->Bitmap?) {
        view.tag=key
        val cached=cache.get(key)
        if(cached!=null) { view.setImageBitmap(cached); return }
        view.setImageDrawable(null)
        executor.execute {
            val bitmap=runCatching { source()?.let { raw ->
                val scale=minOf(1f,width.toFloat()/raw.width,height.toFloat()/raw.height)
                if(scale<1) Bitmap.createScaledBitmap(raw,(raw.width*scale).toInt().coerceAtLeast(1),(raw.height*scale).toInt().coerceAtLeast(1),true) else raw
            } }.getOrNull()
            if(bitmap!=null) cache.put(key,bitmap)
            main.post { if(view.tag==key && bitmap!=null) view.setImageBitmap(bitmap) }
        }
    }
    fun decode(context:Context,uri:Uri,width:Int,height:Int):Bitmap? {
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it,null,bounds) }
        require(bounds.outWidth in 1..30000 && bounds.outHeight in 1..30000)
        var sample=1
        while(bounds.outWidth/sample>width*2 || bounds.outHeight/sample>height*2) sample*=2
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply { inSampleSize=sample; inPreferredConfig=Bitmap.Config.RGB_565 }) }
    }
    fun drawableBitmap(drawable:android.graphics.drawable.Drawable,w:Int=256,h:Int=256):Bitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas=Canvas(bitmap)
        val iw=drawable.intrinsicWidth.coerceAtLeast(1); val ih=drawable.intrinsicHeight.coerceAtLeast(1)
        val scale=maxOf(w.toFloat()/iw,h.toFloat()/ih)
        val dw=(iw*scale).toInt(); val dh=(ih*scale).toInt()
        drawable.setBounds((w-dw)/2,(h-dh)/2,(w+dw)/2,(h+dh)/2); drawable.draw(canvas)
    }
}

fun Activity.wallpaper(target:ImageView,c:Configuration) {
    if(c.wallpaper.startsWith("default")) { target.tag="smoke-black"; target.setImageDrawable(null); target.setBackgroundColor(Color.BLACK); target.alpha=1f; return }
    target.scaleType=ImageView.ScaleType.CENTER_CROP
    val light=c.theme=="light" || (c.theme=="auto" && LocalTime.now().hour in 7..21)
    target.alpha=if(light).5f else .6f
    target.setBackgroundColor(if(light)0xff636873.toInt() else 0xff07080b.toInt())
    if(c.wallpaper.startsWith("content:")) {
        Images.load(this,c.wallpaper,1920,1080,target) { Images.decode(this,Uri.parse(c.wallpaper),1920,1080) }
    } else {
        val id=if(c.wallpaper=="default2")R.drawable.wallpaper2 else R.drawable.wallpaper1
        Images.load(this,c.wallpaper,1920,1080,target) {
            BitmapFactory.decodeResource(resources,id,BitmapFactory.Options().apply { inSampleSize=2; inPreferredConfig=Bitmap.Config.RGB_565 })
        }
    }
}

class ClockDream:DreamService() {
    private val handler=Handler(Looper.getMainLooper())
    private var clock:TextClock?=null
    private val shift=object:Runnable { override fun run() { clock?.translationX=(-120..120).random().toFloat(); clock?.translationY=(-70..70).random().toFloat(); handler.postDelayed(this,60000) } }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow(); isInteractive=false; isFullscreen=true; isScreenBright=false
        val root=FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        clock=TextClock(this).apply { format24Hour="HH:mm"; textSize=52f; setTextColor(0xff8a8e95.toInt()) }
        root.addView(clock,FrameLayout.LayoutParams(-2,-2,Gravity.CENTER)); setContentView(root); handler.post(shift)
    }
    override fun onDetachedFromWindow() { handler.removeCallbacksAndMessages(null); super.onDetachedFromWindow() }
}
