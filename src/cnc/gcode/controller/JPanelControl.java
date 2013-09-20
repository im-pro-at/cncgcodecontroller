/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.GeneralPath;
import java.text.ParseException;
import java.util.ArrayList;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 *
 * @author patrick
 */
public class JPanelControl extends javax.swing.JPanel implements IGUIEvent{

    private IEvent GUIEvent=null;

    private interface IAxesEvent
    {
        void fired(AxesManipulator axis);
    }
    
    private class AxesManipulator
    {
        private final JComponent element;
        private final IAxesEvent event;

        public AxesManipulator(JComponent element,IAxesEvent event)
        {
            this.element=element;
            this.event=event;
            
            FocusAdapter f=new FocusAdapter() {
                        @Override
                        public void focusLost(FocusEvent e) {
                                AxesManipulator.this.event.fired(AxesManipulator.this);
                        }

                        @Override
                        public void focusGained(FocusEvent e) {
                            if(e.getSource() instanceof JComboBox)
                            {
                                ((JComboBox)e.getSource()).getEditor().selectAll();
                            }
                            if(e.getSource() instanceof JTextField)
                            {
                                ((JTextField)e.getSource()).selectAll();
                            }                            
                        }
                        
                    };
            KeyListener k= new KeyAdapter() {
                    @Override
                    public void keyReleased(KeyEvent e) {
                        if(e.getKeyCode()== KeyEvent.VK_ENTER)
                                AxesManipulator.this.event.fired(AxesManipulator.this);
                    }
                };

            if(element instanceof JComboBox)
            {
                ((JComboBox)element).getEditor().getEditorComponent().addFocusListener(f);
                ((JComboBox)element).getEditor().getEditorComponent().addKeyListener(k);
            }
            else
            {
                element.addFocusListener(f);
                element.addKeyListener(k);
            }
        }
        
        public void setFocus()
        {
            element.requestFocusInWindow();
        }

        public void dispatchEvent()
        {
            event.fired(this);
        }
        
        public void set(String text)
        {
            if(element instanceof JTextField)
            {
                ((JTextField)element).setText(text);
            }
            else if(element instanceof JComboBox)
                ((JComboBox)element).setSelectedItem(text);
            else
                throw new UnsupportedOperationException("Not yet implemented");

        }
 
        public String get()
        {
            if(element instanceof JTextField)
                return ((JTextField)element).getText();
            else if(element instanceof JComboBox)
                return ((JComboBox)element).getSelectedItem().toString();
            else
                throw new UnsupportedOperationException("Not yet implemented");
        }

        public void set(Double d)
        {
            set(Tools.dtostr(d));
        }

        
        public Double getd() throws ParseException
        {
            return Tools.strtod(get());
        }

        /**
         * Return 0.0 if its not a Number!
         */
        public Double getdsave()
        {
            try {
                return Tools.strtod(get());
            } catch (ParseException ex) {
                return 0.0;
            }
        }

        private JComponent getComponent() {
            return this.element;
        }
    }
    
    private class PrintableElement {
        boolean arc,ccw;
        double diameter;
        double axes[][] = new double[4][2];

        public PrintableElement() {
            arc=JPanelControl.this.jCBarc.isSelected();
            ccw=JPanelControl.this.jCBarcCCW.isSelected();
            diameter=JPanelControl.this.axes[4][0].getdsave();
            for(int i=0;i<2;i++)
            {
                axes[i][0]=JPanelControl.this.axes[i][0].getdsave();
                axes[i][1]=JPanelControl.this.axes[i][2].getdsave();
            }
            axes[2][0]=JPanelControl.this.axes[3][0].getdsave();
            axes[2][1]=JPanelControl.this.axes[3][1].getdsave();
        }

