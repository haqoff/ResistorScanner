package ru.haqon.layout.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
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
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;

import java.util.Date;

import ru.haqon.resistor.logic.OhmStringFormatter;
import ru.haqon.R;
import ru.haqon.layout.views.ResistorCameraView;
import ru.haqon.resistor.logic.ResistorScannerHelper;
import ru.haqon.data.AppSQLiteDBHelper;
import ru.haqon.data.models.HistoryModel;


public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    static {
        OpenCVLoader.initDebug();
    }

    private ResistorCameraView _resistorCameraView;
    private OhmStringFormatter _formatter;
    private long _lastRecognizedValue;

    private BaseLoaderCallback _loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    _resistorCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
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
        int imageWidth = imageMat.cols();
        int imageHeight = imageMat.rows();

        Mat subMat = imageMat.submat(imageHeight / 2, imageHeight / 2 + 30, imageWidth / 2 - 50, imageWidth / 2 + 50);
        Mat filteredMat = new Mat();
        Imgproc.cvtColor(subMat, subMat, Imgproc.COLOR_RGBA2BGR);
        Imgproc.bilateralFilter(subMat, filteredMat, 5, 80, 80);
        Imgproc.cvtColor(filteredMat, filteredMat, Imgproc.COLOR_BGR2HSV);

        long ohmValue = ResistorScannerHelper.calcResistorValueByColors(ResistorScannerHelper.findResistorColors(filteredMat, 2, 10));
        _lastRecognizedValue = ohmValue;

        if (ohmValue > 0) {
            String formattedValue = _formatter.format(ohmValue);
            Size textSize = Imgproc.getTextSize(formattedValue, Imgproc.FONT_HERSHEY_COMPLEX, 2, 3, null);
            Imgproc.putText(imageMat, formattedValue, new Point((imageWidth / 2d) - (textSize.width / 2d), 100), Imgproc.FONT_HERSHEY_COMPLEX,
                    2, new Scalar(255, 0, 0, 255), 3);
        }


        Scalar color = new Scalar(255, 0, 0, 255);
        Imgproc.line(imageMat, new Point(imageWidth / 2 - 50, imageHeight / 2), new Point(imageWidth / 2 + 50, imageHeight / 2), color, 2);
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
