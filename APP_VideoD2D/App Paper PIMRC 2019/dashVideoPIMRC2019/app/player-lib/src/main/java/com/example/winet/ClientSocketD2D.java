package com.example.winet;
import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

public class ClientSocketD2D {
    public static class ClientReceiverAsync extends AsyncTask<Void, Void, String> {

        private Context context;
        public static String filePath;
        public static boolean cancellationToken = false;
        public static String recvPort;
        public static String macFromGO;
        public static String macFromClient;
        public static ClientSocket4G clientSendSocket4G;

        //int fileID = 1;

        public ClientReceiverAsync(Context context, String fileRecvPort, String macGO, String macClient, ClientSocket4G clientSocket4G) {
            this.context = context;
            //filePath = "/storage/emulated/0/Android/data/com.example.exoplayercodelab/files/";
            //filePath = "/sdcard/Android/data/com.example.exoplayercodelab/files/";
            filePath = context.getExternalCacheDir().getAbsolutePath() + "/";
            recvPort = fileRecvPort;
            macFromGO = macGO;
            macFromClient = macClient;
            clientSendSocket4G = clientSocket4G;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {

                /**
                 * Create a socket and wait for server responses. This
                 * call blocks until a message is received from the GO
                 */
                //ServerSocket recvSocket = new ServerSocket(8443);
                ServerSocket recvSocket = new ServerSocket(Integer.valueOf(recvPort));

                Log.d("ClientSocketD2D", "Cliente escutando no IP " + recvSocket.getInetAddress().getHostAddress() + " na porta " + recvPort);

                while(!cancellationToken) {
                    Socket client = recvSocket.accept(); //Only .exo files will be received from the GO

                    InputStream cis = client.getInputStream();
                    byte[] isBytes = new byte[3];
                    cis.read(isBytes, 0, 3);
                    String fileName = Util.decodeFileName(cis);

                    //String fileName = "arq" + fileID;
                    //fileID++;

                    Log.d("ClientSocketD2D", "Recebendo arquivo '" + fileName + "'...");

                    receiveFile(cis, String.valueOf(fileName));
                }
                return "";
            } catch (IOException e) {
                Log.e("ClientSocketD2D", e.getMessage());
                return null;
            }
        }

        public static void closeRecvSocket() {
            cancellationToken = true;
        }

        private void receiveFile(InputStream is, String fileNumber) {
            try {
                long initialTime = SystemClock.elapsedRealtime();

                File f = new File(filePath + fileNumber);

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();
                Util.copyFile(is, new FileOutputStream(f), true);

                long finalTime = SystemClock.elapsedRealtime();

                float elapsedTime = finalTime - initialTime;
                elapsedTime /= 1000;
                float fileSize = f.length() / (1024.0f * 1024.0f);
                float avgSpeed = fileSize / elapsedTime;

                String logInfo = "Received file: " + fileNumber + "\r\n";
                logInfo += "Time to download: " + elapsedTime + " segundos\r\n" ;
                logInfo += "File size: " + fileSize + " MB\r\n";
                logInfo += "Average speed: " + avgSpeed + " MB/s\r\n";

                Log.d("ClientSocketD2D", "Time to download: " + elapsedTime + " segundos");
                Log.d("ClientSocketD2D", "File size: " + fileSize + " MB");
                Log.d("ClientSocketD2D", "Average speed: " + avgSpeed + " MB/s");

                Util.appendLog(logInfo, filePath);
                logInfo = Util.getFormattedDateTime() + "," + elapsedTime + "," + fileSize;
                Util.internetDownloadLog(logInfo, filePath);

                Log.d("ClientSocketD2D", "real size: " + f.length());

                Log.d("ClientSocketD2D", "Enviando resultados para o SD: " + macFromGO + "," + macFromClient + "," + fileNumber + "," + elapsedTime + "," + fileSize + "," + avgSpeed + "," + f.length());

                clientSendSocket4G.sendData4G("2", macFromGO + "," + macFromClient + "," + fileNumber + "," + elapsedTime + "," + fileSize + "," + avgSpeed + "," + f.length()); //Envie o desempenho da última transmissão para o SD
            }
            catch (IOException e) {
                Log.e("ClientSocketD2D", e.getMessage());
            }
        }
    }

    /*Socket socket = null;
    DataOutputStream os_send = null;
    FileOutputStream fos = null;
    BufferedOutputStream bos = null;

    public final static int FILE_SIZE = 10000000;
    public final static int MAX_READ_LENGTH = 1460;

    public void startConnection(String IP, int port) {
        try {
            this.socket = new Socket();

            Log.d("ClientSocketD2D", "Conectando com o GO através da porta " + String.valueOf(port));

            this.socket.connect(new InetSocketAddress("192.168.49.1", port), 1000);

            this.os_send = new DataOutputStream(this.socket.getOutputStream());
        } catch (UnknownHostException e) {
            Log.d("ClientSocketD2D", "Host desconhecido" );
        } catch (IOException e) {
            Log.d("ClientSocketD2D", "Não foi possível obter a I/O para o host especificado" );
        }
    }

    public void receiveDataCellPhone(int count, Context context) {
        int bytesRead;
        int current = 0;

        if (this.socket != null && this.os_send != null ) {
            try {
                this.os_send.writeBytes(count + "\n"); //Solicitando arquivo cujo nome inicia com 'count'

                // Preparando buffer para receber proximo arquivo
                byte[] mybytearray = new byte[FILE_SIZE];
                InputStream is = this.socket.getInputStream();

                fos = new FileOutputStream("/storage/emulated/0/Android/data/com.example.exoplayercodelab/file.recv" + count);
                bos = new BufferedOutputStream(fos);

                Log.d("Mybytes", String.valueOf(mybytearray));
                Log.d("ClientSocketD2D", "Tamanho do arquivo com final" + count + " = " + String.valueOf(mybytearray.length));

                //bytesRead = is.read(mybytearray, 0, mybytearray.length);
                //current = bytesRead;

                current = 0; //Vamos iniciar a escrita do buffer na posição 0 do mesmo
                do {
                    //Only if a buffer of 10MB is not enough
                    //if(current = FILE_SIZE){
                    //    current = 0;
                    //}
                    //bytesRead = is.read(mybytearray, current, (mybytearray.length - current));
                    bytesRead = is.read(mybytearray, current, MAX_READ_LENGTH); //Receber no máximo 1460 bytes por vez
                    Log.d("ClientSocketD2D", "Bytes recebidos na última leitura para o arquivo com final " + count + ": " + String.valueOf(bytesRead));
                    if (bytesRead >= 0){
                        current += bytesRead;
                    }
                } while (bytesRead > -1);
                bos.write(mybytearray, 0, current);
                bos.flush();
                fos.close();
                Log.d("ClientSocketD2D", "Arquivo com final " + count + "recebido. Tamanho total: " + current + "bytes");
            } catch (UnknownHostException e) {
                Log.d("ClientSocketD2D", "Erro para receber mensagem: " + e);
            } catch (IOException e) {
                Log.d("ClientSocketD2D", "Erro para receber mensagem");
            }
        }
    }*/
}
