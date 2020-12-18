package ru.haqon.layout.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;

import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Date;

import ru.haqon.resistor.logic.OhmStringFormatter;
import ru.haqon.R;
import ru.haqon.layout.views.ResistorCameraView;
import ru.haqon.resistor.logic.ResistorScanner;
import ru.haqon.data.AppSQLiteDBHelper;
import ru.haqon.data.models.HistoryModel;
import ru.haqon.resistor.logic.ScanMode;


public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    static {
        OpenCVLoader.initDebug();
    }

    private ResistorCameraView _resistorCameraView;
    private OhmStringFormatter _formatter;
    private long _lastRecognizedValue;

    private final BaseLoaderCallback _loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                _resistorCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initCameraView();
        showInstructionModalIfNeeded();

        _formatter = new OhmStringFormatter(this);
    }

    private void initCameraView() {
        _resistorCameraView = (ResistorCameraView) findViewById(R.id.ResistorCameraView);
        _resistorCameraView.setVisibility(SurfaceView.VISIBLE);
        _resistorCameraView.setZoomControl((SeekBar) findViewById(R.id.CameraZoomControls));
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CAMERA},
                1);
        _resistorCameraView.setCameraPermissionGranted();
        _resistorCameraView.setCvCameraViewListener(this);
    }

    private void showInstructionModalIfNeeded() {
        SharedPreferences settings = getPreferences(0);
        if (!settings.getBoolean("shownInstructions", false)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.dialog_message)
                    .setTitle(R.string.dialog_title)
                    .setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("shownInstructions", true);
            editor.apply();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (_resistorCameraView != null)
            _resistorCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (_resistorCameraView != null)
            _resistorCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat imageMat = inputFrame.rgba();

        final int imageWidth = imageMat.cols();
        final int imageHeight = imageMat.rows();
        final int searchAreaFromX = imageWidth / 2 - 50;
        final int searchAreaToX = imageWidth / 2 + 50;
        final int searchAreaFromY = imageHeight / 2 - 10;
        final int searchAreaToY = imageHeight / 2 + 10;

        Mat searchAreaInHsv = imageMat.submat(searchAreaFromY, searchAreaToY, searchAreaFromX, searchAreaToX);
        Imgproc.cvtColor(searchAreaInHsv, searchAreaInHsv, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(searchAreaInHsv, searchAreaInHsv, Imgproc.COLOR_RGB2HSV);

        Scalar colorToDraw = new Scalar(255, 0, 0, 255);
        long ohmValue = ResistorScanner.smartScan(searchAreaInHsv, ScanMode.UNKNOWN);
        if (ohmValue > 0) {
            _lastRecognizedValue = ohmValue;
            colorToDraw = new Scalar(0, 255, 0, 255);
        }

        String formattedValue = _formatter.format(_lastRecognizedValue);
        Size textSize = Imgproc.getTextSize(formattedValue, Imgproc.FONT_HERSHEY_COMPLEX, 2, 3, null);
        Imgproc.putText(imageMat, formattedValue, new Point((imageWidth / 2d) - (textSize.width / 2d), 100), Imgproc.FONT_HERSHEY_COMPLEX,
                2, colorToDraw, 3);

        Imgproc.rectangle(imageMat, new Rect(new Point(searchAreaFromX, searchAreaFromY), new Point(searchAreaToX, searchAreaToY)), colorToDraw, 1);
        Imgproc.line(imageMat, new Point(searchAreaFromX, imageHeight / 2d), new Point(searchAreaToX, imageHeight / 2d), colorToDraw, 2);
        return imageMat;
    }

    @Override
    public void onResume() {
        super.onResume();
        _loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    public void btnSaveOnClick(View view) {
        if (_lastRecognizedValue == 0) return;

        HistoryModel m = new HistoryModel();
        m.setValueInOhm(_lastRecognizedValue);
        m.setDate(new Date());

        AppSQLiteDBHelper db = new AppSQLiteDBHelper(this.getApplicationContext());
        db.insertToHistoryTable(m);

        Toast t = Toast.makeText(this.getApplicationContext(), R.string.caption_saved, Toast.LENGTH_SHORT);
        t.setGravity(Gravity.TOP, 0, 50);
        t.show();
    }

    public void btnHistoryOnClick(View view) {
        Intent historyIntent = new Intent(MainActivity.this, HistoryActivity.class);
        MainActivity.this.startActivity(historyIntent);
    }
}
