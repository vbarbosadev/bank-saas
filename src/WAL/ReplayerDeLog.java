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

        String caminhoLog = getCaminhoLog();
        if (caminhoLog == null) {
            System.err.println("[Erro] Bloco inválido: " + bloco);
            return;
        }

        Path path = Path.of(caminhoLog);
        if (!Files.exists(path)) {
            System.out.println("[Aviso] Log não encontrado: " + caminhoLog);
            return;
        }

        int numeroDeOperacoes = 0;
        try {
            List<String> linhas = Files.readAllLines(path);
            enviarRequestHttp("ReplayerLog");

            for (String linha : linhas) {
                String[] partes = linha.split(";");
                if (partes.length < 5) continue;

                String status = partes[4];
                if (!"PENDENTE".equals(status)) continue;

                String comando = partes[1];
                String conta = partes[2];
                String valor = partes[3];

                String msg = comando + ";" + conta + ";" + valor;
                enviarRequestHttp(msg);
                numeroDeOperacoes++;
            }

            System.err.println("[Info] Reexecução do log do bloco " + bloco + " concluída.");
            WALUtils.marcarTodosComoCommit(bloco);

            enviarRequestHttp("COMMIT");

        } catch (IOException e) {
            System.err.println("[Erro] Falha ao ler o log: " + e.getMessage());
        }

        if (numeroDeOperacoes == 0) {
            System.out.println("[Aviso] Nenhuma operação PENDENTE encontrada no log.");
        }
    }

    private static void enviarRequestHttp(String msg) {
        try (Socket socket = new Socket("localhost", portaServidor);
             OutputStream out = socket.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Criando uma requisição HTTP POST simples
            String httpRequest =
                    "POST /operacao HTTP/1.1\r\n" +
                            "Host: localhost\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: " + msg.length() + "\r\n" +
                            "\r\n" +
                            msg;

            out.write(httpRequest.getBytes());
            out.flush();

            // Lendo a resposta HTTP
            String linha;
            StringBuilder resposta = new StringBuilder();
            while ((linha = in.readLine()) != null && !linha.isEmpty()) {
                resposta.append(linha).append("\n");
            }

            System.out.println("[Info] Reenviado via HTTP: " + msg);
            System.out.println("[Info] Resposta HTTP: \n" + resposta);

        } catch (IOException e) {
            System.err.println("[Erro] Falha ao reenviar operação via HTTP para servidor na porta " + portaServidor + ": " + e.getMessage());
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
