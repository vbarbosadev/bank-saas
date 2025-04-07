package objeto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class processRequest{

    /**
     * Recebe uma string no formato: "comando;idConta;valor" ou "comando;idConta"
     * Ex: "depositar;123;100.0" ou "saldo;123"
     * Retorna um objeto Map que pode ser convertido em JSON
     */
    public static Map<String, Object> processar(String entrada) {

        String operacao = null;
        Integer contaP = null;
        Integer contaD = null;
        Double valor = null;

        Map<String, Object> request = new HashMap<>();
        Map<String, Object> data = new HashMap<>();

        StringTokenizer tokenizer = new StringTokenizer(entrada, ";");
        while (tokenizer.hasMoreElements()) {
            operacao = tokenizer.nextToken();
            request.put("command", operacao);
            if (operacao.equals("saldo")) {
                contaP = Integer.parseInt(tokenizer.nextToken());
                data.put("id", contaP);
                break;
            }
            contaP = Integer.parseInt(tokenizer.nextToken());
            data.put("id", contaP);
            if(operacao.equals("transferencia")){
                contaD = Integer.parseInt(tokenizer.nextToken());
                data.put("id-Dest", contaD);
            }
            valor = Double.parseDouble(tokenizer.nextToken().trim());
            data.put("valor", valor);
        }

        request.put("data", data);

        return request;
    }
}