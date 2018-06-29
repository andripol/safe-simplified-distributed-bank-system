import java.io.*;
import java.net.*;
import java.util.*;

class MultiServerToClientDaemon3 implements Runnable{

    final private ServerSocket sSocket;
    final private int sId;

    MultiServerToClientDaemon3(ServerSocket sSocket) {
        this.sSocket = sSocket;
        sId = sSocket.getLocalPort() % 4000;
    }

    public void run(){

        Socket cSocket;

        System.out.println("Starting Server " + sId);

        try{
            while(true){
                System.out.println("Waiting for a new client request...");
                cSocket = sSocket.accept();
                System.out.println("New client connection established");
                //create MultiServerThread1 to serve client request
                new Thread(new MultiServerToClientThread3(cSocket)).start();

            }
        }
        catch (Exception e) {
            System.out.println("Server" + sId + " " + MultiServer3.accounts);
        }

    }

}

class MultiServerToServerDaemon3 implements Runnable{

    final private ServerSocket sSocket;
    final private int sId;

    MultiServerToServerDaemon3(ServerSocket sSocket) {
        this.sSocket = sSocket;
        sId = sSocket.getLocalPort() % 8000;
    }

    public void run(){

        Socket cSocket;

        System.out.println("Starting Server " + sId);

        try{
            while(true){
                System.out.println("Waiting for a new server request...");
                cSocket = sSocket.accept();
                System.out.println("New server connection established");
                //create MultiServerThread1 to serve other server's request
                new Thread(new MultiServerToServerThread3(cSocket)).start();
            }
        }
        catch (Exception e) {
            System.out.println("Server" + sId + " " + MultiServer3.accounts);
        }

    }

}


public class MultiServer3 {

    protected static Map<Integer, Object> account_lock = new HashMap<>();
    protected static Map<Integer, Integer> accounts = new HashMap<>();

    protected static int[] servers_ports = {8001, 8002};

    public static void main(String[] args){

        final int server1Id = 3;
        try {
            ServerSocket server_client_socket = new ServerSocket(4000 + server1Id);
            new Thread(new MultiServerToClientDaemon3(server_client_socket)).start();

            ServerSocket server_server_socket = new ServerSocket(8000 + server1Id );
            new Thread(new MultiServerToServerDaemon3(server_server_socket)).start();

        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
