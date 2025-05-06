package gateway;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class ApiGateway {

    public static void main(String[] args) throws IOException {
        int PORT = Integer.parseInt(args[0]);
        int BACKLOG = Integer.parseInt(args[1]);

        System.out.println("API GATEWAY HTTP " + PORT);

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

            String requestLine = in.readLine();

            System.out.println("Requisição recebida:" + requestLine);

            if (requestLine == null || !requestLine.startsWith("GET")) {
                enviarRespostaHttp(out, "400 Bad Request", "Formato de requisição inválido");
                return;
            }

            // Extrai o caminho da requisição
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                enviarRespostaHttp(out, "400 Bad Request", "Requisição malformada");
                return;
            }

            String comando = parts[1].substring(1); // remove a barra inicial

            // descarta cabeçalhos restantes
            while (in.ready()) in.readLine();

            System.out.println("Comando recebido: " + comando);

            // Envia requisição HTTP para o servidor backend na porta 6000 via Socket
            try (Socket server = new Socket()){
                server.connect(new InetSocketAddress("localhost", 6000), 5000);
                 BufferedReader serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()));
                OutputStream serverOut = socket.getOutputStream();

                // Criando a requisição HTTP para o servidor de backend na porta 6000
                String request = "GET /" + comando + " HTTP/1.1\r\n" +
                        "Host: localhost:6000\r\n";

                // Envia a requisição HTTP para o servidor backend
                //serverOut.print(request);
                enviarRespostaHttp(serverOut, "200 OK", request);

                System.out.println(request);

                // Lê a resposta do servidor backend
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = serverIn.readLine()) != null) {
                    response.append(line).append("\n");
                }

                // Valida a resposta
                String validResponse = validacaoResp(response.toString().trim());

                // Envia a resposta HTTP para o cliente
                enviarRespostaHttp(out, "200 OK", validResponse);

            } catch (IOException e) {
                enviarRespostaHttp(out, "500 Internal Server Error", "Erro ao contatar servidor");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void enviarRespostaHttp(OutputStream out, String status, String body) throws IOException {
        String response = "HTTP/1.0 " + status + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
