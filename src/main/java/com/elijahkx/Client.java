package com.elijahkx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;

import com.elijahkx.constants.commands.CommandConstants;
import com.elijahkx.constants.errors.ErrorConstants;

public class Client {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String nickname;

    private Thread receiveThread;

    private boolean isConnected;

    public Client(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            isConnected = true;
            System.out.println("Connected to server: " + socket);

            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

        } catch (ConnectException e) {
            System.out.println("Connection to the server cannot be made. Make sure the server is started!");
            System.exit(1);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void start() {
        receiveThread = new Thread(this::receiveMessages);
        receiveThread.start();

        try (BufferedReader input = new BufferedReader(new InputStreamReader(System.in))) {
            String message;

            while ((message = input.readLine()) != null && isConnected) {
                if (message.equalsIgnoreCase(CommandConstants.DISCONNECT_COMMAND)) {
                    disconnect();
                    break;
                }

                if (message.startsWith(CommandConstants.NICKNAME_COMMAND_PREFIX)) {
                    handleNicknameCommand(message);
                    continue;
                }

                handleChatMessage(message);
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            String message;

            while ((message = input.readLine()) != null) {

                if (message.startsWith(CommandConstants.NICKNAME_COMMAND_PREFIX)) {
                    setNicknameFromServer(message.substring(CommandConstants.NICKNAME_COMMAND_PREFIX.length()));
                    continue;
                }

                System.out.println(message);
            }
        } catch (IOException e) {
            System.out.println("Server has stopped. Disconnected from the server.");
            isConnected = false;

            e.printStackTrace();
            System.exit(0);
        }
    }

    private void handleNicknameCommand(String message) {
        String newNickname = message.substring(CommandConstants.NICKNAME_COMMAND_PREFIX.length());

        if (newNickname.isEmpty()) {
            System.out.println(ErrorConstants.EMPTY_NICKNAME_ERROR);
            return;
        }

        setNickname(newNickname);
    }

    private void handleChatMessage(String message) {
        if (nickname == null) {
            System.out.println(ErrorConstants.SET_NICKNAME_FIRST_ERROR);
            return;
        }

        sendMessage(message);
    }

    private void sendMessage(String message) {
        output.println(message);
    }

    private void setNickname(String newNickname) {
        output.println(CommandConstants.NICKNAME_COMMAND_PREFIX + newNickname);
    }

    private void setNicknameFromServer(String newNickname) {
        nickname = newNickname;
        System.out.println("Nickname set to: " + nickname);
        System.out.println("You have entered the chat, you can begin typing :)");
    }

    private void disconnect() {
        System.out.println("Disconnecting from the server...");
        sendMessage(CommandConstants.DISCONNECT_COMMAND);
        this.isConnected = false;

        try {
            receiveThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = new Client("localhost", 1234);
        client.start();
    }
}
