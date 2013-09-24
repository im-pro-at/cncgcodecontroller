/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Patrick
 */
public class CNCCommand {

    public static class Transform
    {
        private final double movex;
        private final double movey;
        private final boolean mirrorx;
        private final boolean mirrory;

        public Transform(double movex, double movey, boolean mirrorx, boolean mirrory) {
            this.movex = movex;
            this.movey = movey;
            this.mirrorx = mirrorx;
            this.mirrory = mirrory;
        }
        
        private double x(double x)
        {
            return movex+x*(mirrorx?-1:1);
        }
        
        private double y(double y)
        {
            return movey+y*(mirrory?-1:1);
        }

        private double t(int i, double d) {
            switch(i)
            {
                case 0:
                    return x(d);
                case 1:
                    return y(d);
            }
            
            return d;
        }
        
    }
    
    public static class Move
    {
        private double[] s;
        private double[] e;
        private Type t;

        public Move(double[] s, double[] e, Type t) {
            this.s = s;
            this.e = e;
            this.t = t;
        }

        public double[] getStart() {
            return s;
        }
        public double[] getEend() {
            return e;
        }
        public Type getType() {
            return t;
        }
        
        public double getDistance()
        {
            double dx=s[0]-e[0];
            double dy=s[1]-e[1];
            double dz=s[2]-e[2];
            return Math.sqrt(dx*dx+dy*dy+dz*dz);
        }
    }        
    
    public static class Calchelper
    {
        double[] axes= new double[]{Double.NaN,Double.NaN,Double.NaN,Double.NaN}; //X,Y,Z,F

        Type lastMovetype= Type.UNKNOWN;
        double secounds=0;
        
        @Override
        protected Calchelper clone() {
            Calchelper c= new Calchelper();
            
            c.axes=this.axes.clone();
            c.lastMovetype=this.lastMovetype;
            c.secounds=this.secounds;
            return c;
        }

        @Override
        public String toString() {
            return "axes=" + Arrays.toString(axes) + ", lastMovetype=" + lastMovetype+ ", time="+Tools.formatDuration((long)secounds);
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
        G0(Color.black), 
        G1(Color.orange.darker()), 
        ARC(Color.orange),
        HOMEING(Color.orange),
        SETPOS(Color.orange),
        
        //Settings
        STARTCOMMAND(Color.blue),  
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

        public Color getColor() {
            return color;
        }

        void setupGraphicsOptions(Graphics2D g, boolean selected) {
            g.setStroke(new BasicStroke((float)(double)Tools.strtodsave(Database.TOOLSIZE.get()), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if(!selected)
            {
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 200));
            }    
            else
            {
                g.setColor(Color.red);
            }

        }
    }    
    
    
    private State state=State.UNKNOWN;
    private Type type= Type.UNKNOWN;
    private String command;
    private Calchelper cin;
    private Calchelper cout;
    private CommandParsing p;
    private String message="";

    public CNCCommand(String command) {
        this.command = command;  
    }
    
    public static CNCCommand getStartCommand()
    {
        CNCCommand c=new CNCCommand("Start Command");
        c.state= State.NORMAL;
        c.type= Type.STARTCOMMAND;
        c.cin=new Calchelper();
        c.cout= new Calchelper();
        c.p= new CommandParsing("");
        return c;
    }
    
    public State calcCommand(Calchelper c)
    {
        if(type==Type.STARTCOMMAND)
            return State.NORMAL;
        
        state= State.NORMAL;
        this.cin= c.clone();
        this.cout= c;

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
            if(!p.get(0).isint)
            {
                state=State.ERROR;
                message+="Codenumber is not Natural! ";
            }
            int cnumber= (int)p.get(0).value;
            
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
                            message+="Not Implemented jet may cause problems! ";
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
                            
                        case 110:
                            type= Type.EMPTY;
                            state= State.ERROR;
                            message+="NEVER USE M110! IT CAUSES COMMUNICATION POBLEMS!!! ";
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
                boolean all=true;
                for(int i=0;i<3;i++)
                {
                    if(p.contains(CommandParsing.axesName[i]))
                    {
                        c.axes[i]=0.0;
                        all=false;
                    }
                }
                if(all)
                {
                    for(int i=0;i<3;i++)
                        c.axes[i]=0.0;
                }
                if(state== State.NORMAL)
                    state=State.WARNING;
                message+="Homing will not be Repositioniged! ";
                break;
                
            case SETPOS:
                for(int i=0;i<3;i++)
                    if(p.contains(CommandParsing.axesName[i]))
                        c.axes[i]=p.get(CommandParsing.axesName[i]).value;
                if(state== State.NORMAL)
                    state=State.WARNING;
                message+="Setpos will not be Repositioniged! ";
                break;
            
            case TOOLCHANGE:
                c.lastMovetype=type;
                if(!p.contains('T') || !p.get('T').isint)
                {
                    if(state== State.NORMAL)
                        state=State.WARNING;
                    message+="Toolchnage Number is not a Number / or missing! ";
                }
                for(int i=0;i<3;i++)
                    c.axes[i]=Double.NaN;
                
                break;
                
            case UNKNOWN:
                state=State.ERROR;
                message+="Unknown Code! ";
                break;
        }