        public void print(Graphics2D g, Point point, double scale) {
            g.setStroke(new BasicStroke((float)(diameter*scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if(arc)
            {
                //arc
                double r= Math.hypot(axes[2][0], axes[2][1]);
                double xc= axes[0][0]+axes[2][0];
                double yc= axes[1][0]+axes[2][1];
                
                //Check equi-distant
                double d1=Geometrics.getdistance(xc, yc, axes[0][0], axes[1][0]);
                double d2=Geometrics.getdistance(xc, yc, axes[0][1], axes[1][1]);
            
                if(!Geometrics.doubleequals(d1, d2, 0.001))
                {
                    //Center of ARC is not equi-distant!
                    g.setStroke(new BasicStroke(1));
                    g.setColor(Color.red);
                    g.drawLine(point.x + (int)(xc*scale), point.y +(int)(yc*scale), point.x +(int)(axes[0][0]*scale), point.y +(int)(axes[1][0]*scale));
                    g.drawLine(point.x + (int)(xc*scale), point.y +(int)(yc*scale), point.x +(int)(axes[0][1]*scale), point.y +(int)(axes[1][1]*scale));
                    g.setColor(Color.red);
                    g.fillArc(point.x + (int)(xc*scale)-4, point.y +(int)(yc*scale)-4, 8, 8, 0, 360);
                    g.setColor(Color.black);
                    g.fillArc(point.x + (int)(axes[0][0]*scale)-2, point.y +(int)(axes[1][0]*scale)-2, 4, 4, 0, 360);
                    g.fillArc(point.x + (int)(axes[0][1]*scale)-2, point.y +(int)(axes[1][1]*scale)-2, 4, 4, 0, 360);
                }
                else
                {
                    int as = (int) (180/Math.PI*Math.atan2(yc-axes[1][0], axes[0][0]-xc));
                    int ae = (int) (180/Math.PI*Math.atan2(yc-axes[1][1], axes[0][1]-xc));
//                    int oas=as;
//                    int oae=ae;
                    if(as<0)
                        as=360+as;
                    if(ae<0)
                        ae=360+ae;
/*                
                    g.drawRect(point.x + (int)((xc - r)*scale),point.y +(int)((yc - r)*scale), (int)(2*r*scale), (int)(2*r*scale));
                    g.drawRect(point.x + (int)(xc*scale),point.y +(int)(yc*scale),10,10);
                    g.drawRect(point.x + (int)(axes[0][0]*scale),point.y +(int)(axes[1][0]*scale),10,10);
                    g.drawString("s", point.x + (int)(axes[0][0]*scale),point.y +(int)(axes[1][0]*scale));
                    g.drawRect(point.x + (int)(axes[0][1]*scale),point.y +(int)(axes[1][1]*scale),10,10);
                    g.drawString("e", point.x + (int)(axes[0][1]*scale),point.y +(int)(axes[1][1]*scale));
*/                
                    int a=as-ae;
                    if(a<0)
                        a=360+a;
//                    int oa=a;

                    //Calc mirroring + ccw
                    switch(Integer.parseInt(Database.get(Database.HOMEING)))
                    {
                        case 0:
                        case 3:
                            if(ccw)
                                a=a-360;
                            break;
                        default:
                            if(!ccw)
                                a=a-360;
                            break;
                    }

//                  MainForm.this.setTitle("a="+a+" as="+as+" ae="+ae+" oa="+oa+" oas="+oas+" oae="+oae);
                
                    g.drawArc(point.x + (int)((xc - r)*scale),point.y +(int)((yc - r)*scale), (int)(2*r*scale), (int)(2*r*scale), as,0-a);
                }
            }
            else
            {
                //Line
                GeneralPath line= new GeneralPath();
                line.moveTo(point.x+axes[0][0]*scale, point.y+axes[1][0]*scale);
                line.lineTo(point.x+axes[0][1]*scale, point.y+axes[1][1]*scale);
                g.draw(line);
            }
        }
        
        
    }
    
    AxesManipulator[][] axes;
    ArrayList<PrintableElement> printList= new ArrayList<>();

    boolean pharsnextserial=false;
    
    
    /**
     * Creates new form JPanelControl
     */
    public JPanelControl() {
        initComponents();

        //Init Fileds:
        IAxesEvent event= new IAxesEvent() {
            @Override
            public void fired(AxesManipulator axis) {
                axesEvent(axis);
            }
        };

        axes= new AxesManipulator[][]   {
                                        /*0*/    {new AxesManipulator(jTFXa,event),new AxesManipulator(jTFXd,event),new AxesManipulator(jTFXn,event)},
                                        /*1*/    {new AxesManipulator(jTFYa,event),new AxesManipulator(jTFYd,event),new AxesManipulator(jTFYn,event)},
                                        /*2*/    {new AxesManipulator(jTFZa,event),new AxesManipulator(jTFZd,event),new AxesManipulator(jCBZn,event)},
                                        /*3*/    {new AxesManipulator(jCBarcI,event),new AxesManipulator(jCBarcJ,event)}, // I,J
                                        /*4*/    {new AxesManipulator(jCBdiameter,event)}, //Diameter
                                        /*5*/    {new AxesManipulator(jCBfeedrate,event)} //Feedrate
                                        };
        for(AxesManipulator[] axe:axes)
            for(AxesManipulator field:axe)
                field.set(0.0);
        
        axes[5][0].set(Tools.strtodsave(Database.MAXFEEDRATE.get())/10);

        
       Communication.getInstance().addResiveEvent(new Communication.IResivedLines() {
            @Override
            public void resived(String[] lines) {
                for(String line: lines)
                {
                    if(pharsnextserial)
                    {
                        pharsnextserial=false;
                        String in=line;
                        Double[] values= new Double[3];
                        for(int j=0;j<3;j++)
                        {
                            int pos =in.indexOf(""+CommandParsing.axesName[j]+":");
                            if(pos==-1)
                            {
                                values=null;
                                break;
                            }
                            try {
                                String temp=in.substring(pos+2);
                                values[j]= Tools.strtod(temp);
                            } catch (ParseException ex) {
                                values=null;
                                break;
                            }
                        }
                        if(values!= null)
                        {
                            for(int j=0;j<3;j++)
                            {
                                axes[j][0].set(values[j]);  
                                axes[j][0].dispatchEvent();
                            }
                            JOptionPane.showMessageDialog(JPanelControl.this, "Update done!");
                        }
                        else
                        {
                            JOptionPane.showMessageDialog(JPanelControl.this, "Error reading Position");
                       }
                    }
                }
            }
        });
        
        jPPaint.addPaintEventListener(new JPPaintableListener() {
            @Override
            public void paintComponent(JPPaintableEvent evt) {
                paintAxesAria(evt.g);
            }
        });
        
         
        
    }

    
    @Override
    public void setGUIEvent(IEvent event) {
        GUIEvent=event;
    }

    @Override
    public void updateGUI(boolean serial, boolean isworking) {
        jBHoming.setEnabled(!isworking && serial);
        jBPowerON.setEnabled(!isworking && serial);
        jBPowerOFF.setEnabled(!isworking && serial);
        jBSetPos.setEnabled(!isworking && serial);
        jBGetPos.setEnabled(!isworking && serial);
        jBMove.setEnabled(!isworking && serial);
    }
    
    private void fireupdateGUI()
    {
        if(GUIEvent==null)
            throw new RuntimeException("GUI EVENT NOT USED!");
        GUIEvent.fired();
    }
   
    
    private void axesEvent(AxesManipulator axis) {                               
        //Find Position
        int cat=-1,num=-1;
        for(int i=0;i<axes.length;i++)
            for(int j=0;j<axes[i].length;j++)
                if(axes[i][j]==axis)
                {
                    cat=i;
                    num=j;
                }

        if(cat==-1)
            throw new UnsupportedOperationException("Element not in Axes array!");
        
        Double value;
        try {
           value=axes[cat][num].getd();
        } catch (ParseException ex) {
            Tools.popUpToolTip(axes[cat][num].getComponent(), ex.toString());
            axes[cat][num].setFocus();
            return;
        }
        
        //Write back Value
        axes[cat][num].set(value);
        
        //Test Range
        Double min= Double.MIN_VALUE;
        Double max= Double.MAX_VALUE;
        switch(cat)
        {
            case 0: //X
            case 1: //Y
            case 2: //Z
                if(num!=2)
                {
                    //Manipulating a,d
                    num=1; //only d is manipulatable!
                    min=0.0-axes[cat][0].getdsave();
                    max=Tools.strtodsave(Database.values()[Database.WORKSPACE0.ordinal()+cat].get())-axes[cat][0].getdsave();
                }
                else
                {
                    //Manipulating n
                    min=0.0;
                    max=Tools.strtodsave(Database.values()[Database.WORKSPACE0.ordinal()+cat].get());
                }
                break;
            case 3: //I,J
                max=Tools.strtodsave(Database.values()[Database.WORKSPACE0.ordinal()+num].get());
                min=0.0-max;
                break;
            case 4: //Diameter
                min=0.0;
                break;
            case 5: //Feedrate
                min=0.0;
                max=Tools.strtodsave(Database.MAXFEEDRATE.get());
                break;
        }
        if(axes[cat][num].getdsave()<min)
        {
            Tools.popUpToolTip(axes[cat][num].getComponent(),"Value have to be gater then "+Tools.dtostr(min));
            axes[cat][num].set(min);
            axes[cat][num].setFocus();
        }
        if(axes[cat][num].getdsave()>max)
        {
            Tools.popUpToolTip(axes[cat][num].getComponent(),"Value have to be smaler then "+Tools.dtostr(max));
            axes[cat][num].set(max);
            axes[cat][num].setFocus();
        }

        //Calc other Fields
        if(cat<=2)
            switch(num)
            {
                case 0: //a
                case 1: //d
                    axes[cat][2].set(axes[cat][0].getdsave()+axes[cat][1].getdsave());
                    break;
                case 2: //n
                    axes[cat][1].set(axes[cat][2].getdsave()-axes[cat][0].getdsave());
                    break;
            }

        //Reprint
        jPPaint.repaint();
        
    }                              

    
       private void paintAxesAria(Graphics g) {
        //Calc window
        double ariawidth= Tools.strtodsave(Database.WORKSPACE0.get()); //x
        double ariaheight= Tools.strtodsave(Database.WORKSPACE1.get()); //y
        Rectangle rect=Geometrics.placeRectangle(jPPaint.getWidth(), jPPaint.getHeight(), Geometrics.getRatio(ariawidth,ariaheight));
        
        if(g instanceof Graphics2D == false)
            throw new UnsupportedOperationException("Graphics is not 2D!");
        Graphics2D g2d=(Graphics2D)g;

        //Scale for homeing position
        g2d.translate(jPPaint.getWidth()/2, jPPaint.getHeight()/2);
        switch(Integer.parseInt(Database.HOMEING.get()))
        {
            case 0:
            default:
                g2d.scale(1,1);                
                break;
            case 1:
                g2d.scale(-1,1);
                break;
            case 2:
                g2d.scale(1,-1);
                break;
            case 3:                
                g2d.scale(-1,-1);
                break;
        }
        g2d.translate(-jPPaint.getWidth()/2, -jPPaint.getHeight()/2);
        
        g2d.setColor(Color.white);
        g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
        
        g2d.setColor(Color.black);
        for(PrintableElement p:printList)
            p.print(g2d, new Point(rect.x, rect.y), Geometrics.getScale(rect.width, rect.height, ariawidth, ariaheight));
        
        g2d.setColor(Color.red);
        new PrintableElement().print(g2d, new Point(rect.x, rect.y), Geometrics.getScale(rect.width, rect.height, ariawidth, ariaheight));
        
    }

    private void loadformsavelist(int index)
    {
        if(((SortedComboBoxModel<PositionListElement>)jLSave.getModel()).getElementAt(index)!=null)
        {
            jPPaint.setRepaintEnable(false);
            axes[0][2].set(((SortedComboBoxModel<PositionListElement>)jLSave.getModel()).getElementAt(index).getX());
            axes[0][2].dispatchEvent();
            axes[1][2].set(((SortedComboBoxModel<PositionListElement>)jLSave.getModel()).getElementAt(index).getY());
            axes[1][2].dispatchEvent();
            jPPaint.setRepaintEnable(true);
        }
    }
       
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        jBHoming = new javax.swing.JButton();
        jBPowerON = new javax.swing.JButton();
        jBPowerOFF = new javax.swing.JButton();
        jBSetPos = new javax.swing.JButton();
        jBGetPos = new javax.swing.JButton();
        jPXYname = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jPZname = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jPXYakt = new javax.swing.JPanel();
        jTFXa = new javax.swing.JTextField();
        jTFYa = new javax.swing.JTextField();
        jPZakt = new javax.swing.JPanel();
        jTFZa = new javax.swing.JTextField();
        jPXYdelta = new javax.swing.JPanel();
        jTFXd = new javax.swing.JTextField();
        jTFYd = new javax.swing.JTextField();
        jPZdelta = new javax.swing.JPanel();
        jTFZd = new javax.swing.JTextField();
        jPXYnew = new javax.swing.JPanel();
        jTFXn = new javax.swing.JTextField();
        jTFYn = new javax.swing.JTextField();
        jPZnew = new javax.swing.JPanel();
        jCBZn = new javax.swing.JComboBox();
        jScrollPane4 = new javax.swing.JScrollPane();
        jLSave = new javax.swing.JList();
        jBPosSave = new javax.swing.JButton();
        jBPosLoad = new javax.swing.JButton();
        jBPosRem = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jPdiameter = new javax.swing.JPanel();
        jCBdiameter = new javax.swing.JComboBox();
        jPXdiameter = new javax.swing.JPanel();
        jBTXfp = new javax.swing.JButton();
        jBTXhp = new javax.swing.JButton();
        jBTXfm = new javax.swing.JButton();
        jBTXhm = new javax.swing.JButton();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jPYdiameter = new javax.swing.JPanel();
        jBTYfp = new javax.swing.JButton();
        jBTYhp = new javax.swing.JButton();
        jBTYfm = new javax.swing.JButton();
        jBTYhm = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jPoptions = new javax.swing.JPanel();
        jCBarc = new javax.swing.JCheckBox();
        jLabel25 = new javax.swing.JLabel();
        jCBarcCCW = new javax.swing.JCheckBox();
        jLabel26 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jCBFastMode = new javax.swing.JCheckBox();
        jCBarcI = new javax.swing.JComboBox();
        jCBarcJ = new javax.swing.JComboBox();
        jCBfeedrate = new javax.swing.JComboBox();
        jBMove = new javax.swing.JButton();
        jPPaint = new cnc.gcode.controller.JPPaintable();

        jSplitPane1.setDividerLocation(300);

        jBHoming.setText("Homeing");
        jBHoming.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBHomingActionPerformed(evt);
            }
        });

