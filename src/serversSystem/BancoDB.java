package serversSystem;

import objetos.Banco;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;

public class BancoDB {

    private static Banco bancoDB;

    public static void main(String[] args) throws IOException {
        bancoDB = carregarBanco();

        int PORT = Integer.parseInt(args[0]);
        DatagramSocket socket = new DatagramSocket(PORT);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        // Hook para salvar ao encerrar a aplicação
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            //salvarBanco();
            System.out.println("Banco salvo antes de encerrar.");
        }));

        System.out.println("Servidor BancoDB ativo na porta " + PORT + "...");
        bancoDB.imprimirContas();

        while (true) {
            byte[] buffer = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            executor.submit(() -> start(socket, packet));
            bancoDB.imprimirContas();
        }
    }

    public static int getPort(DatagramPacket packet) {
        int port = packet.getPort();
        int lastDigit = port % 10;
        return lastDigit;
    }

    public static void start(DatagramSocket socket, DatagramPacket packet) {
        try {
            // Reconstruir objeto Banco a partir dos bytes recebidos
            ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
            ObjectInputStream in = new ObjectInputStream(bais);
            Banco bancoRecebido = (Banco) in.readObject();

            mesclarBancos(bancoRecebido);
            salvarBanco();
            System.out.println("Banco mesclado e salvo com sucesso.");

            int bloco = getPort(packet);
            String logPath = switch (bloco) {
                case 1 -> "log_bloco1.txt";
                case 2 -> "log_bloco2.txt";
                case 3 -> "log_bloco3.txt";
                default -> null;
            };

            System.out.println("iniciando checagem");

            if (logPath != null) {
                Path logFile = Path.of(logPath);
                if (Files.exists(logFile)) {
                    List<String> linhas = Files.readAllLines(logFile);
                    List<String> pendentes = linhas.stream()
                            .filter(linha -> !linha.trim().endsWith(";COMMIT"))
                            .toList();

                    if (pendentes.isEmpty()) {
                        Files.delete(logFile);
                        System.out.println("Log totalmente processado e deletado.");
                    } else {
                        Files.write(logFile, pendentes);
                        System.out.println("Log atualizado com apenas operações pendentes.");
                    }
                }
            }

            // Serializar o objeto Banco atualizado para resposta
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(bancoDB);
            out.flush();

            byte[] resposta = baos.toByteArray();
            DatagramPacket respostaPacket = new DatagramPacket(
                    resposta,
                    resposta.length,
                    packet.getAddress(),
                    packet.getPort()
            );
            socket.send(respostaPacket);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao processar requisição UDP: " + e.getMessage());
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
