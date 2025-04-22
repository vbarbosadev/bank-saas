package serversSystem;


public class ListaDeServers {
    private String host;
    private int porta;
    private boolean ativo;
    private boolean lastPing;
    private int bloco;

    public ListaDeServers(String host, int porta) {
        this.host = host;
        this.porta = porta;
        this.ativo = false;
        this.lastPing = true;
        this.bloco = porta % 10;
    }

    public void setLastPing(boolean lastPing) {
        this.lastPing = lastPing;
    }

    public boolean isLastPing() {
        return lastPing;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setBloco(int bloco) {
        this.bloco = bloco;
    }
    public int getBloco() {
        return bloco;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public String getHost() {
        return host;
    }
    public void setPorta(int porta) {
        this.porta = porta;
    }



    public int getPorta(){
        return porta;
    }
}
