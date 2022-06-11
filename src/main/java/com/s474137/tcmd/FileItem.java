package com.s474137.tcmd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;

import javafx.beans.property.SimpleStringProperty;

public class FileItem {
  private File descriptor;
  private SimpleStringProperty type;
  private SimpleStringProperty name;
  private SimpleStringProperty created;

  public FileItem(File f)
  { 
    try{
    this.descriptor = f;
    BasicFileAttributes atr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
    SimpleDateFormat template = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    
    this.type = new SimpleStringProperty(f.isFile() ? "file" : "dir");
    this.name = new SimpleStringProperty(f.getName());
    this.created = new SimpleStringProperty(template.format(atr.creationTime().toMillis()));
    }
    catch(IOException ie){
      ie.printStackTrace();
    }
  }

  public FileItem setDescriptor(File descriptor) {
    this.descriptor = descriptor;
    return this;
  }

  public File getDescriptor() {
    return this.descriptor;
  }

  public FileItem setType(String type) {
    this.type.set(type);
    return this;
  }

  public FileItem setName(String name) {
    this.name.set(name);
    return this;
  }

  public FileItem setCreated(String created) {
    this.created.set(created);
    return this;
  }

  public String getType() {
    return type.get();
  }

  public String getName() {
    return name.get();
  }

  public String getCreated() {
    return created.get();
  }
}