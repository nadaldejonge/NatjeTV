package nl.natje.tv.ui
import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.*
class HeaderIcon(context:Context,private val symbol:Int):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    init { isFocusable=true; isFocusableInTouchMode=true; isClickable=true; setOnFocusChangeListener { _,_->invalidate() } }
    override fun onDraw(canvas:Canvas) {
        val cx=width/2f; val cy=height/2f
        paint.style=Paint.Style.FILL; paint.color=if(hasFocus())0xff444950.toInt() else 0xcc1a1c21.toInt()
        canvas.drawCircle(cx,cy,width*.47f,paint)
        paint.style=Paint.Style.STROKE; paint.strokeWidth=context.dp(1.5f).toFloat(); paint.color=Color.WHITE; paint.strokeCap=Paint.Cap.ROUND
        if(hasFocus())canvas.drawCircle(cx,cy,width*.47f,paint)
        canvas.save(); canvas.translate(cx,cy); canvas.scale(width/36f,height/36f); paint.strokeWidth=1.8f
        when(symbol) {
            0->{ canvas.drawCircle(-2f,-2f,6f,paint); canvas.drawLine(2.5f,2.5f,8f,8f,paint) }
            1->{ canvas.drawCircle(0f,0f,6f,paint); canvas.drawCircle(0f,0f,2f,paint); for(i in 0..7) { val a=i*PI/4; canvas.drawLine((cos(a)*6).toFloat(),(sin(a)*6).toFloat(),(cos(a)*9).toFloat(),(sin(a)*9).toFloat(),paint) } }
            2->{ paint.style=Paint.Style.FILL; val moon=Path().apply { addCircle(0f,0f,8f,Path.Direction.CW) }; moon.op(Path().apply { addCircle(4f,-3f,7f,Path.Direction.CW) },Path.Op.DIFFERENCE); canvas.drawPath(moon,paint) }
        }
        canvas.restore()
    }
}
