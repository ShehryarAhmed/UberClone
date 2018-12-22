package com.uberclone.clone.uberclone.HistoryRecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.uberclone.clone.uberclone.HistorySingleActivity;
import com.uberclone.clone.uberclone.R;

/**
 * Created by Admin on 12/20/2018.
 */

public class HistoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public TextView time;
    public TextView rideId;


    public HistoryViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        time = (TextView) itemView.findViewById(R.id.time);
        rideId = (TextView) itemView.findViewById(R.id.rideId);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(v.getContext(), HistorySingleActivity.class);
        Bundle b = new Bundle();
        b.putString("rideId",rideId.getText().toString());
        intent.putExtras(b);
        v.getContext().startActivity(intent);
    }
}
