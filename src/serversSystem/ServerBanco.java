package serversSystem;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;

public class ServerBanco implements Serializable {
    private static final long serialVersionUID = 1L;
    private static int qtdClientes = 0;
    private static int ordem = 1;


    public static void main(String[] args) throws IOException {
        System.out.println("Iniciando Servidor...");

        var serverSocket = new ServerSocket(6000);
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        while (true) {
            var clientSocket = serverSocket.accept();
            executor.submit(() -> handleRequest(clientSocket));
            qtdClientes++;
        }
    }


    private static void handleRequest(Socket socket) {
        try (socket;
             BufferedReader request = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter response = new PrintWriter(socket.getOutputStream(), true);) {

            String msg = request.readLine();




            // Enviar para o servidor de log (WAL)
            try (Socket logSocket = new Socket("localhost", 9000)) {
                var logOut = new PrintWriter(logSocket.getOutputStream(), true);
                logOut.println(msg);  // WAL antes da operação
                System.out.println("Log enviado com sucesso!");
            }


//            if(ordem == 3){
//                ordem = 1;
//            }
//
//            switch (ordem) {
//                case 1:
//                    // Encaminhar para um servidor auxiliar (ex: localhost:7000) : corrente
//                    try (Socket correnteSocket = new Socket("localhost", 7000)) {
//                        System.out.println("Enviando para o servidor de conta corrente!");
//                        var auxOut = new PrintWriter(correnteSocket.getOutputStream(), true);
//                        var auxIn = new BufferedReader(new InputStreamReader(correnteSocket.getInputStream()));
//                        auxOut.println(msg);
//                        String respCorrente = auxIn.readLine();
//                        response.println(respCorrente); // resposta
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    break;
//                case 2:
//                    // Encaminhar para um servidor auxiliar (ex: localhost:7000) : corrente
//                    try (Socket poupancaSocket = new Socket("localhost", 7001)) {
//                        System.out.println("Enviando para o servidor de conta poupança!");
//                        var auxOut = new PrintWriter(poupancaSocket.getOutputStream(), true);
//                        var auxIn = new BufferedReader(new InputStreamReader(poupancaSocket.getInputStream()));
//                        auxOut.println(msg);
//                        String respostaPoup = auxIn.readLine();
//                        response.println(respostaPoup); // resposta
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    break;
//                case 3:
//                    // Encaminhar para um servidor auxiliar (ex: localhost:7000) : corrente
//                    try (Socket poupancaSocket = new Socket("localhost", 7002)) {
//                        System.out.println("Enviando para o servidor de conta poupança!");
//                        var auxOut = new PrintWriter(poupancaSocket.getOutputStream(), true);
//                        var auxIn = new BufferedReader(new InputStreamReader(poupancaSocket.getInputStream()));
//                        auxOut.println(msg);
//                        String respostaPoup = auxIn.readLine();
//                        response.println(respostaPoup); // resposta
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    break;
//            }
//
//            ordem++;



            String tipoConta = null;
            StringTokenizer tokenizer = new StringTokenizer(msg, ";");
            tipoConta = tokenizer.nextToken();

            switch (tipoConta) {
                case "corrente":
                    // Encaminhar para um servidor auxiliar (ex: localhost:7000) : corrente
                    try (Socket correnteSocket = new Socket("localhost", 7000)) {
                        System.out.println("Enviando para o servidor de conta corrente!");
                        var auxOut = new PrintWriter(correnteSocket.getOutputStream(), true);
                        var auxIn = new BufferedReader(new InputStreamReader(correnteSocket.getInputStream()));
                        auxOut.println(msg);
                        String respCorrente = auxIn.readLine();
                        response.println(respCorrente); // resposta
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "poupanca":
                    // Encaminhar para um servidor auxiliar (ex: localhost:7000) : corrente
                    try (Socket poupancaSocket = new Socket("localhost", 7001)) {
                        System.out.println("Enviando para o servidor de conta poupança!");
                        var auxOut = new PrintWriter(poupancaSocket.getOutputStream(), true);
                        var auxIn = new BufferedReader(new InputStreamReader(poupancaSocket.getInputStream()));
                        auxOut.println(msg);
                        String respostaPoup = auxIn.readLine();
                        response.println(respostaPoup); // resposta
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }



        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
