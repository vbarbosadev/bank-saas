package WAL;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ReplayerDeLog {

    private static int portaServidor = 0;
    private static int bloco = 0;

    private static final String LOG_PATH01 = "log_bloco1.txt";
    private static final String LOG_PATH02 = "log_bloco2.txt";
    private static final String LOG_PATH03 = "log_bloco3.txt";


    public static void reproduzir(int porta, int b) {
        bloco = b;
        portaServidor = porta;
        String caminho = getCaminhoLog();
        if (caminho == null) {
            System.err.println("Bloco inválido: " + bloco);
            return;
        }

        Path path = Path.of(caminho);
        if (!Files.exists(path)) {
            System.out.println("Log não encontrado: " + caminho);
            return;
        }

        int i = 0;
        try {
            List<String> linhas = Files.readAllLines(path);
            for (String msg : linhas) {
                enviarRequest(msg);
                i++;
                System.out.println(i);
            }

            enviarRequest("UPDATE");

            System.out.println("Reexecução do log do bloco " + bloco + " concluída.");
        } catch (IOException e) {
            System.err.println("Erro ao ler log: " + e.getMessage());
        }
    }

    private static void enviarRequest(String msg) {
        try (Socket socket = new Socket("localhost", portaServidor);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(msg);
            String resposta = in.readLine();
            System.out.println("Reenviado: " + msg);
            System.out.println("Resposta: " + resposta);
        } catch (IOException e) {
            System.err.println("Erro ao reenviar operação para servidor na porta " + portaServidor + ": " + e.getMessage());
        }

    }

    private static String getCaminhoLog() {
        return switch (bloco) {
            case 1 -> LOG_PATH01;
            case 2 -> LOG_PATH02;
            case 3 -> LOG_PATH03;
            default -> null;
        };
    }
}
