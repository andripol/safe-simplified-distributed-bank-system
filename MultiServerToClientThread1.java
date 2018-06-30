/**
 * Created by andri on 28/6/2018.
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class MultiServerToClientThread1 extends MultiServer1 implements Runnable {

    final private Socket cSocket;
    final private String failure_message = "failed to connect to server";
    private String request;


    MultiServerToClientThread1(Socket cSocket, String request) {
        this.cSocket = cSocket;
        this.request = request;
    }

    public void run() {

        System.out.println("[Thread:" +  Thread.currentThread().getId() + "]New client connection established");

        Scanner sockReader;
        PrintWriter sockWriter;
        String[] request_parts;

        //use this flag in case you begin another thread for the very same request
        boolean end_without_signs = false;
        //default 0. if the successor server is down change it to forward to the one before
        int next_server_to_send = 0;

        try {
            char action;
            int cId1, amount, cId2;
            int oBalance1, oBalance2;
            int nBalance1, nBalance2;

            if (request.equals("read_from_socket")){
                sockReader = new Scanner(cSocket.getInputStream());
                request = sockReader.nextLine();
            }

            System.out.println("[Thread:" +  Thread.currentThread().getId() + "]Server 1 " + ", Client Request: " + request);
            request_parts = request.split(",");

            action = request_parts[0].charAt(0);
            cId1 = Integer.parseInt(request_parts[1]);
            amount = Integer.parseInt(request_parts[2]);
            cId2 = Integer.parseInt(request_parts[3]);

            //create account if this is the first request ever
            if (!accounts.containsKey(cId1)){
                accounts.put(cId1, 0);
                account_lock.put(cId1, new Object());
            }

            String response;

            //dummy initialization
            nBalance1 = -50;

            switch (action) {
                case '+':
                    synchronized (account_lock.get(cId1)) {

                        oBalance1 = accounts.get(cId1);

                        if (servers_availability[0] == 1){
                            //send message and receive response
                            response = send_to_server_and_get_response(next_server_to_send,request + ",2");

                            if (response.equals(failure_message)){
                                servers_availability[0] = 0;
                                //send new client request to ServerToClient daemon with the current cSocket, and "terminate"
                                end_without_signs = true;
                                new Thread(new MultiServerToClientThread1(cSocket, request)).start();
                                break;
                            }

                            nBalance1 = oBalance1 + amount;
                            accounts.put(cId1, nBalance1);
                            System.out.println("[Thread" +  Thread.currentThread().getId() + "] Client " + cId1 + ", Balance: " + (nBalance1 - amount) + " -> " + nBalance1);

                        }
                        else if(servers_availability[1] == 1){
                            //send message and receive response. Forwarding is 1 since only one more server is up (possibly)
                            next_server_to_send = 1;
                            response = send_to_server_and_get_response(next_server_to_send, request + ",1");

                            if (response.equals(failure_message))
                                servers_availability[1] = 0;

                            //update yourself and leave
                            nBalance1 = oBalance1 + amount;
                            accounts.put(cId1, nBalance1);
                            System.out.println("[Thread" +  Thread.currentThread().getId() + "] Client " + cId1 + ", Balance: " + oBalance1 + " -> " + nBalance1);
                        }
                    }
                    break;

                case '-':
                    synchronized (account_lock.get(cId1)) {

                        oBalance1 = accounts.get(cId1);
                        nBalance1 = oBalance1;
                        //first check the validity of the transaction yourself
                        if ((oBalance1 - amount) < 0){
                            System.out.println("[Thread" +  Thread.currentThread().getId() + "] Transaction request by Client" + cId1 + " aborted. Invalid amount.");
                            break;
                        }

                        if (servers_availability[0] == 1){
                            //send message and receive response
                            response = send_to_server_and_get_response(next_server_to_send, request + ",2");

                            if (response.equals(failure_message)){
                                servers_availability[0] = 0;
                                //send new client request to ServerToClient daemon with the current cSocket, and "terminate"
                                end_without_signs = true;
                                new Thread(new MultiServerToClientThread1(cSocket, request)).start();
                                break;
                            }

                            else if (response.equals("true")){
                                nBalance1 = oBalance1 - amount;
                                accounts.put(cId1, nBalance1);
                                System.out.println("[Thread" +  Thread.currentThread().getId() + "] Client " + cId1 + ", Balance: " + oBalance1 + " -> " + nBalance1);
                            }
                        }
                        else if(servers_availability[1] == 1){
                            //send message and receive response. Forwarding is 1 since only one more server is up (possibly)
                            next_server_to_send = 1;
                            response = send_to_server_and_get_response(next_server_to_send , request + ",1");

                            if (response.equals("false")){
                                System.out.println("[Thread" +  Thread.currentThread().getId() + "] Transaction request by Client" + cId1 + " aborted. Invalid amount.");
                                break;
                            }
                            else if (response.equals(failure_message))
                                servers_availability[1] = 0;

                            //update yourself and leave
                            nBalance1 = oBalance1 + amount;
                            accounts.put(cId1, nBalance1);
                            System.out.println("[Thread" +  Thread.currentThread().getId() + "] Client " + cId1 + ", Balance: " + oBalance1 + " -> " + nBalance1);
                        }
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
                                nBalance1 = oBalance1;
                                //first check the validity of the transaction yourself
                                if (((oBalance1 - amount) < 0) || (cId1 == cId2)){
                                    System.out.println("[Thread" +  Thread.currentThread().getId() + "] Transaction request by Client" + cId1 + " aborted. Invalid amount or Client Id.");
                                    break;
                                }

                                if (servers_availability[0] == 1){
                                    //send message and receive response
                                    response = send_to_server_and_get_response(next_server_to_send, request + ",2");

                                    if (response.equals(failure_message)){
                                        servers_availability[0] = 0;
                                        //send new client request to ServerToClient daemon with the current cSocket, and "terminate"
                                        end_without_signs = true;
                                        new Thread(new MultiServerToClientThread1(cSocket, request)).start();
                                        break;
                                    }

                                    else if (response.equals("true")){
                                        nBalance1 = oBalance1 - amount;
                                        accounts.put(cId1, nBalance1);
                                        nBalance2 = accounts.get(cId2) + amount;
                                        accounts.put(cId2, nBalance2);
                                        System.out.println("[Thread" +  Thread.currentThread().getId() + "] Client " + cId1 + ", Balance: " + oBalance1 + " -> " + nBalance1);
                                    }
                                }
                                else if(servers_availability[1] == 1){
                                    //send message and receive response. Forwarding is 1 since only one more server is up (possibly)
                                    next_server_to_send = 1;
                                    response = send_to_server_and_get_response(next_server_to_send, request + ",1");

                                    if (response.equals("false")){
                                        System.out.println("[Thread" +  Thread.currentThread().getId() + "] Transaction request by Client" + cId1 + " aborted. Invalid amount.");
                                        break;
                                    }
                                    else if (response.equals(failure_message))
                                        servers_availability[1] = 0;

                                    //update yourself and leave
                                    nBalance1 = oBalance1 - amount;
                                    accounts.put(cId1, nBalance1);
                                    nBalance2 = accounts.get(cId2) + amount;
                                    accounts.put(cId2, nBalance2);
                                    System.out.println("[Thread" +  Thread.currentThread().getId() + "] Client " + cId1 + ", Balance: " + oBalance1 + " -> " + nBalance1);
                                }

                            }
                        }
                    }
                    else {
                        synchronized (account_lock.get(cId2)){
                            synchronized (account_lock.get(cId1)) {
                                oBalance1 = accounts.get(cId1);
                                nBalance1 = oBalance1;
                                //first check the validity of the transaction yourself
                                if (((oBalance1 - amount) < 0) || (cId1 == cId2)){
                                    System.out.println("[Thread" +  Thread.currentThread().getId() + "] Transaction request by Client" + cId1 + " aborted. Invalid amount or Client Id.");
                                    break;
                                }

                                if (servers_availability[0] == 1){
                                    //send message and receive response
                                    response = send_to_server_and_get_response(next_server_to_send, request + ",2");

                                    if (response.equals(failure_message)){
                                        servers_availability[0] = 0;
                                        //send new client request to ServerToClient daemon with the current cSocket, and "terminate"
                                        end_without_signs = true;
                                        new Thread(new MultiServerToClientThread1(cSocket, request)).start();
                                        break;
                                    }

                                    else if (response.equals("true")){
                                        nBalance1 = oBalance1 - amount;
                                        accounts.put(cId1, nBalance1);
                                        nBalance2 = accounts.get(cId2) + amount;
                                        accounts.put(cId2, nBalance2);
                                        System.out.println("[Thread" +  Thread.currentThread().getId() + "] Client " + cId1 + ", Balance: " + oBalance1 + " -> " + nBalance1);
                                    }
                                }
                                else if(servers_availability[1] == 1){
                                    //send message and receive response. Forwarding is 1 since only one more server is up (possibly)
                                    next_server_to_send = 1;
                                    response = send_to_server_and_get_response(next_server_to_send, request + ",1");

                                    if (response.equals("false")){
                                        System.out.println("[Thread" +  Thread.currentThread().getId() + "] Transaction request by Client" + cId1 + " aborted. Invalid amount.");
                                        break;
                                    }
                                    else if (response.equals(failure_message))
                                        servers_availability[1] = 0;

                                    //update yourself and leave
                                    nBalance1 = oBalance1 - amount;
                                    accounts.put(cId1, nBalance1);
                                    nBalance2 = accounts.get(cId2) + amount;
                                    accounts.put(cId2, nBalance2);
                                    System.out.println("[Thread" +  Thread.currentThread().getId() + "] Client " + cId1 + ", Balance: " + oBalance1 + " -> " + nBalance1);
                                }
                            }
                        }
                    }
                    break;
                case '?':
                    synchronized (account_lock.get(cId1)){
                        nBalance1 = accounts.get(cId1);
                    }
            }

            if (!end_without_signs){
                sockWriter = new PrintWriter(cSocket.getOutputStream());
                response = "Your new balance is: " + nBalance1;
                sockWriter.println(response);
                sockWriter.flush();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private String send_to_server_and_get_response(int index, String request){
        Socket cSocketSS;
        PrintWriter sockWriterSS;
        Scanner sockReaderSS;

        try {
            //send message and receive response
            cSocketSS = new Socket("localhost", servers_ports[index]);
            sockWriterSS = new PrintWriter(cSocketSS.getOutputStream());
            sockWriterSS.println(request);
            sockWriterSS.flush();
            sockReaderSS = new Scanner(cSocketSS.getInputStream());
            //wait for response... indicates if other servers updated their map
            String response = sockReaderSS.nextLine();
            cSocketSS.close();
            return response;
        }
        catch (Exception e){
            return(failure_message);
        }

    }

}
