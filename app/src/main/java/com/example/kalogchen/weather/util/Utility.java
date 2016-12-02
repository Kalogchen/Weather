package com.example.kalogchen.weather.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.example.kalogchen.weather.db.WeatherDB;
import com.example.kalogchen.weather.model.City;
import com.example.kalogchen.weather.model.County;
import com.example.kalogchen.weather.model.Province;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by kalogchen on 2016/11/26.
 */

public class Utility {

    /**
     * 解析和处理服务器返回的省级数据
     */
    public synchronized static boolean handleProvincesResponse(WeatherDB weatherDB, String response) {

        if (!TextUtils.isEmpty(response)) {
            String[] allProvince = response.split(",");
            if (allProvince != null && allProvince.length > 0) {
                for (String p : allProvince) {
                    String[] array = p.split("\\|");
                    Province province = new Province();
                    province.setProvinceCode(array[0]);
                    province.setProvinceName(array[1]);
                    weatherDB.saveProvince(province);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的市级数据
     */
    public static boolean handleCitiesResponse(WeatherDB weatherDB, String response, int provinceId) {
        if (!TextUtils.isEmpty(response)) {
            String[] allCities = response.split(",");
            if (allCities != null && allCities.length > 0) {
                for (String c : allCities) {
                    String[] array = c.split("\\|");
                    City city = new City();
                    city.setCityCode(array[0]);
                    city.setCityName(array[1]);
                    city.setProvinceId(provinceId);
                    weatherDB.saveCity(city);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的县级数据
     */
    public static boolean handleCountiesResponse(WeatherDB weatherDB, String response, int cityId) {
        if (!TextUtils.isEmpty(response)) {
            String[] allCounties = response.split(",");
            if (allCounties != null && allCounties.length > 0) {
                for (String c : allCounties) {
                    String[] array = c.split("\\|");
                    County county = new County();
                    county.setCountyCode(array[0]);
                    county.setCountyName(array[1]);
                    county.setCityId(cityId);
                    weatherDB.saveCounty(county);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 解析服务器返回的json数据，并将解析出的数据保存到本地。
     */
    public static void handleWeatherResponse(Context context, String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray results = jsonObject.getJSONArray("results");
            if (results.length() > 0) {
                JSONObject obj = results.getJSONObject(0);
                String cityName = obj.getString("currentCity");
                JSONArray weather_data = obj.getJSONArray("weather_data");
                JSONObject object = weather_data.getJSONObject(0);
                String temp = object.getString("temperature");
                String wetherDesc = object.getString("weather");
                String pTime = object.getString("date");
                String dayPictureUrl = object.getString("dayPictureUrl");
                String nightPictureUrl = object.getString("nightPictureUrl");
                Log.d("dd", "解析到的数据-----" + "----" + cityName + "----" + wetherDesc + "----" +  pTime + "----" + dayPictureUrl + "----" + nightPictureUrl);

                saveWeatherInfo(context, cityName, temp, wetherDesc, pTime, dayPictureUrl, nightPictureUrl);
            }


        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("dd", "json解析失败---------");
        }
    }

    /**
     * 将服务器返回的所有天气信息存储到SharedPreferences文件中
     */
    public static void saveWeatherInfo(Context context, String cityName, String temp,String weatherDesc,
                                       String pTime, String dayPictureUrl, String nightPictureUrl) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = sp.edit();
        edit.putBoolean("city_selected", true);
        edit.putString("city_name", cityName);
        edit.putString("temp", temp);
        edit.putString("weatherDesc", weatherDesc);
        edit.putString("publish_time", pTime);
        edit.putString("dayPictureUrl", dayPictureUrl);
        edit.putString("nightPictureUrl", nightPictureUrl);
        edit.commit();
    }

}