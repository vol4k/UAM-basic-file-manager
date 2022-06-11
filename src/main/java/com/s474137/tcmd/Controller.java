package com.s474137.tcmd;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;

public class Controller implements Initializable {

    // FXML objects

    @FXML private VBox root;

    @FXML private TableView<FileItem> leftTable, rightTable;
    
    @FXML private TableColumn<FileItem, String> lType, rType;
    @FXML private TableColumn<FileItem, String> lName, rName;
    @FXML private TableColumn<FileItem, String> lCreated, rCreated;
    
    @FXML private Label lPathLabel, rPathLabel;
    @FXML private Button showButton, createButton, deleteButton;

    // initial functions

    private void initLabels(){
        setLabelPath(lPathLabel, ""); 
        setLabelPath(rPathLabel, "");
    }

    private void initCols(){
        bindCols(Set.<TableColumn<FileItem, String>>of(lType,rType),"type");
        bindCols(Set.<TableColumn<FileItem, String>>of(lName,rName),"name");
        bindCols(Set.<TableColumn<FileItem, String>>of(lCreated,rCreated),"created");
    }

    private void initTables(){
        redrawTables();
    }

    private void initActions(){
        showButton.setOnMouseClicked(event -> onShowAction());
        createButton.setOnMouseClicked(event -> onCreateAction());
        deleteButton.setOnMouseClicked(event -> onDeleteAction());

        setOnKeyPressedAction(root);

        setSortPolicyProperty(leftTable);
        setSortPolicyProperty(rightTable);
        
        setOnTableClickAction(leftTable, lPathLabel);
        setOnTableClickAction(rightTable, rPathLabel);

        setOnDragDetectedAction(leftTable);
        setOnDragDetectedAction(rightTable);

        setOnDragOverAction(leftTable);
        setOnDragOverAction(rightTable);

        setOnDragDroppedAction(leftTable, lPathLabel);
        setOnDragDroppedAction(rightTable, rPathLabel);
    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        initLabels();
        initCols();
        initTables();
        initActions();
    }

    // actions

    private void onShowAction(){
        Boolean shown = showButton.getText().equals("Show (F6)");
        showButton.setText(shown ? "Hide (F6)" : "Show (F6)");

        redrawTables();
    }
    
    private void onCreateAction(){
        try{
            TextInputDialog dialog = new TextInputDialog("New folder");
            dialog.setTitle("Create new folder");
            dialog.setContentText("Please enter folder name:");

            Optional<String> result = dialog.showAndWait();

            if(!result.isPresent())
                return;
            if(result.get().isEmpty())
                throw new IOException("Name is empty");

            ObservableList<ButtonType> buttonList = FXCollections.observableArrayList(
                new ButtonType("Left"), 
                new ButtonType("Right"), 
                new ButtonType("Cancel", ButtonData.CANCEL_CLOSE)
            );

            Optional<ButtonType> choose = callAlert(
                "Сhoose workspace",
                "Information",
                "Choose the workspace where you want to create a folder",
                buttonList,
                AlertType.CONFIRMATION
            );

            if (choose.get().getButtonData().isCancelButton())
                return;
            
            Label label = choose.get().getText().equals("Left") ? lPathLabel : rPathLabel;

            if(!(new File(label.getText()).canWrite()))
                throw new IOException("Writing not allowed");
            
            Files.createDirectories(Paths.get(label.getText() + "/" + result.get()));

            redrawTables();
        }
        catch(IOException ie){
            exceptionHandler(ie);
        }
    }

    private void onDeleteAction(){
        try{
            ObservableList<ButtonType> buttonsList = FXCollections.observableArrayList(
                new ButtonType("Left"),
                new ButtonType("Right"),
                new ButtonType("Cancel", ButtonData.CANCEL_CLOSE)
            );
            
            Optional<ButtonType> choose = callAlert(
                "Сhoose workspace",
                "Information",
                "Choose the workspace where you want to delete an element",
                buttonsList,
                AlertType.CONFIRMATION
            );

            if (choose.get().getButtonData().isCancelButton())
                return;

            
            FileItem selectedItem = choose.get().getText().equals("Left") 
                ? leftTable.getSelectionModel().getSelectedItem()
                : rightTable.getSelectionModel().getSelectedItem();


            if(selectedItem==null)
                throw new IOException("Element not selected");
            else if(!selectedItem.getDescriptor().canWrite())
                throw new IOException("Writing not allowed");

            tryDelete(selectedItem.getDescriptor().toPath());

            redrawTables();
        }
        catch(IOException ie){
            exceptionHandler(ie);
        }
    }


    private void setSortPolicyProperty(TableView<FileItem> table){
        ;
        table.sortPolicyProperty().set(t -> {
            Comparator<FileItem> comparator = (r1, r2) -> 
                 r1.getName().equals("...") ? -1
               : r2.getName().equals("...") ? 1
               : t.getComparator() == null ? 0
               : t.getComparator().compare(r1, r2);
            
            if(t.getItems()==null)
                return false;

            FXCollections.sort(t.getItems(), comparator);
            return true;
        });
    }

