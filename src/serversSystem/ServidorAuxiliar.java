package serversSystem;

import objetos.Banco;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServidorAuxiliar {
    private static final int PORTA = 7010;
    private Banco banco;

    public ServidorAuxiliar(Banco bancoInicial) {
        this.banco = bancoInicial;
    }

    public void iniciar() {
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            System.out.println("Servidor auxiliar aguardando conexões na porta " + PORTA);

            while (true) {
                Socket socket = new Socket("localhost", 7050);
                new Thread(() -> atualizarBanco(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void atualizarBanco(Socket socket) {
        try (socket;
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(banco);
            out.flush();
            out.close();
            System.out.println("Banco atualizado via objeto serializado.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public Banco getBanco() {
        return banco;
    }
}
