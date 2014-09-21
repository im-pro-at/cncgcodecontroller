/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import cnc.gcode.controller.communication.Communication;
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
public enum DatabaseV2 {

    //Port Settings
    PORT,
    SPEED("9"),
    
    //Control
    HOMING("2"), //Homing Point 0= upper left; 1= upper right; 2= lower left; 3= lower right;
    MAXFEEDRATE(Tools.dtostr(500.0)), 
    WORKSPACE0(Tools.dtostr(168.0)),
    WORKSPACE1(Tools.dtostr(101.0)),
    WORKSPACE2(Tools.dtostr(50.0)),
    
    //CNC
    FILEDIRECTORY(System.getProperty("user.home")),
    STARTCODE,
    TOOLCHANGE("M6 T?"),
    SPINDLEON("M?"),
    SPINDLEOFF("M5"),
    GOFEEDRATE(Tools.dtostr(500.0)),
    TOOLSIZE(Tools.dtostr(0.5)),
    OPTIMISATIONTIMEOUT(Tools.dtostr(20)),
    
    //Autoleveling
    ALZERO(Tools.dtostr(0.0)),
    ALMAXPROBDEPTH(Tools.dtostr(5.0)), 
    ALSAVEHEIGHT(Tools.dtostr(5.0)),
    ALCLEARANCE(Tools.dtostr(2.0)),
    ALFEEDRATE(Tools.dtostr(150.0)),
    ALDISTANCE(Tools.dtostr(40.0)),
    ALMAXMOVELENGTH(Tools.dtostr(1.0)), 
    ALSTARTCODE("G0 X11 Y11\nG28 Z"), 
    
    //ARC
    ARCSEGMENTLENGTH(Tools.dtostr(0.1)),
    
    //Backlash
    BL0(Tools.dtostr(0.0)),
    BL1(Tools.dtostr(0.0)),
    BL2(Tools.dtostr(0.0)),
    
    //Modal G1
    G1MODAL("1"), //"0" == off, "1"==on
    
    //Communication Type
    COMTYPE(Communication.MARLIN.toString()),   
    
    
    ;
    
    private final String defaultValue;
    private final static String SETTINGSFILE = System.getProperty("user.home") + File.separator + ".cnccgcodecontroller" + File.separator + "Settings.ois";

    private DatabaseV2(String defaultvalue)
    {
        this.defaultValue = defaultvalue;
    }
    private DatabaseV2()
    {
        this.defaultValue = "";
    }
   
    private static EnumMap<DatabaseV2, String> data = new EnumMap<>(DatabaseV2.class);
    
    public static JFileChooser getFileChooser()
    {
        final JFileChooser fc = new JFileChooser();
        //set currantdiroctary
        try {
            fc.setCurrentDirectory(new File(DatabaseV2.FILEDIRECTORY.get()));
        }
        catch(Exception ex){
            //Its ok ;-)
        }
        fc.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(evt.getPropertyName()))
                {
                    DatabaseV2.FILEDIRECTORY.set(fc.getCurrentDirectory().getPath());
                }
            }
        });
        return fc;
    }
    
    /**
     * Loads the Data from a File 
     * @param file
     * @return 
     * true => no errors 
     */
    public static boolean load(File file){
        try {
            if(file== null)
            {
                file = new File(SETTINGSFILE);
            }
            try (ObjectInput in = new ObjectInputStream(new FileInputStream(file))) 
            {
                EnumMap temp =(EnumMap)in.readObject();
                try
                {
                    EnumMap<DatabaseV2, String> tdata =(EnumMap<DatabaseV2, String>)temp;
                    tdata.put(DatabaseV2.PORT, tdata.get(DatabaseV2.PORT)); //Test if Database V2 => if not exeption is thrown
                    data=tdata;
                    return true;
                }
                catch(Exception ex){
                    Logger.getLogger(DatabaseV2.class.getName()).log(Level.SEVERE, null, ex); 
                    
                    EnumMap<Database, String> tdata =(EnumMap<Database, String>)temp;
                    tdata.put(Database.PORT, tdata.get(Database.PORT)); //Test if Database V1 => if not exeption is thrown

                    data= new EnumMap<>(DatabaseV2.class);
                    
                    for(Database e: Database.values()){
                        if(tdata.containsKey(e)){
                            data.put(e.getLink(), tdata.get(e));
                        }
                    }
                    return true;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(DatabaseV2.class.getName()).log(Level.SEVERE, null, ex); 
            return false;
        }
    }
    
    /**
     * Saves the Data to a File
     * @param file
     * @return 
     * true => no errors
     */
    public static boolean save(File file){
        try {
            if(file == null)
            {
                file = new File(SETTINGSFILE);
            }
            if(file.getParentFile().exists() == false)
            {
                file.getParentFile().mkdirs();
            }
            if(file.exists() == false)
            {
                file.createNewFile();
            }
            try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream(file))) 
            {
                out.writeObject(data);
            }
            return true;
        } catch (IOException ex) {
            Logger.getLogger(DatabaseV2.class.getName()).log(Level.SEVERE, null, ex); 
            return false;
        }
    }

    public synchronized static void set(DatabaseV2 name, String value)
    {
       data.put(name, value);        
    }
    
    public synchronized static String get(DatabaseV2 name)
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
        DatabaseV2.set(this, value);
    }
    
    public String get()
    {
        return DatabaseV2.get(this);
    }

    public double getsaved()
    {
        return Tools.strtodsave(get());
    }
    
    @Override
    public String toString() {
        return DatabaseV2.get(this);
    }

    public static DatabaseV2 getWorkspace(int i)
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

    public static DatabaseV2 getBacklash(int i)
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
