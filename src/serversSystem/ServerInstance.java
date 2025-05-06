package serversSystem;

import WAL.WALUtils;
import objetos.Banco;
import objetos.ProcessadorBancario;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerInstance {

    public static AtomicInteger qtdClients = new AtomicInteger(0);
    public static final Banco banco = new Banco();
    public static final ProcessadorBancario process = new ProcessadorBancario(banco);
    public static final ReentrantReadWriteLock bancoLock = new ReentrantReadWriteLock();
    public static int PORT = 0;
    public static boolean replayer = false;

    public static void main(String[] args) {
        long inicio = System.nanoTime();
        getBanco();
        banco.imprimirContas();

        PORT = Integer.parseInt(args[0]);
        int BACKLOG = Integer.parseInt(args[1]);
        banco.setBloco(PORT % 10);

        System.out.println("[BLOCO] " + banco.getBloco());
        System.out.println("Servidor auxiliar iniciado na porta: " + PORT + " com BACKLOG: " + BACKLOG);
        System.out.println();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(ServerInstance::enviarBancoParaAuxiliar, 30, 30, TimeUnit.SECONDS);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             ServerSocket server = new ServerSocket(PORT, BACKLOG)) {

            while (true) {
                Socket LeaderSocket = server.accept();
                executor.execute(() -> handleClient(LeaderSocket));
                qtdClients.incrementAndGet();
                System.out.println(qtdClients.get() + " clientes conectados.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        long fim = System.nanoTime();
        System.out.printf("Tempo total: %.3f ms%n", (fim - inicio) / 1_000_000.0);
    }

    private static void handleClient(Socket LeaderSocket) {
        try (LeaderSocket;
             BufferedReader input = new BufferedReader(new InputStreamReader(LeaderSocket.getInputStream()));
             PrintWriter output = new PrintWriter(LeaderSocket.getOutputStream())) {

            String msg = input.readLine();
            if (msg == null || !msg.startsWith("GET")) {
                enviarRespostaHttp(output, "400 Bad Request", "Formato de requisição inválido");
                return;
            }

            String[] parts = msg.split(" ");
            if (parts.length < 2) {
                enviarRespostaHttp(output, "400 Bad Request", "Requisição malformada");
                return;
            }

            String comando = parts[1].substring(1);

            if ("PING".equalsIgnoreCase(comando)) {
                enviarRespostaHttp(output, "200 OK", "PONG");
                return;
            }

            if ("COMMIT".equalsIgnoreCase(comando)) {
                enviarBancoParaAuxiliar();
                replayer = false;
                enviarRespostaHttp(output, "200 OK", "Commit realizado com sucesso");
                return;
            }

            if ("ReplayerLog".equalsIgnoreCase(comando)) {
                replayer = true;
                enviarRespostaHttp(output, "200 OK", "Modo Replayer ativado");
                return;
            }

            System.out.println("Replayer Log: " + replayer);
            System.out.println("[REQ] Operação recebida de " + LeaderSocket.getInetAddress() + ": " + comando);

            String reply;
            bancoLock.readLock().lock();
            try {
                reply = process.processar(comando);
                if (validacaoResp(reply) && !replayer) {
                    try (Socket logSocket = new Socket("localhost", 9000);
                         PrintWriter logOut = new PrintWriter(logSocket.getOutputStream(), true)) {
                        logOut.println(banco.getBloco() + ";" + comando);
                        System.out.println("[LOG] Requisição " + comando + " registrada no WAL.");
                    }
                    System.out.println("[LOG] Requisição processada com sucesso!");
                }
            } catch (IOException e) {
                System.err.println("Erro ao enviar para o WAL: " + e.getMessage());
                reply = "Erro: falha ao registrar no log";
            } finally {
                bancoLock.readLock().unlock();
            }

            enviarRespostaHttp(output, "200 OK", reply);

        } catch (IOException e) {
            System.err.println("Erro ao lidar com cliente: " + e.getMessage());
        } finally {
            qtdClients.decrementAndGet();
        }
    }

    private static void enviarBancoParaAuxiliar() {
        bancoLock.writeLock().lock();
        try {
            try (Socket socket = new Socket("localhost", 8000);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 InputStream in = socket.getInputStream()) {

                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
                objOut.writeObject(banco);
                objOut.flush();
                byte[] bancoBytes = byteOut.toByteArray();

                out.println("POST / HTTP/1.0");
                out.println("Host: localhost");
                out.println("Content-Type: application/octet-stream");
                out.println("Content-Length: " + bancoBytes.length);
                out.println();
                out.flush();

                socket.getOutputStream().write(bancoBytes);
                socket.getOutputStream().flush();

                BufferedReader headerReader = new BufferedReader(new InputStreamReader(in));
                String statusLine = headerReader.readLine();
                if (statusLine == null || !statusLine.contains("200")) {
                    System.err.println("[LOG] Erro ao enviar banco para o auxiliar: " + statusLine);
                    return;
                }

                String line;
                while ((line = headerReader.readLine()) != null && !line.isEmpty()) {
                    // Ignora cabeçalhos
                }

                ByteArrayOutputStream byteResponseOut = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    byteResponseOut.write(buffer, 0, bytesRead);
                }

                ByteArrayInputStream byteIn = new ByteArrayInputStream(byteResponseOut.toByteArray());
                ObjectInputStream objIn = new ObjectInputStream(byteIn);
                Banco bancoNovo = (Banco) objIn.readObject();

                mesclarBancos(bancoNovo);
                process.setBanco(banco);

                Thread.sleep(100);

            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                System.err.println("[LOG] Falha ao enviar banco para o auxiliar: " + e.getMessage());
            }
        } finally {
            bancoLock.writeLock().unlock();
        }
    }

    private static void mesclarBancos(Banco recebido) {
        HashMap<Integer, Object> contasRecebidas = recebido.getContas();

        for (Map.Entry<Integer, Object> entry : contasRecebidas.entrySet()) {
            Integer idConta = entry.getKey();
            Map<String, Integer> dadosRecebidos = (Map<String, Integer>) entry.getValue();

            Map<String, Integer> dadosAtuais = (Map<String, Integer>) banco.getContas().get(idConta);
            if (dadosAtuais != null) {
                String nome = dadosRecebidos.keySet().iterator().next();
                Integer saldo = dadosRecebidos.get(nome);
                dadosAtuais.put(nome, saldo);
            } else {
                banco.getContas().put(idConta, dadosRecebidos);
            }
        }
    }

    private static boolean validacaoResp(String resp) {
        String[] partes = resp.split(":", 2);
        String erro = partes[0];
        return !erro.equals("Erro de transação") && !erro.equals("Erro");
    }

    private static void getBanco() {
        bancoLock.writeLock().lock();
        try {
            try (Socket socket = new Socket("localhost", 8000);
                 OutputStream out = socket.getOutputStream();
                 InputStream in = socket.getInputStream()) {

                String request = "GET / HTTP/1.0\r\n" +
                        "Host: localhost\r\n" +
                        "\r\n";
                out.write(request.getBytes());
                out.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String statusLine = reader.readLine();
                if (statusLine == null || !statusLine.contains("200")) {
                    System.err.println("[LOG] Erro ao obter banco do servidor: " + statusLine);
                    return;
                }

                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    // Ignora cabeçalhos
                }

                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    byteOut.write(buffer, 0, bytesRead);
                }

                ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
                ObjectInputStream objIn = new ObjectInputStream(byteIn);
                Banco bancoNovo = (Banco) objIn.readObject();
                System.out.println("[LOG] Banco recebido: " + bancoNovo);

                mesclarBancos(bancoNovo);
                process.setBanco(banco);
                Thread.sleep(500);

            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                System.err.println("[LOG] Falha ao obter banco do servidor: " + e.getMessage());
            }
        } finally {
            bancoLock.writeLock().unlock();
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
}
