package WAL;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ServerWAL {
    private static final String LOG_PATH01 = "log_bloco1.txt";
    private static final String LOG_PATH02 = "log_bloco2.txt";
    private static final String LOG_PATH03 = "log_bloco3.txt";

    public static void main(String[] args) throws IOException {

        int PORT = Integer.parseInt(args[0]);

        var serverSocket = new DatagramSocket(PORT);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        byte[] receiveData = new byte[65535];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        while (true) {
            serverSocket.receive(receivePacket);
            executor.submit(() -> saveLog(receivePacket, serverSocket));
        }
    }

    private static void saveLog(DatagramPacket receivePacket, DatagramSocket serverSocket) {
        try {
            String msg = new String(receivePacket.getData(), 0, receivePacket.getLength());
            String[] partes = msg.split(";", 2);
            String request = partes[1];
            String bloco = partes[0];

            if(getComando(request).equals("saldo")){
                return;
            }

            if (request.equals("COMMIT")) {
                marcarComoCommit(bloco);
                System.out.println("Log do bloco " + bloco + " marcado como COMMIT.");
                return;
            }

            String logLine = System.currentTimeMillis() + ";" + request + ";PENDENTE" + System.lineSeparator();

            switch (bloco){
                case "1" -> Files.writeString(Path.of(LOG_PATH01), logLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                case "2" -> Files.writeString(Path.of(LOG_PATH02), logLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                case "3" -> Files.writeString(Path.of(LOG_PATH03), logLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }

            System.out.println("Log salvo no bloco " + bloco + ": " + logLine);

            // Envia resposta para o cliente
            sendResponse("Log salvo no bloco " + bloco, receivePacket.getAddress(), receivePacket.getPort(), serverSocket);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendResponse(String response, InetAddress clientAddress, int clientPort, DatagramSocket serverSocket) {
        try {
            byte[] responseData = response.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(responseData, responseData.length, clientAddress, clientPort);
            serverSocket.send(sendPacket);
        } catch (IOException e) {
            System.err.println("Error sending response: " + e.getMessage());
        }
    }

    private static void marcarComoCommit(String bloco) throws IOException {

        System.err.println("Marcar como COMMIT entrou");

        Path path = switch (bloco){
            case "1" -> Path.of(LOG_PATH01);
            case "2" -> Path.of(LOG_PATH02);
            case "3" -> Path.of(LOG_PATH03);
            default -> throw new IllegalArgumentException("Bloco inválido");
        };

        if (!Files.exists(path)) return;

        List<String> linhas = Files.readAllLines(path);
        List<String> atualizadas = linhas.stream()
                .map(linha -> linha.endsWith("PENDENTE") ? linha.replace("PENDENTE", "COMMIT") : linha)
                .collect(Collectors.toList());

        Files.write(path, atualizadas);
    }

    private static String getComando(String msg) {
        String[] partes = msg.split(";", 2);
        return partes[0];
    }
}
