package com.zyc.smartcontrol.clock.setting;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.zyc.smartcontrol.R;

public class ClockSettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rgbsetting);

        int deviceNum = -1;
        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null)//判断是否有值传入,并判断是否有特定key
        {
            if (bundle.containsKey("deviceNum")) deviceNum = bundle.getInt("deviceNum");//获取传入值
            if (bundle.containsKey("title"))  setTitle(bundle.getString("title"));
        }
        //加载PrefFragment
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        ClockSettingFragment prefFragment = new ClockSettingFragment(deviceNum);
        transaction.add(R.id.SettingFragment, prefFragment);
        transaction.commit();
    }
}
