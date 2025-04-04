package objeto;

import java.util.ArrayList;
import java.io.Serializable;
import objeto.Conta;

import javax.swing.*;

public class Banco implements Serializable {

    private String nome;
    private ArrayList<Conta> contas = new ArrayList<>();
    private Action action;

    public Banco(String nome){
        this.nome = nome;
    }

    public boolean adicionaConta(Conta conta) {
        try {
            if(!verificaConta(conta.getAccNum())){
                contas.add(conta);
                return true;
            }
            return false;

        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    };


    public boolean saque(Conta conta, int valor) {
        try{
            Conta actConta = getConta(conta.getAccNum());
            if(actConta.getSaldo() >= valor){
                conta.setSaldo(conta.getSaldo() - valor);
                return true;
            }
            return false;

        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public boolean deposito(Conta conta, int valor) {
        try {
            if (valor < 0.0){
                return false;
            }
            Conta actConta = getConta(conta.getAccNum());
            conta.depositar(valor);
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public boolean transferencia(Conta origem, Conta destino, int valor) {
        try {
            if (origem.getSaldo() < valor) {
                return false;
            }
            origem.setSaldo(origem.getSaldo() - valor);
            destino.setSaldo(destino.getSaldo() + valor);
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public boolean validacao(Conta conta) {
        try{
            for(Conta acc : contas){
                if(acc.getAccNum().equals(conta.getAccNum()) & acc.getTitular().equals(conta.getTitular())){
                    return true;
                }
            }
            return false;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public String getNome() {
        return nome;
    }

    public boolean verificaConta(int accNum) {
        for (Conta acc : contas) {
            if (acc.getAccNum() == accNum) {
                return true;
            }
        }
        return false;
    };

    public Conta getConta(int accNum) {
        for (Conta acc : contas) {
            if (acc.getAccNum() == accNum) {
                return acc;
            }
        }
        return null;
    }


    // getArray
    public ArrayList<Conta> getContas() {
        return contas;
    };

    public enum Action {
        DEPOSITO, SAQUE, TRANSFERENCIA, ADD
    }

}
