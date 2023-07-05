package ir.arefdev.irdebitcardscanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Organize the boxes to find possible numbers.
 * <p>
 * After running detection, the post processing algorithm will try to find
 * sequences of boxes that are plausible card numbers. The basic techniques
 * that it uses are non-maximum suppression and depth first search on box
 * sequences to find likely numbers. There are also a number of heuristics
 * for filtering out unlikely sequences.
 */
class PostDetectionAlgorithm {

    private static final Comparator<DetectedBox> colCompare = new Comparator<DetectedBox>() {
        @Override
        public int compare(DetectedBox o1, DetectedBox o2) {
            return (o1.getCol() < o2.getCol()) ? -1 : ((o1.getCol() == o2.getCol()) ? 0 : 1);
        }
    };
    private static final Comparator<DetectedBox> rowCompare = new Comparator<DetectedBox>() {
        @Override
        public int compare(DetectedBox o1, DetectedBox o2) {
            return (o1.getRow() < o2.getRow()) ? -1 : ((o1.getRow() == o2.getRow()) ? 0 : 1);
        }
    };
    private final int kDeltaRowForCombine = 2;
    private final int kDeltaColForCombine = 2;
    private final int numRows;
    private final int numCols;
    private final ArrayList<DetectedBox> sortedBoxes;

    PostDetectionAlgorithm(ArrayList<DetectedBox> boxes, FindFourModel findFour) {
        this.numCols = findFour.getCols();
        this.numRows = findFour.getRows();

        this.sortedBoxes = new ArrayList<>();
        Collections.sort(boxes);
        Collections.reverse(boxes);
        for (DetectedBox box : boxes) {
            int kMaxBoxesToDetect = 20;
            if (this.sortedBoxes.size() >= kMaxBoxesToDetect) {
                break;
            }
            this.sortedBoxes.add(box);
        }
    }

    ArrayList<ArrayList<DetectedBox>> horizontalNumbers() {
        ArrayList<DetectedBox> boxes = this.combineCloseBoxes(kDeltaRowForCombine,
                kDeltaColForCombine);
        int kNumberWordCount = 4;
        ArrayList<ArrayList<DetectedBox>> lines = this.findHorizontalNumbers(boxes, kNumberWordCount);

        ArrayList<ArrayList<DetectedBox>> linesOut = new ArrayList<>();
        // boxes should be roughly evenly spaced, reject any that aren't
        for (ArrayList<DetectedBox> line : lines) {
            ArrayList<Integer> deltas = new ArrayList<>();
            for (int idx = 0; idx < (line.size() - 1); idx++) {
                deltas.add(line.get(idx + 1).getCol() - line.get(idx).getCol());
            }

            Collections.sort(deltas);
            int maxDelta = deltas.get(deltas.size() - 1);
            int minDelta = deltas.get(0);

            if ((maxDelta - minDelta) <= 2) {
                linesOut.add(line);
            }
        }

        return linesOut;
    }

    ArrayList<ArrayList<DetectedBox>> verticalNumbers() {
        ArrayList<DetectedBox> boxes = this.combineCloseBoxes(kDeltaRowForCombine,
                kDeltaColForCombine);
        ArrayList<ArrayList<DetectedBox>> lines = this.findVerticalNumbers(boxes);

        ArrayList<ArrayList<DetectedBox>> linesOut = new ArrayList<>();
        // boxes should be roughly evenly spaced, reject any that aren't
        for (ArrayList<DetectedBox> line : lines) {
            ArrayList<Integer> deltas = new ArrayList<>();
            for (int idx = 0; idx < (line.size() - 1); idx++) {
                deltas.add(line.get(idx + 1).getRow() - line.get(idx).getRow());
            }

            Collections.sort(deltas);
            int maxDelta = deltas.get(deltas.size() - 1);
            int minDelta = deltas.get(0);

            if ((maxDelta - minDelta) <= 2) {
                linesOut.add(line);
            }
        }

        return linesOut;
    }

    private boolean horizontalPredicate(DetectedBox currentWord, DetectedBox nextWord) {
        int kDeltaRowForHorizontalNumbers = 1;
        int deltaRow = kDeltaRowForHorizontalNumbers;
        return nextWord.getCol() > currentWord.getCol() && nextWord.getRow() >= (currentWord.getRow() - deltaRow) &&
                nextWord.getRow() <= (currentWord.getRow() + deltaRow);
    }

    private boolean verticalPredicate(DetectedBox currentWord, DetectedBox nextWord) {
        int kDeltaColForVerticalNumbers = 1;
        int deltaCol = kDeltaColForVerticalNumbers;
        return nextWord.getRow() > currentWord.getRow() && nextWord.getCol() >= (currentWord.getCol() - deltaCol) &&
                nextWord.getCol() <= (currentWord.getCol() + deltaCol);
    }

