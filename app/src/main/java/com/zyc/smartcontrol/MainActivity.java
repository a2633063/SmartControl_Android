package com.zyc.smartcontrol;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.iot.esptouch.demo_activity.EsptouchDemoActivity;
import com.zyc.smartcontrol.button.ButtonFragment;
import com.zyc.smartcontrol.clock.ClockFragment;
import com.zyc.smartcontrol.button.setting.ButtonSettingActivity;
import com.zyc.smartcontrol.clock.setting.ClockSettingActivity;
import com.zyc.smartcontrol.rgb.RGBFragment;
import com.zyc.smartcontrol.rgb.setting.RGBSettingActivity;

import java.util.ArrayList;

import static java.util.Arrays.asList;

public class MainActivity extends AppCompatActivity {
    final String Tag = "MainActivity_Tag";

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    private ArrayList<Fragment> fragmentArray;
    private ViewPager mViewPager;
    private FragmentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSharedPreferences = getSharedPreferences("Setting", 0);
        mEditor = mSharedPreferences.edit();

        mViewPager = (ViewPager) findViewById(R.id.viewpager);

        fragmentArray = new ArrayList<Fragment>();
        fragmentArray.add(new ClockFragment(0));
        fragmentArray.add(new RGBFragment(1));
        fragmentArray.add(new ButtonFragment(2));
        ArrayList<String> fragmentTitle = new ArrayList<>(
                asList(
                        "Clock设置","RGB设置", "Button设置"
                ));
        adapter = new FragmentAdapter(getSupportFragmentManager(), fragmentArray, fragmentTitle);

        mViewPager.setAdapter(adapter);
        mViewPager.setAdapter(new FragmentAdapter(getSupportFragmentManager(), fragmentArray, null));
        mViewPager.setOffscreenPageLimit(4);
        mViewPager.setCurrentItem(mSharedPreferences.getInt("page",0));
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                mEditor.putInt("page", position);
                mEditor.commit();
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }


    //region 右上角菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.menu_wifi:
                startActivity(new Intent(MainActivity.this, EsptouchDemoActivity.class));
                break;
            case R.id.menu_setting:
                Intent intent;

                if (fragmentArray.get(mViewPager.getCurrentItem()).getClass().equals(RGBFragment.class)) {
                    intent = new Intent(MainActivity.this, RGBSettingActivity.class);
                } else if (fragmentArray.get(mViewPager.getCurrentItem()).getClass().equals(ButtonFragment.class)) {
                    intent = new Intent(MainActivity.this, ButtonSettingActivity.class);
                } else if (fragmentArray.get(mViewPager.getCurrentItem()).getClass().equals(ClockFragment.class)) {
                    intent = new Intent(MainActivity.this, ClockSettingActivity.class);
                } else {
                    return true;
                }
                //用Bundle携带数据
                Bundle bundle = new Bundle();
                bundle.putInt("deviceNum", mViewPager.getCurrentItem());
                bundle.putString("title", adapter.getPageTitle(mViewPager.getCurrentItem()).toString());
                intent.putExtras(bundle);
                startActivity(intent);


                return true;
//            break;
        }

        return super.onOptionsItemSelected(item);
    }

    //endregion
    class FragmentAdapter extends FragmentPagerAdapter {

        private ArrayList<Fragment> fragmentArray;
        private ArrayList<String> title;

        public FragmentAdapter(FragmentManager fm, ArrayList<Fragment> fragmentArray, ArrayList<String> title) {
            this(fm);
            this.fragmentArray = fragmentArray;
            this.title = title;
        }

        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        //这个函数的作用是当切换到第arg0个页面的时候调用。
        @Override
        public Fragment getItem(int arg0) {
            return this.fragmentArray.get(arg0);
        }

        @Override
        public int getCount() {
            return this.fragmentArray.size();
        }

        //重写这个方法，将设置每个Tab的标题
        @Override
        public CharSequence getPageTitle(int position) {
            if (title != null)
                return title.get(position);
            else return "";
        }


    }

}
