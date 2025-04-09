package serversSystem;

import objetos.Banco;

import java.io.ObjectInputStream;
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
                Socket socket = serverSocket.accept();
                new Thread(() -> atualizarBanco(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void atualizarBanco(Socket socket) {
        try (socket;
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            Banco recebido = (Banco) in.readObject();
            mesclarBancos(recebido);
            System.out.println("Banco atualizado via objeto serializado.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mesclarBancos(Banco recebido) {
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

    public Banco getBanco() {
        return banco;
    }
}
