package com.uberclone.clone.uberclone.HistoryRecyclerView;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.uberclone.clone.uberclone.HistoryActivity;
import com.uberclone.clone.uberclone.R;

import java.util.List;

/**
 * Created by Admin on 12/20/2018.
 */

public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolder> {

    private List<HistoryObject> itemList;
    private Context mContext;

    public HistoryAdapter(List<HistoryObject> itemList, Context context) {
        this.itemList = itemList;
        this.mContext = context;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);

        HistoryViewHolder hV = new HistoryViewHolder(layoutView);
        return hV;
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {

        holder.time.setText(itemList.get(position).getTime().toString());
        holder.rideId.setText(itemList.get(position).getRideId().toString());
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
