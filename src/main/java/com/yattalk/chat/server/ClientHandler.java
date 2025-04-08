package com.yattalk.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class ClientHandler implements Runnable {

    private Socket socket;
    private Server server;
    private PrintWriter writer;
    private String nickname;
    private String currentChatWith;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            writer = new PrintWriter(socket.getOutputStream(), true);

            // Get nickname
            nickname = reader.readLine();
            server.addClient(nickname, this);

            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println(this.currentChatWith);
                if (message.startsWith("ACCEPT_CHAT:")) {
                    String requester = message.substring(12);
                    server.acceptPrivateChat(nickname, requester);
                } else if (message.startsWith("REJECT_CHAT:")) {
                    String requester = message.substring(12);
                    if (server.getClients().containsKey(requester)) {
                        server.getClients().get(requester).sendMessage("CHAT_REJECTED:" + nickname);
                    }
                } else if (message.startsWith("END_CHAT:")) {
                    String otherUser = message.substring(9);
                    currentChatWith = null;
                    if (server.getClients().containsKey(otherUser)) {
                        server.getClients().get(otherUser).sendMessage("CHAT_ENDED:" + nickname);
                    }
                } else if (message.startsWith("PRIVATE:")) {
                    String[] parts = message.split(":", 3);
                    if (parts.length == 3) {
                        String receiver = parts[1];
                        String msg = parts[2];
                        if (server.startPrivateChat(nickname, receiver, msg)) {
                            currentChatWith = receiver;
                            server.logMessage(nickname, receiver, msg);
                        }
                    }
                } else if (this.currentChatWith != null) {
                    server.logMessage(nickname, currentChatWith, message);
                    sendToReceiver(currentChatWith, message);
                    // sendToReceiver(nickname, "> " + message);

                } else if (message.startsWith("CHAT_ACCEPTED:")) {
                    currentChatWith = message.substring(14);
                    System.out.println("\nChat with " + currentChatWith + " has started!");
                } else if (message.startsWith("CREATE_GROUP:")) {
                    String[] users = message.substring(13).split(",");
                    String groupId = "group_" + System.currentTimeMillis(); // Simple unique ID

                    Set<String> members = new HashSet<>();
                    for (String user : users) {
                        user = user.trim();
                        if (!user.isEmpty()) {
                            members.add(user);
                        }
                    }
                    members.add(nickname); // Include group creator

                    server.createGroup(groupId, members);

                    for (String user : members) {
                        if (server.getClients().containsKey(user)) {
                            server.getClients().get(user).sendMessage("GROUP_INVITE:" + groupId + ":" + String.join(",", members));
                        }
                    }
                } else if (message.startsWith("ACCEPT_GROUP:")) {
                    String groupId = message.substring(13);
                    server.confirmGroupJoin(groupId, nickname);
                } else if (message.startsWith("GROUP_MSG:")) {
                    String[] parts = message.split(":", 3);
                    if (parts.length == 3) {
                        String groupId = parts[1];
                        String msg = parts[2];
                        server.broadcastGroupMessage(groupId, nickname, msg);
                    }
                }

            }
        } catch (IOException e) {
            System.err.println("ClientHandler exception: " + e.getMessage());
        } finally {
            server.removeClient(nickname);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendToReceiver(String receiver, String message) {
        if (server.getClients().containsKey(receiver)) {
            server.getClients().get(receiver).sendMessage("PRIVATE_MSG:" + nickname + ":" + message);
        }
    }

    public void setCurrentChatWith(String nickname) {
        this.currentChatWith = nickname;
    }

    public void sendMessage(String message) {
        writer.println(message);
    }

    public String getNickname() {
        return nickname;
    }
}
