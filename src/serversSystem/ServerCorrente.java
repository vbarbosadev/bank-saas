package serversSystem;
import objetos.Banco;
import objetos.ProcessadorBancario;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerCorrente {

    public static int qtdClients = 0;

    public static void main(String[] args) {

        int PORT = 0;
        PORT = Integer.parseInt(args[0]);
        System.out.println("Server Auxiliar iniciado na porta: " + PORT);
        Banco banco = new Banco();

        ProcessadorBancario process = new ProcessadorBancario(banco);

        // ✅ Thread agendada para enviar o banco a cada 2 minutos
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> enviarBancoParaAuxiliar(banco), 0, 2, TimeUnit.MINUTES);

        // 🔥 Virtual thread para cada requisição
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             ServerSocket server = new ServerSocket(PORT, 1000)) {

            ListaDeServers serverL = new ListaDeServers("localhost", PORT);
            MonitorDeInstancias.addServer(serverL);

            while (true) {
                Socket LeaderSocket = server.accept();
                executor.execute(() -> handleClient(LeaderSocket, process));
                qtdClients++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket LeaderSocket, ProcessadorBancario process) {
        try (LeaderSocket;
             BufferedReader input = new BufferedReader(new InputStreamReader(LeaderSocket.getInputStream()));
             PrintWriter output = new PrintWriter(LeaderSocket.getOutputStream(), true)) {


            String msg = input.readLine();
            if ("PING".equals(msg)) {
                output.println("PONG");
                return;
            }

            System.out.println("Operação recebida de " + LeaderSocket.getInetAddress() + ": " + msg);

            String reply = process.processar(msg);
            output.println("Server response: " + reply);

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
        finally {
            qtdClients--;
        }
    }

    // 🚀 Função que envia o objeto Banco para o servidor auxiliar
    private static void enviarBancoParaAuxiliar(Banco banco) {
        try (Socket socket = new Socket("localhost", 7040); // Porta do servidor auxiliar
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(banco);
            out.flush();
            System.out.println("[LOG] Banco enviado para o servidor auxiliar com sucesso.");

        } catch (IOException e) {
            System.err.println("[LOG] Falha ao enviar banco para o auxiliar: " + e.getMessage());
        }
    }
}

