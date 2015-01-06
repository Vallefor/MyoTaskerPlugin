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

package com.damageddev.myotaskerplugin.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.damageddev.myotaskerplugin.utils.BundleScrubber;
import com.damageddev.myotaskerplugin.utils.Constants;
import com.damageddev.myotaskerplugin.utils.TaskerPlugin;

import net.dinglisch.android.tasker.TaskerIntent;

public final class QueryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {

        if (!com.twofortyfouram.locale.Intent.ACTION_QUERY_CONDITION.equals(intent.getAction())) {
            return;
        }

        BundleScrubber.scrub(intent);

        final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(bundle);

        Bundle myExtras = TaskerPlugin.Event.retrievePassThroughData(intent);
		//myExtras.getDouble(Constants.ACCEL_X);

        if (myExtras != null && myExtras.getString(Constants.POSE).equals(intent.getExtras().get(Constants.POSE))) {
			Bundle varsBundle=new Bundle();
			
			String accel_x = String.valueOf(myExtras.getDouble(Constants.ACCEL_X));
			String accel_y = String.valueOf(myExtras.getDouble(Constants.ACCEL_Y));
			String accel_z = String.valueOf(myExtras.getDouble(Constants.ACCEL_Z));

			String gyro_x = String.valueOf(myExtras.getDouble(Constants.GYRO_X));
			String gyro_x_diff = String.valueOf(myExtras.getDouble(Constants.GYRO_X_DIFF));
			String gyro_y = String.valueOf(myExtras.getDouble(Constants.GYRO_Y));
			String gyro_y_diff = String.valueOf(myExtras.getDouble(Constants.GYRO_Y_DIFF));
			String gyro_z = String.valueOf(myExtras.getDouble(Constants.GYRO_Z));
			String gyro_z_diff = String.valueOf(myExtras.getDouble(Constants.GYRO_Z_DIFF));

			String roll = String.valueOf(myExtras.getDouble(Constants.ROLL));
			String roll_diff = String.valueOf(myExtras.getDouble(Constants.ROLL_DIFF));
			String pitch = String.valueOf(myExtras.getDouble(Constants.PITCH));
			String pitch_diff = String.valueOf(myExtras.getDouble(Constants.PITCH_DIFF));
			String yaw = String.valueOf(myExtras.getDouble(Constants.YAW));
			String yaw_diff = String.valueOf(myExtras.getDouble(Constants.YAW_DIFF));
			
			varsBundle.putString("%myo_accel_x", accel_x);
			varsBundle.putString("%myo_accel_y", accel_y);
			varsBundle.putString("%myo_accel_z", accel_z);
			
			varsBundle.putString("%myo_gyro_x", gyro_x);
			varsBundle.putString("%myo_gyro_x_diff", gyro_x_diff);
			varsBundle.putString("%myo_gyro_y", gyro_y);
			varsBundle.putString("%myo_gyro_y_diff", gyro_y_diff);
			varsBundle.putString("%myo_gyro_z", gyro_z);
			varsBundle.putString("%myo_gyro_z_diff", gyro_z_diff);
			
			varsBundle.putString("%myo_roll", roll);
			varsBundle.putString("%myo_roll_diff", roll_diff);
			varsBundle.putString("%myo_pitch", pitch);
			varsBundle.putString("%myo_pitch_diff", pitch_diff);
			varsBundle.putString("%myo_yaw", yaw);
			varsBundle.putString("%myo_yaw_diff", yaw_diff);
			

			TaskerPlugin.addVariableBundle(getResultExtras(true), varsBundle);

			setResultCode(com.twofortyfouram.locale.Intent.RESULT_CONDITION_SATISFIED);
        }
    }
}