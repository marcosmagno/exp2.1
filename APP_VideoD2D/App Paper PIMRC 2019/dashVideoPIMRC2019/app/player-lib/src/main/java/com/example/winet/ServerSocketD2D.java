package com.example.winet;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class ServerSocketD2D {
    int port;
    String go;
    DataOutputStream os_send = null;
    DataInputStream is_recv = null;
    FileInputStream fis = null;
    BufferedInputStream bis = null;
    OutputStream os = null;
    ReadFile readFile = null;
    public static ArrayList<String> clientsInetAddr = new ArrayList<String>();
    public static ArrayList<Integer> clientsPort = new ArrayList<>();

    public ServerSocketD2D() { }

    public static class FileServerAsync extends AsyncTask<Void, Void, String> {
        private Context context;
        public static String filePath;
        public static boolean cancellationToken = false;

        public FileServerAsync(Context context) {
            this.context = context;
            filePath = context.getExternalCacheDir().getAbsolutePath() + "/downloads/";
            //filePath = context.getExternalCacheDir().getAbsolutePath() + "/";
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                /**
                 * Create a server socket and wait for client connections. This
                 * call blocks until a connection is accepted from a client
                 */
                ServerSocket serverSocket = new ServerSocket(8443);

                while(!cancellationToken) {
                    Socket client = serverSocket.accept();
                    InputStream cis = client.getInputStream();
                    byte[] isBytes = new byte[3];

                    cis.read(isBytes, 0, 3); //Read command string
                    String message = new String(isBytes, Charset.forName("UTF-8"));

                    switch (message) {
                        case "ack":
                            message = Util.DecodeID(cis);
                            Log.d("ServerSocketD2D", "Recebi ACK de cliente escutando na porta " + message);
                            addClientToList(client,message);
                            break;
                        case "dow":
                            message = Util.DecodeID(cis);
                            Log.d("ServerSocketD2D", "Recebi requisicao de arquivo com ID iniciando em " + message);

                            //Modo sem thread
                            //sendFileToClient(client, message);

                            //Modo com thread
                            int port = clientsPort.get(clientsInetAddr.indexOf(client.getInetAddress().getHostAddress()));
                            Runnable r = new SendFileThread(client, port, message);
                            new Thread(r).start();

                            break;
                        default: //Ignore message
                            break;
                    }
                }
                return "";
            } catch (IOException e) {
                Log.e("ServerSocketD2D", e.getMessage());
                return null;
            }
        }

        public static void closeServerSocket() {
            cancellationToken = true;
        }

        public void addClientToList(Socket client, String port) {
            if (!clientsInetAddr.contains(client.getInetAddress().getHostAddress())) {
                clientsInetAddr.add(client.getInetAddress().getHostAddress());
                clientsPort.add(Integer.valueOf(port));

                Log.d("ServerSocketD2D", "Adicionei cliente de IP " + client.getInetAddress().getHostAddress() + " que está escutando conexões na porta " + Integer.valueOf(port));
            }
        }

        public String getFullFileName(String fileID){
            String myDirectory = filePath;
            final String[] fileName = {""};
            File f = new File(myDirectory);

            Log.d("ServerSocketD2D", "Buscando o arquivo que começa com "
                    + fileID + " - f.exists()=" + f.exists() + " and f.isDirectory()=" + f.isDirectory() + ". filePath=" + filePath);

            if (f.exists() && f.isDirectory()) {
                final Pattern p = Pattern.compile(fileID + "\\.0\\.(.*).exo");
                //final Pattern p = Pattern.compile("arq" + fileID);
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
            return fileName[0];
        }

        private void sendFileToClient(Socket client, int destPort, String fileID) {
            String host = client.getInetAddress().getHostAddress();
            Socket socket = new Socket();
            int port = destPort;
            try {
                String fullFileName = getFullFileName(fileID);

                Log.d("ServerSocketD2D", "O Arquivo iniciado em " + fileID + " foi referenciado para o arquivo " + fullFileName);

                Log.d("ServerSocketD2D", "Opening client socket - ");

                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), 5000);

                Log.d("ServerSocketD2D", "Client socket - " + socket.isConnected());

                OutputStream stream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream is;
                String absolutePath = "file://" + filePath + fullFileName;

                try {
                    Log.d("ServerSocketD2D", "Enviando arquivo de caminho absoluto " + absolutePath);
                    is = cr.openInputStream(Uri.parse(absolutePath));
                    Util.copyFileWithPreAppend(is, stream, "arq" + String.valueOf(fullFileName).length() + "-" + fullFileName);
                } catch (FileNotFoundException e) {
                    Log.d("ServerSocketD2D", e.toString());

                    String fileNotAvailableString = "404" + fullFileName;
                    is = new ByteArrayInputStream(fileNotAvailableString.getBytes(Charset.forName("UTF-8")));
                    Util.copyFileWithPreAppend(is, stream, fileNotAvailableString);
                }

                Log.d("ServerSocketD2D", "Client: Data written");
            } catch (IOException e) {
                Log.e("ServerSocketD2D", e.getMessage());
            }
        }

        public class SendFileThread implements Runnable {
            private Socket client;
            private int port;
            private String fileID;

            public SendFileThread(Socket client, int port, String fileID) {
                this.client = client;
                this.port = port;
                this.fileID = fileID;
            }

            public void run() {
                //Espere até que o arquivo requisitado seja fechado
                //(próximo arquivo ainda não existe)
                int followingFile = Integer.parseInt(fileID) + 1;
                String followWingFileStr = String.valueOf(followingFile);
                while(getFullFileName(followWingFileStr) == "") {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                sendFileToClient(client, port, fileID);
            }
        }
    }

    /*public void startSocket(int port, Context context) {
        try {
            String fileRequestLine = null; //ID do arquivo que o cliente D2D está solicitando

            ServerSocket server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(port));

            Log.d("SocketServerD2D", "Porta " + port + " aberta. Aguardando conexão do respectivo cliente D2D");
            Socket clientSocket4G = server.accept();

            // DataOutputStream to send a msg
            this.is_recv = new DataInputStream(clientSocket4G.getInputStream()); //Receber do cliente D2D a ID do arquivo requisitado
            this.os_send = new DataOutputStream(clientSocket4G.getOutputStream()); //Enviar arquivo requisitado para o cliente D2D

            // DataInputStream to recv a msg
            Log.d("SocketServerD2D", "Cliente D2D conectado no IP " + clientSocket4G.getInetAddress().getHostAddress());

            BufferedReader d = new BufferedReader(new InputStreamReader(this.is_recv));
            fileRequestLine = d.readLine();
            Log.d("SocketServerD2D", "Cliente D2D conectado no IP " + clientSocket4G.getInetAddress().getHostAddress() + " solicita o arquivo com ID iniciando em " + fileRequestLine);

            sendFile(fileRequestLine, context, clientSocket4G.getInetAddress().getHostAddress());
        } catch (IOException ex) {
            Log.d("SocketServerD2D:", "Erro de I/O no socket" + ex);
        }
    }

    public void sendFile(String fileID, Context context, String destIP){
        try {
            //Log.d("ListOfFiles", String.valueOf(getReadFile().getListOfTheFiles().get(0)));
            //BufferedReader in = new BufferedReader(new FileReader("/storage/emulated/0/Android/data/com.example.exoplayercodelab/files/"));
            //File file = new File(context.getExternalCacheDir(), "downloads/" + String.valueOf(getReadFile().getListOfTheFiles().get(0)));
            File file = new File(context.getExternalCacheDir(), "/arq" + fileID);
            Log.d("SocketServerD2D", "Nome do arquivo a ser enviado para o cliente D2D " + destIP + ": " + String.valueOf(file));

            byte[] mybytearray = new byte[(int) file.length()];
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            bis.read(mybytearray, 0, mybytearray.length);

            Log.d("SocketServerD2D", "Enviando para o cliente D2D " + destIP + "  arquivo " + String.valueOf(file) + " com " + mybytearray.length + " bytes");
            this.os_send.write(mybytearray, 0, mybytearray.length);
            this.os_send.flush();

            Log.d("SocketServerD2D", "Arquivo enviado para o cliente D2D " + destIP + " com sucesso");
        } catch (FileNotFoundException e) {
            Log.d("SocketServerD2D: ", "Arquivo não encontrado");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d("SocketServerD2D: ", "Erro de I/O");
            e.printStackTrace();
        }
    }

    public void setReadFile(ReadFile readFile) {
        this.readFile = readFile;
    }

    public ReadFile getReadFile(){
        return this.readFile;
    }

    public void listOfFiles(ReadFile readFile) {
    }*/
}
