package com.example.winet;

import android.Manifest;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.MainThread;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FileTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.winet.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
    public static final String EXTRAS_FILE_LISTENING_PORT = "file_listening_port"; //Porta que o cliente ficará esperando os arquivos
    public static final String EXTRAS_FILE_ID = "file_id";
    public static final String ACTION_DOWNLOAD_FILE = "com.example.winet.DOWNLOAD_FILE";
    public static final String ACTION_SEND_ACK = "com.example.winet.ACK";
    public static boolean requestPending = false;

    public FileTransferService(String name) {
        super(name);
    }
    public FileTransferService() {
        super("FileTransferService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(ACTION_DOWNLOAD_FILE)) {
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            String fileID = intent.getExtras().getString(EXTRAS_FILE_ID);
            try {
                Log.d("FileTransferService", "Opening client socket - ");

                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d("FileTransferService", "Client socket - " + socket.isConnected());

                String request = "dow" + fileID;
                OutputStream outputStream = socket.getOutputStream();
                InputStream inputStream = new ByteArrayInputStream(request.getBytes(Charset.forName("UTF-8")));
                Util.copyFile(inputStream, outputStream, false);

                Log.d("FileTransferService", "Client: File requested");
            } catch (IOException e) {
                Log.e("FileTransferService", e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else if (intent.getAction().equals(ACTION_SEND_ACK)) {
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            String fileListeningPort = intent.getExtras().getString(EXTRAS_FILE_LISTENING_PORT);
            try {
                Log.d("FileTransferService", "Opening client socket - ");

                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d("FileTransferService", "Client socket - " + socket.isConnected());

                OutputStream outputStream = socket.getOutputStream();
                String ack = "ack" + fileListeningPort;
                InputStream inputStream = new ByteArrayInputStream(ack.getBytes(Charset.forName("UTF-8")));
                Util.copyFile(inputStream, outputStream, true);

                Log.d("FileTransferService", "ACK enviado para o radio: " + ack);
            } catch (IOException e) {
                Log.e("FileTransferService", e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}

