package serversSystem;

import objetos.Banco;
import objetos.ProcessadorBancario;

import java.io.*;
import java.net.*;
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

        PORT = Integer.parseInt(args[0]);
        System.out.println("Server Auxiliar (UDP) iniciado na porta: " + PORT);


        long inicio = System.nanoTime();
        getBanco();
        banco.imprimirContas();
        PORT = Integer.parseInt(args[0]);
        banco.setBloco(PORT % 10);
        System.out.println("[BLOCO] " + banco.getBloco());
        System.out.println();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> enviarBancoParaAuxiliar(), 30, 30, TimeUnit.SECONDS);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             DatagramSocket serverSocket = new DatagramSocket(PORT)) {

            byte[] receiveData = new byte[65535];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            while (true) {
                serverSocket.receive(receivePacket);
                executor.execute(() -> handleClient(receivePacket, serverSocket));
                qtdClients.incrementAndGet();
                System.out.println(qtdClients.get() + " clientes");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        long fim = System.nanoTime();
        System.out.printf("Tempo total: %.3f ms%n", (fim - inicio) / 1_000_000.0);
    }




    private static void handleClient(DatagramPacket receivePacket, DatagramSocket serverSocket) {
        try {
            InetAddress clientAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();

            String msg = new String(receivePacket.getData(), 0, receivePacket.getLength());

            if ("PING".equals(msg)) {
                sendResponse("PONG", clientAddress, clientPort, serverSocket);
                return;
            }
            if ("COMMIT".equals(msg)) {
                enviarBancoParaAuxiliar();
                sendResponse("COMMIT RECEIVED", clientAddress, clientPort, serverSocket);
                replayer = false;
                return;
            }

            if ("ReplayerLog".equals(msg)){
                sendResponse("REPLAYER RECEIVED", clientAddress, clientPort, serverSocket);
                replayer = true;
                return;
            }

            System.out.println("[REQ] Operação recebida de " + clientAddress + ": " + msg);
            String reply = new String();
            long inicioEspera = System.nanoTime(); // 🕒 Início da espera
            //bancoLock.readLock().lock(); // 🔒 Acesso de leitura ao banco
            long fimEspera = System.nanoTime(); // 🕒 Fim da espera
            System.out.printf("[THREAD] Leitura aguardou %.3f ms%n", (fimEspera - inicioEspera) / 1_000_000.0);

            try {
                reply = process.processar(msg);

                if (validacaoResp(reply) && !replayer) {
                    try (DatagramSocket logSocket = new DatagramSocket()) {
                        int bloco = banco.getBloco();
                        String logMessage = bloco + ";" + msg;
                        byte[] logData = logMessage.getBytes();
                        DatagramPacket logPacket = new DatagramPacket(logData, logData.length, InetAddress.getByName("localhost"), 9000);
                        logSocket.send(logPacket);  // WAL antes da operação

                        //System.out.println("Log enviado com sucesso para o bloco " + bloco + "!");
                    }
                }

            } catch (IOException e) {
                System.err.println("Error envio para o WAL: " + e.getMessage());
            } finally {
                //bancoLock.readLock().unlock();
            }

            sendResponse(reply, clientAddress, clientPort, serverSocket);

        } finally {
            qtdClients.decrementAndGet();
        }
    }

    private static void sendResponse(String response, InetAddress clientAddress, int clientPort, DatagramSocket serverSocket) {
        try {
            byte[] responseData = response.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(responseData, responseData.length, clientAddress, clientPort);
            serverSocket.send(sendPacket);
        } catch (IOException e) {
            System.err.println("Error sending response: " + e.getMessage());
        }
    }

    private static void enviarBancoParaAuxiliar() {
        long inicioEspera = System.nanoTime();
        //bancoLock.writeLock().lock();
        long fimEspera = System.nanoTime();
        System.out.printf("\n[COMMIT] Escrita aguardou %.3f ms%n\n", (fimEspera - inicioEspera) / 1_000_000.0);

        try {
            // Serializa o banco
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(byteStream);
            objOut.writeObject(banco);
            objOut.flush();
            byte[] data = byteStream.toByteArray();

            // Envia via UDP
            DatagramSocket socket = new DatagramSocket();
            InetAddress endereco = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(data, data.length, endereco, 8000);
            socket.send(packet);

            // Espera resposta
            byte[] buffer = new byte[65535];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            //socket.setSoTimeout(3000);
            socket.receive(response);

            // Desserializa resposta
            ByteArrayInputStream byteIn = new ByteArrayInputStream(response.getData(), 0, response.getLength());
            ObjectInputStream objIn = new ObjectInputStream(byteIn);
            Banco bancoNovo = (Banco) objIn.readObject();

            mesclarBancos(bancoNovo);
            process.setBanco(banco);
            socket.close();
            Thread.sleep(100);
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.err.println("[UDP] Erro na comunicação com o auxiliar: " + e.getMessage());
        } finally {
            //bancoLock.writeLock().unlock();
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


    private static void getBanco() {
        long inicioEspera = System.nanoTime();
        bancoLock.writeLock().lock();
        long fimEspera = System.nanoTime();
        System.out.printf("\n[INIT] Tempo para receber o banco foi de %.3f ms%n\n", (fimEspera - inicioEspera) / 1_000_000.0);

        try {
            // Serializa o banco
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(byteStream);
            objOut.writeObject(banco);
            objOut.flush();
            byte[] data = byteStream.toByteArray();

            // Envia via UDP
            DatagramSocket socket = new DatagramSocket();
            InetAddress endereco = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(data, data.length, endereco, 8000);
            socket.send(packet);

            // Espera resposta
            byte[] buffer = new byte[65535];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
           // socket.setSoTimeout(5000);
            socket.receive(response);

            // Desserializa resposta
            ByteArrayInputStream byteIn = new ByteArrayInputStream(response.getData(), 0, response.getLength());
            ObjectInputStream objIn = new ObjectInputStream(byteIn);
            Banco bancoNovo = (Banco) objIn.readObject();

            mesclarBancos(bancoNovo);
            process.setBanco(banco);
            socket.close();
            Thread.sleep(500);
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.err.println("[UDP] Erro ao recuperar banco via UDP: " + e.getMessage());
        } finally {
            bancoLock.writeLock().unlock();
        }
    }


}
