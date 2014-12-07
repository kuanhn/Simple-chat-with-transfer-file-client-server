package main.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.WindowEvent;
import main.Helpers.CustomBundle;
import main.Helpers.MessageHelper;
import main.Main;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginViewController extends ViewController {

    @FXML
    private TextField nicknameTextfield;
    @FXML
    private Label errorLabel;

    public void signIn(ActionEvent event){
        try {
            errorLabel.setText("");
            socket = new Socket("localhost", 7400);
            socket.setSendBufferSize(1029);
            helper = new MessageHelper(socket);
            loadMainView(socket);
        } catch (IOException e) {
            errorLabel.setText("Connection refused. Please check your connection.");
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        // init close request handler
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                try {
                    closeSocket();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Platform.exit();
            }
        });

        errorLabel.setText("");
    }

    private void loadMainView(final Socket socket) throws IOException {
        final String nickname = nicknameTextfield.getText().replace(" ","");
        String str = helper.getMessage();

        if (str.charAt(0) == 'C' && Integer.parseInt(str.substring(1,3).trim()) == MessageHelper.CFG_ID_KEY){
            helper.sendMessage(helper.createConfigMessage(MessageHelper.CFG_NICK_KEY, nickname));

            /* read response message */
            str = helper.getMessage();
            if (str.charAt(0) == 'C' && Integer.parseInt(str.substring(1,3).trim()) == MessageHelper.CFG_NICK_KEY){
                CustomBundle bundle = new CustomBundle();
                bundle.put("stage", stage);
                bundle.put("socket", socket);
                bundle.put("nickname", nickname);
                Parent root = FXMLLoader.load(Main.class.getResource("mainview.fxml"), bundle);
                stage.setScene(new Scene(root, Main.SCENE_WIDTH, Main.SCENE_HEIGHT));
                stage.show();
            } else if (str.charAt(0) == 'N'){
                errorLabel.setText("!!! "+str.substring(1));
            }
        }
    }
}
