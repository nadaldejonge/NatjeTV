package nl.natje.tv.installation

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import nl.natje.tv.ui.*
import java.util.concurrent.Executors

class HdmiActivity:Activity() {
    private val worker=Executors.newSingleThreadExecutor()
    private val main=Handler(Looper.getMainLooper())
    private var busy=false
    override fun onCreate(state:Bundle?) {
        super.onCreate(state); fullscreen()
        val column=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(64),dp(30),dp(64),dp(30)); setBackgroundColor(0xff07080b.toInt()) }
        column.row(label("HDMI / PS5",30f)); column.row(label("Kies de bron op je Philips-tv",16f))
        val status=label("PS5: HDMI 3 · Natje TV-stick: HDMI 1",15f); column.row(status)
        fun switch(port:Int?) {
            if(busy)return
            busy=true; status.text=if(port==null)"Bronmenu openen…" else "Naar HDMI $port schakelen…"
            worker.execute {
                val result=runCatching { PhilipsHdmi(this).switchSource(port) }
                main.post { busy=false; status.text=if(result.isSuccess) { if(port==null)"Bronmenu geopend op de televisie" else "HDMI $port aangevraagd op de televisie" } else "Geen verbinding met de gekoppelde Philips-tv. Controleer netwerk en koppeling." }
            }
        }
        column.row(action("PS5 · HDMI 3") { switch(3) })
        column.row(action("HDMI 1 · Natje TV-stick") { switch(1) })
        column.row(action("HDMI 2") { switch(2) })
        column.row(action("HDMI 4") { switch(4) })
        column.row(action("Bronmenu op televisie openen") { switch(null) })
        column.row(action("Terug") { finish() })
        setContentView(ScrollView(this).apply { addView(column) })
    }
    override fun onDestroy() { main.removeCallbacksAndMessages(null); worker.shutdownNow(); super.onDestroy() }
}
