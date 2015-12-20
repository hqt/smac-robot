package com.example.shopiesmac;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fpt.robot.Robot;
import com.fpt.robot.RobotException;
import com.fpt.robot.RobotInfo;
import com.fpt.robot.app.RobotActivity;
import com.fpt.robot.motion.RobotMotionLocomotionController;
import com.fpt.robot.motion.RobotMotionStiffnessController;
import com.fpt.robot.shopie.ShopieSMAC;
import com.fpt.robot.shopie.ShopieSMAC.DetectedMarker;
import com.fpt.robot.shopie.ShopieSMAC.MarkerPose;
import com.fpt.robot.shopie.ShopieSensors;
import com.fpt.robot.shopie.ShopieSensors.BumperPosition;
import com.fpt.robot.shopie.ShopieSensors.ButtonPosition;
import com.fpt.robot.shopie.ShopieSensors.CliffPosition;
import com.fpt.robot.shopie.ShopieSensors.WheelPosition;
import com.fpt.robot.tts.RobotTextToSpeech;
import com.fpt.robot.types.RobotMoveTargetPosition;
import com.fpt.robot.vision.RobotObjectDetection;

public class MainActivity extends RobotActivity implements OnClickListener,
		ShopieSensors.Listener {
	private Button mBtScan, mBtMove, mBtSpeak, mBtDetectMarker;
	private EditText mMessage, mX, mY, mTheta;
	private TextView mStatus;
	public ShopieSMAC.Monitor mMarkerMonitor;
	private int markerNumber = 0;
	private float markerX = 0, markerY = 0, markerTheta = 0;
	public static List<Pair<Integer, String>> listOrder = new ArrayList<>();
	private boolean canDoSomething = true;
	public static boolean iamMarker14 = true;

	private boolean hadMoveToPosition = false; // Move to position and say
	private boolean completeMission = false; // Done button
	private int numberOfFoodInPackage = 0;
	private int hadMoveToSTep = 0;
	private boolean isMoveBack = false;
	private boolean stopEverything = false;

	// Get Order
	public static String urlOrder = "http://developer.smac.fpt.com.vn/smac2015/game/api/v1/orders?access_token=";
	public static String urlSaleOff = "http://developer.smac.fpt.com.vn/smac2015/game/api/v1/saleoffs?access_token=";
	public static final String accessToken = "096648c0-a4f1-11e5-a708-b73dceb94e66";
	public static List<String> lstSaleOff = null;
	public static final String USER_AGENT = "Mozilla/5.0";

	private Button btnStartRobot, btnDoneRobot, btnRetryRobot;
	private CheckBox cbIamMarker14;

	@Override
	public void onRobotConnected(String addr, int port) {
		super.onRobotConnected(addr, port);
		try {
			if (getConnectedRobot() != null) {
				if (mMarkerMonitor != null) {
					ShopieSMAC.disableMarkerDetection(getConnectedRobot());
					stopMarkersMonitor();
				}
				if (mSensorsMonitor != null) {
					stopSensorsMonitor();
				}
				mSensorsMonitor = new ShopieSensors.Monitor(getRobot(), this);
				log("Start sensor monitor.");
				try {
					startSensorsMonitor();
				} catch (RobotException e) {
					log("Start sensor monitor exeption: " + e.getMessage());
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			log("On robot connected exeption: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void onRobotDisconnected(String addr, int port) {
		if (getConnectedRobot() != null) {
			RobotInfo info = getConnectedRobot().getInfo();
			if (info != null) {
				if (info.getIpAddress().equalsIgnoreCase(addr)) {

				}
			}
		}
		try {
			if (mMarkerMonitor != null) {
				mMarkerMonitor.stop();
			}
			if (mSensorsMonitor != null) {
				mSensorsMonitor.stop();
			}
		} catch (Exception e) {
			log("On robot disconnected exeption: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceSate) {
		super.onCreate(savedInstanceSate);
		setContentView(R.layout.activity_main);
		
		Toast.makeText(MainActivity.this, "API" + urlOrder, Toast.LENGTH_LONG)
		.show();
		
//		listOrder.add(new Pair<Integer, String>(5, "ô ma chi ở tầng 2"));
//		listOrder.add(new Pair<Integer, String>(3, "xà phòng tắm ở tầng 2"));
//		listOrder.add(new Pair<Integer, String>(4, "bánh gạo ở tầng 2"));
//		listOrder.add(new Pair<Integer, String>(2, "nho ở tầng 2"));
//		listOrder.add(new Pair<Integer, String>(1, "coca ở tầng 2"));

		mBtScan = (Button) findViewById(R.id.scan);
		mBtMove = (Button) findViewById(R.id.move);
		mBtSpeak = (Button) findViewById(R.id.speak);
		mBtDetectMarker = (Button) findViewById(R.id.start_marker_detect);
		btnStartRobot = (Button) findViewById(R.id.start_robot);
		btnDoneRobot = (Button) findViewById(R.id.done_robot);
		btnRetryRobot = (Button) findViewById(R.id.retry_robot);
		cbIamMarker14 = (CheckBox) findViewById(R.id.marker14);
		
		mMessage = (EditText) findViewById(R.id.ed_message);
		mBtScan.setOnClickListener(this);
		mBtMove.setOnClickListener(this);
		mBtSpeak.setOnClickListener(this);
		mBtDetectMarker.setOnClickListener(this);

		mX = (EditText) findViewById(R.id.x);
		mY = (EditText) findViewById(R.id.y);
		mTheta = (EditText) findViewById(R.id.t);

		mStatus = (TextView) findViewById(R.id.status);

		// Cua Quy
		btnStartRobot.setOnClickListener(this);
		btnDoneRobot.setOnClickListener(this);
		btnRetryRobot.setOnClickListener(this);
		// Cua Quy

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.start_marker_detect:
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						startMarkerDetection();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
			break;
		case R.id.scan:
			scan();
			break;
		case R.id.move:
			final float x = Float.parseFloat(mX.getText().toString());
			final float y = Float.parseFloat(mY.getText().toString());
			final float theta = Float.parseFloat(mTheta.getText().toString());
			new Thread(new Runnable() {
				@Override
				public void run() {
					move(x, y, theta, wakeUp, getConnectedRobot());
				}
			}).start();
			break;
		case R.id.speak:
			if(cbIamMarker14.isChecked()) {
				iamMarker14 = true;
			}
			else iamMarker14 = false;
			if (iamMarker14) {
				try {
					RobotTextToSpeech.setCurrentVoice(getConnectedRobot(), RobotTextToSpeech.ROBOT_TTS_LANG_VI, "Female");
				} catch (RobotException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				try {
					RobotTextToSpeech.setCurrentVoice(getConnectedRobot(), RobotTextToSpeech.ROBOT_TTS_LANG_VI, "Male");
				} catch (RobotException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			new Thread(new Runnable() {
				@Override
				public void run() {
					speak(mMessage.getText().toString(),
							RobotTextToSpeech.ROBOT_TTS_LANG_VI);
				}
			}).start();
			break;

		case R.id.start_robot:
			if(cbIamMarker14.isChecked()) {
				iamMarker14 = true;
			}
			else iamMarker14 = false;
			new Thread(new Runnable() {

				@Override
				public void run() {
					// Giai thuat o day

					new Thread(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							try {
								sendGetOrder();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}).start();

					for (int i = 0; i < 1000; i++) {
						if (listOrder.size() != 0) {
							break;
						}
						try {
							
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					Log.d("QUYYYY", "Bat Dau Chay");

					for (int i = 0; i < listOrder.size(); i++) {
						Log.d("QUYYY", "--" + listOrder.get(i));
					}

					if (iamMarker14) {
						try {
							RobotTextToSpeech.setCurrentVoice(getConnectedRobot(), RobotTextToSpeech.ROBOT_TTS_LANG_VI, "Female");
						} catch (RobotException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						do {
							if (listOrder != null) {
								showToast("List order size: "
										+ listOrder.size());
							}

							diLayDo14();
						} while (listOrder.size() != 0);

					} else {
						try {
							RobotTextToSpeech.setCurrentVoice(getConnectedRobot(), RobotTextToSpeech.ROBOT_TTS_LANG_VI, "Male");
						} catch (RobotException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						do {
							if (listOrder != null) {
								showToast("List order size: "
										+ listOrder.size());
							}

							diLayDo11();
						} while (listOrder.size() != 0);
					}
				}
			}).start();
			break;
		case R.id.done_robot:
			new Thread(new Runnable() {

				@Override
				public void run() {
					// Hoan thanh cong viec
					completeMission = true;
				}
			}).start();
			break;

		case R.id.retry_robot:
			stopEverything = false;
			new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub

					Log.d("QUYYYY", "Bat Dau Chay");

					for (int i = 0; i < listOrder.size(); i++) {
						Log.d("QUYYY", "--" + listOrder.get(i));
					}

					if (iamMarker14) {
					
						do {
							if (listOrder != null) {
								showToast("List order size: "
										+ listOrder.size());
							}

							diLayDo14();
						} while (listOrder.size() != 0);

					} else {
						
						do {
							if (listOrder != null) {
								showToast("List order size: "
										+ listOrder.size());
							}

							diLayDo11();
						} while (listOrder.size() != 0);
					}

				}
			}).start();
			break;
		default:
			break;
		}
	}

	public Timer timerDetect;
	public Timer timerMarker;
	public boolean wakeUp = true;
	public boolean mMarkerDetect = false;
	public ShopieSensors.Monitor mSensorsMonitor;

	private void diLayDo14() {
		int hadMoved = 0;
		for (int i = 0; i < 6; i++) {
			if (!isMoveBack) {
				action(i);
				hadMoved = i;
			}
		}
		if (isMoveBack && hadMoved < 3) {
			for (int i = hadMoved; i >= 0; i--) {
				action(i);
			}
			speak("giỏ đã đầy vui lòng lấy hàng xuống",
					RobotTextToSpeech.ROBOT_TTS_LANG_VI);
			numberOfFoodInPackage = 0;
			isMoveBack = false;
		} else {
			for (int i = hadMoved+1; i < 6; i++) {
				action(i);
			}

			speak("giỏ đã đầy vui lòng lấy hàng xuống",
					RobotTextToSpeech.ROBOT_TTS_LANG_VI);
			numberOfFoodInPackage = 0;
			isMoveBack = false;
		}

	}

	private void diLayDo11() {
		int hadMoved = 0;
		for (int i = 0; i < 6; i++) {
			if (!isMoveBack) {
				action_11(i);
				hadMoved = i;
			}
		}
		if (isMoveBack && hadMoved < 3) {
			for (int i = hadMoved; i >= 0; i--) {
				action_11(i);
			}
			speak("giỏ đã đầy vui lòng lấy hàng xuống",
					RobotTextToSpeech.ROBOT_TTS_LANG_VI);
			numberOfFoodInPackage = 0;
			isMoveBack = false;
		} else {
			for (int i = hadMoved+1; i < 6; i++) {
				action_11(i);
			}

			speak("giỏ đã đầy vui lòng lấy hàng xuống",
					RobotTextToSpeech.ROBOT_TTS_LANG_VI);
			numberOfFoodInPackage = 0;
			isMoveBack = false;
		}

	}

	private void action(int n) {
		if(stopEverything) return;
		switch (n) {
		case 0:
			vaoViTriChienDau();
			break;
		case 1:
			vaoKhuVuc1();
			layDoOKhuVuc1();
			break;
		case 2:
			vaoKhuVuc2();
			layDoOKhuVuc2();
			break;
		case 3:
			vaoKhuVuc3();
			layDoOKhuVuc3();
			break;
		case 4:
			vaoKhuVuc4();
			layDoOKhuVuc4();
			break;
		case 5:
			quayTroVe();
			numberOfFoodInPackage = 0;
			break;
		default:
			break;
		}
	}

	private void action_11(int n) {
		if(stopEverything) return;
		switch (n) {
		case 0:
			vaoViTriChienDau();
			break;
		case 1:
			vaoKhuVuc1();
			layDoOKhuVuc3();
			break;
		case 2:
			vaoKhuVuc2();
			layDoOKhuVuc4();
			break;
		case 3:
			vaoKhuVuc3();
			layDoOKhuVuc1();
			break;
		case 4:
			vaoKhuVuc4();
			layDoOKhuVuc2();
			break;
		case 5:
			quayTroVe();
			numberOfFoodInPackage = 0;
			break;
		default:
			break;
		}
	}

	private void vaoViTriChienDau() {
		if (!isMoveBack) {
			move((float) 0, (float) -2, (float) 0, wakeUp, getConnectedRobot());
			move((float) 2.5, (float) 0, (float) 0, wakeUp, getConnectedRobot());
		} else {
			move((float) -2.5, (float) 0, (float) 0, wakeUp,
					getConnectedRobot());
			move((float) 0, (float) 2, (float) 0, wakeUp, getConnectedRobot());
		}

	}

	private void vaoKhuVuc1() {
		if (!isMoveBack) {
			move((float) 0, (float) -1.5, (float) 0, wakeUp,
					getConnectedRobot());
		} else {
			move((float) 0, (float) 1.5, (float) 0, wakeUp, getConnectedRobot());
		}

	}

	private void layDoOKhuVuc1() {
		// Truoc mat
		if (!isMoveBack) {

			for (int i = 0; i < listOrder.size(); i++) {
				if (listOrder.get(i).second.toLowerCase().contains("ô ma chi")
						|| listOrder.get(i).second.toLowerCase().contains("hảo hảo")) {
					if (!hadMoveToPosition) {
						move((float) 1.2, (float) 0, (float) 0, wakeUp,
								getConnectedRobot());
						hadMoveToPosition = true;
					}
					
					if(numberOfFoodInPackage == 3) {
						break;
					}
					
					int numberOfFood = 3 - numberOfFoodInPackage;
					int numberOfFoodSay = 0;
					
					if(listOrder.get(i).first <= numberOfFood) {
						numberOfFoodSay = listOrder.get(i).first;
						numberOfFoodInPackage = numberOfFoodInPackage + listOrder.get(i).first;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.remove(i);
					}else {
						numberOfFoodSay = numberOfFood;
						numberOfFoodInPackage = 3;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.set(i, new Pair<Integer, String>(listOrder.get(i).first - numberOfFood, listOrder.get(i).second));
					}
				}
			}
			if (hadMoveToPosition) {
				move((float) -1.2, (float) 0, (float) 0, wakeUp,
						getConnectedRobot());
				hadMoveToPosition = false;
			}
		}
		// Sau lung
		if (!isMoveBack) {
			for (int i = 0; i < listOrder.size(); i++) {
				if(listOrder.get(i).second.toLowerCase().contains("number one")
						|| listOrder.get(i).second.toLowerCase().contains("tân hiệp phát")) {
					return;
				}
				if (listOrder.get(i).second.toLowerCase().contains("xà phòng tắm")
						|| listOrder.get(i).second.toLowerCase()
								.contains("nước rửa tay khô")
						|| listOrder.get(i).second.toLowerCase().contains("sữa tắm")) {
					if (!hadMoveToPosition) {
						move((float) -1.5, (float) 0, (float) 0, wakeUp,
								getConnectedRobot());
						hadMoveToPosition = true;
					}

					if(numberOfFoodInPackage == 3) {
						break;
					}
					
					int numberOfFood = 3 - numberOfFoodInPackage;
					int numberOfFoodSay = 0;
					
					if(listOrder.get(i).first <= numberOfFood) {
						numberOfFoodSay = listOrder.get(i).first;
						numberOfFoodInPackage = numberOfFoodInPackage + listOrder.get(i).first;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.remove(i);
					}else {
						numberOfFoodSay = numberOfFood;
						numberOfFoodInPackage = 3;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.set(i, new Pair<Integer, String>(listOrder.get(i).first - numberOfFood, listOrder.get(i).second));
					}
				}
			}
			if (hadMoveToPosition) {
				move((float) 1.5, (float) 0, (float) 0, wakeUp,
						getConnectedRobot());
				hadMoveToPosition = false;
			}
		}
	}

	private void vaoKhuVuc2() {
		if (!isMoveBack) {
			move((float) 0, (float) -2, (float) 0, wakeUp, getConnectedRobot());
		} else {
			move((float) 0, (float) 2, (float) 0, wakeUp, getConnectedRobot());
		}

	}

	private void layDoOKhuVuc2() {
		// Truoc mat
		if (!isMoveBack) {
			for (int i = 0; i < listOrder.size(); i++) {
				if (listOrder.get(i).second.toLowerCase().contains("cà phê")
						|| listOrder.get(i).second.toLowerCase().contains("trà nhài")
						|| listOrder.get(i).second.toLowerCase()
								.contains("trà chanh ice tea")) {
					if (!hadMoveToPosition) {
						move((float) 1.2, (float) 0, (float) 0, wakeUp,
								getConnectedRobot());
						hadMoveToPosition = true;
					}

					if(numberOfFoodInPackage == 3) {
						break;
					}
					
					int numberOfFood = 3 - numberOfFoodInPackage;
					int numberOfFoodSay = 0;
					
					if(listOrder.get(i).first <= numberOfFood) {
						numberOfFoodSay = listOrder.get(i).first;
						numberOfFoodInPackage = numberOfFoodInPackage + listOrder.get(i).first;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.remove(i);
					}else {
						numberOfFoodSay = numberOfFood;
						numberOfFoodInPackage = 3;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.set(i, new Pair<Integer, String>(listOrder.get(i).first - numberOfFood, listOrder.get(i).second));
					}
				}
			}
			if (hadMoveToPosition) {
				move((float) -1.2, (float) 0, (float) 0, wakeUp,
						getConnectedRobot());
				hadMoveToPosition = false;
			}
		}
		// Sau lung
		if (!isMoveBack) {
			for (int i = 0; i < listOrder.size(); i++) {
				if (listOrder.get(i).second.toLowerCase().contains("giấy ăn")
						|| listOrder.get(i).second.toLowerCase().contains("giấy ăn")) {
					if (!hadMoveToPosition) {
						move((float) -1.5, (float) 0, (float) 0, wakeUp,
								getConnectedRobot());
						hadMoveToPosition = true;
					}

					if(numberOfFoodInPackage == 3) {
						break;
					}
					
					int numberOfFood = 3 - numberOfFoodInPackage;
					int numberOfFoodSay = 0;
					
					if(listOrder.get(i).first <= numberOfFood) {
						numberOfFoodSay = listOrder.get(i).first;
						numberOfFoodInPackage = numberOfFoodInPackage + listOrder.get(i).first;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.remove(i);
					}else {
						numberOfFoodSay = numberOfFood;
						numberOfFoodInPackage = 3;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.set(i, new Pair<Integer, String>(listOrder.get(i).first - numberOfFood, listOrder.get(i).second));
					}
				}
			}
			if (hadMoveToPosition) {
				move((float) 1.5, (float) 0, (float) 0, wakeUp,
						getConnectedRobot());
				hadMoveToPosition = false;
			}
		}
	}

	private void vaoKhuVuc3() {
		if (!isMoveBack) {
			move((float) 0, (float) -1.5, (float) 0, wakeUp,
					getConnectedRobot());
			move((float) 5, (float) 0, (float) 0, wakeUp, getConnectedRobot());
			move((float) 0, (float) 1.5, (float) 0, wakeUp, getConnectedRobot());
		} else {
			move((float) 0, (float) -1.5, (float) 0, wakeUp,
					getConnectedRobot());
			move((float) 5, (float) 0, (float) 0, wakeUp, getConnectedRobot());
			move((float) 0, (float) 1.5, (float) 0, wakeUp, getConnectedRobot());
		}

	}

	private void layDoOKhuVuc3() {
		// Truoc mat
		if (!isMoveBack) {
			for (int i = 0; i < listOrder.size(); i++) {
				if (listOrder.get(i).second.toLowerCase().contains("táo")
						|| listOrder.get(i).second.toLowerCase().contains("cà rốt")
						|| listOrder.get(i).second.toLowerCase().contains("nho")) {
					if (!hadMoveToPosition) {
						move((float) 1.5, (float) 0, (float) 0, wakeUp,
								getConnectedRobot());
						hadMoveToPosition = true;
					}

					if(numberOfFoodInPackage == 3) {
						break;
					}
					
					int numberOfFood = 3 - numberOfFoodInPackage;
					int numberOfFoodSay = 0;
					
					if(listOrder.get(i).first <= numberOfFood) {
						numberOfFoodSay = listOrder.get(i).first;
						numberOfFoodInPackage = numberOfFoodInPackage + listOrder.get(i).first;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.remove(i);
					}else {
						numberOfFoodSay = numberOfFood;
						numberOfFoodInPackage = 3;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.set(i, new Pair<Integer, String>(listOrder.get(i).first - numberOfFood, listOrder.get(i).second));
					}
				}
			}
			if (hadMoveToPosition) {
				move((float) -1.5, (float) 0, (float) 0, wakeUp,
						getConnectedRobot());
				hadMoveToPosition = false;
			}
		}
		// Sau lung
		if (!isMoveBack) {
			for (int i = 0; i < listOrder.size(); i++) {
				if (listOrder.get(i).second.toLowerCase().contains("kẹo cao su")
						|| listOrder.get(i).second.toLowerCase().contains("bánh quy")
						|| listOrder.get(i).second.toLowerCase().contains("bánh gạo")) {
					if (!hadMoveToPosition) {
						move((float) -1, (float) 0, (float) 0, wakeUp,
								getConnectedRobot());
						hadMoveToPosition = true;
					}

					if(numberOfFoodInPackage == 3) {
						break;
					}
					
					int numberOfFood = 3 - numberOfFoodInPackage;
					int numberOfFoodSay = 0;
					
					if(listOrder.get(i).first <= numberOfFood) {
						numberOfFoodSay = listOrder.get(i).first;
						numberOfFoodInPackage = numberOfFoodInPackage + listOrder.get(i).first;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.remove(i);
					}else {
						numberOfFoodSay = numberOfFood;
						numberOfFoodInPackage = 3;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.set(i, new Pair<Integer, String>(listOrder.get(i).first - numberOfFood, listOrder.get(i).second));
					}
				}
			}
			if (hadMoveToPosition) {
				move((float) 1, (float) 0, (float) 0, wakeUp,
						getConnectedRobot());
				hadMoveToPosition = false;
			}
		}
	}

	private void vaoKhuVuc4() {
		if (!isMoveBack) {
			move((float) 0, (float) 2, (float) 0, wakeUp, getConnectedRobot());
		} else {
			move((float) 0, (float) 2, (float) 0, wakeUp, getConnectedRobot());
		}

	}

	private void layDoOKhuVuc4() {
		// Truoc mat
		if (!isMoveBack) {
			for (int i = 0; i < listOrder.size(); i++) {
				if (listOrder.get(i).second.toLowerCase().contains("sữa chua")
						|| listOrder.get(i).second.toLowerCase().contains("nước suối")
						|| listOrder.get(i).second.toLowerCase().contains("coca")) {
					if (!hadMoveToPosition) {
						move((float) 1.5, (float) 0, (float) 0, wakeUp,
								getConnectedRobot());
						hadMoveToPosition = true;
					}

					if(numberOfFoodInPackage == 3) {
						break;
					}
					
					int numberOfFood = 3 - numberOfFoodInPackage;
					int numberOfFoodSay = 0;
					
					if(listOrder.get(i).first <= numberOfFood) {
						numberOfFoodSay = listOrder.get(i).first;
						numberOfFoodInPackage = numberOfFoodInPackage + listOrder.get(i).first;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.remove(i);
					}else {
						numberOfFoodSay = numberOfFood;
						numberOfFoodInPackage = 3;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.set(i, new Pair<Integer, String>(listOrder.get(i).first - numberOfFood, listOrder.get(i).second));
					}
				}
			}
			if (hadMoveToPosition) {
				move((float) -1.5, (float) 0, (float) 0, wakeUp,
						getConnectedRobot());
				hadMoveToPosition = false;
			}
		}
		// Sau lung
		if (!isMoveBack) {
			for (int i = 0; i < listOrder.size(); i++) {
				if (listOrder.get(i).second.toLowerCase().contains("bim bim hành")
						|| listOrder.get(i).second.toLowerCase().contains("poca")) {
					if (!hadMoveToPosition) {
						move((float) -1, (float) 0, (float) 0, wakeUp,
								getConnectedRobot());
						hadMoveToPosition = true;
					}

					if(numberOfFoodInPackage == 3) {
						break;
					}
					
					int numberOfFood = 3 - numberOfFoodInPackage;
					int numberOfFoodSay = 0;
					
					if(listOrder.get(i).first <= numberOfFood) {
						numberOfFoodSay = listOrder.get(i).first;
						numberOfFoodInPackage = numberOfFoodInPackage + listOrder.get(i).first;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.remove(i);
					}else {
						numberOfFoodSay = numberOfFood;
						numberOfFoodInPackage = 3;
						sayOrderItem(numberOfFoodSay + " " + listOrder.get(i).second);
						listOrder.set(i, new Pair<Integer, String>(listOrder.get(i).first - numberOfFood, listOrder.get(i).second));
					}
				}
			}
			if (hadMoveToPosition) {
				move((float) 1, (float) 0, (float) 0, wakeUp,
						getConnectedRobot());
				hadMoveToPosition = false;
			}
		}
	}

	private void quayTroVe() {
		move((float) 0, (float) 1.5, (float) 0, wakeUp, getConnectedRobot());
		move((float) -7.5, (float) 0, (float) 0, wakeUp, getConnectedRobot());
		move((float) 0, (float) 2, (float) 0, wakeUp, getConnectedRobot());
	}

	private void veTraQua() {
		isMoveBack = true;
	}
	
	
	

	private void sayOrderItem(final String itemName) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				speak("lấy giúp tôi " + itemName,
						RobotTextToSpeech.ROBOT_TTS_LANG_VI);

			}
		}).start();
		for (int i = 0; i < 10; i++) {
			if (completeMission) {
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (numberOfFoodInPackage == 3) {
			isMoveBack = true;
		}
		completeMission = false;
	}

	private boolean waitCommand() {
		for (int i = 0; i < 1000; i++) {
			if (canDoSomething) {
				break;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}

	private boolean getStartPosition() {

		// True start is marker 14
		// False start is marker 11
		try {
			startMarkerDetection();
			for (int i = 0; i < 4; i++) {
				Thread.sleep(100);
				if (markerNumber == 11) {
					return false;
				} else if (markerNumber == 14) {
					return true;
				}
				move(Float.parseFloat("0"), Float.parseFloat("0"),
						Float.parseFloat("1.57"), wakeUp, getConnectedRobot());

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}

	public void startDetectMarker() throws Exception {
		if (getConnectedRobot() == null) {
			scanRobot();
			return;
		}
		log("Start marker detection");
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					ShopieSMAC.enableMarkerDetection(getConnectedRobot());
					mMarkerDetect = false;
				} catch (RobotException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void stopDetectMarker() {
		if (getConnectedRobot() == null) {
			scanRobot();
			return;
		}
		log("Disable marker detection");
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					ShopieSMAC.disableMarkerDetection(getConnectedRobot());
					mMarkerDetect = false;
				} catch (RobotException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	public void onRobotServiceConnected() {
		super.onRobotServiceConnected();
		getConnectedRobot();
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (getConnectedRobot() != null) {
					try {
						log("On Service connected Register monitor");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	public void onNetworkConnected(boolean connected) {
		super.onNetworkConnected(connected);
		if (!connected) {
			wakeUp = false;
		}
		if (mSensorsMonitor != null) {
			try {
				stopSensorsMonitor();
			} catch (RobotException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onDestroy() {
		try {
			if (mMarkerMonitor != null) {
				mMarkerMonitor.stop();
				mMarkerMonitor = null;
			}
			if (mSensorsMonitor != null) {
				mSensorsMonitor.stop();
				mSensorsMonitor = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (getConnectedRobot() != null) {
			try {
				RobotObjectDetection.stopDetection(getConnectedRobot());
			} catch (RobotException e) {
				e.printStackTrace();
			}
		}
		super.onDestroy();
		wakeUp = false;
	}

	public void startMarkerDetection() throws Exception {
		if (getConnectedRobot() == null) {
			scanRobot();
		} else {
			mMarkerMonitor = new ShopieSMAC.Monitor(getConnectedRobot(),
					new ShopieSMAC.Listener() {
						@Override
						public void onMarkersDetected(
								ArrayList<DetectedMarker> arg0) {
							for (final DetectedMarker detectedMarker : arg0) {
								try {
									RobotMotionLocomotionController
											.moveStop(getConnectedRobot());
								} catch (RobotException e1) {
									e1.printStackTrace();
								}
								mMarkerDetect = true;
								MarkerPose mMarkerPose = detectedMarker
										.getPose();
								markerNumber = detectedMarker.getMarkerId();
								markerX = mMarkerPose.x;
								markerY = mMarkerPose.y;
								markerTheta = mMarkerPose.theta;
								log("Relocation Marker POS: "
										+ detectedMarker.getMarkerId() + ": "
										+ mMarkerPose.x + " " + mMarkerPose.y
										+ " " + mMarkerPose.theta);
								try {
									stopMarkersMonitor();
									stopDetectMarker();
								} catch (RobotException e) {
									e.printStackTrace();
								}
							}
						}
					});
			startMarkersMonitor();
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						ShopieSMAC.enableMarkerDetection(getConnectedRobot());
						mMarkerDetect = false;
					} catch (RobotException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}

	public boolean startMarkersMonitor() throws RobotException {
		boolean result = mMarkerMonitor.start();
		log("Start marker monitor " + result);
		return result;
	}

	public boolean stopMarkersMonitor() throws RobotException {
		boolean result = mMarkerMonitor.stop();
		log("Stop markers monitor: " + result);
		return result;
	}

	public boolean startSensorsMonitor() throws RobotException {
		boolean result = mSensorsMonitor.start();
		log("Start sensor monitor: " + result);
		return result;
	}

	public boolean stopSensorsMonitor() throws RobotException {
		boolean result = mSensorsMonitor.stop();
		log("Stop markers monitor: " + result);
		return result;
	}

	public void move(final float x, final float y, final float theta,
			final boolean wakeup, final Robot robot) {
		canDoSomething = false;
		if (getConnectedRobot() == null) {
			scan();
		} else {
			try {
				if (!wakeup) {
					RobotMotionStiffnessController.wakeUp(getConnectedRobot());
				} else {
					RobotMoveTargetPosition position = new RobotMoveTargetPosition(
							x, y, theta);
					boolean b = RobotMotionLocomotionController.moveTo(
							getConnectedRobot(), position);
					if (b) {
						canDoSomething = true;
						log("Move successful");
					} else {
						canDoSomething = true;
						log("Move failed");
					}
				}
			} catch (RobotException e) {
				e.printStackTrace();
			}
		}
	}

	public void speak(String message, String language) {
		if (getConnectedRobot() == null) {
			scan();
		} else {
			try {
				boolean b = RobotTextToSpeech.say(getConnectedRobot(), message,
						language);
				if (b)
					showToast("speak ok");
				else
					showToast("speak failed");
			} catch (RobotException e) {
				log("Speak failed ");
				e.printStackTrace();
			}
		}
	}

	public void showToast(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG)
						.show();
			}
		});
	}

	private void sendGetOrder() throws Exception {

		listOrder = new ArrayList<>();
		String url = urlOrder + accessToken;
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		// add request header
		con.setRequestProperty("User-Agent", USER_AGENT);

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(
				con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		JSONObject jsonObj = new JSONObject(response.toString());
		String status = jsonObj.getString("status");
		if ("success".equals(status)) {
			JSONArray array = jsonObj.getJSONArray("result");
			for (int i = 0; i < array.length(); i++) {
				int num = array.getJSONObject(i).getInt("number");
				String name = array.getJSONObject(i).getString("name");
				JSONObject location = array.getJSONObject(i).getJSONObject(
						"location");
				int floor = location.getInt("floor");
				// int aisle = location.getInt("aisle");
				String result = name + " ở tầng " + floor;
				listOrder.add(new Pair<Integer, String>(num, result));
				System.out.println(result);
			}
		}
		// print result
		// System.out.println(response.toString());

	}

	// HTTP GET request
	private void sendGetSaleOff() throws Exception {

		listOrder = new ArrayList<>();
		String url = urlSaleOff + accessToken;
		lstSaleOff = new ArrayList<String>();
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		// add request header
		con.setRequestProperty("User-Agent", USER_AGENT);

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(
				con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		JSONObject jsonObj = new JSONObject(response.toString());
		String status = jsonObj.getString("status");
		if ("success".equals(status)) {
			JSONArray array = jsonObj.getJSONArray("result");
			for (int i = 0; i < array.length(); i++) {
				String name = array.getJSONObject(i).getString("food_name");
				String location = array.getJSONObject(i).getString("location");
				int time = array.getJSONObject(i).getInt("time");
				String result = name + " vị trí " + location + " thời gian "
						+ time;
				lstSaleOff.add(result);
				System.out.println(result);
			}
		}
		// print result
		// System.out.println(response.toString());

	}

	public void log(final String msg) {
		Log.e("Shoppie", msg);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mStatus.append(msg + "\n");
			}
		});
	}

	@Override
	public void onBumperPressed(BumperPosition bumper) {
		if (bumper == BumperPosition.LEFT) {
			log("Bumper LEFT");
		} else if (bumper == BumperPosition.CENTER) {
			log("Bumper CENTER");
		} else if (bumper == BumperPosition.RIGHT) {
			log("Bumper RIGHT");
		}
		
		stopEverything = true;
		
	}

	@Override
	public void onButtonPressed(ButtonPosition arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCliffDetected(CliffPosition arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onWheelDropped(WheelPosition arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onWheelRaised(WheelPosition arg0) {
		// TODO Auto-generated method stub

	}

}
