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
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author Patrick
 */
public class CNCCommand {

    public static interface CNCComandSource
    {
        public CNCCommand getcommand() throws Exception;
    }
    
    public static class Transform
    {
        private final double movex;
        private final double movey;
        private final double movez;
        private final boolean mirrorx;
        private final boolean mirrory;

        public Transform(double movex, double movey, double movez, boolean mirrorx, boolean mirrory) {
            this.movex = movex;
            this.movey = movey;
            this.movez = movez;
            this.mirrorx = mirrorx;
            this.mirrory = mirrory;
        }
        
        private double x(double x)
        {
            return movex + x * (mirrorx ? -1:1);
        }
        
        private double y(double y)
        {
            return movey + y * (mirrory ? -1:1);
        }
        
        private double z(double z){
            return z+movez;
        }

        private double t(int i, double d) 
        {
            if(Double.isNaN(d))
            {
                return d;
            }
            
            switch(i)
            {
                case 0:
                    return x(d);
                case 1:
                    return y(d);
                case 2:
                    return z(d);
            }
            return d;
        }
        
    }
    
    public class Move
    {
        private final double[] s;
        private final double[] e;
        private double a;
        private double spindle;
        private final Type t;
        private final boolean xyz;

        public Move(double[] s, double[] e, double a, Type t, boolean xyz, double spindle) {
            this.s = s;
            this.e = e;
            this.t = t;
            this.a = a;
            this.xyz=xyz;
            this.spindle=spindle;
        }
        
        public Move(double[] s, double[] e, double a, Type t, boolean xyz) {
           this.s = s;
           this.e = e;
           this.t = t;
           this.a = a;
           this.xyz=xyz;
           this.spindle = Double.NaN;
       }
        
        public void scalexy(double[] scale){
            for(int i=0;i<2;i++){
                s[i]=s[i]*scale[i];
                e[i]=e[i]*scale[i];
            }
        }

        public double[] getStart() {
            return s;
        }
        public double[] getEnd() {
            return e;
        }
        public Type getType() {
            return t;
        }
        
        public double getDistance()
        {
            double dx = s[0] - e[0];
            double dy = s[1] - e[1];
            double dz = s[2] - e[2];
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        
        public double getA(){
            return a;
        }
        
        public double getSpindle(){
           return spindle;
       }
        
        public double getDistanceXY()
        {
            double dx = s[0]- e[0];
            double dy = s[1]- e[1];
            return Math.sqrt(dx * dx + dy * dy);
        }
        
        public CNCCommand getCNCCommand(){
            return CNCCommand.this;
        }

        public boolean isXyz() {
            return xyz;
        }
        
   }        
   
    public static class Calchelper
    {
        double[] axes= new double[]{Double.NaN,
                                    Double.NaN,
                                    Double.NaN,
                                    Double.NaN}; //X,Y,Z,F
        
        Type lastMovetype = Type.UNKNOWN;
        double seconds = 0;
        
        /**
         * False is positive move
         * True is negative move
         */
        boolean[] lastmovedirection= new boolean[]{true,true,true};


        
        @Override
        protected Calchelper clone() {
            Calchelper c = new Calchelper();
            
            c.axes          = this.axes.clone();
            c.lastMovetype  = this.lastMovetype;
            c.seconds       = this.seconds;
            c.lastmovedirection = this.lastmovedirection.clone();
            return c;
        }

