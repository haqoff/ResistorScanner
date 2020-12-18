package ru.haqon;

import android.util.SparseIntArray;

import org.junit.Assert;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.HashSet;

import ru.haqon.resistor.logic.ResistorScanner;

/**
 * Предоставляет вспомогательные методы для создания тестов.
 */
public class TestUtils {

    /**
     * Класс, представляющий сгенерированные область поиска и маску цветов.
     */
    public static class Gen {
        /**
         * Область поиска.
         */
        public final Mat source;

        /**
         * Маска всех цветов.
         */
        public final Mat colorsMask;

        private Gen(Mat source, Mat colorsMask) {
            this.source = source;
            this.colorsMask = colorsMask;
        }
    }

    /**
     * Генерирует изображение с указанными цветами.
     */
    public static Gen generateSimpleMatrixWithColors(Scalar... scalars) {
        final int colorWidth = 5;
        final int toNextColorWidth = 5;

        Mat source = new Mat(50, (colorWidth + toNextColorWidth) * scalars.length, CvType.CV_8UC4);
        Mat mask = new Mat(source.rows(), source.cols(), CvType.CV_8UC1, new Scalar(0));
        int startFromX = 0;

        for (int currentScalarIndex = 0; currentScalarIndex < scalars.length; currentScalarIndex++) {
            for (int x = startFromX; x < startFromX + colorWidth; x++) {
                for (int y = 0; y < source.height(); y++) {
                    source.put(y, x, scalars[currentScalarIndex].val);
                    mask.put(y, x, new Scalar(1).val);
                }
            }

            startFromX += colorWidth + toNextColorWidth;
        }

        return new Gen(source, mask);
    }

    /**
     * Проверяет, что указанные распознанные цвета являются ожидаемыми.
     */
    public static void assertColors(IndexToColorEnum[] expectedNames, SparseIntArray actual, Scalar[] expectedColors) {
        Assert.assertEquals("Кол-во ожидаемых цветов и распознанных не равно.", expectedNames.length, actual.size());

        for (int i = 0; i < expectedNames.length; i++) {
            Assert.assertEquals(
                    String.format("Цвет под индексом %d ожидаемый - %s (%s), но распознанный - %s (координата X - %d)",
                            i,
                            expectedNames[i].name(),
                            expectedColors != null ? expectedColors[i].toString() : null,
                            IndexToColorEnum.getColorByIndex(actual.valueAt(i)).name(),
                            actual.keyAt(i)
                    ),
                    expectedNames[i].getIndex(),
                    actual.valueAt(i));
        }
    }

    public static ArrayList<Scalar> generateNColorInRange(Scalar from, Scalar to, int count) {
        ArrayList<Scalar> result = new ArrayList<>(count);
        if (count <= 0) return result;
        if (count == 1) {
            result.add(genColorByIndexInRange(from, to, 0));
        }

        int totalCountInRange = getTotalRange(from, to);

        // добавляем первый и последний из диапазона
        result.add(genColorByIndexInRange(from, to, 0));
        result.add(genColorByIndexInRange(from, to, totalCountInRange - 1));

        int skip = Math.max(totalCountInRange / count, 1);
        int countToAdd = Math.min(count, totalCountInRange) - 2;

        int currentIndex = skip;
        while (countToAdd > 0) {
            result.add(genColorByIndexInRange(from, to, currentIndex));
            countToAdd--;
            currentIndex += skip;
        }

        return result;
    }

    public static Scalar genColorByIndexInRange(Scalar from, Scalar to, int index) {
        int firstLength = (int) (to.val[0] - from.val[0] + 1);
        int secondLength = (int) (to.val[1] - from.val[1] + 1);
        int thirdLength = (int) (to.val[2] - from.val[2] + 1);

        int thirdOffset = index % thirdLength;
        int secondOffset = (index / thirdLength) % secondLength;
        int firstOffset = ((index / thirdLength) / secondLength) % firstLength;

        return new Scalar(from.val[0] + firstOffset, from.val[1] + secondOffset, from.val[2] + thirdOffset);
    }

    public static int getTotalRange(Scalar from, Scalar to) {
        int firstLength = (int) (to.val[0] - from.val[0] + 1);
        int secondLength = (int) (to.val[1] - from.val[1] + 1);
        int thirdLength = (int) (to.val[2] - from.val[2] + 1);

        return firstLength * secondLength * thirdLength;
    }

    public static HashSet<Scalar> colorsFromName(IndexToColorEnum name, int count) {
        ResistorScanner.Range[] ranges = ResistorScanner.COLOR_BOUNDS[name.getIndex()];
        HashSet<Scalar> scalars = new HashSet<>();

        for (int i = 0; i < ranges.length; i++) {
            ResistorScanner.Range r = ranges[i];
            ArrayList<Scalar> rangeColors = generateNColorInRange(r.from, r.to, count);

            scalars.addAll(rangeColors);
        }

        return scalars;
    }
}
