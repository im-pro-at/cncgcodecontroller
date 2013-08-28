/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashMap;

/**
 *
 * @author patrick
 */
public class Database {
    private static Database self= new Database();
    public static Database getDatabase()
    {
        return self;
    }

    HashMap<String, String> data = new HashMap<>();
    
    //private Constractor
    private Database()
    {
        //nothing to do! (just make it private => Sigilton)
    }


    /**
     * Loads the Data from a File 
     * @return 
     * true => no errors 
     */
    public boolean load(){
        try {
            File file = new File("Settings.ois");
            ObjectInput in = new ObjectInputStream(new FileInputStream(file));
            data= (HashMap<String, String>)in.readObject();
            return true;
        } catch (ClassNotFoundException | IOException ex) {
            System.out.print(ex.toString());
            return false;
        }
    }
    
    /**
     * Saves the Data to a File
     * @return 
     * true => no errors
     */
    public boolean save(){
        try {
            File file = new File("Settings.ois");
            if(!file.exists())
                file.createNewFile();
            ObjectOutput out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(data);
            return true;
        } catch (IOException ex) {
            System.out.print(ex.toString());
            return false;
        }
    }

    public void set(String name, String value)
    {
       data.put(name, value);        
    }
    
    public String get(String name, String dvalue)
    {
        if(data.containsKey(name))
        {
            return data.get(name);
        }
        else
        {
            data.put(name, dvalue);        
            return dvalue;
        }
    }
}
