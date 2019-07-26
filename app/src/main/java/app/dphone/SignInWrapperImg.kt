package app.dphone

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View


class OutsideOvalColoredImg(context: Context, attrs: AttributeSet) : View (context, attrs) {
    private lateinit var bm: Bitmap
    private lateinit var cv: Canvas
    private var eraser: Paint

    init {
        eraser = Paint(Paint.ANTI_ALIAS_FLAG)
        eraser.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w != oldw || h != oldh) {
            bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            cv = Canvas(bm)
        }
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        bm.eraseColor(Color.TRANSPARENT)
        cv.drawColor(Color.WHITE)
        cv.drawOval(-1f * w, -4f * h, 2f * w, h, eraser)
        canvas.drawBitmap(bm, 0f, 0f, null)
        super.onDraw(canvas)
    }
}