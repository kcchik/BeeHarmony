package client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;

/**
 * Client
 * Kevin
 */
public class Client extends Application {

    //Class variables
    private Stage primaryStage;
    private BufferedReader reader;
    private PrintWriter writer;
    private HashMap<String, String> beeDictionary;

    private Text chatArea;
    private String chat = "BeeHarmony\u2122";
    private TextField textField;

    private TextField usernameTextField;
    private PasswordField passwordTextField;

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("Connecting...");
        try {
            Socket socket = new Socket("127.0.0.1", 5000); //Create socket
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); //Create input stream
            writer = new PrintWriter(socket.getOutputStream()); //Create output stream
        } catch (Exception e) {
            System.err.println("Error connecting to server");
            e.printStackTrace();
        }
        System.out.println("Connected!");

        beeDictionary = new HashMap<>(); //Initiate bee dictionary
        readDictionary();

        setPrimaryStage(primaryStage);
        loginWindow(primaryStage); //Open login window
    }

    public void close() {
        primaryStage.close();
    }

    private void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * Kevin
     * Reader
     * Displays data from input stream
     */
    public class Reader implements Runnable {
        public void run() {
            while (true) {
                try {
                    String msg = reader.readLine(); //Read from input stream
                    if (msg != null) {
                        if (msg.equals("Quitting...")) {
                            close();
                        } else {
                            chat += "\n" + msg; //Add message to chat
                            chatArea.setText(chat); //Display chat
                        }
                    } else {
                        break; //Stop reading
                    }
                } catch (Exception e) {
                    System.err.println("Error receiving message");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Kevin
     * getMsg
     * Reads one line from input stream
     * @return message
     */
    private String getMsg() {
        try {
            while (true) {
                if (reader.ready()) { //Read from input stream
                    return reader.readLine();
                }
            }
        } catch (Exception e) {
            System.err.println("Error receiving message");
            e.printStackTrace();
        }
        return null;
    }

    private void send() {
        try {
            String msg = textField.getText(); //Get message
            if (msg.equals("")){
                textField.setText(""); //Clear text field
            } else {
                writer.println(msg); //Write message
                writer.flush();
                textField.setText(""); //Clear text field
            }
        } catch (Exception e) {
            System.err.println("Error sending message");
            e.printStackTrace();
        }
    }

    private void translate() {
        String chat = chatArea.getText(); //Get chat
        String newChat = "";
        for (String word : chat.split(" ")) { //Divide into words
            //Construct translated chat
            if (beeDictionary.containsKey(word)) {
                newChat += beeDictionary.get(word);
            } else {
                newChat += word;
            }
            newChat += " ";
        }
        setChat(newChat);
        chatArea.setText(newChat);
    }

    /**
     * Jonah
     * readDictionary
     * Reads the dictionary file
     */
    private void readDictionary() {
        try {
            BufferedReader in = new BufferedReader(new FileReader(new File("src/server/d2.txt")));
            while (in.ready()) {
                String[] curr = in.readLine().split(":");
                this.beeDictionary.put(curr[1], curr[0]);
            }
            in.close();
        } catch (IOException e) {
            System.err.println("IO Error");
        }
    }

    private void setChat(String newChat) {
        chat = newChat;
    }

    private void loginWindow(Stage primaryStage) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Text sceneTitle = new Text("BeeHarmony\u2122");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(sceneTitle, 0, 0, 2, 1);

        Label username = new Label("Username:");
        grid.add(username, 0, 1);

        usernameTextField = new TextField();
        grid.add(usernameTextField, 1, 1);

        Label password = new Label("Password:");
        grid.add(password, 0, 2);

        passwordTextField = new PasswordField();
        grid.add(passwordTextField, 1, 2);

        Button loginButton = new Button("Login");
        Button signUpButton = new Button("Sign Up");
        HBox hb1 = new HBox(10);
        hb1.setAlignment(Pos.BOTTOM_RIGHT);
        hb1.getChildren().add(loginButton);
        hb1.getChildren().add(signUpButton);
        grid.add(hb1, 1, 4);

        loginButton.setOnAction(event -> login(primaryStage));

        signUpButton.setOnAction(event -> signUp(primaryStage));

        Scene scene = new Scene(grid, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void login(Stage primaryStage) {
        try {
            String msg = getMsg(); //Get message
            if (msg != null) {
                if (msg.equals("0")) { //Start authentication
                    String username = usernameTextField.getText(); //Get username
                    writer.println(username); //Write username
                    writer.flush();
                    msg = getMsg(); //Get message
                    switch (msg) {
                        case "1": //Username contains spaces
                            Alert spaceAlert = new Alert(Alert.AlertType.ERROR);
                            spaceAlert.setHeaderText("Username Error");
                            spaceAlert.setContentText("Your username cannot contain spaces!");
                            spaceAlert.showAndWait();
                            break;
                        case "2": //User already connected
                            Alert connectAlert = new Alert(Alert.AlertType.ERROR);
                            connectAlert.setHeaderText("Connection Error");
                            connectAlert.setContentText("That user is already connected!");
                            connectAlert.showAndWait();
                            break;
                        case "5": //User does not exist
                            Alert existError = new Alert(Alert.AlertType.ERROR);
                            existError.setHeaderText("Username Error");
                            existError.setContentText("That user does not exist!");
                            existError.showAndWait();
                            break;
                        case "3": //Enter password
                            String password = passwordTextField.getText(); //Get password
                            writer.println(password); //Write password
                            writer.flush();
                            msg = getMsg(); //Get message
                            if (msg.equals("4")) { //Authenticated
                                chatRoomWindow(primaryStage);
                                new Thread(new Reader()).start(); //Start reader thread
                            } else if (msg.equals("7")) { //Incorrect password
                                Alert passError = new Alert(Alert.AlertType.ERROR);
                                passError.setHeaderText("Password Error");
                                passError.setContentText("Incorrect password!");
                                passError.showAndWait();
                            }
                            break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error writing/receiving message");
            e.printStackTrace();
        }
    }

    private void signUp(Stage primaryStage) {
        try {
            String msg = getMsg(); //Get message
            if (msg != null) {
                if (msg.equals("0")) { //Start authentication
                    writer.println("0"); //Write 0
                    writer.flush();
                    String username = usernameTextField.getText(); //Get username
                    writer.println(username); //Write username
                    writer.flush();
                    msg = getMsg(); //Get message
                    switch (msg) {
                        case "1": //Username contains spaces
                            Alert spaceAlert = new Alert(Alert.AlertType.ERROR);
                            spaceAlert.setHeaderText("Username Error");
                            spaceAlert.setContentText("Your username cannot contain spaces!");
                            spaceAlert.showAndWait();
                            break;
                        case "6": //User already exists
                            Alert existError = new Alert(Alert.AlertType.ERROR);
                            existError.setHeaderText("Username Error");
                            existError.setContentText("That user already exists!");
                            existError.showAndWait();
                            break;
                        case "10": //Enter password for sign up
                            String password = passwordTextField.getText(); //Get password
                            writer.println(password); //Write password
                            writer.flush();
                            msg = getMsg(); //Get message
                            System.out.println(msg);
                            if (msg.equals("4")) { // Enter password
                                chatRoomWindow(primaryStage); //Open chat window
                                new Thread(new Reader()).start(); //Start reader thread
                            }
                            break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error writing/receiving message");
            e.printStackTrace();
        }
    }

    private void chatRoomWindow(Stage primaryStage) {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(1, 1, 1, 1));

        ScrollPane scrollPane = new ScrollPane();
        chatArea = new Text(chat);
        scrollPane.setFitToWidth(true);
        scrollPane.setContent(chatArea);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        pane.setCenter(scrollPane);

        textField = new TextField();
        textField.setPrefWidth(290);

        Button translateButton = new Button("Translate");

        Button sendButton = new Button("Send");

        HBox hb = new HBox();
        hb.getChildren().addAll(translateButton,textField,sendButton);
        pane.setBottom(hb);

        translateButton.setOnAction(event -> translate());

        sendButton.setOnAction(event -> send());

        Scene scene = new Scene(pane, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main (String[] args) {
        launch(args);
    }

}
