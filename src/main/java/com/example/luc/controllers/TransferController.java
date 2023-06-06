package com.example.luc.controllers;

import com.example.luc.client.Client;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class TransferController {
    @FXML
    Button loadButton;
    @FXML
    Button downloadButton;
    @FXML
    TextField fileTextField;
    @FXML
    TextField dirTextField;
    @FXML
    TextField fileNameTextField;
    @FXML
    ProgressBar progressBar;
    @FXML
    Label statusLabel;
    @FXML
    Button cancelButton;

    Client client = null;

    Thread currentTaskThread;

//    @FXML
//    public void initialize() {
//    }
    public void initData(Client client)
    {
        this.client = client;
    }

    @FXML
    protected void onSearchDirButtonClick()
    {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog((Stage) downloadButton.getScene().getWindow());

        if(selectedDirectory == null){
            //No Directory selected
        }else{
            dirTextField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    protected void onSearchFileButtonClick()
    {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog((Stage) downloadButton.getScene().getWindow());

        if(selectedFile == null){
            //No Directory selected
        }
        else
        {
            fileTextField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    protected void onLoadButtonClick()
    {
        Task<Boolean> mainTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        cancelButton.setDisable(false);
                        statusLabel.setText("Loading file...");
                    }
                });
                return client.loadFile(Paths.get(fileTextField.getText()), new Consumer<Double>() {
                    @Override
                    public void accept(Double aDouble) {
                        setProgressBar(aDouble);
                    }
                });
            }
        };
        actionSucceed(mainTask);
    }

    private void actionSucceed(Task<Boolean> mainTask) {
        mainTask.setOnSucceeded(event -> {
            boolean res = true;
            try {
                res = mainTask.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            if(res)
            {
                statusLabel.setText("finished");
            }
            else
            {
                statusLabel.setText("Error");
            }
            progressBar.setProgress(0);
            currentTaskThread = null;
            cancelButton.setDisable(true);
        });
        currentTaskThread = new Thread(mainTask);
        currentTaskThread.start();
    }

    @FXML
    protected void onDownloadButoonClick()
    {
        Task<Boolean> mainTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        cancelButton.setDisable(false);
                        statusLabel.setText("Downloading file...");
                    }
                });
                return client.downloadFile(Paths.get(dirTextField.getText()), fileNameTextField.getText(), new Consumer<Double>() {
                    @Override
                    public void accept(Double aDouble) {
                        setProgressBar(aDouble);
                    }
                });
            }
        };
        actionSucceed(mainTask);
    }

    public void setProgressBar(double val)
    {
        progressBar.setProgress(val);
    }

    @FXML
    protected void onCancelButtonClicked()
    {
        if(currentTaskThread != null)
            currentTaskThread.interrupt();
        cancelButton.setDisable(true);
    }
}
