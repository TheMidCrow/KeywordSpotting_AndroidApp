package com.example.keywordspotting;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InferenceAdapter extends RecyclerView.Adapter<InferenceAdapter.ViewHolder>{
    private List<Inference> inferences;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Inference inference);
    }

    public InferenceAdapter(List<Inference> inferences, OnItemClickListener listener) {
        this.inferences = inferences;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Inference inf = inferences.get(position);
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
        public void bind(Inference inf, OnItemClickListener listener) {
            text1.setText(inf.getTimestamp());
            String desc = inf.getType() + " | click here for more informations";
            text2.setText(desc);
            itemView.setOnClickListener(v -> listener.onItemClick(inf));
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void notifyChanges(List<Inference> newInferences) {
        this.inferences.clear();
        if(newInferences != null && !newInferences.isEmpty()){
            this.inferences.addAll(newInferences);
        }
        this.notifyDataSetChanged();
    }
}
