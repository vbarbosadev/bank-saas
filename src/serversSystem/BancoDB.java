package serversSystem;

import WAL.WALUtils;
import objetos.Banco;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BancoDB {

    private static Banco bancoDB;
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public static void main(String[] args) throws IOException {
        bancoDB = carregarBanco();

        int PORT = Integer.parseInt(args[0]);
        DatagramSocket socket = new DatagramSocket(PORT);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        System.out.println("Servidor BancoDB (UDP) ativo na porta " + PORT);
        bancoDB.imprimirContas();

        byte[] buffer = new byte[65507]; // maior tamanho possível em UDP

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            executor.submit(() -> start(packet, socket));
        }
    }

    public static void start(DatagramPacket packet, DatagramSocket socket) {
        try {
            byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream in = new ObjectInputStream(bais);

            Banco bancoRecebido = (Banco) in.readObject();
            in.close();

            lock.writeLock().lock();
            try {
                mesclarBancos(bancoRecebido);
                salvarBanco();
            } finally {
                lock.writeLock().unlock();
            }

            if (bancoRecebido.getContas().size() > 0) {
                System.out.println("Contas recebidas!");
            }
            System.out.println("Banco mesclado e salvo com sucesso.");

            int bloco = bancoRecebido.getBloco();
            String logPath = switch (bloco) {
                case 1 -> "log_bloco1.txt";
                case 2 -> "log_bloco2.txt";
                case 3 -> "log_bloco3.txt";
                default -> null;
            };

            System.out.println("[BLOCO] " + bloco);
            WALUtils.marcarTodosComoCommit(bloco);

            System.out.println("[LOG] iniciando checagem");

            if (logPath != null) {
                Path logFile = Path.of(logPath);
                if (Files.exists(logFile)) {
                    List<String> linhas = Files.readAllLines(logFile);
                    List<String> pendentes = linhas.stream()
                            .filter(linha -> !linha.trim().endsWith(";COMMIT"))
                            .toList();

                    if (pendentes.isEmpty()) {
                        Files.delete(logFile);
                        System.out.println("[LOG] Log totalmente processado e deletado.");
                    } else {
                        Files.write(logFile, pendentes);
                        System.out.println("[LOG] Log atualizado com apenas operações pendentes.");
                    }
                } else {
                    System.out.println("[LOG] Arquivo não encontrado.");
                }
            }

            // Enviar banco atualizado como confirmação de entrega (ACK com dados)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(bancoDB);
            out.flush();

            byte[] responseBytes = baos.toByteArray();
            DatagramPacket responsePacket = new DatagramPacket(
                    responseBytes,
                    responseBytes.length,
                    packet.getAddress(),
                    packet.getPort()
            );
            socket.send(responsePacket);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[LOG] Erro ao processar requisição UDP: " + e.getMessage());
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
