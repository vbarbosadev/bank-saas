package serversSystem;


public class ListaDeServers {
    public String host;
    public int porta;
    public boolean ativo;

    public ListaDeServers(String host, int porta) {
        this.host = host;
        this.porta = porta;
        this.ativo = false;
    }

    public int getPorta(){
        return porta;
    }
}
