/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

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
        ALSTARTCOMMAND(Color.blue),  
        TOOLCHANGE(Color.blue), 
        SPINDELON(Color.blue),
        SPINDELOFF(Color.blue),
        CORDINATEMODE(Color.darkGray),
        
        //Others
        GXX(Color.darkGray),
        MXX(Color.darkGray), 
        ENDPORGRAM(Color.red),
        
        ;
        
        /*
         * Commands are allwoed for optimisation between the last and the first G1 move:
         */
        public final static List<Type> ALLOWEDFOROPTIMISER = Collections.unmodifiableList(Arrays.asList(G0,G1,ARC,SPINDELON,SPINDELOFF,MXX)); 
        
        private Color color;

        private Type(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }

        void setupGraphicsOptions(Graphics2D g, boolean selected) {
            g.setStroke(new BasicStroke((float)Database.TOOLSIZE.getsaved(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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
        CNCCommand c=new CNCCommand("(Start Command)");
        c.type= Type.STARTCOMMAND;
        return c;
    }
    public static CNCCommand getALStartCommand()
    {
        CNCCommand c=new CNCCommand("(Autoleveler Start Command)");
        c.type= Type.ALSTARTCOMMAND;
        return c;
    }
    
    @Override
    public CNCCommand clone()
    {
        if(type==Type.STARTCOMMAND)
            return getStartCommand();
        if(type==Type.ALSTARTCOMMAND)
            return getALStartCommand();
        return new CNCCommand(command);
    }
    
    public State calcCommand(Calchelper c)
    {
        if(state!=State.UNKNOWN)
            throw new UnsupportedOperationException();
            
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
            if(type==Type.UNKNOWN)
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
                    f=Database.GOFEEDRATE.getsaved();
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
                    
                    if(move.s[2]!=move.e[2] && !Double.isNaN(move.e[2]))
                    {
                        //Z move
                        if(Double.isNaN(move.s[0]) || Double.isNaN(move.s[1]))
                        {
                            if(state== State.NORMAL)
                                state=State.WARNING;
                            message+="Z movement without knowing X/Y! Autoleveling will not work! ";
                        }
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

    String[] execute(Transform t, boolean autoleveling) {
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
                        if(autoleveling && i==2 && !Double.isNaN(move.e[0]) && !Double.isNaN(move.e[1]) && !Double.isNaN(move.e[2]) )
                        {
                            //Autoleveld Z
                            cmd+=" "+CommandParsing.axesName[i]+Tools.dtostr(AutoLevelSystem.correctz(t.t(0,move.e[0]), t.t(1,move.e[1]), move.e[2]));
                            domove=true;
                        }
                        else if((move.s[i]!=move.e[i] || autoleveling )&& !Double.isNaN(move.e[i]))
                        {
                            //Normal Axis
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
            case ALSTARTCOMMAND:
                cmds.addAll(Arrays.asList(Database.ALSTARTCODE.get().split("\n")));
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

    public String getInfos(Transform t,boolean autoleveling) {
        String s= command +"\n"+
                "  Message: "+message+"\n"+
                "  Type:    "+type+"\n"+
                "  Parser:  "+p+"\n"+
                "  Contex:  "+cin+"\n"+
                "  Execute: ";
        for(String cmd:execute(t,autoleveling))
            s+="\n            --> "+cmd;
        
        return s;
    }

    public long getSecounds()
    {
        return (long)cout.secounds;
    }
    
    public static class Optimiser {
        //All Your Base Are Belong To Us
        public static class OElement
        {
            public int startcmd;
            public int endcmd;
            public Point2D s;
            public Point2D e;

            public OElement(int startcmd, int endcmd, Point2D s, Point2D e) {
                this.startcmd = startcmd;
                this.endcmd = endcmd;
                this.s = s;
                this.e = e;
            }
            
            
        }
        
        public interface IProgress{
            public void publish(String message, int progess) throws MyException;
        }

        IProgress progress;

        public Optimiser(IProgress progress) {
            this.progress = progress;
        }

        public ArrayList<CNCCommand> execute(ArrayList<CNCCommand> incmds) throws MyException{
            progress.publish("Checking", 0);

            //search last g1
            int lg1=-1;
            for(int i=0;i<incmds.size();i++)
                if(incmds.get(i).type==Type.G1)
                    lg1=i;

            if(lg1==-1)
                throw new MyException("No G1 Found!");
            
            boolean g1found=false;
            double fastmovelevel=Double.NaN;
            ArrayList<Integer> G0moves= new ArrayList<>();  
            for(int cmdindex=0;cmdindex<lg1;cmdindex++)
            {
                progress.publish("Checking", 100*cmdindex/lg1);
                CNCCommand cmd=incmds.get(cmdindex);
                
                g1found|=(cmd.type==Type.G1);
                if(g1found)
                {
                    if(!Type.ALLOWEDFOROPTIMISER.contains(cmd.type))
                        throw new MyException("Unsupported Commands found!",cmd);
                    
                    if(cmd.type==Type.G0)
                    {
                        boolean[] move= new boolean[3];
                        for(int i=0;i<3;i++)
                        {
                            if(Double.isNaN(cmd.cin.axes[i]) || Double.isNaN(cmd.cout.axes[i]))
                                throw new MyException("G0 command found with undefind context!",cmd);
                            if(cmd.cin.axes[i]!=cmd.cout.axes[i])
                                move[i]=true;
                        }
                        if((move[0] || move[1]) && move[2])
                            throw new MyException("Move of [XY] and Z not supporded!",cmd);             
                        if(move[0] || move[1])
                        {
                            //G0 Move to other position => this is what wie want to optimise
                            if(Double.isNaN(fastmovelevel))
                                fastmovelevel=cmd.cin.axes[2];
                            else if(fastmovelevel!=cmd.cin.axes[2])
                                throw new MyException("G0 move of XY not at the same level!",cmd);                      
                            
                            G0moves.add(cmdindex);
                        }
                    }
                }
            }
            
            if(G0moves.size()<2)
                throw new MyException("Nothing to Optimise");
            
            //Optimisation:
            progress.publish("Optimising", 0);

            LinkedList<OElement> in= new LinkedList<>();
            int startcmd= G0moves.get(0);
            for(int i=1;i<G0moves.size();i++)
            {
                in.add(new OElement(startcmd,G0moves.get(i),
                        new Point2D.Double(incmds.get(startcmd).cout.axes[0], incmds.get(startcmd).cout.axes[1]),
                        new Point2D.Double(incmds.get(G0moves.get(i)).cin.axes[0], incmds.get(G0moves.get(i)).cin.axes[1])));
                startcmd=G0moves.get(i);
            }
            LinkedList<OElement> out= new LinkedList<>();
            
            Point2D aktpos= new Point2D.Double(incmds.get(G0moves.get(0)).cin.axes[0], incmds.get(G0moves.get(0)).cin.axes[1]);
            while(in.size()>0)
            {
                progress.publish("Optimising", 100*out.size()/G0moves.size());
    
                double d=Double.MAX_VALUE;
                OElement nearest=null;
                for(OElement e:in)
                {
                    double aktd=aktpos.distance(e.s);
                    if(d>aktd)
                    {
                        nearest=e;
                        d=aktd;
                    }
                }
                
                if(nearest==null)
                    throw new MyException("Schould not happen!");
                
                in.remove(nearest);
                out.add(nearest);
                aktpos=nearest.e;
            }
            
            //Rebuild List
            progress.publish("Reorder", 0);
            
            ArrayList<CNCCommand> outcmds= new ArrayList<>(incmds.size());
            for(int i=0;i<G0moves.get(0);i++)
                outcmds.add(incmds.get(i).clone());
            
            for(OElement e:out)
            {
                progress.publish("Reorder", 100*outcmds.size()/incmds.size());
                //Make G0 move
                outcmds.add(new CNCCommand("G0 X"+Tools.dtostr(e.s.getX())+" Y"+Tools.dtostr(e.s.getY())));

                //Copy elements
                for(int i=e.startcmd+1;i<e.endcmd;i++)
                    outcmds.add(incmds.get(i).clone());
                
            }
            
            //Last G0 move
            int lastG0move=G0moves.get(G0moves.size()-1);
            outcmds.add(new CNCCommand("G0 X"+Tools.dtostr(incmds.get(lastG0move).cout.axes[0])+" Y"+Tools.dtostr(incmds.get(lastG0move).cout.axes[1])));

            //Copy rest
            for(int i=lastG0move+1;i<incmds.size();i++)
                outcmds.add(incmds.get(i).clone());
            
            if(incmds.size()!=outcmds.size())
                throw new MyException("Internal Error size not equal!");
            
            return outcmds;
        }



    }

}
