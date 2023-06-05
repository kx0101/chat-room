package com.elijahkx;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;

import com.elijahkx.utils.messages.MessagesUtils;
import com.elijahkx.utils.messages.MessagesUtils.MessageType;

import com.elijahkx.constants.commands.CommandConstants;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private PrintWriter output;
    private List<ClientHandler> clients;
    private String nickname;

    public Socket getClientSocket() {
        return clientSocket;
    }

    public ClientHandler(Socket socket, List<ClientHandler> clients) {
        this.clientSocket = socket;
        this.clients = clients;
    }

    private void broadcastMessage(String message, MessageType messageType) {

        clients.stream()
                .filter(client -> client != this && !message.startsWith("/"))
                .forEach(client -> client.output.println(MessagesUtils.getMessageContent(message, messageType, nickname)));
    }

    public void run() {
        try (PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            this.output = output;

            String message;

            while ((message = input.readLine()) != null) {
                if (isDisconnectCommand(message)) {
                    break;
                }

                if (isNicknameCommand(message)) {
                    handleNicknameCommand(message);
                    continue;
                }

                handleChatMessage(message);
            }

            handleDisconnect();
        } catch (IOException e) {
            handleUnexpectedDisconnect();
        }
    }

    private void handleNicknameCommand(String message) {
        String newNickname = message.substring(10);

        if (newNickname.isEmpty()) {
            output.println("Nickname cannot be empty.");
        } else if (checkNicknameAvailability(newNickname)) {
            setNickname(newNickname);
            output.println("/nickname " + newNickname);

            broadcastMessage("has joined the chat", MessageType.JOIN);
        } else {
            output.println("Nickname already taken. Please choose a different nickname.");
        }
    }

    private void handleChatMessage(String message) {
        System.out.println("Received message from client " + nickname + ": " + message);

        broadcastMessage(message, MessageType.NORMAL);
    }

    private void handleDisconnect() {
        System.out.println("Client disconnected: " + clientSocket);
        broadcastMessage("has left the chat.", MessageType.LEAVE);

        clients.remove(this);
        closeClientSocket();
    }

    private void handleUnexpectedDisconnect() {
        System.out.println("Client disconnected unexpectedly: " + clientSocket);
        clients.remove(this);
    }

    private void closeClientSocket() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setNickname(String newNickname) {
        nickname = newNickname;

        System.out.println("Client " + clientSocket + " set nickname to: " + nickname);
    }

    private boolean checkNicknameAvailability(String newNickname) {
        return clients.stream()
                .noneMatch(client -> client != this && client.nickname != null && client.nickname.equals(newNickname));
    }

    private boolean isDisconnectCommand(String message) {
        return message.equalsIgnoreCase(CommandConstants.DISCONNECT_COMMAND);
    }

    private boolean isNicknameCommand(String message) {
        return message.startsWith(CommandConstants.NICKNAME_COMMAND_PREFIX);
    }

    @Override
    public String toString() {
        return nickname;
    }
}
