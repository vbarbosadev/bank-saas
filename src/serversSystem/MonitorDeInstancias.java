package serversSystem;

import WAL.ReplayerDeLog;

import java.io.*;
import java.net.Socket;
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
            try (Socket socket = new Socket(servidor.getHost(), servidor.getPorta());
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {



                out.println("PING");
                socket.setSoTimeout(3000); // timeout de 3 segundos
                String resposta = in.readLine();

                if ("PONG".equals(resposta)) {
                    servidor.setAtivo(true);
                    //System.out.println("[Monitor] Servidor ativo: " + servidor.getHost() + ":" + servidor.getPorta());
                } else {
                    servidor.setAtivo(false);
                    servidor.setLastPing(false);
                    //System.out.println("[Monitor] Servidor inativo: " + servidor.getHost() + ":" + servidor.getPorta());
                }


                if (!servidor.isLastPing() & servidor.isAtivo()) {
                    servidor.setLastPing(true);
                    ReplayerDeLog.reproduzir(servidor.getPorta(), servidor.getBloco());
                }


            } catch (IOException e) {
                servidor.setAtivo(false);
                servidor.setLastPing(false);
                System.out.println("[Monitor] Falha no servidor: " + servidor.getHost() + ":" + servidor.getPorta());
            }
            System.out.println();
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
