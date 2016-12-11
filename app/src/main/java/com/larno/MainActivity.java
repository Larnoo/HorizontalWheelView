package com.larno;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.larno.widget.HorizontalWheelView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    HorizontalWheelView mHorizontalWheelView5;
    private TextView mSelectedTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        mHorizontalWheelView5 = (HorizontalWheelView) findViewById(R.id.wheelview5);
        mSelectedTv = (TextView) findViewById(R.id.selected_tv);

        final List<String> items = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            items.add(String.valueOf(i * 1000));
        }


        mHorizontalWheelView5.setItems(items);
        mHorizontalWheelView5.setOnWheelItemSelectedListener(new HorizontalWheelView.OnWheelItemSelectedListener() {
            @Override
            public void onWheelItemSelected(int position) {
                mSelectedTv.setText("选择：" + items.get(position) + "万");
            }
        });
    }
}
