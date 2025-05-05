package serversSystem;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerBanco implements Serializable {

    public static AtomicInteger qtdClients = new AtomicInteger(0);
    private static MonitorDeInstancias monitor = new MonitorDeInstancias();
    private static List<ListaDeServers> ativos = new ArrayList<>();

    // Controle de portas em recuperação
    private static Set<Integer> portasEmRecuperacao = ConcurrentHashMap.newKeySet();
    private static Lock lock = new ReentrantLock();
    private static Condition podeEnviar = lock.newCondition();

    public static void main(String[] args) throws IOException {
        System.out.println("Iniciando Servidor...\n");

        int PORT = Integer.parseInt(args[0]);
        int BACKLOG = Integer.parseInt(args[1]);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> monitoramento(), 0, 20, TimeUnit.SECONDS);

        var serverSocket = new ServerSocket(PORT, BACKLOG);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        while (true) {
            var clientSocket = serverSocket.accept();
            executor.submit(() -> handleRequest(clientSocket));
            qtdClients.incrementAndGet();
            System.out.println(qtdClients.get() + " clientes");
        }
    }

    private static void handleRequest(Socket socket) {
        try (socket;
             BufferedReader request = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter response = new PrintWriter(socket.getOutputStream(), true)) {

            // Lê a linha de requisição HTTP: "GET /comando;conta;valor HTTP/1.0"
            String requestLine = request.readLine();
            if (requestLine == null || !requestLine.startsWith("GET")) {
                response.println("HTTP/1.0 400 Bad Request");
                response.println();
                return;
            }

            String msg = requestLine.split(" ")[1].substring(1); // remove o '/'
            // descarta cabeçalhos restantes
            while (request.ready()) request.readLine();

            StringTokenizer tokenizer = new StringTokenizer(msg, ";");
            tokenizer.nextToken();
            int accNum = Integer.parseInt(tokenizer.nextToken());
            int lastDigit = accNum % 10;

            int portaDestino;

            if (lastDigit <= 2) {
                portaDestino = 7001;
            } else if (lastDigit <= 5) {
                portaDestino = 7002;
            } else {
                portaDestino = 7003;
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
                String errMsg = "Erro: Servidor indisponível na porta " + portaDestino;
                enviarRespostaHttp(response, errMsg);
                System.out.println("Servidor inativo na porta " + portaDestino + ", operação não realizada.");
                return;
            }

            // Aguarda se a porta está em recuperação do log
            aguardarSeRecuperando(portaDestino);

            try (Socket auxSocket = new Socket("localhost", portaDestino)) {
                var auxOut = new PrintWriter(auxSocket.getOutputStream(), true);
                var auxIn = new BufferedReader(new InputStreamReader(auxSocket.getInputStream()));

                auxOut.println(msg);
                String respBanco = auxIn.readLine();

                enviarRespostaHttp(response, respBanco);

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Falha ao conectar ao servidor na porta " + portaDestino);
                enviarRespostaHttp(response, "Erro: Falha ao conectar ao servidor");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            qtdClients.decrementAndGet();
        }
    }

    private static void enviarRespostaHttp(PrintWriter response, String body) {
        response.println("HTTP/1.0 200 OK");
        response.println("Content-Type: text/plain");
        response.println("Content-Length: " + body.length());
        response.println();
        response.println(body);
    }

    private static void monitoramento() {
        monitor.iniciarMonitoramento();
    }

    // Métodos de controle de recuperação
    public static void pausarEnviosPara(int porta) {
        lock.lock();
        try {
            portasEmRecuperacao.add(porta);
            System.out.println("[ServerBanco] Porta " + porta + " está em recuperação. Aguardando liberação para enviar.");
        } finally {
            lock.unlock();
        }
    }

    public static void liberarEnviosPara(int porta) {
        lock.lock();
        try {
            portasEmRecuperacao.remove(porta);
            System.out.println("[ServerBanco] Porta " + porta + " liberada. Reenvios permitidos.");
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
