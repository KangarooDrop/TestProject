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
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity  extends BlunoLibrary {
	private Button buttonScan;
	private Button buttonSave;
	private Button buttonLoad;
	private Button buttonPause;
	private Button buttonReset;
	private boolean paused = false;

	private DrawView dv;
	private GraphView graph;
	private LineGraphSeries<DataPoint> graphSeries;
	private ArrayList<DataPoint> allGraphSeries;
	private double graphXValue = 0;
	private int numOfPoints = 511;

	private boolean scrollToEndOfGraph = true;

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

		buttonSave = (Button)findViewById(R.id.buttonSave);
		buttonSave.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view)
			{
				ArrayList<String> strArr = new ArrayList<String>();
				double lastTime = 0;
				for(DataPoint p : allGraphSeries) {
					strArr.add("t: " + (p.getX() - lastTime) + " p: " + p.getY());
					lastTime = p.getX();
				}
				writeToDataFile(strArr);
			}
		});

		buttonLoad = findViewById(R.id.buttonLoad);
		buttonLoad.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				resetGraph();

				ArrayList<String> inputBuffer = readFromDataFile();
				for(int i = 0; i < inputBuffer.size(); i++)
				{
					List<String> lineData = Arrays.asList(inputBuffer.get(i).split(" "));
					if (lineData.size() >= 4) {
						int timeStepIndex = lineData.indexOf("t:") + 1;
						int pressureIndex = lineData.indexOf("p") + 1;
						if (timeStepIndex != 0 && pressureIndex != 0) {
							double deltaTime = Double.parseDouble(lineData.get(timeStepIndex));
							double pressureVal = Double.parseDouble(lineData.get(pressureIndex));
							DataPoint receivedPoint = new DataPoint(graphXValue, pressureVal);
							graphSeries.appendData(receivedPoint, true, numOfPoints);
							allGraphSeries.add(receivedPoint);
						}
					}
				}
				paused = true;
				updatePauseButtonText();
			}
		});

		buttonPause = (Button) findViewById(R.id.buttonPause);
		buttonPause.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				paused = !paused;
				updatePauseButtonText();
			}
		});

        buttonReset = (Button) findViewById(R.id.buttonReset);
        buttonReset.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                resetGraph();
                dv.setAngles(0, 0);
            }
        });

		graph = (GraphView)findViewById(R.id.graph);
		graphSeries = new LineGraphSeries<DataPoint>();
		allGraphSeries = new ArrayList<>();
		graph.addSeries(graphSeries);
		graph.getViewport().setXAxisBoundsManual(true);
		graph.getViewport().setMinX(0);
		graph.getViewport().setMaxX(5);

        graph.getViewport().setScrollable(true);
        graph.getViewport().setOnXAxisBoundsChangedListener(new Viewport.OnXAxisBoundsChangedListener() {
            @Override
            public void onXAxisBoundsChanged(double minX, double maxX, Reason reason) {
                scrollToEndOfGraph = maxX >= graphXValue - 0.2;
            }
        });

		dv = (DrawView)findViewById(R.id.drawview);
	}

	private void resetGraph()
	{
		graphXValue = 0;
		allGraphSeries.clear();
		graphSeries.resetData(new DataPoint[] {});
	}

	private void updatePauseButtonText()
	{
		buttonPause.setText(paused ? "Play" : "Pause");
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
		if (!paused) {
			try {
				if (line.contains("t")) {
					List<String> lineData = Arrays.asList(wholeLine.split(" "));
					if (lineData.size() >= 4) {
						int timeStepIndex = lineData.indexOf("t:") + 1;
						int pressureIndex = lineData.indexOf("p") + 1;
						int gyroIndex = lineData.indexOf("a:") + 1;
						if (timeStepIndex != 0) {
                            double deltaTime = Double.parseDouble(lineData.get(timeStepIndex));
							if (pressureIndex != 0)
							{
								double pressureVal = Double.parseDouble(lineData.get(pressureIndex));
								DataPoint receivedPoint = new DataPoint(graphXValue, pressureVal);
								graphSeries.appendData(receivedPoint, scrollToEndOfGraph, numOfPoints);
								allGraphSeries.add(receivedPoint);
								graphXValue += deltaTime;
							}
							if (gyroIndex != 0 && gyroIndex + 2 < lineData.size())
							{
								double pitch = Double.parseDouble(lineData.get(gyroIndex)),
										yaw = Double.parseDouble(lineData.get(gyroIndex + 2)),
										roll = Double.parseDouble(lineData.get(gyroIndex + 1));
								dv.rotate(pitch * deltaTime, yaw * deltaTime);
								dv.invalidate();
							}
						}
					}
					wholeLine = "";
				}
				wholeLine += line;
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
	}

	private void writeToDataFile(ArrayList<String> data)
	{
		try
		{
			FileOutputStream fos = openFileOutput("data_log.txt", MODE_PRIVATE);
			for(int i = 0; i < data.size(); i++)
				fos.write((data.get(i) + "\n").getBytes());
			fos.close();
			Log.w("Writing", "Writing to data log file");
		}
		catch (IOException e)
		{
			Log.w("Exception", "Error writing to file: " + e.toString());
			e.printStackTrace();
		}
	}

	private ArrayList<String> readFromDataFile()
	{
		try
		{
			FileInputStream fis = openFileInput("data_log.txt");
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader br = new BufferedReader(isr);
			ArrayList<String> logData = new ArrayList<>();
			String line;
			while((line = br.readLine()) != null)
				logData.add(line);
			fis.close();
			Log.w("Writing", "Reading from data log file");
			return logData;
		}
		catch (IOException e)
		{
			Log.w("Exception", "Error reading from file");
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
}