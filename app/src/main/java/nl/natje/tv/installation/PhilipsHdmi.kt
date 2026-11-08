package nl.natje.tv.installation

import android.content.Context
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.*

/** Minimal MQTT 5 QoS0 remote keys. Credentials are private files, never exported. */
class PhilipsHdmi(private val context:Context) {
    fun switchSource(port:Int?) {
        require(port==null || port in 1..4)
        val c=JSONObject(java.io.File(context.filesDir,"hdmi_private.json").readText())
        val host=c.getString("host"); require(host.matches(Regex("[0-9.]+")))
        val expected=c.getString("certificateSha256")
        val trust=object:X509TrustManager {
            override fun getAcceptedIssuers()=emptyArray<X509Certificate>()
            override fun checkClientTrusted(chain:Array<X509Certificate>,auth:String) { throw java.security.cert.CertificateException("Client authentication unsupported") }
            override fun checkServerTrusted(chain:Array<X509Certificate>,auth:String) {
                val actual=MessageDigest.getInstance("SHA-256").digest(chain.first().encoded).joinToString("") { "%02x".format(it.toInt() and 255) }
                if(actual!=expected)throw java.security.cert.CertificateException("TV certificate changed")
            }
        }
        val tls=SSLContext.getInstance("TLS").apply { init(null,arrayOf(trust),null) }
        val raw=Socket().apply { connect(InetSocketAddress(host,8883),4000); soTimeout=4000 }
        (tls.socketFactory.createSocket(raw,host,8883,true) as SSLSocket).use { socket ->
            socket.soTimeout=4000; socket.startHandshake()
            val output=socket.outputStream; val input=socket.inputStream
            fun packet(type:Int,body:ByteArray) { output.write(type); var size=body.size; do { var b=size%128; size/=128; if(size>0)b=b or 128; output.write(b) } while(size>0); output.write(body); output.flush() }
            fun text(s:String):ByteArray { val b=s.toByteArray(Charsets.UTF_8); require(b.size<65536); return byteArrayOf((b.size ushr 8).toByte(),b.size.toByte())+b }
            val connect=ByteArrayOutputStream().apply { write(text("MQTT")); write(5); write(0xc2); write(0); write(30); write(0); write(text("eos-"+c.getString("username"))); write(text(c.getString("username"))); write(text(c.getString("password"))) }.toByteArray()
            packet(0x10,connect)
            require(input.read()==0x20) { "No MQTT acknowledgement" }
            var remaining=0; var multiplier=1; var b:Int
            do { b=input.read(); require(b>=0); remaining+=(b and 127)*multiplier; multiplier*=128; require(multiplier<=268435456) } while(b and 128!=0)
            require(remaining in 2..4096); val ack=ByteArray(remaining); var received=0
            while(received<remaining) { val n=input.read(ack,received,remaining-received); require(n>0); received+=n }
            require(ack[1].toInt()==0) { "TV pairing rejected" }
            fun key(code:Int,wait:Long=350) { packet(0x30,text("rc")+byteArrayOf(0)+JSONObject().put("key_code",code).toString().toByteArray(Charsets.UTF_8)); Thread.sleep(wait) }
            // Pinned to the bottom of the source drawer before choosing an HDMI entry.
            // No OK is sent on intermediate entries such as screen mirroring.
            key(102,2000); key(394,2000)
            if(port!=null) { repeat(12) { key(108) }; repeat(4-port) { key(103) }; key(352) }
            packet(0xe0,byteArrayOf())
        }
    }
}