    private void setOnKeyPressedAction(VBox root){
        root.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();

            if(code == KeyCode.F6){
                onShowAction();
            }
        
            else if(code == KeyCode.F7){
                onCreateAction();
            }
        
            else if(code == KeyCode.F8){
                onDeleteAction();
            }
        });
    }

    private void setOnTableClickAction(TableView<FileItem> table, Label label){
        table.setOnMouseClicked(event -> {
            if( event.getClickCount() == 2 ) {
                try{
                    tryOpen(table, label);
                }
                catch(IOException ie){
                    exceptionHandler(ie);
                }
            }
        });
    }

    private void setOnDragDetectedAction(TableView<FileItem> table){
        table.setOnDragDetected(event ->{
            FileItem selected = table.getSelectionModel().getSelectedItem();

            if(selected !=null){
                Dragboard db = table.startDragAndDrop(TransferMode.ANY);
                ClipboardContent content = new ClipboardContent();
                content.putString(selected.getDescriptor().toString());
                db.setContent(content);
                event.consume();
            }
        });
    }

    private void setOnDragOverAction(TableView<FileItem> table){
        table.setOnDragOver(event ->{
            if (event.getDragboard().hasString()){
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
    }

    private void setOnDragDroppedAction(TableView<FileItem> table, Label label){
        table.setOnDragDropped(event ->{
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (event.getDragboard().hasString()) {            
                FileItem fileItem = new FileItem(new File(db.getString()));
                
                Path source = fileItem.getDescriptor().toPath().toAbsolutePath();
                Path target = Paths.get(label.getText() + "/" + fileItem.getName());
                try{
                    tryCopy(source, target);
                    success = true;
                }
                catch(IOException ie){
                    exceptionHandler(ie);
                    return;
                }
            }
            event.setDropCompleted(success);
            event.consume();

            redrawTables();
        });
    }

    // try action group

    private void tryOpen(TableView<FileItem> table, Label label) throws IOException{

        FileItem selected = table.getSelectionModel().getSelectedItem();

        if(selected == null)
            throw new IOException("Element not selected");
        else if(!selected.getDescriptor().canRead())
            throw new IOException("Reading not allowed");

        if(selected.getDescriptor().isFile())
        {
            tryExecute(selected);
            return;
        }
        
        label.setText(selected.getDescriptor().getAbsolutePath());
        
        redrawTable(table, label);
    }

    private void tryCopy(Path source, Path target) throws IOException{
        System.out.println(source);
        System.out.println(target);
        System.out.println();

        if(!(new File(source.toString())).canRead())
        throw new IOException("Reading not allowed");

        try{
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }
        catch(IOException ie){
            throw new IOException("Writing not allowed");
        }

        if(source.toFile().isDirectory())
        {
            for (FileItem fileItem : getFileItems(source.toString(), false)) {
                Path r_source = fileItem.getDescriptor().toPath().toAbsolutePath();
                Path r_target = Paths.get(target + "/" + fileItem.getName());
                if(!target.toAbsolutePath().equals(r_source.toAbsolutePath()))
                    tryCopy(r_source, r_target);
            }
        }
    }

    private void tryDelete(Path current) throws IOException{
        if(Files.isDirectory(current))
        {
            for (FileItem fileItem : getFileItems(current.toString(), false)) {
                Path r_current = fileItem.getDescriptor().toPath().toAbsolutePath();
                tryDelete(r_current);
            }
        }
        Files.deleteIfExists(current);
    }

    private void tryExecute(FileItem file) throws IOException{
        throw new IOException("Method not implemented");
    }

    // helper functions

    private void redrawTable(TableView<FileItem> table, Label label){
        table.setItems(null);
        table.setItems(getFileItems(label.getText(), true));
    }

    private void redrawTables(){
        redrawTable(leftTable,lPathLabel);
        redrawTable(rightTable,rPathLabel);
    }

    private void setLabelPath(Label label, String path){
        label.setText((new File(path)).getAbsolutePath());
    }

    private void bindCols(Set<TableColumn<FileItem, String>> columns, String property){
        for (TableColumn<FileItem,String> tableColumn : columns)
            bindCol(tableColumn, property);
    }

    private void bindCol(TableColumn<FileItem, String> column, String property){
        column.setCellValueFactory(new PropertyValueFactory<>(property));
    }

    private ObservableList<FileItem> getFileItems(String path, boolean withParent){
        File current = new File (path.toString());
        File[] files = (current).listFiles();
        
        ObservableList<FileItem> fileItems = FXCollections.observableArrayList();        
        
        if(withParent)
        {
            File parent = current.getAbsoluteFile().getParentFile();
            if(parent != null)
                fileItems.add(new FileItem(parent));
            else
                fileItems.add(new FileItem(current));
            fileItems.get(0).setName("...");
        }

        for (File file : files) {
            if(file.isHidden() && showButton.getText().equals("Show (F6)"))
                continue;
            
            fileItems.add(new FileItem(file));
        }
        return fileItems;
    }   

    private Optional<ButtonType> callAlert(String title, String header, String content, ObservableList<ButtonType> buttonsList, AlertType alertType){
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        if(!buttonsList.isEmpty())
            alert.getButtonTypes().setAll(buttonsList);        

        return alert.showAndWait();
    }

    private void exceptionHandler(IOException ie){

        String title = "Error";
        String header = ie.getMessage();
        String content = "Oops... Something is wrong";
        ObservableList<ButtonType> buttonsList = FXCollections.observableArrayList();
        AlertType alertType = AlertType.ERROR;


        if(ie.getMessage().equals("Reading not allowed"))
            content = "You do not have permission to read this file or directory";
        
        else if (ie.getMessage().equals("Writing not allowed"))
            content = "You do not have permission to write here";

        else if (ie.getMessage().equals("Element not selected"))
            content = "Please choose a file or directory";

        else if (ie.getMessage().equals("Name is empty"))
            content = "The folder will not be created because the name field is empty. Please enter a folder name next time";

        else if (ie.getMessage().equals("Method not implemented"))
            content = "Sorry, but opening files is not implemented yet :(";


        callAlert(title, header, content, buttonsList, alertType);
    }
}