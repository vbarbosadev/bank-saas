package objetos;

import java.util.HashMap;
import java.util.Map;

public class Banco {


    private HashMap<Integer, Object> contas = new HashMap<>();

    public boolean addConta(int numconta, String nome) {
        try {
            if (contas.get(numconta) == null) {
                HashMap<String, Integer> dados = new HashMap<String, Integer>();
                dados.put(nome, 0);
                contas.put(numconta, dados);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public boolean sacar(int numconta, int valor) {

        try {
            Map<String, Integer> dados = (Map<String, Integer>) contas.get(numconta);
            if (dados != null) {
                String nome = dados.keySet().iterator().next();
                int saldoAtual = dados.get(nome);
                if (saldoAtual > valor) {
                    dados.put(nome, saldoAtual - valor);
                } else {
                    System.out.println("Conta não encontrada.");
                    return false;
                }
                return true;
            } else {
                System.out.println("Conta não encontrada.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return false;
    }


    public void depositar(int numconta, int valor) {

        try {
            Map<String, Integer> dados = (Map<String, Integer>) contas.get(numconta);
            if (dados != null) {
                // Pega o nome do cliente (única chave)
                String nome = dados.keySet().iterator().next();
                int saldoAtual = dados.get(nome);
                dados.put(nome, saldoAtual + valor);
            } else {
                System.out.println("Conta não encontrada.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public int saldo(int numconta) {

        try {
            Map<String, Integer> dados = (Map<String, Integer>) contas.get(numconta);
            if (dados != null && !dados.isEmpty()) {
                String nome = dados.keySet().iterator().next();
                return dados.get(nome);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }


        return numconta;
    }

    public HashMap<Integer, Object> getContas() {
        return contas;
    }

    public Integer getSaldoConta(int numconta) {
        try {
            Map<String, Integer> dados = (Map<String, Integer>) contas.get(numconta);
            if (dados != null && !dados.isEmpty()) {
                String nome = dados.keySet().iterator().next();
                return dados.get(nome);
            } else {
                System.out.println("Conta não encontrada.");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}

