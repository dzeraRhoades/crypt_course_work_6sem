package com.example.luc.controllers;

import com.example.luc.MainApplication;
import com.example.luc.client.Client;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class MainWinController {
    @FXML
    private Label statusLabel;
    @FXML
    private Label addressLabel;
    @FXML
    private Button requestButton;
    @FXML
    private TextField hostTextField;
    @FXML
    private TextField portTextField;
    @FXML
    Button cancelButton;

    Client client;
    Thread currentThread;
    boolean isPressed = false;

    @FXML
    public void initialize() {
        client = new Client("localhost", 8433);
//        addressLabel.setText(client.getLocalAddress());
    }

    @FXML
    protected void onRequestButtonClick() {
        statusLabel.setText("wait...");
        requestButton.setDisable(true);
        cancelButton.setDisable(false);
        Task<Boolean> result = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                boolean res = client.requestSession(hostTextField.getText() + ":" + portTextField.getText());
                if(res) {
                    return res;
                }
                requestButton.setDisable(false);
                Platform.runLater(() -> statusLabel.setText("Something wrong. Try again"));
                return res;
            }
        };
        result.setOnSucceeded(event -> {
            try {
                if(result.get())
                    loadTransferWindow();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        currentThread = new Thread(result);
        currentThread.start();

//        boolean res = client.requestSession(hostTextField.getText() + ":" + portTextField.getText());
//        if(res)
//            loadTransferWindow();

    }

    void loadTransferWindow()
    {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(MainApplication.class.getResource("view/transfer.fxml")));
            Parent root = loader.load();
            TransferController transferController = loader.getController();
            transferController.initData(client);
            Scene scene = new Scene(root);
            Stage stage = (Stage)portTextField.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onCancelButtonClicked()
    {
        if(currentThread != null)
            currentThread.interrupt();
        cancelButton.setDisable(true);
    }
}