package com.example.winet;

import android.app.DownloadManager;
import android.content.Context;
import android.util.Log;
/*
//import com.google.android.exoplayer2.offline.ProgressiveDownloadAction;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.offline.ProgressiveDownloader;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;

import java.io.File;

public class DownloadUtil {
    private static Cache cache;
    private static com.google.android.exoplayer2.offline.DownloadManager downloadManager;

    public static synchronized Cache getCache(Context context) {
        if (cache == null) {

            File cacheDirectory = new File(context.getExternalFilesDir(null), "downloads");
            Log.d("Cache", "cache folder"+cacheDirectory);
            cache = new SimpleCache(cacheDirectory, new NoOpCacheEvictor());
        }
        return cache;
    }

/*
    public static synchronized DownloadManager getDownloadManager(Context context) {
        if (downloadManager == null) {
            File actionFile = new File(context.getExternalFilesDir(null), "actions");
            downloadManager =
                    new DownloadManager(
                            getCache(context),
                            new DefaultDataSourceFactory(
                                    context,
                                    Util.getUserAgent(context, "MediaPlayer")),
                            actionFile);

        }
    }


}
*/