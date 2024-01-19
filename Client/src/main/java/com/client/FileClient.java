package com.client;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileClient {

    private static String DIRECTORY_NAME = "";
    private static String SERVER_IP;
    private static int SERVER_PORT;
    private static String REGULAR_EXPRESSION;

    public static void main(String[] args) {

        if (args != null && args.length > 0) {
            setProperties(args[0]);
        } else {
            System.out.println("Argument not provided for config file! Please provide " +
                    "config file name in the argument and rerun the Client program.");
            System.exit(1);
        }
        Thread processFiles = new Thread(new ProcessFiles());
        processFiles.start();
    }

    static class ProcessFiles implements Runnable {
        @Override
        public void run() {
            while (true) {
                processFiles();
                try {
                    // Sleep for 30 seconds and again look for new files to process
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static void processFiles() {
        List<String> processFiles = new ArrayList<>();

        File[] files = new File(DIRECTORY_NAME).listFiles();
        File fileToDelete = null;
        for (File file : files) {
            if (file.getName().endsWith(".properties")) {
                processFiles.add(file.getName());
                fileToDelete = file;
                break;
            }
        }

        if(processFiles.size() > 0) {
            try {
                Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                System.out.println("Server response: " + objectInputStream.readObject().toString());

                for (String fileNames : processFiles) {
                    Map<String, String> fileDataMap = convertFileToMap(DIRECTORY_NAME + "/" + fileNames);
                    objectOutputStream.writeObject(fileDataMap);
                    objectOutputStream.flush();

                    System.out.println("Server response: " + objectInputStream.readObject().toString());
                }

                objectInputStream.close();
                objectOutputStream.close();
                socket.close();
                if (fileToDelete != null)
                    fileToDelete.delete();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }else{
            System.out.println("No file to process");
        }
    }

    private static void setProperties(String configFile) {
        Properties properties = new Properties();
        String fileName = configFile;
        try {
            FileInputStream fileInputStream = new FileInputStream(fileName);
            properties.load(fileInputStream);

            DIRECTORY_NAME = properties.getProperty("app.directory");
            SERVER_IP = properties.getProperty("app.server");
            SERVER_PORT = Integer.parseInt(properties.getProperty("app.port"));
            REGULAR_EXPRESSION = properties.getProperty("app.pattern");

            fileInputStream.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }
    }

    private static Map<String, String> convertFileToMap(String fileName) {
        Properties properties = new Properties();
        Map<String, String> dataMap = new HashMap<>();
        try {
            FileInputStream fileInputStream = new FileInputStream(fileName);
            properties.load(fileInputStream);

            Pattern pattern = Pattern.compile(REGULAR_EXPRESSION);

            for (Map.Entry<Object, Object> entries : properties.entrySet()) {
                Matcher matcher = pattern.matcher((String) entries.getKey());
                if (matcher.matches()) {
                    dataMap.put((String) entries.getKey(), (String) entries.getValue());
                }
            }

            fileInputStream.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        return dataMap;
    }
}