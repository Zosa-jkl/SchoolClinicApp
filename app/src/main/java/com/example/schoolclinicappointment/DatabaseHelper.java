package com.example.schoolclinicappointment;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database name and version
    private static final String DATABASE_NAME = "appointments_db";
    private static final int DATABASE_VERSION = 1;

    // SQL query to create the appointments table
    private static final String CREATE_APPOINTMENTS_TABLE =
            "CREATE TABLE appointments (" +
                    "appointment_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "appointment_date TEXT NOT NULL, " +
                    "appointment_time TEXT NOT NULL, " +
                    "consultation_type TEXT NOT NULL, " +
                    "status TEXT DEFAULT 'Available');";

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Called when the database is first created
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Execute the SQL query to create the appointments table
        db.execSQL(CREATE_APPOINTMENTS_TABLE);
    }

    // Called when the database version changes (e.g., upgrading)
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // If the database version changes, drop the old table and create a new one
        db.execSQL("DROP TABLE IF EXISTS appointments");
        onCreate(db);
    }
    public long insertAppointment(String date, String time, String type) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Create a new ContentValues object to hold the column-value pairs
        ContentValues values = new ContentValues();
        values.put("appointment_date", date);
        values.put("appointment_time", time);
        values.put("consultation_type", type);

        // Insert the row into the database and return the row ID
        return db.insert("appointments", null, values);
    }
    public ArrayList<String> getAllAppointments() {
        ArrayList<String> appointments = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query the appointments table to get all data
        Cursor cursor = db.query("appointments", null, null, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String date = cursor.getString(cursor.getColumnIndex("appointment_date"));
                String time = cursor.getString(cursor.getColumnIndex("appointment_time"));
                String type = cursor.getString(cursor.getColumnIndex("consultation_type"));
                // Combine the date, time, and type to display in the ListView
                appointments.add(date + " | " + time + " | " + type);
            }
            cursor.close();
        }
        return appointments;
    }
}
