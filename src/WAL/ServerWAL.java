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
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ServerWAL {
    private static final String LOG_PATH01 = "log_bloco1.txt";
    private static final String LOG_PATH02 = "log_bloco2.txt";
    private static final String LOG_PATH03 = "log_bloco3.txt";

    public static void main(String[] args) throws IOException {
        int PORT = Integer.parseInt(args[0]);
        int BACKLOG = Integer.parseInt(args[1]);

        var serverSocket = new ServerSocket(PORT, BACKLOG);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        while (true) {
            var socket = serverSocket.accept();
            executor.submit(() -> saveLog(socket));
        }
    }

    public static String getBloco(String msg){
        String bloco;
        StringTokenizer tokenizer = new StringTokenizer(msg, ";");
        bloco = tokenizer.nextToken();
        return bloco;
    }

    private static void saveLog(Socket socket) {
        try (socket) {
            var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg = in.readLine();
            String bloco = getBloco(msg);
            String[] partes = msg.split(";", 2);
            String restante = partes[1];

            if (restante.equals("COMMIT")) {
                marcarComoCommit(bloco);
                System.out.println("Log do bloco " + bloco + " marcado como COMMIT.");
                return;
            }

            String logLine = System.currentTimeMillis() + ";" + restante + ";PENDENTE" + System.lineSeparator();

            switch (bloco){
                case "1" -> Files.writeString(Path.of(LOG_PATH01), logLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                case "2" -> Files.writeString(Path.of(LOG_PATH02), logLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                case "3" -> Files.writeString(Path.of(LOG_PATH03), logLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }

            System.out.println("Log salvo no bloco " + bloco + ": " + logLine);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void marcarComoCommit(String bloco) throws IOException {
        Path path = switch (bloco) {
            case "1" -> Path.of(LOG_PATH01);
            case "2" -> Path.of(LOG_PATH02);
            case "3" -> Path.of(LOG_PATH03);
            default -> throw new IllegalArgumentException("Bloco inválido");
        };

        if (!Files.exists(path)) return;

        List<String> linhas = Files.readAllLines(path);
        List<String> atualizadas = linhas.stream()
                .map(linha -> linha.endsWith("PENDENTE") ? linha.replace("PENDENTE", "COMMIT") : linha)
                .collect(Collectors.toList());

        Files.write(path, atualizadas);
    }
}