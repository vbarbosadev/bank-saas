package gateway;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class ApiGateway {

    public static void main(String[] args) throws IOException {
        System.out.println("API GATEWAY (UDP)");

        int PORT = Integer.parseInt(args[0]);
        DatagramSocket serverSocket = new DatagramSocket(PORT);

        var executor = Executors.newVirtualThreadPerTaskExecutor();

        byte[] receiveBuffer = new byte[1024];

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            serverSocket.receive(receivePacket);

            executor.submit(() -> handleClient(receivePacket, serverSocket));
        }
    }

    private static void handleClient(DatagramPacket packet, DatagramSocket serverSocket) {
        try {
            String request = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();

            System.out.println("Recebido: " + request);

            // Encaminhar para o servidor principal (também precisa ser UDP nesse caso)
            String response = forwardToMainServer(request);

            response = validacaoResp(response);

            // Enviar resposta (como um ACK) para o cliente UDP
            byte[] sendBuffer = response.getBytes(StandardCharsets.UTF_8);
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress, clientPort);
            serverSocket.send(sendPacket);

        } catch (IOException e) {
            e.printStackTrace();
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

    private static String forwardToMainServer(String request) {
        // envia via UDP para o servidor principal (localhost:6000)
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] buffer = request.getBytes(StandardCharsets.UTF_8);
            InetAddress address = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 6000);
            socket.send(packet);

            // Receber resposta do servidor principal
            byte[] responseBuffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.setSoTimeout(2000); // timeout de 2 segundos
            socket.receive(responsePacket);

            return new String(responsePacket.getData(), 0, responsePacket.getLength(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            e.printStackTrace();
            return "Erro: falha na comunicação com servidor principal";
        }
    }
}
