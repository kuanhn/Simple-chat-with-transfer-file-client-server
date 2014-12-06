package main.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
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

    public void closeScene(ActionEvent actionEvent) throws IOException {
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
        stage.close();
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
}
