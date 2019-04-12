package com.example.winet;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientSocket4G {

    Socket smtpSocket = null;
    DataOutputStream os_send = null;
    DataInputStream is_recv = null;
    String responseLine = null;
    public boolean lastReportSent = true;

    public boolean isLastReportSent() {
        return lastReportSent;
    }

    public void setLastReportSent(boolean lastReportSent) {
        this.lastReportSent = lastReportSent;
    }

    //public static String sd_ip = "150.164.10.123";
    public static String sd_ip = "192.168.0.67";

    public void startConnection() {
        try {
            this.smtpSocket = new Socket(sd_ip, 10001);
            this.os_send = new DataOutputStream(this.smtpSocket.getOutputStream());
            this.is_recv =new DataInputStream(this.smtpSocket.getInputStream());

            Log.d("ClientSocket4G", "Conectando ao SD no IP " + sd_ip);
        } catch (UnknownHostException e) {
            System.err.println("SD com IP " + sd_ip + " não encontrado");

            Log.d("ClientSocket4G", "SD com IP " + sd_ip + " não encontrado");
        } catch (IOException e) {
            System.err.println("Não foi possível obter I/O para conexão com o SD com IP " + sd_ip);

            Log.d("ClientSocket4G", "Não foi possível obter I/O para conexão com o SD com IP " + sd_ip);
        }
    }

    public void sendData4G(String typeMsg, String payload) {
        if (this.os_send != null) {
            try {
                //this.os_send.writeBytes("-"  + typeMsg + "-" + payload);
                this.os_send.writeBytes(typeMsg + "," + payload);

                if(typeMsg.equals("2")) {
                    this.setLastReportSent(true);
                }

                Log.d("ClientSocket4G","Mensagem enviada para o SD: "  + typeMsg + "," + payload);

                /*this.smtpSocket.close();
                this.os_send.close();
                this.is_recv.close();*/
            } catch (UnknownHostException e) {
                System.err.println("Trying to connect to unknown host: " + e);
            } catch (IOException e) {
                System.err.println("IOException:  " + e);
            }
        }
    }

    public void recvData4G() {
        if (this.smtpSocket != null && this.is_recv != null) {
            try {
                BufferedReader d = new BufferedReader(new InputStreamReader(smtpSocket.getInputStream()));
                responseLine = d.readLine();

                Log.d("ClientSocket4G","Recebi mensagem de formação de grupo: " + responseLine);
            } catch (UnknownHostException e) {
                System.err.println("Trying to connect to unknown host: " + e);
            } catch (IOException e) {
                System.err.println("IOException:  " + e);
            }
        }
    }

    public String getResponseLine() {
        return responseLine;
    }
}
