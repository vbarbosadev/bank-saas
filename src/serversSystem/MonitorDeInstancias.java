package serversSystem;

import WAL.ReplayerDeLog;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

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
        System.out.println("[Monitor] Iniciando Monitoramento:");
        verificarHeartbeats();
        System.out.println();
    }

    private static void verificarHeartbeats() {
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket();

            for (ListaDeServers servidor : auxiliares) {
                try {
                    InetAddress serverAddress = InetAddress.getByName(servidor.getHost());
                    String msg = "PING";
                    byte[] sendData = msg.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, servidor.getPorta());

                    // Log para verificar se o pacote de ping está sendo enviado
                    System.out.println("[Monitor] Enviando PING para " + servidor.getHost() + ":" + servidor.getPorta());

                    socket.send(sendPacket);
                    socket.setSoTimeout(3000); // timeout de 3 segundos

                    // Recebendo a resposta
                    byte[] receiveData = new byte[65535];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                    // Adicionando tratamento para o timeout
                    try {
                        socket.receive(receivePacket);
                        System.out.println("[Monitor] Resposta recebida de " + servidor.getHost() + ":" + servidor.getPorta());
                    } catch (SocketTimeoutException e) {
                        // Se o servidor não responder dentro do tempo limite
                        System.out.println("[Monitor] Timeout ao aguardar resposta do servidor: " + servidor.getHost() + ":" + servidor.getPorta());
                        servidor.setAtivo(false);
                        servidor.setLastPing(false);
                        return; // Sai da execução se o servidor não responder
                    }

                    String resposta = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println("[Monitor] Resposta do servidor: " + resposta);

                    if ("PONG".equals(resposta)) {
                        if (!servidor.isAtivo()) {
                            System.out.println("[Monitor] Servidor voltou: " + servidor.getHost() + ":" + servidor.getPorta());
                        }
                        servidor.setAtivo(true);
                    } else {
                        servidor.setAtivo(false);
                        servidor.setLastPing(false);
                        System.out.println("[Monitor] Servidor inativo (sem resposta correta): " + servidor.getHost() + ":" + servidor.getPorta());
                    }

                    if (!servidor.isLastPing() && servidor.isAtivo()) {
                        servidor.setLastPing(true);

                        // Pausa envios enquanto recupera
                        ServerBanco.pausarEnviosPara(servidor.getPorta());

                        try {
                            System.err.println("[Monitor] Recuperando logs para porta: " + servidor.getPorta());
                            ReplayerDeLog.reproduzir(servidor.getPorta(), servidor.getBloco());
                        } catch (Exception e) {
                            System.err.println("[Monitor] Erro ao replicar log para porta " + servidor.getPorta() + ": " + e.getMessage());
                        } finally {
                            // Libera envios após recuperar
                            ServerBanco.liberarEnviosPara(servidor.getPorta());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[Monitor] Erro geral ao tentar enviar ping: " + e.getMessage());
                }

            }

        } catch (SocketException e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
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
