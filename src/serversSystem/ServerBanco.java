package serversSystem;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ServerBanco implements Serializable {
    private static int qtdClientes = 0;
    private static MonitorDeInstancias monitor = new MonitorDeInstancias();
    private static List<ListaDeServers> ativos = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Iniciando Servidor...");

        int PORT = Integer.parseInt(args[0]);
        int BACKLOG = Integer.parseInt(args[1]);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> monitoramento(), 0, 20, TimeUnit.SECONDS);

        var serverSocket = new ServerSocket(PORT, BACKLOG);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        while (true) {
            var clientSocket = serverSocket.accept();
            executor.submit(() -> handleRequest(clientSocket));
            qtdClientes++;
            System.out.println("qtd clientes: " + qtdClientes);
        }
    }

    private static void handleRequest(Socket socket) {
        try (socket;
             BufferedReader request = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter response = new PrintWriter(socket.getOutputStream(), true)) {

            String msg = request.readLine();
            StringTokenizer tokenizer = new StringTokenizer(msg, ";");
            tokenizer.nextToken();
            int accNum = Integer.parseInt(tokenizer.nextToken());
            int lastDigit = accNum % 10;

            int portaDestino;
            int bloco;

            if (lastDigit <= 2) {
                portaDestino = 7001;
                bloco = 1;
            } else if (lastDigit <= 5) {
                portaDestino = 7002;
                bloco = 2;
            } else {
                portaDestino = 7003;
                bloco = 3;
            }

            boolean servidorAtivo = false;
            ativos = MonitorDeInstancias.getServidorAtivo();
            for (var ativo : ativos) {
                if (ativo.getPorta() == portaDestino) {
                    servidorAtivo = true;
                    break;
                }
            }

            if (!servidorAtivo) {
                response.println("Erro Servidor indisponível na porta " + portaDestino);
                System.out.println("Servidor inativo na porta " + portaDestino + ", operação não realizada.");
                return;
            }

            // Comunicação via UDP com o servidor auxiliar
            try (DatagramSocket udpSocket = new DatagramSocket()) {
                InetAddress ip = InetAddress.getByName("localhost");

                // Envia a mensagem para o servidor via UDP
                byte[] sendData = msg.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, portaDestino);
                udpSocket.send(sendPacket);

                // Espera a resposta do servidor
                byte[] receiveBuffer = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                udpSocket.setSoTimeout(3000); // 3 segundos
                udpSocket.receive(receivePacket);

                String respBanco = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                System.out.println("Resp banco: " + respBanco);

                try (Socket logSocket = new Socket("localhost", 9000)) {
                    var logOut = new PrintWriter(logSocket.getOutputStream(), true);
                    logOut.println(bloco + ";" + msg); // WAL antes da operação
                    System.out.println("Log enviado com sucesso para o bloco " + bloco + "!");
                }

                response.println(respBanco);
            } catch (SocketTimeoutException e) {
                System.out.println("error;Timeout ao esperar resposta do servidor UDP " + portaDestino);
                response.println("Erro Timeout no servidor");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("error;Falha na comunicação UDP com a porta " + portaDestino);
                response.println("Erro Falha na comunicação UDP");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void monitoramento() {
        monitor.iniciarMonitoramento();
    }
}
