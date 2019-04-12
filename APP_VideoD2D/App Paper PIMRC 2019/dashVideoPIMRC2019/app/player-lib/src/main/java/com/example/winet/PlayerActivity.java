/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.winet;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.offline.ProgressiveDownloadAction;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class PlayerActivity extends AppCompatActivity {

    // Player configuration
    private static final String TAG = "PlayerActivity";
    private SimpleExoPlayer player;
    private PlayerView playerView;
    private ComponentListener componentListener;
    private long playbackPosition;
    private int currentWindow;
    private boolean playWhenReady = true;
    public Thread threadDownload = null;

    // Create a default LoadControl
    public LoadControl loadControl;

    // Client Sockets
    public ClientSocket4G clientSocket4G = new ClientSocket4G();
    public Thread threadWifiDirect = null;
    public Thread threadReadFile = null;

    // Wi-Fi Direct
    //Button btnOnOff, btnDiscover, btnSend, btnGo;
    ListView listView;
    TextView connectionStatus;
    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    WifiP2pInfo info;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;
    public String macGO = null;
    public String portFileRecvFromGO = null;
    public boolean definedD2DRole = false;
    public boolean connectedToD2D = false;
    //WifiP2pManager.ActionListener actionListener;

    //Video Server Migration State Control (to include in the log)
    String migrationStatus = "NOT_MIGRATED";

    //Calculate the total time when the video was frozen (STATE_BUFFERING) during playback
    int playerLastState = Player.STATE_IDLE;
    long timestampBuffering;
    long totalVideoFreezeTimeSeconds = 0;

    ReadFile readFile = new ReadFile();

    // Bandwidth meter in order to estimate the video bandwidth in real time
    /*
    private final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter(new Handler(), new BandwidthMeter.EventListener() {

        @Override
        public void onBandwidthSample(int elapsedMs, long bytes, long bitrate) {
            Log.d("Bandwidth - Elapsed", String.valueOf(elapsedMs)+";"+String.valueOf(bytes)+";"+String.valueOf(bitrate)+";");
            String logBandwidth = String.valueOf(elapsedMs + ";" + bytes + ";" + bitrate + ";");
            writeToLog(";Bandwidth;" , logBandwidth);


        }
    });


   */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        componentListener = new ComponentListener();
        playerView = findViewById(R.id.video_view);
        /*comment
        // Thread to create manager to Wi-Fi and D2D
        threadWifiDirect = new Thread(new Runnable() {
            public void run() {
                initialWork();
                exqListener();
            }

        });
        threadWifiDirect.start();

        */
    }

    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }

        //Runnable r = new CtrlMsgRecvThread4G();
        //new Thread(r).start();
    }

    public class CtrlMsgRecvThread4G implements Runnable {
        public void run() {
            clientSocket4G.startConnection();

            Log.d("PlayerActivity","Conectando ao SD para troca de informações de controle");

            clientSocket4G.sendData4G("1", getMacAddr()); //Envio do endereço MAC Wi-Fi Direct

            while(true) {
                clientSocket4G.recvData4G(); //Wait for a new message

                String[] splitResponseLine = clientSocket4G.getResponseLine().split("(,)");

                switch (splitResponseLine[0]) {
                    case "3": //Mudar para a interface D2D
                        macGO = splitResponseLine[1];
                        portFileRecvFromGO = splitResponseLine[2];

                        Log.d("ClientSocket4G", "Sou um UE que vai se conectar ao GO com endereço " + macGO + " e vou receber arquivos do mesmo na porta " + portFileRecvFromGO);

                        if (getMacAddr().equals(macGO)){
                            createGroup();
                        } else {
                            startD2DDiscovery(); //Sou cliente; Vou procurar o GO e me conectar a ele
                        }

                        break;
                    case "4": //Mudar para a interface 4G

                        Log.d("ClientSocket4G", "Mudei para a interface 4G");

                        //TODO 'comofas', Marcos?

                        //seekToPlayer();
                        break;
                    case "5": //Troca de estado da migração do servidor de vídeo

                        Log.d("ClientSocket4G", "Status de migração alterado para " + splitResponseLine[1]);

                        migrationStatus = splitResponseLine[1];
                        break;
                }
            }
        }
    }

    public void onResume() {
        super.onResume();
        hideSystemUi();
        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer();
        }
        //registerReceiver(mReceiver, mIntentFilter);
    }

    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
        //unregisterReceiver(mReceiver);
    }

    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();

        }
    }

    private void initializePlayer() {

    /*
        // 1. Create a default TrackSelector
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        // 2. Create a default LoadControl
        LoadControl loadControl = new DefaultLoadControl();

        //get the drm session

        // 3. Create the player
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);



        // Bind the player to the view.
        playerView.setPlayer(player);

  */
        // A factory to create an AdaptiveVideoTrackSelection
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        // A controller
        loadControl = new DefaultLoadControl();


        // Using a DefaultTrackSelector with an adaptive video selection factory
        player = ExoPlayerFactory.newSimpleInstance(getApplicationContext(),trackSelector);




        playerView.setPlayer(player);

        //player.setPlayWhenReady(playWhenReady);
        //player.seekTo(currentWindow, playbackPosition);


        buildMediaSource(Uri.parse(getString(R.string.media_url_dash)));
        //MediaSource mediaSource = buildMediaSource(Uri.parse(getString(R.string.media_url_dash)));
        //MediaSource mediaSource = buildMediaSource(Uri.parse("/storage/emulated/0/Android/data/com.example.exoplayercodelab/cache/downloads/1.0.1555085529237.v3.exo"));
        //threadDownload.start();

        //player.prepare(mediaSource);
        //player.prepare(concatenatingMediaSource, true, false);

        //play the asset - Josh

    }

    private void releasePlayer() {
        if (player != null) {
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();
            //player.removeListener(componentListener);
            //player.removeVideoDebugListener(componentListener);
            //player.removeAudioDebugListener(componentListener);
            //player.release();
            player = null;
        }
    }

    public void seekToPlayer() {
        playbackPosition =  600000;
        currentWindow = player.getCurrentWindowIndex();
        player.seekTo(currentWindow, playbackPosition);
    }

    private void buildMediaSource(Uri uri) {
        final Uri uriRecv = uri;
            /*
        DefaultHttpDataSourceFactory factory = new DefaultHttpDataSourceFactory("device 1", null);

        DashChunkSource.Factory dashChunkSourceFactory = new DefaultDashChunkSource.Factory(new DefaultHttpDataSourceFactory("device 1", BANDWIDTH_METER));

        final DataSource.Factory manifestDataSourceFactory = new DefaultHttpDataSourceFactory("device 1");


        threadDownload = new Thread(new Runnable() {
            @Override
            public void run() {
                File cacheDirectory = new File(getApplicationContext().getExternalCacheDir(), "downloads");
                LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024);

                SimpleCache cache = new SimpleCache(cacheDirectory, new NoOpCacheEvictor());

                //DefaultHttpDataSourceFactory factory = new DefaultHttpDataSourceFactory("ExoPlayer", null);
                DownloaderConstructorHelper constructorHelper =
                        new DownloaderConstructorHelper(cache, manifestDataSourceFactory);
                // TODO testar constructorHelper
                //constructorHelper.


                Log.d("File", String.valueOf(getApplicationContext().getExternalCacheDir()));

                DashDownloader dashDownloader = new DashDownloader(uriRecv, constructorHelper);


                try {
                    dashDownloader.download(new Downloader.ProgressListener() {
                        @Override
                        public void onDownloadProgress(Downloader downloader, float downloadPercentage, long downloadedBytes) {

                        }
                    });
                    dashDownloader.getDownloadPercentage();

                    Log.d("Download", String.valueOf(dashDownloader.getDownloadPercentage()));

                    Log.d("Download", "Download");
                }
                catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });


        return new DashMediaSource.Factory(dashChunkSourceFactory,manifestDataSourceFactory).createMediaSource(uri);
    */
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        //MANUALLY DISCONNECT INTERNET CONNECTION IN DEBUGGER HERE
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        //read up from the local system

        /* Funcionando ate agora
        File cacheDirectory = new File(getApplicationContext().getExternalCacheDir(), "downloads");
        SimpleCache cache = new SimpleCache(cacheDirectory,  new NoOpCacheEvictor());

        FileDataSourceFactory fileDataSource = new FileDataSourceFactory();
        DefaultHttpDataSourceFactory factory = new DefaultHttpDataSourceFactory("device 1", null);
        MediaSource mediaSource = new DashMediaSource(
                Uri.parse(String.valueOf(uriRecv)),
                new CacheDataSourceFactory(
                        cache, factory, CacheDataSource.FLAG_BLOCK_ON_CACHE),
                new DefaultDashChunkSource.Factory(new CacheDataSourceFactory(cache, factory, CacheDataSource.FLAG_BLOCK_ON_CACHE)),
                null,
                null
        );

        return mediaSource;
        */
/*
        FileDataSourceFactory fileDataSource = new FileDataSourceFactory();
        CacheDataSourceFactory dataSourceFactory = new CacheDataSourceFactory(cache2,fileDataSource);
        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);
        return mediaSource;
*/

        final DataSource.Factory manifestDataSourceFactory = new DefaultHttpDataSourceFactory("device 1");


// App initialization
        File cacheDirectory = new File(getApplicationContext().getExternalCacheDir(), "downloads");
        SimpleCache cache = new SimpleCache(cacheDirectory,  new NoOpCacheEvictor());

// Playback

        CacheDataSourceFactory dataSourceFactory = new CacheDataSourceFactory(cache, manifestDataSourceFactory);
        ExtractorMediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(String.valueOf(uri)));
        player.prepare(mediaSource);



    }


    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private class ComponentListener extends Player.DefaultEventListener implements
            VideoRendererEventListener, AudioRendererEventListener {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            String stateString;
            switch (playbackState) {
                case Player.STATE_IDLE:
                    stateString = "ExoPlayer.STATE_IDLE      -";
                    break;
                case Player.STATE_BUFFERING:
                    stateString = "ExoPlayer.STATE_BUFFERING -";
                    timestampBuffering = Calendar.getInstance().getTime().getTime(); //Let's count the duration of the 'video frozen' state
                    playerLastState = Player.STATE_BUFFERING;

                    Log.d("BufferState", "Vídeo travado, bufferizando");
                    break;
                case Player.STATE_READY:
                    stateString = "ExoPlayer.STATE_READY     -";

                    if (playerLastState == Player.STATE_BUFFERING) {
                        long diffSeconds = (Calendar.getInstance().getTime().getTime() - timestampBuffering)/1000;
                        writeToLog(";PartialFreezeTimeSeconds;", String.valueOf(diffSeconds));

                        totalVideoFreezeTimeSeconds += diffSeconds;
                        playerLastState = Player.STATE_READY;

                        Log.d("BufferState", "Vídeo destravado. Tempo total que o vídeo ficou parado: " + diffSeconds);
                    }

                    break;
                case Player.STATE_ENDED: //End of the video
                    stateString = "ExoPlayer.STATE_ENDED     -";
                    writeToLog(";TotalFreezeTimeSeconds;", String.valueOf(totalVideoFreezeTimeSeconds));

                    Log.d("BufferState", "Vídeo concluído. Tempo total de travamento do vídeo: " + totalVideoFreezeTimeSeconds);
                    break;
                default:
                    stateString = "UNKNOWN_STATE-";
                    break;
            }
            Log.d(TAG, "Bandwidth - changed state to " + stateString + " playWhenReady: " + playWhenReady + playbackState);
        }

        // Implementing VideoRendererEventListener.
        @Override
        public void onVideoEnabled(DecoderCounters counters) {
            // Do nothing.
        }

        @Override
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            // Do nothing.
        }

        @Override
        public void onVideoInputFormatChanged(Format format) {
            // Do nothing.
        }

        @Override
        public void onDroppedFrames(int count, long elapsedMs) {
            Log.d("Qualidade Video", "Dropped Frames: " + " " + count);
            writeToLog(";DroppedFrames;", String.valueOf(count));
        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            Log.d("Qualidade Video", "Video Horizontal Resolution Changed: " + " " + width);
            writeToLog(";VideoHorizontalRes;", String.valueOf(width));
        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {
            // Do nothing.
        }

        @Override
        public void onVideoDisabled(DecoderCounters counters) {
            // Do nothing.
        }

        // Implementing AudioRendererEventListener.
        @Override
        public void onAudioEnabled(DecoderCounters counters) {
            // Do nothing.
        }

        @Override
        public void onAudioSessionId(int audioSessionId) {
            // Do nothing.
        }

        @Override
        public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            // Do nothing.
        }

        @Override
        public void onAudioInputFormatChanged(Format format) {
            // Do nothing.
        }

        @Override
        public void onAudioSinkUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
            // Do nothing.
        }

        @Override
        public void onAudioDisabled(DecoderCounters counters) {
            // Do nothing.
        }
    }

    // Creates a file in the primary external storage space of the current application.
    // If the file does not exist, it is created.
    public void writeToLog(String type, String data) {
        try {
            Date currentTime = Calendar.getInstance().getTime();
            File testFile = new File(getApplicationContext().getExternalFilesDir(null), "Logs.txt");
            if (!testFile.exists())
                testFile.createNewFile();

            // Adds a line to the file
            BufferedWriter writer = new BufferedWriter(new FileWriter(testFile, true /*append*/));
            Log.d("currentData", String.valueOf(currentTime));
            writer.write( currentTime + ";" + migrationStatus + type + data + "\n");
            writer.close();

            // Refresh the data so it can seen when the device is plugged in a
            // computer. You may have to unplug and replug the device to see the
            // latest changes. This is not necessary if the user should not modify
            // the files.
            MediaScannerConnection.scanFile(getApplicationContext(),
                    new String[]{testFile.toString()},
                    null, null);
        } catch (IOException e) {
            Log.e("ReadWriteFile", "Unable to write to the file.");
        }
    }

    // Não clicamos em nenhum botão durante os experimentos
    private void exqListener() {
      /*
      btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(false);
                    btnOnOff.setText("ON");
                } else {
                    wifiManager.setWifiEnabled(true);
                    btnOnOff.setText("OFF");
                }
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Discovery Started");
                    }

                    @Override
                    public void onFailure(int reason) {
                        connectionStatus.setText("Discovery Starting Failed");
                    }
                });
            }
        });
        */

        /*listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
                final WifiP2pDevice device = deviceArray[i];

                final WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;

                // Connect
                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("D2D", String.valueOf(config));
                        Toast.makeText(getApplicationContext(),"Connected to" + device.deviceName,Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(getApplicationContext(),"Not Connected",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });*/

        /*
        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createGroup();
                Toast.makeText(getApplicationContext(),"GO",Toast.LENGTH_LONG).show();
            }
        });
        */
    }

    public void createGroup() {
        /* Cancela conexões */
        mManager.cancelConnect(mChannel, null);

        /* Remove grupo - desconexão */
        mManager.removeGroup(mChannel, null);

        /* Limpa os serviços locais */
        mManager.clearLocalServices(mChannel, null);

        /* Limpa as requisições de serviços */
        mManager.clearServiceRequests(mChannel, null);

        /* Cria grupo */
        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(),"GO",Toast.LENGTH_LONG).show();
                /* D2DReceiver irá tratar */
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(getApplicationContext(),"Failed to become GO",Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initialWork() {
        listView = (ListView) findViewById(R.id.peerListView);
        connectionStatus = (TextView) findViewById(R.id.connectionStatus);
        //Wifi manager object
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //Get IP
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this,getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if(!peerList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                deviceNameArray = new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index = 0;

                for(final WifiP2pDevice device : peerList.getDeviceList()) {
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;

                    Log.d("PlayerActivity", "deviceName: " + device.deviceName + " / macDevice: " + device.deviceAddress + " / macGo: " + macGO);

                    //O primeiro byte do endereço MAC lido da interface 'wlan0' difere do que é anunciado pelos devices na descoberta
                    String trunkedMacDevice = removeFirstByteFromMAC(device.deviceAddress);
                    String trunkedMacGO = removeFirstByteFromMAC(macGO);

                    Log.d("PlayerActivity", "trunkedMacDevice: " + trunkedMacDevice + " - trunkedMacGO: " + trunkedMacDevice);

                    //Client found the GO. Stop D2D discovery and connect to him
                    if(trunkedMacDevice.equals(trunkedMacGO) && !connectedToD2D) {
                        connectedToD2D = true;
                        //stopD2DDiscovery();

                        Log.d("PlayerActivity", "Trying to connect to '" + device.deviceName + "' as GO with mac " + device.deviceAddress);

                        final WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = device.deviceAddress;

                        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.d("PlayerActivity", "Connected to GO: '" + device.deviceName + "'");
                                Toast.makeText(getApplicationContext(),"Connected to GO: '" + device.deviceName + "'",Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onFailure(int reason) {
                                Toast.makeText(getApplicationContext(),"Failed to connect to GO '" + device.deviceName + "'. Reason: " + reason,Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
                listView.setAdapter(adapter);
            }
            if(peers.size() == 0) {
                Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_LONG).show();
                return;
            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(final WifiP2pInfo wifiP2pInfo) {
            if(wifiP2pInfo.groupFormed && !definedD2DRole) {
                definedD2DRole = true;

                //Get general info about the group, including the Group Owner address
                setWifiP2pInfo(wifiP2pInfo);

                if (wifiP2pInfo.isGroupOwner) {
                    connectionStatus.setText("Set up as GO");

                    Log.d("PlayerActivity", "Este UE é o GO");

                    //Inicia o servidor de arquivos
                    ServerSocketD2D.FileServerAsync fileServerAsync = new ServerSocketD2D.FileServerAsync(getApplicationContext());
                    fileServerAsync.execute();


                    Log.d("PlayerActivity", "GO iniciou servidor de arquivos");
                } else {
                    onStop(); //Pause o video
                    onPause();
                    connectionStatus.setText("Connected to GO");

                    Log.d("PlayerActivity", "Este UE é cliente");

                    ClientSocketD2D.ClientReceiverAsync clientRecvD2D = new ClientSocketD2D.ClientReceiverAsync(getApplicationContext(), portFileRecvFromGO, macGO, getMacAddr(), clientSocket4G);
                    clientRecvD2D.execute();

                    Log.d("PlayerActivity", "Cliente iniciou serviço de download");

                    sendAck(portFileRecvFromGO); //Anuncie para o GO a porta para recepção dos arquivos

                    Runnable r = new RequestFilesThread();
                    new Thread(r).start();
                }
            }
        }
    };

    public class RequestFilesThread implements Runnable {
        public void run() {
            for(int i = 1; i <= 15; i++) {
                Log.d("PlayerActivity", "Aguardando para enviar requisicao do arquivo com ID iniciando em " + i + "...");

                while(!clientSocket4G.isLastReportSent()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                /*try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/

                clientSocket4G.setLastReportSent(false);

                requestFile(String.valueOf(i).length() + String.valueOf(i));
            }
        }
    }

    public void startD2DDiscovery() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                connectionStatus.setText("Discovery Started");
            }

            @Override
            public void onFailure(int reason) {
                connectionStatus.setText("Discovery Start Process Failed");
            }
        });
    }

    public void stopD2DDiscovery() {
        mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() { }

            @Override
            public void onFailure(int reason) {
                connectionStatus.setText("Discovery Stop Process Failed");
            }
        });
    }

    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02x:",b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }

    public void sendAck(String fileListeningPort) {
        Intent serviceIntent = new Intent(PlayerActivity.this, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_ACK);
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                "192.168.49.1");
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8443);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_LISTENING_PORT, fileListeningPort.length() + fileListeningPort);
        this.startService(serviceIntent);
        Log.d("PlayerActivity", "ACK enviado como intent na porta " + fileListeningPort);
    }

    public void requestFile(String fileID) {
        Intent serviceIntent = new Intent(PlayerActivity.this, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_DOWNLOAD_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                "192.168.49.1");
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8443);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_ID, fileID);
        this.startService(serviceIntent);
        Log.d("PlayerActivity", "Requisicao do arquivo iniciando em " + fileID + " enviada como intent");
    }

    public void setWifiP2pInfo(WifiP2pInfo info) {
        this.info = info;
    }

    public String removeFirstByteFromMAC (String macAddr) {
        String[] macArray = macAddr.split(":");
        String macWihtoutFirstByte = "";
        for(int pos = 1; pos < macArray.length; pos++) {
            macWihtoutFirstByte += macArray[pos];
        }
        return macWihtoutFirstByte;
    }

    public class CheckFilesThread implements Runnable {
        public void run() {
            String myDirectory = getApplicationContext().getExternalCacheDir().getAbsolutePath() + "/downloads/";
            File f = new File(myDirectory);
            final String[] fileName = {""};

            while(fileName[0].equals("")){
                if (f.exists() && f.isDirectory()) {
                    final Pattern p = Pattern.compile("15\\.0\\.(.*).exo"); //We wait for the first 15 files
                    File[] flists = f.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                            p.matcher(file.getName()).matches();

                            if (p.matcher(file.getName()).matches()) {
                                fileName[0] =  file.getName();
                            }
                            return p.matcher(file.getName()).matches();
                        }
                    });
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                synchronized (this) {
                    wait(100);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectionStatus.setText("UE with all the files");
                            //onPause(); //Pause o video. Não precisamos baixar os próximos arquivos
                        }
                    });

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Log.d("PlayerActivity", "UE pronto para enviar os arquivos. Já tem até o arquivo " + fileName[0]);
        }
    }
}