package nl.natje.tv.launcher

import android.app.*
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import nl.natje.tv.R
import nl.natje.tv.discovery.*
import nl.natje.tv.settings.*
import nl.natje.tv.nightmode.*
import nl.natje.tv.ui.*
import java.util.concurrent.Executors

class MainActivity:Activity() {
    private val headerIds=IntArray(3) { View.generateViewId() }
    private val firstAppId=View.generateViewId()
    private data class NavPoint(val position:Int,val column:Int)
    private val navPoints=java.util.WeakHashMap<View,NavPoint>()
    private val navViews=mutableMapOf<NavPoint,View>()
    private var favoriteCount=0
    private var navigating=false
    private var okView:View?=null
    private var longOk=false
    private val holdOk=Runnable { val view=okView; if(view!=null && view.isAttachedToWindow && view.tag in listOf("natje-favorite","natje-app")) { longOk=true; view.performLongClick() } }
    private lateinit var store:ConfigStore
    private lateinit var list:RecyclerView
    private lateinit var backgroundImage:ImageView
    private lateinit var smoke:SmokeView
    private lateinit var progress:ProgressBar
    private lateinit var remote:RemoteChord
    private lateinit var discovery:AppDiscovery
    private var apps:List<TvApp> = emptyList()
    private var movePackage:String?=null
    private var moveFavorites=false
    private val main=Handler(Looper.getMainLooper())
    private val worker=Executors.newSingleThreadExecutor()
    private val packageReceiver=object:BroadcastReceiver() { override fun onReceive(c:Context,i:Intent) { reload() } }
    private val themePulse=object:Runnable { override fun run() { wallpaper(backgroundImage,store.load()); main.postDelayed(this,60000) } }
    private val waitPhrases=listOf("Heb je al gedraaid?","Moet je niet allang slapen?","Ga slapen.","Heb je niks beters te doen?")
    private lateinit var loading:TextView
    private val waiting=Runnable { loading.text=waitPhrases.random(); loading.visibility=View.VISIBLE; progress.visibility=View.VISIBLE }
    override fun onCreate(state:Bundle?) {
        super.onCreate(state); fullscreen(); store=ConfigStore(this); discovery=AppDiscovery(this); remote=RemoteChord(this)
        val root=FrameLayout(this)
        backgroundImage=ImageView(this); root.addView(backgroundImage,FrameLayout.LayoutParams(-1,-1))
        smoke=SmokeView(this); root.addView(smoke,FrameLayout.LayoutParams(-1,-1))
        val shade=View(this).apply { background=android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(0x5507080b,0x1107080b,0xf007080b.toInt())) }
        root.addView(shade,FrameLayout.LayoutParams(-1,-1))
        val column=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(38),dp(22),dp(38),0) }
        val header=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        val logo=ImageView(this).apply { setImageResource(R.drawable.natje_logo); scaleType=ImageView.ScaleType.FIT_START; contentDescription="Natje TV" }
        header.addView(logo,LinearLayout.LayoutParams(dp(125),dp(40)))
        header.addView(View(this),LinearLayout.LayoutParams(0,1,1f))
        listOf("Zoeken" to { search() },"Instellingen" to { startActivity(Intent(this,SettingsActivity::class.java)) },"NatjeNightMode" to { startActivity(Intent(this,SettingsActivity::class.java).putExtra("section","night")) }).forEachIndexed { index,(name,fn) ->
            header.addView(HeaderIcon(this,index).apply { id=headerIds[index]; contentDescription=name; setOnClickListener { fn() }; val point=NavPoint(-1,index); navPoints[this]=point; navViews[point]=this },LinearLayout.LayoutParams(dp(36),dp(36)).apply { marginEnd=dp(12) })
        }
        val clock=TextClock(this).apply { format24Hour="HH:mm"; textSize=25f; setTextColor(Color.WHITE) }
        val date=TextClock(this).apply { format24Hour="EEEE d MMMM"; format12Hour="EEEE d MMMM"; textSize=13f; setTextColor(0xffd8dce3.toInt()) }
        header.addView(LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.END; addView(clock); addView(date) })
        column.addView(header,LinearLayout.LayoutParams(-1,dp(62)))
        list=RecyclerView(this).apply { clipChildren=false; clipToPadding=false; setPadding(dp(5),0,dp(5),dp(28)); itemAnimator=null }
        val manager=GridLayoutManager(this,4)
        manager.spanSizeLookup=object:GridLayoutManager.SpanSizeLookup() { override fun getSpanSize(position:Int)=if((list.adapter as? HomeAdapter)?.items?.getOrNull(position) is Item.Tile)1 else 4 }
        list.layoutManager=manager
        column.addView(list,LinearLayout.LayoutParams(-1,0,1f)); root.addView(column)
        loading=label("Apps ophalen…",14f).apply { visibility=View.GONE }
        root.addView(loading,FrameLayout.LayoutParams(-2,-2,Gravity.CENTER))
        progress=ProgressBar(this).apply { visibility=View.GONE; isFocusable=false }
        root.addView(progress,FrameLayout.LayoutParams(dp(32),dp(32),Gravity.CENTER).apply { bottomMargin=dp(65) })
        setContentView(root)
        registerReceiver(packageReceiver,IntentFilter().apply { addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED); addAction(Intent.ACTION_PACKAGE_CHANGED); addDataScheme("package") })
        if(state==null) logo.apply { alpha=0f; animate().alpha(1f).setDuration(150).start() }
        if(!store.load().onboarding && state==null) startActivity(Intent(this,SettingsActivity::class.java).putExtra("onboarding",true))
        if(intent.getBooleanExtra("open_hdmi",false)) { intent.removeExtra("open_hdmi"); startActivity(Intent(this,nl.natje.tv.installation.HdmiActivity::class.java)) }
    }
    override fun onResume() {
        super.onResume(); wallpaper(backgroundImage,store.load()); smoke.running=true; if(list.adapter==null)render(); reload(); NightController.refresh(this); list.post { ensureFocus() }
        main.post(themePulse)
        val c=store.load()
        if(c.sleepMinutes>0) main.postDelayed(sleep,c.sleepMinutes*60000L)
    }
    private val sleep=Runnable { startActivity(Intent(this,SettingsActivity::class.java).putExtra("sleep",true)) }
    override fun onPause() { main.removeCallbacks(holdOk); okView=null; smoke.running=false; main.removeCallbacks(themePulse); main.removeCallbacks(sleep); main.removeCallbacks(waiting); remote.close(); super.onPause() }
    private fun ensureFocus() {
        if(!hasWindowFocus() || navigating) return
        val focused=currentFocus
        if(focused!=null && (focused.id in headerIds || focused.tag=="natje-favorite" || focused.tag=="natje-app") && focused.isShown && focused.isEnabled) return
        fun first(v:View):View? { if(v.isFocusable && v.contentDescription!=null)return v; if(v is ViewGroup) for(i in 0 until v.childCount) { first(v.getChildAt(i))?.let { return it } }; return null }
        first(list)?.requestFocusFromTouch()
    }
    override fun onWindowFocusChanged(hasFocus:Boolean) { super.onWindowFocusChanged(hasFocus); if(hasFocus && ::list.isInitialized) { list.post { ensureFocus() }; list.postDelayed({ensureFocus()},250); list.postDelayed({ensureFocus()},800) } }
    override fun onNewIntent(intent:Intent) { super.onNewIntent(intent); if(intent.getBooleanExtra("open_hdmi",false)) { intent.removeExtra("open_hdmi"); startActivity(Intent(this,nl.natje.tv.installation.HdmiActivity::class.java)) }; if(::list.isInitialized) list.post { ensureFocus() } }
    private fun reload() {
        main.postDelayed(waiting,450)
        worker.execute {
            val result=runCatching { discovery.discover() }.getOrDefault(emptyList())
            main.post {
                main.removeCallbacks(waiting); loading.visibility=View.GONE; progress.visibility=View.GONE
                if(result!=apps) { apps=result; render() }; list.post { ensureFocus() }
                val boot=getSharedPreferences("boot",MODE_PRIVATE)
                if(boot.getBoolean("pendingAutoStart",false)) {
                    // Clear BEFORE launch; crash or return to HOME cannot create a loop.
                    boot.edit().putBoolean("pendingAutoStart",false).commit()
                    val c=store.load()
                    if(c.onboarding) apps.firstOrNull { it.packageName==c.autoStart }?.let { discovery.launch(it) }
                }
            }
        }
    }
    private fun render() {
        val c=store.load()
        val items=mutableListOf<Item>(Item.Space,Item.Title("Mijn apps"),Item.Favorites)
        val history=getSharedPreferences("history",MODE_PRIVATE).getString("recent","")!!.split('|').mapNotNull { pkg -> apps.find { it.packageName==pkg && pkg !in c.hidden } }.take(4)
        if(history.isNotEmpty()) { items+=Item.Title("Recent geopend"); items+=history.map { Item.Tile(it) } }
        items+=Item.Title("Alle apps")
        items+=AppDiscovery.ordered(apps.filter { it.packageName !in c.hidden },c.order).map { Item.Tile(it) }
        if(apps.isEmpty()) items+=Item.Title("Nog geen lanceerbare apps gevonden. Natje TV blijft beschikbaar.")
        val layoutState=list.layoutManager?.onSaveInstanceState()
        list.adapter=HomeAdapter(items)
        list.layoutManager?.onRestoreInstanceState(layoutState)
        if(movePackage!=null && !moveFavorites) {
            val position=items.indexOfFirst { it is Item.Tile && it.app.packageName==movePackage }
            if(position>=0) list.scrollToPosition(position)
        }
    }
    private fun launch(app:TvApp?) {
        if(app==null || !discovery.launch(app)) { Toast.makeText(this,"Deze app is niet geïnstalleerd of niet beschikbaar.",Toast.LENGTH_SHORT).show(); return }
        val p=getSharedPreferences("history",MODE_PRIVATE)
        val recent=(listOf(app.packageName)+p.getString("recent","")!!.split('|')).filter { it.isNotBlank() }.distinct().take(12)
        p.edit().putString("recent",recent.joinToString("|")).apply()
    }
    fun selectPackage(title:String,onSelect:(TvApp)->Unit) {
        if(apps.isEmpty()) { Toast.makeText(this,"Geen apps beschikbaar",Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this).setTitle(title).setItems(apps.map { "${it.label}\n${it.packageName}" }.toTypedArray()) { _,i -> onSelect(apps[i]) }.setNegativeButton("Annuleren",null).show()
    }
    private fun options(app:TvApp,fromFavorites:Boolean) {
        val c=store.load()
        val defaultFavorite=AppDiscovery.slots.any { AppDiscovery.match(it,apps,c)?.packageName==app.packageName }
        val isFavorite=(app.packageName in c.favorites || defaultFavorite) && app.packageName !in c.excludedFavorites
        val choices=arrayOf("Verplaatsen met D-pad",if(isFavorite)"Uit favorieten" else "Als favoriet toevoegen","Icoon / banner kiezen","Artwork herstellen","Categorie kiezen","Verbergen","App-info")
        AlertDialog.Builder(this).setTitle(app.label).setItems(choices) { _,i -> when(i) {
            0->{ movePackage=app.packageName; moveFavorites=fromFavorites; Toast.makeText(this,"Gebruik D-pad links/rechts/omhoog/omlaag; OK of Terug stopt verplaatsen.",Toast.LENGTH_LONG).show() }
            1->{ store.update { it.copy(favorites=if(isFavorite)it.favorites-app.packageName else (it.favorites+app.packageName).distinct(),excludedFavorites=if(isFavorite)it.excludedFavorites+app.packageName else it.excludedFavorites-app.packageName) }; render() }
            2->{ pendingArtwork=app.packageName; startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),61) }
            3->{ store.update { it.copy(artwork=it.artwork-app.packageName) }; render() }
            4->{ val cats=arrayOf("Mijn apps","Streaming","Muziek","Tools","Games"); AlertDialog.Builder(this).setTitle("Categorie").setItems(cats) { _,which -> store.update { it.copy(categories=it.categories+(app.packageName to cats[which])) }; render() }.show() }
            5->{ store.update { it.copy(hidden=it.hidden+app.packageName) }; render() }
            6->startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${app.packageName}")))
        } }.show()
    }
    private var pendingArtwork:String?=null
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==61 && resultCode==RESULT_OK) data?.data?.let { uri ->
            runCatching { contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); require(Images.decode(this,uri,480,270)!=null); val pkg=pendingArtwork ?: return@runCatching; store.update { it.copy(artwork=it.artwork+(pkg to uri.toString())) }; render() }.onFailure { Toast.makeText(this,"Afbeelding kon niet worden gelezen",Toast.LENGTH_LONG).show() }
        }
    }
    override fun dispatchKeyEvent(event:KeyEvent):Boolean {
        if(movePackage==null && event.keyCode in listOf(KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER)) {
            if(event.action==KeyEvent.ACTION_DOWN) {
                if(event.repeatCount==0) { ensureFocus(); main.removeCallbacks(holdOk); okView=currentFocus; longOk=false; main.postDelayed(holdOk,500) }
                else if(event.isLongPress && !longOk) { main.removeCallbacks(holdOk); holdOk.run() }
                return true
            }
            if(event.action==KeyEvent.ACTION_UP) { main.removeCallbacks(holdOk); val view=okView; okView=null; if(!longOk)view?.performClick(); return true }
        }
        if(movePackage==null && event.action==KeyEvent.ACTION_DOWN && event.keyCode in listOf(KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN,KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT,KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER)) {
            ensureFocus()
            if(event.keyCode in listOf(KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN,KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT)) { navigate(event.keyCode); return true }
        }
        if(NightAccessibilityService.current==null) remote.onKey(event)
        val pkg=movePackage
        if(pkg!=null && event.action==KeyEvent.ACTION_DOWN) {
            if(event.keyCode in listOf(KeyEvent.KEYCODE_BACK,KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER)) { movePackage=null; return true }
            val delta=when(event.keyCode) { KeyEvent.KEYCODE_DPAD_LEFT->-1; KeyEvent.KEYCODE_DPAD_RIGHT->1; KeyEvent.KEYCODE_DPAD_UP->-4; KeyEvent.KEYCODE_DPAD_DOWN->4; else->0 }
            if(delta!=0) {
                val ordered=AppDiscovery.ordered(apps,store.load().order).map { it.packageName }.toMutableList(); val index=ordered.indexOf(pkg)
                if(index>=0) { ordered.removeAt(index); ordered.add((index+delta).coerceIn(0,ordered.size),pkg); store.update { it.copy(order=ordered,favorites=it.favorites.sortedBy { p -> ordered.indexOf(p) }) }; render() }; return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
    private fun navigate(key:Int) {
        if(navigating)return
        val adapter=list.adapter as? HomeAdapter ?: return
        val manager=list.layoutManager as GridLayoutManager
        val rows=sortedMapOf<Int,MutableList<NavPoint>>()
        rows[-1]=(0..2).map { NavPoint(-1,it) }.toMutableList()
        adapter.items.forEachIndexed { position,item ->
            val group=manager.spanSizeLookup.getSpanGroupIndex(position,4)
            if(item==Item.Favorites) rows[group]=(0 until favoriteCount).map { NavPoint(position,it) }.toMutableList()
            else if(item is Item.Tile) rows.getOrPut(group) { mutableListOf() }.add(NavPoint(position,manager.spanSizeLookup.getSpanIndex(position,4)))
        }
        val current=navPoints[currentFocus] ?: return
        val groups=rows.keys.toList(); val group=if(current.position<0)-1 else manager.spanSizeLookup.getSpanGroupIndex(current.position,4)
        val row=rows[group] ?: return; val index=row.indexOf(current).coerceAtLeast(0)
        val horizontal=key==KeyEvent.KEYCODE_DPAD_LEFT || key==KeyEvent.KEYCODE_DPAD_RIGHT
        val target=if(horizontal) row[(index+if(key==KeyEvent.KEYCODE_DPAD_LEFT)-1 else 1).coerceIn(0,row.lastIndex)] else {
            val nextGroup=groups[(groups.indexOf(group)+if(key==KeyEvent.KEYCODE_DPAD_UP)-1 else 1).coerceIn(0,groups.lastIndex)]
            val next=rows.getValue(nextGroup); next[index.coerceAtMost(next.lastIndex)]
        }
        navigating=true
        fun focus(attempt:Int) {
            val view=navViews[target]
            if(view!=null && view.isAttachedToWindow) {
                view.requestFocusFromTouch(); view.requestRectangleOnScreen(android.graphics.Rect(0,0,view.width,view.height),true); navigating=false
            } else if(attempt<4) { if(target.position>=0)list.scrollToPosition(target.position); list.postDelayed({focus(attempt+1)},60) }
            else navigating=false
        }
        focus(0)
    }
    override fun onBackPressed() { if(movePackage!=null)movePackage=null else list.smoothScrollToPosition(0) }
    override fun onDestroy() { unregisterReceiver(packageReceiver); main.removeCallbacksAndMessages(null); worker.shutdownNow(); remote.close(); super.onDestroy() }

    sealed class Item { data object Space:Item(); data class Title(val text:String):Item(); data object Favorites:Item(); data class Tile(val app:TvApp):Item() }
    private inner class Holder(val container:FrameLayout):RecyclerView.ViewHolder(container)
    private inner class HomeAdapter(val items:List<Item>):RecyclerView.Adapter<Holder>() {
        override fun getItemCount()=items.size
        override fun getItemViewType(position:Int)=when(items[position]) { Item.Space->0; is Item.Title->1; Item.Favorites->2; is Item.Tile->4 }
        override fun onCreateViewHolder(parent:ViewGroup,viewType:Int)=Holder(FrameLayout(this@MainActivity).apply { clipChildren=false; clipToPadding=false; layoutParams=RecyclerView.LayoutParams(-1,-2) })
        override fun onBindViewHolder(holder:Holder,position:Int) {
            val root=holder.container; root.removeAllViews(); val c=store.load()
            when(val item=items[position]) {
                Item.Space->root.addView(View(this@MainActivity),FrameLayout.LayoutParams(1,dp(125)))
                is Item.Title->root.addView(label(item.text,16f,0xffd8dce3.toInt()).apply { setPadding(dp(4),dp(24),0,dp(14)) })
                Item.Favorites->{
                    val row=LinearLayout(this@MainActivity).apply { orientation=LinearLayout.HORIZONTAL; clipChildren=false; clipToPadding=false; setPadding(dp(7),dp(8),dp(7),dp(16)) }
                    val width=dp(112)
                    val defaults=AppDiscovery.slots.map { it to (AppDiscovery.match(it,apps,c) ?: if(it.name=="Appwinkel")StoreAccess.app(this@MainActivity) else null) }.filter { it.second?.packageName !in c.hidden && it.second?.packageName !in c.excludedFavorites }
                    val custom=c.favorites.mapNotNull { p -> apps.firstOrNull { it.packageName==p } }.filter { a -> defaults.none { it.second?.packageName==a.packageName } }
                    val tiles=defaults.map { Triple(it.first.name,it.second,it.first.name as String?) }+custom.map { Triple(it.label,it,null as String?) }
                    val ordered=tiles.sortedBy { t -> c.order.indexOf(t.second?.packageName).let { if(it<0)Int.MAX_VALUE else it } }+Triple("HDMI / PS5",null,"HDMI / PS5")
                    favoriteCount=ordered.size
                    ordered.forEachIndexed { index,(name,app,slot) -> row.addView(tile(name,app,slot,true,index==0,position,index),LinearLayout.LayoutParams(width,dp(138)).apply { marginEnd=dp(14) }) }
                    root.addView(HorizontalScrollView(this@MainActivity).apply { isHorizontalScrollBarEnabled=false; clipChildren=false; clipToPadding=false; addView(row) })
                }
                is Item.Tile->root.addView(tile(item.app.label,item.app,null,navPosition=position,navColumn=(list.layoutManager as GridLayoutManager).spanSizeLookup.getSpanIndex(position,4)),FrameLayout.LayoutParams(-1,dp(140)).apply { marginEnd=dp(12); bottomMargin=dp(12) })
            }
        }
    }
    private fun tile(name:String,app:TvApp?,slot:String?,favorite:Boolean=false,firstFavorite:Boolean=false,navPosition:Int=-2,navColumn:Int=0):View {
        val column=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; clipChildren=false; clipToPadding=false }
        fun roundCard(focus:Boolean=false)=surface(focused=focus).apply { cornerRadius=dp(60).toFloat() }
        val hdmi=slot=="HDMI / PS5"
        val card=FrameLayout(this).apply { background=roundCard(); clipToOutline=true; isFocusable=true; isFocusableInTouchMode=true; isSoundEffectsEnabled=store.load().sounds; contentDescription=if(app==null && !hdmi)"$name, niet beschikbaar" else name }
        card.id=if(firstFavorite)firstAppId else View.generateViewId()
        card.tag=if(favorite)"natje-favorite" else "natje-app"
        val point=NavPoint(navPosition,navColumn); navPoints[card]=point; navViews[point]=card
        val image=ImageView(this).apply { scaleType=ImageView.ScaleType.CENTER_CROP }
        card.addView(image,FrameLayout.LayoutParams(-1,-1))
        if(hdmi) { image.setImageResource(R.drawable.ic_hdmi); image.setPadding(dp(22),dp(22),dp(22),dp(22)) }
        else if(app!=null) {
            val custom=store.load().artwork[app.packageName]
            Images.load(this,"round-v2"+app.packageName+app.lastUpdate+(custom ?: ""),256,256,image) {
                if(custom!=null) Images.decode(this,Uri.parse(custom),480,270) else {
                    val info=packageManager.getApplicationInfo(app.packageName,0)
                    val icon=info.loadIcon(packageManager)
                    if(app.packageName=="com.android.vending") android.graphics.Bitmap.createBitmap(256,256,android.graphics.Bitmap.Config.ARGB_8888).also { bitmap -> icon.setBounds(38,38,218,218); icon.draw(android.graphics.Canvas(bitmap)) } else Images.drawableBitmap(icon)
                }
            }
        } else {
            image.setImageResource(if(slot=="Natje IPTV")R.drawable.iptv_logo else R.drawable.natje_symbol)
            image.alpha=.28f
            card.addView(label("Niet beschikbaar",11f,0xffb5b9c1.toInt()).apply { gravity=Gravity.CENTER },FrameLayout.LayoutParams(-1,dp(24),Gravity.BOTTOM))
        }
        val caption=label(name,13f).apply { gravity=Gravity.CENTER; alpha=0f; maxLines=1; ellipsize=android.text.TextUtils.TruncateAt.END }
        column.addView(card,LinearLayout.LayoutParams(dp(104),dp(104)).apply { gravity=Gravity.CENTER_HORIZONTAL }); column.addView(caption,LinearLayout.LayoutParams(-1,dp(28)))
        card.setOnFocusChangeListener { _,focus ->
            card.animate().scaleX(if(focus)1.055f else 1f).scaleY(if(focus)1.055f else 1f).setDuration(120).start()
            card.background=roundCard(focus); card.elevation=if(focus)dp(10).toFloat() else 0f; caption.alpha=if(focus)1f else 0f
            card.foreground=if(focus)android.graphics.drawable.GradientDrawable().apply { shape=android.graphics.drawable.GradientDrawable.OVAL; setColor(Color.TRANSPARENT); setStroke(dp(2.5f),Color.WHITE) } else null
            if(Build.VERSION.SDK_INT>=28) { card.outlineAmbientShadowColor=Color.WHITE; card.outlineSpotShadowColor=Color.WHITE }
        }
        card.setOnClickListener {
            if(hdmi)startActivity(Intent(this,nl.natje.tv.installation.HdmiActivity::class.java))
            else if(slot=="Appwinkel") { if(!StoreAccess.open(this,null,app))Toast.makeText(this,"Geen geïnstalleerde appwinkel gevonden",Toast.LENGTH_LONG).show() }
            else if(app==null && slot!=null) missingApp(slot) else launch(app)
        }
        card.setOnLongClickListener { longOk=true; if(hdmi)startActivity(Intent(this,nl.natje.tv.installation.HdmiActivity::class.java)) else if(app!=null)options(app,favorite) else if(slot!=null)selectPackage("Koppel $slot") { chosen -> store.update { it.copy(bindings=it.bindings+(slot to chosen.packageName)) }; render() }; true }
        if(app!=null && app.packageName==movePackage && favorite==moveFavorites) card.post { card.requestFocus() }
        return column
    }
    private fun missingApp(slot:String) {
        val owned=slot=="NatjeTube" || slot=="Natje IPTV"
        val choices=if(owned)arrayOf("Geïnstalleerde app koppelen") else arrayOf("Zoeken in Appwinkel","Geïnstalleerde app koppelen")
        AlertDialog.Builder(this).setTitle("$slot installeren").setMessage(if(owned)"Installeer de losse $slot-APK uit het meegeleverde apppakket. Deze app blijft zelfstandig bij te werken." else "Open de officiële winkel op jouw apparaat en installeer de beschikbare TV-versie. Daarna verschijnt de app hier automatisch.")
            .setPositiveButton(if(owned)"Pakket koppelen" else "Appwinkel openen") { _,_ ->
                if(owned)selectPackage("Koppel $slot") { chosen -> store.update { it.copy(bindings=it.bindings+(slot to chosen.packageName)) }; render() }
                else if(!StoreAccess.open(this,slot))Toast.makeText(this,"Geen geïnstalleerde appwinkel gevonden",Toast.LENGTH_LONG).show()
            }.setNeutralButton("Ander pakket kiezen") { _,_ -> selectPackage("Koppel $slot") { chosen -> store.update { it.copy(bindings=it.bindings+(slot to chosen.packageName)) }; render() } }.setNegativeButton("Sluiten",null).show()
    }
    private fun search() {
        val field=EditText(this).apply { hint="Naam van geïnstalleerde app"; isSingleLine=true }
        AlertDialog.Builder(this).setTitle("Apps zoeken").setView(field).setPositiveButton("Zoeken") { _,_ ->
            val matches=apps.filter { it.label.contains(field.text.toString(),ignoreCase=true) }
            if(matches.isEmpty())Toast.makeText(this,"Geen apps gevonden",Toast.LENGTH_SHORT).show()
            else AlertDialog.Builder(this).setTitle("Resultaten").setItems(matches.map { it.label }.toTypedArray()) { _,i -> launch(matches[i]) }.show()
        }.setNegativeButton("Annuleren",null).show()
    }
}
