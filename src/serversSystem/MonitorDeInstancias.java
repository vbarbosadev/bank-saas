package serversSystem;

import WAL.ReplayerDeLog;
import java.io.*;
import java.net.Socket;
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
        for (ListaDeServers servidor : auxiliares) {
            try (Socket socket = new Socket(servidor.getHost(), servidor.getPorta());
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println("PING");
                socket.setSoTimeout(3000); // timeout de 3 segundos
                String resposta = in.readLine();

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

            } catch (IOException e) {
                if (servidor.isAtivo()) {
                    System.out.println("[Monitor] Servidor caiu: " + servidor.getHost() + ":" + servidor.getPorta());
                }
                servidor.setAtivo(false);
                servidor.setLastPing(false);
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
