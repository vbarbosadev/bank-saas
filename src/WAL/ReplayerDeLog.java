package WAL;

import java.io.*;
import java.net.*;
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
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();

            List<String> linhas = Files.readAllLines(path);
            for (String linha : linhas) {
                String[] partes = linha.split(";");
                if (partes.length < 5) continue;

                String status = partes[4];
                if (!status.equals("PENDENTE")) continue;

                String comando = partes[1];
                String conta = partes[2];
                String valor = partes[3];

                String msg = comando + ";" + conta + ";" + valor;
                enviarRequest(socket, msg);
                i++;
            }

            System.err.println("Reexecução do log do bloco " + bloco + " concluída.");
            WALUtils.marcarTodosComoCommit((bloco));

            enviarRequest(socket, "COMMIT");

        } catch (IOException e) {
            System.err.println("Erro ao ler log: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private static void enviarRequest(DatagramSocket socket, String msg) {
        try {
            InetAddress serverAddress = InetAddress.getByName("localhost");
            byte[] sendData = msg.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, portaServidor);

            socket.send(sendPacket);
            System.out.println("Reenviado: " + msg);

            // Espera pela resposta
            byte[] receiveData = new byte[65535];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            String resposta = new String(receivePacket.getData(), 0, receivePacket.getLength());
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