        //Calc Time used:
        Move[] moves =getMoves();
        if(moves.length>0)
            for(Move move:moves)
            {
                //calc distance
                double d=move.getDistance();
                //get feedrate
                double f=c.axes[3];
                if(type==Type.G0)
                    f=Tools.strtodsave(Database.GOFEEDRATE.get());
                c.secounds+=d/f *60;

                //If not a Number:
                if(Double.isNaN(c.secounds))
                    c.secounds=cin.secounds;
            }
        
        //Test movements
        switch(type)
        {
            case G0:
            case G1:
            case ARC:
                for(Move move:moves)
                {
                    boolean used=false;
                    for(int i=0;i<3;i++)
                        if(move.s[i]!=move.e[i] && !Double.isNaN(move.e[i]))
                            used=true;
                    if(!used)
                    {
                        if(state== State.NORMAL)
                            state=State.WARNING;
                        message+="Command Without any Moving! ";
                    }
                }
        }
        
        this.cout= c.clone();

        return state;
    }

    public Move[] getMoves()
    {
        //calc move
        switch(type)
        {
            case G0:
            case G1:
            case HOMEING:
            case SETPOS:
                return new Move[]{new Move(Arrays.copyOfRange(cin.axes, 0, 3), Arrays.copyOfRange(cout.axes, 0, 3), type)};
                
            case ARC:
                //TODO!
                break;
        }
        return new Move[0];
    }

    String[] execute(Transform t) {
        ArrayList<String> cmds= new ArrayList<>();
        String cmd;
        switch(type)
        {
            //Move
            case G0:
            case G1:
            case ARC:
                Move[] moves= getMoves();
                for(Move move:moves)
                {
                    //Name
                    cmd="G1";
                    if(type==Type.G0) cmd="G0";
                           
                    boolean domove=false;
                    //Cordinates
                    for(int i=0;i<3;i++)
                        if(move.s[i]!=move.e[i] && !Double.isNaN(move.e[i]))
                        {
                            cmd+=" "+CommandParsing.axesName[i]+Tools.dtostr(t.t(i,move.e[i]));
                            domove=true;
                        }
                    if(!domove)
                        continue;
                    
                    //Feedrate
                    if(cin.lastMovetype!=type)
                    {
                        if(type==Type.G0)
                            cmd+=" "+CommandParsing.axesName[3]+Database.GOFEEDRATE.get();
                        else if(!Double.isNaN(cout.axes[3]))
                            cmd+=" "+CommandParsing.axesName[3]+Tools.dtostr(cout.axes[3]);
                    }
                    cmds.add(cmd);
                }
                break;

            //Settings
            case STARTCOMMAND:
                cmds.addAll(Arrays.asList(Database.STARTCODE.get().split("\n")));
                break;
            case TOOLCHANGE:
                cmd=Database.TOOLCHANGE.get();
                if(p.contains('T'))
                    cmd=cmd.replace("?", ""+(int)p.get('T').value);
                cmds.addAll(Arrays.asList(cmd.split("\n")));
                break;
            case SPINDELON:
                cmds.addAll(Arrays.asList(Database.SPINDLEON.get().replace("?", ""+(int)p.get('M').value).split("\n")));
                break;
            case SPINDELOFF:
                cmds.addAll(Arrays.asList(Database.SPINDLEOFF.get().split("\n")));
                break;

            //Send directly:
            case CORDINATEMODE:
            case HOMEING:
            case SETPOS:
            case GXX:
            case MXX: 
                cmds.add(command.split(";")[0]);
                break;
        }
        
        return cmds.toArray(new String[0]);
    }



    public Color getBColor() {
        return state.getColor();
    }
    public Color getFColor(){
        return type.getColor();
    }

    public Type getType()
    {
        return type;
    }
    
    public State getState() {
        return state;
    }

    @Override
    public String toString() {
        return command;
    }

    public String getInfos(Transform t) {
        String s= command +"\n"+
                "  Message: "+message+"\n"+
                "  Type:    "+type+"\n"+
                "  Parser:  "+p+"\n"+
                "  Contex:  "+cin+"\n"+
                "  Execute: ";
        for(String cmd:execute(t))
            s+="\n            --> "+cmd;
        
        return s;
    }

    public long getSecounds()
    {
        return (long)cout.secounds;
    }
    
}
