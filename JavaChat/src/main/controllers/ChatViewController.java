package main.controllers;

import javafx.event.ActionEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by eleven on 12/7/14.
 */
public class ChatViewController extends ViewController{
    @Override
    public void closeScene(ActionEvent actionEvent) throws IOException {
        stage.close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);


    }

    public void sendMessage(ActionEvent actionEvent) {

    }

    public void uploadFile(ActionEvent actionEvent) {

    }
}
