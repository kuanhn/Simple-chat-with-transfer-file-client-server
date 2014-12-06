package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import main.Helpers.CustomBundle;

public class Main extends Application {
    public static final String APPNAME = "NP Simple Chat";
    public static final int SCENE_WIDTH = 400;
    public static final int SCENE_HEIGHT = 600;

    @Override
    public void start(final Stage primaryStage) throws Exception{
        CustomBundle bundle = new CustomBundle();
        bundle.put("stage", primaryStage);
        Parent root = FXMLLoader.load(getClass().getResource("loginview.fxml"), bundle);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle(APPNAME);
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(root, SCENE_WIDTH, SCENE_HEIGHT));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
