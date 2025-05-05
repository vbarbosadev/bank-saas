package serversSystem;

import WAL.WALUtils;
import objetos.Banco;
import objetos.ProcessadorBancario;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerInstance {



    public static AtomicInteger qtdClients = new AtomicInteger(0);
    public static final Banco banco = new Banco();
    public static final ProcessadorBancario process = new ProcessadorBancario(banco);
    public static final ReentrantReadWriteLock bancoLock = new ReentrantReadWriteLock(); // 🔐 Lock para sincronização
    public static int PORT = 0;
    public static boolean replayer = false;

    public static void main(String[] args) {
        long inicio = System.nanoTime();
        getBanco();
        banco.imprimirContas();
        PORT = Integer.parseInt(args[0]);
        int BACKLOG = Integer.parseInt(args[1]);
        banco.setBloco(PORT % 10);
        System.out.println("[BLOCO] " + banco.getBloco());
        System.out.println("Server Auxiliar iniciado na porta: " + PORT + " com BACKLOG: " + BACKLOG);
        System.out.println();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> enviarBancoParaAuxiliar(), 30, 30, TimeUnit.SECONDS);

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

        long fim = System.nanoTime();
        System.out.printf("Tempo total: %.3f ms%n", (fim - inicio) / 1_000_000.0);
    }

    private static void handleClient(Socket LeaderSocket) {

        try (LeaderSocket;
             BufferedReader input = new BufferedReader(new InputStreamReader(LeaderSocket.getInputStream()));
             OutputStream output = LeaderSocket.getOutputStream()) {

            String msg = input.readLine();
            if (msg == null || !msg.startsWith("GET")) {
                sendHttpResponse(output, "400 Bad Request", "Formato de requisição inválido");
                return;
            }

            String[] parts = msg.split(" ");
            if (parts.length < 2) {
                sendHttpResponse(output, "400 Bad Request", "Requisição malformada");
                return;
            }

            String comando = parts[1].substring(1); // remove a barra inicial



            if ("PING".equals(msg)) {
                //output.println("PONG");
                return;
            }
            if ("COMMIT".equals(msg)) {
                enviarBancoParaAuxiliar();
                replayer = false;
                return;
            }

            if ("ReplayerLog".equals(msg)){
                replayer = true;
                return;
            }

            System.out.println("Replayer Log: " + replayer);

            System.out.println("[REQ] Operação recebida de " + LeaderSocket.getInetAddress() + ": " + msg);
            String reply = new String();
            long inicioEspera = System.nanoTime(); // 🕒 Início da espera
            bancoLock.readLock().lock(); // 🔒 Acesso de leitura ao banco
            long fimEspera = System.nanoTime(); // 🕒 Fim da espera
            //System.out.printf("[THREAD] Leitura aguardou %.3f ms%n", (fimEspera - inicioEspera) / 1_000_000.0);
            try {
                reply = process.processar(msg);
                if(validacaoResp(reply) && !replayer){
                    try (Socket logSocket = new Socket("localhost", 9000)) {
                        int bloco  = (banco.getBloco());
                        var logOut = new PrintWriter(logSocket.getOutputStream(), true);
                        logOut.println(bloco + ";" + msg);  // WAL antes da operação

                        //System.out.println("Log enviado com sucesso para o bloco " + bloco + "!");

                    }
                }
                if(!replayer){
                    System.out.println("[LOG] requisição" + msg + "reprocessada!");
                }

            }   catch (IOException e){
                System.err.println("Error envio para o WAL : " + e.getMessage());
            }
            finally {
                bancoLock.readLock().unlock();
            }

            //output.println(reply);


        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            qtdClients.decrementAndGet();
        }
    }

    // 🚀 Função que envia o objeto Banco para o servidor auxiliar
    private static void enviarBancoParaAuxiliar() {
        long inicioEspera = System.nanoTime(); // 🕒
        bancoLock.writeLock().lock(); // 🔐
        long fimEspera = System.nanoTime(); // 🕒
        //System.out.printf("\n[COMMIT] Escrita aguardou %.3f ms%n\n", (fimEspera - inicioEspera) / 1_000_000.0);

        try {
            try (Socket socket = new Socket("localhost", 8000); // Porta do servidor auxiliar
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // Enviar o objeto banco
                out.writeObject(banco);
                out.flush();
                System.out.println("[LOG] Contato com o database foi bem sucedido.\n");
                // Atualizar o banco com a resposta do servidor auxiliar
                Banco bancoNovo = (Banco) in.readObject();
                out.close();
                in.close();
                socket.close();
                mesclarBancos(bancoNovo);
                process.setBanco(banco);
                Thread.sleep(100);


            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                System.err.println("[LOG] Falha ao enviar banco para o auxiliar: " + e.getMessage());
            }
        } finally {
            bancoLock.writeLock().unlock();

        }
    }

    private static void mesclarBancos(Banco recebido) {
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

    public static boolean validacaoResp(String resp) {
        String[] partes = resp.split(":", 2); // divide em 2 partes
        String erro = partes[0];
        if(erro.equals("Erro de transação")) {
            //System.out.println("ERROOOOOOOOOOOOOOOOOOOOR");

            return false;
        } else if (erro.equals("Erro")) {

            return false;

        }

        return true;
    }

    private static void getBanco(){
        long inicioEspera = System.nanoTime(); // 🕒
        bancoLock.writeLock().lock(); // 🔐
        long fimEspera = System.nanoTime(); // 🕒
        System.out.printf("\n[INIT] Tempo para receber o banco foi de %.3f ms%n\n", (fimEspera - inicioEspera) / 1_000_000.0);

        try {
            try (Socket socket = new Socket("localhost", 8000); // Porta do servidor auxiliar
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {


                out.writeObject(banco);
                out.flush();

                Banco bancoNovo = (Banco) in.readObject();
                out.close();
                in.close();
                socket.close();
                mesclarBancos(bancoNovo);
                process.setBanco(banco);
                Thread.sleep(500);

            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                System.err.println("[LOG] Falha ao enviar banco para o auxiliar: " + e.getMessage());
            }
        } finally {
            bancoLock.writeLock().unlock();

        }
    }

    private static void sendHttpResponse(OutputStream out, String status, String body) throws IOException {
        String response = "HTTP/1.0 " + status + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

}
