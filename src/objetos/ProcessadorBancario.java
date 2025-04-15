package objetos;

import java.util.StringTokenizer;

public class ProcessadorBancario {
    private static int numContas = 0;
    private Banco banco;
    public ProcessadorBancario(Banco banco) {
        this.banco = banco;
    }

    public void setBanco(Banco banco) {
        this.banco = banco;
    }

    public String processar(String request) {

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String comando = null;
        int numConta;
        int contaDestino = 0;
        int valor = 0;

        StringTokenizer tokenizer = new StringTokenizer(request, ";");
        comando = tokenizer.nextToken();

        switch (comando) {
            case "criar":
                numConta = Integer.parseInt(tokenizer.nextToken());
                String nome = tokenizer.nextToken();
                if (banco.addConta(numConta, nome)) {
                    numContas++;
                    System.out.println("Conta criada com sucesso!");
                    return ("Conta >" + numConta + "< criada com sucesso!");
                } else {
                    System.out.println("Erro ao criar conta!");
                    return ("Erro ao criar conta! número de conta " + numConta + "já existe.");
                }
            case "sacar":
                numConta = Integer.parseInt(tokenizer.nextToken());
                valor = Integer.parseInt(tokenizer.nextToken());
                if (banco.sacar(numConta, valor)) {
                    System.out.println("Saque realizado com sucesso!");
                    return ("Novo saldo da conta >" + numConta + "< é de " + banco.getSaldoConta(numConta));
                } else {
                    System.out.println("Erro ao sacar!");
                    return ("Erro ao sacar!");
                }
            case "depositar":
                numConta = Integer.parseInt(tokenizer.nextToken());
                valor = Integer.parseInt(tokenizer.nextToken());
                if (banco.depositar(numConta, valor)) {
                    System.out.println("Depósito realizado com sucesso!");
                    return ("Depósito realizado. Novo saldo da conta >" + numConta + "< é de " + banco.getSaldoConta(numConta));
                } else {
                    System.out.println("Erro ao depositar!");
                    return ("Erro ao depositar");
                }
            case "saldo":
                numConta = Integer.parseInt(tokenizer.nextToken());
                if (banco.saldo(numConta) != -1) {
                    System.out.println("Saldo da conta " + banco.getSaldoConta(numConta));
                    return ("O saldo da conta é de " + banco.getSaldoConta(numConta));
                } else {
                    System.out.println("Erro ao consultar o saldo!");
                    return ("Erro ao consultar o saldo");
                }
            default:
                return "Comando desconhecido! " + comando;

        }
    }
}
