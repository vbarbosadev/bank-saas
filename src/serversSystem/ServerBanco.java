package serversSystem;

import objeto.Banco;
import objeto.Conta;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

public class ServerBanco {
    private static final Banco banco = new Banco("Banco");

    public static void main(String[] args) throws IOException {
        var serverSocket = new ServerSocket(6000);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        while (true) {
            var clientSocket = serverSocket.accept();
            executor.submit(() -> handleRequest(clientSocket));
        }
    }

    private static void handleRequest(Socket socket) {
        try (socket) {
            var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            var out = new PrintWriter(socket.getOutputStream(), true);
            String msg = in.readLine();

            // Enviar para o servidor de log (WAL)
            try (Socket logSocket = new Socket("localhost", 9000)) {
                var logOut = new PrintWriter(logSocket.getOutputStream(), true);
                logOut.println(msg);  // WAL antes da operação
            }

            // Encaminhar para um servidor auxiliar (ex: localhost:7000)
            try (Socket auxSocket = new Socket("localhost", 7000)) {
                var auxOut = new PrintWriter(auxSocket.getOutputStream(), true);
                var auxIn = new BufferedReader(new InputStreamReader(auxSocket.getInputStream()));
                auxOut.println(msg);
                String respostaAux = auxIn.readLine();
                out.println(respostaAux);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
