package WAL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ServerWAL {

    private static final String LOG_PATH01 = "log_bloco1.txt";
    private static final String LOG_PATH02 = "log_bloco2.txt";
    private static final String LOG_PATH03 = "log_bloco3.txt";

    public static void main(String[] args) throws IOException {

        int PORT = Integer.parseInt(args[0]);
        int BACKLOG = Integer.parseInt(args[1]);

        try (var serverSocket = new ServerSocket(PORT, BACKLOG)) {
            var executor = Executors.newVirtualThreadPerTaskExecutor();

            while (true) {
                var socket = serverSocket.accept();
                executor.submit(() -> saveLog(socket));
            }
        } catch (IOException e) {
            System.err.println("[Erro] Falha ao iniciar o servidor: " + e.getMessage());
        }
    }

    private static void saveLog(Socket socket) {
        try (socket) {
            var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg = in.readLine();

            String[] partes = msg.split(";", 2);
            String bloco = partes[0];
            String request = partes[1];

            if (getComando(request).equals("saldo")) {
                return; // Se o comando for 'saldo', não faz nada
            }

            if (request.equals("COMMIT")) {
                marcarComoCommit(bloco);
                System.out.println("Log do bloco " + bloco + " marcado como COMMIT.");
                return;
            }

            String logLine = System.currentTimeMillis() + ";" + request + ";PENDENTE" + System.lineSeparator();
            gravarLogNoBloco(bloco, logLine);
            System.out.println("Log salvo no bloco " + bloco + ": " + logLine);

        } catch (IOException e) {
            System.err.println("[Erro] Erro ao processar o log: " + e.getMessage());
        }
    }

    private static void gravarLogNoBloco(String bloco, String logLine) throws IOException {
        Path path = getPathDoBloco(bloco);
        if (path != null) {
            Files.writeString(path, logLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    private static Path getPathDoBloco(String bloco) {
        switch (bloco) {
            case "1":
                return Path.of(LOG_PATH01);
            case "2":
                return Path.of(LOG_PATH02);
            case "3":
                return Path.of(LOG_PATH03);
            default:
                System.err.println("[Erro] Bloco inválido: " + bloco);
                return null;
        }
    }

    private static void marcarComoCommit(String bloco) throws IOException {
        Path path = getPathDoBloco(bloco);
        if (path == null || !Files.exists(path)) {
            return;
        }

        List<String> linhas = Files.readAllLines(path);
        List<String> atualizadas = linhas.stream()
                .map(linha -> linha.endsWith("PENDENTE") ? linha.replace("PENDENTE", "COMMIT") : linha)
                .collect(Collectors.toList());

        Files.write(path, atualizadas);
        System.out.println("Log do bloco " + bloco + " atualizado para COMMIT.");
    }

    private static String getComando(String msg) {
        StringTokenizer tokenizer = new StringTokenizer(msg, ";");
        return tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";
    }
}
