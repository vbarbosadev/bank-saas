package gateway;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class ApiGateway {

    public static void main(String[] args) throws IOException {
        System.out.println("API GATEWAY HTTP/1.0");

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
        String[] partes = resp.split(":", 2);
        String erro = partes[0];
        if (erro.equals("Erro de transação")) {
            System.out.println("resp: OK");
            return "OK";
        } else if (erro.equals("Erro")) {
            System.err.println("resp: OK");
            return erro;
        }
        System.out.println("resp: OK");
        return "OK";
    }

    private static void handleClient(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            // Lê a primeira linha da requisição HTTP
            String requestLine = in.readLine();
            if (requestLine == null || !requestLine.startsWith("GET")) {
                sendHttpResponse(out, "400 Bad Request", "Formato de requisição inválido");
                return;
            }

            System.out.println("Requisição recebida: " + requestLine);

            // Extrai o caminho da requisição
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendHttpResponse(out, "400 Bad Request", "Requisição malformada");
                return;
            }

            String comando = parts[1].substring(1); // remove a barra inicial

            System.out.println("Comando recebido: " + comando);

            // Encaminha comando para o servidor backend (TCP como antes)
            try (Socket server = new Socket("localhost", 6000);
                 BufferedReader serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()));
                 PrintWriter serverOut = new PrintWriter(server.getOutputStream(), true)) {

                serverOut.println(comando);
                String response = serverIn.readLine();

                response = validacaoResp(response);
                sendHttpResponse(out, "200 OK", response);

            } catch (IOException e) {
                sendHttpResponse(out, "500 Internal Server Error", "Erro ao contatar servidor");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendHttpResponse(OutputStream out, String status, String body) throws IOException {
        String response = "HTTP/1.0 " + status + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
