package main.controllers;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import main.Helpers.MessageHelper;
import main.controls.ChatCellData;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class ChatViewController extends ViewController{
    @FXML
    private TextArea chatArea;
    @FXML
    private ListView chatView;
    @FXML
    private Label nicknameLabel;

    private String nickname;
    private String object;

    private MainViewController owner;
    private ArrayList<String> chatContents = new ArrayList<String>();

    @Override
    public void closeScene(ActionEvent actionEvent) throws IOException {
        stage.close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        socket = (Socket) resources.getObject("socket");
        nickname = (String)resources.getObject("nickname");
        object = (String)resources.getObject("object");
        owner = (MainViewController)resources.getObject("owner");
        nicknameLabel.setText(object);
        try {
            helper = new MessageHelper(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        chatView.setCellFactory(new Callback<ListView, ListCell>() {
            @Override
            public ListCell call(ListView param) {
                return new ListChatCell();
            }
        });
    }

    public void sendMessage(ActionEvent actionEvent) throws IOException {
        if (chatArea.getText().trim().length() > 0) {
            helper.sendMessage(helper.createChatMessage(object, chatArea.getText().trim()));
            showMessage(nickname, chatArea.getText().trim());
            chatArea.setText("");
        }

    }

    public void uploadFile(ActionEvent actionEvent) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload file");
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            helper.sendMessage(helper.createUploadFirstMessage(object, file.getName()));
            owner.addPendingFile(object,file);
        }
    }

    public void textAreaKeyPressHandle(KeyEvent event) throws IOException {
        if (event.getCode() == KeyCode.ENTER && !event.isAltDown()){
            sendMessage(null);
            event.consume();
        } else if (event.getCode() == KeyCode.ENTER && event.isAltDown()){
            chatArea.insertText(chatArea.getText().length(), "\n");
        }
    }

    public void showMessage(String nickname, String message){
        chatContents.add(nickname+":"+message);
        chatView.getItems().clear();
        chatView.getItems().addAll(chatContents);
    }

    public class ListChatCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (item != null){
                ChatCellData data = new ChatCellData();
//                UserCellData data = new UserCellData();
                String text, name;
                int index = item.indexOf(':');
                text = item.substring(index+1);
                name = item.substring(0,index);
                data.setInfo(name, text);
                if(data.getActionButton().isVisible()){
                    index = text.lastIndexOf("id=");
                    final int id = Integer.parseInt(text.substring(index + 3));
                    data.getActionButton().setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            try {
                                helper.sendMessage(String.format("D%4d",id));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
                setGraphic(data.getHBox());
            } else {
                setGraphic(null);
            }
        }
    }
}
