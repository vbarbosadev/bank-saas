package serversSystem;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

public class ServerBanco implements Serializable {

    public static AtomicInteger qtdClients = new AtomicInteger(0);
    private static MonitorDeInstancias monitor = new MonitorDeInstancias();
    private static List<ListaDeServers> ativos = new ArrayList<>();

    private static Set<Integer> portasEmRecuperacao = ConcurrentHashMap.newKeySet();
    private static Lock lock = new ReentrantLock();
    private static Condition podeEnviar = lock.newCondition();

    public static void main(String[] args) throws IOException {
        System.out.println("Iniciando Servidor...\n");

        int PORT = Integer.parseInt(args[0]);
        DatagramSocket udpSocket = new DatagramSocket(PORT);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(ServerBanco::monitoramento, 0, 20, TimeUnit.SECONDS);

        var executor = Executors.newVirtualThreadPerTaskExecutor();

        byte[] buffer = new byte[1024];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            udpSocket.receive(packet);
            executor.submit(() -> handleRequest(packet, udpSocket));
        }
    }

    private static void handleRequest(DatagramPacket packet, DatagramSocket udpSocket) {
        try {
            String msg = new String(packet.getData(), 0, packet.getLength());
            StringTokenizer tokenizer = new StringTokenizer(msg, ";");
            tokenizer.nextToken();
            int accNum = Integer.parseInt(tokenizer.nextToken());
            int lastDigit = accNum % 10;

            int portaDestino;
            if (lastDigit <= 2) portaDestino = 7001;
            else if (lastDigit <= 5) portaDestino = 7002;
            else portaDestino = 7003;

            boolean servidorAtivo = false;
            ativos = MonitorDeInstancias.getServidorAtivo();
            for (var ativo : ativos) {
                if (ativo.getPorta() == portaDestino) {
                    servidorAtivo = true;
                    break;
                }
            }

            String resp;
            if (!servidorAtivo) {
                resp = "Erro: Servidor indisponível na porta " + portaDestino;
                System.out.println("Servidor inativo na porta " + portaDestino + ", operação não realizada.");
            } else {
                aguardarSeRecuperando(portaDestino);

                // Envia via UDP
                DatagramSocket auxSocket = new DatagramSocket();
                byte[] sendData = msg.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                        InetAddress.getByName("localhost"), portaDestino);
                auxSocket.send(sendPacket);

                // Espera resposta
                byte[] receiveBuffer = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                auxSocket.setSoTimeout(3000); // timeout opcional
                auxSocket.receive(receivePacket);

                resp = new String(receivePacket.getData(), 0, receivePacket.getLength());
                auxSocket.close();
            }

            // Resposta para cliente original
            byte[] responseData = resp.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length,
                    packet.getAddress(), packet.getPort());
            udpSocket.send(responsePacket);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            qtdClients.decrementAndGet();
        }
    }

    private static void monitoramento() {
        monitor.iniciarMonitoramento();
    }

    public static void pausarEnviosPara(int porta) {
        lock.lock();
        try {
            portasEmRecuperacao.add(porta);
            System.out.println("[ServerBanco] Porta " + porta + " está em recuperação.");
        } finally {
            lock.unlock();
        }
    }

    public static void liberarEnviosPara(int porta) {
        lock.lock();
        try {
            portasEmRecuperacao.remove(porta);
            System.out.println("[ServerBanco] Porta " + porta + " liberada.");
            podeEnviar.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private static void aguardarSeRecuperando(int porta) {
        lock.lock();
        try {
            while (portasEmRecuperacao.contains(porta)) {
                System.out.println("[ServerBanco] Aguardando recuperação da porta " + porta);
                podeEnviar.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }
}
