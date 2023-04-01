/*
 *
 * webnautes@naver.com
 *
 * 참고
 * http://www.kotemaru.org/2013/10/30/android-bluetooth-sample.html
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;


public class Server{

    public static void main(String[] args){


        log("Local Bluetooth device...\n");
        LocalDevice local = null;
        try {
            local = LocalDevice.getLocalDevice(); //로컬 블루투스 장치 가져오기
        } catch (BluetoothStateException e2) {
        }

        log( "address: " + local.getBluetoothAddress() );
        log( "name: " + local.getFriendlyName() );


        Runnable r = new ServerRunable();
        Thread thread = new Thread(r);
        thread.start();

    }


    private static void log(String msg) {
        System.out.println("["+(new Date()) + "] " + msg);
    }

}


class ServerRunable implements Runnable{

    //private static final UUID SERVICE_UUID = UUIDs.OBEX_OBJECT_PUSH; // 예제를 위해 OBEX_OBJECT_PUSH 서비스 UUID 사용
    private static final String SERVICE_NAME = "BluetoothServer"; // 서비스 이름
    //UUID for SPP
    final UUID uuid = new UUID("2D629562155410889541033C40BD4FA7", false);
    String name = SERVICE_NAME;
    // UUID 및 이름으로 서비스 등록
    final String CONNECTION_URL_FOR_SPP = "btspp://localhost:"
            + uuid +";name=" + name + ";authenticate=false;encrypt=false;";
    private StreamConnectionNotifier mStreamConnectionNotifier = null;
    private StreamConnection mStreamConnection = null;
    private int count = 0;


    @Override
    public void run() {

        try {

            //서비스 등록
           mStreamConnectionNotifier = (StreamConnectionNotifier) Connector.open(CONNECTION_URL_FOR_SPP);
            log("Opened connection successful.");
            log("Server is now running.");

            //연결 대기
            while(true){
                log("wait for client requests...");
                try {
                    mStreamConnection = mStreamConnectionNotifier.acceptAndOpen();
                    RemoteDevice device = RemoteDevice.getRemoteDevice(mStreamConnection);
                    log("Connected to " + device.getFriendlyName(true));
                } catch (IOException e1) {
                    log("Could not open connection: " + e1.getMessage() );
                }

                count++;
                log("현재 접속 중인 클라이언트 수: " + count);
                new Receiver(mStreamConnection).start();
            }
        } catch (BluetoothStateException e) {
            e.printStackTrace();
        }catch (IOException e) {
            log("Could not open connection: " + e.getMessage());
            return;
        }
        finally {
            // 연결 종료
            if (mStreamConnection != null) {
                try {
                    mStreamConnection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 서비스 종료
            if (mStreamConnectionNotifier != null) {
                try {
                    mStreamConnectionNotifier.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }



    class Receiver extends Thread {

        private InputStream mInputStream = null;
        private OutputStream mOutputStream = null;
        private String mRemoteDeviceString = null;
        private StreamConnection mStreamConnection = null;
        Receiver(StreamConnection streamConnection){
            mStreamConnection = streamConnection;
            try {
                mInputStream = mStreamConnection.openInputStream();
                mOutputStream = mStreamConnection.openOutputStream();
                log("Open streams...");
            } catch (IOException e) {
                log("Couldn't open Stream: " + e.getMessage());
                Thread.currentThread().interrupt();
                return;
            }


            try {
                RemoteDevice remoteDevice
                        = RemoteDevice.getRemoteDevice(mStreamConnection);
                mRemoteDeviceString = remoteDevice.getBluetoothAddress();
                log("Remote device");
                log("address: "+ mRemoteDeviceString);

            } catch (IOException e1) {
                log("Found device, but couldn't connect to it: " + e1.getMessage());
                return;
            }
            log("Client is connected...");
        }


        @Override
        public void run() {

            try {
                Reader mReader = new BufferedReader(new InputStreamReader
                        ( mInputStream, Charset.forName(StandardCharsets.UTF_8.name())));
                boolean isDisconnected = false;
                Sender("에코 서버에 접속하셨습니다.");
                Sender( "보내신 문자를 에코해드립니다.");
                while(true){

                    log("ready");
                    StringBuilder stringBuilder = new StringBuilder();
                    int c = 0;
                    while ( '\n' != (char)( c = mReader.read()) ) {
                        if ( c == -1){
                            log("Client has been disconnected");
                            count--;
                            log("현재 접속 중인 클라이언트 수: " + count);
                            isDisconnected = true;
                            Thread.currentThread().interrupt();
                            break;
                        }
                        stringBuilder.append((char) c);
                    }

                    if ( isDisconnected ) break;
                    String recvMessage = stringBuilder.toString();
                    log( mRemoteDeviceString + ": " + recvMessage );
                    Sender(recvMessage);
                }

            } catch (IOException e) {
                log("Receiver closed" + e.getMessage());
            }
        }


        void Sender(String msg){
            PrintWriter printWriter = new PrintWriter(new BufferedWriter
                    (new OutputStreamWriter(mOutputStream,
                            Charset.forName(StandardCharsets.UTF_8.name()))));

            printWriter.write(msg+"\n");
            printWriter.flush();
            log( "Me : " + msg );
        }
    }


    private static void log(String msg) {
        System.out.println("["+(new Date()) + "] " + msg);
    }

}