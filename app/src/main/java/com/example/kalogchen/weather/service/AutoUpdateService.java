package com.example.kalogchen.weather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.example.kalogchen.weather.receiver.AutoUpdateReceiver;
import com.example.kalogchen.weather.util.HttpCallbackListener;
import com.example.kalogchen.weather.util.HttpUtil;
import com.example.kalogchen.weather.util.Utility;

/**
 * Created by kalogchen on 2016/12/2.
 */

public class AutoUpdateService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateWeather();
            }
        }).start();

        //创建更新频率，这里为8小时
        int time = 1000 * 60 * 60 * 8;
        long triggerAtTime = SystemClock.elapsedRealtime() + time;

        Intent intent1 = new Intent(this, AutoUpdateReceiver.class);
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent1, 0);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pendingIntent);

        return super.onStartCommand(intent, flags, startId);
    }

    private void updateWeather() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String cityName = sp.getString("city_name", "");
        String address = "http://api.map.baidu.com/telematics/v3/weather?location=" + cityName +"&output=json&ak=YCSK2uqKRutUv3EA8fxA6IEyKRWPp4a1&mcode=B4:B2:3D:54:1E:B7:F4:D0:6F:B5:2F:D6:8F:4A:23:75:82:DD:D3:28;com.example.kalogchen.weather";
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                Utility.handleWeatherResponse(AutoUpdateService.this, response);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });

    }
}
