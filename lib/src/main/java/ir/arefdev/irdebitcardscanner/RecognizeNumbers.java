package ir.arefdev.irdebitcardscanner;

import android.graphics.Bitmap;

import java.util.ArrayList;

class RecognizeNumbers {

    private final Bitmap image;
    private final RecognizedDigits[][] recognizedDigits;

    RecognizeNumbers(Bitmap image, int numRows, int numCols) {
        this.image = image;
        this.recognizedDigits = new RecognizedDigits[numRows][numCols];
    }

    String number(RecognizedDigitsModel model, ArrayList<ArrayList<DetectedBox>> lines) {
        for (ArrayList<DetectedBox> line : lines) {
            StringBuilder candidateNumber = new StringBuilder();

            for (DetectedBox word : line) {
                RecognizedDigits recognized = this.cachedDigits(model, word);
                if (recognized == null) {
                    return null;
                }

                candidateNumber.append(recognized.stringResult());
            }

            if (candidateNumber.length() == 16 && DebitCardUtils.INSTANCE.luhnCheck(candidateNumber.toString())) {
                return candidateNumber.toString();
            }
        }

        return null;
    }

    private RecognizedDigits cachedDigits(RecognizedDigitsModel model, DetectedBox box) {
        if (this.recognizedDigits[box.getRow()][box.getCol()] == null) {
            this.recognizedDigits[box.getRow()][box.getCol()] = RecognizedDigits.from(model, image, box.getRect());
        }

        return this.recognizedDigits[box.getRow()][box.getCol()];
    }

}
