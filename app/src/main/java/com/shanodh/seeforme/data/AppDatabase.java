package com.shanodh.seeforme.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;

/**
 * Room database for the SeeForMe app
 */
@Database(entities = {Note.class, FamiliarFace.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "seeforme_db";
    private static AppDatabase instance;

    public abstract NoteDao noteDao();
    public abstract FamiliarFaceDao familiarFaceDao();

    // Migration from version 1 to 2: Add firestoreId to notes and firebaseId to familiar_faces
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add firestoreId column to notes table
            database.execSQL("ALTER TABLE notes ADD COLUMN firestoreId TEXT");
            // Add firebaseId column to familiar_faces table
            database.execSQL("ALTER TABLE familiar_faces ADD COLUMN firebaseId TEXT");
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DATABASE_NAME
            )
            .addMigrations(MIGRATION_1_2)
            .build();
        }
        return instance;
    }

    /**
     * Get the size of the database in bytes
     */
    public static long getDatabaseSize(Context context) {
        File dbFile = context.getDatabasePath(DATABASE_NAME);
        return dbFile.exists() ? dbFile.length() : 0;
    }

    /**
     * Get the number of records in each table
     */
    public static String getDatabaseStats(Context context) {
        SQLiteDatabase db = context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
        StringBuilder stats = new StringBuilder();
        
        // Get notes count
        Cursor notesCursor = db.rawQuery("SELECT COUNT(*) FROM notes", null);
        if (notesCursor.moveToFirst()) {
            stats.append("Notes: ").append(notesCursor.getInt(0)).append("\n");
        }
        notesCursor.close();
        
        // Get faces count
        Cursor facesCursor = db.rawQuery("SELECT COUNT(*) FROM familiar_faces", null);
        if (facesCursor.moveToFirst()) {
            stats.append("Familiar Faces: ").append(facesCursor.getInt(0));
        }
        facesCursor.close();
        
        db.close();
        return stats.toString();
    }

    /**
     * Vacuum the database to reclaim space
     */
    public static void optimizeStorage(Context context) {
        SQLiteDatabase db = context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
        db.execSQL("VACUUM");
        db.close();
    }
} 