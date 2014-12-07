package main.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import main.Helpers.MessageHelper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class ViewController implements Initializable{

    protected Stage stage;
    protected Socket socket;
    protected MessageHelper helper;

    private double xOffset = 0;
    private double yOffset = 0;

    public void closeScene(ActionEvent actionEvent) throws IOException {
        closeSocket();

        stage.close();
        Platform.exit();
    }

    protected void closeSocket() throws IOException {
        if (socket != null && !socket.isClosed()){
            if (!socket.isClosed() && socket.getOutputStream() != null){
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                writer.write("XClient is down");
                writer.flush();
                socket.getOutputStream().close();
            }
            if (!socket.isClosed() && socket.getInputStream() != null){
                socket.getInputStream().close();
            }
            if (!socket.isClosed())
                socket.close();
        }
    }

    public void minimizeScene(ActionEvent actionEvent) {
        stage.setIconified(true);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (resources != null) {
            stage = (Stage) resources.getObject("stage");
        }
    }

    public void mousePressHandle(MouseEvent event){
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    public void mouseDragHandle(MouseEvent event){
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }
}
