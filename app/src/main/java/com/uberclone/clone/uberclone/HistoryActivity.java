package com.uberclone.clone.uberclone;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.uberclone.clone.uberclone.HistoryRecyclerView.HistoryAdapter;
import com.uberclone.clone.uberclone.HistoryRecyclerView.HistoryObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class HistoryActivity extends AppCompatActivity {

    private RecyclerView mHistoryRecuclerView;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;
    private List<HistoryObject> resultHistory;

    private String customerOrDriver, userID;

    private TextView mBalance;
    private EditText mPayoutEmail;
    private Button mPayout;
    private Double Balance = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        resultHistory = new ArrayList<HistoryObject>();

        mBalance = (TextView) findViewById(R.id.balance);
        mPayoutEmail = (EditText) findViewById(R.id.payOutEmail);
        mPayout = (Button) findViewById(R.id.payout);


        mHistoryRecuclerView = (RecyclerView) findViewById(R.id.historyRecyclerView);
        mHistoryRecuclerView.setNestedScrollingEnabled(false);
        mHistoryRecuclerView.setHasFixedSize(true);

        mHistoryLayoutManager = new LinearLayoutManager(HistoryActivity.this);
        mHistoryRecuclerView.setLayoutManager(mHistoryLayoutManager);
        mHistoryAdapter = new HistoryAdapter(getDataSetHistory(), HistoryActivity.this);

        mHistoryRecuclerView.setAdapter(mHistoryAdapter);

        customerOrDriver = getIntent().getExtras().getString("customerOrDriver");
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid().toString();
        getUserHistoryIds();

        if(customerOrDriver.equals("Drivers")){
            mBalance.setVisibility(View.VISIBLE );
            mPayout.setVisibility(View.VISIBLE );
            mPayoutEmail.setVisibility(View.VISIBLE );
        }

        mPayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void getUserHistoryIds(){
        DatabaseReference HistoryDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(customerOrDriver).child(userID).child("History");
        HistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot history : dataSnapshot.getChildren()){
                        fetchRideInformation(history.getKey());
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

    }

    private void fetchRideInformation(String rideKey) {
        DatabaseReference userHistoryDatabase = FirebaseDatabase.getInstance().getReference().child("History").child(rideKey);
        userHistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String rideId = dataSnapshot.getKey();
                    Long timeStamp = 0l;

                    String distance = "";
                    Double ridePrice = 0.0;

                    for(DataSnapshot child : dataSnapshot.getChildren()){

                        if(child.getKey().equals("timeStamp")){
                            timeStamp = Long.valueOf(child.getValue().toString());
                        }

                        if(dataSnapshot.child("customerPaid") != null && dataSnapshot.child("driverPaidOut") == null){
                            if(dataSnapshot.child("distance").getValue() != null){
                                distance = dataSnapshot.child("distance").getValue().toString();
                                ridePrice = (Double.valueOf(dataSnapshot.child("price").getValue().toString())) ;
                                Balance += ridePrice;
                                mBalance.setText("Balance : "+Balance+" $");
                            }
                        }

                    }

                    resultHistory.add(new HistoryObject(rideId, getTime(timeStamp)));
                    mHistoryAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

    }

    private String getTime(Long timeStamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timeStamp*1000);
        String date = DateFormat.format("dd-mm-yyyy hh:mm", cal).toString();
        return date;
    }

    public List<HistoryObject> getDataSetHistory() {
        return resultHistory;
    }

    public static final MediaType MEDIA_TYPE = MediaType.parse("application/json");
    ProgressDialog progress;
    private void payoutRequest(){
        progress = new ProgressDialog(this);
        progress.setTitle("Processing Your Payment");
        progress.setMessage("Please wait");
        progress.setCancelable(false);
        progress.show();

        final OkHttpClient client = new OkHttpClient();
        JSONObject postData = new JSONObject();

        try {
            postData.put("uid", FirebaseAuth.getInstance().getUid());
            postData.put("email", mPayoutEmail.getText().toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(MEDIA_TYPE,postData.toString());
        final Request request = new Request.Builder()
                .url("https://us-central1-asherauth.cloudfunctions.net/payouthttps://us-central1-asherauth.cloudfunctions.net/payout")
                .post(body)
                .addHeader("Conmtent-Type","application/json")
                .addHeader("cache-cntrol","no-cache")
                .addHeader("Authorization","Your Token")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                progress.dismiss();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                int ressponseCode = response.code();
                if(response.isSuccessful()){
                    switch (ressponseCode){
                        case 200:
                            Snackbar.make(findViewById(R.id.layout),"Payout Successfully", Snackbar.LENGTH_LONG).show();
                            break;
                        case 500:
                            Snackbar.make(findViewById(R.id.layout),"Payout Successfully", Snackbar.LENGTH_LONG).show();
                            break;
                        default:
                            Snackbar.make(findViewById(R.id.layout),"Error: Cloud not complete payout", Snackbar.LENGTH_LONG).show();
                            break;
                    }
                    progress.dismiss();
                }
            }
        });

    }
}
