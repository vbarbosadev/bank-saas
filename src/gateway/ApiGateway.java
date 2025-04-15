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
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {

            String request = in.readLine();



            // Encaminhar para o servidor principal
            try (Socket server = new Socket("localhost", 6000);
                 BufferedReader serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()));

                 PrintWriter serverOut = new PrintWriter(server.getOutputStream(), true)){

                serverOut.println(request);
                System.out.println("request " + request);
                String response = serverIn.readLine();
                output.writeObject(response);
                output.flush();
                output.close();
                in.close();
                System.out.println("response " + response);
            }



        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
