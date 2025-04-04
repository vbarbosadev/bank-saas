package objeto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.StringTokenizer;

public class processRequest extends Thread {
    static int port = 0;
    private Socket clientSocket;


    public processRequest(Socket s){ this.clientSocket = s; }

    @Override
    public void run() {

        try {

            Thread.sleep(100);


            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String entrada = in.readLine();

            String action;
            StringTokenizer tokenizer = new StringTokenizer(entrada, ";");
            action = tokenizer.nextToken();

            switch (action) {
                case "DEPOSITO":

                    port = 8081;
                    break;
                case "SAQUE":
                    port = 8081;
                    break;
                case "SALDO":
                    port = 8081;
                    break;
                case "CRIAR":
                    port = 8082;
                    break;
                case "BUSCAR":
                    port = 8082;
                    break;
                default:
                    port = 8080;
                    break;
            }


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public static int getRequest(String entrada) {
        try {
            Thread.sleep(100);

            String action;
            StringTokenizer tokenizer = new StringTokenizer(entrada, ";");
            action = tokenizer.nextToken();

            switch (action) {
                case "DEPOSITO":
                    port = 8081;
                    break;
                case "SAQUE":
                    port = 8081;
                    break;
                case "SALDO":
                    port = 8081;
                    break;
                case "CRIAR":
                    port = 8082;
                    break;
                case "BUSCAR":
                    port = 8082;
                    break;
                default:
                    port = 8080;
                    break;
            }


        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        return port;


    }
}
