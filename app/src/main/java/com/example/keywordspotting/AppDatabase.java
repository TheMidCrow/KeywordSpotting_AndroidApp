package com.example.keywordspotting;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

@Database(entities = {InferenceEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract InferenceDao inferenceDao();

    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "keyword_spotting_encrypted_db";
    private static final String DATABASE_PASSPHRASE = "KeywordSpottingSecurePass2024!";

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // Carichiamo la libreria SQLCipher
                    SQLiteDatabase.loadLibs(context);

                    // Creiamo il support factory con la passphrase
                    // Usiamo il costruttore che NON cancella automaticamente la passphrase
                    final byte[] passphrase = SQLiteDatabase.getBytes(DATABASE_PASSPHRASE.toCharArray());
                    final SupportFactory factory = new SupportFactory(passphrase, null, false);

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .openHelperFactory(factory)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}