        @Override
        public String toString() {
            return "axes=" + Arrays.toString(axes) + ", lastMovetype=" + lastMovetype + ", time=" + Tools.formatDuration((long)seconds);
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
        G0(null), //From Settings 
        G1(null), //From Settings
        ARC(Color.orange),
        HOMING(Color.orange), 
        SETPOS(Color.orange),
        
        //Settings
        STARTCOMMAND(Color.blue),  
        ALSTARTCOMMAND(Color.blue),  
        TOOLCHANGE(Color.blue), 
        SPINDLEON(Color.blue),
        SPINDLEOFF(Color.blue),
        CORDINATEMODE(Color.darkGray),
        
        //Others
        GXX(Color.darkGray),
        PAUSE(Color.darkGray),
        MXX(Color.darkGray), 
        ENDPROGRAM(Color.red),
        
        ;
        
        /*
         * Commands are allowed for optimisation between the last and the first G1 move:
         */
        public final static List<Type> ALLOWEDFOROPTIMISER = Collections.unmodifiableList(Arrays.asList(EMPTY, G0, G1, ARC, SPINDLEON, SPINDLEOFF, MXX, PAUSE)); 
        
        private final Color color;

        private Type(Color color) 
        {
            this.color = color;
        }

        public Color getColor() 
        {
            switch (this){
                case G0:
                    return new Color(Integer.parseInt(DatabaseV2.CG0.get()));
                case G1:
                    return new Color(Integer.parseInt(DatabaseV2.CG1.get()));
            }
            return color;
        }

        void setupGraphicsOptions(Graphics2D g, boolean selected) {
            setupGraphicsOptions(g, selected, getColor());
        }
        
        static void setupGraphicsOptions(Graphics2D g, boolean selected, Color c) {
            g.setStroke(new BasicStroke((float)DatabaseV2.TOOLSIZE.getsaved(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if(selected == false)
            {
                
                g.setColor(Tools.setAlpha(c,0.8));
            }    
            else
            {
                g.setColor(Color.red);
            }

        }
    }    
    
    
    private State state = State.UNKNOWN;
    private Type type   = Type.UNKNOWN;
    private final String command;
    private Calchelper cin;
    private Calchelper cout;
    private CommandParsing p;
    private String message = "";
    private double[] scale={1,1};
    private double apmm=0;
    private boolean xyzmove=false;

    public CNCCommand(String command) {
        this.command = command;  
    }
    
    private CNCCommand(String command, double scalex, double scaley, double apmm) {
        this.command = command;  
        this.scale[0]=scalex;
        this.scale[1]=scaley;
        this.apmm=apmm;
    }
    
    public static CNCCommand getStartCommand()
    {
        CNCCommand c = new CNCCommand("(Start Command)");
        c.type = Type.STARTCOMMAND;
        return c;
    }
    
    public static CNCCommand getALStartCommand()
    {
        CNCCommand c = new CNCCommand("(Autoleveler Start Command)");
        c.type = Type.ALSTARTCOMMAND;
        return c;
    }
    
    @Override
    public CNCCommand clone()
    {
        if(type == Type.STARTCOMMAND)
        {
            return getStartCommand();
        }
        if(type == Type.ALSTARTCOMMAND)
        {
            return getALStartCommand();
        }
        return new CNCCommand(command,scale[0],scale[1],apmm);
    }
    
    public CNCCommand clone(double scalex, double scaley, double apmm)
    {
        if(type == Type.STARTCOMMAND)
        {
            return getStartCommand();
        }
        if(type == Type.ALSTARTCOMMAND)
        {
            return getALStartCommand();
        }
        return new CNCCommand(command,scalex,scaley,apmm);
    }
    
    public State calcCommand(Calchelper c)
    {
        if(state != State.UNKNOWN)
        {
            throw new UnsupportedOperationException("Recaled cmd");
        }
            
        state = State.NORMAL;
        this.cin = c.clone();
        this.cout = c;

        //Phars the command
        p = new CommandParsing(command);
        if(p.iserror() == true)
        {
            state = State.ERROR;
            message += "Parsing Error! ";
        }
        
        //Determind Type
        if(p.isEmpty())
        {
            //Empty
            if(type == Type.UNKNOWN)
            {
                type = Type.EMPTY;
            }
        }
        else
        {
            //No G no M Command => Modal?
            if(p.get(0).letter!='M' && p.get(0).letter!='G'){
                if(p.contains('X') || p.contains('Y') || p.contains('Z')){
                    if(DatabaseV2.EOnOff.get(DatabaseV2.G0MODAL)==DatabaseV2.EOnOff.ON){
                        //Support Modal G0: X00 Y00 Z00
                        if(c.lastMovetype == Type.G0)
                        {
                            p.insert(0, 'G', 0, true);
                            message += "Using modal G0! ";
                        }
                    }
                    if(DatabaseV2.EOnOff.get(DatabaseV2.G1MODAL)==DatabaseV2.EOnOff.ON){
                        //Support Modal G1: X00 Y00 Z00
                        if(c.lastMovetype == Type.G1)
                        {
                            p.insert(0, 'G', 1, true);
                            message += "Using modal G1! ";
                        }
                    }
                }
            }
            
            if(p.get(0).isint == false)
            {
                state = State.ERROR;
                message += "Codenumber is not Natural! ";
            }
            int cnumber = (int)p.get(0).value;
            
            switch(p.get(0).letter)
            {
                case 'G':
                    switch(cnumber)
                    {
                        case 0:
                            type = Type.G0;
                            break;
                        case 1:
                            type = Type.G1;
                            break;
                        case 2:
                        case 3:
                            type = Type.ARC;
                            message += "ARC moves are experimental and may cause problems.  Please look at the preview to determine if they are correct! ";
                            break;
                        case 28:
                            type = Type.HOMING;
                            break;
                        case 38:
                            type = Type.GXX;
                            state = State.NORMAL;
                            message="Probing!";
                            break;
                        case 92:
                            type = Type.SETPOS;
                            if(state == State.NORMAL)
                            {
                                state = State.WARNING;
                            }
                            message += "May cause problems! ";
                            break;
                            
                        case 91: //Relative Positioning
                            type = Type.GXX;
                            if(state == State.NORMAL)
                            {
                                state = State.WARNING;
                            }
                            message += "Not Implemented & might cause problems! ";
                            break;
                            
                        case 20:
                        case 21:
                            type = Type.CORDINATEMODE;
                            break;
                            
                        case 4: //Dwell (Pause)
                            type = Type.PAUSE;
                            break;
                        case 90: //Absolute Positioning
                            type = Type.GXX;
                            break;
                    }
                    break;
                case 'M':
                    switch(cnumber)
                    {
                        case 6:
                            type = Type.TOOLCHANGE;
                            break;
                        
                        case 3:
                        case 4:
                            type = Type.SPINDLEON;
                            break;

                        case 5:
                            type = Type.SPINDLEOFF;
                            break;
                            
                        case 0: //Pause
                        case 1: //Pause
                            type = Type.MXX;
                            break;

                        case 2:
                            type = Type.ENDPROGRAM;
                            break;
                            
                        case 110:
                            type = Type.EMPTY;
                            state = State.ERROR;
                            message += "NEVER USE M110! IT CAUSES COMMUNICATION PROBLEMS!!! ";
                            break;
                            
                    }
                    break;
            }
        }
        
        //Do necessary checks
        switch(type)
        {
            case ARC:
                if(p.contains('Z') || p.contains('K'))
                {
                    state = State.ERROR;
                    message += "ARC in Z are not supported! ";
                }
                if(p.contains('P') )
                {
                    state = State.ERROR;
                    message += "ARC with P are not supported! ";
                }
                if(p.contains('R') )
                {
                    state = State.ERROR;
                    message += "ARC with R are not supported! ";
                }
                if(!p.contains('I') || !p.contains('I'))
                {
                    state = State.ERROR;
                    message += "ARC has to have I and J as parameters! ";
                }
            case G0:
                if(p.contains('A'))
                {
                    state = State.ERROR;
                    message += "A with G0 Move is dangerous!";
                }
            case G1:
                if((p.contains('X')|| p.contains('Y')) && p.contains('Z'))
                {
                    xyzmove=true;
                }
                for(int i = 0;i < 4;i++)
                {
                    if(p.contains(CommandParsing.axesName[i]))
                    {
                        c.axes[i] = p.get(CommandParsing.axesName[i]).value;
                    }
                }
                c.lastMovetype = type;

                if(type == Type.ARC && p.contains('I') && p.contains('J') && Math.abs(Math.hypot(p.get('I').value, p.get('J').value)-Math.hypot(cin.axes[0]+p.get('I').value-cout.axes[0] , cin.axes[1]+p.get('J').value-cout.axes[1]))>0.01)
                {
                    if(state == State.NORMAL)
                    {
                        state = State.WARNING;
                    }
                    message += "Distance from current point to the center differs form the distance from current to the end point! ";
                }
                break;
                
            case HOMING:
                boolean all = true;
                for(int i = 0;i < 3;i++)
                {
                    if(p.contains(CommandParsing.axesName[i]))
                    {
                        c.axes[i] = 0.0;
                        all = false;
                    }
                }
                if(all)
                {
                    for(int i = 0;i < 3;i++)
                    {
                        c.axes[i] = 0.0;
                    }
                }
                if(state == State.NORMAL)
                {
                    state = State.WARNING;
                }
                message += "Homing will not be repositioned! ";
                break;
                
            case SETPOS:
                for(int i = 0;i < 3;i++)
                {
                    if(p.contains(CommandParsing.axesName[i]))
                    {
                        c.axes[i] = p.get(CommandParsing.axesName[i]).value;
                    }
                }
                if(state == State.NORMAL)
                {
                    state = State.WARNING;
                }
                message += "Setpos will not be repositioned! ";
                break;
            
            case TOOLCHANGE:
                c.lastMovetype = type;
                if(p.contains('T') == false || p.get('T').isint == false)
                {
                    if(state == State.NORMAL)
                    {
                        state = State.WARNING;
                    }
                    message += "Toolchange number is either incorrect or missing! ";
                }
                for(int i = 0;i < 3;i++)
                {
                    c.axes[i] = Double.NaN;
                }
                
                break;
                
            case UNKNOWN:
                state = State.ERROR;
                message += "Unknown Code! ";
                break;
        }

        //Calc Time used:
        Move[] moves = getMoves();    
        if(moves.length > 0)
            for(Move move:moves)
            {
                //calc distance
                double d = move.getDistance();
                //get feedrate
                double f = c.axes[3];
                if(type == Type.G0)
                {
                    f = DatabaseV2.GOFEEDRATE.getsaved();
                }
                
                d=d / f * 60;
                
                //Take A into account
                double da;
                if(apmm==0 || type==Type.G0){
                    da=move.getA()/DatabaseV2.AFEEDRATE.getsaved()*60;
                }
                else{
                    da=(move.getDistanceXY()*apmm)/DatabaseV2.AFEEDRATE.getsaved()*60;            
                }
                
                if(da>d){ 
                    c.seconds += da;                
                }
                else{
                    c.seconds += d;                
                }
                

                //If not a Number:
                if(Double.isNaN(c.seconds))
                {
                    c.seconds = cin.seconds;
                }
            }
        
        //Test movements
        switch(type)
        {
            case G0:
            case G1:
            case ARC:
                for(Move move:moves)
                {
                    boolean used = false;
                    for(int i = 0;i < 3;i++)
                    {
                        if(move.s[i] != move.e[i] && Double.isNaN(move.e[i]) == false)
                        {
                            used = true;
                        }
                    }
                    if(move.a!=0){
                        used=true;
                    }
                    if(Double.isNaN(move.spindle) == false){
                       message += "Spindle speed set";
                       used=true;
                   }
                    if(used == false)
                    {
                        if(state == State.NORMAL)
                        {
                            state = State.WARNING;
                        }
                        message += "Command without any movment!";
                    }
                    
                    if(move.s[2] != move.e[2] && Double.isNaN(move.e[2]) == false)
                    {
                        //Z move
                        if(Double.isNaN(move.s[0]) || Double.isNaN(move.s[1]))
                        {
                            if(state == State.NORMAL)
                            {
                                state = State.WARNING;
                            }
                            message += "Z movement without knowing X/Y! Autoleveling will not work! ";
                        }
                    }
                }
        }
        
        //Last move direction:
        for(Move move:moves)
        {
            for(int i = 0;i < 3;i++)
            {
                if(Double.isNaN(move.s[i]) == false
                  && Double.isNaN(move.e[i]) == false 
                  && move.s[i]!= move.e[i])
                {
                    c.lastmovedirection[i] = move.s[i]<move.e[i];
                }
            }
        }
        
        if(message.equals(""))
        {
            message="OK!";
        }
        
        this.cout = c.clone();

        return state;
    }

    public Move[] getMoves()
    {
        ArrayList<Move> moves = new ArrayList<>();
        //calc move
        switch(type)
        {
            case G0:
            case G1:
            case HOMING:
            case SETPOS:
                if(p.contains('S')){
                  moves.add(new Move(Arrays.copyOfRange(cin.axes, 0, 3), Arrays.copyOfRange(cout.axes, 0, 3),p.contains('A')?p.get('A').value:0.0, type,xyzmove, p.get('S').value));
               } else {
                  moves.add(new Move(Arrays.copyOfRange(cin.axes, 0, 3), Arrays.copyOfRange(cout.axes, 0, 3),p.contains('A')?p.get('A').value:0.0, type,xyzmove));
               }
                break;

            case ARC:
                /* Vector rotation by transformation matrix: r is the original vector, r_T is the rotated vector,
                   and phi is the angle of rotation. Based on the solution approach by Jens Geisler.
                       r_T = [cos(phi) -sin(phi);
                              sin(phi)  cos(phi] * r ;

                   For arc generation, the center of the circle is the axis of rotation and the radius vector is 
                   defined from the circle center to the initial position. Each line segment is formed by successive
                   vector rotations. This requires only two cos() and sin() computations to form the rotation
                   matrix for the duration of the entire arc. Error may accumulate from numerical round-off, since
                   all double numbers are single precision on the Arduino. (True double precision will not have
                   round off issues for CNC applications.) Single precision error can accumulate to be greater than
                   tool precision in some cases. Therefore, arc path correction is implemented. 
                */

                //radius=hypot(I,J)
                double radius = Math.hypot(p.get('I').value, p.get('J').value);
                //isclockwise=true for G2 and false for G3      
                boolean isclockwise = ((int)p.get(0).value) == 2 ? true:false;
                //float center_axis0 = position[axis_0] + offset[axis_0];
                double center_axis0 = cin.axes[0] + p.get('I').value;
                //float center_axis1 = position[axis_1] + offset[axis_1];
                double center_axis1 = cin.axes[1] + p.get('J').value;
                //float r_axis0 = -offset[axis_0];  // Radius vector from center to current location
                double r_axis0 = -p.get('I').value;
                //float r_axis1 = -offset[axis_1];
                double r_axis1 = -p.get('J').value;
                //float rt_axis0 = target[axis_0] - center_axis0;
                double rt_axis0 = cout.axes[0] - center_axis0;
                //float rt_axis1 = target[axis_1] - center_axis1;
                double rt_axis1 = cout.axes[1] - center_axis1;

                // CCW angle between position and target from circle center. Only one atan2() trig computation required.
                double angular_travel = Math.atan2(r_axis0 * rt_axis1 - r_axis1 * rt_axis0,
                                                   r_axis0 * rt_axis0+r_axis1 * rt_axis1);
                if (angular_travel < 0) 
                { 
                    angular_travel += 2 * Math.PI; 
                }
                if (isclockwise)
                { 
                    angular_travel -= 2 * Math.PI; 
                }

                double millimeters_of_travel = angular_travel * radius;
                if (Math.abs(millimeters_of_travel) > 0.00001) 
                {
                    int segments = (int)Math.floor(Math.abs(millimeters_of_travel) / DatabaseV2.ARCSEGMENTLENGTH.getsaved());
                    if(segments == 0)
                    {
                        segments = 1;
                    }

                    double theta_per_segment = angular_travel / segments;

                    double[] position = Arrays.copyOfRange(cin.axes, 0, 3);
                    double sin_T;
                    double cos_T;
                    int i;

                    for (i = 1; i < segments; i++) 
                    { 
                        // Compute exact location by applying transformation matrix from initial radius vector(=-offset).
                        cos_T = Math.cos(i * theta_per_segment);
                        sin_T = Math.sin(i * theta_per_segment);
                        //r_axis0 = -offset[axis_0]*cos_Ti + offset[axis_1]*sin_Ti;
                        r_axis0 = -p.get('I').value * cos_T + p.get('J').value * sin_T;
                        //r_axis1 = -offset[axis_0]*sin_Ti - offset[axis_1]*cos_Ti;
                        r_axis1 = -p.get('I').value * sin_T - p.get('J').value * cos_T;
                        
                        // Update akt_position
                        double[] temp = position.clone();
                        position[0] = center_axis0 + r_axis0;
                        position[1] = center_axis1 + r_axis1;

                        //Add move
                        moves.add(new Move(temp, position.clone(),0, type,xyzmove));
                    }
                    moves.add(new Move(position.clone(),Arrays.copyOfRange(cout.axes, 0, 3),0, type,xyzmove));

                }
                else
                    moves.add(new Move(Arrays.copyOfRange(cin.axes, 0, 3), Arrays.copyOfRange(cout.axes, 0, 3),0, type,xyzmove));
                    
                
                break;
        }
        
        //Scale Moves:
        for(Move move:moves){
            move.scalexy(scale);
        }
        
        return moves.toArray(new Move[0]);
    }

    String[] execute(Transform t, boolean autoleveling, boolean noshort) {
        ArrayList<String> cmds = new ArrayList<>();
        String cmd;
        Move[] moves = getMoves();
        switch(type)
        {
            //Move
            case G1:
            case ARC:
                //Do add laser
                if(apmm>0){
                    for(Move move:moves)
                    {
                        if(Double.isNaN(move.getDistanceXY())==false)
                            move.a=move.getDistanceXY()*apmm;
                    }
                }                
            
            case G0:

                //Do Repositoning
                for(Move move:moves)
                {
                    for(int i = 0;i < 3;i++) //X,Y,Z
                    {
                        move.s[i] = t.t(i,move.s[i]);
                        move.e[i] = t.t(i,move.e[i]);
                    }
                }

                //Autoleveling
                if(autoleveling)
                {
                    //If its long moves make it shorter
                    ArrayList<Move> newmoves = new ArrayList<>(moves.length);
                    for(Move move:moves)
                    {
                        int parts = (int)Math.ceil(move.getDistanceXY() / DatabaseV2.ALMAXMOVELENGTH.getsaved());
                        if(parts == 0)
                        {
                            //no X or Y move
                            newmoves.add(move);
                        }
                        else
                        {
                            for(int part = 0;part < parts;part++)
                            {
                                double[] s = new double[3];
                                double[] e = new double[3];
                                for(int i = 0;i < 3;i++)
                                {
                                    double d = (move.e[i] - move.s[i])/parts;
                                    s[i] = move.s[i] + d * part;
                                    e[i] = move.s[i] + d * (part + 1);
                                }
                                newmoves.add(new Move(s, e ,move.a/parts ,move.t,xyzmove));
                            }
                        }
                    }
                    moves = newmoves.toArray(new Move[0]);

                    //Correct Z
                    for(Move move:moves)
                    {
                        move.s[2] = AutoLevelSystem.correctZ(move.s[0], move.s[1], move.s[2]);
                        move.e[2] = AutoLevelSystem.correctZ(move.e[0], move.e[1], move.e[2]);
                    }
                }
                
                //Correct Backlash  (http://forums.reprap.org/read.php?154,178612 thanks to georgeflug)
                boolean[] direction = cin.lastmovedirection.clone(); //false positive true negative
                ArrayList<Move> newmoves = new ArrayList<>(moves.length);
                for(Move move:moves)
                {
                    Move compmove = new Move(move.s.clone(), move.s.clone(),0, move.t,xyzmove);
                    //calc
                    for(int i = 0;i < 3;i++)
                    {
                        compmove.s[i] = compmove.s[i] + DatabaseV2.getBacklash(i).getsaved() / 2 * (direction[i] ? 1:-1);
                        if(Double.isNaN(move.s[i]) == false 
                           && Double.isNaN(move.e[i]) == false 
                           && move.s[i] != move.e[i])
                        {
                            direction[i] = move.s[i]<move.e[i];
                        }
                        compmove.e[i] = compmove.e[i] + DatabaseV2.getBacklash(i).getsaved() / 2 * (direction[i] ? 1:-1);
                        move.s[i] = compmove.e[i];
                        move.e[i] = move.e[i] + DatabaseV2.getBacklash(i).getsaved() / 2 * (direction[i] ? 1:-1);
                    }
                    
                    if(compmove.getDistance() > 0.00001)
                    {
                       newmoves.add(compmove);
                    }
                    newmoves.add(move);
                }
                moves = newmoves.toArray(new Move[0]);
                
                
                //Make moves command Strings
                boolean feedRateSet = false; 
                for(Move move:moves)
                {
                    //Name
                    cmd = "G1";
                    if(type == Type.G0)
                    { 
                        cmd = "G0";
                    }
                           
                    boolean doMove = false;
                    //Cordinates
                    for(int i = 0;i < 3;i++)
                        if((move.s[i] != move.e[i] || noshort) 
                           && Double.isNaN(move.e[i]) == false)
                        {
                            //Normal Axis
                            cmd += " " + CommandParsing.axesName[i] + Tools.dtostr(move.e[i]);
                            doMove = true;
                        }
                    if(move.a!=0 && Double.isNaN(move.a)==false){
                        cmd += " " + 'A' + Tools.dtostr(move.a);                        
                        doMove = true;
                    }
                    
                    if(Double.isNaN(move.spindle)==false){
                       cmd += " " + 'S' + Tools.dtostr(move.spindle);                        
                       doMove = true;
                   }
                    
                    if(doMove == false && !Double.isNaN(cin.axes[3]) && !Double.isNaN(cout.axes[3]) && cin.axes[3] == cout.axes[3])
                    {
                        continue;
                    }
                    
                    //Feedrate
                    if((noshort || cin.lastMovetype!=type || (!Double.isNaN(cin.axes[3]) && !Double.isNaN(cout.axes[3]) && cin.axes[3] != cout.axes[3])) && !feedRateSet)
                    {
                        if(type==Type.G0)
                        {
                            cmd += " " + CommandParsing.axesName[3] + DatabaseV2.GOFEEDRATE.get();
                        }
                        else if(!Double.isNaN(cout.axes[3]))
                        {
                            cmd += " " + CommandParsing.axesName[3] + Tools.dtostr(cout.axes[3]);
                        }
                        feedRateSet = true;
                    }
                    cmds.add(cmd);
                }
                break;

            //Settings
            case STARTCOMMAND:
                cmds.addAll(Arrays.asList(DatabaseV2.STARTCODE.get().split("\n")));
                break;
            case ALSTARTCOMMAND:
                cmds.addAll(Arrays.asList(DatabaseV2.ALSTARTCODE.get().split("\n")));
                break;
            case TOOLCHANGE:
                cmd=DatabaseV2.TOOLCHANGE.get();
                if(p.contains('T'))
                {
                    cmd = cmd.replace("?", "" + (int)p.get('T').value);
                }
                cmds.addAll(Arrays.asList(cmd.split("\n")));
                break;
            case SPINDLEON:
                cmds.addAll(Arrays.asList(DatabaseV2.SPINDLEON.get().replace("?", "" + (int)p.get('M').value).split("\n")));
                break;
            case SPINDLEOFF:
                cmds.addAll(Arrays.asList(DatabaseV2.SPINDLEOFF.get().split("\n")));
                break;

            //Send directly:
            case CORDINATEMODE:
            case HOMING:
            case SETPOS:
            case GXX:
            case PAUSE:
            case MXX: 
                cmds.add(p.getCmdwithoutcomments());
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

    public String getMessage() {
        return message;
    }
    
    @Override
    public String toString() {
        return command;
    }

    public String getInfos(Transform t,boolean autoleveling) {
        String s = command + "\n"+
                "  Message: "+ message + "\n"+
                "  Type:    "+ type + "\n"+
                "  Parser:  "+ p + "\n"+
                "  Contex:  "+ cin +"\n"+
                "  Execute: ";
        for(String cmd:execute(t,autoleveling,false))
            s += "\n            --> " + cmd;
        
        return s;
    }

    public long getSecounds()
    {
        return (long)cout.seconds;
    }
    
    public static class Optimiser {
        //All Your Base Are Belong To Us
        public static class OElement
        {
            OElement n = null;
            OElement l = null;
            public int startcmd;
            public int endcmd;
            public Point2D s;
            public Point2D e;
            private boolean done = false;
            private double d = Double.MAX_VALUE;

            public OElement(int startcmd, int endcmd, Point2D s, Point2D e) {
                this.startcmd = startcmd;
                this.endcmd = endcmd;
                this.s = s;
                this.e = e;
                
            }
            
            public boolean isneighbor(OElement e)
            {
                return this == e || n == e || l == e;
            }
            
            public boolean isfarneighbor(OElement e)
            {
                return isneighbor(e) || (n!=null?n.l == e:false) || (l != null?l.l == e:false);
            }
            
            
        }
        
        public interface IProgress{
            public void publish(String message, int progess) throws MyException;
        }

        IProgress progress;

        public Optimiser(IProgress progress) {
            this.progress = progress;
        }

        
        private int reccount;
        private int doneccount;
        public ArrayList<CNCCommand> execute(ArrayList<CNCCommand> incmds) throws MyException{
            reccount    = 0;
            doneccount  = 0;
            return rec_execute(incmds);
        }
        
        private ArrayList<CNCCommand> rec_execute(ArrayList<CNCCommand> incmds) throws MyException{
            progress.publish("Checking", 0);
            reccount++;
            
            //search for Toolchange:
            ArrayList<CNCCommand> calced        = null;
            ArrayList<CNCCommand> processCmds   = incmds;
            ArrayList<CNCCommand> outCmds       = incmds;
            for(int i = 0;i < incmds.size();i++)
                if(processCmds.get(i).type == Type.TOOLCHANGE)
                {
                    processCmds =    new ArrayList<>(incmds.subList(0, i+1));
                    outCmds     =    processCmds;
                    calced      =    rec_execute(new ArrayList<>(incmds.subList(i+1,incmds.size()))); //Recrution
                    break;
                }
            
            //search last g1
            int lg1 = -1;
            for(int i = 0;i < processCmds.size();i++)
            {
                if(processCmds.get(i).type == Type.G1)
                {
                    lg1 = i;
                }
            }
                        
            if(lg1 >= 0)
            {

                boolean g1found = false;
                double fastmovelevel= Double.NaN;
                ArrayList<Integer> G0moves = new ArrayList<>();  
                for(int cmdindex = 0;cmdindex < lg1;cmdindex++)
                {
                    progress.publish("Checking", 100 * cmdindex/lg1);
                    CNCCommand cmd = processCmds.get(cmdindex);

                    g1found |= (cmd.type == Type.G1);
                    if(g1found)
                    {
                        if(Type.ALLOWEDFOROPTIMISER.contains(cmd.type) == false)
                        {
                            throw new MyException("Unsupported Commands found!",cmd);
                        }

                        if(cmd.type == Type.G0)
                        {
                            boolean[] move = new boolean[3];
                            for(int i = 0;i < 3;i++)
                            {
                                if(Double.isNaN(cmd.cin.axes[i]) || Double.isNaN(cmd.cout.axes[i]))
                                {
                                    throw new MyException("G0 command found with undefined context!",cmd);
                                }
                                if(cmd.cin.axes[i] != cmd.cout.axes[i])
                                {
                                    move[i] = true;
                                }
                            }
                            if((move[0] || move[1]) && move[2])
                            {
                                throw new MyException("Move of [XY] and Z not supporded!",cmd);
                            }             
                            if(move[0] || move[1])
                            {
                                //G0 Move to other position => this is what wie want to optimise
                                if(Double.isNaN(fastmovelevel))
                                {
                                    fastmovelevel = cmd.cin.axes[2];
                                }
                                else if(fastmovelevel!=cmd.cin.axes[2])
                                {
                                    throw new MyException("G0 move of XY not at the same level!",cmd);
                                }                     
                                G0moves.add(cmdindex);
                            }
                        }
                    }
                }

                if(G0moves.size()>1)
                {
                    //Local Optimisation thry this for maximum of time:
                    long timeout = System.currentTimeMillis() + (int)(1000 * DatabaseV2.OPTIMISATIONTIMEOUT.getsaved())/reccount; 


                    //Copy list
                    final OElement TAIL = new OElement(-1, G0moves.get(0), 
                                                     new Point2D.Double(Double.NaN, Double.NaN), 
                                                     new Point2D.Double(processCmds.get(G0moves.get(0)).cin.axes[0], processCmds.get(G0moves.get(0)).cin.axes[1]));
                    OElement temp = TAIL;
                    
                    int startcmd = G0moves.get(0);
                    G0moves.remove(0);
                    for(int i:G0moves)
                    {
                        OElement akt = new OElement(startcmd,i,
                                new Point2D.Double(processCmds.get(startcmd).cout.axes[0], processCmds.get(startcmd).cout.axes[1]),
                                new Point2D.Double(processCmds.get(i).cin.axes[0], processCmds.get(i).cin.axes[1]));
                        startcmd = i;
                        
                        //Add to linked list:
                        temp.n = akt;
                        akt.l = temp;
                        temp = akt;
                    }

                    final OElement HEAD = new OElement(G0moves.get(G0moves.size()-1), processCmds.size(),
                                                    new Point2D.Double(processCmds.get(G0moves.get(G0moves.size()-1)).cout.axes[0], processCmds.get(G0moves.get(G0moves.size()-1)).cout.axes[1]),
                                                    new Point2D.Double(Double.NaN, Double.NaN));
                    temp.n  = HEAD;
                    HEAD.l  = temp;
                    temp.d  = temp.e.distance(temp.n.s);
                    
                    //Algoritm
                    OElement hp = TAIL;
                    while(hp.n != HEAD 
                            && timeout > System.currentTimeMillis())
                    {
                        OElement a = null;
                        double d = Double.MAX_VALUE;
                        for(OElement e = hp.n;e != HEAD;e = e.n)
                        {
                            double aktd = hp.e.distance(e.s);
                            if(d > aktd)
                            {
                                d = aktd;
                                a = e;
                            }
                        }
                        
                        if(a == null)
                        {
                            throw new MyException("Should not happen!");
                        }
                        
                        //remove a;
                        a.l.n = a.n;
                        a.n.l = a.l;

                        //insert a;
                        OElement hpn = hp.n;
                        hp.n    = a;
                        a.l     = hp;
                        a.n     = hpn;
                        hpn.l   = a;
                        
                        //next
                        hp      = a;
                        
                    }
                    
                    for(OElement e = TAIL;e != HEAD;e = e.n)
                    {
                        if(e != TAIL &&(e != e.n.l || e != e.l.n))
                        {
                            throw new MyException("Internal structure error");
                        }
                        e.d = e.e.distance(e.n.s);
                    }
                    
                    //Secound Algoritim
                    while(timeout > System.currentTimeMillis())
                    {
                        progress.publish("Optimizing", (int)(doneccount * (100 / reccount) + 100 / reccount - (timeout - System.currentTimeMillis())/(10 * DatabaseV2.OPTIMISATIONTIMEOUT.getsaved())));

                        OElement as = null;
                        double   d  = 0;
                        for(OElement s = TAIL;s != HEAD && s != HEAD.l;s = s.n)
                        {
                            if(d < s.d && !s.done)
                            {
                                as  = s;
                                d   = s.d;
                            }
                        }
                        
                        if(as != null)
                        {
                            as.done = true;
                            OElement ae = null;
                            OElement bs = null;
                            OElement be = null;
                            d = 0;
                            for(ae = as.n;ae != HEAD;ae = ae.n)
                            {
                                progress.publish("Optimizing", (int)(doneccount * (100 / reccount) + 100 / reccount - (timeout - System.currentTimeMillis())/(10 * DatabaseV2.OPTIMISATIONTIMEOUT.getsaved())));

                                for(OElement s = ae.n;s != HEAD && s != HEAD.l;s = s.n)
                                {
                                    if((as.d + ae.d) <  (as.e.distance(s.n.s) + s.e.distance(as.n.s)))
                                    {
                                        for(OElement e = s.n;e != HEAD; e = e.n)
                                        {
                                            double t = ae.d + e.d + as.d + s.d - (ae.e.distance(e.n.s) + e.e.distance(ae.n.s) + as.e.distance(s.n.s) + s.e.distance(as.n.s));
                                            if(d < t)
                                            {
                                                bs  = s;
                                                be  = e;
                                                d   = t;
                                                break;
                                            }
                                            if(timeout < System.currentTimeMillis())
                                            {
                                                break;
                                            }
                                        }
                                    }    
                                    if(timeout < System.currentTimeMillis())
                                    {
                                        break;
                                    }
                                }
                                if(bs != null && be != null)
                                {
                                    break;
                                }
                                if(timeout < System.currentTimeMillis())
                                {
                                    break;
                                }
                            }
                            if(ae != HEAD && bs != null && be != null)
                            {
                                //S
                                OElement asn = as.n;
                                OElement bsn = bs.n;
                                as.n    = bsn;
                                asn.l   = bs;
                                bs.n    = asn;
                                bsn.l   = as;
 
                                //e
                                OElement aen = ae.n;
                                OElement ben = be.n;
                                ae.n    = ben;
                                aen.l   = be;
                                be.n    = aen;
                                ben.l   = ae;

                                //recalc d
                                as.d    = as.e.distance(as.n.s);
                                bs.d    = bs.e.distance(bs.n.s);
                                ae.d    = ae.e.distance(ae.n.s);
                                be.d    = be.e.distance(be.n.s);
                                
                                for(OElement e = TAIL.n;e != HEAD;e = e.n)
                                {
                                    e.done = false;
                                } 
                                continue;
                            }
                            continue;
                        }

                        break;
                    }
                    //Rebuild List
                    progress.publish("Reorder", 0);

                    outCmds = new ArrayList<>(processCmds.size());
                    for(OElement e = TAIL;e != null;e = e.n)
                        {
                        progress.publish("Reorder", 100 * outCmds.size() / processCmds.size());
                        //Make G0 move
                        if(!Double.isNaN(e.s.getX()) && !Double.isNaN(e.s.getY()))
                        {
                            outCmds.add(new CNCCommand("G0 X" + Tools.dtostr(e.s.getX()) + " Y" + Tools.dtostr(e.s.getY())));
                        }

                        //Copy elements
                        for(int i = e.startcmd + 1;i < e.endcmd;i++)
                        {
                            outCmds.add(processCmds.get(i));
                        }
                    }

                }
            }
            
            //Clone elements
            for(ListIterator<CNCCommand> i= outCmds.listIterator();i.hasNext();)
            {
                i.set(i.next().clone());
            }
            
            //Add caled if nessesarry
            if(calced != null)
            {
                outCmds.addAll(calced);
            }
            
            //Test if somthing get lost
            if(incmds.size() != outCmds.size())
            {
                throw new MyException("Internal Error: Size not equal!");
            }
            
            doneccount++;
            
            return outCmds;
        }



    }

}
