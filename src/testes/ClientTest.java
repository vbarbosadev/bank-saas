package testes;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClientTest {

    public static void main(String[] args) {

        //Scanner sc = new Scanner(System.in);

        try (DatagramSocket socket = new DatagramSocket()) {

            System.out.println("Iniciando o cliente...");

            String msg = "criar;2526;vinicius";
            // String msg = "saldo;2526\n";
            //String msg = "sacar;2526;2454";
            // String msg = "depositar;2526;8880";

            // msg = sc.nextLine();
            System.out.println("Mensagem: " + msg);

            // Envia a mensagem para o servidor
            byte[] buffer = msg.getBytes();
            InetAddress serverAddress = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, 5000);
            socket.send(packet);

            // Recebe a resposta do servidor
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(responsePacket);
            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());

            System.out.println("Resposta: " + response);

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Fim do cliente");
    }
}