    private void findNumbers(ArrayList<DetectedBox> currentLine, ArrayList<DetectedBox> words,
                             boolean useHorizontalPredicate, int numberOfBoxes,
                             ArrayList<ArrayList<DetectedBox>> lines) {
        if (currentLine.size() == numberOfBoxes) {
            lines.add(currentLine);
            return;
        }

        if (words.size() == 0) {
            return;
        }

        DetectedBox currentWord = currentLine.get(currentLine.size() - 1);
        if (currentWord == null) {
            return;
        }


        for (int idx = 0; idx < words.size(); idx++) {
            DetectedBox word = words.get(idx);
            if (useHorizontalPredicate && horizontalPredicate(currentWord, word)) {
                ArrayList<DetectedBox> newCurrentLine = new ArrayList<>(currentLine);
                newCurrentLine.add(word);
                findNumbers(newCurrentLine, dropFirst(words, idx + 1), useHorizontalPredicate,
                        numberOfBoxes, lines);
            } else if (verticalPredicate(currentWord, word)) {
                ArrayList<DetectedBox> newCurrentLine = new ArrayList<>(currentLine);
                newCurrentLine.add(word);
                findNumbers(newCurrentLine, dropFirst(words, idx + 1), useHorizontalPredicate,
                        numberOfBoxes, lines);
            }
        }
    }

    private ArrayList<DetectedBox> dropFirst(ArrayList<DetectedBox> boxes, int n) {
        ArrayList<DetectedBox> result = new ArrayList<>();
        for (int idx = n; idx < boxes.size(); idx++) {
            result.add(boxes.get(idx));
        }
        return result;
    }

    // Note: this is simple but inefficient. Since we're dealing with small
    // lists (eg 20 items) it should be fine
    private ArrayList<ArrayList<DetectedBox>> findHorizontalNumbers(ArrayList<DetectedBox> words,
                                                                    int numberOfBoxes) {
        Collections.sort(words, colCompare);
        ArrayList<ArrayList<DetectedBox>> lines = new ArrayList<>();
        for (int idx = 0; idx < words.size(); idx++) {
            ArrayList<DetectedBox> currentLine = new ArrayList<>();
            currentLine.add(words.get(idx));
            findNumbers(currentLine, dropFirst(words, idx + 1), true,
                    numberOfBoxes, lines);
        }

        return lines;
    }

    private ArrayList<ArrayList<DetectedBox>> findVerticalNumbers(ArrayList<DetectedBox> words) {
        int numberOfBoxes = 4;
        Collections.sort(words, rowCompare);
        ArrayList<ArrayList<DetectedBox>> lines = new ArrayList<>();
        for (int idx = 0; idx < words.size(); idx++) {
            ArrayList<DetectedBox> currentLine = new ArrayList<>();
            currentLine.add(words.get(idx));
            findNumbers(currentLine, dropFirst(words, idx + 1), false,
                    numberOfBoxes, lines);
        }

        return lines;
    }

    /**
     * Combine close boxes favoring high confidence boxes.
     */
    private ArrayList<DetectedBox> combineCloseBoxes(int deltaRow, int deltaCol) {
        boolean[][] cardGrid = new boolean[this.numRows][this.numCols];
        for (int row = 0; row < this.numRows; row++) {
            for (int col = 0; col < this.numCols; col++) {
                cardGrid[row][col] = false;
            }
        }

        for (DetectedBox box : this.sortedBoxes) {
            cardGrid[box.getRow()][box.getCol()] = true;
        }

        // since the boxes are sorted by confidence, go through them in order to
        // result in only high confidence boxes winning. There are corner cases
        // where this will leave extra boxes, but that's ok because we don't
        // need to be perfect here
        for (DetectedBox box : this.sortedBoxes) {
            if (!cardGrid[box.getRow()][box.getCol()]) {
                continue;
            }
            for (int row = (box.getRow() - deltaRow); row <= (box.getRow() + deltaRow); row++) {
                for (int col = (box.getCol() - deltaCol); col <= (box.getCol() + deltaCol); col++) {
                    if (row >= 0 && row < this.numRows && col >= 0 && col < this.numCols) {
                        cardGrid[row][col] = false;
                    }
                }
            }

            // add this box back
            cardGrid[box.getRow()][box.getCol()] = true;
        }

        ArrayList<DetectedBox> combinedBoxes = new ArrayList<>();
        for (DetectedBox box : this.sortedBoxes) {
            if (cardGrid[box.getRow()][box.getCol()]) {
                combinedBoxes.add(box);
            }
        }

        return combinedBoxes;
    }
}
