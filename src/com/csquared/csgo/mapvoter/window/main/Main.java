package com.csquared.csgo.mapvoter.window.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("main_ui.fxml"));
        primaryStage.setTitle("NJUPT MAJOR MapVoter - 1.0");
        primaryStage.setScene(new Scene(root, 990, 590));
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(e ->
            System.exit(0)
        );
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
