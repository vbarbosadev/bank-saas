package WAL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;

public class ServerWAL {
    private static final String LOG_PATH = "logs.txt";

    public static void main(String[] args) throws IOException {
        var serverSocket = new ServerSocket(9000);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        while (true) {
            var socket = serverSocket.accept();
            executor.submit(() -> saveLog(socket));
        }
    }

    private static void saveLog(Socket socket) {
        try (socket) {
            var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg = in.readLine();
            Files.writeString(Path.of(LOG_PATH), msg + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("Log salvo: " + msg);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
