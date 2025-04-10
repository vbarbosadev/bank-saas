package serversSystem;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class MonitorDeInstancias {
    private static List<ListaDeServers> auxiliares = new ArrayList<>();

    public MonitorDeInstancias(List<ListaDeServers> listaInicial) {
        this.auxiliares.addAll(listaInicial);
    }

    public static void addServer(ListaDeServers server) {
        auxiliares.add(server);
    }

    public void iniciarMonitoramento() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::verificarHeartbeats, 0, 10, TimeUnit.SECONDS);
    }

    private void verificarHeartbeats() {
        for (ListaDeServers servidor : auxiliares) {
            try (Socket socket = new Socket(servidor.host, servidor.porta);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println("PING");
                socket.setSoTimeout(3000); // timeout de 3 segundos
                String resposta = in.readLine();

                if ("PONG".equals(resposta)) {
                    servidor.ativo = true;
                    System.out.println("[Monitor] Servidor ativo: " + servidor.host + ":" + servidor.porta);
                } else {
                    servidor.ativo = false;
                    System.out.println("[Monitor] Servidor inativo: " + servidor.host + ":" + servidor.porta);
                }

            } catch (IOException e) {
                servidor.ativo = false;
                System.out.println("[Monitor] Falha no servidor: " + servidor.host + ":" + servidor.porta);
            }
        }
    }

    public List<ListaDeServers> getServidorAtivo() {
        List<ListaDeServers> ativos = new ArrayList<>();
        for (ListaDeServers servidor : auxiliares) {
            if (servidor.ativo) {
                ativos.add(servidor);
            }
        }
        return ativos;
    }
}
