package com.uberclone.clone.uberclone;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DriverSettingActivity extends AppCompatActivity {

    private static final String TAG = "DriverSettingActivity";
    private ImageView mProfileImg;
    private EditText mNameEdit;
    private EditText mPhoneEdit;
    private EditText mCarField;

    private Button mConfirm;
    private Button mBack;

    private FirebaseAuth mAuth;
    private DatabaseReference mDriverDatabase;

    private String userID;
    private String mUserName;
    private String mPhone;
    private String mCarColor;
    private String mSerivces;
    private String mProfileImageUrl;
    private Uri resultUri;

    private RadioGroup mRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_setting);

        mProfileImg = (ImageView) findViewById(R.id.driverProfileImage);
        mNameEdit = (EditText) findViewById(R.id.DriverName);
        mPhoneEdit = (EditText) findViewById(R.id.driverPhone);
        mCarField = (EditText) findViewById(R.id.driverCar);
        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);

        mConfirm = (Button) findViewById(R.id.confirm);
        mBack = (Button) findViewById(R.id.back);

        mProfileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
//                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
//                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

//                startActivityForResult(intent,1);
                startActivityForResult(intent, 1);

            }
        });

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        mDriverDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userID);

        getUSerInfo();
        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveUserInformation();
            }
        });

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                return;
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mProfileImg.setImageURI(resultUri);
        }
    }

    private void getUSerInfo(){
        mDriverDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0 ){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name") != null){
                        mUserName = map.get("name").toString();
                        mNameEdit.setText(mUserName);
                    }
                    if(map.get("phone") != null){
                        mPhone = map.get("phone").toString();
                        mPhoneEdit.setText(mPhone);

                    }
                    if(map.get("car") != null){
                        mCarColor = map.get("car").toString();
                        mCarField.setText(mCarColor);

                    }
                    if(map.get("service") != null){
                        mSerivces = map.get("service").toString();
                        switch (mSerivces){
                            case "UberX":
                                mRadioGroup.check(R.id.uberX);
                                break;
                            case "UberBlack":
                                mRadioGroup.check(R.id.uberBlack);
                                break;
                            case "UberXl":
                                mRadioGroup.check(R.id.uberXl);
                                break;

                        }

                    }
                    if(map.get("profileImageUrl") != null){
                        mProfileImageUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplicationContext()).load(mProfileImageUrl).into(mProfileImg);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void saveUserInformation() {

        mUserName = mNameEdit.getText().toString();
        mPhone   = mPhoneEdit.getText().toString();
        mCarColor = mCarField.getText().toString();

        int selectedID = mRadioGroup.getCheckedRadioButtonId();

        final RadioButton radioButton = (RadioButton) findViewById(selectedID);

        if(radioButton.getText() == null){
            return;
        }

        mSerivces = radioButton.getText().toString();
        Map userInfo = new HashMap();

        userInfo.put("name",mUserName);
        userInfo.put("phone",mPhone);
        userInfo.put("car",mCarColor);
        userInfo.put("service",mSerivces);
        mDriverDatabase.updateChildren(userInfo);

        if(resultUri != null){
            final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("Profile_images").child(userID);
            Bitmap bitmap = null;
            try{
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
            }catch (IOException e){
                e.printStackTrace();
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, byteArrayOutputStream);
            byte[] data = byteArrayOutputStream.toByteArray();
            UploadTask uploadTask = filePath.putBytes(data);

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String DownloadUrl = uri.toString();
                            Log.d(TAG, "onSuccess: "+DownloadUrl);

                            Map newImage = new HashMap();
                            newImage.put("profileImageUrl",DownloadUrl.toString());
                            mDriverDatabase.updateChildren(newImage);

                            finish();
                            return;

                        }
                    });


                }
            });

        }

        finish();
    }

}
