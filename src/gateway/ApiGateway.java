package gateway;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class ApiGateway {


    public static void main(String[] args) throws IOException {
        System.out.println("API GATEWAY");
        ServerSocket serverSocket = new ServerSocket(5000);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            executor.submit(() -> handleClient(clientSocket));
        }
    }


    private static void handleClient(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

            String request = in.readLine();
            System.out.println("request " + request);


            // Encaminhar para o servidor principal
            try (Socket server = new Socket("localhost", 6000);
                 BufferedReader serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()));
                 PrintWriter serverOut = new PrintWriter(server.getOutputStream(), true)){

                serverOut.println(request);

                String response = serverIn.readLine();
                output.println(response);
            }



        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