        jBPowerON.setText("Power ON");
        jBPowerON.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBPowerONActionPerformed(evt);
            }
        });

        jBPowerOFF.setText("Power OFF");
        jBPowerOFF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBPowerOFFActionPerformed(evt);
            }
        });

        jBSetPos.setText("Set Position");
        jBSetPos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSetPosActionPerformed(evt);
            }
        });

        jBGetPos.setText("Get Position");
        jBGetPos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBGetPosActionPerformed(evt);
            }
        });

        jPXYname.setToolTipText("");
        jPXYname.setName(""); // NOI18N

        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel8.setText("X");

        jLabel9.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel9.setText("Y");

        javax.swing.GroupLayout jPXYnameLayout = new javax.swing.GroupLayout(jPXYname);
        jPXYname.setLayout(jPXYnameLayout);
        jPXYnameLayout.setHorizontalGroup(
            jPXYnameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel8)
            .addComponent(jLabel9)
        );
        jPXYnameLayout.setVerticalGroup(
            jPXYnameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPXYnameLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel9)
                .addGap(28, 28, 28))
        );

        jPZname.setToolTipText("");
        jPZname.setName(""); // NOI18N

        jLabel12.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel12.setText("Z");

        javax.swing.GroupLayout jPZnameLayout = new javax.swing.GroupLayout(jPZname);
        jPZname.setLayout(jPZnameLayout);
        jPZnameLayout.setHorizontalGroup(
            jPZnameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPZnameLayout.createSequentialGroup()
                .addComponent(jLabel12)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPZnameLayout.setVerticalGroup(
            jPZnameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPZnameLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jLabel12)
                .addGap(56, 56, 56))
        );

        jPXYakt.setBorder(javax.swing.BorderFactory.createTitledBorder("Akt Position"));

        jTFXa.setEditable(false);

        jTFYa.setEditable(false);

        javax.swing.GroupLayout jPXYaktLayout = new javax.swing.GroupLayout(jPXYakt);
        jPXYakt.setLayout(jPXYaktLayout);
        jPXYaktLayout.setHorizontalGroup(
            jPXYaktLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTFXa)
            .addComponent(jTFYa)
        );
        jPXYaktLayout.setVerticalGroup(
            jPXYaktLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPXYaktLayout.createSequentialGroup()
                .addComponent(jTFXa, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTFYa, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        jPZakt.setBorder(javax.swing.BorderFactory.createTitledBorder("Akt Position"));
        jPZakt.setToolTipText("");
        jPZakt.setName(""); // NOI18N

        jTFZa.setEnabled(false);

        javax.swing.GroupLayout jPZaktLayout = new javax.swing.GroupLayout(jPZakt);
        jPZakt.setLayout(jPZaktLayout);
        jPZaktLayout.setHorizontalGroup(
            jPZaktLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTFZa, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jPZaktLayout.setVerticalGroup(
            jPZaktLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTFZa, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        jPXYdelta.setBorder(javax.swing.BorderFactory.createTitledBorder("Delta"));
        jPXYdelta.setToolTipText("");
        jPXYdelta.setName(""); // NOI18N

        javax.swing.GroupLayout jPXYdeltaLayout = new javax.swing.GroupLayout(jPXYdelta);
        jPXYdelta.setLayout(jPXYdeltaLayout);
        jPXYdeltaLayout.setHorizontalGroup(
            jPXYdeltaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTFXd, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jTFYd, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jPXYdeltaLayout.setVerticalGroup(
            jPXYdeltaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPXYdeltaLayout.createSequentialGroup()
                .addComponent(jTFXd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTFYd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPZdelta.setBorder(javax.swing.BorderFactory.createTitledBorder("Delta"));
        jPZdelta.setToolTipText("");
        jPZdelta.setName(""); // NOI18N

        javax.swing.GroupLayout jPZdeltaLayout = new javax.swing.GroupLayout(jPZdelta);
        jPZdelta.setLayout(jPZdeltaLayout);
        jPZdeltaLayout.setHorizontalGroup(
            jPZdeltaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTFZd, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jPZdeltaLayout.setVerticalGroup(
            jPZdeltaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTFZd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        jPXYnew.setBorder(javax.swing.BorderFactory.createTitledBorder("New Position"));
        jPXYnew.setToolTipText("");
        jPXYnew.setName(""); // NOI18N

        javax.swing.GroupLayout jPXYnewLayout = new javax.swing.GroupLayout(jPXYnew);
        jPXYnew.setLayout(jPXYnewLayout);
        jPXYnewLayout.setHorizontalGroup(
            jPXYnewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTFXn, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jTFYn, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jPXYnewLayout.setVerticalGroup(
            jPXYnewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPXYnewLayout.createSequentialGroup()
                .addComponent(jTFXn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTFYn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPZnew.setBorder(javax.swing.BorderFactory.createTitledBorder("New Position"));

        jCBZn.setEditable(true);
        jCBZn.setModel(new DefaultComboBoxModel<String>(new String[0]));
        jCBZn.setMinimumSize(new java.awt.Dimension(6, 20));
        jCBZn.setPreferredSize(new java.awt.Dimension(6, 20));

        javax.swing.GroupLayout jPZnewLayout = new javax.swing.GroupLayout(jPZnew);
        jPZnew.setLayout(jPZnewLayout);
        jPZnewLayout.setHorizontalGroup(
            jPZnewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jCBZn, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPZnewLayout.setVerticalGroup(
            jPZnewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPZnewLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jCBZn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jLSave.setModel(new SortedComboBoxModel(new PositionListElement[0]));
        jLSave.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jLSave.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLSaveMouseClicked(evt);
            }
        });
        jLSave.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jLSaveKeyPressed(evt);
            }
        });
        jScrollPane4.setViewportView(jLSave);

        jBPosSave.setText("Save Akt Position");
        jBPosSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBPosSaveActionPerformed(evt);
            }
        });

        jBPosLoad.setText("Load Position");
        jBPosLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBPosLoadActionPerformed(evt);
            }
        });

        jBPosRem.setText("Remove Position");
        jBPosRem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBPosRemActionPerformed(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel7.setText("Save:");

        jLabel11.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel11.setText("Z:");

        jLabel13.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel13.setText("X,Y:");

        jLabel14.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel14.setText("Tool:");

        jPdiameter.setBorder(javax.swing.BorderFactory.createTitledBorder("Diameter"));
        jPdiameter.setToolTipText("");
        jPdiameter.setName(""); // NOI18N

        jCBdiameter.setEditable(true);

        javax.swing.GroupLayout jPdiameterLayout = new javax.swing.GroupLayout(jPdiameter);
        jPdiameter.setLayout(jPdiameterLayout);
        jPdiameterLayout.setHorizontalGroup(
            jPdiameterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jCBdiameter, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPdiameterLayout.setVerticalGroup(
            jPdiameterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPdiameterLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jCBdiameter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPXdiameter.setBorder(javax.swing.BorderFactory.createTitledBorder("X"));
        jPXdiameter.setToolTipText("");
        jPXdiameter.setName(""); // NOI18N

        jBTXfp.setText("+");
        jBTXfp.setPreferredSize(new java.awt.Dimension(6, 23));
        jBTXfp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBTool(evt);
            }
        });

        jBTXhp.setText("+");
        jBTXhp.setPreferredSize(new java.awt.Dimension(6, 23));
        jBTXhp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBTool(evt);
            }
        });

        jBTXfm.setText("-");
        jBTXfm.setPreferredSize(new java.awt.Dimension(6, 23));
        jBTXfm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBTool(evt);
            }
        });

        jBTXhm.setText("-");
        jBTXhm.setPreferredSize(new java.awt.Dimension(6, 23));
        jBTXhm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBTool(evt);
            }
        });

        jLabel16.setText("1");

        jLabel17.setText("1/2");

        javax.swing.GroupLayout jPXdiameterLayout = new javax.swing.GroupLayout(jPXdiameter);
        jPXdiameter.setLayout(jPXdiameterLayout);
        jPXdiameterLayout.setHorizontalGroup(
            jPXdiameterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPXdiameterLayout.createSequentialGroup()
                .addGroup(jPXdiameterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPXdiameterLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jLabel16)
                        .addGap(6, 6, 6)
                        .addComponent(jBTXfp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPXdiameterLayout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(jLabel17)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBTXhp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPXdiameterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jBTXfm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jBTXhm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPXdiameterLayout.setVerticalGroup(
            jPXdiameterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPXdiameterLayout.createSequentialGroup()
                .addGroup(jPXdiameterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBTXfp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBTXfm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                .addGroup(jPXdiameterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBTXhp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBTXhm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17)))
        );

        jPYdiameter.setBorder(javax.swing.BorderFactory.createTitledBorder("Y"));
        jPYdiameter.setToolTipText("");
        jPYdiameter.setName(""); // NOI18N

        jBTYfp.setText("+");
        jBTYfp.setPreferredSize(new java.awt.Dimension(6, 23));
        jBTYfp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBTool(evt);
            }
        });

        jBTYhp.setText("+");
        jBTYhp.setPreferredSize(new java.awt.Dimension(6, 23));
        jBTYhp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBTool(evt);
            }
        });

        jBTYfm.setText("-");
        jBTYfm.setPreferredSize(new java.awt.Dimension(6, 23));
        jBTYfm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBTool(evt);
            }
        });

        jBTYhm.setText("-");
        jBTYhm.setPreferredSize(new java.awt.Dimension(6, 23));
        jBTYhm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBTool(evt);
            }
        });

        jLabel10.setText("1");

        jLabel15.setText("1/2");

        javax.swing.GroupLayout jPYdiameterLayout = new javax.swing.GroupLayout(jPYdiameter);
        jPYdiameter.setLayout(jPYdiameterLayout);
        jPYdiameterLayout.setHorizontalGroup(
            jPYdiameterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPYdiameterLayout.createSequentialGroup()
                .addGroup(jPYdiameterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPYdiameterLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jLabel10)
                        .addGap(6, 6, 6)
                        .addComponent(jBTYfp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPYdiameterLayout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(jLabel15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBTYhp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPYdiameterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jBTYfm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jBTYhm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPYdiameterLayout.setVerticalGroup(
            jPYdiameterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPYdiameterLayout.createSequentialGroup()
                .addGroup(jPYdiameterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBTYfp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBTYfm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPYdiameterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBTYhp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBTYhm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15)))
        );

        jLabel18.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel18.setText("Global:");

        jLabel21.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel21.setText("Move:");

        jPoptions.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jCBarc.setText("ARC");
        jCBarc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBarcActionPerformed(evt);
            }
        });

        jLabel25.setText("I:");

        jCBarcCCW.setText("ccw");
        jCBarcCCW.setEnabled(false);
        jCBarcCCW.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBarcCCWActionPerformed(evt);
            }
        });

        jLabel26.setText("J:");

        jLabel22.setText("Feedrate:");

        jCBFastMode.setText("Fast Moves");
        jCBFastMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBFastModeActionPerformed(evt);
            }
        });

        jCBarcI.setEditable(true);
        jCBarcI.setModel(new DefaultComboBoxModel<String>(new String[0]));
        jCBarcI.setEnabled(false);

        jCBarcJ.setEditable(true);
        jCBarcJ.setModel(new DefaultComboBoxModel<String>(new String[0]));
        jCBarcJ.setEnabled(false);

        jCBfeedrate.setEditable(true);
        jCBfeedrate.setModel(new DefaultComboBoxModel<String>(new String[0]));

        javax.swing.GroupLayout jPoptionsLayout = new javax.swing.GroupLayout(jPoptions);
        jPoptions.setLayout(jPoptionsLayout);
        jPoptionsLayout.setHorizontalGroup(
            jPoptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPoptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPoptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPoptionsLayout.createSequentialGroup()
                        .addComponent(jCBarc)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel25)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCBarcI, 0, 1, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel26)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCBarcJ, 0, 1, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jCBarcCCW))
                    .addGroup(jPoptionsLayout.createSequentialGroup()
                        .addComponent(jLabel22)
                        .addGap(18, 18, 18)
                        .addComponent(jCBfeedrate, 0, 1, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jCBFastMode))))
        );
        jPoptionsLayout.setVerticalGroup(
            jPoptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPoptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPoptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCBarc)
                    .addComponent(jLabel25)
                    .addComponent(jLabel26)
                    .addComponent(jCBarcCCW)
                    .addComponent(jCBarcI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCBarcJ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPoptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(jCBFastMode)
                    .addComponent(jCBfeedrate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jBMove.setFont(new java.awt.Font("Tahoma", 1, 48)); // NOI18N
        jBMove.setText("Move");
        jBMove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBMoveActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel14)
                    .addComponent(jLabel13)
                    .addComponent(jLabel11)
                    .addComponent(jLabel18)
                    .addComponent(jLabel7)
                    .addComponent(jLabel21))
                .addGap(20, 20, 20)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPZname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPZakt, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPZdelta, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPZnew, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jPoptions, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jBMove, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jBPosRem, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jBPosLoad, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jBPosSave, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jBHoming, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBPowerON, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBPowerOFF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBSetPos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBGetPos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPXYname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPXYakt, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPXYdelta, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPXYnew, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPdiameter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPXdiameter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPYdiameter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jBSetPos)
                        .addComponent(jBGetPos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jBHoming)
                        .addComponent(jBPowerON)
                        .addComponent(jBPowerOFF))
                    .addComponent(jLabel18))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPXYnew, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPXYdelta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jPXYakt, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel13)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addComponent(jPXYname, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jBPosSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBPosLoad)
                        .addGap(7, 7, 7)
                        .addComponent(jBPosRem))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jPZdelta, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPZname, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jPZakt, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jPZnew, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jPYdiameter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPdiameter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPXdiameter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jLabel14))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jBMove, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPoptions, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jLabel21))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane2.setViewportView(jPanel1);

        jSplitPane1.setRightComponent(jScrollPane2);

        jPPaint.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jPPaintMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout jPPaintLayout = new javax.swing.GroupLayout(jPPaint);
        jPPaint.setLayout(jPPaintLayout);
        jPPaintLayout.setHorizontalGroup(
            jPPaintLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 299, Short.MAX_VALUE)
        );
        jPPaintLayout.setVerticalGroup(
            jPPaintLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 451, Short.MAX_VALUE)
        );

        jSplitPane1.setLeftComponent(jPPaint);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 944, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 944, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 551, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 551, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jBHomingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBHomingActionPerformed
        if(Communication.getInstance().isbussy())
        {
            JOptionPane.showMessageDialog(this, "An other command is in Progress!");
            return;
        }
        Communication.getInstance().send("G28 X Y Z");
        jPPaint.setRepaintEnable(false);
        for(int i=0;i<3;i++)
        {
            axes[i][0].set(0.0);
            axes[i][0].dispatchEvent();
        }
        jPPaint.setRepaintEnable(true);
    }//GEN-LAST:event_jBHomingActionPerformed

    private void jBPowerONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBPowerONActionPerformed
        if(Communication.getInstance().isbussy())
        {
            JOptionPane.showMessageDialog(this, "An other command is in Progress!");
            return;
        }
        Communication.getInstance().send("M80");
    }//GEN-LAST:event_jBPowerONActionPerformed

    private void jBPowerOFFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBPowerOFFActionPerformed
        if(Communication.getInstance().isbussy())
        {
            JOptionPane.showMessageDialog(this, "An other command is in Progress!");
            return;
        }
        Communication.getInstance().send("M81");
    }//GEN-LAST:event_jBPowerOFFActionPerformed

    private void jBSetPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBSetPosActionPerformed
        if(Communication.getInstance().isbussy())
        {
            JOptionPane.showMessageDialog(this, "An other command is in Progress!");
            return;
        }
        Double[] values=new Double[3];
        Double[] max=new Double[3];
        String[] messages= new String[3];
        for(int i=0;i<3;i++)
        {
            messages[i]="Set the Value for the "+CommandParsing.axesName[i]+" Axis";
            values[i]= axes[i][0].getdsave();
            max[i]= Tools.strtodsave(Database.values()[Database.WORKSPACE0.ordinal()+i].get());
        }

        values = Tools.getValues(messages, values, max, new Double[]{0.0,0.0,0.0});

        if(values!= null)
        {
            String cmd= "G92";
            jPPaint.setRepaintEnable(false);
            for(int i=0;i<3;i++)
            {
                axes[i][0].set(values[i]);
                axes[i][0].dispatchEvent();
                cmd+=" "+CommandParsing.axesName[i]+Tools.dtostr(values[i]);
            }
            Communication.getInstance().send(cmd);
            jPPaint.setRepaintEnable(true);
        }

    }//GEN-LAST:event_jBSetPosActionPerformed

    private void jBGetPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBGetPosActionPerformed
        if(Communication.getInstance().isbussy())
        {
            JOptionPane.showMessageDialog(this, "An other command is in Progress!");
            return;
        }
        pharsnextserial=true;
        Communication.getInstance().send("M114");
    }//GEN-LAST:event_jBGetPosActionPerformed

    private void jLSaveMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLSaveMouseClicked
        if(evt.getClickCount()==2)
        {
            loadformsavelist(jLSave.locationToIndex(evt.getPoint()));
        }
    }//GEN-LAST:event_jLSaveMouseClicked

    private void jLSaveKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jLSaveKeyPressed
        if(evt.getKeyCode()==KeyEvent.VK_ENTER)
        {
            loadformsavelist(jLSave.getSelectedIndex());
        }
        if(evt.getKeyCode()==KeyEvent.VK_DELETE)
        {
            if(jLSave.getSelectedIndex()!=-1)
            {
                ((SortedComboBoxModel<PositionListElement>)jLSave.getModel()).removeElementAt(jLSave.getSelectedIndex());
            }
        }

    }//GEN-LAST:event_jLSaveKeyPressed

    private void jBPosSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBPosSaveActionPerformed
        String name = JOptionPane.showInputDialog("Name for the Position");
        if(name==null)
        return;

        PositionListElement element= new PositionListElement(name, axes[0][0].getdsave(), axes[1][0].getdsave());

        ((SortedComboBoxModel<PositionListElement>)jLSave.getModel()).addElement(element);

    }//GEN-LAST:event_jBPosSaveActionPerformed

    private void jBPosLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBPosLoadActionPerformed
        loadformsavelist(jLSave.getSelectedIndex());
    }//GEN-LAST:event_jBPosLoadActionPerformed

    private void jBPosRemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBPosRemActionPerformed
        if(jLSave.getSelectedIndex()!=-1)
        ((SortedComboBoxModel<PositionListElement>)jLSave.getModel()).removeElementAt(jLSave.getSelectedIndex());
    }//GEN-LAST:event_jBPosRemActionPerformed

    private void jBTool(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBTool
        JButton[][][] buttons= new JButton[][][]
        {{ //X
            {jBTXfp, jBTXfm}, //1
            {jBTXhp, jBTXhm}  //1/2
        },{ //Y
            {jBTYfp, jBTYfm}, //1
            {jBTYhp, jBTYhm}}};  //1/2
    int axis=-1,type=-1,sign=-1;

    for(int i=0;i<buttons.length;i++)
    for(int j=0;j<buttons[i].length;j++)
    for(int k=0;k<buttons[i][j].length;k++)
    if(buttons[i][j][k]==evt.getSource())
    {
        axis=i;
        type=j;
        sign=k;
        }

        Double d=axes[4][0].getdsave();

        if(type==1)
        d=d/2;

        if(sign==1)
        d=0.0-d;

        axes[axis][1].set(axes[axis][1].getdsave()+d);
        axes[axis][1].dispatchEvent();

        //Save Diameter
        if(((DefaultComboBoxModel<String>)jCBdiameter.getModel()).getIndexOf(axes[4][0].get())==-1)
        jCBdiameter.addItem(axes[4][0].get());

    }//GEN-LAST:event_jBTool

    private void jCBarcActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBarcActionPerformed
        if(jCBarc.isSelected())
        {
            jCBarcI.setEnabled(true);
            jCBarcJ.setEnabled(true);
            jCBarcCCW.setEnabled(true);
        }
        else
        {
            jCBarcI.setEnabled(false);
            jCBarcJ.setEnabled(false);
            jCBarcCCW.setEnabled(false);
        }
        jPPaint.repaint();
    }//GEN-LAST:event_jCBarcActionPerformed

    private void jCBarcCCWActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBarcCCWActionPerformed
        jPPaint.repaint();
    }//GEN-LAST:event_jCBarcCCWActionPerformed

    private void jCBFastModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBFastModeActionPerformed
        if(jCBFastMode.isSelected())
        {
            jCBfeedrate.setEnabled(false);
        }
        else
        {
            jCBfeedrate.setEnabled(true);
        }
    }//GEN-LAST:event_jCBFastModeActionPerformed

    private void jBMoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBMoveActionPerformed
        if(Communication.getInstance().isbussy())
        {
            JOptionPane.showMessageDialog(this, "An other command is in Progress!");
            return;
        }

        jPPaint.setRepaintEnable(false);

        //Test Variables:
        for(AxesManipulator[] aa:axes)
        for(AxesManipulator a:aa)
        {
            String temp=a.get();
            a.dispatchEvent();
            if(!temp.equals(a.get()))
            {
                JOptionPane.showMessageDialog(this, "Not all Parameters in range!");
                return;
            }
        }
        //Z or XY move allowed but not both
        boolean mxy= !Geometrics.doubleequals(axes[0][0].getdsave(), axes[0][2].getdsave(),0.001) || !Geometrics.doubleequals(axes[1][0].getdsave(), axes[1][2].getdsave(),0.001);
        boolean mz=!Geometrics.doubleequals(axes[2][0].getdsave(), axes[2][2].getdsave(),0.001);
        if(mxy && mz)
        {
            JOptionPane.showMessageDialog(this, "Only XY move or Z move allowed .. but not both!");
            return;
        }

        if(jCBarc.isSelected())
        {

            //Check equi-distant
            double cx=axes[0][0].getdsave()+axes[3][0].getdsave();
            double cy=axes[1][0].getdsave()+axes[3][1].getdsave();
            double d1=Geometrics.getdistance(cx, cy, axes[0][0].getdsave(), axes[1][0].getdsave());
            double d2=Geometrics.getdistance(cx, cy, axes[0][2].getdsave(), axes[1][2].getdsave());

            if(!Geometrics.doubleequals(d1, d2, 0.001))
            {
                JOptionPane.showMessageDialog(this, "Center of ARC is not equi-distant!");
                return;
            }

        }

        //Build Command
        String cmd;
        if(jCBarc.isSelected())
        {
            if(jCBarcCCW.isSelected())
            cmd="G3"; //Counterclockwise
            else
            cmd="G2"; //Clockwise
        }
        else
        {
            cmd="G1";  //Linear Move
        }

        //Load Axes
        for(int i=0;i<3;i++)
        cmd+=" "+CommandParsing.axesName[i]+Tools.dtostr(axes[i][2].getdsave());

        if(jCBarc.isSelected())
        {
            cmd+=" I"+Tools.dtostr(axes[3][0].getdsave());
            cmd+=" J"+Tools.dtostr(axes[3][1].getdsave());
        }

        //Get feedrate
        Double feedrate;
        if(jCBFastMode.isSelected())
        feedrate=Tools.strtodsave(Database.MAXFEEDRATE.get());
        else
        feedrate=axes[5][0].getdsave();
        cmd+=" F"+Tools.dtostr(feedrate);

        //Execute
        Communication.getInstance().send(cmd);

        //Save Move
        printList.add(new PrintableElement());

        //Update Values
        for(int i=0;i<3;i++)
        {
            axes[i][0].set(axes[i][2].getdsave());
            axes[i][0].dispatchEvent();
        }

        //Save Values
        if(((DefaultComboBoxModel<String>)jCBZn.getModel()).getIndexOf(axes[2][0].get())==-1)
        jCBZn.addItem(axes[2][0].get());
        if(((DefaultComboBoxModel<String>)jCBarcI.getModel()).getIndexOf(axes[3][0].get())==-1)
        jCBarcI.addItem(axes[3][0].get());
        if(((DefaultComboBoxModel<String>)jCBarcJ.getModel()).getIndexOf(axes[3][1].get())==-1)
        jCBarcJ.addItem(axes[3][1].get());
        if(((DefaultComboBoxModel<String>)jCBfeedrate.getModel()).getIndexOf(axes[5][0].get())==-1)
        jCBfeedrate.addItem(axes[5][0].get());

        jPPaint.setRepaintEnable(true);

    }//GEN-LAST:event_jBMoveActionPerformed

    private void jPPaintMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPPaintMouseReleased
        double ariawidth= Tools.strtodsave(Database.WORKSPACE0.get()); //x
        double ariaheight= Tools.strtodsave(Database.WORKSPACE1.get()); //y
        Rectangle rect=Geometrics.placeRectangle(jPPaint.getWidth(), jPPaint.getHeight(), Geometrics.getRatio(ariawidth,ariaheight));

        if(Geometrics.pointInRectangle(new Point(evt.getX(),evt.getY()), rect, 0))
        {
            double x=(evt.getX()-rect.x)*Geometrics.getScale(ariawidth, ariaheight, rect.width, rect.height);
            double y=(evt.getY()-rect.y)*Geometrics.getScale(ariawidth, ariaheight, rect.width, rect.height);
            switch(Integer.parseInt(Database.HOMEING.get()))
            {
                case 1:
                x=ariawidth-x;
                break;
                case 2:
                y=ariaheight-y;
                break;
                case 3:
                x=ariawidth-x;
                y=ariaheight-y;
                break;
            }

            //Calc pos:
            jPPaint.setRepaintEnable(false);
            axes[0][2].set(x);
            axes[1][2].set(y);
            axes[0][2].dispatchEvent();
            axes[1][2].dispatchEvent();
            jPPaint.setRepaintEnable(true);
        }

    }//GEN-LAST:event_jPPaintMouseReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBGetPos;
    private javax.swing.JButton jBHoming;
    private javax.swing.JButton jBMove;
    private javax.swing.JButton jBPosLoad;
    private javax.swing.JButton jBPosRem;
    private javax.swing.JButton jBPosSave;
    private javax.swing.JButton jBPowerOFF;
    private javax.swing.JButton jBPowerON;
    private javax.swing.JButton jBSetPos;
    private javax.swing.JButton jBTXfm;
    private javax.swing.JButton jBTXfp;
    private javax.swing.JButton jBTXhm;
    private javax.swing.JButton jBTXhp;
    private javax.swing.JButton jBTYfm;
    private javax.swing.JButton jBTYfp;
    private javax.swing.JButton jBTYhm;
    private javax.swing.JButton jBTYhp;
    private javax.swing.JCheckBox jCBFastMode;
    private javax.swing.JComboBox jCBZn;
    private javax.swing.JCheckBox jCBarc;
    private javax.swing.JCheckBox jCBarcCCW;
    private javax.swing.JComboBox jCBarcI;
    private javax.swing.JComboBox jCBarcJ;
    private javax.swing.JComboBox jCBdiameter;
    private javax.swing.JComboBox jCBfeedrate;
    private javax.swing.JList jLSave;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private cnc.gcode.controller.JPPaintable jPPaint;
    private javax.swing.JPanel jPXYakt;
    private javax.swing.JPanel jPXYdelta;
    private javax.swing.JPanel jPXYname;
    private javax.swing.JPanel jPXYnew;
    private javax.swing.JPanel jPXdiameter;
    private javax.swing.JPanel jPYdiameter;
    private javax.swing.JPanel jPZakt;
    private javax.swing.JPanel jPZdelta;
    private javax.swing.JPanel jPZname;
    private javax.swing.JPanel jPZnew;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPdiameter;
    private javax.swing.JPanel jPoptions;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTextField jTFXa;
    private javax.swing.JTextField jTFXd;
    private javax.swing.JTextField jTFXn;
    private javax.swing.JTextField jTFYa;
    private javax.swing.JTextField jTFYd;
    private javax.swing.JTextField jTFYn;
    private javax.swing.JTextField jTFZa;
    private javax.swing.JTextField jTFZd;
    // End of variables declaration//GEN-END:variables
}
