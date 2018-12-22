package com.uberclone.clone.uberclone;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button mDrive, mCustomer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrive = (Button) findViewById(R.id.driver);
        mCustomer = (Button) findViewById(R.id.customer);

        startService(new Intent(MainActivity.this,OnAppKilled.class));
        mDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,DriverLoginActivity.class));
                finish();
            }
        });

        mCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,CustomerLoginActivity.class));
                finish();
            }
        });
    }
}
