package ru.haqon;

import android.util.SparseIntArray;

import org.junit.Assert;
import org.junit.Test;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import ru.haqon.resistor.logic.ResistorScannerHelper;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class FindResistorColorsTests {
    static {
        OpenCVLoader.initDebug();
    }

    @Test
    public void testFindResistorColors() {

        Mat source = generateSimpleMatrixWithColors(new Scalar(24, 42, 51), new Scalar(0, 0, 0), new Scalar(0, 100, 100));
        SparseIntArray res = ResistorScannerHelper.findResistorColors(source);
        IndexToColorEnum[] expected = {IndexToColorEnum.BROWN, IndexToColorEnum.BLACK, IndexToColorEnum.RED};
        assertColors(expected, res);
    }

    private static Mat generateSimpleMatrixWithColors(Scalar... scalars) {
        Mat m = new Mat(50, 80, CvType.CV_8UC4, new Scalar(42, 51, 58));

        final int columnWidth = 5;
        final int toNextColumnWidth = 5;
        int startFromX = 0;

        for (int currentScalarIndex = 0; currentScalarIndex < scalars.length; currentScalarIndex++) {
            for (int x = startFromX; x < startFromX + columnWidth; x++) {
                for (int y = 0; y < m.height(); y++) {
                    m.put(y, x, scalars[currentScalarIndex].val);
                }
            }

            startFromX += columnWidth + toNextColumnWidth;
        }
        return m;
    }

    private void assertColors(IndexToColorEnum[] expected, SparseIntArray actual) {
        Assert.assertEquals("Кол-во ожидаемых цветов и распознанных не равно.", expected.length, actual.size());

        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(String.format("Цвет под индексом %d ожидаемый - %s, но распознанный - %s",
                    i,
                    expected[i].name(),
                    IndexToColorEnum.getColorByIndex(actual.valueAt(i)).name()),
                    expected[i].getIndex(),
                    actual.valueAt(i));
        }
    }
}