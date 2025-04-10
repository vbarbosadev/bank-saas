package serversSystem;

import objetos.Banco;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class BancoDB {

    private static Banco bancoDB = new Banco();

    public static void main(String[] args) throws IOException {

        ServerSocket mysocket = new ServerSocket(7040);

        while (true){
            Socket client = mysocket.accept();
            BancoDB.start(client);
        }
    }

    public static void start(Socket s) {
        try(ObjectInputStream in = new ObjectInputStream(s.getInputStream());){
            Banco bancoRecebido = (Banco) in.readObject();
            mesclarBancos(bancoRecebido);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }



    private static void mesclarBancos(Banco recebido) {
        HashMap<Integer, Object> contasRecebidas = recebido.getContas();

        for (Map.Entry<Integer, Object> entry : contasRecebidas.entrySet()) {
            Integer idConta = entry.getKey();
            Map<String, Integer> dadosRecebidos = (Map<String, Integer>) entry.getValue();

            Map<String, Integer> dadosAtuais = (Map<String, Integer>) bancoDB.getContas().get(idConta);

            if (dadosAtuais != null) {
                String nome = dadosRecebidos.keySet().iterator().next();
                Integer saldo = dadosRecebidos.get(nome);
                dadosAtuais.put(nome, saldo);
            } else {
                bancoDB.getContas().put(idConta, dadosRecebidos);
            }
        }
    }

}
