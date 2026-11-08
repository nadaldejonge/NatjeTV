package nl.natje.tv

import android.content.*
import android.graphics.*
import android.view.*
import android.widget.*
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import nl.natje.tv.settings.*
import nl.natje.tv.discovery.*
import nl.natje.tv.launcher.MainActivity
import nl.natje.tv.nightmode.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.*
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="nl-rNL-w960dp-h540dp-land-television-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidBehaviorTest {
    private val context get()=RuntimeEnvironment.getApplication()
    @Before fun clean() { context.getSharedPreferences("natje_tv",0).edit().clear().commit(); context.getSharedPreferences("history",0).edit().clear().commit() }
    @Test fun persistentConfigurationSurvivesStoreRecreation() {
        val original=Configuration(onboarding=true,order=listOf("a","b"),favorites=listOf("b"),hidden=setOf("c"),nightMode="schedule",warmth=100,dim=80,profiles=listOf(NightProfile("Mijn avond",90,60)),observedKeys=setOf(19,20))
        ConfigStore(context).save(original)
        assertEquals(original,ConfigStore(context).load())
    }
    @Test fun importIsAtomicAndCannotEnableServices() {
        val store=ConfigStore(context); store.save(Configuration(warmth=75))
        assertThrows(Exception::class.java) { store.import("{broken}") }; assertEquals(75,store.load().warmth)
        store.import(ConfigStore.encode(Configuration(nightMode="on",backend="accessibility",autoStart="example",warmth=100)).toString())
        assertEquals("off",store.load().nightMode); assertEquals("application",store.load().backend); assertEquals("",store.load().autoStart)
    }
    @Test fun malformedProfilesAndRangesAreHandled() {
        val c=ConfigStore.decode("""{"schema":1,"warmth":999,"dim":-5,"start":9999,"end":-2,"profiles":[]}""")
        assertEquals(100,c.warmth); assertEquals(0,c.dim); assertEquals(1439,c.start); assertEquals(0,c.end); assertTrue(c.profiles.isNotEmpty())
        assertThrows(Exception::class.java) { ConfigStore.decode("""{"schema":7}""") }
    }
    @Test fun favoriteMatchingUsesInstalledAppsAndManualBinding() {
        val app=TvApp("custom.player","Natje's IPTV-player",ComponentName("custom.player","Player"))
        val slot=AppDiscovery.slots[1]
        assertEquals(app,AppDiscovery.match(slot,listOf(app),Configuration()))
        assertNull(AppDiscovery.match(slot,emptyList(),Configuration()))
        assertNull(AppDiscovery.match(slot,listOf(app),Configuration(bindings=mapOf(slot.name to "removed.package"))))
        assertEquals(app,AppDiscovery.match(slot,listOf(app),Configuration(bindings=mapOf(slot.name to "custom.player"))))
    }
    @Test fun missingLaunchIntentDoesNotCrash() {
        assertFalse(AppDiscovery(context).launch(TvApp("not.installed","Missing",ComponentName("not.installed","Main"))))
    }
    @Test fun marketOnlyOpensAnInstalledHandlerAndEncodesAppSearch() {
        assertFalse(StoreAccess.open(context,"NPO Start"))
        val base=StoreAccess.marketIntent("NPO Start")
        assertEquals("NPO Start",base.data!!.getQueryParameter("q"))
        val info=ResolveInfo().apply { activityInfo=ActivityInfo().apply { packageName="com.android.vending"; name="com.android.vending.Market"; enabled=true; exported=true } }
        Shadows.shadowOf(context.packageManager).addResolveInfoForIntent(base,info)
        assertEquals("com.android.vending",StoreAccess.resolve(context,"NPO Start")!!.component!!.packageName)
        assertTrue(StoreAccess.open(context,"NPO Start"))
        assertEquals("NPO Start",Shadows.shadowOf(context).nextStartedActivity.data!!.getQueryParameter("q"))
    }
    @Test fun installedTvAndSideloadedAppsAreDiscoveredAndDeduplicated() {
        val pm=Shadows.shadowOf(context.packageManager)
        fun entry(pkg:String,name:String):ResolveInfo = ResolveInfo().apply {
            nonLocalizedLabel=name
            activityInfo=ActivityInfo().apply { packageName=pkg; this.name="$pkg.Main"; enabled=true; exported=true; applicationInfo=ApplicationInfo().apply { packageName=pkg; enabled=true } }
        }
        val tv=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val side=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.addResolveInfoForIntent(tv,entry("test.tv","TV app"))
        pm.addResolveInfoForIntent(side,entry("test.tv","Duplicate"))
        pm.addResolveInfoForIntent(side,entry("test.side","Side app"))
        pm.addResolveInfoForIntent(side,entry(context.packageName,"Self"))
        val apps=AppDiscovery(context).discover()
        assertEquals(2,apps.size); assertEquals("TV app",apps.first { it.packageName=="test.tv" }.label)
        assertEquals(listOf("test.tv","test.side"),AppDiscovery.ordered(apps,listOf("test.tv")).map { it.packageName })
    }
    @Test fun remoteEmergencyHoldWorksWithoutConsumingKeys() {
        ConfigStore(context).save(Configuration(nightMode="on"))
        val chord=RemoteChord(context)
        chord.onKey(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK))
        chord.onKey(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(2999,TimeUnit.MILLISECONDS)
        assertEquals("on",ConfigStore(context).load().nightMode)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(2,TimeUnit.MILLISECONDS)
        assertEquals("off",ConfigStore(context).load().nightMode); chord.close()
    }
    @Test fun profileCanBeCreatedFromRemoteAccessibleSettings() {
        ConfigStore(context).save(Configuration(onboarding=true))
        val controller=Robolectric.buildActivity(SettingsActivity::class.java,Intent(context,SettingsActivity::class.java).putExtra("section","night")).setup().visible()
        val button=findViews(controller.get().window.decorView).filterIsInstance<Button>().first { it.text.toString()=="Huidige waarden als profiel opslaan" }
        button.performClick()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        val dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
        val field=findViews(dialog.window!!.decorView).filterIsInstance<EditText>().first()
        field.setText("Mijn testavond"); dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        assertTrue(ConfigStore(context).load().profiles.toString(),ConfigStore(context).load().profiles.any { it.name=="Mijn testavond" && it.warmth==45 && it.dim==30 })
        controller.pause().stop().destroy()
    }
    @Test fun overlayRequiresExplicitPermission() {
        ConfigStore(context).save(Configuration(nightMode="on"))
        NightController.refresh(context)
        assertTrue(NightController.status.contains("ontbreekt")); assertFalse(NightService.running)
    }
    @Test fun emergencyPersistsOffWithoutPermission() {
        ConfigStore(context).save(Configuration(nightMode="schedule",warmth=100))
        NightController.emergencyOff(context)
        assertEquals("off",ConfigStore(context).load().nightMode); assertEquals(100,ConfigStore(context).load().warmth)
    }
    @Test fun actualFilterDrawHasAmberAndDimming() {
        val filter=FilterView(context).apply { warmth=100; dim=0; strength=1f; layout(0,0,10,10) }
        val bitmap=Bitmap.createBitmap(10,10,Bitmap.Config.ARGB_8888); val canvas=Canvas(bitmap)
        canvas.drawColor(Color.WHITE); filter.draw(canvas)
        val pixel=bitmap.getPixel(5,5)
        assertEquals(255,Color.red(pixel)); assertTrue(Color.blue(pixel) in 145..152)
        filter.dim=80; canvas.drawColor(Color.WHITE); filter.draw(canvas)
        assertTrue(Color.red(bitmap.getPixel(5,5)) in 49..53)
    }
    @Test fun homeAndSettingsRenderWithoutInstalledApps() {
        ConfigStore(context).save(Configuration(onboarding=true))
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        val activity=controller.get()
        // Allow async PackageManager discovery to post its bounded result.
        repeat(15) { Thread.sleep(40); Shadows.shadowOf(android.os.Looper.getMainLooper()).idle() }
        val decor=activity.window.decorView
        val captions=findViews(decor).filterIsInstance<TextView>()
        assertTrue(captions.any { it.text.toString()=="Mijn apps" })
        val tile=findViews(decor).firstOrNull { it.contentDescription?.toString()?.contains("NatjeTube")==true }
        assertNotNull(tile); assertTrue(tile!!.isFocusable); assertTrue(tile.requestFocusFromTouch()); assertTrue(tile.hasFocus())
        capture(activity,"home-robolectric.png")
        assertTrue(tile.performLongClick())
        controller.pause().stop().destroy()
        val settings=Robolectric.buildActivity(SettingsActivity::class.java,Intent(context,SettingsActivity::class.java)).setup().visible()
        capture(settings.get(),"settings-robolectric.png"); settings.pause().stop().destroy()
        val night=Robolectric.buildActivity(SettingsActivity::class.java,Intent(context,SettingsActivity::class.java).putExtra("section","night")).setup().visible()
        assertTrue(findViews(night.get().window.decorView).filterIsInstance<TextView>().any { it.text.toString().contains("NOODSTOP") })
        capture(night.get(),"nightmode-robolectric.png"); night.pause().stop().destroy()
    }
    private fun findViews(view:View):List<View> = listOf(view)+(if(view is ViewGroup)(0 until view.childCount).flatMap { findViews(view.getChildAt(it)) }else emptyList())
    private fun capture(activity:android.app.Activity,name:String) {
        val root=activity.window.decorView
        root.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY)); root.layout(0,0,1920,1080)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(200,TimeUnit.MILLISECONDS)
        val bitmap=Bitmap.createBitmap(1920,1080,Bitmap.Config.ARGB_8888); root.draw(Canvas(bitmap))
        val out=File("../artifacts/screenshots").apply { mkdirs() }
        File(out,name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
}
