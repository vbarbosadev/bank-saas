package objetos;

import java.util.StringTokenizer;

public class ProcessadorBancario {
    private static int numContas = 0;
    private Banco banco;
    public ProcessadorBancario(Banco banco) {
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
        tokenizer.nextToken();
        comando = tokenizer.nextToken();
        System.out.println("Comandooooo " + comando);


        switch (comando){
            case "criar":
                numConta = Integer.parseInt(tokenizer.nextToken());
                String nome = tokenizer.nextToken();
                if(banco.addConta(numConta, nome)){
                    numContas++;
                    System.out.println("Conta criada com sucesso!");
                    return ("Conta >" + numConta + "< criada com sucesso!");
                } else {
                    System.out.println("Erro ao criar conta!");
                    return ("Erro ao criar conta! número de conta " + numConta + "já existe.");
                }
                break;
            case "sacar":
                numConta = Integer.parseInt(tokenizer.nextToken());
                valor = Integer.parseInt(tokenizer.nextToken());
                if(banco.sacar(numConta, valor)){
                    System.out.println("Saque realizado com sucesso!");
                    return ("Novo saldo da conta >" + numConta + "< é de " + banco.getSaldoConta(numConta));
                }
                else {
                    System.out.println("Erro ao sacar!");
                    return ("Erro ao sacar o valor " + valor + " o saldo disponivel é menor que o solicitado!");
                }
                break;
            case "depositar":
                numConta = Integer.parseInt(tokenizer.nextToken());
                valor = Integer.parseInt(tokenizer.nextToken());
                if(banco.sacar(numConta, valor)){
                    System.out.println("Saque realizado com sucesso!");
                    return ("Novo saldo da conta >" + numConta + "< é de " + banco.getSaldoConta(numConta));
                }
                else {
                    System.out.println("Erro ao sacar!");
                    return ("Erro ao sacar o valor " + valor + " o saldo disponivel é menor que o solicitado!");
                }
                break;

        }

        while (tokenizer.hasMoreElements()) {
            if (comando.equals("saldo")) {
                contaPessoal = Integer.parseInt(tokenizer.nextToken());
                break;
            }
            contaPessoal = Integer.parseInt(tokenizer.nextToken());
            if(comando.equals("transferencia")){
                contaDestino = Integer.parseInt(tokenizer.nextToken());
            }
            valor = Integer.parseInt(tokenizer.nextToken().trim());
        }

    }
}
