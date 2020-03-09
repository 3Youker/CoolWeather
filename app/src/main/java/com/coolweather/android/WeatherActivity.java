package com.coolweather.android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;
import java.util.prefs.PreferenceChangeEvent;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    public DrawerLayout drawerLayout;
    private Button navBu;
    public SwipeRefreshLayout swipeRefresh;
    private ScrollView wL;
    private TextView titleCity;
    private TextView tU;
    private TextView dT;
    private TextView wI;
    private LinearLayout fL;
    private TextView aT;
    private TextView pT;
    private TextView cT;
    private TextView cW;
    private TextView sT;
    private ImageView bingP;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>=21){
            View dV=getWindow().getDecorView();
            dV.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        drawerLayout=(DrawerLayout)findViewById(R.id.drawer_layout);
        navBu=(Button)findViewById(R.id.nav_button);
        swipeRefresh=(SwipeRefreshLayout)findViewById(R.id.ref);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        bingP=(ImageView)findViewById(R.id.bing_pic);
        wL=(ScrollView)findViewById(R.id.weather_layout);
        titleCity=(TextView)findViewById(R.id.title_city);
        tU=(TextView)findViewById(R.id.title_update_time);
        dT=(TextView)findViewById(R.id.degree_text);
        wI=(TextView)findViewById(R.id.weather_info_text);
        fL=(LinearLayout)findViewById(R.id.forecast_layout);
        aT=(TextView)findViewById(R.id.aqi_text);
        pT=(TextView)findViewById(R.id.pm25_text);
        cT=(TextView)findViewById(R.id.comfort_text);
        cW=(TextView)findViewById(R.id.car_wash_text);
        sT=(TextView)findViewById(R.id.sport_text);
        navBu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        SharedPreferences pres= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString=pres.getString("weather",null);
        final String wId;
        String bP=pres.getString("bing_pic",null);
        if(bP!=null){
            Glide.with(this).load(bP).into(bingP);
        }else {
            loadP();
        }
        if (weatherString!=null){
            Weather wt= Utility.handleWeatherResponse(weatherString);
            wId=wt.basic.weatherId;
            sWI(wt);
        }else {
            wId=getIntent().getStringExtra("weather_id");
            wL.setVisibility(View.INVISIBLE);
            rW(wId);
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                rW(wId);
            }
        });
    }
    public void rW(final String wId){
        String wU="http://guolin.tech/api/weather?cityid="+wId+"&key=481b9b44c4f3498598fdf351616f8663";
        HttpUtil.sendOkHttpRequest(wU, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取失败",Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);

                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final String responseT=response.body().string();
                final Weather weather=Utility.handleWeatherResponse(responseT);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null&&"ok".equals(weather.status)){
                            SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseT);
                            editor.apply();
                            sWI(weather);
                        }else {
                            Toast.makeText(WeatherActivity.this,"获取失败",Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });

            }
        });
        loadP();

    }
    public void sWI(Weather weather){
        String cN=weather.basic.cityName;
        String uT=weather.basic.update.updateTime.split(" ")[1];
        String degree=weather.now.temperature + "℃";
        String wIo=weather.now.more.info;
        titleCity.setText(cN);
        tU.setText(uT);
        dT.setText(degree);
        wI.setText(wIo);
        fL.removeAllViews();
        for (Forecast forecast:weather.forecastList){
            View view= LayoutInflater.from(this).inflate(R.layout.forecast_items,fL,false);
            TextView dT=(TextView)view.findViewById(R.id.date_text);
            TextView iT=(TextView)view.findViewById(R.id.info_text);
            TextView mT=(TextView)view.findViewById(R.id.max_text);
            TextView miT=(TextView)view.findViewById(R.id.min_text);
            dT.setText(forecast.date);
            iT.setText(forecast.more.info);
            mT.setText(forecast.temperature.max);
            miT.setText(forecast.temperature.min);
            fL.addView(view);
        }
        if (weather.aqi!=null){
            aT.setText(weather.aqi.city.aqi);

            pT.setText(weather.aqi.city.pm25);
        }
        String cf="舒适度："+weather.suggestion.comfort.info;
        String cw="洗车指数："+weather.suggestion.carwash.info;
        String sp="运动指数:"+weather.suggestion.sport.info;
        cT.setText(cf);
        cW.setText(cw);
        sT.setText(sp);
        wL.setVisibility(View.VISIBLE);


    }
    private void loadP(){
        String rqBp="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(rqBp, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final String bP=response.body().string();
                SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bP);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bP).into(bingP);
                    }
                });
            }
        });
    }
}
