package ru.haqon.resistor.logic;

import android.util.SparseIntArray;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

/**
 * Представляет собой класс для распознования сопротивления резисторов с цветовой маркировкой.
 */
public class ResistorScanner {
    public static class Range {
        public final Scalar from;
        public final Scalar to;

        Range(Scalar from, Scalar to) {
            this.from = from;
            this.to = to;
        }
    }

    public static final Range[][] COLOR_BOUNDS = {
            {new Range(new Scalar(0, 0, 0), new Scalar(180, 250, 50))},    // black
            {new Range(new Scalar(0, 31, 41), new Scalar(25, 250, 99))},    // brown
            {new Range(new Scalar(0, 65, 60), new Scalar(8, 100, 100)),
                    new Range(new Scalar(158, 65, 50), new Scalar(180, 250, 150))},  // red
            {new Range(new Scalar(7, 150, 150), new Scalar(18, 250, 250))},   // orange
            {new Range(new Scalar(25, 130, 100), new Scalar(34, 250, 160))}, // yellow
            {new Range(new Scalar(35, 60, 60), new Scalar(75, 250, 150))},   // green
            {new Range(new Scalar(80, 50, 50), new Scalar(106, 250, 150))},  // blue
            {new Range(new Scalar(130, 60, 50), new Scalar(165, 250, 150))}, // purple
            {new Range(new Scalar(0, 0, 50), new Scalar(180, 50, 80))},       // gray
            {new Range(new Scalar(0, 0, 90), new Scalar(180, 15, 140))}      // white
    };

    /* OLD:
    private static final Range[][] COLOR_BOUNDS = {
            {new Range(new Scalar(0, 0, 0), new Scalar(180, 250, 50))},    // black
            {new Range(new Scalar(0, 90, 10), new Scalar(15, 250, 100))},    // brown
            {new Range(new Scalar(0, 65, 100), new Scalar(2, 250, 150)),
                    new Range(new Scalar(171, 65, 50), new Scalar(180, 250, 150))},  // red (defined by two bounds)
            {new Range(new Scalar(4, 100, 100), new Scalar(9, 250, 150))},   // orange
            {new Range(new Scalar(20, 130, 100), new Scalar(30, 250, 160))}, // yellow
            {new Range(new Scalar(45, 50, 60), new Scalar(72, 250, 150))},   // green
            {new Range(new Scalar(80, 50, 50), new Scalar(106, 250, 150))},  // blue
            {new Range(new Scalar(130, 40, 50), new Scalar(155, 250, 150))}, // purple
            {new Range(new Scalar(0, 0, 50), new Scalar(180, 50, 80))},       // gray
            {new Range(new Scalar(0, 0, 90), new Scalar(180, 15, 140))}      // white
    };
     */

    private ResistorScanner() {
    }

    /**
     * Выполняет необходимый препроцессинг указанной области, а затем и сканирование, возвращая результат в Омах.
     *
     * @param searchArea Область поиска в виде матрицы в модели HSV.
     * @param mode       Режим сканирования.
     * @return Результат в Омах или 0, если распознование не удалось.
     */
    public static long smartScan(Mat searchArea, ScanMode mode) {
        Mat searchAreaInBgr = new Mat();
        Mat searchAreaFilteredInHsv = new Mat();

        Imgproc.cvtColor(searchArea, searchAreaInBgr, Imgproc.COLOR_HSV2BGR);
        Imgproc.bilateralFilter(searchAreaInBgr, searchAreaFilteredInHsv, 5, 80, 80);
        Imgproc.cvtColor(searchAreaFilteredInHsv, searchAreaFilteredInHsv, Imgproc.COLOR_BGR2HSV);

        // сначала пытаетмся распознать без фона, если нет результата - распознаем вместе с фоном
        Mat allColorsMask = maskAllResistorColors(searchAreaFilteredInHsv);
        long res = calcResistorValueByColors(findResistorColors(searchAreaFilteredInHsv, allColorsMask), mode);

        if (res == 0) {
            res = calcResistorValueByColors(findResistorColors(searchAreaFilteredInHsv, null), mode);
        }

        return res;
    }

    /**
     * Обрабатывает и получает упорядоченный словарь вида 'Положение полосы (x) - Значение полосы.'
     *
     * @param searchArea    Область поиска в виде матрицы в модели HSV.
     * @param allColorsMask Маска, на которой выделены необходимые цвета на области searchArea для распознания. Может быть null.
     */
    public static SparseIntArray findResistorColors(Mat searchArea, Mat allColorsMask) {
        return findResistorColors(searchArea, allColorsMask, 2, 8);
    }

