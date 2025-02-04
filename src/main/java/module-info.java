module org.example.myjavafxapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.net.http;
    requires org.json;

    // Open the main package to JavaFX FXMLLoader
    opens org.example.myjavafxapp to javafx.fxml;

    //   opens org.example.myjavafxapp to javafx.fxml;
    opens org.example.myjavafxapp.model to javafx.base; // <-- Add this line

    exports org.example.myjavafxapp;
}