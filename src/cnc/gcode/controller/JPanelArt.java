/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import de.unikassel.ann.util.ColorHelper;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 *
 * @author patrick
 */
public class JPanelArt extends javax.swing.JPanel implements IGUIEvent{

    IEvent GUIevent=null;
    private BufferedImage ipanel=null;
    private BufferedImage img=null;
    private NumberFieldManipulator[] positions;
    private double pzoom=1;
    private double mzoom=1;
    private Point2D pviewmove    =  new Point();
    private Point2D mviewmove    =  new Point();
    private Point2D viewmovelast=  new Point();
    private Point viewmovestart = new Point();
    private AffineTransform viewmovetrans = new AffineTransform();
    
    private ArrayList<CNCCommand.Move> moves = null;
    private long secounds = 0;
    private double toold=0;
    private double zmin=0;
    private double zmax=0;
        
    
    
    private abstract class PMySwingWorker<R, P> extends MySwingWorker<R, P>
    {
        @Override
        protected final void progress(int progress,String message) {
            jPBar.setValue(progress);
            jPBar.setString(message);
        }
        
    }
    
    private PMySwingWorker worker   = null;


    
        private final TriggertSwingWorker<BufferedImage> painter = new TriggertSwingWorker<BufferedImage>() {
            class GetDataSyncedHelper
            {
                private int jpw;
                private int jph;
                private double zoom;
                private Point2D viewmove;
                private double px,py,pxw,pyw,pa;
                private boolean pxm,pym;
                private int ishape;
                private CNCCommand.Move[] moves;
                private double toold;
            }
            
            @Override
            protected BufferedImage doJob() throws Exception {
                
                //Load Parameter:
                final GetDataSyncedHelper data = new GetDataSyncedHelper();
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        data.jpw        = jPPaint.getWidth();
                        data.jph        = jPPaint.getHeight();
                        data.zoom       = moves==null?pzoom:mzoom;
                        data.viewmove   = moves==null?pviewmove:mviewmove;
                        data.px         = positions[0].getdsave();
                        data.pxw        = positions[1].getdsave()-positions[0].getdsave();
                        data.py         = positions[2].getdsave();
                        data.pyw        = positions[3].getdsave()-positions[2].getdsave();
                        data.pa         = positions[4].getdsave();
                        data.pxm        = jCBmirroX.isSelected();
                        data.pym        = jCBmirroY.isSelected();
                        data.ishape     = jCBShape.getSelectedIndex();                        
                        data.moves      = moves==null?null:moves.toArray(new CNCCommand.Move[0]);
                        data.toold      = toold;
                    }
                });    
                
                Shape shape;
                switch (data.ishape){
                    case 0:
                    default:
                        shape=new Rectangle2D.Double(data.px,data.py,data.pxw,data.pyw);                
                        break;
                    case 1:
                        shape= new RoundRectangle2D.Double(data.px,data.py,data.pxw,data.pyw, Math.max(data.pxw,data.pyw)*0.1, Math.max(data.pxw,data.pyw)*0.1);
                        break;
                    case 2:
                        shape= new RoundRectangle2D.Double(data.px,data.py,data.pxw,data.pyw, Math.max(data.pxw,data.pyw)*0.2, Math.max(data.pxw,data.pyw)*0.2);
                        break;
                    case 3:
                        shape= new RoundRectangle2D.Double(data.px,data.py,data.pxw,data.pyw, Math.max(data.pxw,data.pyw)*0.3, Math.max(data.pxw,data.pyw)*0.3);
                        break;
                    case 4:
                        shape= new Ellipse2D.Double(data.px,data.py,data.pxw,data.pyw);
                        break;
                }            

                if(data.jpw < 1)
                {
                    data.jpw = 1;
                }
                if(data.jph < 1)
                {
                    data.jph = 1;
                }
                    
                BufferedImage ipanel = new BufferedImage(data.jpw, data.jph, BufferedImage.TYPE_4BYTE_ABGR);
                
                Graphics2D g2 = ipanel.createGraphics();
                
                
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                //Paint scall:
                if(data.moves!=null) {
                    data.jpw=data.jpw-100;
                    Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
                    g2.setFont(font);
                    int zh      = (int)(font.getStringBounds(Tools.dtostr(100.0), g2.getFontRenderContext()).getHeight()) + 10;                    
                    int elements= data.jph / zh;
                    int dy      = (data.jph-elements * zh) / 2;
                    for(int i = 0;i < elements && elements >= 2;i++)
                    {
                        double z = zmax - i * ((zmax-zmin) / (elements - 1));
                        double relative = (z - zmin) / (zmax-zmin);
                        relative = Tools.adjustDouble(relative, 0, 1);

                        Color c = ColorHelper.numberToColorPercentage(relative);
                        g2.setColor(c);
                        g2.fillRect(data.jpw + 5,
                                    dy + zh * i,
                                    90,
                                    zh - 4);
                        g2.setColor(((299 * c.getRed() + 587 * c.getGreen() + 114 * c.getBlue())> 128000) ? Color.black:Color.white);
                        g2.drawString(Tools.dtostr(z), data.jpw + 10, dy + zh * i + zh - 10);
                        g2.setColor(Color.black);
                        g2.drawRect(data.jpw + 5,
                                    dy + zh * i,
                                    90,
                                    zh - 4);
                    }    
                    g2.setClip(0,0,data.jpw,data.jph);
                }
                               
                //Scalling transforming ...
                //StartCorner
                g2.translate(data.jpw / 2, data.jph / 2);
                switch(DatabaseV2.EHoming.get()) 
                {
                    case UPPER_LEFT:
                    default:
                        g2.scale(1,1);                
                        break;
                    case UPPER_RIGHT:
                        g2.scale(-1,1);
                        break;
                    case LOWER_LEFT:
                        g2.scale(1,-1);
                        break;
                    case LOWER_RIGHT:                
                        g2.scale(-1,-1);
                        break;
                }
                
                if(data.moves!=null) {
                    g2.scale(data.zoom,data.zoom);
                }
                
                g2.translate(-data.jpw / 2, -data.jph / 2);
                                
                //Display Position
                double ariawidth    = DatabaseV2.WORKSPACE0.getsaved(); //x
                double ariaheight   = DatabaseV2.WORKSPACE1.getsaved(); //y
                Rectangle rect      = Geometrics.placeRectangle(data.jpw, data.jph, Geometrics.getRatio(ariawidth,ariaheight));
                double scalex       = rect.width / ariawidth;
                double scaley       = rect.height / ariaheight;
                g2.translate(rect.x, rect.y);
                g2.scale(scalex, scaley);
                
                
                //ViewMove
                if(data.moves!=null){
                    g2.translate(data.viewmove.getX(), data.viewmove.getY());                
                    try {
                        AffineTransform t = g2.getTransform();
                        t.invert();
                        viewmovetrans = t;
                    } catch (NoninvertibleTransformException ex) {
                        viewmovetrans = new AffineTransform();
                    }
                }
                
                //Draw base
                g2.setColor(new Color(Integer.parseInt(DatabaseV2.CBACKGROUND.get())));
                g2.fill(new Rectangle2D.Double(0, 0, ariawidth, ariaheight));
                
                //Draw Koardinates
                if(DatabaseV2.CGRIDDISTANCE.getsaved()>0){
                    g2.setColor(new Color(Integer.parseInt(DatabaseV2.CGRID.get())));

                    g2.setStroke(new BasicStroke((float)(1/scalex)));
                    for(int x=1;x<ariawidth/DatabaseV2.CGRIDDISTANCE.getsaved();x++){
                        Shape l = new Line2D.Double(x*DatabaseV2.CGRIDDISTANCE.getsaved(), 0, x*DatabaseV2.CGRIDDISTANCE.getsaved(), ariaheight);
                        g2.draw(l);
                    }

                    g2.setStroke(new BasicStroke((float)(1/scaley)));
                    for(int y=1;y<ariaheight/DatabaseV2.CGRIDDISTANCE.getsaved();y++){
                        Shape l = new Line2D.Double(0,(y*DatabaseV2.CGRIDDISTANCE.getsaved()),(ariawidth),(y*DatabaseV2.CGRIDDISTANCE.getsaved()));
                        g2.draw(l);
                    }
                }

                if(data.moves==null){
                    AffineTransform oldat=g2.getTransform();

                    if(img!=null)
                    {
                        g2.setClip(shape);
                        //ViewMove
                        g2.translate(data.viewmove.getX(), data.viewmove.getY());                
                        try {
                            AffineTransform t = g2.getTransform();
                            t.invert();
                            viewmovetrans = t;
                        } catch (NoninvertibleTransformException ex) {
                            viewmovetrans = new AffineTransform();
                        }

                        Rectangle r=Geometrics.placeRectangle((int)data.pxw,(int)data.pyw,Geometrics.getRatio(img.getWidth(), img.getHeight()));
                        g2.translate(r.x+data.px, r.y+data.py);

                        //Zoom
                        g2.scale((double)r.width/img.getWidth(), (double)r.height/img.getHeight());

                        g2.translate(img.getWidth()/2,img.getHeight()/2);
                        g2.rotate(Math.PI/180*data.pa);
                        g2.scale(data.zoom*(data.pxm?-1:1), data.zoom*(data.pym?-1:1));
                        g2.translate(-img.getWidth()/2,-img.getHeight()/2);

                        g2.drawImage(img, 0, 0, null);

                        g2.setClip(null);

                        g2.drawImage(img,new RescaleOp(new float[]{1f,1f,1f,0.1f},new float[4], null),0,0);


                    }
                    
                    //Draw selection
                    g2.setTransform(oldat);
                    g2.setStroke(new BasicStroke((float)(2/scaley)));
                    g2.setColor(Color.BLACK);                
                    g2.draw(shape);
                }
                else{
                    
                    for(CNCCommand.Move move:data.moves){
                        if(move.getType()==CNCCommand.Type.G1){
                            g2.setStroke(new BasicStroke((float)data.toold,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                            //make 10 segments
                            double sx=move.getStart()[0];
                            double sy=move.getStart()[1];
                            double sz=move.getStart()[2];
                            double wx=move.getEnd()[0]-sx;
                            double wy=move.getEnd()[1]-sy;
                            double wz=move.getEnd()[2]-sz;

                            if(move.getDistanceXY()>0){
                                g2.setColor(Tools.setAlpha(ColorHelper.numberToColorPercentage( Tools.adjustDouble((sz+wz/2 -zmin)/(zmax-zmin) ,0,1)),1));     
                                g2.draw(new Line2D.Double(sx, sy, sx+wx, sy+wy));
                            }
                            
                        }                    
                    }

                }

                return ipanel;
            }

            @Override
            protected void process(BufferedImage chunk) {
                ipanel = chunk;
                
                jPPaint.repaint();
            }
            
        };

    
    /**
     * Creates new form JPanelArt
     */
    public JPanelArt() {
        initComponents();
        
        NumberFieldManipulator.IChangeEvent numbereventx= (NumberFieldManipulator axis) -> {
            Double value;
            try {
                value = axis.getd();
            } catch (ParseException ex) {
                axis.popUpToolTip(ex.toString());
                axis.setFocus();
                return;
            }

            if(value<0 || value>DatabaseV2.WORKSPACE0.getsaved()){
                axis.popUpToolTip("Must be between 0 and "+DatabaseV2.WORKSPACE0.getsaved());
                axis.setFocus();
                return;
            }
            if(axis==positions[0] && value>=positions[1].getdsave()){
                positions[1].set(value+1);
            }
            if(axis==positions[1] && value<=positions[0].getdsave()){
                axis.popUpToolTip("Must be between bigger then "+positions[0].getdsave());                
                axis.setFocus();
                return;
            }
            
            //Write back Value
            axis.set(value);

            pzoom=1;
            pviewmove=new Point();
            mzoom=1;
            mviewmove=new Point();
            painter.trigger();            

        };
        NumberFieldManipulator.IChangeEvent numbereventy= (NumberFieldManipulator axis) -> {
            double value;
            try {
                value = axis.getd();
            } catch (ParseException ex) {
                axis.popUpToolTip(ex.toString());
                axis.setFocus();
                return;
            }

            if(value<0 || value>DatabaseV2.WORKSPACE1.getsaved()){
                axis.popUpToolTip("Must be between 0 and "+DatabaseV2.WORKSPACE1.getsaved());
                axis.setFocus();
                return;
            }
            if(axis==positions[2] && value>=positions[3].getdsave()){
                positions[3].set(value+1);
            }

            
            if(axis==positions[3] && value<=positions[2].getdsave()){
                axis.popUpToolTip("Must be between bigger then "+positions[2].getdsave());                
                axis.setFocus();
                return;
            }

            
            
            //Write back Value
            axis.set(value);
            
            pzoom=1;
            pviewmove=new Point();
            mzoom=1;
            mviewmove=new Point();
            painter.trigger();            
        };
        
        NumberFieldManipulator.IChangeEvent numbereventa= (NumberFieldManipulator axis) -> {
            Double value;
            try {
                value = axis.getd();
            } catch (ParseException ex) {
                axis.popUpToolTip(ex.toString());
                axis.setFocus();
                return;
            }

            if(value<-180 || value>180){
                axis.popUpToolTip("Must be between -180 and 180!");
                axis.setFocus();
                return;
            }
            
            //Write back Value
            axis.set(value);
            
            painter.trigger();            
        };
        
        positions = new NumberFieldManipulator[] 
        {
            new NumberFieldManipulator(jTFXStart, numbereventx), 
            new NumberFieldManipulator(jTFXEnd, numbereventx), 
            new NumberFieldManipulator(jTFYStart, numbereventy),
            new NumberFieldManipulator(jTFYEnd, numbereventy),
            new NumberFieldManipulator(jTFAngel, numbereventa)
        };
        
        positions[0].set(5.0);
        positions[1].set(DatabaseV2.WORKSPACE0.getsaved()-5);
        positions[2].set(5.0);
        positions[3].set(DatabaseV2.WORKSPACE1.getsaved()-5);
        positions[4].set(0.0);

        jPPaint.addPaintEventListener((JPPaintableEvent evt) -> {
            if(ipanel != null)
            {
                evt.getGaraphics().drawImage(ipanel, 0, 0, null);
            }
        });
        
        jLCNCCommands.setModel(new DefaultListModel());
        jLCNCCommands.setCellRenderer(new BasicComboBoxRenderer(){
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                if(value instanceof CNCCommand)
                {
                    final CNCCommand message=(CNCCommand)value;
                    if(message.getState()!=CNCCommand.State.NORMAL)
                    {
                        setBackground(message.getBColor());
                    }
                    else
                    {
                        setForeground(message.getFColor());
                    }
                }
                
                return this;
            }
            
        });        


    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPPaint = new cnc.gcode.controller.JPPaintable();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jBLoadImage = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jBGenerate = new javax.swing.JButton();
        jLabel23 = new javax.swing.JLabel();
        jBMilling = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jPBar = new javax.swing.JProgressBar();
        jBAbrote = new javax.swing.JButton();
        jBPause = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jTFXStart = new javax.swing.JTextField();
        jTFYStart = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jTFXEnd = new javax.swing.JTextField();
        jTFYEnd = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jCBmirroX = new javax.swing.JCheckBox();
        jCBmirroY = new javax.swing.JCheckBox();
        jLabel8 = new javax.swing.JLabel();
        jTFAngel = new javax.swing.JTextField();
        jCBShape = new javax.swing.JComboBox();
        jScrollPane5 = new javax.swing.JScrollPane();
        jLCNCCommands = new javax.swing.JList();

        jPPaint.setMinimumSize(new java.awt.Dimension(10, 10));
        jPPaint.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                jPPaintMouseDragged(evt);
            }
        });
        jPPaint.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                jPPaintMouseWheelMoved(evt);
            }
        });
        jPPaint.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jPPaintMousePressed(evt);
            }
        });
        jPPaint.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                jPPaintComponentResized(evt);
            }
        });

        javax.swing.GroupLayout jPPaintLayout = new javax.swing.GroupLayout(jPPaint);
        jPPaint.setLayout(jPPaintLayout);
        jPPaintLayout.setHorizontalGroup(
            jPPaintLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 412, Short.MAX_VALUE)
        );
        jPPaintLayout.setVerticalGroup(
            jPPaintLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 618, Short.MAX_VALUE)
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Steps"));

        jLabel1.setText("1.)");

        jBLoadImage.setText("Load Image");
        jBLoadImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBLoadImageActionPerformed(evt);
            }
        });

        jLabel3.setText("2.)");

        jBGenerate.setText("Generate");
        jBGenerate.setEnabled(false);
        jBGenerate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBGenerateActionPerformed(evt);
            }
        });

        jLabel23.setText("3.)");

        jBMilling.setText("Milling");
        jBMilling.setEnabled(false);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel23))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jBMilling, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jBLoadImage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBGenerate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jBLoadImage))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jBGenerate))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 7, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel23)
                    .addComponent(jBMilling)))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Progress"));

        jPBar.setRequestFocusEnabled(false);
        jPBar.setString("");
        jPBar.setStringPainted(true);

        jBAbrote.setText("Abort");
        jBAbrote.setEnabled(false);
        jBAbrote.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBAbroteActionPerformed(evt);
            }
        });

        jBPause.setText("Pause");
        jBPause.setEnabled(false);
        jBPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBPauseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jBAbrote)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jBPause)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPBar, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBPause)
                    .addComponent(jBAbrote))
                .addGap(0, 0, 0))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Position"));
        jPanel4.setName("Position"); // NOI18N

        jLabel2.setText("X");

        jLabel4.setText("Y");

        jTFXStart.setMinimumSize(null);
        jTFXStart.setName("null"); // NOI18N

        jTFYStart.setMinimumSize(null);
        jTFYStart.setName("[6, 20]"); // NOI18N

        jLabel5.setText("Start");

        jLabel6.setText("End");

        jTFXEnd.setName(""); // NOI18N

        jTFYEnd.setMinimumSize(null);
        jTFYEnd.setName("null"); // NOI18N

        jLabel7.setText("Mirror");

        jCBmirroX.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCBmirroXItemStateChanged(evt);
            }
        });

        jCBmirroY.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCBmirroYItemStateChanged(evt);
            }
        });

        jLabel8.setText("A");

        jTFAngel.setMinimumSize(null);
        jTFAngel.setName("[6, 20]"); // NOI18N

        jCBShape.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Rectangle", "RoundR. 10%", "RoundR. 20%", "RoundR. 30%", "Ellipse" }));
        jCBShape.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCBShapeItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel2)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jTFAngel, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTFYStart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTFXStart, javax.swing.GroupLayout.DEFAULT_SIZE, 65, Short.MAX_VALUE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(jTFXEnd, javax.swing.GroupLayout.DEFAULT_SIZE, 59, Short.MAX_VALUE)
                            .addComponent(jLabel6)
                            .addComponent(jTFYEnd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(12, 12, 12)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(jCBmirroY)
                            .addComponent(jCBmirroX)
                            .addComponent(jLabel7)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jCBShape, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(jLabel6)
                            .addComponent(jLabel7))
                        .addGap(0, 0, 0)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(jTFXStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTFXEnd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jCBmirroX)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(jTFYStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jTFYEnd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jCBmirroY))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jTFAngel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCBShape, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(21, 21, 21))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        jScrollPane5.setMaximumSize(new java.awt.Dimension(258, 130));
        jScrollPane5.setMinimumSize(new java.awt.Dimension(258, 130));
        jScrollPane5.setPreferredSize(new java.awt.Dimension(258, 130));

        jLCNCCommands.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "dsfds", "sd", "f", "sd", "f", "sd", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "sdfs", " ", "dsf", "ds", "f" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jLCNCCommands.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jLCNCCommands.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLCNCCommandsMouseClicked(evt);
            }
        });
        jScrollPane5.setViewportView(jLCNCCommands);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPPaint, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPPaint, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jPPaintComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPPaintComponentResized
        painter.trigger();
    }//GEN-LAST:event_jPPaintComponentResized

    private void jBLoadImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBLoadImageActionPerformed
        //File choose dialog
        JFileChooser fc = DatabaseV2.getFileChooser();
        if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        try {
            BufferedImage timg = ImageIO.read(fc.getSelectedFile());
            if(timg==null){
                JOptionPane.showMessageDialog(this, "Could not read Image!");  
                img=null;
            }
            else
            {
                img = new BufferedImage(timg.getWidth(),timg.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);

                Graphics2D g2 = img.createGraphics();

                g2.drawImage(timg, 0,0,null);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex);
            img=null;
        }
        
        //clear generated data:        
        moves=null;
        jLCNCCommands.setModel(new DefaultListModel());

        
        pzoom=1;
        pviewmove=new Point();
        mzoom=1;
        mviewmove=new Point();
        positions[4].set(0.0);
        
        fireupdateGUI();
    }//GEN-LAST:event_jBLoadImageActionPerformed

    private void jPPaintMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPPaintMouseDragged
        Point2D p1 = viewmovetrans.transform(new Point2D.Double(evt.getPoint().getX(), evt.getPoint().getY()),null);        
        Point2D p2 = viewmovetrans.transform(new Point2D.Double(viewmovestart.getX(), viewmovestart.getY()),null);
        if(moves==null) 
            pviewmove = new Point2D.Double(viewmovelast.getX() + p1.getX()- p2.getX(), viewmovelast.getY() + p1.getY()- p2.getY());
        else
            mviewmove = new Point2D.Double(viewmovelast.getX() + p1.getX()- p2.getX(), viewmovelast.getY() + p1.getY()- p2.getY());
        painter.trigger();
    }//GEN-LAST:event_jPPaintMouseDragged

    private void jPPaintMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPPaintMousePressed
        if(moves==null) 
            viewmovelast  = pviewmove;
        else
            viewmovelast  = mviewmove;
        viewmovestart = evt.getPoint();
    }//GEN-LAST:event_jPPaintMousePressed

    private void jPPaintMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_jPPaintMouseWheelMoved
        if (evt.getWheelRotation() > 0 )//mouse wheel was rotated up/away from the user
        {
            if(moves==null)
                pzoom*=1.01;
            else
                mzoom*=1.2;
                
        }
        else
        {
            if(moves==null)
                pzoom*=0.99;
            else
                mzoom*=0.80;
        }    
        painter.trigger();
    }//GEN-LAST:event_jPPaintMouseWheelMoved

    private void jBAbroteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBAbroteActionPerformed
        if(worker != null)
            worker.cancel();

        fireupdateGUI();
    }//GEN-LAST:event_jBAbroteActionPerformed

    private void jBPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBPauseActionPerformed
        if(worker != null)
            worker.pause(!worker.isPaused());

        fireupdateGUI();
    }//GEN-LAST:event_jBPauseActionPerformed
    
    enum averageMode{Maximal,Minimal,Meridian,RMS}
    
    private void jBGenerateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBGenerateActionPerformed
        
        ArtSettings a= new ArtSettings();
        try{
            a.fromString(DatabaseV2.ARTSETTINGS.get());
        }
        catch(Exception e){}
        
        if(a.showDialog()==false)
            return;
        
        DatabaseV2.ARTSETTINGS.set(a.toString());
        
        worker= (new PMySwingWorker<DefaultListModel<CNCCommand>,Object>() {
            
            private ArtSettings a=null;
            
            PMySwingWorker init(ArtSettings a){
                this.a=a;
                return this;
            }
            
            @Override
            protected DefaultListModel<CNCCommand> doInBackground() throws Exception {

                double px=positions[0].getdsave();
                double pxw=positions[1].getdsave()-positions[0].getdsave();
                double py=positions[2].getdsave();
                double pyw=positions[3].getdsave()-positions[2].getdsave();
                double pa=positions[4].getdsave();
                boolean pxm=jCBmirroX.isSelected();
                boolean pym=jCBmirroY.isSelected();            
                int pointsx=(int)(pxw/(a.bit_size*a.pline/100))+1;
                int pointsy=(int)(pyw/(a.bit_size*a.psegment/100))+1;

                Shape boarder;
                switch (jCBShape.getSelectedIndex()){
                    case 0:
                    default:
                        boarder=new Rectangle2D.Double(px,py,pxw,pyw);                
                        break;
                    case 1:
                        boarder=( new RoundRectangle2D.Double(px,py,pxw,pyw, Math.max(pxw,pyw)*0.1, Math.max(pxw,pyw)*0.1));
                        break;
                    case 2:
                        boarder=( new RoundRectangle2D.Double(px,py,pxw,pyw, Math.max(pxw,pyw)*0.2, Math.max(pxw,pyw)*0.2));
                        break;
                    case 3:
                        boarder=( new RoundRectangle2D.Double(px,py,pxw,pyw, Math.max(pxw,pyw)*0.3, Math.max(pxw,pyw)*0.3));
                        break;
                    case 4:
                        boarder=( new Ellipse2D.Double(px,py,pxw,pyw));
                        break;
                }            
                
                progress(1, "Process Image");
                
                BufferedImage i = new BufferedImage((int)(pxw*a.ires), (int)(pyw*a.ires), BufferedImage.TYPE_USHORT_GRAY);
                                
                Graphics2D g2 = i.createGraphics();
                
                g2.setColor(a.bgc);
                g2.fillRect(0, 0, i.getWidth(), i.getHeight());
                
                //ViewMove                
                g2.scale(a.ires, a.ires);
                g2.translate(-px, -py);                
                
                g2.setClip(boarder);
                
                //ViewMove
                g2.translate(pviewmove.getX(), pviewmove.getY());                
                    
                Rectangle r=Geometrics.placeRectangle((int)(pxw*1),(int)(pyw*1),Geometrics.getRatio(img.getWidth(), img.getHeight()));
                g2.translate(r.x+px, r.y+py);

                //Zoom
                g2.scale((double)r.width/img.getWidth(), (double)r.height/img.getHeight());

                g2.translate(img.getWidth()/2,img.getHeight()/2);
                g2.rotate(Math.PI/180*pa);
                
                //g2.scale(zoom*(pxm?-1:1), zoom*(pym?-1:1));
                g2.scale(pzoom, pzoom);
                g2.translate(-img.getWidth()/2,-img.getHeight()/2);
                    
                g2.drawImage(img, 0, 0, null);

                if(a.fembossing)
                    i= (new ConvolveOp(new Kernel(3, 3, new float[] { -2, 0, 0, 0, 1, 0, 0, 0, 2 }))).filter(i, null);                                
                if(a.fedge)
                    i= (new ConvolveOp(new Kernel(3, 3,  new float[] {-1, -1, -1,-1,8,-1,-1, -1, -1 }))).filter(i, null);
                if(a.flow)
                    i= (new ConvolveOp(new Kernel(3, 3,  new float[] {0.1f, 0.1f, 0.1f,0.1f,0.2f,0.1f,0.1f, 0.1f, 0.1f }))).filter(i, null);

                BufferedImage ti=new BufferedImage(pointsx, pointsy, BufferedImage.TYPE_USHORT_GRAY);
                ti.getGraphics().drawImage(i.getScaledInstance(pointsx,pointsy,a.iscale.getValue()), 0, 0, null);
                i=ti;

                System.out.println( );
                
                int min=Integer.MAX_VALUE;
                int max=Integer.MIN_VALUE;
                int[][] id= new int[pointsx][pointsy];
                for(int x=0;x<pointsx;x++)
                    for(int y=0;y<pointsy;y++){
                        id[x][y]= Short.toUnsignedInt(((short[])i.getRaster().getDataElements(x, y, null))[0]);
                        min=id[x][y]<min?id[x][y]:min;
                        max=id[x][y]>max?id[x][y]:max;
                        dopause();
                    }                
                //scale
                double[][] z= new double[pointsx][pointsy];
                for(int x=0;x<pointsx;x++)
                    for(int y=0;y<pointsy;y++){
                        z[x][y]= (double)(id[x][y]-min)/(max-min);
                        z[x][y]=a.zmin +z[x][y]*(a.zmax-a.zmin);
                        dopause();
                    }                
                
                progress(1, "Outline Path");

                
                ArrayList<ArrayList<Point>> paths =new ArrayList<>();
                switch(a.pathtype){
                    case YLINES: //ymin to ymax then x++ 
                    default:
                        for(int x=0;x<pointsx;x++){
                            ArrayList<Point> path= new ArrayList<>();
                            for(int y=0;y<pointsy;y++){
                                path.add(new Point(x, y));
                            }                
                            paths.add(path);
                        }
                        break;
                    case XLINES: //xmin to xmax then y++ 
                        for(int y=0;y<pointsy;y++){
                            ArrayList<Point> path= new ArrayList<>();
                            for(int x=0;x<pointsx;x++){
                                path.add(new Point(x, y));
                            }                
                            paths.add(path);
                        }
                        break;
                    case DIAGONAL1: //diagonal +45°
                        for (int diag = 0; diag < (pointsx +  pointsy - 1); diag++) {
                            ArrayList<Point> path= new ArrayList<>();
                            int row_stop = Math.max(0,  diag -  pointsx + 1);
                            int row_start = Math.min(diag, pointsy - 1);
                            for (int row = row_start; row >= row_stop; row--) {
                                // on a given diagonal row + col = constant "diag"
                                // diag labels the diagonal number
                                int col = diag - row;
                                path.add(new Point(col, row));
                            }
                            paths.add(path);
                        }                        
                        break;
                    case DIAGONAL2: //diagonal -45°
                        for (int diag = 0; diag < (pointsy +  pointsx - 1); diag++) {
                            ArrayList<Point> path= new ArrayList<>();
                            int row_stop = Math.max(0,  diag -  pointsy + 1);
                            int row_start = Math.min(diag, pointsx - 1);
                            for (int row = row_start; row >= row_stop; row--) {
                                // on a given diagonal row + col = constant "diag"
                                // diag labels the diagonal number
                                int col = diag - row;
                                path.add(new Point(row, col));
                            }
                            paths.add(path);
                        }                        
                        break;
                }
                //make sweep:
                if(a.sweep){
                    for(int ps=0;ps<paths.size();ps++){
                        if(ps%2==1){
                            //turn path around:
                            for(int p=0;p<paths.get(ps).size()/2;p++){
                                Point temp=paths.get(ps).get(p);
                                int rp=paths.get(ps).size()-1-p;
                                paths.get(ps).set(p, paths.get(ps).get(rp));
                                paths.get(ps).set(rp, temp);                           
                            }
                        }
                    }
                }
                //invert path axis
                paths.stream().forEach((path) -> {path.stream().forEach((p) -> {
                    if(a.mdirx)
                        p.x=pointsx-1-p.x;
                    if(a.mdiry)
                        p.y=pointsy-1-p.y;
                });});
                
                progress(1, "Generate Path");
                
                LinkedList<CNCCommand> cmds = new LinkedList<>();
                
                //Init CNC
                cmds.add(CNCCommand.getStartCommand());                
                cmds.add(new CNCCommand("G0 X"+Tools.dtostr(px)+" Y"+Tools.dtostr(py)+" F"+Tools.dtostr(a.ftravel)));

                //Start Spindel
                cmds.add(new CNCCommand("M4"));
                
                boolean down=false;
                int count=0;
                for(ArrayList<Point> path:paths)
                {
                    for(Point p:path){
                        //calc pos:
                        Point2D.Double ap = new Point2D.Double(px+pxw/pointsx*p.x, py+pyw/pointsy*p.y);
                        
                        if(!boarder.contains(ap)){
                            if(down){
                                cmds.add(new CNCCommand("G0 Z"+Tools.dtostr(a.zsave)+" F"+Tools.dtostr(a.ftravel)));
                                down=false;                                
                            }
                        }
                        else
                        {
                            if(!down){
                                cmds.add(new CNCCommand("G0 X"+Tools.dtostr(ap.x)+" Y"+Tools.dtostr(ap.y)+" F"+Tools.dtostr(a.ftravel)));                                
                                cmds.add(new CNCCommand("G1 Z"+Tools.dtostr(z[p.x][p.y])+" F"+Tools.dtostr(a.fmill)));
                                down=true;                                
                            }
                            else{
                                cmds.add(new CNCCommand("G1 X"+Tools.dtostr(ap.x)+" Y"+Tools.dtostr(ap.y)+" Z"+Tools.dtostr(z[p.x][p.y])+" F"+Tools.dtostr(a.fmill)));
                            }
                        }                        
                    }                                
                    //Move to save height
                    if(down){
                        cmds.add(new CNCCommand("G0 Z"+Tools.dtostr(a.zsave)+" F"+Tools.dtostr(a.ftravel)));
                        down=false;
                    }
                    progress((int)(50.0*(count++)/(paths.size())), "Generate Path");
                    dopause();
                }
                //Stop Spindel
                cmds.add(new CNCCommand("M5"));
                
                
                progress(50, "Process Path");

                CNCCommand.Calchelper c = new CNCCommand.Calchelper();
                c.ignorZMoveWaring=true;
                ArrayList<CNCCommand.Move> moves= new ArrayList<>();

                DefaultListModel<CNCCommand> model = new DefaultListModel<>();
                count=0;
                for(CNCCommand command:cmds){
                    CNCCommand.State t = command.calcCommand(c);
                    if(t!=CNCCommand.State.NORMAL){
                        //throw new Exception("Generator dont knows its own commands... should not happen :-(");
                    }
                    
                    model.addElement(command);
                    
                    moves.addAll(Arrays.asList(command.getMoves()));
                    
                    progress((int)(50+50.0*(count++)/(cmds.size())), "Process Path");
                    dopause();
                }
                
                JPanelArt.this.secounds=(long)c.seconds;
                JPanelArt.this.moves=moves;
                return model;
            }

            @Override
            protected void done(DefaultListModel<CNCCommand> rvalue, Exception ex, boolean canceled) {
                String message;
                if(canceled)
                {
                    message = "Canceled!";
                    JPanelArt.this.moves=null;
                }
                else if(ex != null)
                {
                    message ="Error loading File! (" + ex.getMessage() + ")";
                    Logger.getLogger(JPanelCNCMilling.class.getName()).log(Level.SEVERE, null, ex);
                    JPanelArt.this.moves=null;
                }
                else
                {
                    message      = "Done";
                    jPBar.setString("~" + Tools.formatDuration(secounds));
                    JPanelArt.this.toold=a.bit_size;
                    JPanelArt.this.zmax=a.zmax;
                    JPanelArt.this.zmin=a.zmin;
                    jLCNCCommands.setModel(rvalue);
                }

                
                mzoom=1;
                mviewmove=new Point();
                positions[4].set(0.0);
               
                JOptionPane.showMessageDialog(JPanelArt.this, message);

                fireupdateGUI();
            }
        }).init(a);

        worker.execute();

        fireupdateGUI();
    }//GEN-LAST:event_jBGenerateActionPerformed

    private void jCBmirroXItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCBmirroXItemStateChanged
        painter.trigger();
    }//GEN-LAST:event_jCBmirroXItemStateChanged

    private void jCBmirroYItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCBmirroYItemStateChanged
        painter.trigger();
    }//GEN-LAST:event_jCBmirroYItemStateChanged

    private void jCBShapeItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCBShapeItemStateChanged
        painter.trigger();
    }//GEN-LAST:event_jCBShapeItemStateChanged

    private void showInfos(int index){
        if(index != -1)
        {
            ArrayList<String> lines= new ArrayList<>();
            lines.addAll(Arrays.asList(
            ((CNCCommand)jLCNCCommands.getModel().getElementAt(index)).getInfos(new CNCCommand.Transform(0,
                                                                                                                          0,
                                                                                                                          0,
                                                                                                                          false,
                                                                                                                          false),
                                                                                                                          AutoLevelSystem.leveled()).split("\n")
            ));
            
            for(CNCCommand.Move m:((CNCCommand)jLCNCCommands.getModel().getElementAt(index)).getMoves()){
                lines.add("Move: Start->" +Arrays.toString(m.getStart())+ " End->" +Arrays.toString(m.getEnd())+" Type ->" +m.getType() );
            }
            
            
            
            JList<String> list = new JList<>(lines.toArray(new String[0]));

            JScrollPane sp = new JScrollPane(list);

            JOptionPane.showMessageDialog(this,sp);
        }
    }
    
    private void jLCNCCommandsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLCNCCommandsMouseClicked
        if(evt.getClickCount() == 2)
        {
            showInfos(jLCNCCommands.locationToIndex(evt.getPoint()));
        }
    }//GEN-LAST:event_jLCNCCommandsMouseClicked


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBAbrote;
    private javax.swing.JButton jBGenerate;
    private javax.swing.JButton jBLoadImage;
    private javax.swing.JButton jBMilling;
    private javax.swing.JButton jBPause;
    private javax.swing.JComboBox jCBShape;
    private javax.swing.JCheckBox jCBmirroX;
    private javax.swing.JCheckBox jCBmirroY;
    private javax.swing.JList jLCNCCommands;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JProgressBar jPBar;
    private cnc.gcode.controller.JPPaintable jPPaint;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTextField jTFAngel;
    private javax.swing.JTextField jTFXEnd;
    private javax.swing.JTextField jTFXStart;
    private javax.swing.JTextField jTFYEnd;
    private javax.swing.JTextField jTFYStart;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setGUIEvent(IEvent event) {
        GUIevent=event;
    }

    public boolean isRunning()
    {
        return worker != null && !worker.isDone();
    }
    
    private void fireupdateGUI()
    {
        if(GUIevent == null)
        {
            throw new RuntimeException("GUI EVENT NOT USED!");
        }
        GUIevent.fired();
    }
    
    @Override
    public void updateGUI(boolean serial, boolean isworking) {
        jBLoadImage.setEnabled(!isRunning());
        jBGenerate.setEnabled(img!=null && !isRunning());        
        jBMilling.setEnabled(!isworking && moves!=null);

        if(serial){
            jBMilling.setText("Milling");            
        }
        else
        {
            jBMilling.setText("Export");
        }
        
        jBAbrote.setEnabled(isRunning());
        if(isRunning() == false) 
        {
            jPBar.setValue(0);
        }
        if(moves==null)
        {
            jPBar.setString(" ");
        }
        jBPause.setEnabled(isRunning());
        jBPause.setText((isRunning() && worker.isPaused()) ? "Resume":"Pause");

        jTFAngel.setEnabled(!isRunning() && moves==null);
        jTFXEnd.setEnabled(!isRunning() && moves==null);
        jTFXStart.setEnabled(!isRunning() && moves==null);
        jTFYEnd.setEnabled(!isRunning() && moves==null);
        jTFYStart.setEnabled(!isRunning() && moves==null);
        jCBmirroX.setEnabled(!isRunning() && moves==null);
        jCBmirroY.setEnabled(!isRunning() && moves==null);
        jTFAngel.setEnabled(!isRunning() && moves==null);
        jCBShape.setEnabled(!isRunning() && moves==null);

        painter.trigger();
    }
}
