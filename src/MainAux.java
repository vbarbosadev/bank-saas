import objetos.Banco;
import serversSystem.ServidorAuxiliar;

public class MainAux {
    public static void main(String[] args) {
        Banco bancoInicial = new Banco();
        ServidorAuxiliar servidor = new ServidorAuxiliar(bancoInicial);
        servidor.iniciar();
    }
}


