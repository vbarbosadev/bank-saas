package getway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.StringTokenizer;

public class ThreadGetway extends Thread{

    private Socket clientSocket;
    private Socket bancoSocket;

    public ThreadGetway(Socket clientSocket, Socket bancoSocket) {
        super();
        this.clientSocket = clientSocket;
        this.bancoSocket = bancoSocket;
    }

    Runnable r = new Runnable() {
        @Override
        public void run() {

            try {
                while(true){

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    };
    var b = Thread.ofVirtual().name("y").start(r);





}
