package com.example.keywordspotting;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChronologyFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChronologyFragment extends Fragment {

    private static final String TAG = "ChronologyFragment";
    private Button clearChronology;
    private RecyclerView recyclerView;
    private InferenceAdapter adapter;
    private List<InferenceEntity> inferences;
    private EncryptedInferencesDB database;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ChronologyFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChronologyFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChronologyFragment newInstance(String param1, String param2) {
        ChronologyFragment fragment = new ChronologyFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_chronology, container, false);

        recyclerView = v.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.requireContext()));

        return v;
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState){
        clearChronology = v.findViewById(R.id.clearChronology);
        if(clearChronology != null){
            clearChronology.setOnClickListener(this::deleteAllInferences);
        }

        // Inizializziamo il database criptato
        database = EncryptedInferencesDB.getInstance(this.requireContext());

        this.loadInferences();
    }

    public void deleteAllInferences(View v){
        boolean success = database.deleteAllInferences();
        if (success) {
            Log.d(TAG, "All inferences deleted successfully");
        } else {
            Log.e(TAG, "Failed to delete all inferences");
        }
        this.loadInferences();
    }

    public void loadInferences(){
        inferences = database.getAllInferences();

        if(adapter != null){
            adapter.notifyChanges(inferences);
        } else {
            adapter = new InferenceAdapter(inferences, inference -> {
                Bundle args = new Bundle();
                args.putString("Results", inference.getResults());

                ResultsFragment results = ResultsFragment.newInstance(inference.getResults());
                results.show(this.getParentFragmentManager(), TAG);
            });
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Opzionale
        if (database != null) {
            database.close();
        }
    }
}