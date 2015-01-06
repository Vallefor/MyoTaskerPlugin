/*
 * Copyright 2013 two forty four a.m. LLC <http://www.twofortyfouram.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.damageddev.myotaskerplugin.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.damageddev.myotaskerplugin.EditActivity;
import com.damageddev.myotaskerplugin.R;
import com.damageddev.myotaskerplugin.utils.Constants;
import com.damageddev.myotaskerplugin.utils.TaskerPlugin;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;




public final class BackgroundService extends Service {
    public static final int NOTIFICATION_ID = 6592;

    public static final String ACTION_DISCONNECT = "com.damageddev.myotaskerplugin.ACTION_DISCONNECT";
    public static final String ACTION_CONNECT = " com.damageddev.myotaskerplugin.ACTION_CONNECT";

    private static final String SHOW_MYO_STATUS_NOTIFICATION = "show_myo_status_notification";
    private static final Intent INTENT_REQUEST_REQUERY =
            new Intent(com.twofortyfouram.locale.Intent.ACTION_REQUEST_QUERY)
                    .putExtra(com.twofortyfouram.locale.Intent.EXTRA_ACTIVITY,
                            EditActivity.class.getName());

	private boolean isUnlocked;
	
    private Toast mToast;
    private Hub mHub;
	
	private Vector3 vect;
	private Vector3 gyro;

	private float roll;
	private float pitch;
	private float yaw;

	private Vector3 lastVect;
	private Vector3 lastGyro;

	private float lastRoll;
	private float lastPitch;
	private float lastYaw;
	private long lastTimePose=0;
	
	private boolean rollUnlock=false;
	private long lastPoseTime=0;
	private int poseInRow=0;
	private long lastTimestampRealtimeSend=0;
	private boolean realTimeProgress=false;
	

	private long unlockTime;
	
	private Pose lastPos;
	private Pose lastPosFull=Pose.UNKNOWN;


	private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;

    private long mLastUnlockTime = 0;
	private long mLastUnlockGesture = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        mHub = Hub.getInstance();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //mHub.setLockingPolicy(Hub.LockingPolicy.NONE);
    }

    private DeviceListener mListener = new AbstractDeviceListener() {
        @Override
        public void onConnect(Myo myo, long timestamp) {
            showToast(getString(R.string.myo_connected));

            SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_MULTI_PROCESS);
			unlockTime = Long.valueOf(sharedPreferences.getString("relock_time", "5")) * 1000;

            if (sharedPreferences.getBoolean(SHOW_MYO_STATUS_NOTIFICATION, true)) {
                mNotificationBuilder = buildDisconnectNotification()
                        .setContentTitle(getString(R.string.myo_connected))
                        .setContentText(myo.getName())
                        .setSubText(myo.getMacAddress());
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
            }
        }

		@Override
		public void onUnlock (Myo myo, long timestamp)
		{
			//myo.vibrate(Myo.VibrationType.LONG);
			//isUnlocked=true;
		}

		@Override
		public void onLock (Myo myo, long timestamp)
		{
			//isUnlocked=false;
		}

        @Override
        public void onDisconnect(Myo myo, long timestamp) {

            SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_MULTI_PROCESS);

            if (sharedPreferences.getBoolean(SHOW_MYO_STATUS_NOTIFICATION, true)) {
                mNotificationBuilder = buildConnectNotification()
                        .setContentTitle(getString(R.string.disconnected_from_myo))
                        .setContentText(getString(R.string.last_connected_to) + " " + myo.getName());
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
            }
        }
		
		@Override
		public void	onGyroscopeData (Myo myo, long timestamp, Vector3 gyr)
		{
			gyro=gyr;
		}

		@Override
		public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {

			isUnlocked = (mLastUnlockTime + unlockTime) > timestamp;
			if(!isUnlocked)
				rollUnlock=false;

			if(lastPosFull!=Pose.REST && lastPosFull!=Pose.UNKNOWN)
				poseInRow++;
			
			// Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
			roll = (float) Math.toDegrees(Quaternion.roll(rotation));
			pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
			yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));
			// Adjust roll and pitch for the orientation of the Myo on the arm.
			if (myo.getXDirection() != XDirection.TOWARD_ELBOW) {
				roll *= -1;
				pitch *= -1;
			}
			//Log.i("MainActivity", "dif: "+(timestamp-lastPoseTime)+" "+(lastPos==Pose.DOUBLE_TAP)+" "+(lastRoll-roll)/*+" ru:"+rollUnlock*/);
			Log.i("MainActivity", "posInRow dif: "+poseInRow);

			if(lastPos==Pose.DOUBLE_TAP && /*Math.abs(lastRoll-roll)>30*/vect.length()>2  && (timestamp-lastPoseTime)<1000 && !rollUnlock && !isUnlocked)
			{
				rollUnlock=true;
				mLastUnlockTime = timestamp;
				mLastUnlockGesture=timestamp;
				myo.vibrate(Myo.VibrationType.SHORT);
			}
			
			if(isUnlocked && poseInRow>30)
			{
				if(!realTimeProgress)
				{
					Log.i("LastRoll","lastRoll:"+lastRoll);
				}
				realTimeProgress=true;
				if((timestamp-lastTimestampRealtimeSend)>300)
				{
					lastTimestampRealtimeSend = timestamp;
					mLastUnlockTime = timestamp;
					if(lastPos== Pose.FIST) {
						Bundle bundle = getPositionBundle("first_hold");
						TaskerPlugin.Event.addPassThroughData(INTENT_REQUEST_REQUERY, bundle);
						BackgroundService.this.sendBroadcast(INTENT_REQUEST_REQUERY);
					}
					if(lastPos== Pose.WAVE_IN) {
						Bundle bundle = getPositionBundle("wave_in_hold");
						TaskerPlugin.Event.addPassThroughData(INTENT_REQUEST_REQUERY, bundle);
						BackgroundService.this.sendBroadcast(INTENT_REQUEST_REQUERY);
					}
					if(lastPos== Pose.WAVE_OUT) {
						Bundle bundle = getPositionBundle("wave_out_hold");
						TaskerPlugin.Event.addPassThroughData(INTENT_REQUEST_REQUERY, bundle);
						BackgroundService.this.sendBroadcast(INTENT_REQUEST_REQUERY);
					}
					if(lastPos== Pose.FINGERS_SPREAD) {
						Bundle bundle = getPositionBundle("fingers_spread_hold");
						TaskerPlugin.Event.addPassThroughData(INTENT_REQUEST_REQUERY, bundle);
						BackgroundService.this.sendBroadcast(INTENT_REQUEST_REQUERY);
					}
					lastGyro = gyro;
					lastVect = vect;
					lastPitch = pitch;
					lastRoll = roll;
					lastYaw = yaw;
				}
			}
				
		}

        @Override
        public void onAccelerometerData(Myo myo, long timestamp, Vector3 vec) {
            vect=vec;
			Log.i("AccelVector","Length: "+vect.length());
        }

		public Bundle getPositionBundle(String pose)
		{
			Bundle bundle = new Bundle();

			bundle.putString(Constants.POSE, pose);
			bundle.putDouble(Constants.ACCEL_X, vect.x());
			bundle.putDouble(Constants.ACCEL_Y, vect.y());
			bundle.putDouble(Constants.ACCEL_Z, vect.z());

			bundle.putDouble(Constants.GYRO_X, gyro.x());
			bundle.putDouble(Constants.GYRO_X_DIFF, lastGyro.x()-gyro.x());
			bundle.putDouble(Constants.GYRO_Y, gyro.y());
			bundle.putDouble(Constants.GYRO_Y_DIFF, lastGyro.y()-gyro.y());
			bundle.putDouble(Constants.GYRO_Z, gyro.z());
			bundle.putDouble(Constants.GYRO_Z_DIFF, lastGyro.z()-gyro.z());

			bundle.putDouble(Constants.ROLL, roll);
			bundle.putDouble(Constants.ROLL_DIFF, lastRoll-roll);
			bundle.putDouble(Constants.PITCH, pitch);
			bundle.putDouble(Constants.PITCH_DIFF, lastPitch-pitch);
			bundle.putDouble(Constants.YAW, yaw);
			bundle.putDouble(Constants.YAW_DIFF, lastYaw-yaw);
			return bundle;
		}
		
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {

			if((timestamp-mLastUnlockGesture)<700/* || (poseInRow<2 && pose != Pose.DOUBLE_TAP)*/)
			{
				Log.i("MainActivity", "lastPosFull false: "+pose.toString());
				return;
			}
			/*if(timestamp-lastTimePose<50)
				return;*/
			lastTimePose=timestamp;
            //showToast(pose.toString());
			
			Log.i("MainActivity", "poseInRow reset");
			poseInRow=0;
			
			if(pose!=Pose.REST && pose!=Pose.UNKNOWN)
			{
				lastPos=pose;
				lastPoseTime=timestamp;
				lastGyro = gyro;
				lastVect = vect;
				lastPitch = pitch;
				lastRoll = roll;
				lastYaw = yaw;
			}
			

            SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_MULTI_PROCESS);

            //long unlockTime = Long.valueOf(sharedPreferences.getString("relock_time", "5")) * 1000;

            //boolean isUnlocked = (mLastUnlockTime + unlockTime) > timestamp;

			/*
            if (pose == Pose.DOUBLE_TAP && !isUnlocked && rollUnlock) {
                mLastUnlockTime = timestamp;
                myo.vibrate(Myo.VibrationType.SHORT);
            }
            */

			

			if(isUnlocked && (pose==Pose.REST || pose==Pose.UNKNOWN) && !realTimeProgress)
			{
				Log.i("SendStatic",lastPosFull.toString());
				Bundle bundle = getPositionBundle(lastPosFull.toString());
				TaskerPlugin.Event.addPassThroughData(INTENT_REQUEST_REQUERY, bundle);
				BackgroundService.this.sendBroadcast(INTENT_REQUEST_REQUERY);
				mLastUnlockTime = timestamp;
			}
            if (isUnlocked && (pose != Pose.REST || pose != Pose.UNKNOWN)) {
				/*
                TaskerPlugin.Event.addPassThroughData(INTENT_REQUEST_REQUERY, bundle);
                BackgroundService.this.sendBroadcast(INTENT_REQUEST_REQUERY);
                mLastUnlockTime = timestamp;
                */

                if (sharedPreferences.getBoolean("show_toasts", true)) {
                    showToast(pose.toString());
                }

                if (sharedPreferences.getBoolean(SHOW_MYO_STATUS_NOTIFICATION, true)) {
                    mNotificationBuilder
                            .setSubText(myo.getMacAddress())
                            .setContentTitle(myo.getName())
                            .setContentText(getString(R.string.last_gesture) + " " + pose.toString());
                    mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
                }
                
            }
			lastPosFull=pose;
			Log.i("MainActivity", "lastPosFull: "+lastPosFull.toString());
			if (pose == Pose.DOUBLE_TAP && isUnlocked) {
				mLastUnlockTime=0;
				isUnlocked=false;
				rollUnlock=false;
				lastPos=Pose.UNKNOWN;
				myo.vibrate(Myo.VibrationType.SHORT);
			}
			realTimeProgress=false;
        }

        @Override
        public void onAttach(Myo myo, long timestamp) {
            super.onAttach(myo, timestamp);

            SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_MULTI_PROCESS);

            if (sharedPreferences.getBoolean(SHOW_MYO_STATUS_NOTIFICATION, true)) {
                mNotificationBuilder
                        .setContentTitle(getString(R.string.myo_attached))
                        .setContentText(myo.getName());
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
            }
        }

        @Override
        public void onDetach(Myo myo, long timestamp) {
            super.onDetach(myo, timestamp);

            mHub.attachToAdjacentMyo();

            SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_MULTI_PROCESS);

            if (sharedPreferences.getBoolean(SHOW_MYO_STATUS_NOTIFICATION, true)) {
                mNotificationBuilder
                        .setContentTitle(getString(R.string.myo_detached))
                        .setContentText(myo.getName())
                        .setContentText(getString(R.string.detached_myo) + " " + myo.getName());
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
            }
        }

        @Override
        public void onArmUnsync(Myo myo, long timestamp) {
            super.onArmUnsync(myo, timestamp);

            SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_MULTI_PROCESS);

            if (sharedPreferences.getBoolean(SHOW_MYO_STATUS_NOTIFICATION, true)) {
                mNotificationBuilder.setContentTitle(getString(R.string.myo_needs_sync));
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
            }
        }
    };

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if (TextUtils.isEmpty(action)) {
                init();
            } else {
                if (action.equals(ACTION_DISCONNECT)) {
                    mHub.shutdown();
                    mNotificationManager.cancelAll();
                } else if (action.equals(ACTION_CONNECT)) {
                    init();
                }
            }
        }

        return START_STICKY;
    }

    private void init() {
        if (!mHub.init(this, getPackageName())) {
            stopSelf();
        }
		
		mHub.setLockingPolicy(Hub.LockingPolicy.NONE);
        mHub.removeListener(mListener);
        mHub.addListener(mListener);
        mHub.attachToAdjacentMyo();

        mNotificationBuilder = buildConnectNotification();
    }

    private NotificationCompat.Builder buildConnectNotification() {
        Intent connectIntent = new Intent(ACTION_DISCONNECT);
        PendingIntent connectPendingIntent = PendingIntent.getService(this, 0, connectIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(BackgroundService.this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.myo_connected))
                .setOngoing(true)
                .addAction(R.drawable.ic_connect, getString(R.string.connect_to_myo), connectPendingIntent);
    }

    private NotificationCompat.Builder buildDisconnectNotification() {
        Intent disconnectIntent = new Intent(ACTION_DISCONNECT);
        PendingIntent disconnectPendingIntent = PendingIntent.getService(this, 0, disconnectIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(BackgroundService.this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.myo_connected))
                .setOngoing(true)
                .addAction(R.drawable.ic_disconnect, getString(R.string.disconnect), disconnectPendingIntent);
    }

    @Override
    public IBinder onBind(final Intent arg0) {
        return null;
    }

    private void showToast(String text) {
        if (mToast == null) {
            mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
        }

        mToast.show();
    }

}