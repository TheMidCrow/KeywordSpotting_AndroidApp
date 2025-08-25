package com.example.keywordspotting;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.task.audio.classifier.Classifications;

import com.jlibrosa.audio.JLibrosa;
import com.jlibrosa.audio.wavFile.WavFileException;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    private Button recordButton;
    private boolean isRecording = false;
    private Button uploadButton;
    private final static int MICROPHONE_PERMISSION_CODE = 200;
    private AudioRecord audioRecord;
    private AudioClassifier audioClassifier;
    private TensorAudio tensorAudio;
    private final List<String> requiredLabels = Arrays.asList("_silence_", "_unknown_", "yes", "no", "up", "down", "left", "right", "on", "off", "stop", "go");
    private Thread recordingThread;
    private boolean isCooldownActive = false;
    private Handler handler;
    private ActivityResultLauncher<String> audioPickerLauncher;
    private boolean isInferencing = false;
    private List<Result> sessionResults;
    private EncryptedInferencesDB database;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
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
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState){
        // Utile ad utilizzare la formattazione dati internazionale
        Locale.setDefault(Locale.US);

        recordButton = v.findViewById(R.id.recordButton);
        uploadButton = v.findViewById(R.id.uploadButton);

        if(recordButton != null){
            recordButton.setOnClickListener(this::handleRecordButtonClick);
        }

        if(uploadButton != null){
            uploadButton.setOnClickListener(this::handleUploadButtonClick);
        }

        if(isMicrophoneAvailable()){
            getMicrophonePermission();
        }

        audioPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if(uri != null){
                        handleAudioFile(uri);
                    } else {
                        Toast.makeText(requireContext(), "No file selected.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        database = EncryptedInferencesDB.getInstance(this.requireContext());
    }

    // Imposta e fa partire la registrazione audio
    private void record(View V){
        if(!isRecording){
            try{

                setupAudioRecognition();

                if(audioRecord == null){
                    Log.e("record", "AudioRecord is not initialized.");
                    return;
                }

                audioRecord.startRecording();
                isRecording = true;

                recordButton.setText("Stop");
                recordButton.setOnClickListener(this::stopRecording);

                handler = new Handler();
                recordingThread = new Thread(this::startInference);
                recordingThread.start();

                Toast.makeText(this.requireContext(), "Recording started", Toast.LENGTH_LONG).show();
            }catch(SecurityException e) {
                Log.e("record", "Error: " + e.getMessage(), e);
                if(isRecording){
                    isRecording = false;
                }
                recordButton.setText("Record");
                recordButton.setOnClickListener(this::record);
            }
        }
    }

    // Ferma la registrazione audio
    private void stopRecording(View v){
        if(isRecording){
            try{
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;

                recordButton.setText("Record");
                recordButton.setOnClickListener(this::handleRecordButtonClick);

                isRecording = false;

                Toast.makeText(this.requireContext(), "Recording stopped", Toast.LENGTH_LONG).show();
            } catch(Exception e) {
                Log.e("stopRecording","Error: " + e.getMessage(), e);
            }
        }
    }

    // Richiama l'apertura del file manager
    private void upload(View V){
        this.openFileManager();
    }

    // Verifica se i permessi per l'utilizzo del microfono sono stati acquisiti
    private boolean isMicrophoneAvailable(){
        return requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }

    // Richiede i permessi per l'utilizzo del microfono
    private void getMicrophonePermission(){
        if(ContextCompat.checkSelfPermission(this.requireContext(), android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this.requireActivity(), new String[]{android.Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_CODE);
        }
    }

    // Inizializza l'audioClassifier, il tensorAudio e l'audioRecord
    public void setupAudioRecognition() {
        try {

            audioClassifier = AudioClassifier.createFromFile(this.requireContext(), "speech_commands.tflite");
            tensorAudio = audioClassifier.createInputTensorAudio();

            AudioFormat audioFormat = getAudioFormat();
            audioRecord = getAudioRecord(audioFormat);

        } catch (Exception e) {
            Log.e("setupAudioRecognition", e.getMessage(), e);
        }
    }

    // Ottiene le informazioni di formato dell'audio
    public AudioFormat getAudioFormat() throws IllegalArgumentException{
        return new AudioFormat.Builder()
                .setSampleRate(tensorAudio.getFormat().getSampleRate())
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build();
    }

    // Ottiene l'audioRecord con le informazioni di formato
    public AudioRecord getAudioRecord(AudioFormat audioFormat) throws SecurityException{

        int bufferSize = AudioRecord.getMinBufferSize(
                tensorAudio.getFormat().getSampleRate(),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        return new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .build();
    }

    // Inizia l'inferenza tramite acquisizione audio da microfono
    public void startInference(){
        while(isRecording){
            try{
                tensorAudio.load(audioRecord);
                List<Classifications> results = audioClassifier.classify(tensorAudio);

                for(Classifications classification : results) {
                    for (Category category : classification.getCategories()) {
                        float score = category.getScore();
                        if(score > 0.999f && requiredLabels.contains(category.getLabel())) {
                            if(!isCooldownActive){
                                String keyword = category.getLabel();
                                String message = "Attention! Keyword detected: " + keyword + " (Confidence: " + score + ")";
                                Log.d("TFLite", message);

                                Result result = new Result(keyword, score);

                                sessionResults.add(result);

                                isCooldownActive = true;

                                handler.postDelayed(() -> isCooldownActive = false, 1000);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                Log.e("startInference", "Error: " + e.getMessage(), e);
            }
        }

        String finalResult = formatResultsString();

        this.showResultFragment(finalResult);

        this.saveSessionResultsToDB("Record", finalResult);

        Log.d("TFLite", "Inference terminated.");
    }

    // Gestisce il click del bottone "Record"
    private void handleRecordButtonClick(View v){
        if(isInferencing){
            Toast.makeText(this.requireContext(), "Wait for the inference to end...", Toast.LENGTH_SHORT).show();
        } else {
            sessionResults = new ArrayList<>();
            this.record(v);
        }
    }

    // Gestisce il click del bottone "Upload"
    private void handleUploadButtonClick(View v){
        if(isInferencing){
            Toast.makeText(this.requireContext(), "Wait for the inference to end...", Toast.LENGTH_SHORT).show();
        } else if(isRecording) {
            Toast.makeText(this.requireContext(), "Wait for the audio recording to end...", Toast.LENGTH_SHORT).show();
        } else {
            sessionResults = new ArrayList<>();
            this.upload(v);
        }
    }

    // Apre il file manager per selezionare un file audio
    public void openFileManager(){
        audioPickerLauncher.launch("audio/*");
    }

    // Ottenuta un'uri dall'audioPicker, inizializza l'inferenza
    public void handleAudioFile(@NonNull Uri audioUri){
        isInferencing = true;

        Toast.makeText(requireContext(), "Analyzing file...", Toast.LENGTH_LONG).show();

        recordingThread = new Thread(() -> startInferenceFromFile(audioUri));
        recordingThread.start();
    }

    // Inizia l'inferenza caricando il file audio tramite JLibrosa, lo preprocessa per poi passarlo alla funzione di inferenza
    private void startInferenceFromFile(@NonNull Uri audioUri) {
        try {
            InputStream inputStream = this.requireContext().getContentResolver().openInputStream(audioUri);
            if (inputStream == null) {
                throw new FileNotFoundException("File not found: " + audioUri);
            }

            // Otteniamo il path reale del file audio
            ContentResolver contentResolver = this.requireContext().getContentResolver();
            String path = getRealPath(contentResolver, audioUri);

            Log.d("startInferenceFromFile", "Processing file: " + path);

            // Carica il file audio
            JLibrosa jLibrosa = new JLibrosa();
            float[] audioData = jLibrosa.loadAndRead(path, 16000, 1);

            // Preprocessing
            audioData = preprocessing(audioData);

            inputStream.close();

            Log.d("audioData", "Final audioData length: " + audioData.length);
            Log.d("audioData", "Final range: min=" + getMin(audioData) + ", max=" + getMax(audioData));
            Log.d("audioData", "Final RMS: " + calculateRMS(audioData));

            if (audioData.length == 0) {
                Log.e("startInferenceFromFile", "Audio data is empty");
                showToastToUiThread("Impossible to read audio data");
                return;
            }

            // Processa l'audio con il modello tflite
            processAudioData(audioData);

        }catch(WavFileException e){
            Log.e("startInferenceFromFile", "Error: " + e.getMessage(), e);
            showToastToUiThread("Error: WAV file is not valid");
        } catch (IOException e) {
            Log.e("startInferenceFromFile", "Error: " + e.getMessage(), e);
            showToastToUiThread("Error while loading the audio file");
        } catch (Exception e) {
            Log.e("startInferenceFromFile", "Error: " + e.getMessage(), e);
            showToastToUiThread("Errore during the inference process");
        } finally {
            Log.d("startInferenceFromFile", "Inference terminated.");
            isInferencing = false;
            this.freeCache();
        }
    }

    // Permette di ottenere il path reale di una Uri
    private String getRealPath(ContentResolver contentResolver, Uri uri) throws IOException{
        InputStream audioStream = contentResolver.openInputStream(uri);
        File temp = File.createTempFile("temp", ".wav", this.requireContext().getCacheDir());
        FileOutputStream fileOutputStream = new FileOutputStream(temp);


        byte[] buffer = new byte[1024];
        int length;
        while ((length = audioStream.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, length);
        }

        fileOutputStream.close();
        audioStream.close();

        return temp.getAbsolutePath();
    }

    // Calcola il valore minimo in un array di float
    private float getMin(float[] array) {
        float min = Float.MAX_VALUE;
        for (float value : array) {
            if (value < min) min = value;
        }
        return min;
    }

    // Calcola il valore massimo in un array di float
    private float getMax(float[] array) {
        float max = Float.MIN_VALUE;
        for (float value : array) {
            if (value > max) max = value;
        }
        return max;
    }

    // Calcola l'RMS (Root Mean Square) dell'array di float del segnale audio
    private float calculateRMS(float[] audio) {
        double sum = 0.0;
        for (float sample : audio) {
            sum += sample * sample;
        }
        return (float) Math.sqrt(sum / audio.length);
    }

    // Effettua il preprocessing dell'audio
    private float[] preprocessing(float[] audioData) throws WavFileException{
        Log.d("preprocessing", "Original audio - length: " + audioData.length);

        // Assicuriamoci che l'audio abbia esattamente 16000 campioni
        if (audioData.length != 16000) {
            throw new WavFileException("Wav file has invalid format");
        }

        // Rimuoviamo il DC offset (componente continua)
        audioData = removeDCOffset(audioData);

        // Applicachiamo il pre-emphasis per migliorare il riconoscimento vocale
        audioData = applyPreEmphasis(audioData, 0.97f);

        // Normalizziamo l'audio nel modo più appropriato per il modello
        audioData = normalizeAudio(audioData);

        // Applichiamo windowing per ridurre artefatti ai bordi
        audioData = applyWindow(audioData);

        Log.d("preprocessing", "Preprocessed audio - RMS: " + calculateRMS(audioData));

        return audioData;
    }

    // Permette di ottenere il DC Offset
    public double getDCOffset(float[] audio) {
        if(audio.length == 0) {
            return 0.0;
        }

        double mean = 0.0;
        for (float sample : audio) {
            mean += sample;
        }
        mean /= audio.length;

        return mean;
    }

    // Rimuove il DC Offset dal segnale audio
    private float[] removeDCOffset(float[] audio) {
        // Ottieni la media (DC offset)
        double mean = getDCOffset(audio);

        // Rimuovi la media da tutti i campioni
        float[] result = new float[audio.length];
        for (int i = 0; i < audio.length; i++) {
            result[i] = (float) (audio[i] - mean);
        }

        Log.d("preprocessing", "DC offset removed: " + mean);
        return result;
    }

    // Aggiungiamo il pre-emphasis filter per migliorare il rilevamento vocale
    private float[] applyPreEmphasis(float[] audio, float alpha) {
        if (audio.length < 2) {
            return audio;
        }

        float[] filtered = new float[audio.length];
        filtered[0] = audio[0];

        for (int i = 1; i < audio.length; i++) {
            filtered[i] = audio[i] - alpha * audio[i - 1];
        }

        return filtered;
    }

    // Normalizza il segnale audio
    private float[] normalizeAudio(float[] audio) {
        // Per modelli di speech recognition, spesso è meglio normalizzare per il picco
        // piuttosto che per RMS

        float maxAbs = 0.0f;
        for (float sample : audio) {
            maxAbs = Math.max(maxAbs, Math.abs(sample));
        }

        if (maxAbs > 1e-6) { // Evita divisione per zero
            float[] normalized = new float[audio.length];
            // Normalizza al 70% del range massimo per evitare clipping
            float normalizationFactor = 0.7f / maxAbs;

            for (int i = 0; i < audio.length; i++) {
                normalized[i] = audio[i] * normalizationFactor;
            }

            Log.d("preprocessing", "Peak normalization - Max: " + maxAbs + " | Factor: " + normalizationFactor);
            return normalized;
        }

        return audio;
    }

    // Applica una finestra di Hanning per ridurre artefatti ai bordi
    private float[] applyWindow(float[] audio) {
        float[] windowed = new float[audio.length];
        int N = audio.length;

        for (int i = 0; i < N; i++) {
            double window = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / (N - 1)));
            windowed[i] = (float) (audio[i] * window);
        }

        return windowed;
    }

    // Processa i dati audio attraverso il modello tflite
    private void processAudioData(float[] audioData) throws Exception {
        Log.d("processAudioDataWithConvModel", "Start inference...");

        // Carica il modello per ottenerne le informazioni
        Interpreter interpreter = new Interpreter(loadModelFile(this.requireContext(), "conv_actions_frozen.tflite"));

        // Log delle informazioni del modello
        Tensor inputTensor0 = interpreter.getInputTensor(0);
        Tensor inputTensor1 = interpreter.getInputTensor(1);
        Tensor outputTensor = interpreter.getOutputTensor(0);

        Log.d("TFLite", "Input tensor 0: " + Arrays.toString(inputTensor0.shape()) + " - " + inputTensor0.dataType());
        Log.d("TFLite", "Input tensor 1: " + Arrays.toString(inputTensor1.shape()) + " - " + inputTensor1.dataType());
        Log.d("TFLite", "Output tensor: " + Arrays.toString(outputTensor.shape()) + " - " + outputTensor.dataType());

        interpreter.close();

        // Assicuriamoci nuovamente che l'audio abbia esattamente 16000 campioni
        if (audioData.length != 16000) {
            throw new WavFileException("Wav file has invalid format");
        }

        Log.d("TFLite", "Preprocessed audio - length: " + audioData.length);
        Log.d("TFLite", "Audio range: min=" + getMin(audioData) + ", max=" + getMax(audioData));

        //Prepariamo il primo input con la forma corretta [16000, 1]
        float[][] audioMatrix = new float[16000][1];
        for (int i = 0; i < 16000; i++) {
            audioMatrix[i][0] = audioData[i];
        }

        // Prepariamo il secondo input (solitamente [1] per il modello che stiamo usando)
        int[] possibleParams = {16000, 1, 0}; // sample rate, flag, o valore predefinito

        // Estraiamo i risultati dal parametro migliore (secondo input)
        float[] scores = getBestScores(audioMatrix, possibleParams);

        Log.d("TFLite", "Final scores array length: " + scores.length);
        Log.d("TFLite", "Final scores: " + Arrays.toString(scores));

        // Troviamo il punteggio massimo e l'indice corrispondente
        float maxScore = getMaxValue(scores);
        int maxIndex = getMaxValueIndex(scores, maxScore);
        String label = requiredLabels.get(maxIndex);

        Log.d("TFLite", "The keyword that was most likely said: " + label + " | Confidence: " + maxScore);

        // Aggiungiamo la keyword col punteggio massimo se rispetta i seguenti requisiti
        if(!label.contains("_silence_") && maxScore > 0.3f){
            Result result = new Result(label, maxScore);
            sessionResults.add(result);
        }

        Log.d("TFLite", "Other probable keywords detected:\n");

        // Analizziamo tutti gli altri risultati
        for (int i = 0; i < scores.length && i < requiredLabels.size(); i++) {
            float score = scores[i];
            String otherLabel = requiredLabels.get(i);

            boolean isDetected = false;

            if (!otherLabel.equals("_silence_") && !otherLabel.equals("_unknown_") && score > 0.4f) {
                isDetected = true;
            } else if ((otherLabel.equals("_silence_") || otherLabel.equals("_unknown_")) && score > 0.6f) {
                isDetected = true;
            }

            if (isDetected && !otherLabel.contains(label)) {
                Log.d("TFLite", "Keyword: " + otherLabel + " | Confidence: " + score);
            } else if (!isDetected && i == scores.length - 1){
                Log.d("TFLite", "No other keywords detected");
            }
        }

        // Mostra i risultati finali all'utente
        String finalResult = formatResultsString();

        this.showResultFragment(finalResult);

        this.showToastToUiThread("Analysis terminated.");

        this.saveSessionResultsToDB("Upload", finalResult);
    }

    // Carica il modello tflite
    private MappedByteBuffer loadModelFile(Context context, String modelFileName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Recupera i migliori risultati tra tutti i parametri possibili
    public float[] getBestScores(float[][] audioMatrix, int[] possibleParams) throws Exception{
        if(audioMatrix == null || possibleParams == null){
            return null;
        }

        float[] bestScores = null;

        // Per ogni parametro possibile, eseguiamo l'inferenza, in modo da trovare il parametro adatto al modello
        for (int param : possibleParams) {
            try {
                Log.d("TFLite", "Testing with parameter: " + param);

                // Creiamo un nuovo interpreter per ogni tentativo
                Interpreter tempInterpreter = new Interpreter(loadModelFile(this.requireContext(), "conv_actions_frozen.tflite"));

                // Input arrays
                Object[] inputs = new Object[2];
                inputs[0] = audioMatrix; // [16000, 1] float32
                inputs[1] = new int[]{param}; // [1] int32

                // Output map
                Map<Integer, Object> outputs = new HashMap<>();
                float[][] outputArray = new float[1][12]; // [1, 12] float32
                outputs.put(0, outputArray);

                // Eseguiamo l'inferenza
                tempInterpreter.runForMultipleInputsOutputs(inputs, outputs);
                tempInterpreter.close();

                // Estraiamo i risultati
                float[] currentScores = outputArray[0];

                // Calcoliamo la varianza per determinare quale set di parametri funziona meglio
                float variance = calculateVariance(currentScores);
                float maxScore = getMaxValue(currentScores);

                Log.d("TFLite", "Parameter " + param + " - Max score: " + maxScore + ", Variance: " + variance);
                Log.d("TFLite", "Scores with param " + param + ": " + Arrays.toString(currentScores));

                // Prendiamo il set di risultati con la varianza più alta o con il punteggio massimo
                if (bestScores == null || variance > calculateVariance(bestScores) || maxScore > getMaxValue(bestScores)) {
                    bestScores = currentScores.clone();
                    Log.d("TFLite", "Using results from parameter: " + param);
                }

            } catch (Exception e) {
                Log.e("TFLite", "Failed with parameter " + param + ": " + e.getMessage());
            }
        }

        if (bestScores == null) {
            throw new RuntimeException("All possible parameters failed");
        }

        return bestScores;
    }

    // Calcola la varianza
    private float calculateVariance(float[] scores) {
        if (scores == null || scores.length == 0) {
            return 0.0f;
        }

        // Ottieni la media
        double mean = getDCOffset(scores);

        // Calcola la varianza
        double variance = 0.0;
        for (float score : scores) {
            variance += Math.pow(score - mean, 2);
        }
        variance /= scores.length;

        return (float) variance;
    }

    // Ottiene il punteggio massimo
    private float getMaxValue(float[] scores) {
        if (scores == null || scores.length == 0) {
            return 0;
        }

        float max = scores[0];
        for (float score : scores) {
            if (score > max) max = score;
        }
        return max;
    }

    // Ottiene l'indice del punteggio massimo
    public int getMaxValueIndex(float[] scores, float maxScore){
        if (scores == null || scores.length == 0) {
            return -1;
        }

        int index = 0;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] == maxScore) {
                index = i;
            }
        }
        return index;
    }

    // Mostra il messaggio d'errore all'utente
    private void showToastToUiThread(String message) {
        if (getActivity() != null) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                isInferencing = false;
            });
        }
    }

    // Salva i risultati della sessione di inferenza nel database
    private void saveSessionResultsToDB(String type, String results){
        String timestamp = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
        InferenceEntity inference = new InferenceEntity(type, results, timestamp);
        database.addOrUpdateInference(inference);
    }

    // Formatta i risultati ottenuti dalla sessione di inferenza in un unica stringa
    private String formatResultsString(){
        if(sessionResults == null || sessionResults.isEmpty()){
            return "No keywords have been detected";
        }

        StringBuilder sb = new StringBuilder("Keywords detected:\n");
        for(Result result : sessionResults){
            sb.append("Keyword: ").append(result.getKeyword())
                    .append(" | Confidence: ").append(result.getConfidence())
                    .append("\n");
        }
        return sb.toString();
    }

    // Mostra il DialogFragment all'utente contenente i risultati ottenuti dall'inferenza
    public void showResultFragment(String finalResult){
        this.requireActivity().runOnUiThread(() -> {
            ResultsFragment resultsFragment = ResultsFragment.newInstance(finalResult);
            Log.d("TFLite", finalResult);
            resultsFragment.show(this.getParentFragmentManager(), "ResultsDialog");
        });
    }

    // Libera la cache dai file spazzatura
    private void freeCache(){
        File cacheDir = new File(this.requireContext().getCacheDir().getAbsolutePath());
        File[] listFiles = cacheDir.listFiles();

        if(listFiles != null && listFiles.length > 0){
            for (File file : listFiles) {
                if(file.exists() && file.isFile()){
                    file.delete();
                }
            }
            Log.d("processAudioDataWithConvModel", "Cache successfully freed");
        } else {
            Log.e("processAudioDataWithConvModel", "Cache could not be freed");
        }
    }

}