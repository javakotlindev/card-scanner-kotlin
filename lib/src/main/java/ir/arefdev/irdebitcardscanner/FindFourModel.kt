package ir.arefdev.irdebitcardscanner

import android.content.Context
import java.io.IOException
import java.nio.MappedByteBuffer

/**
 * This classifier works with the float MobileNet model.
 */
internal open class FindFourModel(context: Context) : ImageClassifier(context) {
    val rows = 34
    val cols = 51
    val boxSize = CGSize(80f, 36f)
    val cardSize = CGSize(480f, 302f)

    /**
     * An array to hold inference results, to be feed into Tensorflow Lite as outputs. This isn't part
     * of the super class, because we need a primitive array here.
     */
    private val labelProbArray: Array<Array<Array<FloatArray>>>
    fun hasDigits(row: Int, col: Int): Boolean {
        return digitConfidence(row, col) >= 0.5
    }

    fun hasExpiry(row: Int, col: Int): Boolean {
        return expiryConfidence(row, col) >= 0.5
    }

    fun digitConfidence(row: Int, col: Int): Float {
        val digitClass = 1
        return labelProbArray[0][row][col][digitClass]
    }

    fun expiryConfidence(row: Int, col: Int): Float {
        val expiryClass = 2
        return labelProbArray[0][row][col][expiryClass]
    }

    @Throws(IOException::class)
    override fun loadModelFile(context: Context?): MappedByteBuffer? {
        return ResourceModelFactory.getInstance().loadFindFourFile(context)
    }

    override val imageSizeX: Int = 480
    override val imageSizeY: Int = 302

    // Float.SIZE / Byte.SIZE;
    override val numBytesPerChannel: Int = 4 // Float.SIZE / Byte.SIZE;

    init {
        val classes = 3
        labelProbArray = Array(1) { Array(rows) { Array(cols) { FloatArray(classes) } } }
    }

    override fun addPixelValue(pixelValue: Int) {
        imgData?.putFloat((pixelValue shr 16 and 0xFF) / 255f)
        imgData?.putFloat((pixelValue shr 8 and 0xFF) / 255f)
        imgData?.putFloat((pixelValue and 0xFF) / 255f)
    }

    override fun runInference() {
        tflite?.run(imgData, labelProbArray)
    }
}
