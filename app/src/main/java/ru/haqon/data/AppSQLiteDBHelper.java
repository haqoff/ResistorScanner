package ru.haqon.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import ru.haqon.data.models.HistoryModel;

public class AppSQLiteDBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "app_database";

    public static final String HISTORY_TABLE_NAME = "scan_history";
    public static final String HISTORY_COLUMN_ID = "id";
    public static final String HISTORY_COLUMN_VALUE = "value";
    public static final String HISTORY_COLUMN_DATE = "date";

    public AppSQLiteDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + HISTORY_TABLE_NAME + " (" +
                HISTORY_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                HISTORY_COLUMN_VALUE + " INTEGER, " +
                HISTORY_COLUMN_DATE + " TEXT " + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + HISTORY_TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    /**
     * Вставляет указанный объект модели истории в таблицу базы данных.
     */
    public long insertToHistoryTable(HistoryModel model) {
        ContentValues values = new ContentValues();
        values.put(HISTORY_COLUMN_VALUE, model.getValueInOhm());
        values.put(HISTORY_COLUMN_DATE, dateToString(model.getDate()));
        return getWritableDatabase().insert(HISTORY_TABLE_NAME, null, values);
    }

    /**
     * Получает все записи из таблицы истории сканирований, которые отсортированны по убыванию даты.
     */
    public ArrayList<HistoryModel> selectHistoryTableInDateDesc() {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<HistoryModel> result = new ArrayList<>();

        Cursor cursor = db.rawQuery(String.format("select * from %s order by datetime(%s) DESC", HISTORY_TABLE_NAME, HISTORY_COLUMN_DATE), null);

        while (cursor.moveToNext()) {
            HistoryModel m = new HistoryModel();
            m.setId(cursor.getInt(cursor.getColumnIndex(HISTORY_COLUMN_ID)));
            m.setValueInOhm(cursor.getLong(cursor.getColumnIndex(HISTORY_COLUMN_VALUE)));
            m.setDate(stringToDate(cursor.getString(cursor.getColumnIndex(HISTORY_COLUMN_DATE))));

            result.add(m);
        }
        return result;
    }

    /**
     * Удаляет все записи из таблицы истории сканирований.
     */
    public void deleteHistoryTableData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(HISTORY_TABLE_NAME, null, null);
    }

    private String dateToString(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return sdf.format(d);
    }

    private Date stringToDate(String s) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        try {
            return sdf.parse(s);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }
}
