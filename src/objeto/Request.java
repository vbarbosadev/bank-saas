package objeto;

import java.io.Serializable;

public class Request implements Serializable {

    private Banco banco;
    private String req;

    public Request(Banco banco, String req){
        this.banco = banco;
        this.req = req;
    }





    private int socketPort;

    Request(int socketPort, String entrada) {
        this.socketPort = socketPort;
    }

    public int getSocketPort() {
        return socketPort;
    }

    public void setSocketPort(int socketPort) {
        this.socketPort = socketPort;
    }

}
