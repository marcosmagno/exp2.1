package com.example.winet;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Util {

    private static String TAG = "Util";

    public static boolean copyFile(InputStream inputStream, OutputStream out, boolean canClose) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return false;
        }
        return true;
    }

    public static boolean copyFileWithPreAppend(InputStream inputStream, OutputStream out, String preAppendString) {
        byte buf[] = new byte[1024];
        int len;
        byte[] preAppend;
        preAppend = preAppendString.getBytes(Charset.forName("UTF-8"));
        try {
            out.write(preAppend);
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return false;
        }
        return true;
    }

    public static String DecodeID(InputStream is) {
        try {
            byte[] isBytes = new byte[1];
            is.read(isBytes, 0, 1);
            String message = new String(isBytes, Charset.forName("UTF-8"));

            if (message.equals("1")) {
                isBytes = new byte[1];
                is.read(isBytes, 0, 1);
            } else if (message.equals("2")) {
                isBytes = new byte[2];
                is.read(isBytes, 0, 2);
            } else if (message.equals("3")) {
                isBytes = new byte[3];
                is.read(isBytes, 0, 3);
            } else if (message.equals("4")) {
                isBytes = new byte[4];
                is.read(isBytes, 0, 4);
            } else {
                isBytes = new byte[5];
                is.read(isBytes, 0, 5);
            }

            message = new String(isBytes, Charset.forName("UTF-8"));
            return message;
        } catch (Exception e) {
            e.printStackTrace();
            return "1";
        }
    }

    public static String decodeFileName(InputStream is) {
        try {
            byte[] bytesForSizeAux = new byte[3];
            int bytesRead = 0;

            byte[] currByte = new byte[1];
            String currChar = "";
            while(!currChar.equals("-")) {
                is.read(currByte, 0, 1);
                bytesForSizeAux[bytesRead] = currByte[0];
                currChar = new String(currByte, Charset.forName("UTF-8"));
                bytesRead++;
            }

            byte[] bytesForSize = new byte[bytesRead-1];
            System.arraycopy(bytesForSizeAux,0,bytesForSize,0,bytesRead-1);

            byte[] fileNameBytes = new byte[Integer.valueOf(new String(bytesForSize, Charset.forName("UTF-8")))];
            is.read(fileNameBytes, 0, Integer.valueOf(new String(bytesForSize, Charset.forName("UTF-8"))));

            String fileNameStr = new String(fileNameBytes, Charset.forName("UTF-8"));

            return fileNameStr;
        } catch (Exception e) {
            e.printStackTrace();
            return "1";
        }
    }

    public static String getFormattedDateTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
        Date now = Calendar.getInstance().getTime();
        String nowString = simpleDateFormat.format(now).toString();
        return nowString;
    }

    public static void appendLog(String logInfo, String fileUri)
    {
        final File logFile = new File(fileUri + "/logs/downloadLog.txt");

        String logHeader = "Download Log: " + Calendar.getInstance().getTime() + "\r\n";
        logHeader += logInfo;

        if (!logFile.exists())
        {
            try
            {
                File dirs = new File(logFile.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(logHeader);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void internetDownloadLog(String logInfo, String fileUri)
    {
        final File logFile = new File(fileUri + "/logs/downloadLog.csv");

        if (!logFile.exists())
        {
            try
            {
                File dirs = new File(logFile.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(logInfo);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}
