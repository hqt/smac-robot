package com.example.shopiesmac;

import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
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

public class MainActivity extends RobotActivity implements OnClickListener, ShopieSensors.Listener {
	private Button mBtScan, mBtMove, mBtSpeak, mBtDetectMarker;
	private EditText mMessage, mX, mY, mTheta;
	private TextView mStatus;
	public ShopieSMAC.Monitor mMarkerMonitor;
	private int markerNumber = 0;
	private float markerX = 0, markerY = 0, markerTheta = 0;
	private List<String> listOrder = new ArrayList<String>();
	private boolean canDoSomething = true;
	public static boolean iamMarker14 = true;
	
	private Button btnStartRobot, btnDoneRobot;
	
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
		listOrder.add("Omachi");
		listOrder.add("Giấy ăn");
		listOrder.add("Táo");
		listOrder.add("Socola");
		listOrder.add("Poca");
		mBtScan = (Button) findViewById(R.id.scan);
		mBtMove = (Button) findViewById(R.id.move);
		mBtSpeak = (Button) findViewById(R.id.speak);
		mBtDetectMarker = (Button) findViewById(R.id.start_marker_detect);
		btnStartRobot = (Button) findViewById(R.id.start_robot);
		btnDoneRobot = (Button) findViewById(R.id.done_robot);
		mMessage = (EditText) findViewById(R.id.ed_message);
		mBtScan.setOnClickListener(this);
		mBtMove.setOnClickListener(this);
		mBtSpeak.setOnClickListener(this);
		mBtDetectMarker.setOnClickListener(this);
		
		mX = (EditText) findViewById(R.id.x);
		mY = (EditText) findViewById(R.id.y);
		mTheta = (EditText) findViewById(R.id.t);
		
		mStatus = (TextView) findViewById(R.id.status);
		
		
		//Cua Quy
		btnStartRobot.setOnClickListener(this);
		btnDoneRobot.setOnClickListener(this);
		
		
		
		//Cua Quy
		
		
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
			new Thread(new Runnable() {
				@Override
				public void run() {
					speak(mMessage.getText().toString(), RobotTextToSpeech.ROBOT_TTS_LANG_VI);
				}
			}).start();
			break;
			
		
		case R.id.start_robot:
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					// Giai thuat o day
					Log.d("QUYYYY", "Bat Dau Chay");
					if(iamMarker14) {
						
						//Vao vi tri chien dau
						move((float) 0, (float) -2, (float) 0, wakeUp, getConnectedRobot());
						move((float) 2.5, (float) 0, (float) 0, wakeUp, getConnectedRobot());
						
						//Vao khu vuc 1
						move((float) 0, (float) -1.5, (float) 0, wakeUp, getConnectedRobot());
						
						//Lay do o khu vuc 1
						if(listOrder.get(0).toLowerCase().contains("omachi") || listOrder.get(0).toLowerCase().contains("hảo hảo")) {
							
						}
						
						//Vao khu vuc 2
						move((float) 0, (float) -2, (float) 0, wakeUp, getConnectedRobot());
						move((float) 0, (float) -1.5, (float) 0, wakeUp, getConnectedRobot());
						move((float) 5, (float) 0, (float) 0, wakeUp, getConnectedRobot());
						move((float) 0, (float) 1.5, (float) 0, wakeUp, getConnectedRobot());
						
						//Lay do o khu vuc 2
						
						//Vao khu vuc 3
						move((float) 0, (float) 2, (float) 0, wakeUp, getConnectedRobot());
						
						//Lay do o khu vuc 3
						
						//Vao khu vuc 4
						move((float) 0, (float) 1.5, (float) 0, wakeUp, getConnectedRobot());
						
						//Lay do o khu vuc 4
						
						//Quay tro ve
						move((float) -7.5, (float) 0, (float) 0, wakeUp, getConnectedRobot());
						move((float) 0, (float) 2, (float) 0, wakeUp, getConnectedRobot());

					} else {
						//Dang o vi tri 11
						Log.d("QUYYYY", "Dang o vi tri 11");
                        move((float) 0, (float) 0, (float) -1.57, wakeUp, getConnectedRobot());
                        move((float) 2, (float) 0, (float) 0, wakeUp, getConnectedRobot());
                        move((float) 0, (float) 0, (float) 1.57, wakeUp, getConnectedRobot());
                        move((float) 2.5, (float) 0, (float) 0, wakeUp, getConnectedRobot());
                        move((float) 0, (float) 0, (float) -1.57, wakeUp, getConnectedRobot());
                        move((float) 1, (float) 0, (float) 0, wakeUp, getConnectedRobot());
					}
				}
			}).start();
			break;
		case R.id.done_robot:
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					// Hoan thanh cong viec
					
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

    private void sayOrderItem(final String itemName) {
    	new Thread(new Runnable() {
			@Override
			public void run() {
				speak(itemName, RobotTextToSpeech.ROBOT_TTS_LANG_VI);
			}
		}).start();
    }
    
    private boolean waitCommand() {
    	for(int i = 0; i < 1000; i++) {
    		if(canDoSomething) {
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
		
		//True start is marker 14
		//False start is marker 11
		try {
			startMarkerDetection();
			for(int i = 0; i < 4; i++) {
				Thread.sleep(100);
				if(markerNumber == 11) {
					return false;
				} else if (markerNumber == 14) {
					return true;
				}
				move(Float.parseFloat("0"), Float.parseFloat("0"), Float.parseFloat("1.57"), wakeUp, getConnectedRobot());

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
			mMarkerMonitor = new ShopieSMAC.Monitor(getConnectedRobot(), new ShopieSMAC.Listener() {
				@Override
				public void onMarkersDetected(ArrayList<DetectedMarker> arg0) {
					for (final DetectedMarker detectedMarker : arg0) {
						try {
							RobotMotionLocomotionController.moveStop(getConnectedRobot());
						} catch (RobotException e1) {
							e1.printStackTrace();
						}
						mMarkerDetect = true;
						MarkerPose mMarkerPose = detectedMarker.getPose();
						markerNumber = detectedMarker.getMarkerId();
						markerX = mMarkerPose.x;
						markerY = mMarkerPose.y;
						markerTheta = mMarkerPose.theta;
						log("Relocation Marker POS: " + detectedMarker.getMarkerId() + ": " + mMarkerPose.x + " " + mMarkerPose.y + " " + mMarkerPose.theta);
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
	public void move(final float x, final float y, final float theta, final boolean wakeup, final Robot robot) {
		canDoSomething = false;
		if(getConnectedRobot() == null) {
			scan();
		} else {
			try {
				if (!wakeup) {
					RobotMotionStiffnessController.wakeUp(getConnectedRobot());
				} else {
					RobotMoveTargetPosition position = new RobotMoveTargetPosition(x, y, theta);
					boolean b = RobotMotionLocomotionController.moveTo(getConnectedRobot(), position);
					if(b) {
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
		if(getConnectedRobot() == null) {
			scan();
		} else {
			try {
				boolean b = RobotTextToSpeech.say(getConnectedRobot(), message, language);
				if(b) showToast("speak ok"); else showToast("speak failed");
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
				Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
			}
		});
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
