package gateway;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class ApiGateway {

    public static void main(String[] args) throws IOException {
        System.out.println("API GATEWAY (UDP)");

        int PORT = Integer.parseInt(args[0]);
        DatagramSocket gatewaySocket = new DatagramSocket(PORT);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        while (true) {
            byte[] buffer = new byte[1024];
            DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
            gatewaySocket.receive(requestPacket);

            executor.submit(() -> handleClient(requestPacket, gatewaySocket));
        }
    }

    public static String validacaoResp(String resp) {
        String[] partes = resp.split(" ", 2); // divide em 2 partes
        String restante = partes[0];
        if (restante.equals("Erro")) {
            return resp;
        }
        return "OK";
    }

    private static void handleClient(DatagramPacket clientPacket, DatagramSocket gatewaySocket) {
        try {
            String request = new String(clientPacket.getData(), 0, clientPacket.getLength(), StandardCharsets.UTF_8);
            System.out.println("request: " + request);

            // Enviar para o servidor principal via UDP
            DatagramSocket serverSocket = new DatagramSocket();
            byte[] sendData = request.getBytes(StandardCharsets.UTF_8);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                    InetAddress.getByName("localhost"), 6000);
            serverSocket.send(sendPacket);

            // Receber resposta do servidor principal
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            serverSocket.receive(receivePacket);
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);

            response = validacaoResp(response);
            System.out.println("resp: " + response);

            // Enviar resposta de volta ao cliente (JMeter)
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            DatagramPacket responsePacket = new DatagramPacket(
                    responseBytes,
                    responseBytes.length,
                    clientPacket.getAddress(),
                    clientPacket.getPort()
            );
            gatewaySocket.send(responsePacket);

            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
