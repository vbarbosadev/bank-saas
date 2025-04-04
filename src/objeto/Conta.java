package objeto;

import java.io.Serializable;


public class Conta implements Serializable {
    private Integer accNum;
    private String titular;
    private Double saldo;


    public Conta(Integer accNumber, String titular) {
        this.accNum = accNumber;
        this.titular = titular;
        this.saldo = 0.0;
    }

    public void depositar(double valor) {
        this.saldo += valor;
    }

    public Integer getAccNum() {
        return accNum;
    }
    public String getTitular() {
        return titular;
    }
    public double getSaldo() {
        return saldo;
    }

    public void setAccNum(Integer accNum) {
        this.accNum = accNum;
    }
    public void setTitular(String titular) {
        this.titular = titular;
    }
    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }
}
