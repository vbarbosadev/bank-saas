package serversSystem;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerBanco implements Serializable {
    private static int qtdClientes = 0;
    private static MonitorDeInstancias monitor = new MonitorDeInstancias();
    private static List<ListaDeServers> ativos = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Iniciando Servidor...\n");

        int PORT = Integer.parseInt(args[0]);
        int BACKLOG = Integer.parseInt(args[1]);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> monitoramento(), 0, 20, TimeUnit.SECONDS);


        var serverSocket = new ServerSocket(PORT, BACKLOG);
        var executor = Executors.newVirtualThreadPerTaskExecutor();



        while (true) {
            var clientSocket = serverSocket.accept();
            executor.submit(() -> handleRequest(clientSocket));
            qtdClientes++;
            System.out.println("qtd clientes: " + qtdClientes);
        }
    }



    private static void handleRequest(Socket socket) {
        try (socket;
             BufferedReader request = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter response = new PrintWriter(socket.getOutputStream(), true)) {



            String msg = request.readLine();
            StringTokenizer tokenizer = new StringTokenizer(msg, ";");
            tokenizer.nextToken();
            int accNum = Integer.parseInt(tokenizer.nextToken());
            int lastDigit = accNum % 10;



            int portaDestino;
            int bloco;




            if (lastDigit <= 2) {
                portaDestino = 7001;
                bloco = 1;
            } else if (lastDigit <= 5) {
                portaDestino = 7002;
                bloco = 2;
            } else {
                portaDestino = 7003;
                bloco = 3;
            }

            boolean servidorAtivo = false;

            ativos = MonitorDeInstancias.getServidorAtivo();
            for(var ativo : ativos) {
                if (ativo.getPorta() == portaDestino) {
                    servidorAtivo = true;
                    break;
                }
            }

            if (!servidorAtivo) {
                response.println("Erro: Servidor indisponível na porta " + portaDestino);
                System.out.println("Servidor inativo na porta " + portaDestino + ", operação não realizada.");
                return;
            }

            try (Socket auxSocket = new Socket("localhost", portaDestino)) {
                System.out.println("Enviando para o servidor da porta " + portaDestino + "!");
                var auxOut = new PrintWriter(auxSocket.getOutputStream(), true);
                var auxIn = new BufferedReader(new InputStreamReader(auxSocket.getInputStream()));

                auxOut.println(msg);
                String respBanco = auxIn.readLine();

                System.out.println("Resp banco: " + respBanco);



                response.println(respBanco);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Falha ao conectar ao servidor na porta " + portaDestino);
                response.println("Erro: Falha ao conectar ao servidor");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void monitoramento() {
        monitor.iniciarMonitoramento();
    }


}
