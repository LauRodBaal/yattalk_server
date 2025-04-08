package com.yattalk.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.yattalk.chat.model.ClientData;

public class Server {

    private static final int PORT = 12345;
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private ClientData clientData = new ClientData();

    private Map<String, Set<String>> groups = new ConcurrentHashMap<>();

    public void createGroup(String groupId, Set<String> members) {
        groups.put(groupId, members);
    }

    public void confirmGroupJoin(String groupId, String nickname) {
        groups.getOrDefault(groupId, new HashSet<>()).add(nickname);
    }

    public void broadcastGroupMessage(String groupId, String sender, String message) {
        Set<String> members = groups.get(groupId);
        if (members == null) {
            return;
        }

        for (String member : members) {
            if (!member.equals(sender) && clients.containsKey(member)) {
                clients.get(member).sendMessage("GROUP_MSG:" + groupId + ":" + sender + ":" + message);
            }
        }
    }

    // Add this getter method
    public Map<String, ClientHandler> getClients() {
        return clients;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat Server is listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                ClientHandler clientHandler = new ClientHandler(socket, this);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addClient(String nickname, ClientHandler handler) {
        clients.put(nickname, handler);
        System.out.println("Added client: " + nickname);
        broadcastClientList();

        // Send chat history with all other clients
        clients.keySet().forEach(otherClient -> {
            if (!otherClient.equals(nickname)) {
                List<String> history = clientData.getChatHistory(nickname, otherClient);
                if (!history.isEmpty()) {
                    handler.sendMessage("CHAT_HISTORY:" + otherClient + ":" + String.join("|", history));
                }
            }
        });
    }

    public void removeClient(String nickname) {
        clients.remove(nickname);
        System.out.println("Removed client: " + nickname);
        broadcastClientList();
    }

    public void broadcastClientList() {
        String clientList = "CLIENT_LIST:" + String.join(",", clients.keySet());
        clients.values().forEach(client -> client.sendMessage(clientList));
    }

    public boolean startPrivateChat(String sender, String receiver, String initialMessage) {
        ClientHandler senderHandler = clients.get(sender);
        ClientHandler receiverHandler = clients.get(receiver);

        if (receiverHandler == null) {
            senderHandler.sendMessage("User " + receiver + " not found.");
            return false;
        }

        // Send a chat request to the receiver
        receiverHandler.sendMessage("CHAT_REQUEST:" + sender + ":" + initialMessage);
        return true;
    }

    public void sendPrivateMessage(String sender, String receiver, String message) {
        if (clients.containsKey(receiver)) {
            clients.get(receiver).sendMessage("PRIVATE_MSG:" + sender + ":" + message);
            logMessage(sender, receiver, message);
        }
    }

    public void logMessage(String sender, String receiver, String message) {
        clientData.addMessage(sender, receiver, sender, message);
    }

    public List<String> getChatHistory(String user1, String user2) {
        return clientData.getChatHistory(user1, user2);
    }

    public boolean acceptPrivateChat(String acceptor, String requester) {
        if (clients.containsKey(requester)) {
            ClientHandler requesterHandler = clients.get(requester);
            ClientHandler acceptorHandler = clients.get(acceptor);

            // Notify both users
            requesterHandler.sendMessage("CHAT_ACCEPTED:" + acceptor);
            acceptorHandler.sendMessage("CHAT_ACCEPTED:" + requester);

            // Set current chat partners
            requesterHandler.setCurrentChatWith(acceptor);
            acceptorHandler.setCurrentChatWith(requester);

            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        new Server().start();
    }
}
