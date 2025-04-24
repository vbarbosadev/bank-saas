package serversSystem;

import objetos.Banco;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class BancoDB {

    private static Banco bancoDB;

    public static void main(String[] args) throws IOException {
        bancoDB = carregarBanco();

        int PORT = Integer.parseInt(args[0]);
        int BACKLOG = Integer.parseInt(args[1]);

        // Hook para salvar ao encerrar a aplicação (Ctrl+C, kill, etc)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            salvarBanco();
            System.out.println("Banco salvo antes de encerrar.");
        }));

        ServerSocket mysocket = new ServerSocket(PORT, BACKLOG);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        System.out.println("Servidor BancoDB ativo na porta 7040...");
        bancoDB.imprimirContas();

        while (true) {
            Socket client = mysocket.accept();
            executor.submit(() -> start(client));
            bancoDB.imprimirContas();
        }
    }

    public static int getPort(Socket socket){
        int port = socket.getPort();
        int lastDigit = port % 10;
        return lastDigit;
    }

    public static void start(Socket s) {
        try (s; ObjectInputStream in = new ObjectInputStream(s.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {

            Banco bancoRecebido = (Banco) in.readObject();
            mesclarBancos(bancoRecebido);
            salvarBanco(); // salva após mesclagem
            if (bancoRecebido.getContas().size() > 0) {
                System.out.println("Contas recebidas!");
            }
            System.out.println("Banco mesclado e salvo com sucesso.");

            int bloco = getPort(s);
            String logPath = switch (bloco) {
                case 1 -> "log_bloco1.txt";
                case 2 -> "log_bloco2.txt";
                case 3 -> "log_bloco3.txt";
                default -> null;
            };

            System.out.println("[LOG] iniciando checagem");

            if (logPath != null) {
                Path logFile = Path.of(logPath);
                if (Files.exists(logFile)) {
                    List<String> linhas = Files.readAllLines(logFile);
                    List<String> pendentes = linhas.stream()
                            .filter(linha -> !linha.trim().endsWith(";COMMIT"))
                            .toList();

                    if (pendentes.isEmpty()) {
                        Files.delete(logFile); // deleta se não restar mais nada
                        System.out.println("[LOG] Log totalmente processado e deletado.");
                    } else {
                        Files.write(logFile, pendentes); // sobrescreve só com pendentes
                        System.out.println("[LOG] Log atualizado com apenas operações pendentes.");
                    }
                } else {
                    System.out.println("[LOG] Arquivo nao encontrado.");

                }
            }

            out.writeObject(bancoDB);
            out.flush();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[LOG] Erro ao receber ou salvar dados do banco: " + e.getMessage());
        }
    }


    private static synchronized void mesclarBancos(Banco recebido) {
        HashMap<Integer, Object> contasRecebidas = recebido.getContas();

        for (Map.Entry<Integer, Object> entry : contasRecebidas.entrySet()) {
            Integer idConta = entry.getKey();
            Map<String, Integer> dadosRecebidos = (Map<String, Integer>) entry.getValue();

            Map<String, Integer> dadosAtuais = (Map<String, Integer>) bancoDB.getContas().get(idConta);

            if (dadosAtuais != null) {
                String nome = dadosRecebidos.keySet().iterator().next();
                Integer saldo = dadosRecebidos.get(nome);
                dadosAtuais.put(nome, saldo);
            } else {
                bancoDB.getContas().put(idConta, dadosRecebidos);
            }
        }
    }

    private static void salvarBanco() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("bancoDB.dat"))) {
            out.writeObject(bancoDB);
        } catch (IOException e) {
            System.err.println("[LOG] Erro ao salvar banco em arquivo: " + e.getMessage());
        }
    }

    private static Banco carregarBanco() {
        File arquivo = new File("bancoDB.dat");
        if (!arquivo.exists()) {
            System.out.println("[LOG] Nenhum banco salvo encontrado. Criando novo...");
            return new Banco();
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(arquivo))) {
            System.out.println("[LOG] Banco carregado com sucesso a partir do arquivo.");
            return (Banco) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[LOG] Erro ao carregar banco salvo: " + e.getMessage());
            return new Banco();
        }
    }
}
