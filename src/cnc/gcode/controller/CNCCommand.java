/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.Color;
import java.util.Arrays;

/**
 *
 * @author Patrick
 */
public class CNCCommand {

    public static class Calchelper
    {
        double[] axes= new double[]{0.0,0.0,0.0,0.0}; //X,Y,Z,F
        Type lastMovetype= Type.UNKNOWN;
        
        @Override
        protected Calchelper clone() {
            Calchelper c= new Calchelper();
            
            c.axes=this.axes.clone();
            c.lastMovetype=this.lastMovetype;
            return c;
        }

        @Override
        public String toString() {
            return "axes=" + Arrays.toString(axes) + ", lastMovetype=" + lastMovetype;
        }

        
        
    }
    
    public enum State {

        UNKNOWN(Color.LIGHT_GRAY), 
        NORMAL( Color.white),
        WARNING(Color.yellow), 
        ERROR(Color.red),
        ;
        
        private Color color;

        private State(Color color) {
            this.color = color;
        }

        private Color getColor() {
            return color;
        }
    }

    public enum Type {

        UNKNOWN(Color.red), 
        EMPTY(Color.lightGray), 
        
        //Move
        G0(Color.orange), 
        G1(Color.orange), 
        ARC(Color.orange),
        HOMEING(Color.orange),
        SETPOS(Color.orange),
        
        //Settings
        TOOLCHANGE(Color.blue), 
        SPINDELON(Color.blue),
        SPINDELOFF(Color.blue),
        CORDINATEMODE(Color.darkGray),
        
        //Others
        GXX(Color.darkGray),
        MXX(Color.darkGray), 
        ENDPORGRAM(Color.red),
        
        ;
        
        private Color color;

        private Type(Color color) {
            this.color = color;
        }

        private Color getColor() {
            return color;
        }
    }    
    
    
    private State state;
    private Type type= Type.UNKNOWN;
    private String command;
    private Calchelper c;
    private CommandParsing p;
    private String message="";

    public CNCCommand(String command) {
        this.command = command;  
        this.state= State.UNKNOWN;
    }
    
    public State calcCommand(Calchelper c)
    {
        state= State.NORMAL;
        this.c= c.clone();

        //Phars the command
        p= new CommandParsing(command);
        if(p.iserror()==true)
        {
            state= State.ERROR;
            message+="Parsing Error! ";
        }
        
        //Determind Type
        if(p.isEmpty())
        {
            //Empty
            type= Type.EMPTY;
        }
        else
        {
            int cnumber= (int)((double)p.get(0).value);
            if(p.get(0).value!=cnumber)
            {
                state=State.ERROR;
                message+="Codenumber is not Natural! ";
            }
            
            switch(p.get(0).letter)
            {
                case 'G':
                    switch(cnumber)
                    {
                        case 0:
                            type=Type.G0;
                            break;
                        case 1:
                            type=Type.G1;
                            break;
                        case 2:
                        case 3:
                            type=Type.ARC;
                            if(state== State.NORMAL)
                                state=State.WARNING;
                            message+="Not Implemented May cause problems! ";
                            break;
                        case 28:
                            type=Type.HOMEING;
                            break;
                        
                        case 92:
                            type=Type.SETPOS;
                            if(state== State.NORMAL)
                                state=State.WARNING;
                            message+="May cause problems! ";
                            break;
                            
                        case 91: //Relative Positioning
                            type=Type.GXX;
                            if(state== State.NORMAL)
                                state=State.WARNING;
                            message+="Not Implemented May cause problems! ";
                            break;
                            
                        case 20:
                        case 21:
                            type=Type.CORDINATEMODE;
                            break;
                            
                        case 4: //Dwell (Pause)
                        case 90: //Absolute Positioning
                            type= Type.GXX;
                            break;
                    }
                    break;
                case 'M':
                    switch(cnumber)
                    {
                        case 6:
                            type=Type.TOOLCHANGE;
                            break;
                        
                        case 3:
                        case 4:
                            type= Type.SPINDELON;
                            break;

                        case 5:
                            type= Type.SPINDELOFF;
                            break;
                            
                        case 0: //Pause
                        case 1: //Pause
                            type= Type.MXX;
                            break;

                        case 2:
                            type= Type.ENDPORGRAM;
                            break;
                            
                    }
                    break;
            }
        }
        
        //Do Nessassary Checking
        switch(type)
        {
            case ARC:
                if(p.contains('Z')){
                    state=State.ERROR;
                    message+="ARC in Z not Sopported! ";
                }
            case G0:
            case G1:
                if((p.contains('X')|| p.contains('Y')) && p.contains('Z')){
                    if(state== State.NORMAL)
                        state=State.WARNING;
                    message+="[X,Y] + Z Move makes Problems with Visualisation! ";
                }
                for(int i=0;i<4;i++)
                    if(p.contains(CommandParsing.axesName[i]))
                        c.axes[i]=p.get(CommandParsing.axesName[i]).value;
                c.lastMovetype=type;
                break;
                
            case HOMEING:
                for(int i=0;i<3;i++)
                    if(p.contains(CommandParsing.axesName[i]))
                        c.axes[i]=0.0;
                break;
                
            case SETPOS:
                for(int i=0;i<3;i++)
                    if(p.contains(CommandParsing.axesName[i]))
                        c.axes[i]=p.get(CommandParsing.axesName[i]).value;
                break;
            
            case TOOLCHANGE:
                c.lastMovetype=type;
                break;
                
            case UNKNOWN:
                state=State.ERROR;
                message+="Unknown Code! ";
                break;
        }
                
        return state;
    }
    

    public Color getBColor() {
        return state.getColor();
    }
    public Color getFColor(){
        return type.getColor();
    }

    public State getState() {
        return state;
    }

    @Override
    public String toString() {
        return command ;
    }

    Object getInfos() {
        return command +"\n"+
                "  Message: "+message+"\n"+
                "  Type:    "+type+"\n"+
                "  Parser:  "+p.toString()+"\n"+
                "  Contex:  "+c.toString();
    }

    
    
}
