package serversSystem;

import WAL.WALUtils;
import objetos.Banco;
import objetos.ProcessadorBancario;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ServerInstance {

    public static int qtdClients = 0;
    public static Banco banco = new Banco();
    public static ProcessadorBancario process = new ProcessadorBancario(banco);
    public static int PORT = 0;

    public static void main(String[] args) {

        PORT = Integer.parseInt(args[0]);
        System.out.println("Server Auxiliar (UDP) iniciado na porta: " + PORT);

        // ✅ Envia banco a cada 60 segundos
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> enviarBancoParaAuxiliar(), 0, 60, TimeUnit.SECONDS);

        // 🔥 Thread pool para requisições recebidas via UDP
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        try (DatagramSocket udpSocket = new DatagramSocket(PORT)) {
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                executor.execute(() -> handleClient(packet, udpSocket));
                qtdClients++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(DatagramPacket packet, DatagramSocket udpSocket) {
        try {
            String msg = new String(packet.getData(), 0, packet.getLength()).trim();
            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();

            if ("PING".equals(msg)) {
                sendUdpResponse("PONG", clientAddress, clientPort, udpSocket);
                return;
            }
            if ("UPDATE".equals(msg)) {
                enviarBancoParaAuxiliar();
                return;
            }

            System.out.println("Operação recebida de " + clientAddress + ": " + msg);
            String reply = process.processar(msg);

            sendUdpResponse(reply, clientAddress, clientPort, udpSocket);

        } catch (Exception e) {
            System.err.println("Erro ao processar pacote UDP: " + e.getMessage());
        } finally {
            qtdClients--;
        }
    }

    private static void sendUdpResponse(String message, InetAddress address, int port, DatagramSocket socket) {
        try {
            byte[] data = message.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(data, data.length, address, port);
            socket.send(responsePacket);
        } catch (IOException e) {
            System.err.println("Erro ao enviar resposta UDP: " + e.getMessage());
        }
    }

    // 🚀 Envia o objeto Banco para o servidor auxiliar via TCP
    private static void enviarBancoParaAuxiliar() {
        try (Socket socket = new Socket("localhost", 8000); // TCP ainda
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            banco.imprimirContas();
            out.writeObject(banco);
            out.flush();
            System.out.println("[LOG] Contato com o servidor auxiliar foi bem sucedido.");

            banco = (Banco) in.readObject();
            banco.imprimirContas();
            process.setBanco(banco);

        } catch (IOException e) {
            System.err.println("[LOG] Falha ao enviar banco para o auxiliar: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
