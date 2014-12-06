package main.controls;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import main.Main;

import java.io.IOException;

/**
 * Created by eleven on 12/7/14.
 */
public class UserCellData {
    @FXML
    private AnchorPane cellContainer;
    @FXML
    private Label nameLabel;
    @FXML
    private Label badgeLabel;

    public UserCellData(){
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("list_user_cell.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setInfo(String name, String badge){
        nameLabel.setText(name);
        badgeLabel.setText(badge);
    }

    public AnchorPane getHBox(){
        return cellContainer;
    }

    public String getName(){
        if (nameLabel != null) return nameLabel.getText();
        return "";
    }

    public String getBadge(){
        if (badgeLabel != null) return badgeLabel.getText();
        return "";
    }
}
