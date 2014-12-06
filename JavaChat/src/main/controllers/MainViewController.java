package main.controllers;

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

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MainViewController extends ViewController {

    @FXML
    private Label nicknameLabel;
    @FXML
    private ListView listChatUserView;

    private List<String> listUser;
    private String nickname;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        CustomBundle bundle = (CustomBundle) resources;
        super.initialize(location, bundle);

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
                                new ChatWindow(MainViewController.this, c.data.getName());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                });
                return cell;
            }
        });


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
            }
        }
    }
}
