package ranavisu.bt200;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import ranavisu.bt200.demo.RA_Navisu;
import ranavisu.bt200.demo.Test1RaNavisu;
import ranavisu.bt200.demo.TestRaNavisu_screenshot;

import jp.epson.moverio.bt200.DisplayControl;
import jp.epson.moverio.bt200.SensorControl;
import ranavisu.bt200.demo.TestWiFiActivity;

public class MainActivity extends Activity {

    private DisplayControl mDisplayControl;
    private SensorControl mSensorControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDisplayControl = new DisplayControl(MainActivity.this);
        mSensorControl = new SensorControl(MainActivity.this);

        Button demo01 = (Button) findViewById(R.id.demo_01);
        Button demo02 = (Button) findViewById(R.id.demo_02);
        Button demo03 = (Button) findViewById(R.id.demoWiFi);
        Button demo04 = (Button) findViewById(R.id.demoRAnavisu);

        demo01.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Test1RaNavisu.class);
                startActivity(intent);
            }
        });


        demo02.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TestRaNavisu_screenshot.class);
                startActivity(intent);
            }
        });

        demo03.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TestWiFiActivity.class);
                startActivity(intent);
            }
        });

        demo04.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RA_Navisu.class);
                startActivity(intent);
            }
        });


    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // We are not using 3D mode in this Activity so we make sure that we are in 2D mode
        mDisplayControl.setMode(DisplayControl.DISPLAY_MODE_2D, false);

        // We ensure that the current Gyroscope is the headset one
        // in case it was changed in Demo1BT200CtrlActivity
        // note that there is no function to find out which Gyroscope is currently selected
        mSensorControl.setMode(SensorControl.SENSOR_MODE_HEADSET);
    }
}
