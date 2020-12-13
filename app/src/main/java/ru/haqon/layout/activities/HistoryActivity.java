package ru.haqon.layout.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.SimpleDateFormat;

import ru.haqon.resistor.logic.OhmStringFormatter;
import ru.haqon.R;
import ru.haqon.data.AppSQLiteDBHelper;
import ru.haqon.data.models.HistoryModel;

public class HistoryActivity extends AppCompatActivity {
    private TableLayout _table;
    private OhmStringFormatter _formatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _formatter = new OhmStringFormatter(this);

        setContentView(R.layout.activity_history);
        initHistoryTableAndLoadRows();
    }

    private void initHistoryTableAndLoadRows() {
        _table = (TableLayout) findViewById(R.id.table);

        AppSQLiteDBHelper db = new AppSQLiteDBHelper(this.getApplicationContext());
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy HH:mm:ss");

        for (HistoryModel model : db.selectHistoryTableInDateDesc()) {
            TableRow row = (TableRow) LayoutInflater.from(HistoryActivity.this).inflate(R.layout.table_row_template, null);
            ((TextView) row.findViewById(R.id.tableHistoryDate)).setText(sdf.format(model.getDate()));
            ((TextView) row.findViewById(R.id.tableHistoryValue)).setText(_formatter.format(model.getValueInOhm()));
            _table.addView(row);
        }
        _table.requestLayout();
    }

    public void btnClearTableOnClick(View view) {
        AppSQLiteDBHelper db = new AppSQLiteDBHelper(this.getApplicationContext());
        db.deleteHistoryTableData();
        _table.removeViews(1, Math.max(0, _table.getChildCount() - 1));
    }
}
