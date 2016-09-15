/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import cnc.gcode.controller.communication.ComInterruptException;
import cnc.gcode.controller.communication.Communication;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
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
public class JPanelCNCMilling extends javax.swing.JPanel implements IGUIEvent{

    private IEvent GUIEvent = null;

    private abstract class PMySwingWorker<R, P> extends MySwingWorker<R, P>
    {

        @Override
        protected final void progress(int progress,String message) {
            jPBar.setValue(progress);
            jPBar.setString(message);
        }
        
    }
    
    
    private NumberFieldManipulator[] positioningMove;
    private NumberFieldManipulator[] positioningScale;
    private PMySwingWorker worker   = null;
    private boolean cncLoadedFile   = false;
    private PrintableLayers layers  = new PrintableLayers();
    private AffineTransform trans   = new AffineTransform();
    private long maxTime            = 0;
    private BufferedImage image;
    
    private Point2D viewmove    =  new Point();
    private Point2D viewmovelast=  new Point();
    private Point viewmovestart = new Point();
    private AffineTransform viewmovetrans = new AffineTransform();

    private final TriggertSwingWorker<BufferedImage> painter = new TriggertSwingWorker<BufferedImage>() {
        class GetDataSyncedHelper
        {
            private int jpw;
            private int jph;
            private int zoom;
            private double movex;
            private double movey;
            private boolean mirrorx;
            private boolean mirrory;
            private int index;
            private Point2D viewmove;

        }

        @Override
        protected BufferedImage doJob() throws Exception {

            //Load Parameter:
            final GetDataSyncedHelper data = new GetDataSyncedHelper();
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    data.jpw    = jPPaint.getWidth();
                    data.jph    = jPPaint.getHeight();
                    data.zoom   = jSZoom.getValue();
                    data.movex  = positioningMove[0].getdsave();
                    data.movey  = positioningMove[1].getdsave();
                    data.mirrorx= jCBmirroX.isSelected();
                    data.mirrory= jCBmirroY.isSelected();
                    data.index  = jCBPerview.getSelectedIndex();
                    data.viewmove = viewmove;
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

            BufferedImage image = new BufferedImage(data.jpw, data.jph, BufferedImage.TYPE_4BYTE_ABGR);

            Graphics2D g2 = image.createGraphics();


            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            //Scalling transforming ...
            if(layers.paintlegend(g2, data.index,data.jpw,data.jph)){
                data.jpw=data.jpw-100;
            }
            
            
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
            //Zoom
            double zoom = 1;
            if(data.zoom <= 10)
            {
                zoom = data.zoom / 10.0;
            }
            else
            {
                zoom = data.zoom - 9;
            }
            g2.scale(zoom, zoom);

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
            g2.translate(data.viewmove.getX(), data.viewmove.getY());                
            try {
                AffineTransform t = g2.getTransform();
                t.invert();
                viewmovetrans = t;
            } catch (NoninvertibleTransformException ex) {
                viewmovetrans = new AffineTransform();
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

            //Positioning
            g2.translate(data.movex, data.movey);
            g2.scale((data.mirrorx ? -1:1),
                     (data.mirrory ? -1:1));

            try {
                AffineTransform t = g2.getTransform();
                t.invert();
                trans = t;
            } catch (NoninvertibleTransformException ex) {
                trans = new AffineTransform();
            }

            layers.paint(g2, data.index); 

            return image;
        }

        @Override
        protected void process(BufferedImage chunk) {
            image = chunk;

            jPPaint.repaint();
        }

    };
    
    
    /**
     * Creates new form JPanelCNCMilling
     */
    public JPanelCNCMilling() {
        initComponents();

        jLCNCCommands.setModel(new DefaultListModel());
        jLCNCCommands.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e)
            {
                    if ( SwingUtilities.isRightMouseButton(e) )
                    {
                            jLCNCCommands.setSelectedIndex(jLCNCCommands.locationToIndex(e.getPoint()));
                    }
            }        
        });
        
        NumberFieldManipulator.IChangeEvent numbereventxy= new NumberFieldManipulator.IChangeEvent() {
            @Override
            public void fired(NumberFieldManipulator axis) {
                Double value;
                try {
                   value = axis.getd();
                } catch (ParseException ex) {
                    axis.popUpToolTip(ex.toString());
                    axis.setFocus();
                    return;
                }
        
                //Write back Value
                axis.set(value);
                
                jCBPerview.setModel(new DefaultComboBoxModel(layers.getLayers(positioningMove[2].getdsave()))); //Clear Layers
                painter.trigger();
                
            }
        };
        NumberFieldManipulator.IChangeEvent numbereventz= new NumberFieldManipulator.IChangeEvent() {
            @Override
            public void fired(NumberFieldManipulator axis) {
                Double value;
                try {
                   value = axis.getd();
                } catch (ParseException ex) {
                    axis.popUpToolTip(ex.toString());
                    axis.setFocus();
                    return;
                }
        
                //Write back Value
                axis.set(value);
                
                jCBPerview.setModel(new DefaultComboBoxModel(layers.getLayers(positioningMove[2].getdsave()))); //Clear Layers
                painter.trigger();
                
            }
        };
        NumberFieldManipulator.IChangeEvent numberevents= new NumberFieldManipulator.IChangeEvent() {
            @Override
            public void fired(NumberFieldManipulator axis) {
                Double value;
                try {
                   value = axis.getd();
                } catch (ParseException ex) {
                    axis.popUpToolTip(ex.toString());
                    axis.setFocus();
                    return;
                }
        
                if(value==0){
                    axis.popUpToolTip("Scale cannot be 0");
                    axis.setFocus();
                    return;
                }
                
                //Write back Value
                axis.set(value);

                if(jCBScalleXY.isSelected()){
                    for(NumberFieldManipulator fild:positioningScale)
                    {
                        fild.set(value);
                    }
                }
                
                painter.trigger();
                
            }
        };

        
        positioningMove = new NumberFieldManipulator[] {new NumberFieldManipulator(jTFmoveX, numbereventxy), new NumberFieldManipulator(jTFmoveY, numbereventxy), new NumberFieldManipulator(jTFmoveZ, numbereventz)};
        
        for(NumberFieldManipulator fild:positioningMove)
        {
            fild.set(0.0);
        }

        positioningScale = new NumberFieldManipulator[] {new NumberFieldManipulator(jTFscaleX, numberevents), new NumberFieldManipulator(jTFscaleY, numberevents)};

        for(NumberFieldManipulator fild:positioningScale)
        {
            fild.set(1.0);
        }
        
        
        //CNC Milling
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
        
        
        jPPaint.addPaintEventListener((JPPaintableEvent evt) -> {
            if(image != null)
            {
                evt.getGaraphics().drawImage(image, 0, 0, null);
            }
        });
        
    }

    @Override
    public void setGUIEvent(IEvent event)
    {
        GUIEvent = event;
    }

    @Override
    public void updateGUI(boolean serial, boolean isworking)
    {
        jCBPerview.setEnabled(cncLoadedFile);
        jLoadFile.setEnabled(!isRunning());
        jBOptimise.setEnabled(cncLoadedFile && !isRunning());
        jBMilling.setEnabled(!isworking && cncLoadedFile);
        jMIStart.setEnabled(!isworking && cncLoadedFile);
        jMIEdit.setEnabled(!isworking && cncLoadedFile);
        
        if(serial){
            jBMilling.setText("Milling");            
            jMIStart.setText("Milling from here");            
        }
        else
        {
            jBMilling.setText("Export");
            jMIStart.setText("Export form here");
        }
        
        jBAbrote.setEnabled(isRunning());
        if(isRunning() == false) 
        {
            jPBar.setValue(0);
        }
        if(cncLoadedFile == false)
        {
            jPBar.setString(" ");
        }
        jBPause.setEnabled(isRunning());
        jBPause.setText((isRunning() && worker.isPaused()) ? "Resume":"Pause");

        jTFmoveX.setEnabled(!cncLoadedFile || !isRunning() );
        jTFmoveY.setEnabled(!cncLoadedFile || !isRunning() );
        jCBmirroX.setEnabled(!cncLoadedFile || !isRunning() );
        jCBmirroY.setEnabled(!cncLoadedFile || !isRunning() );
        jCBAutoLeveling.setEnabled((!cncLoadedFile || !isRunning())&& AutoLevelSystem.leveled()  );
        if(AutoLevelSystem.leveled() == false)
        {
            jCBAutoLeveling.setSelected(false);
        }
        painter.trigger();

    }
    
    private void fireupdateGUI()
    {
        if(GUIEvent == null)
        {
            throw new RuntimeException("GUI EVENT NOT USED!");
        }
        GUIEvent.fired();
    }

    
    @Override
    public boolean isRunning()
    {
        return worker != null && !worker.isDone();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMPcommand = new javax.swing.JPopupMenu();
        jMIInfo = new javax.swing.JMenuItem();
        jMIStart = new javax.swing.JMenuItem();
        jMIEdit = new javax.swing.JMenuItem();
        jPPaint = new cnc.gcode.controller.JPPaintable();
        jPanel3 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLoadFile = new javax.swing.JButton();
        jLabel23 = new javax.swing.JLabel();
        jBMilling = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jBOptimise = new javax.swing.JButton();
        jCBSingleStep = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jTFmoveX = new javax.swing.JTextField();
        jTFmoveY = new javax.swing.JTextField();
        jCBmirroY = new javax.swing.JCheckBox();
        jCBmirroX = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jCBAutoLeveling = new javax.swing.JCheckBox();
        jLabel10 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jTFscaleX = new javax.swing.JTextField();
        jTFscaleY = new javax.swing.JTextField();
        jBScale = new javax.swing.JButton();
        jTFmoveZ = new javax.swing.JTextField();
        jCBScalleXY = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jCBPerview = new javax.swing.JComboBox();
        jLabel9 = new javax.swing.JLabel();
        jSZoom = new javax.swing.JSlider();
        jCBScroll = new javax.swing.JCheckBox();
        jLabel11 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jPBar = new javax.swing.JProgressBar();
        jBAbrote = new javax.swing.JButton();
        jBPause = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        jLCNCCommands = new javax.swing.JList();

        jMIInfo.setText("Info");
        jMIInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIInfoActionPerformed(evt);
            }
        });
        jMPcommand.add(jMIInfo);

        jMIStart.setText("Start here");
        jMIStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIStartActionPerformed(evt);
            }
        });
        jMPcommand.add(jMIStart);

        jMIEdit.setText("Edit");
        jMIEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIEditActionPerformed(evt);
            }
        });
        jMPcommand.add(jMIEdit);

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
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPPaintMouseClicked(evt);
            }
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
            .addGap(0, 710, Short.MAX_VALUE)
        );
        jPPaintLayout.setVerticalGroup(
            jPPaintLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Steps"));

        jLabel1.setText("1.)");

        jLoadFile.setText("Load File");
        jLoadFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jLoadFileActionPerformed(evt);
            }
        });

        jLabel23.setText("3.)");

        jBMilling.setText("Milling");
        jBMilling.setEnabled(false);
        jBMilling.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBMillingActionPerformed(evt);
            }
        });

        jLabel3.setText("2.)");

        jBOptimise.setText("Optimize");
        jBOptimise.setEnabled(false);
        jBOptimise.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBOptimiseActionPerformed(evt);
            }
        });

        jCBSingleStep.setText("Single step");

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
                        .addGap(18, 18, 18)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLoadFile)
                            .addComponent(jBMilling)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(jCBSingleStep))))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(18, 18, 18)
                        .addComponent(jBOptimise)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLoadFile))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jBOptimise))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel23)
                    .addComponent(jBMilling))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCBSingleStep)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Positioning"));

        jLabel6.setText("Y");

        jCBmirroY.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCBmirroItemStateChanged(evt);
            }
        });

        jCBmirroX.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCBmirroItemStateChanged(evt);
            }
        });

        jLabel2.setText("X");

        jLabel7.setText("Mirror:");

        jLabel8.setText("Move:");

        jCBAutoLeveling.setText("AutoLeveling");
        jCBAutoLeveling.setToolTipText("");

        jLabel10.setText("Z");

        jLabel13.setText("Scale:");

        jBScale.setText("Scale");
        jBScale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBScaleActionPerformed(evt);
            }
        });

        jCBScalleXY.setText("X=Y");
        jCBScalleXY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBScalleXYActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel2)
                                .addComponent(jLabel6))
                            .addComponent(jLabel10))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(jLabel8)
                            .addComponent(jTFmoveX, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTFmoveY, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTFmoveZ, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(jCBmirroX)
                            .addComponent(jLabel7)
                            .addComponent(jCBmirroY)))
                    .addComponent(jCBAutoLeveling))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jCBScalleXY)
                    .addComponent(jBScale)
                    .addComponent(jTFscaleY, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTFscaleX, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(jLabel13)
                            .addComponent(jLabel8))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel2)
                                    .addComponent(jTFmoveX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(jCBmirroX))
                            .addComponent(jTFscaleX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel6)
                                .addComponent(jTFmoveY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jCBmirroY, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jTFscaleY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTFmoveZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCBScalleXY)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCBAutoLeveling)
                    .addComponent(jBScale))
                .addGap(0, 3, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Preview"));

        jLabel5.setText("Layer:");

        jCBPerview.setEnabled(false);
        jCBPerview.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCBPerviewItemStateChanged(evt);
            }
        });

        jLabel9.setText("Zoom:");

        jSZoom.setMaximum(19);
        jSZoom.setMinimum(1);
        jSZoom.setMinorTickSpacing(1);
        jSZoom.setSnapToTicks(true);
        jSZoom.setValue(10);
        jSZoom.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSZoomStateChanged(evt);
            }
        });

        jCBScroll.setSelected(true);
        jCBScroll.setText("AutoScroll");

        jLabel11.setText("Commands:");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(jLabel9)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSZoom, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCBScroll)
                            .addComponent(jCBPerview, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jCBPerview, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(jSZoom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCBScroll)
                    .addComponent(jLabel11)))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Progress"));

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

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jBAbrote)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jBPause)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPBar, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBPause)
                    .addComponent(jBAbrote))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jPanel1, jPanel2});

        jScrollPane5.setMaximumSize(new java.awt.Dimension(258, 130));
        jScrollPane5.setMinimumSize(new java.awt.Dimension(258, 130));
        jScrollPane5.setPreferredSize(new java.awt.Dimension(258, 130));

        jLCNCCommands.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "dsfds", "sd", "f", "sd", "f", "sd", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "sdfs", " ", "dsf", "ds", "f" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jLCNCCommands.setComponentPopupMenu(jMPcommand);
        jLCNCCommands.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jLCNCCommands.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLCNCCommandsMouseClicked(evt);
            }
        });
        jLCNCCommands.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jLCNCCommandsValueChanged(evt);
            }
        });
        jScrollPane5.setViewportView(jLCNCCommands);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPPaint, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPPaint, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jBAbroteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBAbroteActionPerformed

        if(worker != null)
        {
            worker.cancel();
        }

        fireupdateGUI();
    }//GEN-LAST:event_jBAbroteActionPerformed

    private void showInfos(int index){
        if(index != -1)
        {
            String[] lines = ((CNCCommand)jLCNCCommands.getModel().getElementAt(index)).getInfos(new CNCCommand.Transform(positioningMove[0].getdsave(),
                                                                                                                          positioningMove[1].getdsave(),
                                                                                                                          positioningMove[2].getdsave(),
                                                                                                                          jCBmirroX.isSelected(),
                                                                                                                          jCBmirroY.isSelected()),
                                                                                                                          jCBAutoLeveling.isSelected()).split("\n");

            JList<String> list = new JList<>(lines);
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

    private void jBPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBPauseActionPerformed
        if(worker != null)
            worker.pause(!worker.isPaused());

        fireupdateGUI();
    }//GEN-LAST:event_jBPauseActionPerformed

    private void jCBPerviewItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCBPerviewItemStateChanged
        jSZoom.setValue(10);
        viewmove = new Point();
        painter.trigger();
    }//GEN-LAST:event_jCBPerviewItemStateChanged

    private void jCBmirroItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCBmirroItemStateChanged
        painter.trigger();
    }//GEN-LAST:event_jCBmirroItemStateChanged

    private void jSZoomStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSZoomStateChanged
        painter.trigger();
    }//GEN-LAST:event_jSZoomStateChanged

    private void jLCNCCommandsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jLCNCCommandsValueChanged
        if(!evt.getValueIsAdjusting())
        {
            layers.setSelectedIndexs(jLCNCCommands.getSelectedIndices());
            painter.trigger();
        }
    }//GEN-LAST:event_jLCNCCommandsValueChanged

    private void jPPaintMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPPaintMouseClicked
        Point2D p = trans.transform(new Point2D.Double(evt.getX(), evt.getY()), null);
        jLCNCCommands.setSelectedIndices(layers.getIndexes(p,jCBPerview.getSelectedIndex()));
        jLCNCCommands.ensureIndexIsVisible(jLCNCCommands.getSelectedIndex());
    }//GEN-LAST:event_jPPaintMouseClicked

    private void executeCNC(int istart){
        
        final boolean serial = Communication.isConnected();
        final CNCCommand[] cmds;
        
        CNCCommand[] cmds_ = new CNCCommand[jLCNCCommands.getModel().getSize()];
        ((DefaultListModel<CNCCommand>)jLCNCCommands.getModel()).copyInto(cmds_);
        if(istart > 0)
        {
            //Skipp all to istart (expect start command (index 0)
             cmds = new CNCCommand[cmds_.length-istart+1];
             cmds[0] = cmds_[0]; //Compy home
             for(int i = 1;i < cmds.length;i++)
             {
                 cmds[i] = cmds_[i + istart - 1];
             }
        }
        else
        {
             cmds = cmds_;
        }

        PrintWriter temp = null;
        if (!serial)
        {
            //File choose dialog
            JFileChooser fc = DatabaseV2.getFileChooser();
            if(fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            {
                return;
            }
            try {
                temp = new PrintWriter(fc.getSelectedFile());
            } catch (FileNotFoundException ex) {
                return;
            }
        }
        final PrintWriter export = temp;
        
        
        worker= new PMySwingWorker<Object,CNCCommand>() {
            CNCCommand.Transform t = new CNCCommand.Transform(positioningMove[0].getdsave(), positioningMove[1].getdsave(), positioningMove[2].getdsave(), jCBmirroX.isSelected(), jCBmirroY.isSelected());
 
            @Override
            protected Object doInBackground() throws Exception {
                int length = cmds.length;
                if (length == 0)
                {
                    length = 1;
                }
                for(int i = 0;i < cmds.length;i++)
                {
                    CNCCommand cmd = cmds[i];
                    publish(cmd);
                    setProgress(100 * i / length, "~" + Tools.formatDuration(maxTime - cmd.getSecounds()));
                
                    final ObjectProxy<Boolean> status= new ObjectProxy<>();
                    SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            status.set(jCBSingleStep.isSelected());
                        }
                    });
                    
                    if(status.get() && serial){
                        //Go into Pause mode:
                        worker.pause(true);
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                fireupdateGUI();    
                            }
                        });
                        dopause();
                    }
                    
                    if(cmd.getType() == CNCCommand.Type.ENDPROGRAM && cmds[cmds.length - 1] != cmd)
                    {
                        final ObjectProxy<Boolean> h = new ObjectProxy<>();
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                h.set(JOptionPane.showConfirmDialog(JPanelCNCMilling.this, "End Command before end of Program! Continue?") == JOptionPane.YES_OPTION);
                            }
                        });
                        if(!h.get())
                        {
                            return null;
                        }
                    }
                    
                    if(!serial)
                    {
                        export.println(";" + cmd.toString());
                    }
                    
                    for(String execute:cmd.execute(t,jCBAutoLeveling.isSelected(),i == 1))
                    {
                        while(true)
                        {
                            if(this.isCancelled())
                            {
                                return null;
                            }

                            while(Communication.isBussy())
                            {
                                if(this.isCancelled())
                                {
                                    return null;
                                }
                                try
                                {
                                    if(serial)
                                    {
                                        Thread.sleep(1);
                                    }
                                    dopause();
                                }
                                catch(InterruptedException ex)
                                {
                                    return null;
                                }

                                if(!serial)
                                {
                                    break;
                                }
                            }

                            if(serial){
                                try{
                                    Communication.send(execute);
                                }
                                catch(ComInterruptException ex){
                                    continue;
                                }
                                break;
                            }
                            else{
                                export.println(execute);
                                break;
                            }
                        }
                    }
                    
                }

                if(!serial)
                {
                    export.close();
                }
                
                return null;
            }

            @Override
            protected void done(Object rvalue, Exception ex, boolean canceled) {

                if(ex != null)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(JPanelCNCMilling.this, "Error during Milling (" + ex.toString() + ")");
                }
                
                fireupdateGUI();

            }

            @Override
            protected void process(List<CNCCommand> chunks) {
                CNCCommand cmd = chunks.get(chunks.size() - 1);

                if(jCBScroll.isSelected())
                {
                    layers.blocknextIndexSet();
                    jLCNCCommands.setSelectedValue(cmd,false); 
                    
                    int index = jLCNCCommands.getSelectedIndex();
                    layers.setSelectedRange(0, index);                
                
                    //Get cmd centerd:
                    int elementsvisual = jLCNCCommands.getLastVisibleIndex() - jLCNCCommands.getFirstVisibleIndex();
                    if (index == -1)
                    {
                        index = 0;
                    }
                    int sIndex = index - elementsvisual / 2;
                    if(sIndex < 0)
                    {
                        sIndex = 0;
                    }
                    int eIndex = sIndex + elementsvisual;
                    if(eIndex > jLCNCCommands.getModel().getSize())
                    {
                        eIndex = jLCNCCommands.getModel().getSize()- 1;
                    }
                    jLCNCCommands.scrollRectToVisible(jLCNCCommands.getCellBounds(sIndex, eIndex));
                }
            }

        };

        worker.execute();

        fireupdateGUI();
    }
    
    
    private void jBMillingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBMillingActionPerformed
        executeCNC(0);
    }//GEN-LAST:event_jBMillingActionPerformed
    
    
    void sendCNCCommands(CNCCommand[] commands) {
        Iterator<CNCCommand> i= Arrays.asList(commands).iterator();
        processCNCCommands(() -> i.hasNext()?i.next():null,commands.length);
    }
 
     
    private void jLoadFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jLoadFileActionPerformed
        JFileChooser fc = DatabaseV2.getFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        
        if(fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)
        {
            return;
        }

        final File f = fc.getSelectedFile();

        if(!f.canRead())
        {
            JOptionPane.showMessageDialog(this, "File cannot be read!");
            return;
        }
        
        worker = new PMySwingWorker<ArrayList<CNCCommand>, CNCCommand>() 
        {
            @Override
            protected ArrayList<CNCCommand> doInBackground() throws Exception {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) 
                {
                    ArrayList<CNCCommand> list = new ArrayList<>();

                    //read all elements:
                    String l;
                    while((l=br.readLine())!=null){
                        list.add(new CNCCommand(l));
                        progress(0, "Read File...");
                    }
                    return list;
                }
            }

            @Override
            protected void done(ArrayList<CNCCommand> list, Exception ex, boolean canceled) {
                if(ex!=null){
                    JOptionPane.showMessageDialog(JPanelCNCMilling.this, ex.getMessage());
                }
                Iterator<CNCCommand> i= list.iterator();
                processCNCCommands(() ->i.hasNext()?i.next():null,list.size());
            }
            
        };
        worker.execute();
        fireupdateGUI();
    }//GEN-LAST:event_jLoadFileActionPerformed

    
    private void processCNCCommands(CNCCommand.CNCComandSource source,int length){
        cncLoadedFile   = false;
        layers          = new PrintableLayers();
        jLCNCCommands.setModel(new DefaultComboBoxModel()); //Clear Listbox
        jCBPerview.setModel(new DefaultComboBoxModel()); //Clear Layers

        worker = new PMySwingWorker<String, CNCCommand>() 
        {

            DefaultListModel<CNCCommand> model = new DefaultListModel<>();
            long secounds = 0;

            @Override
            protected String doInBackground() throws Exception {
                CNCCommand.Calchelper c = new CNCCommand.Calchelper();
                PrintableLayers layer = new PrintableLayers();
                LinkedList<CNCCommand> cmds = new LinkedList<>();

                CNCCommand command;
                int warnings = 0;
                int errors  = 0;
                long count = 0;
                
                CNCCommand start = CNCCommand.getStartCommand();
                start.calcCommand(c);
                cmds.add(start);
                
                while((command= source.getcommand() ) != null && !this.isCancelled())
                {
                    setProgress((int)(100 * count / length),""+(int)(100 * count / length) + "%");
                    count++;
                    CNCCommand.State t = command.calcCommand(c);

                    layer.processMoves(cmds.size(), command.getMoves());

                    if(t == CNCCommand.State.WARNING)
                    {
                        warnings++;
                    }

                    if(t == CNCCommand.State.ERROR)
                    {
                        errors++;
                    }

                    cmds.add(command);

                    dopause();
                }

                for(CNCCommand cmd:cmds)
                {
                    model.addElement(cmd);
                }
                JPanelCNCMilling.this.layers=layer;
                secounds=(long)c.seconds;
                return "File loaded with " + warnings + " Warnings and " + errors + " Errors!";
            }

            @Override
            protected void done(String rvalue, Exception ex, boolean canceled) {
                String message;

                if(canceled)
                {
                    message = "Canceled!";
                }
                else if(ex != null)
                {
                    message ="Error loading File! (" + ex.getMessage() + ")";
                    Logger.getLogger(JPanelCNCMilling.class.getName()).log(Level.SEVERE, null, ex);
                }
                else
                {
                    message      = rvalue;
                    cncLoadedFile = true;
                    jPBar.setString("~" + Tools.formatDuration(secounds));
                    maxTime     = secounds;
                }
                jLCNCCommands.setModel(model);
                jCBPerview.setModel(new DefaultComboBoxModel(layers.getLayers(positioningMove[2].getdsave()))); //Clear Layers

                jSZoom.setValue(10);
                viewmove    = new Point();

                JOptionPane.showMessageDialog(JPanelCNCMilling.this, message);

                fireupdateGUI();
            }

        };

        worker.execute();

        fireupdateGUI();        
    }
    
    
    
    private void jPPaintComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPPaintComponentResized
        painter.trigger();
    }//GEN-LAST:event_jPPaintComponentResized

    private void jBOptimiseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBOptimiseActionPerformed
        final CNCCommand[] incmds = new CNCCommand[jLCNCCommands.getModel().getSize()];
        ((DefaultListModel<CNCCommand>)jLCNCCommands.getModel()).copyInto(incmds);

        worker= new PMySwingWorker<String,Object>() {
            DefaultListModel<CNCCommand> model = new DefaultListModel<>();
            private long secounds;
            
            @Override
            protected String doInBackground() throws Exception {
                CNCCommand.Optimiser o = new CNCCommand.Optimiser(new CNCCommand.Optimiser.IProgress() {
                    @Override
                    public void publish(String message, int progess) throws MyException {
                        setProgress(progess,message);
                        try {
                            dopause();
                        } catch (InterruptedException ex) {
                            throw new MyException("Interruped");
                        }
                        if(worker.isCancelled())
                            throw new MyException("Interruped");
                    }
                });
                
                //Not much to do the CNCOpimiser does all the work :-)
                ArrayList<CNCCommand> outcmds = o.execute(new ArrayList<>(Arrays.asList(incmds)));

                //Process new comands
                CNCCommand.Calchelper c = new CNCCommand.Calchelper();
                PrintableLayers layer = new PrintableLayers();
                int warnings    = 0;
                int errors      = 0;
                for(int i = 0;i < outcmds.size();i++)
                {
                    setProgress((int)(100 * i / (double)outcmds.size()),"Recalc");
                            
                    CNCCommand command = outcmds.get(i);
 
                    CNCCommand.State t = command.calcCommand(c);

                    layer.processMoves(i, command.getMoves());

                    if(t == CNCCommand.State.WARNING)
                    {
                        warnings++;
                    }

                    if(t == CNCCommand.State.ERROR)
                    {
                        errors++;
                    }

                    dopause();
                }

                for(CNCCommand cmd:outcmds)
                {
                    model.addElement(cmd);
                }

                JPanelCNCMilling.this.layers    = layer;
                secounds = (long)c.seconds;
                return "Optimised! Saved time: " + Tools.formatDuration(maxTime - secounds) + "! \nCommands now have " + warnings +"  Warnings and " + errors + " Errors!";
            }

            @Override
            protected void done(String rvalue, Exception ex, boolean canceled) {
                String message;

                if(canceled)
                {
                    message = "Canceled!";
                }
                else if(ex != null)
                {
                    message = "Opimisation failed: " + ex.getMessage();
                    if(ex instanceof MyException && ((MyException)ex).getO() instanceof CNCCommand)
                    {
                        jLCNCCommands.setSelectedValue(((MyException)ex).getO(), canceled);
                    }
                    Logger.getLogger(JPanelCNCMilling.class.getName()).log(Level.SEVERE, null, ex);
                }
                else
                {
                    message = rvalue;
                    jLCNCCommands.setModel(model);
                    jCBPerview.setModel(new DefaultComboBoxModel(layers.getLayers(positioningMove[2].getdsave()))); //Clear Layers
                    jPBar.setString("~" + Tools.formatDuration(secounds));
                    maxTime = secounds;
                    fireupdateGUI();
                }

                JOptionPane.showMessageDialog(JPanelCNCMilling.this, message);

                fireupdateGUI();
            }
        };

        worker.execute();

        fireupdateGUI();

    }//GEN-LAST:event_jBOptimiseActionPerformed

    private void jPPaintMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPPaintMousePressed
        viewmovelast  = viewmove;
        viewmovestart = evt.getPoint();
    }//GEN-LAST:event_jPPaintMousePressed

    private void jPPaintMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPPaintMouseDragged
        Point2D p1 = viewmovetrans.transform(new Point2D.Double(evt.getPoint().getX(), evt.getPoint().getY()),null);        
        Point2D p2 = viewmovetrans.transform(new Point2D.Double(viewmovestart.getX(), viewmovestart.getY()),null);
        viewmove = new Point2D.Double(viewmovelast.getX() + p1.getX()- p2.getX(), viewmovelast.getY() + p1.getY()- p2.getY());
        painter.trigger();
    }//GEN-LAST:event_jPPaintMouseDragged

    private void jMIInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIInfoActionPerformed
        showInfos(jLCNCCommands.getSelectedIndex());
    }//GEN-LAST:event_jMIInfoActionPerformed

    private void jMIStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIStartActionPerformed
        int index = jLCNCCommands.getSelectedIndex();
        if(index == -1)
        {
            JOptionPane.showMessageDialog(this, "No position Selected");
        }
        else
        {
            executeCNC(index);
        }
    }//GEN-LAST:event_jMIStartActionPerformed

    private void jBScaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBScaleActionPerformed
        final CNCCommand[] incmds = new CNCCommand[jLCNCCommands.getModel().getSize()];
        ((DefaultListModel<CNCCommand>)jLCNCCommands.getModel()).copyInto(incmds);

        worker= new PMySwingWorker<String,Object>() {
            DefaultListModel<CNCCommand> model = new DefaultListModel<>();
            private long secounds;
            
            @Override
            protected String doInBackground() throws Exception {
                
                //Scale comands
                ArrayList<CNCCommand> outcmds =new ArrayList<>();
                for(int i = 0;i < incmds.length;i++){
                    setProgress((int)(100 * i / (double)incmds.length),"Scale");
                    outcmds.add(incmds[i].clone(positioningScale[0].getdsave(), positioningScale[1].getdsave()));
                    dopause();
                }
                
                //Recalc Commands
                CNCCommand.Calchelper c = new CNCCommand.Calchelper();
                PrintableLayers layer = new PrintableLayers();
                int warnings    = 0;
                int errors      = 0;
                for(int i = 0;i < outcmds.size();i++)
                {
                    setProgress((int)(100 * i / (double)outcmds.size()),"Recalc");
                            
                    CNCCommand command = outcmds.get(i);
 
                    CNCCommand.State t = command.calcCommand(c);

                    layer.processMoves(i, command.getMoves());

                    if(t == CNCCommand.State.WARNING)
                    {
                        warnings++;
                    }

                    if(t == CNCCommand.State.ERROR)
                    {
                        errors++;
                    }

                    dopause();
                }

                for(CNCCommand cmd:outcmds)
                {
                    model.addElement(cmd);
                }

                JPanelCNCMilling.this.layers    = layer;
                secounds = (long)c.seconds;
                return "";
            }

            @Override
            protected void done(String rvalue, Exception ex, boolean canceled) {
                String message;

                if(ex != null)
                {
                    message = "Scaling failed: " + ex.getMessage();
                    Logger.getLogger(JPanelCNCMilling.class.getName()).log(Level.SEVERE, null, ex);
                    JOptionPane.showMessageDialog(JPanelCNCMilling.this, message);
                }
                else
                {
                    jLCNCCommands.setModel(model);
                    jCBPerview.setModel(new DefaultComboBoxModel(layers.getLayers(positioningMove[2].getdsave()))); //Clear Layers
                    jPBar.setString("~" + Tools.formatDuration(secounds));
                    maxTime = secounds;
                    fireupdateGUI();
                }
                fireupdateGUI();
            }
        };

        worker.execute();

        fireupdateGUI();


    }//GEN-LAST:event_jBScaleActionPerformed

    private void jPPaintMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_jPPaintMouseWheelMoved
        if (evt.getWheelRotation() > 0 )//mouse wheel was rotated up/away from the user
        {
            int iNewValue = jSZoom.getValue() - jSZoom.getMinorTickSpacing()  ;
            if (iNewValue >= jSZoom.getMinimum())
            {
                jSZoom.setValue(iNewValue);
            }
            else
            {
                jSZoom.setValue(0);
            }
        }
        else
        {
            int iNewValue = jSZoom.getValue() + jSZoom.getMinorTickSpacing()  ;
            if (iNewValue <= jSZoom.getMaximum())
            {
                jSZoom.setValue(iNewValue);
            }
            else
            {
                jSZoom.setValue(jSZoom.getMaximum());
            }
        }    

    }//GEN-LAST:event_jPPaintMouseWheelMoved

    private void jMIEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIEditActionPerformed
        int index = jLCNCCommands.getSelectedIndex();
        final CNCCommand[] incmds = new CNCCommand[jLCNCCommands.getModel().getSize()];
        ((DefaultListModel<CNCCommand>)jLCNCCommands.getModel()).copyInto(incmds);

        if(index == -1)
        {
            JOptionPane.showMessageDialog(this, "No position Selected");
            return;
        }
        if(incmds[index].getType()==CNCCommand.Type.STARTCOMMAND)
        {
            JOptionPane.showMessageDialog(this, "Start Command cannot be edited!");
            return;            
        }
        
        //Edit Command
        String cmdedit=(String)JOptionPane.showInputDialog(null, "Edit Command:","Edit",JOptionPane.PLAIN_MESSAGE,null,null,incmds[index].toString());
        if(cmdedit==null)
            return;
        incmds[index]=new CNCCommand(cmdedit);
        
        //Recalc
        worker= new PMySwingWorker<String,Object>() {
            DefaultListModel<CNCCommand> model = new DefaultListModel<>();
            private long secounds;
            
            @Override
            protected String doInBackground() throws Exception {
                
                //Scale comands
                ArrayList<CNCCommand> outcmds =new ArrayList<>();
                for(int i = 0;i < incmds.length;i++){
                    setProgress((int)(100 * i / (double)incmds.length),"Scale");
                    outcmds.add(incmds[i].clone(positioningScale[0].getdsave(), positioningScale[1].getdsave()));
                    dopause();
                }
                
                //Recalc Commands
                CNCCommand.Calchelper c = new CNCCommand.Calchelper();
                PrintableLayers layer = new PrintableLayers();
                int warnings    = 0;
                int errors      = 0;
                for(int i = 0;i < outcmds.size();i++)
                {
                    setProgress((int)(100 * i / (double)outcmds.size()),"Recalc");
                            
                    CNCCommand command = outcmds.get(i);
 
                    CNCCommand.State t = command.calcCommand(c);

                    layer.processMoves(i, command.getMoves());

                    if(t == CNCCommand.State.WARNING)
                    {
                        warnings++;
                    }

                    if(t == CNCCommand.State.ERROR)
                    {
                        errors++;
                    }

                    dopause();
                }

                for(CNCCommand cmd:outcmds)
                {
                    model.addElement(cmd);
                }

                JPanelCNCMilling.this.layers    = layer;
                secounds = (long)c.seconds;
                return "";
            }

            @Override
            protected void done(String rvalue, Exception ex, boolean canceled) {
                String message;

                if(ex != null)
                {
                    message = "Edit failed: " + ex.getMessage();
                    Logger.getLogger(JPanelCNCMilling.class.getName()).log(Level.SEVERE, null, ex);
                    JOptionPane.showMessageDialog(JPanelCNCMilling.this, message);
                }
                else
                {
                    jLCNCCommands.setModel(model);
                    jCBPerview.setModel(new DefaultComboBoxModel(layers.getLayers(positioningMove[2].getdsave()))); //Clear Layers
                    jPBar.setString("~" + Tools.formatDuration(secounds));
                    maxTime = secounds;
                    fireupdateGUI();
                }
                fireupdateGUI();
            }
        };

        worker.execute();

        fireupdateGUI();
    }//GEN-LAST:event_jMIEditActionPerformed

    private void jCBScalleXYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBScalleXYActionPerformed
        jTFscaleY.setEnabled(!jCBScalleXY.isSelected());
        positioningScale[1].set(positioningScale[0].getdsave());
    }//GEN-LAST:event_jCBScalleXYActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBAbrote;
    private javax.swing.JButton jBMilling;
    private javax.swing.JButton jBOptimise;
    private javax.swing.JButton jBPause;
    private javax.swing.JButton jBScale;
    private javax.swing.JCheckBox jCBAutoLeveling;
    private javax.swing.JComboBox jCBPerview;
    private javax.swing.JCheckBox jCBScalleXY;
    private javax.swing.JCheckBox jCBScroll;
    private javax.swing.JCheckBox jCBSingleStep;
    private javax.swing.JCheckBox jCBmirroX;
    private javax.swing.JCheckBox jCBmirroY;
    private javax.swing.JList jLCNCCommands;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JButton jLoadFile;
    private javax.swing.JMenuItem jMIEdit;
    private javax.swing.JMenuItem jMIInfo;
    private javax.swing.JMenuItem jMIStart;
    private javax.swing.JPopupMenu jMPcommand;
    private javax.swing.JProgressBar jPBar;
    private cnc.gcode.controller.JPPaintable jPPaint;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JSlider jSZoom;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTextField jTFmoveX;
    private javax.swing.JTextField jTFmoveY;
    private javax.swing.JTextField jTFmoveZ;
    private javax.swing.JTextField jTFscaleX;
    private javax.swing.JTextField jTFscaleY;
    // End of variables declaration//GEN-END:variables
}
