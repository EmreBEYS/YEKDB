package com.yekdb.launcher;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public final class YekdbLauncherApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(
                YekdbLauncherApp.class.getResource("/fxml/launcher.fxml")));
        Scene scene = new Scene(root, 1180, 720);
        scene.getStylesheets().add(Objects.requireNonNull(
                YekdbLauncherApp.class.getResource("/css/yekdb-dark-purple.css")).toExternalForm());

        stage.setTitle("YEKDB Launcher");
        stage.getIcons().add(new Image(Objects.requireNonNull(
                YekdbLauncherApp.class.getResourceAsStream("/assets/yekdb-logo.png"))));
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
