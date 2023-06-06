module com.example.luc {
    requires javafx.controls;
    requires javafx.fxml;
    requires lombok;
    requires com.google.gson;
    requires apache.log4j.extras;
    opens com.example.luc to javafx.fxml;
//    opens sun.nio.fs to com.google.gson;
    opens com.example.luc.entity to com.google.gson;
    exports com.example.luc;
    exports com.example.luc.controllers;
    exports com.example.luc.entity;
    exports com.example.luc.client;
    opens com.example.luc.controllers to javafx.fxml;
    exports com.example.luc.encryption;
    opens com.example.luc.encryption to javafx.fxml;
}