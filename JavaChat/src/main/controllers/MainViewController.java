package main.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import main.ChatWindow;
import main.Helpers.CustomBundle;
import main.Helpers.Logger;
import main.Helpers.MessageHelper;
import main.controls.UserCellData;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;

public class MainViewController extends ViewController {

    @FXML
    private Label nicknameLabel;
    @FXML
    private ListView listChatUserView;

    private List<String> listUser;
    private String nickname;

    private ChatHandler handler;
    private Map<String, ChatWindow> mapChatWindow;
    private Map<String, File> pendingFiles = new HashMap<String, File>();
    private Map<Integer, File> pendingDownloadFiles = new HashMap<Integer, File>();
    private Map<String, String> files = new HashMap<String, String>();
    private Map<Integer, String> filesDownload = new HashMap<Integer, String>();

    @Override
    public void closeScene(ActionEvent actionEvent) throws IOException {
        handler.isStop = true;
        super.closeScene(actionEvent);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        CustomBundle bundle = (CustomBundle) resources;
        super.initialize(location, bundle);

        mapChatWindow = new HashMap<String, ChatWindow>();
        nickname = (String)resources.getObject("nickname");
        nicknameLabel.setText(nickname);
        socket = (Socket)resources.getObject("socket");
        try {
            helper = new MessageHelper(socket);
            // get list user from server
            helper.sendMessage("Lusers");

            listUser = new ArrayList<String>();
            List<String> list = helper.getMessages(6+600);

            for (String str: list){
                listUser.addAll(parseListUser(str.substring(6)));
            }
            listChatUserView.getItems().addAll(listUser);
            Logger.writeLog("size:"+listUser.size());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // open new window to chat when double click a user
        listChatUserView.setCellFactory(new Callback<ListView, ListCell>() {
            @Override
            public ListCell call(ListView param) {
                ListCell cell = new ListUserCell();
                cell.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        if (event.getClickCount() > 1){
                            System.out.println("double clicked!");
                            ListUserCell c = (ListUserCell) event.getSource();
                            try {
                                ChatWindow chat = new ChatWindow(MainViewController.this, c.data.getName());
                                mapChatWindow.put(c.data.getName(), chat);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                });
                return cell;
            }
        });

        handler = new ChatHandler();
        handler.start();
    }

    private List<String> parseListUser(String str){
        ArrayList<String> result = new ArrayList<String>();
        for (int i = 0; i < str.length(); i+=20){
            result.add(str.substring(i, i + 20).trim());
        }
        return result;
    }

    public String getNickname(){
        return nickname;
    }

    public Socket getSocket() {
        return socket;
    }

    public void addPendingFile(String object, File file) {
        pendingFiles.put(object, file);
        files.put(file.getName().trim(), object);
    }

    public void refreshList(ActionEvent actionEvent) throws IOException {
        helper.sendMessage("Lusers");
    }

    public class ListUserCell extends ListCell<String>{
        UserCellData data = new UserCellData();
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (item != null){
                String badge, name;
                if(item.split(" ").length > 1){
                    badge = item.split(" ")[1];
                    name = item.split(" ")[0];
                } else {
                    badge = "0";
                    name = item;
                }
                data.setInfo(name, badge);
                setGraphic(data.getHBox());
            } else {
                setGraphic(null);
            }
        }
    }

    public void checkChatWindow(String obj) throws IOException {
        if (!mapChatWindow.containsKey(obj)) {
            ChatWindow chat = new ChatWindow(MainViewController.this, obj);
            mapChatWindow.put(obj, chat);
        }
    }

    public class ChatHandler extends Thread {
        volatile boolean isStop = false;
        String message;
        byte[] data = new byte[1029];
        int count = 0;

        @Override
        public void run() {
            super.run();

            while (true && !isStop) {
                try {
                    count = socket.getInputStream().read(data, 0, 1029);
                } catch (IOException e) {
                    if (isStop) break;
                    e.printStackTrace();
                }
                if (count <= 0) continue;
                final String receiver, filename,sender;
                final int id;
                char[] buffer = new char[1029];
                    for (int i = 0; i < count; i++){
                        buffer[i] = (char)data[i];
                    }
                    message = String.copyValueOf(buffer, 0, count);
                switch (data[0]) {
                    case 'M': // update list chat view
                        receiver = message.substring(1,21).trim();
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                if (mapChatWindow.get(receiver) == null){
                                    createChatWindow(receiver);
                                }

                                mapChatWindow.get(receiver).mController.showMessage(receiver, message.substring(21));
                            }
                        });
                        break;
                    case 'F':
                        id = Integer.parseInt(message.substring(1,5).trim());
                        filename = message.substring(5);
                        Logger.writeLog("file id:"+id);

                        /* open file to upload */
                        receiver = files.get(filename);
                        File f = pendingFiles.get(receiver);
                        if (f != null) {
                            try {
                                FileInputStream input = new FileInputStream(f);
                                byte[] data = new byte[1024];
                                int count = 0;
                                while((count = input.read(data,0, 1024)) != -1){
                                    byte[] buf = helper.createUploadMessage(id, data, count);
                                    helper.sendRawMessage(buf, count+5);
                                }
                                input.close();

                                /*notify user when finish*/
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mapChatWindow.get(receiver) == null){
                                            createChatWindow(receiver);
                                        }
                                        mapChatWindow.get(receiver).mController.showMessage("NP","sending file "+filename+" successfully");
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case 'D':
                        sender = message.substring(1,21).trim();
                        filename = message.substring(21, 41).trim();
                        id = Integer.parseInt(message.substring(41).trim());

                        f = new File(filename);
                        pendingDownloadFiles.put(id, f);
                        filesDownload.put(id, sender);

                        /* notify user*/
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                if (mapChatWindow.get(sender) == null)
                                    createChatWindow(sender);
                                mapChatWindow.get(sender).mController.showMessage("NP","Download:"+sender+" send file "+filename+" id="+id);
                            }
                        });

                        break;
                    case 'T':
                        Logger.writeLog(">>");
                        id = Integer.parseInt(message.substring(1,5).trim());
                        f = pendingDownloadFiles.get(id);
                        if (f != null) {
                            try {
                                FileOutputStream out = new FileOutputStream(f, true);
                                out.write(Arrays.copyOfRange(data,5,count));
                                out.flush();
                                out.close();

                                if(count < 1029){/*download finished*/
                                    sender = filesDownload.get(id);
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mapChatWindow.get(sender) == null)
                                                createChatWindow(sender);
                                            mapChatWindow.get(sender).mController.showMessage("NP","Finished download");
                                        }
                                    });
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case 'L':
                        listUser = new ArrayList<String>();
                        List<String> list = null;
                        try {
                            list = helper.getMessages(6+600);

                            for (String str: list){
                                listUser.addAll(parseListUser(str.substring(6)));
                            }
                            listChatUserView.getItems().clear();
                            listChatUserView.getItems().addAll(listUser);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case 'N':
                        Logger.writeLog(message.substring(1));
                        break;
                    case 'X':
                        break;

                }
            }
        }
    }

    private void createChatWindow(String receiver) {
        ChatWindow chat = null;
        try {
            chat = new ChatWindow(MainViewController.this, receiver);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mapChatWindow.put(receiver, chat);
    }
}
