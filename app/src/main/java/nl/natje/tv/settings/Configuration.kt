package nl.natje.tv.settings

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime

data class NightProfile(val name: String, val warmth: Int, val dim: Int)

data class Configuration(
    val onboarding: Boolean = false,
    val order: List<String> = emptyList(),
    val favorites: List<String> = emptyList(),
    val excludedFavorites: Set<String> = emptySet(),
    val hidden: Set<String> = emptySet(),
    val bindings: Map<String, String> = emptyMap(),
    val artwork: Map<String, String> = emptyMap(),
    val categories: Map<String, String> = emptyMap(),
    val wallpaper: String = "default1",
    val theme: String = "dark",
    val nightMode: String = "off",
    val backend: String = "application",
    val warmth: Int = 45,
    val dim: Int = 30,
    val profile: String = "Avond",
    val profiles: List<NightProfile> = defaults,
    val start: Int = 22 * 60,
    val end: Int = 7 * 60,
    val chord: String = "menu_down",
    val observedKeys: Set<Int> = emptySet(),
    val autoStart: String = "",
    val sleepMinutes: Int = 0,
    val ps5: Boolean = false,
    val sounds: Boolean = true
) {
    fun nightActive(time: LocalTime = LocalTime.now()): Boolean = when (nightMode) {
        "on" -> true
        "schedule" -> inSchedule(time.hour * 60 + time.minute, start, end)
        else -> false
    }
    companion object {
        val defaults = listOf(NightProfile("Normaal",25,15), NightProfile("Avond",45,30), NightProfile("Extra Warm",75,40), NightProfile("Film",15,45))
        fun inSchedule(now: Int, start: Int, end: Int): Boolean = when {
            start == end -> false
            start < end -> now >= start && now < end
            else -> now >= start || now < end
        }
    }
}

/** One versioned atomic preference record. No URIs are exported: grants are device local. */
class ConfigStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("natje_tv", Context.MODE_PRIVATE)
    fun load(): Configuration {
        val raw=prefs.getString("configuration",null) ?: return Configuration()
        return synchronized(lock) { if(raw==cachedRaw) cachedValue else runCatching { decode(raw) }.getOrDefault(Configuration()).also { cachedRaw=raw; cachedValue=it } }
    }
    fun save(value: Configuration) {
        val raw=encode(value).toString()
        synchronized(lock) { cachedRaw=raw; cachedValue=value; prefs.edit().putString("configuration",raw).apply() }
    }
    fun update(transform: (Configuration) -> Configuration): Configuration = synchronized(lock) { transform(load()).also(::save) }
    fun export(): String = encode(load().copy(wallpaper = "default1", artwork = emptyMap(), observedKeys = emptySet())).toString(2)
    fun import(text: String) {
        require(text.length <= 131072) { "Configuratie te groot" }
        val decoded = decode(text)
        // Import never starts a service, requests a permission or auto-launches an app.
        save(decoded.copy(nightMode = "off", backend = "application", autoStart = "", artwork = emptyMap(), wallpaper = "default1"))
    }
    companion object {
        private val lock = Any()
        private var cachedRaw:String?=null
        private var cachedValue=Configuration()
        private fun list(a: JSONArray?): List<String> = if (a == null) emptyList() else (0 until minOf(a.length(),256)).map { a.getString(it).take(256) }.distinct()
        private fun map(o: JSONObject?): Map<String,String> = o?.keys()?.asSequence()?.take(256)?.associateWith { o.getString(it).take(1024) } ?: emptyMap()
        fun encode(c: Configuration): JSONObject = JSONObject().apply {
            put("schema",1); put("onboarding",c.onboarding)
            put("order",JSONArray(c.order)); put("favorites",JSONArray(c.favorites)); put("excludedFavorites",JSONArray(c.excludedFavorites.toList())); put("hidden",JSONArray(c.hidden.toList()))
            put("bindings",JSONObject(c.bindings)); put("artwork",JSONObject(c.artwork)); put("categories",JSONObject(c.categories))
            put("wallpaper",c.wallpaper); put("theme",c.theme); put("nightMode",c.nightMode); put("backend",c.backend)
            put("warmth",c.warmth); put("dim",c.dim); put("profile",c.profile)
            put("profiles",JSONArray().apply { c.profiles.forEach { put(JSONObject().put("name",it.name).put("warmth",it.warmth).put("dim",it.dim)) } })
            put("start",c.start); put("end",c.end); put("chord",c.chord); put("observedKeys",JSONArray(c.observedKeys.map { it.toString() }))
            put("autoStart",c.autoStart); put("sleepMinutes",c.sleepMinutes); put("ps5",c.ps5); put("sounds",c.sounds)
        }
        fun decode(text: String): Configuration {
            val o = JSONObject(text)
            require(o.optInt("schema") == 1) { "Onbekende configuratieversie" }
            fun choice(key: String, default: String, choices: Set<String>) = o.optString(key,default).also { require(it in choices) { "Ongeldige instelling: $key" } }
            val p = o.optJSONArray("profiles")
            val profiles = if(p == null) Configuration.defaults else (0 until minOf(p.length(),20)).map { i ->
                val item = p.getJSONObject(i)
                NightProfile(item.getString("name").trim().take(32).also { require(it.isNotBlank()) },item.optInt("warmth",45).coerceIn(0,100),item.optInt("dim",30).coerceIn(0,80))
            }.distinctBy { it.name }.ifEmpty { Configuration.defaults }
            return Configuration(
                onboarding=o.optBoolean("onboarding"), order=list(o.optJSONArray("order")), favorites=list(o.optJSONArray("favorites")), excludedFavorites=list(o.optJSONArray("excludedFavorites")).toSet(), hidden=list(o.optJSONArray("hidden")).toSet(),
                bindings=map(o.optJSONObject("bindings")),artwork=map(o.optJSONObject("artwork")),categories=map(o.optJSONObject("categories")),wallpaper=o.optString("wallpaper","default1"),
                theme=choice("theme","dark",setOf("dark","light","auto")),nightMode=choice("nightMode","off",setOf("off","on","schedule")),backend=choice("backend","application",setOf("application","accessibility")),
                warmth=o.optInt("warmth",45).coerceIn(0,100),dim=o.optInt("dim",30).coerceIn(0,80),profile=o.optString("profile","Avond").take(32),profiles=profiles,
                start=o.optInt("start",1320).coerceIn(0,1439),end=o.optInt("end",420).coerceIn(0,1439),chord=choice("chord","menu_down",setOf("menu_down","menu_up","none")),
                observedKeys=list(o.optJSONArray("observedKeys")).mapNotNull { it.toIntOrNull() }.toSet(),autoStart=o.optString("autoStart",""),sleepMinutes=o.optInt("sleepMinutes",0).coerceIn(0,180),ps5=o.optBoolean("ps5"),sounds=o.optBoolean("sounds",true)
            )
        }
    }
}
