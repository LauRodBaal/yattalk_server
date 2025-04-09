package com.yattalk.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private String nickname;
    private String currentChatWith;
    private Scanner scanner = new Scanner(System.in);

    public void start() throws IOException {
        System.out.print("Enter your nickname: ");
        nickname = scanner.nextLine();

        Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

        writer.println(nickname);

        new Thread(new ServerListener(socket)).start();

        while (true) {
            String input = scanner.nextLine();

            if (currentChatWith == null) {
                if (input.startsWith("/accept")) {
                    String requester = input.substring(8);
                    writer.println("ACCEPT_CHAT:" + requester);
                } else if (input.startsWith("/reject")) {
                    String requester = input.substring(8);
                    writer.println("REJECT_CHAT:" + requester);
                } else if (input.startsWith("/chat")) {
                    String[] parts = input.split(" ", 3);
                    if (parts.length == 3) {
                        writer.println("PRIVATE:" + parts[1] + ":" + parts[2]);
                    }
                } else if (input.startsWith("/group ")) {
                    String[] users = input.substring(7).split(" ");
                    writer.println("CREATE_GROUP:" + String.join(",", users));
                } else if (input.startsWith("/gaccept ")) {
                    String groupId = input.substring(9);
                    writer.println("ACCEPT_GROUP:" + groupId);
                } else if (input.startsWith("/gmsg ")) {
                    String[] parts = input.split(" ", 3);
                    if (parts.length == 3) {
                        writer.println("GROUP_MSG:" + parts[1] + ":" + parts[2]);
                    }
                }

            } else {
                if ("/exit".equals(input)) {
                    writer.println("END_CHAT:" + currentChatWith);
                    currentChatWith = null;
                    System.out.println("Chat ended");
                } else {
                    writer.println(input);
                }
            }
        }
    }

    private class ServerListener implements Runnable {

        private BufferedReader reader;

        public ServerListener(Socket socket) {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.startsWith("CLIENT_LIST:")) {
                        System.out.println("\nConnected clients: " + message.substring(12));
                    } else if (message.startsWith("PRIVATE_MSG:")) {
                        String[] parts = message.split(":", 3);
                        System.out.println(parts[1] + ": " + parts[2]);
                    } else if (message.startsWith("CHAT_REQUEST:")) {
                        String[] parts = message.split(":", 3);
                        System.out.println("\n" + parts[1] + " wants to chat with you: " + parts[2]);
                        System.out.println("Type '/accept " + parts[1] + "' to accept or '/reject " + parts[1] + "' to reject");
                    } else if (message.startsWith("CHAT_ACCEPTED:")) {
                        currentChatWith = message.substring(14);
                        System.out.println("\nChat with " + currentChatWith + " has started!");
                    } else if (message.startsWith("CHAT_REJECTED:")) {
                        System.out.println("\n" + message.substring(14) + " rejected your chat request");
                    } else if (message.startsWith("GROUP_INVITE:")) {
                        String[] parts = message.split(":", 3);
                        String groupId = parts[1];
                        String members = parts[2];
                        System.out.println("You've been invited to group " + groupId + " with members: " + members);
                        System.out.println("Use /gaccept " + groupId + " to join.");
                    } else if (message.startsWith("GROUP_MSG:")) {
                        String[] parts = message.split(":", 4);
                        String groupId = parts[1];
                        String sender = parts[2];
                        String msg = parts[3];
                        System.out.println("[" + groupId + "] " + sender + ": " + msg);
                    } else {
                        System.out.println(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            new Client().start();
        } catch (IOException e) {
            System.err.println("Client exception: " + e.getMessage());
        }
    }
}
