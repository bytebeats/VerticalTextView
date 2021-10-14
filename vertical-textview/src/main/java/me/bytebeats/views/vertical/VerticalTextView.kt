package me.bytebeats.views.vertical

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.FontMetrics
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.absoluteValue


/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created at 2021/10/14 11:00
 * @Version 1.0
 * @Description VerticalTextView displays text vertically.
 */

class VerticalTextView @JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    var text: String? = ""
    var textColor = 0x000000
    var textSize = sp2px(context, 14F)
    var rowSpacing = 0
    var columnSpacing = dp2px(context, 4F)
    var columnCharCount = 0
    var maxColumns = 0
    var textStyle = 0
    var textGravity = Gravity.LEFT//字符是否居中展示
    var includeFontPadding = true //是否使用包裹字体的高度，减少底部可能出现的空白区域

    private var ellipsisPaint: Paint? = null
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var mMeasureWidth = 0
    private var mMeasureHeight = 0
    private val columnTexts = mutableListOf<String>()

    var isShowEllipsis = false
    var charWidth = 0
    var charHeight = 0
    private var fontMetrics: FontMetrics? = null

    private var lastShowColumnIndex = -1

    init {
        // Load attributes
        val a = getContext().obtainStyledAttributes(attrs, R.styleable.VerticalTextView, defStyleAttr, 0)

        text = a.getString(R.styleable.VerticalTextView_text)
        textColor = a.getColor(R.styleable.VerticalTextView_textColor, Color.BLACK)
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        textSize = a.getDimensionPixelSize(R.styleable.VerticalTextView_textSize, textSize)
        rowSpacing = a.getDimensionPixelSize(R.styleable.VerticalTextView_rowSpacing, rowSpacing)
        columnSpacing = a.getDimensionPixelSize(R.styleable.VerticalTextView_columnSpacing, columnSpacing)
        columnCharCount = a.getInteger(R.styleable.VerticalTextView_columnCharCount, -1)
        maxColumns = a.getInteger(R.styleable.VerticalTextView_maxColumns, -1)
        includeFontPadding = a.getBoolean(R.styleable.VerticalTextView_includeFontPadding, true)
        textGravity = Gravity.values()[a.getInt(R.styleable.VerticalTextView_textGravity, 0)]
        textStyle = a.getInt(R.styleable.VerticalTextView_textStyle, textStyle)
        a.recycle()
        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasure()
    }

    private fun invalidateTextPaintAndMeasure() {
        lastShowColumnIndex = -1
        isShowEllipsis = false
        columnTexts.clear()
        fontMetrics = null
        invalidateTextPaint()
        invalidateMeasure()
    }

    private fun invalidateTextPaint() {
        if (text.isNullOrEmpty()) {
            return
        }
        textPaint.textSize = textSize.toFloat()
        textPaint.color = textColor
        textPaint.textAlign = if (textGravity.isCenter()) Paint.Align.CENTER else Paint.Align.LEFT
        textPaint.isFakeBoldText = textStyle and Typeface.BOLD != 0
        textPaint.textSkewX = if (textStyle and Typeface.ITALIC != 0) -0.25F else 0F
        fontMetrics = textPaint.fontMetrics

        if (maxColumns > 0) {
            if (ellipsisPaint == null) {
                ellipsisPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val typeface = Typeface.createFromAsset(context.assets, "fonts/vertical_ellipsis.TTF")
                ellipsisPaint?.typeface = typeface
                ellipsisPaint?.isFakeBoldText = textStyle and Typeface.BOLD != 0
                ellipsisPaint?.textSkewX = if (textStyle and Typeface.ITALIC != 0) -0.25F else 0F
            }
            ellipsisPaint?.textSize = textSize.toFloat()
            ellipsisPaint?.color = textColor
            ellipsisPaint?.textAlign = if (textGravity.isCenter()) Paint.Align.CENTER else Paint.Align.LEFT
        }
    }

    private fun invalidateMeasure() {
        if (text.isNullOrEmpty()) {
            return
        }
        text?.forEach { c ->
            val width = textPaint.measureText("$c")
            if (charWidth < width) {
                charWidth = width.toInt()
            }
        }
    }

    private fun updateColumnTexts(charCount: Int) {
        columnTexts.clear()
        var i = charCount
        while (i < text!!.length) {
            columnTexts.add(text!!.substring(i - charCount, i))
            i += charCount
        }
        if (i - charCount < text!!.length) {
            columnTexts.add(text!!.substring(i - charCount))
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (heightMode == MeasureSpec.EXACTLY) {
            mMeasureHeight = heightSize - paddingTop - paddingBottom
        } else {
            if (text.isNullOrEmpty()) {
                mMeasureHeight = 0
            } else {
                mMeasureHeight = heightSize - paddingTop - paddingBottom
                /*
                 * bug fix 当parent是RelativeLayout时，RelativeLayout onMeasure会测量两次，
                 * 当自定义view宽或高设置为wrap_content时，会出现计算出错，显示异常。这是由于
                 * 第一次调用时宽高mode默认是wrap_content类型，size会是parent size。这将导致
                 * 自定义view第一次计算出的size不是我们需要的值，影响第二次正常计算。
                 */
                if ((layoutParams?.height ?: 0) > 0) {
                    mMeasureHeight = layoutParams.height
                }
                if (columnCharCount > 0) {
                    mMeasureHeight = Int.MIN_VALUE
                    updateColumnTexts(columnCharCount)
                    for (i in 0 until columnTexts.size) {
                        mMeasureHeight = mMeasureHeight.coerceAtLeast(charHeight * columnTexts[i].length)
                    }
                } else {
                    mMeasureHeight = mMeasureHeight.coerceAtMost(charHeight * text!!.length)
                }
            }
        }
        if (widthMode == MeasureSpec.EXACTLY) {
            mMeasureWidth = widthSize - paddingStart - paddingEnd
            if (charHeight > 0) {
                val charCount = (mMeasureHeight - charHeight) / (charHeight + rowSpacing) + 1//一列的字符个数
                updateColumnTexts(charCount)
            }
        } else {
            if (text.isNullOrEmpty()) {
                mMeasureWidth = 0
            } else {
                if (charHeight > 0) {
                    var charCount = 1
                    if (columnCharCount > 0) {
                        charCount = columnCharCount
                        includeFontPadding = true
                    } else if (mMeasureHeight > 0) {
                        charCount = (mMeasureHeight - charHeight) / (charHeight + rowSpacing) + 1//一列的字符个数
                    }
                    updateColumnTexts(charCount)
                    if (includeFontPadding) {
                        mMeasureHeight =
                            (charHeight + rowSpacing) * (charCount - 1) + charHeight + fontMetrics!!.descent.absoluteValue.toInt()
                    }
                    var column = columnTexts.size
                    if (maxColumns > 0) {
                        if (column > maxColumns) {
                            isShowEllipsis = true
                            column = maxColumns
                            lastShowColumnIndex = maxColumns
                        } else {
                            lastShowColumnIndex = column
                        }
                    }
                    mMeasureWidth = if (lastShowColumnIndex > 0) {
                        (charWidth + columnSpacing) * (lastShowColumnIndex - 1) + charWidth
                    } else {
                        (charWidth + columnSpacing) * (column - 1) + charWidth
                    }
                } else {
                    mMeasureWidth = suggestedMinimumWidth
                }
            }
        }

        setMeasuredDimension(mMeasureWidth, mMeasureHeight)
    }

    override fun onDraw(canvas: Canvas?) {
        var x = 0
        var y = 0
        if (columnTexts.isEmpty()) {
            return
        }
        for (i in 0 until columnTexts.size) {
            x = if (i == 0) paddingLeft else x + charWidth + columnSpacing
            val chars = columnTexts[i]
            val isLastColumn = i == lastShowColumnIndex - 1
            for (j in 0 until chars.length) {
                y =
                    if (j == 0) paddingTop + fontMetrics!!.descent.absoluteValue.toInt() else y + charHeight + rowSpacing
                if (lastShowColumnIndex == maxColumns && isShowEllipsis && j == chars.length - 1 && isLastColumn) {
                    canvas?.drawText(
                        "\uE606",
                        (if (textGravity.isCenter()) x + charWidth / 2 + 1 else x).toFloat(),
                        y.toFloat(),
                        ellipsisPaint!!
                    )
                    return
                } else {
                    canvas?.drawText(
                        "${chars[j]}",
                        (if (textGravity.isCenter()) x + charWidth / 2 + 1 else x).toFloat(),
                        y.toFloat(),
                        textPaint
                    )
                }
            }
        }
    }

    fun typeface(typeface: Typeface?, style: Int) {
        if (style > 0) {
            typeface ?: return
            val tf = Typeface.create(typeface, style)
            typeface(tf)
            val tfStyle = tf.style ?: 0
            val need = style and tfStyle.inv()
            textPaint.apply {
                isFakeBoldText = need and Typeface.BOLD != 0
                textSkewX = if (need and Typeface.ITALIC != 0) -0.25F else 0F
            }
        } else {
            textPaint.apply {
                isFakeBoldText = false
                textSkewX = 0F
            }
            typeface(typeface)
        }
    }

    fun typeface(typeface: Typeface?) {
        textPaint.typeface = typeface ?: Typeface.DEFAULT
    }

    fun text(text: String?) {
        this.text = text
        invalidateTextPaintAndMeasure()
    }

    fun textSize(size: Int) {
        textSize = size
        invalidateTextPaintAndMeasure()
    }

    fun textColor(color: Int) {
        textColor = color
        invalidateTextPaintAndMeasure()
    }

    fun rowSpacing(spacing: Int) {
        rowSpacing = spacing
        invalidateTextPaintAndMeasure()
    }

    fun columnSpacing(spacing: Int) {
        columnSpacing = spacing
        invalidateTextPaintAndMeasure()
    }

    private fun sp2px(context: Context, sp: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics
    ).toInt()

    private fun dp2px(context: Context, dp: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics
    ).toInt()

    enum class Gravity {
        LEFT, CENTER;

        fun isCenter(): Boolean = this == CENTER
    }

}