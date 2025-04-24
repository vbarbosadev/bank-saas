package serversSystem;
import WAL.WALUtils;
import objetos.Banco;
import objetos.ProcessadorBancario;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerInstance {

    public static AtomicInteger qtdClients = new AtomicInteger(0);
    public static final Banco banco = new Banco(); // usado como lock também
    public static final ProcessadorBancario process = new ProcessadorBancario(banco);
    public static int PORT = 0;

    public static void main(String[] args) {

        PORT = Integer.parseInt(args[0]);
        int BACKLOG = Integer.parseInt(args[1]);
        System.out.println("Server Auxiliar iniciado na porta: " + PORT + " com BACKLOG: " + BACKLOG);
        System.out.println();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> enviarBancoParaAuxiliar(), 10, 120, TimeUnit.SECONDS);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             ServerSocket server = new ServerSocket(PORT, BACKLOG)) {

            while (true) {
                Socket LeaderSocket = server.accept();
                executor.execute(() -> handleClient(LeaderSocket));
                qtdClients.incrementAndGet();
                System.out.println(qtdClients.get() + " clientes");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket LeaderSocket) {
        try (LeaderSocket;
             BufferedReader input = new BufferedReader(new InputStreamReader(LeaderSocket.getInputStream()));
             PrintWriter output = new PrintWriter(LeaderSocket.getOutputStream(), true)) {

            String msg = input.readLine();
            if ("PING".equals(msg)) {
                output.println("PONG");
                return;
            }
            if ("COMMIT".equals(msg)) {
                enviarBancoParaAuxiliar();
                return;
            }

            System.out.println("Operação recebida de " + LeaderSocket.getInetAddress() + ": " + msg);

            String reply;
            synchronized (banco) { // proteger o acesso ao banco
                reply = process.processar(msg);
            }

            output.println(reply);

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            qtdClients.decrementAndGet();
        }
    }

    // 🚀 Função que envia o objeto Banco para o servidor auxiliar
    private static void enviarBancoParaAuxiliar() {
        synchronized (banco) {  // Garantir que o banco não seja alterado durante o envio ou recebimento
            try (Socket socket = new Socket("localhost", 8000); // Porta do servidor auxiliar
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {



                // Enviar o objeto banco
                out.writeObject(banco);
                out.flush();
                System.out.println("[LOG] Contato com o database foi bem sucedido.\n");

                // Atualizar o banco com a resposta do servidor auxiliar
                Banco bancoNovo = (Banco) in.readObject();
                mesclarBancos(bancoNovo);
                process.setBanco(banco);  // Garantir que o processador também tenha o banco atualizado
                Thread.sleep(500);
            } catch (IOException e) {
                System.err.println("[LOG] Falha ao enviar banco para o auxiliar: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static synchronized void mesclarBancos(Banco recebido) {
        HashMap<Integer, Object> contasRecebidas = recebido.getContas();

        for (Map.Entry<Integer, Object> entry : contasRecebidas.entrySet()) {
            Integer idConta = entry.getKey();
            Map<String, Integer> dadosRecebidos = (Map<String, Integer>) entry.getValue();

            Map<String, Integer> dadosAtuais = (Map<String, Integer>) banco.getContas().get(idConta);

            if (dadosAtuais != null) {
                String nome = dadosRecebidos.keySet().iterator().next();
                Integer saldo = dadosRecebidos.get(nome);
                dadosAtuais.put(nome, saldo);
            } else {
                banco.getContas().put(idConta, dadosRecebidos);
            }
        }
    }


}
