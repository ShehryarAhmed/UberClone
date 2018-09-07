package com.uberclone.clone.uberclone;

import android.content.Intent;
import android.icu.text.DateFormat;
import android.icu.text.UnicodeSetSpanner;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginActivity extends AppCompatActivity {

    private EditText mEmail, mPassword;
    private Button mLoginBtn, mRegisterBtn;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        mAuth = FirebaseAuth.getInstance();

        firebaseAuthListner =  new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user  = FirebaseAuth.getInstance().getCurrentUser();
                if(user != null){
                    Intent intent = new Intent(DriverLoginActivity.this, DriverMapActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        mEmail = (EditText) findViewById(R.id.email);
        mPassword = (EditText) findViewById(R.id.password);

        mLoginBtn = (Button) findViewById(R.id.login_driver);
        mRegisterBtn = (Button) findViewById(R.id.register_driver);

        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String email = mEmail.getText().toString().trim();
                final String password = mPassword.getText().toString().trim();
                mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(DriverLoginActivity.this, "Some thing went wrong", Toast.LENGTH_SHORT).show();
                        }else{
                            String userID = mAuth.getCurrentUser().getUid();
                            DatabaseReference current_user_db =
                                    FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userID);
                            current_user_db.setValue(true);
//                            startActivity(new Intent(DriverLoginActivity.this, DriverMapActivity.class));

                        }
                    }
                });

            }
        });

        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String email = mEmail.getText().toString().trim();
                final String password = mPassword.getText().toString().trim();

                mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                        Toast.makeText(DriverLoginActivity.this, "Some thing wrong", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(DriverLoginActivity.this, "now login", Toast.LENGTH_SHORT).show();

                            startActivity(new Intent(DriverLoginActivity.this, DriverMapActivity.class));
                            String userID = mAuth.getCurrentUser().getUid();
                            DatabaseReference current_user_db =
                                    FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userID);
                            current_user_db.setValue(true);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.addAuthStateListener(firebaseAuthListner);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.removeAuthStateListener(firebaseAuthListner);
    }
}
