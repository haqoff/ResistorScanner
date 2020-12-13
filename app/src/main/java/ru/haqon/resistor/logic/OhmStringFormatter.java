package ru.haqon.resistor.logic;

import android.content.Context;

import ru.haqon.R;

/**
 * Представляет собой форматировщик преобразования и выражения Ома в значение строки.
 */
public class OhmStringFormatter {
    private String captionOhm;
    private String captionKOhm;
    private String captionMOhm;
    private String captionGOhm;
    private String captionTOhm;
    private String captionPOhm;

    public OhmStringFormatter(String captionOhm, String captionKOhm, String captionMOhm, String captionGOhm, String captionTOhm, String captionPOhm) {
        this.captionOhm = captionOhm;
        this.captionMOhm = captionMOhm;
        this.captionKOhm = captionKOhm;
        this.captionGOhm = captionGOhm;
        this.captionTOhm = captionTOhm;
        this.captionPOhm = captionPOhm;
    }

    public OhmStringFormatter(Context context) {
        this.captionOhm = context.getString(R.string.caption_value_Ohm);
        this.captionKOhm = context.getString(R.string.caption_value_KOhm);
        this.captionMOhm = context.getString(R.string.caption_value_MOhm);
        this.captionGOhm = context.getString(R.string.caption_value_GOhm);
        this.captionTOhm = context.getString(R.string.caption_value_TOhm);
        this.captionPOhm = context.getString(R.string.caption_value_POhm);
    }

    /**
     * Форматирует указанное значение в омах в наиболее подходящую единицу измерения.
     */
    public String format(long ohmValue) {
        double valueInUnit;
        String unitCaption;

        if (ohmValue >= 1e15) {
            valueInUnit = ohmValue / 1e15;
            unitCaption = captionPOhm;
        } else if (ohmValue >= 1e12) {
            valueInUnit = ohmValue / 1e12;
            unitCaption = captionTOhm;
        } else if (ohmValue >= 1e9) {
            valueInUnit = ohmValue / 1e9;
            unitCaption = captionGOhm;
        } else if (ohmValue >= 1e6) {
            valueInUnit = ohmValue / 1e6;
            unitCaption = captionMOhm;
        } else if (ohmValue >= 1e3) {
            valueInUnit = ohmValue / 1e3;
            unitCaption = captionKOhm;
        } else {
            valueInUnit = ohmValue;
            unitCaption = captionOhm;
        }

        return String.format("%s %s", String.valueOf(valueInUnit), unitCaption);
    }
}
