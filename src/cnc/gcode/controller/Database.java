/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import javax.swing.JFileChooser;

/**
 *
 * @author patrick
 */
public enum Database {
    //Port Settings
    PORT,
    SPEED("9"),
    
    //Control
    HOMEING("0"), //Homing Point 0= upper left; 1= upper right; 2= lower left; 3= lower right;
    MAXFEEDRATE(Tools.dtostr(600.0)), 
    WORKSPACE0(Tools.dtostr(200.0)),
    WORKSPACE1(Tools.dtostr(200.0)),
    WORKSPACE2(Tools.dtostr(200.0)),
    
    //CNC
    FILEDIRECTORY(System.getProperty("user.home")),
    STARTCODE,
    TOOLCHANGE("M6 T?"),
    SPINDLEON("M?"),
    SPINDLEOFF("M5"),
    GOFEEDRATE(Tools.dtostr(100.0)),
    TOOLSIZE(Tools.dtostr(5.0)),
    OPTIMISATIONTIMEOUT(Tools.dtostr(10)),
    
    //Autoleveling
    ALZERO(Tools.dtostr(0.0)),
    ALMAXPROPDEPTH(Tools.dtostr(-1.0)), 
    ALSAVEHEIGHT(Tools.dtostr(10.0)),
    ALCLEARENCE(Tools.dtostr(10.0)),
    ALFEEDRATE(Tools.dtostr(10.0)),
    ALDISTANACE(Tools.dtostr(10.0)),
    ALMAXMOVELENGTH(Tools.dtostr(1.0)), 
    ALSTARTCODE("G28"), 
    
    //ARC
    ARCSEGMENTLENGTH(Tools.dtostr(0.1)),
    
    //Backlash
    BL0(Tools.dtostr(0.0)),
    BL1(Tools.dtostr(0.0)),
    BL2(Tools.dtostr(0.0)),
    
    ;

    private final String defaultValue;
    private final static String SETTINGSFILE=System.getProperty("user.home")+File.separator+".cnccgcodecontroller"+File.separator+"Settings.ois";

    private Database(String defaultvalue)
    {
        this.defaultValue=defaultvalue;
    }
    private Database()
    {
        this.defaultValue="";
    }
   
    private static EnumMap<Database, String> data = new EnumMap<>(Database.class);
    
    public static JFileChooser getFileChooser()
    {
        final JFileChooser fc = new JFileChooser();
        //set currantdiroctary
        try {
            fc.setCurrentDirectory(new File(Database.FILEDIRECTORY.get()));
        }
        catch(Exception ex){
            //Its ok ;-)
        }
        fc.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(evt.getPropertyName()))
                {
                    Database.FILEDIRECTORY.set(fc.getCurrentDirectory().getPath());
                }
            }
        });
        return fc;
    }
    
    /**
     * Loads the Data from a File 
     * @return 
     * true => no errors 
     */
    public static boolean load(File file){
        try {
            if(file==null)
                file = new File(SETTINGSFILE);
            try (ObjectInput in = new ObjectInputStream(new FileInputStream(file))) {
                data= (EnumMap<Database, String>)in.readObject();
                return true;
            }
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
    public static boolean save(File file){
        try {
            if(file==null)
                file = new File(SETTINGSFILE);
            if(!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            if(!file.exists())
                file.createNewFile();
            try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream(file))) {
                out.writeObject(data);
            }
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

    public double getsaved()
    {
        return Tools.strtodsave(get());
    }
    
    @Override
    public String toString() {
        return Database.get(this);
    }

    public static Database getWorkspace(int i)
    {
        switch(i)
        {
            case 0:
            default:
                return WORKSPACE0;
            case 1:
                return WORKSPACE1;
            case 2:
                return WORKSPACE2;
        }
    }

    public static Database getBacklash(int i)
    {
        switch(i)
        {
            case 0:
            default:
                return BL0;
            case 1:
                return BL1;
            case 2:
                return BL2;
        }
    }
    
}
