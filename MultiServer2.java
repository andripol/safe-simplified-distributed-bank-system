/***
 *
 * Created by andri on 02/07
 */


import java.io.*;
import java.net.*;
import java.util.*;

class MultiServerToClientDaemon2 implements Runnable{

    final private ServerSocket sSocket;
    final private int sId;

    MultiServerToClientDaemon2(ServerSocket sSocket) {
        this.sSocket = sSocket;
        sId = sSocket.getLocalPort() % 4000;
    }

    public void run(){

        Socket cSocket;

        //System.out.println("Starting Server " + sId);

        try{
            while(true){
                // System.out.println("Waiting for a new client request...");
                cSocket = sSocket.accept();
                //System.out.println("New client connection established");
                //create MultiServerThread2 to serve client request
                new Thread(new MultiServerToClientThread2(cSocket)).start();
            }
        }
        catch (Exception e) {
            //System.out.println("Server" + sId + " " + MultiServer2.accounts);
        }
    }
}

class MultiServerToServerDaemon2 implements Runnable{

    final private ServerSocket sSocket;
    final private int sId;

    MultiServerToServerDaemon2(ServerSocket sSocket) {
        this.sSocket = sSocket;
        sId = sSocket.getLocalPort() % 8000;
    }

    public void run(){

        Socket cSocket;

        System.out.println("Starting Server " + sId);



        try{
            while(true){
                //System.out.println("Waiting for a new server request...");
                cSocket = sSocket.accept();
                //System.out.println("New server connection established");
                //create MultiServerThread2 to serve other server's request
                new Thread(new MultiServerToServerThread2(cSocket)).start();
            }
        }
        catch (Exception e) {
            //System.out.println("Server" + sId + " " + MultiServer2.accounts);
        }

    }

}


public class MultiServer2 {

    protected static Map<Integer, Object> account_lock = new HashMap<>();
    public static Map<Integer, Integer> accounts = new HashMap<>();

    protected static int[] servers_ports = {8003, 8001};

    protected static final int serverId = 2;

    public static void main(String[] args){

        try{
            //initialize the accounts of the server
            Thread initialization_thread = new Thread(new InitializeThread2());
            initialization_thread.start();
            initialization_thread.join();

            try {
                ServerSocket server_client_socket = new ServerSocket(4000 + serverId);
                new Thread(new MultiServerToClientDaemon2(server_client_socket)).start();

                ServerSocket server_server_socket = new ServerSocket(8000 + serverId );
                new Thread(new MultiServerToServerDaemon2(server_server_socket)).start();

            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


}
