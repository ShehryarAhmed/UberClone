package com.uberclone.clone.uberclone;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.uberclone.clone.uberclone.HistoryRecyclerView.HistoryAdapter;
import com.uberclone.clone.uberclone.HistoryRecyclerView.HistoryObject;

import java.util.ArrayList;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class HistoryActivity extends AppCompatActivity {

    private RecyclerView mHistoryRecuclerView;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;
    private List<HistoryObject> resultHistory;

    private String customerOrDriver, userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        resultHistory = new ArrayList<HistoryObject>();

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

                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        if(child.getKey().equals("timeStamp")){
                            timeStamp = Long.valueOf(child.getValue().toString());
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
}
