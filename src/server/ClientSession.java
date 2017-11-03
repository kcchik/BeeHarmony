package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

/**
 * @author Jonah Shapiro
 * @description This class manages the client sessions
 */
class ClientSession {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String name;

    ClientSession(Socket socket) { //initialize the session
        this.socket = socket;
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * getName
     * @author Jonah Shapiro
     * @description gets the name of the client
     */
    String getName() {
        return name;
    }

    /**
     * setName
     * @author Jonah Shapiro
     * @description Sets the name of the client
     */
    void setName(String name) {
        this.name = name;
    }

    /**
     * write
     * @author Jonah Shapiro
     * @description Sends text to the client
     */
    void write(String msg){
        this.writer.println(msg);
        this.writer.flush();
    }



    /**
     * read
     * @author Jonah Shapiro
     * @description Reads data from the client
     * @return null if socket closed, else the data
     */
    String read(){
        String line = null;
        try {
            line = this.reader.readLine();
        }catch (SocketException e) {
            System.out.println("Socket disconnected");
        } catch (IOException e) {
            System.out.println("IO Error");
        }
        return line;
    }

    /**
     * disconnect
     * @author Jonah Shapiro
     * @description Gracefully shuts down the client
     */
    boolean disconnect(){
        this.write("Quitting...");
        try {
            this.reader.close();
            this.writer.flush();
            this.writer.close();
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }




}
