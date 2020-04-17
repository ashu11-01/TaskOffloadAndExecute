package com.demo.nearbyfiletransfer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class SplashScreen extends AppCompatActivity {

    ImageView splashImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        initview();
        splash();
    }

    private void splash() {
        Animation splashAnim = AnimationUtils.loadAnimation(this,android.R.anim.fade_in);
        Thread t = new Thread(){
            @Override
            public void run() {
//                super.run();
                try{
                    Thread.sleep(5000);
                }catch (InterruptedException e){}
                finally {
                    startActivity(new Intent(SplashScreen.this,MainActivity.class));
                    finish();
                }
            }
        };
        t.start();
    }

    private void initview() {
        splashImage = (ImageView)findViewById(R.id.imageView);
    }

}
