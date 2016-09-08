package de.dynamicfiles.projects.gradle.example;

import javafx.stage.Stage;

/**
 *
 * @author Danny Althoff
 */
public class MainClass extends javafx.application.Application {

    public static void main(String[] args) {
        MainClass.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // here you can do your normal JavaFX magic ;)
        System.exit(0);
    }
}
