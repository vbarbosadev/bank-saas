package gateway;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class ApiGateway {


    public static void main(String[] args) throws IOException {
        System.out.println("API GATEWAY");

        int PORT = Integer.parseInt(args[0]);
        int BACKLOG = Integer.parseInt(args[1]);

        ServerSocket serverSocket = new ServerSocket(PORT, BACKLOG);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            executor.submit(() -> handleClient(clientSocket));
        }
    }

    public static String validacaoResp(String resp) {
        String[] partes = resp.split(" ", 2); // divide em 2 partes
        String restante = partes[0];
        //System.out.println(restante); // saída: cmd;num;val
        if(restante.equals("Erro")) {
            //System.out.println("ERROOOOOOOOOOOOOOOOOOOOR");
            return resp;
        }
        return "OK";
    }


    private static void handleClient(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

            String request = in.readLine();



            // Encaminhar para o servidor principal
            try (Socket server = new Socket("localhost", 6000);
                 BufferedReader serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()));
                 PrintWriter serverOut = new PrintWriter(server.getOutputStream(), true)){

                serverOut.println(request);
                System.out.println("request " + request);
                String response = serverIn.readLine();

                response = validacaoResp(response);

                output.println(response);
                output.flush();
                output.close();
                socket.close();
                System.out.println("resp: " + response);
            }



        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
