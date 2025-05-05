package testes;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ClientTest {

    public static void main(String[] args) {

        //Scanner sc = new Scanner(System.in);

        try(Socket socket = new Socket("localhost", 5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Iniciando o servidor cliente...");

            String msg = "criar;2521;vinicius\n";
            //String msg = "saldo;2526\n";
            //String msg = "sacar;2526;2454";
            //String msg = "depositar;2526;8880";

            // msg = sc.nextLine();
            System.out.println("Mensagem: " + msg);


            out.println(msg);
            out.flush();
            System.out.println("AQUI: ");
            String resp = in.readLine();

            System.out.println("Resposta: " + resp);
            System.out.println("Resposta: " + resp);


        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Fim do servidor cliente");
    }
}
