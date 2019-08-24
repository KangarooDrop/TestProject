package com.dfrobot.angelo.blunobasicdemo;

//https://github.com/DFRobot/BlunoBasicDemo
//https://github.com/jjoe64/GraphView/wiki/Realtime-chart

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.content.Intent;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity  extends BlunoLibrary {
	private Button buttonScan;

	private GraphView graph;
	private LineGraphSeries<DataPoint> graphSeries;
	private ArrayList<DataPoint> allGraphSeries;
	private double graphXValue = 0;
	private int numOfPoints = 40;

	private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;

	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case 1: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Log.w("PKM", "Access granted");
				} else {
					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					Log.w("PKM", "Access denied");
				}
				return;
			}

			// other 'case' lines to check for other
			// permissions this app might request
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);

		setContentView(R.layout.activity_main);
		onCreateProcess();														//onCreate Process by BlunoLibrary

		serialBegin(115200);

		buttonScan = (Button)findViewById(R.id.buttonScan);					//initial the button for scanning the BLE device
		buttonScan.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				buttonScanOnClickProcess();										//Alert Dialog for selecting the BLE device
			}
		});

		graph = (GraphView)findViewById(R.id.graph);
		graphSeries = new LineGraphSeries<DataPoint>();
		allGraphSeries = new ArrayList<>();
		graph.addSeries(graphSeries);
		graph.getViewport().setXAxisBoundsManual(true);
		graph.getViewport().setMinX(0);
		graph.getViewport().setMaxX(numOfPoints * .2);
	}

	@Override
	protected void onResume(){
		super.onResume();
		System.out.println("BLUNOActivity onResume");
		onResumeProcess();														//onResume Process by BlunoLibrary
	}



	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		onActivityResultProcess(requestCode, resultCode, data);					//onActivityResult Process by BlunoLibrary
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		super.onPause();
		onPauseProcess();														//onPause Process by BlunoLibrary
	}

	@Override
	protected void onStop() {
		super.onStop();
		onStopProcess();														//onStop Process by BlunoLibrary
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		onDestroyProcess();														//onDestroy Process by BlunoLibrary
	}

	@Override
	public void onConectionStateChange(connectionStateEnum theConnectionState) {//Once connection state changes, this function will be called
		switch (theConnectionState) {											//Four connection state
			case isConnected:
				buttonScan.setText("Connected");
				break;
			case isConnecting:
				buttonScan.setText("Connecting");
				break;
			case isToScan:
				buttonScan.setText("Scan");
				break;
			case isScanning:
				buttonScan.setText("Scanning");
				break;
			case isDisconnecting:
				buttonScan.setText("isDisconnecting");
				break;
			default:
				break;
		}
	}

	String wholeLine;
	@Override
	public void onSerialReceived(String line)
	{
		try
		{
			if (line.contains("t")) {
				List<String> lineData = Arrays.asList(wholeLine.split(" "));
				if (lineData.size() >= 4) {
					int timeStepIndex = lineData.indexOf("t:") + 1;
					int pressureIndex = lineData.indexOf("p") + 1;
					if (timeStepIndex != 0 && pressureIndex != 0) {
						Log.w("Got here", "Got here");
						double deltaTime = Double.parseDouble(lineData.get(timeStepIndex));
						double pressureVal = Double.parseDouble(lineData.get(pressureIndex));
						DataPoint receivedPoint = new DataPoint(graphXValue, pressureVal);
						graphSeries.appendData(receivedPoint, true, numOfPoints);
						allGraphSeries.add(receivedPoint);
						graphXValue += deltaTime;
					}
				}
				wholeLine = "";
			}
			wholeLine += line;
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
		}
	}
}