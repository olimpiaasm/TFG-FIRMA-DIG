package com.example.signapp;

import android.app.Notification;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "UserDatabase";
    private static final int DATABASE_VERSION = 2;
    public static final String TABLE_NAME = "users";
    public static final String TABLE_NAME_NOTIFICATIONS = "notifications";
    private static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_NOTIFICATION_TITLE = "title"; // Nueva columna
    public static final String COLUMN_NOTIFICATION_MESSAGE = "message"; // Nueva columna

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUserTable = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME + " TEXT, "
                + COLUMN_EMAIL + " TEXT, "
                + COLUMN_PASSWORD + " TEXT);";

        String createNotificationTable = "CREATE TABLE " + TABLE_NAME_NOTIFICATIONS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NOTIFICATION_TITLE + " TEXT, "
                + COLUMN_NOTIFICATION_MESSAGE + " TEXT);";

        db.execSQL(createUserTable);
        db.execSQL(createNotificationTable);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean addUser(User user) {
        if (userExists(user.getEmail())) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, user.getName());
        values.put(COLUMN_EMAIL, user.getEmail());
        values.put(COLUMN_PASSWORD, user.getPassword());

        long result = db.insert(TABLE_NAME, null, values);
        db.close();

        return result != -1;
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_ID};
        String selection = COLUMN_EMAIL + " = ?" + " AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {email, password};
        Cursor cursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null);

        int count = cursor.getCount();
        cursor.close();
        db.close();

        return count > 0;
    }

    public boolean userExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_ID};
        String selection = COLUMN_EMAIL + " = ?";
        String[] selectionArgs = {email};
        Cursor cursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null);

        int count = cursor.getCount();
        cursor.close();
        db.close();

        return count > 0;
    }

    public boolean updateUser(String email, String newName, String newEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, newName);
        values.put(COLUMN_EMAIL, newEmail);

        int rowsAffected = db.update(TABLE_NAME, values, COLUMN_EMAIL + " = ?", new String[]{email});
        db.close();
        return rowsAffected > 0;
    }

    public boolean updateUserPassword(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PASSWORD, newPassword);

        int rowsAffected = db.update(TABLE_NAME, values, COLUMN_EMAIL + " = ?", new String[]{email});
        db.close();
        return rowsAffected > 0;
    }

    public User getUser(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, COLUMN_EMAIL + " = ?", new String[]{email}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(COLUMN_NAME);
            int emailIndex = cursor.getColumnIndex(COLUMN_EMAIL);
            int passwordIndex = cursor.getColumnIndex(COLUMN_PASSWORD);

            if (nameIndex != -1 && emailIndex != -1 && passwordIndex != -1) {
                String name = cursor.getString(nameIndex);
                String userEmail = cursor.getString(emailIndex);
                String password = cursor.getString(passwordIndex);
                cursor.close();
                return new User(name, userEmail, password);
            }
        }
        return null;
    }

    public boolean addNotification(String title, String message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTIFICATION_TITLE, title);
        values.put(COLUMN_NOTIFICATION_MESSAGE, message);

        long result = db.insert(TABLE_NAME_NOTIFICATIONS, null, values);
        db.close();
        return result != -1;
    }


}
