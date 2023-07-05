package ir.arefdev.irdebitcardscanner

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import org.tensorflow.lite.Interpreter

/**
 * Classifies images with Tensorflow Lite.
 */
abstract class ImageClassifier(context: Context) {
    /**
     * Preallocated buffers for storing image data in.
     */
    private val intValues = IntArray(imageSizeX * imageSizeY)

    /**
     * Options for configuring the Interpreter.
     */
    private val tfliteOptions = Interpreter.Options()

    /**
     * The loaded TensorFlow Lite model.
     */
    private var tfliteModel: MappedByteBuffer? = null

    /**
     * An instance of the driver class to run model inference with Tensorflow Lite.
     */
    var tflite: Interpreter? = null

    /**
     * A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs.
     */
    var imgData: ByteBuffer? = null

    init {
        tfliteModel = loadModelFile(context)
        tflite = Interpreter(tfliteModel!!, tfliteOptions)
        imgData = ByteBuffer.allocateDirect(
            DIM_BATCH_SIZE * imageSizeX * imageSizeY * DIM_PIXEL_SIZE * numBytesPerChannel
        )
        imgData?.order(ByteOrder.nativeOrder())
    }

    /**
     * Classifies a frame from the preview stream.
     */
    fun classifyFrame(bitmap: Bitmap) {
        if (tflite == null) Log.e(TAG, "Image classifier has not been initialized; Skipped.")
        convertBitmapToByteBuffer(bitmap)
        // Here's where the magic happens!!!
        runInference()
    }

    private fun recreateInterpreter() {
        if (tflite != null) {
            tflite?.close()
            tfliteModel?.let { tflite = Interpreter((it), tfliteOptions) }
        }
    }

    fun setNumThreads(numThreads: Int) {
        tfliteOptions.setNumThreads(numThreads)
        recreateInterpreter()
    }

    /**
     * Memory-map the model file in Assets.
     */
    @Throws(IOException::class)
    abstract fun loadModelFile(context: Context?): MappedByteBuffer?

    /**
     * Writes Image data into a `ByteBuffer`.
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        if (imgData == null) {
            return
        }
        imgData?.rewind()
        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            imageSizeX,
            imageSizeY, false
        )
        resizedBitmap.getPixels(
            intValues, 0, resizedBitmap.width, 0, 0,
            resizedBitmap.width, resizedBitmap.height
        )
        // Convert the image to floating point.
        var pixel = 0
        for (i in 0 until imageSizeX) {
            for (j in 0 until imageSizeY) {
                val data = intValues[pixel++]
                addPixelValue(data)
            }
        }
    }

    /**
     * Get the image size along the x axis.
     */
    protected abstract val imageSizeX: Int

    /**
     * Get the image size along the y axis.
     */
    protected abstract val imageSizeY: Int

    /**
     * Get the number of bytes that is used to store a single color channel value.
     */
    protected abstract val numBytesPerChannel: Int

    /**
     * Add pixelValue to byteBuffer.
     */
    protected abstract fun addPixelValue(pixelValue: Int)

    /**
     * Run inference using the prepared input in [.imgData]. Afterwards, the result will be
     * provided by getProbability().
     *
     *
     * This additional method is necessary, because we don't have a common base for different
     * primitive data types.
     */
    protected abstract fun runInference()

    companion object {
        /**
         * Tag for the [Log].
         */
        private const val TAG = "CardScan"

        /**
         * Dimensions of inputs.
         */
        private const val DIM_BATCH_SIZE = 1
        private const val DIM_PIXEL_SIZE = 3
    }
}
