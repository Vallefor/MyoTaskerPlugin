package com.damageddev.myotaskerplugin.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.damageddev.myotaskerplugin.services.BackgroundService;
import com.damageddev.myotaskerplugin.utils.TaskerPlugin;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by agessel on 11/13/14.
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, BackgroundService.class));
    }
}
