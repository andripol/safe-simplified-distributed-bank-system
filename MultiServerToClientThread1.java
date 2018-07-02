/**
 * Created by andri on 2/7/2018.
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class MultiServerToClientThread1 extends MultiServer1 implements Runnable {

    final private Socket cSocket;
    final private String failure_message = "failed to connect to server";
    private String request;

    MultiServerToClientThread1(Socket cSocket) {
        this.cSocket = cSocket;
    }

    public void run() {

        //System.out.println("[Thread:" +  Thread.currentThread().getId() + "]New client connection established");

        Scanner sockReader;
        PrintWriter sockWriter;
        String[] request_parts;

        boolean is_minority = false;
        int account_just_created = 0;

        //default 0. if the successor server is down change it to forward to the one before
        int next_server_to_send = 0;

        try {
            char action;
            int cId1, amount, cId2;
            int oBalance1;
            int nBalance1;

            sockReader = new Scanner(cSocket.getInputStream());
            request = sockReader.nextLine();


            //System.out.println("[Thread:" +  Thread.currentThread().getId() + "]Server 1 " + ", Client Request: " + request);
            request_parts = request.split(",");

            action = request_parts[0].charAt(0);
            cId1 = Integer.parseInt(request_parts[1]);
            amount = Integer.parseInt(request_parts[2]);
            cId2 = Integer.parseInt(request_parts[3]);

            //create account if this is the first request ever
            if (cId1 >=0 && !accounts.containsKey(cId1)){
                accounts.put(cId1, 0);
                account_lock.put(cId1, new Object());
                account_just_created = cId1;
            }

            String response, response1, response2 = "false";

            //dummy initialization
            nBalance1 = 0;

            switch (action) {
                case '+':

                    synchronized (account_lock.get(cId1)) {
                        oBalance1 = accounts.get(cId1);

                        response1 = send_to_server_and_get_response(next_server_to_send,request + ",2");

                        if (response1.equals(failure_message)) {
                            response2 = send_to_server_and_get_response(next_server_to_send, request + ",1");
                            if (response2.equals(failure_message)) {
                                is_minority = true;
                                System.out.println("No majority. Request aborted.");
                                break;
                            }
                        }

                        if (response1.equals("true") || response2.equals("true")){
                            nBalance1 = oBalance1 + amount;
                            accounts.put(cId1, nBalance1);
                        }
                        //System.out.println("[Thread" +  Thread.currentThread().getId() + "] Client " + cId1 + ", Balance: " + (nBalance1 - amount) + " -> " + nBalance1);
                    }
                    break;

                case '-':
                    if (!accounts.containsKey(cId1)){
                        System.out.println("transaction aborted. No such account.");
                        break;
                    }

                    synchronized (account_lock.get(cId1)) {
                        oBalance1 = accounts.get(cId1);
                        nBalance1 = oBalance1;

                        if ((oBalance1 - amount) < 0){
                            System.out.println("Transaction Aborted. Invalid amount");
                            break;
                        }

                        response1 = send_to_server_and_get_response(next_server_to_send,request + ",2");

                        if (response1.equals(failure_message)) {
                            response2 = send_to_server_and_get_response(next_server_to_send, request + ",1");
                            if (response2.equals(failure_message)) {
                                is_minority = true;
                                System.out.println("All other servers are down. No majority. Request aborted.");
                                break;
                            }
                        }

                        if (response1.equals("true") || response2.equals("true")){
                            nBalance1 = oBalance1 - amount;
                            accounts.put(cId1, nBalance1);
                        }
                        //System.out.println("[Thread" +  Thread.currentThread().getId() + "] Client " + cId1 + ", Balance: " + (nBalance1 - amount) + " -> " + nBalance1);
                    }

                    break;

                case '>':

                    if (!accounts.containsKey(cId2)){
                        accounts.put(cId2, 0);
                        account_lock.put(cId2, new Object());
                    }

                    //avoid deadlock
                    if (cId1 > cId2){
                        synchronized (account_lock.get(cId1)) {
                            synchronized (account_lock.get(cId2)) {
                                oBalance1 = accounts.get(cId1);
                                //if anything goes wrong, new balance is equal to the old balance
                                nBalance1 = oBalance1;

                                if ((oBalance1 - amount) < 0){
                                    System.out.println("Transaction Aborted. Invalid amount");
                                    break;
                                }

                                response1 = send_to_server_and_get_response(next_server_to_send,request + ",2");

                                if (response1.equals(failure_message)) {
                                    response2 = send_to_server_and_get_response(next_server_to_send, request + ",1");
                                    if (response2.equals(failure_message)) {
                                        is_minority = true;
                                        System.out.println("All other servers are down. No majority. Request aborted.");
                                        break;
                                    }
                                }

                                if (response1.equals("true") || response2.equals("true")){
                                    nBalance1 = oBalance1 - amount;
                                    accounts.put(cId1, nBalance1);
                                    accounts.put(cId2, accounts.get(cId2) + amount);
                                }

                            }
                        }
                    }
                    else {
                        synchronized (account_lock.get(cId2)) {
                            synchronized (account_lock.get(cId1)) {
                                oBalance1 = accounts.get(cId1);
                                nBalance1 = oBalance1;
                                //first check the validity of the transaction yourself
                                if ((oBalance1 - amount) < 0){
                                    //System.out.println("Transaction Aborted. Invalid amount");
                                    break;
                                }

                                response1 = send_to_server_and_get_response(next_server_to_send,request + ",2");

                                if (response1.equals(failure_message)) {
                                    response2 = send_to_server_and_get_response(next_server_to_send, request + ",1");
                                    if (response2.equals(failure_message)) {
                                        is_minority = true;
                                        System.out.println("All other servers are down. No majority. Request aborted.");
                                        break;
                                    }
                                }

                                if (response1.equals("true") || response2.equals("true")){
                                    nBalance1 = oBalance1 - amount;
                                    accounts.put(cId1, nBalance1);
                                    accounts.put(cId2, accounts.get(cId2) + amount);
                                }

                            }
                        }
                    }
                    break;
                case '?':
                    synchronized (account_lock.get(cId1)) {
                        oBalance1 = accounts.get(cId1);

                        response1 = send_to_server_and_get_response(next_server_to_send,request + ",2");

                        if (response1.equals(failure_message)) {
                            response2 = send_to_server_and_get_response(next_server_to_send, request + ",1");
                            if (response2.equals(failure_message)) {
                                is_minority = true;
                                System.out.println("No majority. Request aborted.");
                                break;
                            }
                        }

                        if (response1.equals("true") || response2.equals("true")){
                            nBalance1 = accounts.get(cId1);
                        }
                        //System.out.println("[Thread" +  Thread.currentThread().getId() + "] Client " + cId1 + ", Balance: " + (nBalance1 - amount) + " -> " + nBalance1);
                    }

            }

            sockWriter = new PrintWriter(cSocket.getOutputStream());
            response = "Your new balance is: " + nBalance1;
            sockWriter.println(response);
            sockWriter.flush();

            if (is_minority && (account_just_created >= 0)){
                accounts.remove(account_just_created);
                account_lock.remove(account_just_created);
            }

            System.out.println("HashMap of server 1: ");
            System.out.println(accounts.size());
            System.out.println(Arrays.asList(accounts));
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
