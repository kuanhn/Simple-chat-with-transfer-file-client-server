package main;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import main.Helpers.CustomBundle;
import main.controllers.MainViewController;

import java.io.IOException;

/**
 * Created by eleven on 12/7/14.
 */
public class ChatWindow extends Stage {
    public static final int SCENE_CHAT_WIDTH = 500;
    public static final int SCENE_CHAT_HEIGHT = 400;

    private String object;
    public ChatWindow(MainViewController owener, String object) throws IOException {
        this.object = object;
        CustomBundle bundle = new CustomBundle();
        bundle.put("object", object);
        bundle.put("stage", this);
        Parent root = FXMLLoader.load(Main.class.getResource("chatview.fxml"), bundle);
        initStyle(StageStyle.UNDECORATED);
        setResizable(false);
        setScene(new Scene(root, SCENE_CHAT_WIDTH, SCENE_CHAT_HEIGHT));
        show();
    }
}
