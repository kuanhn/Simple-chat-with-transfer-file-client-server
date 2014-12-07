package main.controls;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import main.Main;

import java.io.IOException;

public class ChatCellData {
    @FXML
    private VBox chatContainer;
    @FXML
    private Label nameLabel;
    @FXML
    private Text chatText;
    @FXML
    private Button actionButton;

    private int downloadId;

    public ChatCellData(){
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("list_chat_cell.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setInfo(String name, String badge){
        nameLabel.setText(name);
        chatText.setText(badge);
        if (badge.startsWith("Download") && name.compareTo("NP") == 0){
            actionButton.setVisible(true);
        } else actionButton.setVisible(false);
    }

    public VBox getHBox(){
        return chatContainer;
    }

    public Button getActionButton(){return  actionButton;}
}
