package com.frz.refreshloadlayout;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private RecyclerView my;

    private RefreshLoadLayout layout;

    private TextView header, footer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        my = findViewById(R.id.my);
        layout = findViewById(R.id.layout);
        header = findViewById(R.id.header);
        footer = findViewById(R.id.footer);
        final LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        MyAdapter adapter = new MyAdapter();
        my.setLayoutManager(manager);
        my.setAdapter(adapter);
        initListener();
    }

    private void initListener() {
        layout.setListener(new RefreshLoadLayout.OnChangeListener() {
            @Override
            public void headerChange(final RefreshLoadLayout layout, int nowH, int maxH, int action) {
                if(action == RefreshLoadLayout.UP){
                    header.setText("刷新中.......");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            header.setText("加载完成");
                            layout.finish();
                        }
                    }, 2000);
                }
            }
            @Override
            public void footerChange(final RefreshLoadLayout layout, int nowH, int maxH, int action) {
//                Log.e("TAG", "footer "+nowH+" "+" "+action);
                if(action == RefreshLoadLayout.UP){
                    footer.setText("加载中.......");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            footer.setText("加载完成");
                            layout.finish();
                        }
                    }, 2000);
                }
            }
        });
    }
}
