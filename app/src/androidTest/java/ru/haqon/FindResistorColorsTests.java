package ru.haqon;

import android.util.SparseIntArray;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Scalar;

import java.util.Arrays;

import ru.haqon.resistor.logic.ResistorScanner;

@RunWith(DataProviderRunner.class)
public class FindResistorColorsTests {
    static {
        OpenCVLoader.initDebug();
    }

    private static class TestSource {
        private IndexToColorEnum colorName;
        private Scalar[] colors;

        public TestSource(IndexToColorEnum colorName, Scalar[] colors) {
            this.colorName = colorName;
            this.colors = colors;
        }

        public TestSource(IndexToColorEnum colorName) {
            this(colorName, new Scalar[0]);
        }

        public TestSource append(Scalar... newColors) {
            Scalar[] array = new Scalar[colors.length + newColors.length];
            System.arraycopy(colors, 0, array, 0, colors.length);
            System.arraycopy(newColors, 0, array, colors.length, newColors.length);
            colors = array;

            return this;
        }

        public TestSource appendTestColors(int count) {
            append(TestUtils.colorsFromName(colorName, count).toArray(new Scalar[0]));
            return this;
        }

        @Override
        public String toString() {
            return String.format("TestSource{colorName=%s}", colorName.name());
        }
    }

    @Test()
    @UseDataProvider("getTestColors")
    public void testColorRecognitionShouldRecognized(TestSource source) {
        TestUtils.Gen gen = TestUtils.generateSimpleMatrixWithColors(source.colors);
        SparseIntArray res = ResistorScanner.findResistorColors(gen.source, gen.colorsMask);

        IndexToColorEnum[] expected = new IndexToColorEnum[source.colors.length];
        Arrays.fill(expected, source.colorName);
        TestUtils.assertColors(expected, res, source.colors);
    }

    @DataProvider
    public static Object[][] getTestColors() {
        TestSource[] s = {
                new TestSource(IndexToColorEnum.BLACK).appendTestColors(1),
                new TestSource(IndexToColorEnum.BROWN).appendTestColors(1),
                new TestSource(IndexToColorEnum.RED).appendTestColors(1),
                new TestSource(IndexToColorEnum.ORANGE).appendTestColors(1),
                new TestSource(IndexToColorEnum.YELLOW).appendTestColors(1),
                new TestSource(IndexToColorEnum.GREEN).appendTestColors(1),
                new TestSource(IndexToColorEnum.BLUE).appendTestColors(1),
                new TestSource(IndexToColorEnum.PURPLE).appendTestColors(1),
                new TestSource(IndexToColorEnum.GRAY).appendTestColors(1),
                new TestSource(IndexToColorEnum.WHITE).appendTestColors(1),

        };
        Object[][] objects = new Object[s.length][1];
        for (int i = 0; i < objects.length; i++) objects[i][0] = s[i];
        return objects;
    }
}