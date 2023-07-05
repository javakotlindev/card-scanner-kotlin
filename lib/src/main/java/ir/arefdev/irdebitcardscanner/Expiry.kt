package ir.arefdev.irdebitcardscanner

import android.graphics.Bitmap

class Expiry {
    var data: String? = null
        private set
    var month = 0
        private set
    var year = 0
        private set

    fun format(): String {
        val result = StringBuilder()
        for (i in 0 until (data?.length ?: 0)) {
            if (i == 4) result.append("/")
            data?.getOrNull(i)?.let(result::append)
        }
        return result.toString()
    }

    companion object {
        fun from(model: RecognizedDigitsModel?, image: Bitmap?, box: CGRect?): Expiry? {
            val digits = RecognizedDigits.from(model, image, box)
            val data = digits.stringResult()
            if (data.length != 6) return null
            val monthString = data.substring(4)
            val yearString = data.substring(0, 3)
            return try {
                val month = monthString.toInt()
                val year = yearString.toInt()
                if (month <= 0 || month > 12) {
                    return null
                }
                val fullYear = (if (year > 90) 1300 else 1400) + year
                val expiry = Expiry()
                expiry.month = month
                expiry.year = fullYear
                expiry.data = data
                expiry
            } catch (nfe: NumberFormatException) {
                null
            }
        }
    }
}
