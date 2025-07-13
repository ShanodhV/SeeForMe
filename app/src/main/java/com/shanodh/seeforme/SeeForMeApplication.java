package com.shanodh.seeforme;

import android.app.Application;

import com.shanodh.seeforme.data.AppDatabase;

public class SeeForMeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize the database
        AppDatabase.getInstance(this);
    }
} 