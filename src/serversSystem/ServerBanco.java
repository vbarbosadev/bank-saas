package serversSystem;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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

    private static Set<Integer> portasEmRecuperacao = ConcurrentHashMap.newKeySet();
    private static Lock lock = new ReentrantLock();
    private static Condition podeEnviar = lock.newCondition();

    public static void main(String[] args) throws IOException {
        System.out.println("Iniciando Servidor...\n");

        int PORT = Integer.parseInt(args[0]);
        int BACKLOG = Integer.parseInt(args[1]);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(ServerBanco::monitoramento, 0, 20, TimeUnit.SECONDS);

        ServerSocket serverSocket = new ServerSocket(PORT, BACKLOG);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            executor.submit(() -> handleRequest(clientSocket));
            qtdClients.incrementAndGet();
            System.out.println(qtdClients.get() + " clientes");
        }
    }

    private static void handleRequest(Socket socket) {
        try (socket;
             BufferedReader request = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter response = new PrintWriter(socket.getOutputStream(), true)) {

            String requestLine = request.readLine();
            System.out.println("Requisição recebida: " + requestLine);

            if (requestLine == null || !requestLine.startsWith("GET")) {
                enviarRespostaHttp(response, "400 Bad Request", "Formato de requisição inválido");
                return;
            }


            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                enviarRespostaHttp(response, "400 Bad Request", "Requisição malformada");
                return;
            }

            String msg = parts[1].substring(1); // remove o '/'

            // Descarta cabeçalhos restantes
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
                enviarRespostaHttp(response, "503 Service Unavailable", errMsg);
                System.out.println("Servidor inativo na porta " + portaDestino + ", operação não realizada.");
                return;
            }

            aguardarSeRecuperando(portaDestino);

            try (Socket auxSocket = new Socket("localhost", portaDestino);
                 BufferedReader auxIn = new BufferedReader(new InputStreamReader(auxSocket.getInputStream()));
                 PrintWriter auxOut = new PrintWriter(auxSocket.getOutputStream(), true)) {

                auxOut.println("GET /" + msg + " HTTP/1.0");
                auxOut.println("Host: localhost");
                auxOut.println();

                String statusLine = auxIn.readLine();
                if (statusLine == null || !statusLine.contains("200")) {
                    enviarRespostaHttp(response, "502 Bad Gateway", "Erro na resposta do servidor backend");
                    return;
                }

                String line;
                while ((line = auxIn.readLine()) != null && !line.isEmpty()) {
                }

                StringBuilder responseBody = new StringBuilder();
                String contentLine;
                while ((contentLine = auxIn.readLine()) != null) {
                    responseBody.append(contentLine);
                }

                enviarRespostaHttp(response, "200 OK", responseBody.toString());

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Falha ao conectar ao servidor na porta " + portaDestino);
                enviarRespostaHttp(response, "500 Internal Server Error", "Erro: Falha ao conectar ao servidor");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            qtdClients.decrementAndGet();
        }
    }

    private static void enviarRespostaHttp(PrintWriter out, String status, String body) {
        out.print("HTTP/1.0 " + status + "\r\n");
        out.print("Content-Type: text/plain\r\n");
        out.print("Content-Length: " + body.length() + "\r\n");
        out.print("\r\n");
        out.print(body);
        out.flush();
    }

    private static void monitoramento() {
        monitor.iniciarMonitoramento();
    }

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
