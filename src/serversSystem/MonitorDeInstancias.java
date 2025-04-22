package serversSystem;

import WAL.ReplayerDeLog;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MonitorDeInstancias {
    private static List<ListaDeServers> auxiliares;

    public MonitorDeInstancias() {
        ListaDeServers s1 = new ListaDeServers("localhost", 7001);
        ListaDeServers s2 = new ListaDeServers("localhost", 7002);
        ListaDeServers s3 = new ListaDeServers("localhost", 7003);

        auxiliares = new ArrayList<>();
        auxiliares.add(s1);
        auxiliares.add(s2);
        auxiliares.add(s3);
    }

    public static void addServer(ListaDeServers server) {
        auxiliares.add(server);
    }

    public void iniciarMonitoramento() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(MonitorDeInstancias::verificarHeartbeats, 0, 20, TimeUnit.SECONDS);
    }

    private static void verificarHeartbeats() {
        for (ListaDeServers servidor : auxiliares) {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(3000); // 3 segundos de timeout

                byte[] sendData = "PING".getBytes();
                InetAddress ip = InetAddress.getByName(servidor.getHost());
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, servidor.getPorta());
                socket.send(sendPacket);

                byte[] receiveBuffer = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                socket.receive(receivePacket);
                String resposta = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

                if ("PONG".equals(resposta)) {
                    servidor.setAtivo(true);
                } else {
                    servidor.setAtivo(false);
                    servidor.setLastPing(false);
                }

                if (!servidor.isLastPing() && servidor.isAtivo()) {
                    servidor.setLastPing(true);
                    System.out.println("[Monitor] Reexecutando log para servidor " + servidor.getPorta());
                    ReplayerDeLog.reproduzir(servidor.getPorta(), servidor.getBloco());
                }

            } catch (Exception e) {
                servidor.setAtivo(false);
                servidor.setLastPing(false);
                System.out.println("[Monitor] Falha no servidor: " + servidor.getHost() + ":" + servidor.getPorta());
            }
        }
        System.out.println();
    }

    public static List<ListaDeServers> getServidorAtivo() {
        List<ListaDeServers> ativos = new ArrayList<>();
        for (ListaDeServers servidor : auxiliares) {
            if (servidor.isAtivo()) {
                ativos.add(servidor);
            }
        }
        return ativos;
    }
}
