package com.example.winet;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ReadFile extends Service {

    public static FileObserver observer;
    String fileLast = null;
    public List listOfTheFiles = new ArrayList();
    BufferedInputStream bis = null;
    public boolean valid = false;
    public DataOutputStream os_send = null;
    public Context context = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void startWatching(String directory) {

        final String pathToWatch = directory;
        observer = new FileObserver(pathToWatch, FileObserver.ALL_EVENTS) {
            @Override
            public void onEvent(int event, final String file) {
                if (event == FileObserver.CREATE && file != "cached_content_index.exi") {
                    Log.d("MediaListenerService", "File created ["+ file + "]");
                    setItemInList(file);

                     new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });
                }
            }
        };
        observer.startWatching();

    }

    public void setItemInList(String fileName) {
        this.listOfTheFiles.add(fileName);
        Log.d("ListOfilesSet", String.valueOf(this.listOfTheFiles));
    }

    public List getListOfTheFiles(){
        return this.listOfTheFiles;
    }

    public String getLastFile() {
        fileLast = String.valueOf(listOfTheFiles.get(listOfTheFiles.size() - 1));
        if(fileLast.equals("cached_content_index.exi")){
            Log.d("ReadFileEq", String.valueOf(listOfTheFiles.get(listOfTheFiles.size() - 1)));
            return String.valueOf(listOfTheFiles.get(listOfTheFiles.size() - 2));

        } else {
            return String.valueOf(listOfTheFiles.get(listOfTheFiles.size() - 1));
        }

    }


    public void getList(){
        Log.d("ListIterator", "Get list");
        Log.d("ListIterator", String.valueOf(listOfTheFiles.size()));



    }


}