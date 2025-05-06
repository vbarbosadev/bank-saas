package serversSystem;

import WAL.ReplayerDeLog;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MonitorDeInstancias {

    private static List<ListaDeServers> auxiliares;
    private ScheduledExecutorService scheduler;

    public MonitorDeInstancias() {
        ListaDeServers s1 = new ListaDeServers("localhost", 7001);
        ListaDeServers s2 = new ListaDeServers("localhost", 7002);
        ListaDeServers s3 = new ListaDeServers("localhost", 7003);

        auxiliares = new ArrayList<>();
        auxiliares.add(s1);
        auxiliares.add(s2);
        auxiliares.add(s3);
    }

    public void addServer(ListaDeServers server) {
        auxiliares.add(server);
    }

    public void iniciarMonitoramento() {
        System.out.println("[Monitor] Iniciando Monitoramento:");
        verificarHeartbeats();
        System.out.println();
    }

    private void verificarHeartbeats() {
        for (ListaDeServers servidor : auxiliares) {
            boolean isServerActive = verificarServidor(servidor);

            if (isServerActive) {
                if (!servidor.isAtivo()) {
                    System.out.println("[Monitor] Servidor voltou: " + servidor.getHost() + ":" + servidor.getPorta());
                }
                servidor.setAtivo(true);

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

            } else {
                if (servidor.isAtivo()) {
                    System.out.println("[Monitor] Servidor caiu: " + servidor.getHost() + ":" + servidor.getPorta());
                }
                servidor.setAtivo(false);
                servidor.setLastPing(false);
                System.out.println("[Monitor] Servidor inativo (sem resposta correta): " + servidor.getHost() + ":" + servidor.getPorta());
            }
        }
    }



    private boolean verificarServidor(ListaDeServers servidor) {
        try (Socket socket = new Socket(servidor.getHost(), servidor.getPorta());
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Enviando requisição HTTP GET
            out.println("GET /PiNG HTTP/1.0");
            out.println("Host: " + servidor.getHost());
            out.println(); // Linha em branco para sinalizar o fim dos headers

            // Lendo a primeira linha da resposta (status)
            String statusLine = in.readLine();
            if (statusLine == null || !statusLine.contains("200")) {
                return false; // Se não for 200 OK, o servidor não respondeu corretamente
            }

            // Lê e ignora os headers da resposta
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                // Ignora headers
            }

            // Lê o corpo da resposta, se necessário
            StringBuilder responseBody = new StringBuilder();
            while ((line = in.readLine()) != null) {
                responseBody.append(line);
            }

            // Verificando se o corpo da resposta é esperado (opcional, dependendo do seu caso)
            System.out.println("[Monitor] Resposta do servidor: " + responseBody.toString());

            return true;

        } catch (IOException e) {
            System.err.println("[Monitor] Erro ao verificar servidor: " + e.getMessage());
            return false;
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
