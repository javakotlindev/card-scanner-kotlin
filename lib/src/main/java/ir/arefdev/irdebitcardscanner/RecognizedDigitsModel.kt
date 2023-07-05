package ir.arefdev.irdebitcardscanner

import android.content.Context
import java.nio.MappedByteBuffer

class RecognizedDigitsModel(context: Context) : ImageClassifier(context) {
    private val classes = 11

    /**
     * An array to hold inference results, to be feed into Tensorflow Lite as outputs. This isn't part
     * of the super class, because we need a primitive array here.
     */
    private val labelProbArray: Array<Array<Array<FloatArray>>> = Array(1) {
        Array(1) { Array(kNumPredictions) { FloatArray(classes) } }
    }

    inner class ArgMaxAndConfidence(val argMax: Int, val confidence: Float)

    fun argAndValueMax(col: Int): ArgMaxAndConfidence {
        var maxIdx = -1
        var maxValue = (-1.0).toFloat()
        for (idx in 0 until classes) {
            val value = labelProbArray[0][0][col][idx]
            if (value > maxValue) {
                maxIdx = idx
                maxValue = value
            }
        }
        return ArgMaxAndConfidence(maxIdx, maxValue)
    }

    override fun addPixelValue(pixelValue: Int) {
        imgData?.putFloat((pixelValue shr 16 and 0xFF) / 255f)
        imgData?.putFloat((pixelValue shr 8 and 0xFF) / 255f)
        imgData?.putFloat((pixelValue and 0xFF) / 255f)
    }

    override fun runInference() {
        tflite?.run(imgData, labelProbArray)
    }


    override fun loadModelFile(context: Context?): MappedByteBuffer? {
        return ResourceModelFactory.getInstance().loadRecognizeDigitsFile(context)
    }

    override val imageSizeX: Int = 80
    override val imageSizeY: Int = 36
    override val numBytesPerChannel: Int = 4

    companion object {
        const val kNumPredictions = 17
    }
}
