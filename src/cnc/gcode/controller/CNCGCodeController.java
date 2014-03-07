/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.text.Utilities;

/**
 *
 * @author patrick
 */
public class CNCGCodeController {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        
        if(args.length!=0)
        {
            //Usage:
            if(!args[0].equals("-postprocessor") || args.length!=11)
            {
                System.out.println("Usage:");
                System.out.println("java -jar "+Tools.getJarName()+" -postprocessor [input file] [output file] [max time for optimization in seconds] [move x] [move y] [mirror x (true/false)] [mirror y (true/false)] [backlash x] [backlash y] [backlash z]");
                System.exit(1);
            }

            //Load data:
            File fin= new File(args[1]);
            if(!fin.exists())
            {
                System.out.println("inout file does not exist!");
                System.exit(1);
            }
            if(!fin.canRead())
            {
                System.out.println("inout file not readable!");
                System.exit(1);
            }
            File fout= new File(args[2]);
            Database.OPTIMISATIONTIMEOUT.set(""+Integer.parseInt(args[3]));
            double movex = Double.parseDouble(args[4]);
            double movey = Double.parseDouble(args[5]);
            boolean mirrorx = Boolean.parseBoolean(args[6]);
            boolean mirrory = Boolean.parseBoolean(args[7]);
            Database.BL0.set(""+Double.parseDouble(args[8]));
            Database.BL1.set(""+Double.parseDouble(args[9]));
            Database.BL2.set(""+Double.parseDouble(args[10]));

            //Load File
            System.out.println("Loading file ...");
            CNCCommand.Calchelper c= new CNCCommand.Calchelper();
            ArrayList<CNCCommand> cmds= new ArrayList<>();

            String line;
            int warings=0;
            int errors=0;
                
            try (BufferedReader br = new BufferedReader(new FileReader(fin))) {
                while((line = br.readLine() )!=null)
                {
                    CNCCommand command= new CNCCommand(line);
                    CNCCommand.State t=command.calcCommand(c);
                    if(t==CNCCommand.State.WARNING)
                        warings++;
                    if(t==CNCCommand.State.ERROR)
                        errors++;
                    cmds.add(command);
                }
            }
            double maxTime=c.secounds;
            System.out.println("File loaded with "+warings+" Warnings and "+errors+" Errors!");
            
            //Optimize
            CNCCommand.Optimiser o= new CNCCommand.Optimiser(new CNCCommand.Optimiser.IProgress() {
                    String lastmessage="";
                    @Override
                    public void publish(String message, int progess) throws MyException {
                        if(!lastmessage.equals(message))
                        {
                            lastmessage=message;
                            System.out.println(message);
                        }
                    }
                });

            try {
                //Not much to do the CNCOpimiser does all the work :-)
                cmds=o.execute(cmds);
            } catch (MyException ex) {
                System.out.println(ex.getMessage());
                System.exit(1);
            }

            //Process new comands
            System.out.println("Process new comands ...");
            c= new CNCCommand.Calchelper();
            warings=0;
            errors=0;
            for(int i=0;i<cmds.size();i++)
            {
                CNCCommand command=cmds.get(i);
                CNCCommand.State t=command.calcCommand(c);
                if(t==CNCCommand.State.WARNING)
                    warings++;
                if(t==CNCCommand.State.ERROR)
                    errors++;
            }
            
            System.out.println("Optimized! Saved time: "+Tools.formatDuration((long)(maxTime-c.secounds)) +"! \nCommands now have "+warings+" Warnings and "+errors+" Errors!");

            //Process new comands
            System.out.println("Export comands ...");
            PrintWriter export=new PrintWriter(fout);
            CNCCommand.Transform t= new CNCCommand.Transform(movex, movey, mirrorx, mirrory);            

            for(int i=0;i<cmds.size();i++)
            {
                CNCCommand cmd=cmds.get(i);
                export.println(";"+cmd.toString());

                for(String execute:cmd.execute(t,false))
                {
                    export.println(execute);
                }
            }

            export.close();
            
            System.out.println("Done!");
            
            System.exit(0);
        }
        
        try{
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
        
        
        //Run Mainwindow 
        new MainForm().setVisible(true);      
        
    }
    
    
}
