package serversSystem;

import objeto.Banco;
import objeto.Conta;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class serverBanco {
    static Banco banco = new Banco("BB");


    public static void main(String[] args) {

        while (true){

            try (ServerSocket serverSocket = new ServerSocket(4444)) {

                Socket socket = serverSocket.accept();

               // ObjectInputStream bancoIn = new ObjectInputStream(socket.getInputStream());
//          BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//          String req = input.readLine();



                //banco = (Banco) bancoIn.readObject();

                Conta conta = new Conta(126, "Joao");


                System.out.println("Qtd de contas " + banco.getContas().size());



                banco.adicionaConta(conta);

                System.out.println("Operação recebida para o banco " + banco.getNome());



                try {
                    Conta conta1 = banco.getConta(123);

                    System.out.println("Operação recebida para a conta " + conta.getTitular());

                    System.out.println("Qtd de contas " + banco.getContas().size());

                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                    output.writeObject(banco);
                    ObjectInputStream bancoIn = new ObjectInputStream(socket.getInputStream());
                    banco = (Banco) bancoIn.readObject();
                    System.out.println("Qtd de contas 2 " + banco.getContas().size());
                    System.out.println("Operação recebida para a conta " + banco.getConta(1272).getTitular());



                    output.flush();
                    output.close();
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }




            } catch (IOException e) {
                System.out.println("Ocorreu um erro");
                e.printStackTrace();


            }

        }
    }
}
