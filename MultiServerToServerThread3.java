/**
 * Created by andri on 29/6/2018.
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class MultiServerToServerThread3 extends MultiServer3 implements  Runnable {

    final private Socket cSocket;
    final private String failure_message = "failed to connect to server";

    MultiServerToServerThread3(Socket cSocket){
        this.cSocket = cSocket;
    }

    public void run() {

        System.out.println("[Thread:" +  Thread.currentThread().getId() + "]New server connection established");

        Scanner sockReader;
        PrintWriter sockWriter;
        String request;
        String[] request_parts;
        String final_response;

        try {
            char action;
            int cId1, amount, cId2;
            int oBalance1;
            int forwarding;

            sockReader = new Scanner(cSocket.getInputStream());
            request = sockReader.nextLine();
            System.out.println("[Thread:" +  Thread.currentThread().getId() + "]Server1 response to Server3 request: " + request);
            request_parts = request.split(",");

            action = request_parts[0].charAt(0);
            cId1 = Integer.parseInt(request_parts[1]);
            amount = Integer.parseInt(request_parts[2]);
            cId2 = Integer.parseInt(request_parts[3]);
            forwarding = Integer.parseInt(request_parts[4]);

            //create account if this is the first request ever
            if (!accounts.containsKey(cId1)){
                accounts.put(cId1, 0);
                account_lock.put(cId1, new Object());
            }


            //send messages and receive response from the third server before proceeding
            String response;
            final_response = "true";

            switch (action) {
                case '+':
                    synchronized (account_lock.get(cId1)){

                        oBalance1 = accounts.get(cId1);

                        //update yourself
                        accounts.put(cId1, oBalance1 + amount);

                        if (forwarding == 1)
                            break;

                        //update other servers as well if forwarding = 2
                        if (servers_availability[0] == 1){
                            //wait for response... indicates if the other server updated its map
                            response = forward_to_server_and_get_response(request_parts, forwarding);
                            System.out.println("Server1 response to Server 3 request: " + response);

                            if (response.equals(failure_message))
                                servers_availability[0] = 0;
                        }

                    }
                    break;

                case '-':
                    synchronized (account_lock.get(cId1)){

                        oBalance1 = accounts.get(cId1);

                        //first check the validity of the transaction yourself
                        if ((oBalance1 - amount) < 0){
                            System.out.println("[Thread" +  Thread.currentThread().getId() + "] Transaction request by Server2 aborted.");
                            final_response = "false";
                            break;
                        }

                        if (forwarding == 1){
                            accounts.put(cId1, oBalance1 - amount);
                            break;
                        }

                        //update other servers as well if forwarding = 2
                        if (servers_availability[0] == 1){
                            //wait for response... indicates if the other server updated its map
                            response = forward_to_server_and_get_response(request_parts, forwarding);
                            System.out.println("Server1 response to Server3 request: " + response);

                            if (response.equals("false")) {
                                final_response = "false";
                                break;
                            }
                            else if (response.equals(failure_message))
                                servers_availability[0] = 0;
                        }

                        //successor server responsed with "true" or is down. update yourself and leave
                        accounts.put(cId1, oBalance1 - amount);
                    }
                    break;

                case '>':
                    if (!accounts.containsKey(cId2)){
                        accounts.put(cId2, 0);
                        account_lock.put(cId2, new Object());
                    }
                    if (cId1 > cId2) {
                        synchronized (account_lock.get(cId1)){
                            synchronized (account_lock.get(cId2)) {
                                oBalance1 = accounts.get(cId1);
                                //first check the validity of the transaction yourself
                                if ((oBalance1 - amount) < 0){
                                    final_response = "false";
                                    break;
                                }

                                if (forwarding == 1){
                                    accounts.put(cId1, oBalance1 - amount);
                                    accounts.put(cId2, accounts.get(cId2) + amount);
                                    break;
                                }

                                //update other servers as well if forwarding = 2
                                if (servers_availability[0] == 1){
                                    //wait for response... indicates if the other server updated its map
                                    response = forward_to_server_and_get_response(request_parts, forwarding);
                                    System.out.println("Server1 response to Server3 request: " + response);

                                    if (response.equals("false")) {
                                        final_response = "false";
                                        break;
                                    }
                                    else if (response.equals(failure_message))
                                        servers_availability[0] = 0;
                                }

                                //successor server responsed with "true" or is down. update yourself and leave
                                accounts.put(cId1, oBalance1 - amount);
                                accounts.put(cId2, accounts.get(cId2) + amount);

                            }
                        }
                    }
                    else{
                        synchronized (account_lock.get(cId2)){
                            synchronized (account_lock.get(cId1)) {
                                oBalance1 = accounts.get(cId1);
                                //first check the validity of the transaction yourself
                                if ((oBalance1 - amount) < 0){
                                    final_response = "false";
                                    break;
                                }

                                if (forwarding == 1){
                                    accounts.put(cId1, oBalance1 - amount);
                                    accounts.put(cId2, accounts.get(cId2) + amount);
                                    break;
                                }

                                //update other servers as well if forwarding = 2
                                if (servers_availability[0] == 1){
                                    //wait for response... indicates if the other server updated its map
                                    response = forward_to_server_and_get_response(request_parts, forwarding);
                                    System.out.println("Server1 response to Server3 request: " + response);

                                    if (response.equals("false")) {
                                        final_response = "false";
                                        break;
                                    }
                                    else if (response.equals(failure_message))
                                        servers_availability[0] = 0;
                                }

                                //successor server responsed with "true" or is down. update yourself and leave
                                accounts.put(cId1, oBalance1 - amount);
                                accounts.put(cId2, accounts.get(cId2) + amount);
                            }
                        }
                    }
                    break;

            }
            sockWriter = new PrintWriter(cSocket.getOutputStream());
            sockWriter.println(final_response);
            System.out.println("Server3 response to Server2 request: " + final_response);
            sockWriter.flush();

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private String forward_to_server_and_get_response(String[] request_parts, int forwarding){
        Socket cSocketSS;
        PrintWriter sockWriterSS;
        Scanner sockReaderSS;

        try {
            cSocketSS = new Socket("localhost", servers_ports[0]);
            sockWriterSS = new PrintWriter(cSocketSS.getOutputStream());
            sockWriterSS.println(request_parts[0] + "," + request_parts[1] + "," + request_parts[2] + "," + request_parts[3] + "," + Integer.toString(forwarding - 1));
            sockWriterSS.flush();
            sockReaderSS = new Scanner(cSocketSS.getInputStream());
            //wait for response... indicates if other servers updated their map
            String response = sockReaderSS.nextLine();
            cSocketSS.close();
            return response;
        }
        catch (Exception e){
            e.printStackTrace();
            return(failure_message);
        }

    }

}
