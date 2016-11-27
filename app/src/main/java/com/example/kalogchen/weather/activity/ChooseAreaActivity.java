package com.example.kalogchen.weather.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kalogchen.weather.R;
import com.example.kalogchen.weather.db.WeatherDB;
import com.example.kalogchen.weather.model.City;
import com.example.kalogchen.weather.model.County;
import com.example.kalogchen.weather.model.Province;
import com.example.kalogchen.weather.util.HttpCallbackListener;
import com.example.kalogchen.weather.util.HttpUtil;
import com.example.kalogchen.weather.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kalogchen on 2016/11/27.
 */

public class ChooseAreaActivity extends Activity {

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;
    private TextView tv_title;
    private ListView lv_area;
    private ArrayAdapter<String> adapter;
    private WeatherDB weatherDB;
    private List<String> dataList = new ArrayList<String>();

    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;

    /**
     * 县列表
     */
    private List<County> countyList;

    /**
     * 选中的省份
     */
    private Province selectedProvince;

    /**
     * 选中的城市
     */
    private City selectedCity;

    /**
     * 选中的级别
     */
    private int currentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);

        tv_title = (TextView) findViewById(R.id.tv_title);
        lv_area = (ListView) findViewById(R.id.lv_area);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
        lv_area.setAdapter(adapter);
        weatherDB = WeatherDB.getInstance(this);
        lv_area.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }

            }
        });
        queryProvinces();//加载省数据
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果数据库没有则从服务器查询
     */
    private void queryProvinces(){
        provinceList = weatherDB.loadProvinces();
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            lv_area.setSelection(0);
            tv_title.setText("中国");
            currentLevel = LEVEL_PROVINCE;
        }else {
            queryFromServer(null, "province");
        }
    }

    /**
     * 查询选中的省内所有的市，优先从数据库查询，如果数据库没有则从服务器查询
     */
    private void queryCities() {
        cityList = weatherDB.loadCities(selectedProvince.getId());
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            lv_area.setSelection(0);
            tv_title.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        }else {
            queryFromServer(selectedProvince.getProvinceCode(), "city");
        }
    }

    /**
     * 查询选中城市内所有的县，优先从数据库查询，如果数据库没有则从服务器查询
     */
    private void queryCounties() {
        countyList = weatherDB.loadCounties(selectedCity.getId());
        if (countyList.size() > 0) {
            dataList.clear();;
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            lv_area.setSelection(0);
            tv_title.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        }else {
            queryFromServer(selectedCity.getCityCode(), "county");
        }
    }

    /**
     * 根据传入的代号和类型从服务器上查询省市县数据
     */
    private void queryFromServer(final String code, final String type) {
        String address;
        if (!TextUtils.isEmpty(code)) {
            address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
        }else {
            address = "http://www.weather.com.cn/data/list3/city.xml";
        }
        shouwProgressDialog();

        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvincesResponse(weatherDB, response);
                }else  if ("city".equals(type)) {
                    result = Utility.handleCitiesResponse(weatherDB, response, selectedProvince.getId());
                }else if ("county".equals(type)) {
                    result = Utility.handleCountiesResponse(weatherDB, response, selectedCity.getId());
                }
                if (result) {
                    //通过runOnUiThread（）方法回到主线程呢个处理逻辑
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            }else if ("city".equals(type)) {
                                queryCities();
                            }else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                //通过runOnUiThread（）方法回到主线程处理逻辑
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
    }

    /**
     * 显示进度对话框
     */
    private void shouwProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载中...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    /**
     * 捕获back按键，根据当前级别来判断，此时应该返回市列表、省类表还是直接退出。
     */
    @Override
    public void onBackPressed() {
        if (currentLevel == LEVEL_COUNTY) {
            queryCities();
        }else if (currentLevel == LEVEL_CITY) {
            queryProvinces();
        }else {
            finish();
        }
    }
}