package com.example.administrator.bluetoothchat_2;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Administrator on 2018/8/4.
 */

public class LoadingAlertDialog extends AlertDialog {

    private ImageView progressImg;
    private TextView message;
    //旋转动画
    private Animation animation;

    public LoadingAlertDialog(Context context) {
        super(context, R.style.LoadDialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_alert_dialog);

        //点击imageview外侧区域，动画不会消失
        setCanceledOnTouchOutside(false);

        progressImg = (ImageView) findViewById(R.id.refreshing_img);
        message = (TextView) findViewById(R.id.message);

        //加载动画资源
        animation = AnimationUtils.loadAnimation(getContext(), R.anim.loading_progress_rotate);
        //动画完成后，是否保留动画最后的状态，设为true
        animation.setFillAfter(true);
    }

    public void show(String msg) {
        super.show();
        if (message != null) {
            message.setText(msg);
        }
    }

    /**
     * 在AlertDialog的 onStart() 生命周期里面执行开始动画
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (animation != null) {
            progressImg.startAnimation(animation);
        }
    }

    @Override
    protected void onStop() {
        progressImg.clearAnimation();
    }
}

