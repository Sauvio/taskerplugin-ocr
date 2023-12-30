package io.github.sauvio.ocr.tasker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import com.google.mlkit.vision.text.Text
import java.util.Arrays
import kotlin.math.max
import kotlin.math.min

class InMemoryImageDrawer constructor(
    drawer: ImageDrawer,
    private val context: Context,
    private val text: Text,
    private val shouldGroupTextInBlocks: Boolean,
    private val showLanguageTag: Boolean,
    private val showConfidence: Boolean
) : ImageDrawer.Drawer(drawer) {
    private val rectPaint: Paint = Paint()
    private val textPaint: Paint
    private val labelPaint: Paint

    private val paint: Paint = Paint()

    init {
        rectPaint.color = MARKER_COLOR
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = STROKE_WIDTH
        textPaint = Paint()
        textPaint.color = TEXT_COLOR
        textPaint.textSize = TEXT_SIZE
        textPaint.style = Paint.Style.FILL
        textPaint.strokeWidth = STROKE_WIDTH
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isAntiAlias = true
        textPaint.strokeJoin = Paint.Join.ROUND
        textPaint.isDither = true
        labelPaint = Paint()
        labelPaint.color = BACKGROUND_COLOR
        labelPaint.style = Paint.Style.FILL
        drawer.awareResolution(getScreenResolution(context))
        // Redraw the image, as this drawer has been added.
        postInvalidate()

    }



    fun getScreenResolution(context: Context): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displayMetrics = DisplayMetrics()
        val displays: Array<out Display> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            displayManager.displays
        } else {
            arrayOf(windowManager.defaultDisplay)
        }
        val display = displays.firstOrNull()
        display?.getRealMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        return Pair(screenWidth, screenHeight)
    }

    /** Draws the text block annotations for position, size, and raw value on the supplied canvas. */
    override fun draw(canvas: Canvas) {
        Log.d(TAG, "Text is: " + text.text)
        for (textBlock in text.textBlocks) { // Renders the text at the bottom of the box.
            Log.d(TAG, "TextBlock text is: " + textBlock.text)
            Log.d(TAG, "TextBlock boundingbox is: " + textBlock.boundingBox)
            Log.d(TAG, "TextBlock cornerpoint is: " + Arrays.toString(textBlock.cornerPoints))
            if (shouldGroupTextInBlocks) {
                drawText(
                    getFormattedText(
                        textBlock.text,
                        textBlock.recognizedLanguage,
                        confidence = null
                    ),
                    RectF(textBlock.boundingBox),
                    TEXT_SIZE * textBlock.lines.size + 2 * STROKE_WIDTH,
                    canvas
                )
            } else {
                for (line in textBlock.lines) {
                    Log.d(TAG, "Line text is: " + line.text)
                    Log.d(TAG, "Line boundingbox is: " + line.boundingBox)
                    Log.d(TAG, "Line cornerpoint is: " + Arrays.toString(line.cornerPoints))
                    Log.d(TAG, "Line confidence is: " + line.confidence)
                    Log.d(TAG, "Line angle is: " + line.angle)
                    // Draws the bounding box around the TextBlock.
                    val rect = RectF(line.boundingBox)
                    drawText(
                        getFormattedText(line.text, line.recognizedLanguage, line.confidence),
                        rect,
                        TEXT_SIZE + 2 * STROKE_WIDTH,
                        canvas
                    )
                    for (element in line.elements) {
                        Log.d(TAG, "Element text is: " + element.text)
                        Log.d(TAG, "Element boundingbox is: " + element.boundingBox)
                        Log.d(
                            TAG,
                            "Element cornerpoint is: " + Arrays.toString(element.cornerPoints)
                        )
                        Log.d(TAG, "Element language is: " + element.recognizedLanguage)
                        Log.d(TAG, "Element confidence is: " + element.confidence)
                        Log.d(TAG, "Element angle is: " + element.angle)
                        for (symbol in element.symbols) {
                            Log.d(TAG, "Symbol text is: " + symbol.text)
                            Log.d(TAG, "Symbol boundingbox is: " + symbol.boundingBox)
                            Log.d(
                                TAG,
                                "Symbol cornerpoint is: " + Arrays.toString(symbol.cornerPoints)
                            )
                            Log.d(TAG, "Symbol confidence is: " + symbol.confidence)
                            Log.d(TAG, "Symbol angle is: " + symbol.angle)
                        }
                    }
                }
            }
        }
    }

    private fun getFormattedText(text: String, languageTag: String, confidence: Float?): String {
        val res =
            if (showLanguageTag) String.format(
                TEXT_WITH_LANGUAGE_TAG_FORMAT,
                languageTag,
                text
            ) else text
        return if (showConfidence && confidence != null) String.format("%s (%.2f)", res, confidence)
        else res
    }

    private fun drawText(text: String, rect: RectF, textHeight: Float, canvas: Canvas) {
        // Translate and scale the bounding box coordinates
        val left = translateX(rect.left)
        val top = translateY(rect.top)
        val right = translateX(rect.right)
        val bottom = translateY(rect.bottom)

        // If the image is flipped, adjust left and right
        val flippedLeft = if (isImageFlipped()) canvas.width.toFloat() - right else left
        val flippedRight = if (isImageFlipped()) canvas.width.toFloat() - left else right

        rect.left = min(flippedLeft, flippedRight)
        rect.right = max(flippedLeft, flippedRight)
        rect.top = min(top, bottom)
        rect.bottom = max(top, bottom)

        // Draw the bounding box
        canvas.drawRect(rect, rectPaint)

        // Draw the label background
        /**
        canvas.drawRect(
        rect.left - STROKE_WIDTH,
        rect.top - textHeight,
        rect.left + textPaint.measureText(text) + 2 * STROKE_WIDTH,
        rect.top,
        labelPaint
        )
         **/
        // Draw the text
        canvas.drawText(text, rect.left, rect.top - STROKE_WIDTH, textPaint)
    }

    companion object {
        private const val TAG = "InMemoryImageDrawer"
        private const val TEXT_WITH_LANGUAGE_TAG_FORMAT = "%s:%s"
        private const val TEXT_COLOR = Color.GREEN
        private const val MARKER_COLOR = Color.BLUE
        private val BACKGROUND_COLOR = Color.parseColor("#00000000")
        private const val TEXT_SIZE = 20.0f
        private const val STROKE_WIDTH = 1.0f
    }
}