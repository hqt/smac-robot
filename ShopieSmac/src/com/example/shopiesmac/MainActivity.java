package com.example.shopiesmac;

import java.util.ArrayList;
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
		
		mBtScan = (Button) findViewById(R.id.scan);
		mBtMove = (Button) findViewById(R.id.move);
		mBtSpeak = (Button) findViewById(R.id.speak);
		mBtDetectMarker = (Button) findViewById(R.id.start_marker_detect);
		mMessage = (EditText) findViewById(R.id.ed_message);
		mBtScan.setOnClickListener(this);
		mBtMove.setOnClickListener(this);
		mBtSpeak.setOnClickListener(this);
		mBtDetectMarker.setOnClickListener(this);
		
		mX = (EditText) findViewById(R.id.x);
		mY = (EditText) findViewById(R.id.y);
		mTheta = (EditText) findViewById(R.id.t);
		
		mStatus = (TextView) findViewById(R.id.status);
		
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
		default:
			break;
		}
	}
	
	public Timer timerDetect;
	public Timer timerMarker;
	public boolean wakeUp = true;
	public boolean mMarkerDetect = false;
	public ShopieSensors.Monitor mSensorsMonitor;
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
						log("Move successful");
					} else {
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
