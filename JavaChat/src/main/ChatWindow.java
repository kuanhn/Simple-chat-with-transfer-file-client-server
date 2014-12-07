package main;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import main.Helpers.CustomBundle;
import main.controllers.ChatViewController;
import main.controllers.MainViewController;

import java.io.IOException;

public class ChatWindow extends Stage {
    public static final int SCENE_CHAT_WIDTH = 500;
    public static final int SCENE_CHAT_HEIGHT = 400;

    private String object;
    private MainViewController mOwner;
    public ChatViewController mController;

    public ChatWindow(MainViewController owner, String object) throws IOException {
        this.object = object;
        this.mOwner = owner;
        CustomBundle bundle = new CustomBundle();
        bundle.put("object", object);
        bundle.put("stage", this);
        bundle.put("owner", mOwner);
        bundle.put("socket", owner.getSocket());
        bundle.put("nickname", owner.getNickname());
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("chatview.fxml"), bundle);
        Parent root = loader.load();
        mController = loader.getController();
        initStyle(StageStyle.UNDECORATED);
        setResizable(false);
        setScene(new Scene(root, SCENE_CHAT_WIDTH, SCENE_CHAT_HEIGHT));
        show();
    }
}