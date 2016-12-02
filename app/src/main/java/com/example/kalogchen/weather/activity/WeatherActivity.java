package com.example.kalogchen.weather.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.kalogchen.weather.R;
import com.example.kalogchen.weather.service.AutoUpdateService;
import com.example.kalogchen.weather.util.HttpCallbackListener;
import com.example.kalogchen.weather.util.HttpUtil;
import com.example.kalogchen.weather.util.Utility;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by kalogchen on 2016/11/29.
 */

public class WeatherActivity extends Activity implements View.OnClickListener {

    private LinearLayout weatherInfoLayout;

    //城市名
    private TextView cityName;

    //发布时间
    private TextView updateTime;

    //天气图片
    private ImageView ivWeather;

    //天气描述信息
    private TextView weatherDesc;

    //气温
    private TextView temp;

    //当前日期
    private TextView currentData;

    //切换天气按钮
    private Button switchCity;

    //更新天气按钮
    private Button refreshWeather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.weather_layout);

        //初始化控件
        weatherInfoLayout = (LinearLayout) findViewById(R.id.layout_weather_info);
        cityName = (TextView) findViewById(R.id.tv_city_name);
        updateTime = (TextView) findViewById(R.id.tv_update_time);
        ivWeather = (ImageView) findViewById(R.id.iv_weather);
        weatherDesc = (TextView) findViewById(R.id.tv_weather_desc);
        temp = (TextView) findViewById(R.id.temp);
        currentData = (TextView) findViewById(R.id.tv_current_data);
        switchCity = (Button) findViewById(R.id.bt_switch_city);
        refreshWeather = (Button) findViewById(R.id.bt_refresh);

        String countyName = getIntent().getStringExtra("county_name");
        if (!TextUtils.isEmpty(countyName)) {
            //有县级名称时就去查询天气
            updateTime.setText("更新中...");
            weatherInfoLayout.setVisibility(View.INVISIBLE);
            cityName.setVisibility(View.INVISIBLE);
            queryWeatherInfo(countyName);
        } else {
            //没有县级代号时就显示本地天气
            showWeather();
        }
        switchCity.setOnClickListener(this);
        refreshWeather.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_switch_city:
                Intent intent = new Intent(this, ChooseAreaActivity.class);
                intent.putExtra("from_weather_activity", true);
                startActivity(intent);
                finish();
                break;
            case R.id.bt_refresh:
                updateTime.setText("更新中...");
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                String city_name = sp.getString("city_name", null);
                if (!TextUtils.isEmpty(city_name)) {
                    queryWeatherInfo(city_name);
                }
                break;
            default:
                break;
        }

    }

    /**
     * 查询县级拼接请求链接
     *
     * @param cityName
     */
    private void queryWeatherInfo(String cityName) {
        String address = "http://api.map.baidu.com/telematics/v3/weather?location=" + cityName +"&output=json&ak=YCSK2uqKRutUv3EA8fxA6IEyKRWPp4a1&mcode=B4:B2:3D:54:1E:B7:F4:D0:6F:B5:2F:D6:8F:4A:23:75:82:DD:D3:28;com.example.kalogchen.weather";
        queryFromServer(address);
    }


    /**
     * 根据传入的地址和类型去向服务器查询天气代号或天气信息
     *
     * @param address
     */
    private void queryFromServer(final String address) {
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                    if (!TextUtils.isEmpty(response)) {
                        //处理服务器返回的天气信息
                        Utility.handleWeatherResponse(WeatherActivity.this, response);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showWeather();
                            }
                        });
                    }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateTime.setText("更新失败");
                    }
                });
            }
        });

    }

    /**
     * 从SharedPreferences文件中读取存储的天气信息，并显示到界面上
     */
    private void showWeather() {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        cityName.setText(sp.getString("city_name", "解析失败"));
        temp.setText(sp.getString("temp", ""));
        weatherDesc.setText(sp.getString("weatherDesc", ""));
        updateTime.setText("更新时间：" + sp.getString("publish_time", ""));
        //获取时间年月日
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        String format = dateFormat.format(new java.util.Date());
        Log.d("ss", "--------现在日期-------" + format);
        currentData.setText(format);
        weatherInfoLayout.setVisibility(View.VISIBLE);
        cityName.setVisibility(View.VISIBLE);

        //新建子线程，加载图片
        new Thread(new Runnable() {
            @Override
            public void run() {
                String dayNight;
                SimpleDateFormat hh = new SimpleDateFormat("HH");
                String format = hh.format(new Date());
                int i = Integer.parseInt(format);
                Log.d("ss", "--------------时针为:-----" + i);
                if (i < 18 && i > 6) {
                    dayNight = sp.getString("dayPictureUrl", "");
                }else {
                    dayNight = sp.getString("nightPictureUrl", "");
                }
                final Bitmap bitmap = getHttpBitmap(dayNight);
                //view的post方法是运行在主线程中的，因为所有view都自带一个handler，所有handler都有post方法
                ivWeather.post(new Runnable() {
                    @Override
                    public void run() {
                        ivWeather.setImageBitmap(bitmap);
                    }
                });
            }
        }).start();

        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

    public static Bitmap getHttpBitmap(String address) {
        URL myFileURL;
        Bitmap bitmap=null;
        try{
            myFileURL = new URL(address);
            //获得连接
            HttpURLConnection conn=(HttpURLConnection)myFileURL.openConnection();
            //设置超时时间为6000毫秒，conn.setConnectionTiem(0);表示没有时间限制
            conn.setConnectTimeout(6000);
            //连接设置获得数据流
            conn.setDoInput(true);
            //不使用缓存
            conn.setUseCaches(false);
            //这句可有可无，没有影响
            conn.connect();
            //得到数据流
            InputStream is = conn.getInputStream();
            Log.d("ss", "--获取到的图片数据流：---------" + is);
            //解析得到图片
            bitmap = BitmapFactory.decodeStream(is);
            //关闭数据流
            is.close();
        }catch(Exception e){
            Log.d("ss", "------------------天气图片获取失败");
        }
        return bitmap;
    }

}
