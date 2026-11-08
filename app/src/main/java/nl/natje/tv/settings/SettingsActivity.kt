package nl.natje.tv.settings

import android.app.*
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import nl.natje.tv.R
import nl.natje.tv.discovery.*
import nl.natje.tv.nightmode.*
import nl.natje.tv.ui.*

class SettingsActivity:Activity() {
    private lateinit var store:ConfigStore
    private lateinit var content:LinearLayout
    private lateinit var scroll:ScrollView
    private lateinit var remote:RemoteChord
    private var section="general"
    private var wizardStep=0
    private var onboarding=false
    private val handler=Handler(Looper.getMainLooper())
    private var status:TextView?=null
    private val pulse=object:Runnable { override fun run() { status?.text=statusText(); handler.postDelayed(this,2000) } }
    override fun onCreate(state:Bundle?) {
        super.onCreate(state); fullscreen(); store=ConfigStore(this); remote=RemoteChord(this)
        if(intent.getBooleanExtra("sleep",false)) { showRest(); return }
        onboarding=intent.getBooleanExtra("onboarding",false)
        section=state?.getString("section") ?: intent.getStringExtra("section") ?: "general"
        wizardStep=state?.getInt("wizardStep") ?: 0
        val root=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(dp(32),dp(24),dp(32),dp(24)); setBackgroundColor(0xff07080b.toInt()) }
        val nav=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,0,dp(24),0) }
        nav.row(ImageView(this).apply { setImageResource(R.drawable.natje_logo); scaleType=ImageView.ScaleType.FIT_START; contentDescription="Natje TV" },dp(55))
        listOf("Algemeen" to "general","NatjeNightMode" to "night","Apps & favorieten" to "apps","Uiterlijk" to "appearance","Configuratie" to "config").forEach { (name,key) -> nav.row(action(name) { section=key; render() }) }
        nav.row(action("Terug naar home") { finish() })
        root.addView(nav,LinearLayout.LayoutParams(dp(215),-1))
        content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(14),0,dp(14),dp(40)) }
        scroll=ScrollView(this).apply { isFillViewport=true; addView(content) }
        root.addView(scroll,LinearLayout.LayoutParams(0,-1,1f)); setContentView(root)
        render()
    }
    override fun onResume() { super.onResume(); if(::content.isInitialized) { NightController.refresh(this); handler.post(pulse) } }
    override fun onPause() { handler.removeCallbacks(pulse); if(::remote.isInitialized)remote.close(); super.onPause() }
    override fun onSaveInstanceState(out:Bundle) { out.putString("section",section); out.putInt("wizardStep",wizardStep); super.onSaveInstanceState(out) }
    private fun title(text:String) { content.row(label(text,27f).apply { setPadding(0,dp(6),0,dp(10)) }) }
    private fun note(text:String) { content.row(label(text,14f,0xffb5b9c1.toInt()).apply { setLineSpacing(dp(3).toFloat(),1f); setPadding(0,0,0,dp(6)) }) }
    private fun button(text:String,click:()->Unit)=content.row(action(text,click))
    private fun change(fn:(Configuration)->Configuration) { runCatching { store.update(fn); NightController.refresh(this) }.onFailure { toast("Opslaan mislukt") } }
    private fun toast(text:String)=Toast.makeText(this,text,Toast.LENGTH_LONG).show()
    fun render() {
        content.removeAllViews(); status=null
        if(onboarding) { wizard(); return }
        when(section) {
            "night"->night()
            "appearance"->appearance()
            "apps"->apps()
            "config"->configuration()
            else->general()
        }
        scroll.scrollTo(0,0)
    }
    private fun wizard() {
        title("Welkom bij Natje TV · ${wizardStep+1}/4")
        when(wizardStep) {
            0->{ val found=AppDiscovery(this).discover(); note("${found.size} lanceerbare apps gevonden. Nieuwe apps verschijnen automatisch. NatjeTube en Natje IPTV blijven zelfstandige apps."); found.take(6).forEach { note(it.label) } }
            1->{ note("Favorieten staan klaar in deze volgorde: NatjeTube, Natje IPTV, Netflix, Videoland, NPO Start en Prime Video. Ontbrekende apps blijven herkenbaar als niet beschikbaar. In Apps & favorieten kun je zelf een pakket koppelen."); button("Pakket koppelen") { AlertDialog.Builder(this).setTitle("Favoriet koppelen").setItems(AppDiscovery.slots.map { it.name }.toTypedArray()) { _,i -> val slot=AppDiscovery.slots[i]; chooseApp(slot.name,false) { pkg -> change { it.copy(bindings=it.bindings+(slot.name to pkg)) }; render() } }.show() } }
            2->{ note("Kies je eerste Night Mode-profiel. De schermfilter blijft uit tot je hem zelf activeert."); profilePickerButtons(); button("Eigen profiel maken") { profileName(null) } }
            3->{ note("Een schermfilter over andere apps vereist aparte toestemming. Je kunt dit overslaan en later instellen. Beveiligde video en HDR moeten op jouw stick worden getest."); permissionButtons(); button("Tijdschema 22:00–07:00 activeren") { change { it.copy(nightMode="schedule") }; toast("Tijdschema opgeslagen; werking vereist toestemming") } }
        }
        button(if(wizardStep==3)"Klaar · naar home" else "Volgende") {
            if(wizardStep==3) { change { it.copy(onboarding=true) }; finish() } else { wizardStep++; render() }
        }
        button("Later instellen") { change { it.copy(onboarding=true) }; finish() }
    }
    private fun general() {
        title("Instellingen")
        note("Natje TV 1.0.0 · nl.natje.tv\nEén rustig startscherm. Lang OK op een app opent de opties; verplaatsen doe je met de D-pad.")
        button("Android-systeeminstellingen openen") { supportedIntent(Intent(Settings.ACTION_SETTINGS)) }
        button("NatjeNightMode instellen") { section="night"; render() }
        button("Introductie opnieuw bekijken") { onboarding=true; wizardStep=0; render() }
        button("Navigatiegeluid: ${if(store.load().sounds)"aan" else "uit"}") { change { it.copy(sounds=!it.sounds) }; render() }
        button("Automatisch starten na boot: ${store.load().autoStart.ifBlank { "uit" }}") {
            chooseApp("App starten na eerste HOME na boot",true) { pkg -> change { it.copy(autoStart=pkg) }; render() }
        }
        note("Automatisch starten gebeurt één keer zodra Natje TV na boot opent. Android staat vrij starten van activiteiten vanuit de achtergrond niet altijd toe.")
        button("Donkere klok / screensaver instellen") { supportedIntent(Intent(Settings.ACTION_DREAM_SETTINGS)) }
        button("Rustscherm op home: ${store.load().sleepMinutes} minuten (0 = uit)") {
            val minutes=intArrayOf(0,15,30,60,90,120)
            AlertDialog.Builder(this).setTitle("Rustscherm bij inactiviteit op home").setItems(minutes.map { if(it==0)"Uit" else "$it minuten" }.toTypedArray()) { _,i -> change { it.copy(sleepMinutes=minutes[i]) }; render() }.show()
        }
        note("Het rustscherm zet de televisie niet fysiek uit en loopt alleen wanneer het homescreen open is. Gebruik voor tv-slaap de timer van je televisie.")
        button("HDMI / PS5 · bron kiezen") { startActivity(Intent(this,nl.natje.tv.installation.HdmiActivity::class.java)) }
    }
    private fun night() {
        val c=store.load()
        title("NatjeNightMode")
        status=label(statusText(),14f,0xffd8dce3.toInt()).also { content.row(it) }
        note("Zelfde amberkleur en 0–60% warmtebereik als NatjeTube 2.0.1. Dimmer 0–80%. Geen tv-backlightregeling. Dubbele filtering is mogelijk als Night Mode ook in NatjeTube aan staat.")
        val states=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        listOf("Uit" to "off","Aan" to "on","Tijdschema" to "schedule").forEach { (name,key) -> states.addView(action(if(c.nightMode==key)"✓ $name" else name) { change { it.copy(nightMode=key) }; render() },LinearLayout.LayoutParams(0,dp(48),1f).apply { marginEnd=dp(8) }) }
        content.row(states)
        button("NOODSTOP · filter direct uit") { NightController.emergencyOff(this); render() }
        slider("Warmte",c.warmth,100) { value -> change { it.copy(warmth=value,profile="Custom") } }
        slider("Extra verduistering",c.dim,80) { value -> change { it.copy(dim=value,profile="Custom") } }
        note("Overgang: 5 seconden bij aan/uit. Noodstop is direct. Profiel: ${c.profile}")
        profilePickerButtons()
        button("Huidige waarden als profiel opslaan") { profileName(null) }
        button("Profiel hernoemen") { pickProfile { profileName(it) } }
        button("Profiel wissen") { pickProfile { selected -> change { it.copy(profiles=it.profiles.filterNot { p -> p.name==selected.name }.ifEmpty { Configuration.defaults }) }; render() } }
        button("Starttijd: ${time(c.start)}") { timePicker(c.start) { value -> change { it.copy(start=value) }; render() } }
        button("Eindtijd: ${time(c.end)}") { timePicker(c.end) { value -> change { it.copy(end=value) }; render() } }
        note("Planning gebruikt de lokale tijd, ook over middernacht. Gelijke begin- en eindtijd betekent geen actieve periode. Tijdschema is pas actief na selectie bovenaan.")
        button("Filtertype: ${if(c.backend=="application")"gewone overlay" else "toegankelijkheids-overlay"}") {
            AlertDialog.Builder(this).setTitle("Filtertype").setItems(arrayOf("Gewone overlay (voorkeur)","Toegankelijkheids-overlay (optioneel)")) { _,i -> change { it.copy(backend=if(i==0)"application" else "accessibility") }; render() }.show()
        }
        permissionButtons()
        button("Combinatie: ${when(c.chord){"menu_down"->"Menu + omlaag";"menu_up"->"Menu + omhoog";else->"uit"}}") {
            AlertDialog.Builder(this).setTitle("Combinatie voor aan/uit").setItems(arrayOf("Menu + omlaag","Menu + omhoog","Uit")) { _,i -> change { it.copy(chord=listOf("menu_down","menu_up","none")[i]) }; render() }.show()
        }
        note("Combinaties werken op home; boven andere apps alleen als de optionele toegankelijkheidsservice toetsen ontvangt. Android/firmware kan toetsen reserveren. Noodstop: houd Terug + omlaag 3 seconden vast. Home en Terug worden niet geblokkeerd.")
        note("DRM, Netflix, HDR10 en Dolby Vision: NOG NIET OP ECHTE STICK GETEST. Een actief venster bewijst niet dat het over beveiligde video zichtbaar is. Bij blokkade kies je zelf het andere filtertype; beveiliging wordt niet omzeild.")
    }
    private fun statusText():String {
        val c=store.load()
        return "${NightController.status}\nGewone overlay: ${if(Settings.canDrawOverlays(this))"toestemming aanwezig" else "geen toestemming"} · toegankelijkheid: ${if(NightAccessibilityService.current!=null)"verbonden" else "niet verbonden"}\nOntvangen toetsen: ${c.observedKeys.sorted().joinToString { KeyEvent.keyCodeToString(it).removePrefix("KEYCODE_") }.ifBlank { "nog geen" }}"
    }
    private fun permissionButtons() {
        button("Toestemming voor schermoverlay openen") { supportedIntent(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName"))) }
        button("Optionele toegankelijkheid instellen") {
            AlertDialog.Builder(this).setTitle("Optionele toegankelijkheidsservice").setMessage("Gebruikt alleen een filtervenster en toetscodes voor jouw combinatie. Leest geen schermtekst of accounts. Je schakelt de service zelf in. Geen garantie voor beveiligde video.").setPositiveButton("Instellingen openen") { _,_ -> supportedIntent(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }.setNegativeButton("Annuleren",null).show()
        }
        if(Build.VERSION.SDK_INT>=33) button("Meldingen voor status / uitknop toestaan") { requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),91) }
    }
    private fun slider(name:String,value:Int,max:Int,onChange:(Int)->Unit) {
        val caption=label("$name: $value%",16f); content.row(caption)
        content.row(SeekBar(this).apply {
            this.max=max; progress=value; keyProgressIncrement=5; minimumHeight=dp(44); contentDescription=name
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar:SeekBar?,progress:Int,fromUser:Boolean) { if(fromUser) { caption.text="$name: $progress%"; onChange(progress) } }
                override fun onStartTrackingTouch(bar:SeekBar?) {}
                override fun onStopTrackingTouch(bar:SeekBar?) {}
            })
        })
    }
    private fun profilePickerButtons() { button("Profiel kiezen: ${store.load().profile}") { pickProfile { p -> change { it.copy(profile=p.name,warmth=p.warmth,dim=p.dim) }; render() } } }
    private fun pickProfile(chosen:(NightProfile)->Unit) { val ps=store.load().profiles; AlertDialog.Builder(this).setTitle("Profielen").setItems(ps.map { "${it.name} · ${it.warmth}% / ${it.dim}%" }.toTypedArray()) { _,i -> chosen(ps[i]) }.show() }
    private fun profileName(old:NightProfile?) {
        val field=EditText(this).apply { hint="Profielnaam"; setSingleLine(); setText(old?.name ?: "") }
        val dialog=AlertDialog.Builder(this).setTitle(if(old==null)"Profiel opslaan" else "Profiel hernoemen").setView(field).setPositiveButton("Opslaan",null).setNegativeButton("Annuleren",null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name=field.text.toString().trim(); val c=store.load()
            if(name.isEmpty() || name.length>32 || c.profiles.any { it.name==name && it.name!=old?.name } || (old==null && c.profiles.size>=20)) { field.error="Kies een unieke naam van 1–32 tekens (max. 20 profielen)" }
            else { change { it.copy(profiles=it.profiles.filterNot { p->p.name==old?.name }+NightProfile(name,old?.warmth ?: it.warmth,old?.dim ?: it.dim),profile=name) }; dialog.dismiss(); render() }
        } }; dialog.show()
    }
    private fun apps() {
        title("Apps & favorieten")
        val c=store.load(); val apps=AppDiscovery(this).discover()
        AppDiscovery.slots.forEach { slot ->
            val found=AppDiscovery.match(slot,apps,c)
            button("${slot.name} · ${found?.label ?: "niet beschikbaar"}") { chooseApp("Koppel ${slot.name}",false) { pkg -> change { it.copy(bindings=it.bindings+(slot.name to pkg)) }; render() } }
        }
        button("Automatische herkenning en standaardfavorieten herstellen") { change { it.copy(bindings=emptyMap(),excludedFavorites=emptySet()) }; render() }
        button("Verborgen apps herstellen (${c.hidden.size})") { change { it.copy(hidden=emptySet()) }; render() }
        note("Lang OK op een app: verplaatsen, favoriet, eigen artwork, categorie, verbergen of app-info. Nieuwe apps worden dynamisch ontdekt. Recents toont alleen apps die je vanuit Natje TV opent.")
        button("Categorie bekijken") {
            val cats=arrayOf("Mijn apps","Streaming","Muziek","Tools","Games")
            AlertDialog.Builder(this).setTitle("Categorie").setItems(cats) { _,i ->
                val selected=apps.filter { (c.categories[it.packageName] ?: "Mijn apps")==cats[i] }
                AlertDialog.Builder(this).setTitle(cats[i]).setItems(selected.map { it.label }.toTypedArray()) { _,which -> AppDiscovery(this).launch(selected[which]) }.show()
            }.show()
        }
    }
    private fun appearance() {
        val c=store.load(); title("Uiterlijk")
        button("Thema: ${c.theme}") { AlertDialog.Builder(this).setTitle("Thema").setItems(arrayOf("Donker","Licht","Automatisch · licht 07:00–22:00")) { _,i -> change { it.copy(theme=listOf("dark","light","auto")[i]) }; render() }.show() }
        button("Standaardachtergrond 1") { change { it.copy(wallpaper="default1") }; toast("Achtergrond opgeslagen") }
        button("Standaardachtergrond 2") { change { it.copy(wallpaper="default2") }; toast("Achtergrond opgeslagen") }
        button("Eigen foto / afbeelding kiezen") { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),71) }
        note("Originele meegeleverde 4K-wallpapers. Eigen foto's worden begrensd gedecodeerd en passend bijgesneden. Focus: 1,055× / 120 ms. Vier 16:9-tegels naast elkaar. Geen live-blur of videowallpaper.")
    }
    private fun configuration() {
        title("Configuratie & privacy")
        button("Configuratie exporteren") { startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"NatjeTV-configuratie.json"),72) }
        button("Configuratie importeren") { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE),73) }
        note("Geen tracking, reclame of internetpermissie. Alleen lokale instellingen, profielen en zelf geopende apps worden bewaard. Export bevat geen accounts of beeldbestanden. Na import blijven filter en automatisch starten uit; kies eigen afbeeldingen opnieuw.")
        button("Recente apps wissen") { getSharedPreferences("history",MODE_PRIVATE).edit().clear().apply(); toast("Geschiedenis gewist") }
        button("Lokale instellingen herstellen") { AlertDialog.Builder(this).setTitle("Instellingen herstellen?").setMessage("Eigen profielen en appvolgorde worden gewist. Andere apps blijven intact.").setPositiveButton("Herstellen") { _,_ -> NightController.emergencyOff(this); store.save(Configuration(onboarding=true)); render() }.setNegativeButton("Annuleren",null).show() }
        button("Licenties bekijken") { val text=assets.open("NOTICE.txt").bufferedReader().use { it.readText() }; AlertDialog.Builder(this).setTitle("Bronnen en licenties").setMessage(text).setPositiveButton("Sluiten",null).show() }
    }
    private fun chooseApp(title:String,allowOff:Boolean,onSelect:(String)->Unit) {
        val apps=AppDiscovery(this).discover()
        val labels=(if(allowOff)listOf("Uit")else emptyList())+apps.map { "${it.label}\n${it.packageName}" }
        AlertDialog.Builder(this).setTitle(title).setItems(labels.toTypedArray()) { _,i -> onSelect(if(allowOff && i==0)"" else apps[i-if(allowOff)1 else 0].packageName) }.setNegativeButton("Annuleren",null).show()
    }
    private fun supportedIntent(intent:Intent) { runCatching { startActivity(intent) }.onFailure { toast("Dit instellingenscherm is niet beschikbaar op deze firmware") } }
    private fun time(minutes:Int)="%02d:%02d".format(minutes/60,minutes%60)
    private fun timePicker(value:Int,onSet:(Int)->Unit) { TimePickerDialog(this,{ _,h,m -> onSet(h*60+m) },value/60,value%60,true).show() }
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode!=RESULT_OK) return
        val uri=data?.data ?: return
        runCatching { when(requestCode) {
            71->{ contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); require(Images.decode(this,uri,1920,1080)!=null); change { it.copy(wallpaper=uri.toString()) } }
            72->contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(store.export()) } ?: error("Niet schrijfbaar")
            73->{ val text=contentResolver.openInputStream(uri)?.use { input -> val buffer=ByteArray(131073); var count=0; while(count<buffer.size) { val n=input.read(buffer,count,buffer.size-count); if(n<0)break; count+=n }; require(count<=131072); String(buffer,0,count,Charsets.UTF_8) } ?: error("Niet leesbaar"); store.import(text); NightController.refresh(this); render() }
        } }.onSuccess { toast("Opgeslagen") }.onFailure { toast("Bestand ongeldig of niet toegankelijk") }
    }
    private fun showRest() {
        val root=FrameLayout(this).apply { setBackgroundColor(Color.BLACK); isFocusable=true; isFocusableInTouchMode=true; setOnClickListener { finish() }; setOnKeyListener { _,_,event -> if(event.action==KeyEvent.ACTION_DOWN) { finish(); true } else false } }
        root.addView(TextClock(this).apply { format24Hour="HH:mm"; textSize=48f; setTextColor(0xff8a8e95.toInt()) },FrameLayout.LayoutParams(-2,-2,Gravity.CENTER)); setContentView(root); root.requestFocus()
    }
    override fun dispatchKeyEvent(event:KeyEvent):Boolean { if(::remote.isInitialized && NightAccessibilityService.current==null)remote.onKey(event); return super.dispatchKeyEvent(event) }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); if(::remote.isInitialized)remote.close(); super.onDestroy() }
}
