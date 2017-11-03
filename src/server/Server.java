package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Jonah Shapiro
 * @version 3
 */
public class Server {

    private int numClients; // keeps track of the number of connected clients

    private ServerSocket listener; // the listening socket

    private boolean running; // boolean for the mail loop

    private HashMap<String, String> users; // name, password

    private HashMap<String, ClientSession> clients; // name, session

    private HashMap<String, String> beeDictionary; // english, bee

    private ArrayList<ClientHandler> threads; // a list of clientHandler threads. Necessary for
    // kicking users.

    /**
     * @author Jonah Shapiro
     */
    private Server() { // initialize the server
        this.numClients = 0;
        try {
            int PORT = 5000;
            this.listener = new ServerSocket(PORT); // create the server socket
            // the following code initializes the collections
            this.clients = new HashMap<>();
            this.users = new HashMap<>();
            this.beeDictionary = new HashMap<>();
            this.threads = new ArrayList<>();
            readDictionary(); // read the dictionary for the bee translation
            readUsers(); // read the users
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * readDictionary
     *
     * @author Jonah Shapiro
     * @description reads the bee translation dictionary
     */
    private void readDictionary() {
        try {
            BufferedReader in = new BufferedReader(new FileReader(new File("src/server/d2.txt")));
            while (in.ready()) {
                String[] curr = in.readLine().split(":");
                this.beeDictionary.put(curr[0], curr[1]);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * readUsers
     *
     * @author Jonah Shapiro
     * @description Reads the users from users.dat
     */
    private void readUsers() {
        try {
            BufferedReader in = new BufferedReader(new FileReader(new File("src/server/users.dat")));
            while (in.ready()) {
                String line = in.readLine();
                String[] user = line.split(":");
                this.users.put(user[0], user[1]);
            }
            in.close();
        } catch (IOException e) {
            System.err.println("IO error!");
            e.printStackTrace();
        }
    }

    /**
     * writeUsers
     * @author Jonah Shapiro
     * @description Writes the users to users.dat
     */
    private void writeUsers() {
        try {
            PrintWriter out = new PrintWriter("src/server/users.dat");
            for (String name : users.keySet()) {
                String user = "";
                user += name + ":" + users.get(name);
                out.println(user);
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            System.err.println("IO error!");
            e.printStackTrace();
        }
    }

    /**
     * run
     * @author Jonah Shapiro
     * @description runs the server
     */
    private void run() {
        System.out.println("Server starting!");
        this.running = true;
        while (running) {
            int MAX_CONNECTIONS = 100;
            if (numClients <= MAX_CONNECTIONS) { // check for maximum channel occupancy
                try {
                    Socket client = listener.accept();
                    System.out.println("connect");
                    ClientHandler t = new ClientHandler(client);
                    new Thread(t).start();
                    this.threads.add(t);
                    this.numClients++;
                } catch (SocketException e) {
                    if (!running) { // check if the socket was supposed to close
                        System.out.println("Socket closed from server shutdown");
                    } else {
                        System.err.println("Socket closed.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * shutdown
     * @author Jonah Shapiro
     * @description shuts down the server
     */
    private void shutdown() {
        System.out.println("Server shutting down!");
        this.running = false;
        for (ClientHandler g : this.threads) { // disconnect all clients
            g.running = false; // make sure each client loop stops
            g.client.disconnect();
        }
        try {
            this.listener.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * send
     * @author Jonah Shapiro
     * @description sends a message to all clients
     * @param msg the message to send
     * @param name the name of the sender
     */
    private synchronized void send(String msg, String name) {
        for (ClientSession session : clients.values()) {
            if (name.equals("Server")) { // if it is a server broadcast skip bee translation
                session.write(name + ": " + msg);
            } else {
                String[] temp = msg.split(" ");
                String finalMessage = "";
                for (String aTemp : temp) {
                    if (this.beeDictionary.containsKey(aTemp)) {
                        finalMessage += this.beeDictionary.get(aTemp) + " ";
                    } else if (this.beeDictionary.containsKey(aTemp.toLowerCase())) {
                        finalMessage += this.beeDictionary.get(aTemp.toLowerCase()) + " ";
                    } else {

                        finalMessage += aTemp + " ";
                    }
                }
                session.write(name + ": " + finalMessage); // send the message
            }
        }
    }

    /**
     * main
     * @author Jonah Shapiro
     * @description You know what this is
     */
    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }

    /**
     * @author Jonah Shapiro
     * @description the client handler inner class, manages client threads
     */
    public class ClientHandler implements Runnable {

        private ClientSession client;
        private boolean running;
        private boolean admin;

        /**
         * @author Jonah Shapiro
         * @description initializes a client thread
         */
        ClientHandler(Socket client) {
            this.client = new ClientSession(client);
            String name = authenticate(); // authenticate the client
            if (name.equals("-1")) { // if the name returns a disconnect code
                System.err.println("Client disconnected during auth.");
                numClients--;
                running = false;
                return;
            } else if (name.equals("jonah")) { // jonah is the admin
                this.admin = true;
            }
            this.client.setName(name);
            clients.put(this.client.getName(), this.client);
            System.out.println("Client Connected with name " + this.client.getName());
            send((this.client.getName() + " has connected."), "Server");
            this.running = true;
            listClients();
        }

        /**
         * listClients
         * @author Jonah Shapiro
         * @description list the users in the chat
         */
        void listClients() {
            this.client.write("Users in chat:");
            this.client.write("------------------------------------");
            for (String s : clients.keySet()) {
                this.client.write(s);
            }
            this.client.write("------------------------------------");
        }

        public void run() {
            if (!running) { // this is in case the client disconnects during auth
                return;
            }
            while (running) {
                String line = this.client.read();
                if (line != null) { // if the socket is connected
                    if (line.startsWith("/")) { // is it a command?
                        command(line.substring(1));
                    } else {
                        send(line, this.client.getName());
                    }
                } else { // if the socket closes gracefully kill everything
                    this.client.disconnect();
                    clients.remove(this.client.getName());
                    send((this.client.getName() + " has left the channel. Reason: Disconnected"), "Server");
                    System.out.println(this.client.getName() + " disconnected");
                    numClients--;
                    this.running = false;
                    break;
                }
            }

        }

        /**
         * command
         * @author Jonah Shapiro
         * @description handles commands sent to the server
         * @param msg
         *            the command to be handled
         */
        void command(String msg) {
   /*
    * Available commands:
    * quit: disconnects the client from the server
    * slap [user]: sends "[sender] slaps [user] with a large trout"
    * nick: changes the users nickname
    * pm: privately messages another user. Syntax: "/pm jonah [message]"
    * list: lists the users in the chat
    * help: prints this list of commands
    *
    * Admins get access to the following commands:
    * broadcast: sends a message as the server
    * shutdown: shuts down the server
    *
    */
            String[] command = msg.split(" ");
            switch (command[0]) {
                case "quit":
                    this.client.disconnect();
                    clients.remove(this.client.getName());
                    send((this.client.getName() + " has left the channel."), "Server");
                    this.running = false;
                    numClients--;
                    break;

                case "slap":
                    send((this.client.getName() + " slaps " + command[1] + " with a large trout"), "Server");
                    break;

                case "nick":
                    if (command.length > 2) {
                        this.client.write("Name cannot contain spaces!");
                        break;
                    }
                    String name = command[1];
                    if (!users.containsKey(name)) {
                        clients.put(name, clients.remove(this.client.getName()));
                        users.put(name, users.remove(this.client.getName()));
                        this.client.setName(name);
                        writeUsers();
                    } else {
                        this.client.write("Name already in use.");
                    }
                    break;

                case "pm":
                    String message = "";
                    for (int i = 2; i < command.length; i++) {
                        message += command[i] + " ";
                    }
                    String target = command[1];
                    clients.get(target).write("PM from " + this.client.getName() + ": " + message);
                    break;

                case "help":
                    this.client.write("Available commands\nquit: disconnects the client from the server\nslap [user]: sends \"[sender] slaps [user] with a large trout\"\nnick: changes the users nickname\npm: privately messages another user. Syntax: \"/pm jonah [message]\"\nhelp: prints this list of commands\nlist: lists the users in the chat.");
                    break;

                case "list":
                    listClients();
                    break;

                case "broadcast":
                    if (this.admin) {
                        String bc = "";
                        for (int i = 1; i < command.length; i++) {
                            bc += command[i] + " ";
                        }
                        send(bc, "Server");
                    }
                    break;

                case "shutdown":
                    if (this.admin) {
                        send("Server is shutting down!", "Server");
                        shutdown();
                    }
                    break;

                default:
                    this.client.write("Invalid Command");
                    break;
            }
        }

        /**
         * authenticate
         * @author Jonah Shapiro
         * @description authenticates the user
         */
        String authenticate() {
            String name;
            try {
                while (true) {
                    this.client.write("0");
                    name = this.client.read();
                    if (name.equals("0")) {
                        name = this.client.read();
                        if (name.contains(" ")) {
                            this.client.write("1"); // the name contains spaces
                        } else if (users.containsKey(name)) {
                            this.client.write("6"); // user already exists
                        } else {
                            createUser(name);
                            return name;
                        }
                    } else if (name.contains(" ")) {
                        this.client.write("1"); // the name contains spaces
                    } else if (clients.containsKey(name)) {
                        this.client.write("2"); // that user is already connected
                    } else if (users.containsKey(name)) {
                        this.client.write("3"); // enter password
                        String pass = this.client.read();
                        if (users.get(name).equals(md5(pass))) {
                            this.client.write("4"); // authentication successful
                            this.client.write("Welcome, " + name);
                            return name;
                        } else {
                            this.client.write("7"); //incorrect password
                        }
                    } else {
                        this.client.write("5"); //user doesn't exist
                    }
                }
            } catch (Exception e) {
                return "-1";
            }
        }

        /**
         * createUser
         * @author Jonah Shapiro
         * @description Creates a user
         */
        void createUser(String name) {
            this.client.write("10");
            String pass = md5(this.client.read());
            users.put(name, pass);
            this.client.write("4");
            writeUsers();
        }

        /**
         * md5
         *
         * @author Jonah Shapiro
         * @description creates an MD5-encrypted string
         */
        String md5(String md5) {
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                byte[] array = md.digest(md5.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte anArray : array) {
                    sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
                }
                return sb.toString();
            } catch (java.security.NoSuchAlgorithmException e) {
                System.err.println("MD5 not found!");
            }
            return null;
        }

    }

}