    /**
     * Обрабатывает и получает упорядоченный словарь вида 'Положение полосы (x) - Значение полосы.'
     *
     * @param searchArea          Область поиска в виде матрицы в модели HSV.
     * @param allColorsMask       Маска, на которой выделены необходимые цвета на области searchArea для распознания. Может быть null.
     * @param minColorContourArea Минимально необходимая площадь цвета для его принятия как валидный.
     * @param ringMaxWidth        Максимальная ширина одного цвета (кольца).
     *                            Этот параметр влияет таким образом, что если встречено несколько цветов по дистанции в пределах этого значения,
     *                            они считаются одним кольцом (из этих цветов выбирается больший по площади).
     */
    @org.jetbrains.annotations.NotNull
    public static SparseIntArray findResistorColors(Mat searchArea, Mat allColorsMask, int minColorContourArea, int ringMaxWidth) {
        SparseIntArray result = new SparseIntArray(4);
        SparseIntArray areas = new SparseIntArray(4);

        for (int i = 0; i < COLOR_BOUNDS.length; i++) {
            for (int rangeIndex = 0; rangeIndex < COLOR_BOUNDS[i].length; rangeIndex++) {
                Range currentRange = COLOR_BOUNDS[i][rangeIndex];

                List<MatOfPoint> contours = new ArrayList<>();
                Mat mask = new Mat();
                if (allColorsMask == null) {
                    Core.inRange(searchArea, currentRange.from, currentRange.to, mask);
                } else {
                    Mat rangeMask = new Mat();
                    Core.inRange(searchArea, currentRange.from, currentRange.to, rangeMask);
                    rangeMask.copyTo(mask, allColorsMask);
                }

                Mat hierarchy = new Mat();
                Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
                for (int contIdx = 0; contIdx < contours.size(); contIdx++) {
                    int currentContourArea = (int) Imgproc.contourArea(contours.get(contIdx));
                    if (currentContourArea <= minColorContourArea) continue;

                    Moments moments = Imgproc.moments(contours.get(contIdx));
                    int currentContourX = (int) (moments.get_m10() / moments.get_m00());

                    // если цветная полоса разбита на несколько контуров
                    // берем наибольшее по площади и рассматриваем только его
                    boolean shouldStoreLocation = true;
                    for (int locIdx = 0; locIdx < result.size(); locIdx++) {
                        int savedColorX = result.keyAt(locIdx);

                        if (Math.abs(savedColorX - currentContourX) < ringMaxWidth) {
                            if (areas.get(savedColorX) > currentContourArea) {
                                shouldStoreLocation = false;
                                break;
                            } else {
                                result.delete(savedColorX);
                                areas.delete(savedColorX);
                            }
                        }
                    }

                    if (shouldStoreLocation) {
                        areas.put(currentContourX, currentContourArea);
                        result.put(currentContourX, i);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Получает значение в Омах с помощью указанного упорядоченного словаря вида 'Положение полосы (x) - Значение полосы.'.
     *
     * @return Результат в Омах или 0, если не удалось рассчитать.
     */
    public static long calcResistorValueByColors(SparseIntArray colors, ScanMode mode) {
        long value = 0;
        if ((mode == ScanMode.FOUR_BAND && colors.size() == 3 || colors.size() == 4) || (mode == ScanMode.UNKNOWN && colors.size() == 3)) {
            int k_tens = colors.keyAt(0);
            int k_units = colors.keyAt(1);
            int k_power = colors.keyAt(2);

            value = 10 * colors.get(k_tens) + colors.get(k_units);
            value *= Math.pow(10, colors.get(k_power));
        } else if ((mode == ScanMode.FIVE_BAND && colors.size() >= 4) || (mode == ScanMode.UNKNOWN && colors.size() == 4)) {
            int k_hundreds = colors.keyAt(0);
            int k_tens = colors.keyAt(1);
            int k_units = colors.keyAt(2);
            int k_power = colors.keyAt(3);

            value = 100 * colors.get(k_hundreds) + 10 * colors.get(k_tens) + colors.get(k_units);
            value *= Math.pow(10, colors.get(k_power));
        }

        return value;
    }

    /**
     * Создаёт маску колец на резисторе.
     *
     * @param src Область поиска резистора в модели HSV.
     * @return Маску, на которой выделены предполагаемые необходимые цвета для распознания.
     */
    public static Mat maskAllResistorColors(Mat src) {
        Mat gray = new Mat(src.rows(), src.cols(), src.type());
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_HSV2RGB);
        Imgproc.cvtColor(gray, gray, Imgproc.COLOR_RGB2GRAY);

        Mat thresh = new Mat(src.rows(), src.cols(), CvType.CV_8UC1);
        Imgproc.threshold(gray, thresh, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);
        return thresh;
    }
}
