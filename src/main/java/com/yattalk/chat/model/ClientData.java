package com.yattalk.chat.model;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ClientData {

    private static final String FILE_PATH = "clients.json";
    private Map<String, Map<String, List<Map<String, String>>>> data;

    public ClientData() {
        this.data = loadData();
    }

    private Map<String, Map<String, List<Map<String, String>>>> loadData() {
        try (Reader reader = new FileReader(FILE_PATH)) {
            return new Gson().fromJson(reader,
                    new TypeToken<Map<String, Map<String, List<Map<String, String>>>>>() {
                    }.getType());
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    public void saveData() {
        try (Writer writer = new FileWriter(FILE_PATH)) {
            new Gson().toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addMessage(String user1, String user2, String sender, String message) {
        String key = user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;

        data.putIfAbsent(key, new HashMap<>());
        data.get(key).putIfAbsent("messages", new ArrayList<>());

        Map<String, String> msg = new HashMap<>();
        msg.put(sender, message);
        data.get(key).get("messages").add(msg);

        saveData();
    }

    public List<String> getChatHistory(String user1, String user2) {
        String key = user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
        if (data.containsKey(key)) {
            List<String> history = new ArrayList<>();
            for (Map<String, String> msg : data.get(key).get("messages")) {
                msg.forEach((k, v) -> history.add(k + ": " + v));
            }
            return history;
        }
        return new ArrayList<>();
    }
}
