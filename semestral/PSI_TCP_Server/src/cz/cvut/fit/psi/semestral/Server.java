package cz.cvut.fit.psi.semestral;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Socket programming idea is taken from: https://www.geeksforgeeks.org/socket-programming-in-java/
 */
public class Server {
    public static void main(String[] args) {
        ServerSocket server = null;

        //Starts server and waits for a connection
        try {
            server = new ServerSocket(Constants.PORT);
            System.out.println("Server started on port: " + Constants.PORT);

            System.out.println("Waiting for a client ...");

            while (true) {
                Socket client = server.accept();
                System.out.println("Client accepted: " + client.getInetAddress().getHostAddress());

                Handler handler = new Handler(client);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}