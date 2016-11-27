package com.example.kalogchen.weather.util;

/**
 * Created by kalogchen on 2016/11/26.
 */

public interface HttpCallbackListener {

    void onFinish(String response);

    void onError(Exception e);
}
