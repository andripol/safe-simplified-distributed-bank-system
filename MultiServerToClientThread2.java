/**
 * Created by andri on 28/6/2018.
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class MultiServerToClientThread2 extends MultiServer2 implements Runnable {

    final private Socket cSocket;

    MultiServerToClientThread2(Socket cSocket) {
        this.cSocket = cSocket;
    }

    public void run() {

        Scanner sockReader;
        PrintWriter sockWriter;
        String request;
        String[] request_parts;

        try {
            char action;
            int cId1, amount, cId2;
            int oBalance1, oBalance2;
            int nBalance1, nBalance2;

            sockReader = new Scanner(cSocket.getInputStream());
            request = sockReader.nextLine();
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

            //send messages and receive responses from other servers before proceeding
            Socket cSocketSS;
            PrintWriter sockWriterSS;
            Scanner sockReaderSS;
            String response;

            //dummy initialization
            nBalance1 = -50;

            switch (action) {
                case '+':
                    synchronized (account_lock.get(cId1)) {

                        oBalance1 = accounts.get(cId1);
                        //send message and receive response
                        cSocketSS = new Socket("localhost", servers_ports[0]);
                        sockWriterSS = new PrintWriter(cSocketSS.getOutputStream());
                        sockWriterSS.println(request + ",2");
                        sockWriterSS.flush();
                        sockReaderSS = new Scanner(cSocketSS.getInputStream());
                        //wait for response... indicates if other servers updated their map
                        response = sockReaderSS.nextLine();
                        cSocketSS.close();

                        nBalance1 = oBalance1 + amount;
                        accounts.put(cId1, nBalance1);
                        System.out.println("[Thread" +  Thread.currentThread().getId() + "] Client " + cId1 + ", Balance: " + (nBalance1 - amount) + " -> " + nBalance1);

                    }
                    break;

                case '-':
                    synchronized (account_lock.get(cId1)) {

                        oBalance1 = accounts.get(cId1);
                        //first check the validity of the transaction yourself
                        if ((oBalance1 - amount) < 0){
                            nBalance1 = oBalance1;
                            System.out.println("[Thread" +  Thread.currentThread().getId() + "] Transaction request by Client" + cId1 + " aborted. Invalid amount.");
                            break;
                        }

                        //send message and receive response
                        cSocketSS = new Socket("localhost", servers_ports[0]);
                        sockWriterSS = new PrintWriter(cSocketSS.getOutputStream());
                        sockWriterSS.println(request + ",2");
                        sockWriterSS.flush();
                        sockReaderSS = new Scanner(cSocketSS.getInputStream());
                        //wait for response... indicates if other servers updated their map
                        response = sockReaderSS.nextLine();
                        cSocketSS.close();

                        if (response.equals("true")) {
                            nBalance1 = oBalance1 - amount;
                            accounts.put(cId1, nBalance1);
                        }
                        else {
                            nBalance1 = oBalance1;
                            System.out.println("[Thread" +  Thread.currentThread().getId() + "] Transaction request by Client" + cId1 + " aborted. Invalid amount. Balance is: " + nBalance1);
                        }
                    }

                    break;

                case '>':

                    oBalance1 = accounts.get(cId1);
                    //first check the validity of the transaction yourself
                    if ((oBalance1 - amount) < 0 || (cId1 == cId2)){
                        nBalance1 = oBalance1;
                        System.out.println("[Thread" +  Thread.currentThread().getId() + "] Transaction request by Client" + cId1 + " aborted. Invalid amount or client Id.");
                        break;
                    }

                    if (!accounts.containsKey(cId2)){
                        accounts.put(cId2, 0);
                        account_lock.put(cId2, new Object());
                    }

                    if (cId1 > cId2) {
                        synchronized (account_lock.get(cId1)){
                            synchronized (account_lock.get(cId2)) {
                                cSocketSS = new Socket("localhost", servers_ports[0]);
                                sockWriterSS = new PrintWriter(cSocketSS.getOutputStream());
                                sockWriterSS.println(request + ",2");
                                sockWriterSS.flush();
                                sockReaderSS = new Scanner(cSocketSS.getInputStream());
                                //wait for response... indicates if other servers updated their map
                                response = sockReaderSS.nextLine();
                                cSocketSS.close();

                                if (response.equals("true")) {
                                    nBalance1 = oBalance1 - amount;
                                    accounts.put(cId1, nBalance1);
                                    oBalance2 = accounts.get(cId2);
                                    nBalance2 = oBalance2 + amount;
                                    accounts.put(cId2, nBalance2);
                                    System.out.println("[Thread" +  Thread.currentThread().getId() + "] Client " + cId1 + ", Balance: " + (nBalance1 - amount) + " -> " + nBalance2);
                                }
                                else {
                                    System.out.println("[Thread" +  Thread.currentThread().getId() + "] Transaction request by Client" + cId1 + " aborted. Invalid amount.");
                                }
                            }
                        }
                    }
                    else {
                        synchronized (account_lock.get(cId2)){
                            synchronized (account_lock.get(cId1)) {
                                cSocketSS = new Socket("localhost", servers_ports[0]);
                                sockWriterSS = new PrintWriter(cSocketSS.getOutputStream());
                                sockWriterSS.println(request + ",2");
                                sockWriterSS.flush();
                                sockReaderSS = new Scanner(cSocketSS.getInputStream());
                                //wait for response... indicates if other servers updated their map
                                response = sockReaderSS.nextLine();
                                cSocketSS.close();

                                if (response.equals("true")) {
                                    nBalance1 = oBalance1 - amount;
                                    accounts.put(cId1, nBalance1);
                                    oBalance2 = accounts.get(cId2);
                                    nBalance2 = oBalance2 + amount;
                                    accounts.put(cId2, nBalance2);
                                    System.out.println("[Thread" +  Thread.currentThread().getId() + "] Client " + cId1 + ", Balance: " + (nBalance1 - amount) + " -> " + nBalance2);
                                }
                                else {
                                    System.out.println("[Thread" +  Thread.currentThread().getId() + "] Transaction request by Client" + cId1 + " aborted. Invalid amount.");
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

            sockWriter = new PrintWriter(cSocket.getOutputStream());
            response = "Your new balance is: " + nBalance1;
            sockWriter.println(response);
            sockWriter.flush();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }


}
