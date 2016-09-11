/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author patrick
 */
public class JPanelArt extends javax.swing.JPanel implements IGUIEvent{

    IEvent GUIevent=null;
    private BufferedImage ipanel=null;
    private BufferedImage img=null;
    private NumberFieldManipulator[] positions;
    private double zoom=1;
    private Point2D viewmove    =  new Point();
    private Point2D viewzoom    =  new Point();
    private Point2D viewmovelast=  new Point();
    private Point viewmovestart = new Point();
    private AffineTransform viewmovetrans = new AffineTransform();
    
    private boolean ganeraded=false;
    
    
    
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
                        data.zoom       = zoom;
                        data.viewmove   = viewmove;
                        data.px         = positions[0].getdsave();
                        data.pxw        = positions[1].getdsave()-positions[0].getdsave();
                        data.py         = positions[2].getdsave();
                        data.pyw        = positions[3].getdsave()-positions[2].getdsave();
                        data.pa         = jSAngel.getValue();
                        data.pxm        = jCBmirroX.isSelected();
                        data.pym        = jCBmirroY.isSelected();
                    }
                });        
                
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

                //Scalling transforming ...

                //StartCorner
                g2.translate(data.jpw / 2, data.jph / 2);
                switch(Integer.parseInt(DatabaseV2.HOMING.get()))
                {
                    case 0:
                    default:
                        g2.scale(1,1);
                        break;
                    case 1:
                       g2.scale(-1,1);
                       break;
                    case 2:
                       g2.scale(1,-1);
                       break;
                    case 3:                
                       g2.scale(-1,-1);
                       break;
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

                AffineTransform oldat=g2.getTransform();
                            
                g2.setClip(new Rectangle2D.Double(data.px,data.py,data.pxw,data.pyw));
                
                if(img!=null)
                {
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
                    g2.scale(data.zoom*(data.pxm?-1:1), data.zoom*(data.pym?-1:1));
                    g2.rotate(Math.PI/180*data.pa);
                    g2.translate(-img.getWidth()/2,-img.getHeight()/2);
                    
                    g2.drawImage(img, 0, 0, null);
                }

                
                //Draw selection
                g2.setTransform(oldat);
                g2.setStroke(new BasicStroke((float)(2/scaley)));
                g2.setColor(Color.BLACK);
                //g2.fill(new Rectangle2D.Double(data.px, data.py, data.pxw, data.pyw));
                g2.draw(new Line2D.Double(data.px,data.py,data.px+data.pxw,data.py));
                g2.draw(new Line2D.Double(data.px,data.py+data.pyw,data.px+data.pxw,data.py+data.pyw));
                g2.draw(new Line2D.Double(data.px,data.py,data.px,data.py+data.pyw));
                g2.draw(new Line2D.Double(data.px+data.pxw,data.py,data.px+data.pxw,data.py+data.pyw));
                
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
        
        NumberFieldManipulator.IAxesEvent numbereventx= (NumberFieldManipulator axis) -> {
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

            zoom=1;
            viewmove=new Point();
            painter.trigger();            

        };
        NumberFieldManipulator.IAxesEvent numbereventy= (NumberFieldManipulator axis) -> {
            Double value;
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
            
            zoom=1;
            viewmove=new Point();
            painter.trigger();            
        };
        
        positions = new NumberFieldManipulator[] {new NumberFieldManipulator(jTFXStart, numbereventx), new NumberFieldManipulator(jTFXEnd, numbereventx), new NumberFieldManipulator(jTFYStart, numbereventy),new NumberFieldManipulator(jTFYEnd, numbereventy)};
        
        positions[0].set(0.0);
        positions[1].set(DatabaseV2.WORKSPACE0.getsaved());
        positions[2].set(0.0);
        positions[3].set(DatabaseV2.WORKSPACE1.getsaved());

        jPPaint.addPaintEventListener((JPPaintableEvent evt) -> {
            if(ipanel != null)
            {
                evt.getGaraphics().drawImage(ipanel, 0, 0, null);
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
        jSAngel = new javax.swing.JSlider();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();

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
            .addGap(0, 413, Short.MAX_VALUE)
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

        jLabel8.setText("Rotate");

        jSAngel.setMajorTickSpacing(10);
        jSAngel.setMaximum(180);
        jSAngel.setMinimum(-180);
        jSAngel.setValue(0);
        jSAngel.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSAngelStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(jLabel5)
                            .addComponent(jTFXStart, javax.swing.GroupLayout.DEFAULT_SIZE, 65, Short.MAX_VALUE)
                            .addComponent(jTFYStart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
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
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSAngel, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
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
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addComponent(jSAngel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
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
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        jList1.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(jList1);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 407, Short.MAX_VALUE))
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
        if(fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        try {
            img = ImageIO.read(fc.getSelectedFile());
            if(img==null){
                JOptionPane.showMessageDialog(this, "Could not read Image!");            
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex);
            return;
        }
        
        zoom=1;
        viewmove=new Point();
        
        fireupdateGUI();
    }//GEN-LAST:event_jBLoadImageActionPerformed

    private void jPPaintMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPPaintMouseDragged
        Point2D p1 = viewmovetrans.transform(new Point2D.Double(evt.getPoint().getX(), evt.getPoint().getY()),null);        
        Point2D p2 = viewmovetrans.transform(new Point2D.Double(viewmovestart.getX(), viewmovestart.getY()),null);
        viewmove = new Point2D.Double(viewmovelast.getX() + p1.getX()- p2.getX(), viewmovelast.getY() + p1.getY()- p2.getY());
        painter.trigger();
    }//GEN-LAST:event_jPPaintMouseDragged

    private void jPPaintMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPPaintMousePressed
        viewmovelast  = viewmove;
        viewmovestart = evt.getPoint();
    }//GEN-LAST:event_jPPaintMousePressed

    private void jPPaintMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_jPPaintMouseWheelMoved
        viewzoom=viewmovetrans.transform(new Point2D.Double(evt.getPoint().getX(), evt.getPoint().getY()),null);
        if (evt.getWheelRotation() > 0 )//mouse wheel was rotated up/away from the user
        {
            zoom*=1.01;
        }
        else
        {
            zoom*=0.99;
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

    private void jBGenerateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBGenerateActionPerformed
        
        final Double px=positions[0].getdsave();
        final Double pxw=positions[1].getdsave()-positions[0].getdsave();
        final Double py=positions[2].getdsave();
        final Double pyw=positions[3].getdsave()-positions[2].getdsave();
        final int resscale=10;
        final Color bgc=Color.WHITE;
        
        worker= new PMySwingWorker<String,Object>() {
            
            @Override
            protected String doInBackground() throws Exception {
                
                progress(0, "1/3 Generate Image");
                
                BufferedImage i = new BufferedImage((int)(pxw*resscale), (int)(pyw*resscale), BufferedImage.TYPE_USHORT_GRAY);
                
                Graphics2D g2 = i.createGraphics();
                
                g2.setColor(bgc);
                g2.fillRect(0, 0, i.getWidth(), i.getHeight());
                
                //ViewMove
                g2.translate(i.getWidth() / 2, i.getHeight() / 2);
                switch(Integer.parseInt(DatabaseV2.HOMING.get()))
                {
                    case 0:
                    default:
                        g2.scale(1,1);
                        break;
                    case 1:
                       g2.scale(-1,1);
                       break;
                    case 2:
                       g2.scale(1,-1);
                       break;
                    case 3:                
                       g2.scale(-1,-1);
                       break;
                }
                g2.translate(-i.getWidth() / 2, -i.getHeight() / 2);
                
                g2.scale(resscale, resscale);
                g2.translate(-px, -py);                
                g2.translate(viewmove.getX(), viewmove.getY());                

                Rectangle r=Geometrics.placeRectangle((int)(pxw*1),(int)(pyw*1),Geometrics.getRatio(img.getWidth(), img.getHeight()));
                g2.translate(r.x+px, r.y+py);

                //Zoom
                g2.scale((double)r.width/img.getWidth(), (double)r.height/img.getHeight());

                g2.translate(img.getWidth()/2,img.getHeight()/2);
                g2.scale(zoom, zoom);
                g2.translate(-img.getWidth()/2,-img.getHeight()/2);

                g2.drawImage(img, 0, 0, null);

                JLabel lbl = new JLabel(new ImageIcon(i));
                JOptionPane.showMessageDialog(null, lbl, "ImageDialog", 
                                             JOptionPane.PLAIN_MESSAGE, null);
                
                dopause();
                return "";
            }

            @Override
            protected void done(String rvalue, Exception ex, boolean canceled) {

                fireupdateGUI();
            }
        };

        worker.execute();

        fireupdateGUI();
    }//GEN-LAST:event_jBGenerateActionPerformed

    private void jCBmirroXItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCBmirroXItemStateChanged
        painter.trigger();
    }//GEN-LAST:event_jCBmirroXItemStateChanged

    private void jCBmirroYItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCBmirroYItemStateChanged
        painter.trigger();
    }//GEN-LAST:event_jCBmirroYItemStateChanged

    private void jSAngelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSAngelStateChanged
        painter.trigger();
    }//GEN-LAST:event_jSAngelStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBAbrote;
    private javax.swing.JButton jBGenerate;
    private javax.swing.JButton jBLoadImage;
    private javax.swing.JButton jBMilling;
    private javax.swing.JButton jBPause;
    private javax.swing.JCheckBox jCBmirroX;
    private javax.swing.JCheckBox jCBmirroY;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JList jList1;
    private javax.swing.JProgressBar jPBar;
    private cnc.gcode.controller.JPPaintable jPPaint;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JSlider jSAngel;
    private javax.swing.JScrollPane jScrollPane1;
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
        jBMilling.setEnabled(!isworking && ganeraded);

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
        if(ganeraded == false)
        {
            jPBar.setString(" ");
        }
        jBPause.setEnabled(isRunning());
        jBPause.setText((isRunning() && worker.isPaused()) ? "Resume":"Pause");

        jSAngel.setEnabled(!isRunning() );
        jTFXEnd.setEnabled(!isRunning() );
        jTFXStart.setEnabled(!isRunning() );
        jTFYEnd.setEnabled(!isRunning() );
        jTFYStart.setEnabled(!isRunning() );
        jCBmirroX.setEnabled(!isRunning() );
        jCBmirroY.setEnabled(!isRunning() );

        painter.trigger();
    }
}
