package com.example.keywordspotting;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InferenceAdapter extends RecyclerView.Adapter<InferenceAdapter.ViewHolder>{
    private List<InferenceEntity> inferences;
    private OnItemClickListener listener;

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public interface OnItemClickListener {
        void onItemClick(InferenceEntity inference);
    }

    public InferenceAdapter(List<InferenceEntity> inferences, OnItemClickListener listener) {
        this.inferences = inferences;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View v = LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        InferenceEntity inf = inferences.get(position);
        holder.bind(inf, listener);
    }

    @Override
    public int getItemCount() {
        return inferences.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;
        public ViewHolder(View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
        public void bind(InferenceEntity inf, OnItemClickListener listener) {
            text1.setText(inf.getTimestamp());
            String desc = inf.getType() + " | " + context.getString(R.string.Click_Here);
            text2.setText(desc);
            itemView.setOnClickListener(v -> listener.onItemClick(inf));
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void notifyChanges(List<InferenceEntity> newInferences) {
        this.inferences.clear();
        if(newInferences != null && !newInferences.isEmpty()){
            this.inferences.addAll(newInferences);
        }
        this.notifyDataSetChanged();
    }
}
