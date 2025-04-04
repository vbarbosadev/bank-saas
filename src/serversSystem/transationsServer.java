package serversSystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import serversSystem.Threads.transationsThreads;

public class transationsServer {

    static int qtdRes = 0;

    public static void main(String[] args) throws IOException {

        try (ServerSocket ss = new ServerSocket(8081)) {
            while (true) {
                Socket apiSocket = ss.accept();
                apiSocket.setSoTimeout(5000);

                // recebendo requisição
                ObjectInputStream req = new ObjectInputStream(apiSocket.getInputStream());

                qtdRes++;



//                ObjectInputStream banco = new ObjectInputStream(apiSocket.getInputStream());
//                transationsThreads t = new transationsThreads(banco);

            }
        }



    }

}
