/** Created by andri **/
/** June 2018   **/

import java.io.*;
import java.net.*;
import java.util.*;

class ClientRun implements Runnable {

    final private int cId1, amount, cId2;
    final private  char action;

    ClientRun(int cId1, char action, int amount, int cId2) {
        this.cId1 = cId1;
        this.action = action;
        this.amount = amount;
        this.cId2 = cId2;
    }

    public void run() {

        //final int requests = 100;
        final Random rand = new Random();

        Socket cSocket;
        PrintWriter sockWriter;
        Scanner sockReader;

        String request; /* request = "Action,Client1,Amount,Client2"; */

        int sId;
        String response;
        //int balance = 500;

        try {
            sId = rand.nextInt(3) + 1; //we have 3 server daemons
            System.out.println("\nWill be served by server:" + sId + "\n");
            cSocket = new Socket("localhost", 4000 + sId);

            switch (action) {
                case '+':
                    request = "+," + cId1 + "," + amount + ",-1";
                    break;
                case '-':
                    request = "-," + cId1 + "," + amount + ",-1";
                    break;
                case '>':
                    request = ">," + cId1 + "," + amount + "," + cId2;
                    break;
                case '?':
                    request = "?," + cId1 + ",0" + ",-1";
                    break;
                default:
                    request = "?," + cId1 + ",0" + ",-1";
                    break;
            }

            //System.out.println("Client " + cId1 + ", Request " + request + " to Server " + sId);

            //send request
            sockWriter = new PrintWriter(cSocket.getOutputStream());
            sockWriter.println(request);
            sockWriter.flush();

            //wait for the server to response before running the next request
            sockReader = new Scanner(cSocket.getInputStream());
            response = sockReader.nextLine();
            //System.out.println("Your balance is equal to: " + response);
            System.out.println(response);

            cSocket.close();

        } catch (Exception e) {
            new Thread(new ClientRun(cId1, action, amount, cId2 )).start();
        }
    }
}

public class Client {

    public static void main(String[] args) {

        int cId1, cId2, amount;
        char action;

        Scanner in = new Scanner(System.in);
        System.out.printf("Enter Client Id:  ");
        cId1 = in.nextInt();
        while (true){
            try{
                System.out.printf("Enter Type of transaction ('+', '-', '>', '?'):  ");
                action = in.next().charAt(0);
                if (action == '>') {
                    System.out.printf("Enter second Client Id :  ");
                    cId2 = in.nextInt();
                }
                else { cId2 = 0; }

                if (action != '?') {
                    System.out.printf("Enter amount:  ");
                    amount = in.nextInt();
                }
                else { amount = 0; }

                new Thread(new ClientRun(cId1, action, amount, cId2 )).start();
            }

            catch(Exception e){
                System.out.println("Wrong type of transaction. Please try again.\n");
            }
        }
    }
}