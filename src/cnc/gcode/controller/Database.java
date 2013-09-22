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
import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author patrick
 */
public enum Database {
    
    //Port Settings
    PORT(""),
    SPEED("9"),
    
    //Control
    HOMEING("0"), //Homing Point 0= upper left; 1= upper right; 2= lower left; 3= lower right;
    MAXFEEDRATE(Tools.dtostr(600.0)), 
    WORKSPACE0(Tools.dtostr(200.0)),
    WORKSPACE1(Tools.dtostr(200.0)),
    WORKSPACE2(Tools.dtostr(200.0)),
    
    //CNC
    FILEDIRECTORY(System.getProperty("user.home")),
    TOOLCHANGE("M6 T?"),
    SPINDLEON("M?"),
    SPINDLEOFF("M5"),
    GOFEEDRATE(Tools.dtostr(100.0)),
    TOOLSIZE(Tools.dtostr(5.0)),
    
    ;

    private final String defaultValue;

    private Database(String defaultvalue)
    {
        this.defaultValue=defaultvalue;
    }
   
    private static EnumMap<Database, String> data = new EnumMap<>(Database.class);
    
    /**
     * Loads the Data from a File 
     * @return 
     * true => no errors 
     */
    public static boolean load(){
        try {
            File file = new File("Settings.ois");
            ObjectInput in = new ObjectInputStream(new FileInputStream(file));
            data= (EnumMap<Database, String>)in.readObject();
            return true;
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex); 
            return false;
        }
    }
    
    /**
     * Saves the Data to a File
     * @return 
     * true => no errors
     */
    public static boolean save(){
        try {
            File file = new File("Settings.ois");
            if(!file.exists())
                file.createNewFile();
            ObjectOutput out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(data);
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex); 
            return false;
        }
    }

    public synchronized static void set(Database name, String value)
    {
       data.put(name, value);        
    }
    
    public synchronized static String get(Database name)
    {
        if(data.containsKey(name))
        {
            return data.get(name);
        }
        else
        {
            return name.defaultValue;
        }
    }
    
    public void set(String value)
    {
        Database.set(this, value);
    }
    
    public String get()
    {
        return Database.get(this);
    }

    @Override
    public String toString() {
        return Database.get(this);
    }
    
    
}
