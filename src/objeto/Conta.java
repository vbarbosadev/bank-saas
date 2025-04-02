package objeto;

public class Conta {
    private Integer accNum;
    private String titular;
    private double saldo;


    public Conta(Integer accNumber, String titular, String tipo, double saldo) {
        this.accNum = accNumber;
        this.titular = titular;
        this.saldo = saldo;
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
