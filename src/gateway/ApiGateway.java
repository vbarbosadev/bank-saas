package gateway;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class ApiGateway {


    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(5000);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        while (true) {
            var clientSocket = serverSocket.accept();
            executor.submit(() -> handleClient(clientSocket));
        }
    }


    private static void handleClient(Socket socket) {
        try (socket) {
            var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            var out = new PrintWriter(socket.getOutputStream(), true);
            String request = in.readLine();


            // Encaminhar para o servidor principal
            try (Socket server = new Socket("localhost", 6000)) {
                var serverOut = new PrintWriter(server.getOutputStream(), true);
                var serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()));
                serverOut.println(request);
                String response = serverIn.readLine();
                out.println(response);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
