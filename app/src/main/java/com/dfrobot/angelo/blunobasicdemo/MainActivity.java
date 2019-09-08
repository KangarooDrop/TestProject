package com.dfrobot.angelo.blunobasicdemo;

//https://github.com/DFRobot/BlunoBasicDemo
//https://github.com/jjoe64/GraphView/wiki/Realtime-chart

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.content.Intent;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MainActivity  extends BlunoLibrary {

	private TextView angleText;
	private Button buttonScan;

	private double startRecordTime = -1;
	private double stopRecordTime = -1;
	private LineGraphSeries<DataPoint> recordSeries = new LineGraphSeries<>();
	private boolean recording = false;

	private Spinner dataSelectionSpinner;
	ArrayList<String> spinnerList = new ArrayList<>();

	//private DrawView dv;
	private GraphView graph;
	private LineGraphSeries<DataPoint> liveSeries = new LineGraphSeries<>();
	private LineGraphSeries<DataPoint> fileSeries = new LineGraphSeries<>();
	private ArrayList<DataPoint> allGraphSeries;
	private double graphXValue = 0;

	private int numOfPoints = 511;

	private DrawView dv;

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

		//dv = findViewById(R.id.drawview);
		angleText = findViewById(R.id.angletext);

		graph = findViewById(R.id.graph);
		graph.getViewport().setXAxisBoundsManual(true);
		graph.getViewport().setMinX(0);
		graph.getViewport().setMaxX(5);
		graph.getViewport().setScrollable(true);

		recordSeries.setColor(0xffff0000);

		spinnerList.add("Live");
		List<String> fileNames = getAllFilenames();
		for(int i = 0; fileNames != null && i < fileNames.size(); i++)
		{
			String fileName = fileNames.get(i);
			spinnerList.add(fileName.substring(0, fileName.length() - 4));
		}
		dataSelectionSpinner = findViewById(R.id.data_spinner);
		updateSpinnerList();
		dataSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
			{
				scrollToEndOfGraph = (position == 0);
				graph.removeAllSeries();
				if (position != 0)
				{
					graph.getViewport().setScrollable(true);
					double startX = graphXValue;
					graphXValue = 0;
					fileSeries.resetData(new DataPoint[] {});
					graph.addSeries(fileSeries);
					List<String> strData = readFromDataFile(spinnerList.get(position));
					boolean pastScroll = scrollToEndOfGraph;
					scrollToEndOfGraph = true;
					for(String line : strData)
						addStringToGraph(line, fileSeries);
					graphXValue = startX;
					scrollToEndOfGraph = pastScroll;
				}
				else {
					graph.getViewport().setScrollable(false);
					graph.addSeries(liveSeries);
					graph.addSeries(recordSeries);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{

			}
		});

		buttonScan = (Button)findViewById(R.id.buttonScan);					//initial the button for scanning the BLE device
		buttonScan.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				buttonScanOnClickProcess();										//Alert Dialog for selecting the BLE device
			}
		});

		Button saveButton = findViewById(R.id.savebutton);
		saveButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				final EditText input = new EditText(MainActivity.this);
				input.setInputType(InputType.TYPE_CLASS_TEXT);

				new AlertDialog.Builder(MainActivity.this).setTitle("Save Data As").setView(input).setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						ArrayList<String> dataToSave = new ArrayList<>();
						double lastTime = -1;
						Iterator<DataPoint> iter = recordSeries.getValues(startRecordTime, stopRecordTime);
						DataPoint currentPoint;
						while (iter.hasNext())
						{
							currentPoint = iter.next();
							if(lastTime == -1)
								lastTime = currentPoint.getX();
							dataToSave.add("t: " + (currentPoint.getX() - lastTime) + " p: " + currentPoint.getY());
							lastTime = currentPoint.getX();
						}
						if (writeToDataFile(dataToSave, input.getText().toString()))
						{
							if (!spinnerList.contains(input.getText().toString())) {
								spinnerList.add(input.getText().toString());
								updateSpinnerList();
							}
							recordSeries.resetData(new DataPoint[] {});
						}
						else
							Toast.makeText(MainActivity.this, "Could not create file with the selected name. Please try again with a different file name", Toast.LENGTH_LONG).show();
					}
				}).setNegativeButton("Cancel", null).show();
			}
		});

		final Button startButton = findViewById(R.id.startrecordbutton);
		startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!recording) {
					startRecordTime = graphXValue;
					recordSeries.resetData(new DataPoint[]{});
					recording = true;
					startButton.setText("Stop Recording");
				}
				else {
					stopRecordTime = graphXValue;
					recording = false;
					startButton.setText("Start Recording");
				}
			}
		});

		Button closeButton = findViewById(R.id.closebuttton);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view)
			{
				String name = dataSelectionSpinner.getSelectedItem().toString();
				if (dataSelectionSpinner.getSelectedItemPosition() != 0) {
					new AlertDialog.Builder(MainActivity.this).setTitle("Are you sure you want to delete \"" + name + "\" forever?")
							.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									String name = dataSelectionSpinner.getSelectedItem().toString();
									if (deleteFileFromDir(name)) {
										spinnerList.remove(name);
										updateSpinnerList();
									}
									else
										Toast.makeText(MainActivity.this, "Could not delete file", Toast.LENGTH_SHORT).show();
								}
							}).setNegativeButton("No", null)
							.show();
				}
				else
					Toast.makeText(MainActivity.this, "Cannot close the live feed", Toast.LENGTH_SHORT).show();
			}
		});

		findViewById(R.id.calibratebutton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view)
			{
				calibrate();
			}
		});

		//dv = (DrawView)findViewById(R.id.drawview);
	}

	private boolean calibrating = false;
	private double calibTimer = 0;

	private double calibGyroX = 0,
					calibGyroY = 0,
					calibGyroZ = 0;
	private int calibNumGyroPoints = 0;

	private double calibPressure = 0;
	private int calibNumPressurePoints = 0;

	private double paddleAngleX = 0,
					paddleAngleY = 0,
					paddleAngleZ = 0;

	private double averageGyroX = 0,
					averageGyroY = 0,
					averageGyroZ = 0,

					averagePressure;

	private void calibrate()
	{
		calibrating = true;
		calibTimer = 0;

		calibGyroX = 0;
		calibGyroY = 0;
		calibGyroZ = 0;
		calibNumGyroPoints = 0;

		calibPressure = 0;
		calibNumPressurePoints = 0;
	}

	private void addStringToGraph(String line, LineGraphSeries series)
	{
		try {
			List<String> lineData = Arrays.asList(line.split(" "));
			if (lineData.size() >= 4) {
				int timeStepIndex = lineData.indexOf("t:") + 1;
				int pressureIndex = lineData.indexOf("p:") + 1;
				int accelIndex = lineData.indexOf("a:") + 1;
				int gyroIndex = lineData.indexOf("g:");
				if (timeStepIndex != 0) {
					double deltaTime = Double.parseDouble(lineData.get(timeStepIndex));
					if (calibrating)
						calibTimer += deltaTime;

					if (pressureIndex != 0)
					{
						double pressureVal = Double.parseDouble(lineData.get(pressureIndex));
						if (!calibrating) {
							DataPoint receivedPoint = new DataPoint(graphXValue, pressureVal);
							series.appendData(receivedPoint, scrollToEndOfGraph, numOfPoints);
							graphXValue += deltaTime;
						}
						else
						{
							calibPressure += pressureVal;
							++calibNumPressurePoints;
						}
					}

					if (gyroIndex != 0 && gyroIndex + 3 < lineData.size() && accelIndex != 0 && accelIndex + 3 < lineData.size())
					{
						double gyroX = Double.parseDouble(lineData.get(gyroIndex + 0)),
								gyroY = Double.parseDouble(lineData.get(gyroIndex + 1)),
								gyroZ = Double.parseDouble(lineData.get(gyroIndex + 2));
						double accX = Double.parseDouble(lineData.get(accelIndex + 0)),
								accY = Double.parseDouble(lineData.get(accelIndex + 1)),
								accZ = Double.parseDouble(lineData.get(accelIndex + 2));
						if (calibrating) {
							paddleAngleX += (gyroX - averageGyroX) * deltaTime;
							paddleAngleY += (gyroY - averageGyroY) * deltaTime;
							paddleAngleZ += (gyroZ - averageGyroZ) * deltaTime;
							String angleDisplayText = "Angle: (" + Math.floor(100*paddleAngleX)/100.0 + ", " + Math.floor(100*paddleAngleY)/100.0 + ", " + Math.floor(100*paddleAngleZ)/100.0 + ")";
							angleText.setText(angleDisplayText);
							dv.invalidate();


/*
							double k = 0.98,
								gyroConst = 1;

							paddleAngleX += gyroX;
							paddleAngleY += gyroY;
							paddleAngleZ += gyroZ;

							double forceMag = Math.sqrt(accX*accX + accY*accY + accZ*accZ);
							if (forceMag >= -20 && forceMag <= 20)
							{
								double accAngleX = Math.atan2(accY, accZ) * gyroConst * deltaTime;
								paddleAngleX = paddleAngleX * k + accAngleX * (k - 1);

								double accAngleY = Math.atan2(accX, accZ) * gyroConst * deltaTime;
								paddleAngleY = paddleAngleY * k + accAngleY * (k - 1);

								double accAngleZ = Math.atan2(accX, accY) * gyroConst * deltaTime;
								paddleAngleZ = paddleAngleZ * k + accAngleZ * (k - 1);
							}

/*
							float pitch, roll
							float k, q [0 >= k, q <= 1]
							GIVEN: accData[3], gyroData[3]

							pitch += gyroData[0] * gyroConst * dt
							roll -= gyroData[1] * gyroConst * dt

							int forceMag = Math.sqrt(accData[0]*(accData[0] + (accData[1]*(accData[1] + (accData[2]*(accData[2])
							if forceMag > -2Gs && < 2Gs:
								float pitchAcc = atan2(accData[1], accData[2]) * 180 / PI
								pitch = pitch * k + pitchAcc * (k - 1)

								float rollAcc = atan(accData[0], accData[2]) * 180 / PI
								roll = roll * q + rollAcc * (q - 1)

							 */

						}
						else
						{
							calibGyroX += gyroX;
							calibGyroY += gyroY;
							calibGyroZ += gyroZ;
							++calibNumGyroPoints;
						}
					}
					if (calibrating && calibTimer >= 3)
					{
						averagePressure = calibPressure / calibNumPressurePoints;

						averageGyroX = calibGyroX / calibNumGyroPoints;
						averageGyroY = calibGyroY / calibNumGyroPoints;
						averageGyroZ = calibGyroZ / calibNumGyroPoints;

						calibrating = false;
					}
				}
			}
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
		}
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

	String wholeLine = "";
	@Override
	public void onSerialReceived(String line)
	{
			if (line.contains("t")) {
				if (recording) {
					double lastTime = graphXValue;
					addStringToGraph(wholeLine, recordSeries);
					graphXValue = lastTime;
				}
				addStringToGraph(wholeLine, liveSeries);
				wholeLine = "";
			}
			wholeLine += line;
	}

	private boolean writeToDataFile(ArrayList<String> data, String fileName)
	{
		try
		{/*
            String path = "";
            int slashIndex = 0;
            while((slashIndex = fileName.indexOf('/')) != -1) {
                path += fileName.substring(0, slashIndex+1);
                fileName = fileName.substring(slashIndex+1, fileName.length());
            }
            Log.w("path", "Path="+path);*/
			File parentFolder = getDir("dataset", MODE_PRIVATE);
			File outFile = new File(parentFolder, fileName + ".txt");
			FileOutputStream fos = new FileOutputStream(outFile);
			for(int i = 0; i < data.size(); i++)
				fos.write((data.get(i) + "\n").getBytes());
			fos.close();
			Log.w("Writing", "Writing to data log file");
			return true;
		}
		catch (IOException e)
		{
			Log.w("Exception", "Error writing to file: " + e.toString());
			e.printStackTrace();
			return false;
		}
	}

	private ArrayList<String> readFromDataFile(String fileName)
	{
		try
		{
			File parentFolder = getDir("dataset", MODE_PRIVATE);
			File outFile = new File(parentFolder, fileName + ".txt");
			FileInputStream fis = new FileInputStream(outFile);
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

	private List<String> getAllFilenames ()
	{
		List<String> fileNames = new ArrayList<>();
		File dir = getDir("dataset", MODE_PRIVATE);
		if (dir != null && dir.list() != null)
			Collections.addAll(fileNames, dir.list());
		else
			return new ArrayList<>();
		return fileNames;
	}

	private boolean deleteFileFromDir (String fileName)
	{
		File dir = getDir("dataset", MODE_PRIVATE);
		File f = new File(dir, fileName + ".txt");
		return f.delete();
	}

	private void updateSpinnerList()
	{
		ArrayAdapter<String> adp1 = new ArrayAdapter<>(this,
				android.R.layout.simple_list_item_1, spinnerList);
		adp1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dataSelectionSpinner.setAdapter(adp1);
	}
}