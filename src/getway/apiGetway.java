package getway;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class apiGetway {

    static int qtdClientes = 0;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(4215);
        System.out.println("A porta 2354 foi aberta!");
        System.out.println("Servidor, com Thread, esperando receber mensagens de vários clientes...");

        // implementar funcao para ler o log e pegar os dados dos user conectados



        while (true) {
            // recieve socket

            Socket apiSocket;
            apiSocket = serverSocket.accept();

            // sintaxe do protocolo
            // "conta : acao : complementos"
            // "accNum;nome : Acao;Complementos"
            // "criar;accNum;nome"
            // "mostrar;accNum"
            // "sacar;valorDeSaque;"
            // "depositar;valorDeDeposito"
            // "transferir;contaDestino;valor"
            //


            BufferedReader input = new BufferedReader(new InputStreamReader(apiSocket.getInputStream()));
            PrintWriter output = new PrintWriter(apiSocket.getOutputStream(), true);



            qtdClientes++;

            //Mostrando endereço IP do cliente conectado
            System.out.println("Cliente " + apiSocket.getInetAddress().getHostAddress() + " conectado");

//            ThreadServidor thread = new ThreadServidor(apiSocket);
//            thread.setName("Thread Servidor: " + String.valueOf(qtdClientes));
//            thread.start();
        }
    }


}
