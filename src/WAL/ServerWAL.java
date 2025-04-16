package WAL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;

public class ServerWAL {
    private static final String LOG_PATH01 = "log_bloco1.txt";
    private static final String LOG_PATH02 = "log_bloco2.txt";
    private static final String LOG_PATH03 = "log_bloco3.txt";



    public static void main(String[] args) throws IOException {
        var serverSocket = new ServerSocket(9000, 300);
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
            String[] partes = msg.split(";", 2); // divide em 2 partes
            String restante = partes[1];
            System.out.println(restante); // saída: cmd;num;val



            switch (bloco){
                case "1":
                    Files.writeString(Path.of(LOG_PATH01), restante + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    System.out.println("Log salvo no bloco 1: " + restante);
                    break;
                case "2":
                    Files.writeString(Path.of(LOG_PATH02), restante + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    System.out.println("Log salvo no bloco 2: " + restante);
                    break;
                case "3":
                    Files.writeString(Path.of(LOG_PATH03), restante + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    System.out.println("Log salvo no bloco 3: " + restante);
                    break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
