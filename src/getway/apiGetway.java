package getway;


import objeto.Banco;
import objeto.Conta;
import objeto.processRequest;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class apiGetway {

    static int qtdClientes = 0;
    static Socket bancoSocket;

    static {
        try {
            bancoSocket = new Socket("localhost", 8081);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("API Getway está ativa!");

        while (true) {
            Socket socket;
            socket = serverSocket.accept();

            qtdClientes++;

            System.out.println("Cliente " + socket.getInetAddress().getHostAddress() + " conectado");

            ThreadGetway thread = new ThreadGetway(socket, bancoSocket);
            thread.setName("Cliente " + String.valueOf(qtdClientes));
            thread.start();

        }


    }
}