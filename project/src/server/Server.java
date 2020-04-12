package server;

import client.Client;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * This class handles connection requests from clients and creates
 * a new instance of ClientHandler for each new client, and adds it
 * a shared list
 * @author magnubau
 */
public class Server {
    /**
     *This main method goes in loops waiting for new connections to
     * handle
     *
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        final int PORTNR = 3000;
        int id = 0;

        System.out.println("Server is running. Waiting for clients...");
        ServerSocket server = new ServerSocket(PORTNR);
        ArrayList<ClientHandler> participants = new ArrayList<>();
        boolean wait = true;
        while(wait){

            ExecutorService executor = Executors.newCachedThreadPool();
            Callable<Object> waiter = new Callable<Object>() {
                public Object call() throws IOException {
                    return server.accept();
            }
            };
            Future<Object> promise = executor.submit(waiter);
            try {
                Object res = promise.get(5, TimeUnit.SECONDS);
                participants.add(new ClientHandler((Socket)res, id));
                System.out.println("Client connected.\nClient id: " + id + "\nNumber of clients: " + participants.size() + "\n");
            } catch (TimeoutException toe) {
                System.out.println("Stopped waiting for clients");
                wait = false;
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                wait = false;
            } catch (ExecutionException ee) {
                ee.printStackTrace();
                wait = false;
            } finally {
                promise.cancel(true);
            }
            id++;
        }
        System.out.println("Donn Morison");
        Coordinator coordinator = new Coordinator(participants);
        server.close();

    }
}
