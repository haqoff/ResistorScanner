package ru.haqon.data.models;

import java.util.Date;

/**
 * Представляет собой модель истории о сканировании.
 */
public class HistoryModel {
    private int id;
    private long valueInOhm;
    private Date date;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getValueInOhm() {
        return valueInOhm;
    }

    public void setValueInOhm(long value) {
        this.valueInOhm = value;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
