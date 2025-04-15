package serversSystem;

import objetos.Banco;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class BancoDB {

    private static Banco bancoDB;

    public static void main(String[] args) throws IOException {
        bancoDB = carregarBanco();

        // Hook para salvar ao encerrar a aplicação (Ctrl+C, kill, etc)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            salvarBanco();
            System.out.println("Banco salvo antes de encerrar.");
        }));

        ServerSocket mysocket = new ServerSocket(7040);
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
            System.out.println("Banco mesclado e salvo com sucesso.");
            // ******
            switch (getPort(s)){
                case 1:
                    Files.deleteIfExists(Path.of("log_bloco1.txt"));
                    break;
                case 2:
                    Files.deleteIfExists(Path.of("log_bloco2.txt"));
                    break;
                case 3:
                    Files.deleteIfExists(Path.of("log_bloco3.txt"));
                    break;
            }
            System.out.println("Logs deletados com sucesso.");
            out.writeObject(bancoDB);
            out.flush();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao receber ou salvar dados do banco: " + e.getMessage());
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
            System.err.println("Erro ao salvar banco em arquivo: " + e.getMessage());
        }
    }

    private static Banco carregarBanco() {
        File arquivo = new File("bancoDB.dat");
        if (!arquivo.exists()) {
            System.out.println("Nenhum banco salvo encontrado. Criando novo...");
            return new Banco();
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(arquivo))) {
            System.out.println("Banco carregado com sucesso a partir do arquivo.");
            return (Banco) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao carregar banco salvo: " + e.getMessage());
            return new Banco();
        }
    }
}
