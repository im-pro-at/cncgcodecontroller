/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.CancellationException;
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

    private IEvent GUIEvent=null;
    
    private abstract class PMySwingWorker<R, P> extends MySwingWorker<R, P>
    {

        @Override
        protected final void progress(int progress) {
            jPBar.setValue(progress);
        }
        
    }
    
    
    private NumberFildManipulator[] positioningmove;
    private PMySwingWorker cncworker=null;
    private boolean cncloadedfile=false;
    private PrintableLayers layers= new PrintableLayers();
    private AffineTransform trans=new AffineTransform();
    private long maxTime=0;
    private BufferedImage image;
    private final TriggertSwingWorker<BufferedImage> painter =new TriggertSwingWorker<BufferedImage>() {
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
                
                
            }
            
            @Override
            protected BufferedImage doJob() throws Exception {
                
                //Load Parameter:
                final GetDataSyncedHelper data= new GetDataSyncedHelper();
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        data.jpw=jPPaint.getWidth();
                        data.jph=jPPaint.getHeight();
                        data.zoom=jSZoom.getValue();
                        data.movex=positioningmove[0].getdsave();
                        data.movey=positioningmove[1].getdsave();
                        data.mirrorx=jCBmirroX.isSelected();
                        data.mirrory=jCBmirroY.isSelected();
                        data.index=jCBPerview.getSelectedIndex();
                    }
                });        
                
                if(data.jpw<=0) data.jpw=1;
                if(data.jph<=0) data.jph=1;
                    
                BufferedImage image= new BufferedImage(data.jpw, data.jph, BufferedImage.TYPE_4BYTE_ABGR);
                
                Graphics2D g2=image.createGraphics();
                
                
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                
                //Scalling transforming ...
                //StartCorner
                g2.translate(data.jpw/2, data.jph/2);
                switch(Integer.parseInt(Database.HOMEING.get()))
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
                //Zoom
                double zoom=(10-data.zoom)/10.0;
                g2.scale(zoom, zoom);

                g2.translate(-data.jpw/2, -data.jph/2);
                
                //Display Position
                double ariawidth= Tools.strtodsave(Database.WORKSPACE0.get()); //x
                double ariaheight= Tools.strtodsave(Database.WORKSPACE1.get()); //y
                Rectangle rect=Geometrics.placeRectangle(data.jpw, data.jph, Geometrics.getRatio(ariawidth,ariaheight));
                double scalex=rect.width/ariawidth;
                double scaley=rect.height/ariaheight;
                g2.translate(rect.x, rect.y);
                g2.scale(scalex, scaley);
                
                //Draw base
                g2.setColor(Color.white);
                g2.fill(new Rectangle2D.Double(0, 0, ariawidth, ariaheight));
                
                //Positioning
                g2.translate(data.movex, data.movey);
                g2.scale(data.mirrorx?-1:1, data.mirrory?-1:1);
                
                try {
                    AffineTransform t=g2.getTransform();
                    t.invert();
                    trans=t;
                } catch (NoninvertibleTransformException ex) {
                    trans=new AffineTransform();
                }
                
                layers.paint(g2, data.index); 
                
                return image;
            }

            @Override
            protected void process(BufferedImage chunk) {
                image=chunk;
                
                jPPaint.repaint();
            }
            
        };
    
    
    /**
     * Creates new form JPanelCNCMilling
     */
    public JPanelCNCMilling() {
        initComponents();

        NumberFildManipulator.IAxesEvent numberevent= new NumberFildManipulator.IAxesEvent() {
            @Override
            public void fired(NumberFildManipulator axis) {
                Double value;
                try {
                   value=axis.getd();
                } catch (ParseException ex) {
                    axis.popUpToolTip(ex.toString());
                    axis.setFocus();
                    return;
                }
        
                //Write back Value
                axis.set(value);
                
                painter.trigger();
            }
        };
        
        positioningmove = new NumberFildManipulator[] {new NumberFildManipulator(jTFmoveX, numberevent), new NumberFildManipulator(jTFmoveY, numberevent)};
        
        for(NumberFildManipulator fild:positioningmove)
            fild.set(0.0);
        
        //CNC Milling
        jLCNCCommands.setCellRenderer(new BasicComboBoxRenderer(){

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                if(value instanceof CNCCommand){
                    final CNCCommand message=(CNCCommand)value;
                    if(message.getState()!=CNCCommand.State.NORMAL)
                        setBackground(message.getBColor());
                    else
                        setForeground(message.getFColor());
                }
                
                return this;
            }
            
        });        
        
        
        jPPaint.addPaintEventListener(new JPPaintableListener() {
            @Override
            public void paintComponent(JPPaintableEvent evt) {
                if(image!=null)
                    evt.getGaraphics().drawImage(image, 0, 0, null);
            }
        });
        
    }

    @Override
    public void setGUIEvent(IEvent event)
    {
        GUIEvent=event;
    }

    @Override
    public void updateGUI(boolean serial, boolean isworking, boolean isleveled)
    {
        jCBPerview.setEnabled(cncloadedfile);
        jLoadFile.setEnabled(!isworking);
        jBMilling.setEnabled(!isworking && cncloadedfile && serial);
        jBAbrote.setEnabled(isworking);
        if(!isworking) jPBar.setValue(0);
        if(!cncloadedfile) jPBar.setString(" ");
        jBPause.setEnabled(isworking);
        jBPause.setText((isworking && cncworker.isPuased())?"Resume":"Pause");

        painter.trigger();

    }
    
    private void fireupdateGUI()
    {
        if(GUIEvent==null)
            throw new RuntimeException("GUI EVENT NOT USED!");
        GUIEvent.fired();
    }

    
    public boolean isRunning()
    {
        return cncworker!=null && !cncworker.isDone();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane2 = new javax.swing.JSplitPane();
        jPPaint = new cnc.gcode.controller.JPPaintable();
        jPanel3 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLoadFile = new javax.swing.JButton();
        jLabel23 = new javax.swing.JLabel();
        jBMilling = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        jScrollPane5 = new javax.swing.JScrollPane();
        jLCNCCommands = new javax.swing.JList();
        jPanel1 = new javax.swing.JPanel();
        jPBar = new javax.swing.JProgressBar();
        jBAbrote = new javax.swing.JButton();
        jBPause = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jCBPerview = new javax.swing.JComboBox();
        jLabel9 = new javax.swing.JLabel();
        jSZoom = new javax.swing.JSlider();
        jPanel4 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jTFmoveY = new javax.swing.JTextField();
        jCBmirroY = new javax.swing.JCheckBox();
        jCBmirroX = new javax.swing.JCheckBox();
        jTFmoveX = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();

        jSplitPane2.setDividerLocation(500);
        jSplitPane2.setResizeWeight(0.8);

        jPPaint.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPPaintMouseClicked(evt);
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
            .addGap(0, 499, Short.MAX_VALUE)
        );
        jPPaintLayout.setVerticalGroup(
            jPPaintLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 419, Short.MAX_VALUE)
        );

        jSplitPane2.setLeftComponent(jPPaint);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Steps"));

        jLabel1.setText("1.)");

        jLoadFile.setText("Load File");
        jLoadFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jLoadFileActionPerformed(evt);
            }
        });

        jLabel23.setText("2.)");

        jBMilling.setText("Milling");
        jBMilling.setEnabled(false);
        jBMilling.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBMillingActionPerformed(evt);
            }
        });

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Scroll");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel23))
                .addGap(18, 18, 18)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLoadFile)
                        .addGap(49, 81, Short.MAX_VALUE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jBMilling)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckBox1)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLoadFile))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel23)
                    .addComponent(jBMilling)
                    .addComponent(jCheckBox1))
                .addGap(0, 0, Short.MAX_VALUE))
        );

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
            .addComponent(jPBar, javax.swing.GroupLayout.DEFAULT_SIZE, 169, Short.MAX_VALUE)
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
                .addComponent(jPBar, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBPause)
                    .addComponent(jBAbrote))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Preview"));

        jLabel5.setText("Layer:");

        jCBPerview.setEnabled(false);
        jCBPerview.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCBPerviewItemStateChanged(evt);
            }
        });

        jLabel9.setText("Zoom out:");

        jSZoom.setMaximum(9);
        jSZoom.setSnapToTicks(true);
        jSZoom.setValue(0);
        jSZoom.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSZoomStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCBPerview, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSZoom, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
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
                    .addComponent(jSZoom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
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

        jLabel7.setText("Mirroring:");

        jLabel8.setText("Move:");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addComponent(jLabel7))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel2)
                    .addComponent(jTFmoveX, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCBmirroX))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel6)
                    .addComponent(jTFmoveY, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCBmirroY))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel8)
                    .addComponent(jTFmoveX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTFmoveY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jCBmirroX)
                    .addComponent(jLabel7)
                    .addComponent(jCBmirroY)))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jPanel4, jPanel5});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(11, 11, 11)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jPanel2, jPanel5});

        jPanel3Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jPanel1, jPanel4});

        jSplitPane2.setRightComponent(jPanel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 927, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jSplitPane2))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 421, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jSplitPane2))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jBAbroteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBAbroteActionPerformed

        if(cncworker!=null)
            cncworker.cancel();

        fireupdateGUI();
    }//GEN-LAST:event_jBAbroteActionPerformed

    private void jLCNCCommandsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLCNCCommandsMouseClicked
        if(evt.getClickCount()==2)
        {
            int index=jLCNCCommands.locationToIndex(evt.getPoint());
            if(index!=-1)
            {
                String[] lines=((CNCCommand)jLCNCCommands.getModel().getElementAt(index)).getInfos(new CNCCommand.Transform(positioningmove[0].getdsave(), positioningmove[1].getdsave(), jCBmirroX.isSelected(), jCBmirroY.isSelected())).split("\n");
                
                JList<String> list= new JList<>(lines);
                JScrollPane sp= new JScrollPane(list);
                
                JOptionPane.showMessageDialog(this,sp);
            
            
            }
        }
    }//GEN-LAST:event_jLCNCCommandsMouseClicked

    private void jBPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBPauseActionPerformed
        if(cncworker!=null)
            cncworker.pause(!cncworker.isPuased());

        fireupdateGUI();
    }//GEN-LAST:event_jBPauseActionPerformed

    private void jCBPerviewItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCBPerviewItemStateChanged
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
        Point2D p= trans.transform(new Point2D.Double(evt.getX(), evt.getY()), null);
        jLCNCCommands.setSelectedIndices(layers.getIndexes(p,jCBPerview.getSelectedIndex()));
        jLCNCCommands.ensureIndexIsVisible(jLCNCCommands.getSelectedIndex());
    }//GEN-LAST:event_jPPaintMouseClicked

    private void jBMillingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBMillingActionPerformed
        final CNCCommand[] cmds= new CNCCommand[jLCNCCommands.getModel().getSize()];
        ((DefaultListModel<CNCCommand>)jLCNCCommands.getModel()).copyInto(cmds);

        cncworker= new PMySwingWorker<Object,CNCCommand>() {
            CNCCommand.Transform t= new CNCCommand.Transform(positioningmove[0].getdsave(), positioningmove[1].getdsave(), jCBmirroX.isSelected(), jCBmirroY.isSelected());
            
            class Helper
            {
                boolean status=false;
            }
            
            @Override
            protected Object doInBackground() throws Exception {
                int length=cmds.length;
                if (length==0) length=1;
                for(int i=0;i<cmds.length;i++)
                {
                    CNCCommand cmd=cmds[i];
                    publish(cmd);
                    setProgress(100*i/length);
                    
                    if(cmd.getType()==CNCCommand.Type.ENDPORGRAM && cmds[cmds.length-1]!=cmd)
                    {
                        final Helper h= new Helper();
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                h.status=(JOptionPane.showConfirmDialog(JPanelCNCMilling.this, "End Command before end of Program! Continue?")==JOptionPane.YES_OPTION);
                            }
                        });
                        if(!h.status)
                            return null;
                    }
                    
                    for(String execute:cmd.execute(t))
                    {
                        if(this.isCancelled())
                            return null;

                        while(Communication.getInstance().isbussy())
                        {
                            if(this.isCancelled())
                                return null;
                            try
                            {
                                Thread.sleep(1);
                                dopause();
                            }
                            catch(InterruptedException ex)
                            {
                                return null;
                            }
                        }
                    
                        Communication.getInstance().send(execute);
                    }
                    
                }

                return null;
            }

            @Override
            protected void done(Object rvalue, Exception ex, boolean canceled) {

                if(ex!=null)
                {
                    JOptionPane.showMessageDialog(JPanelCNCMilling.this, "Error during Milling ("+ex.toString()+")");
                }
                
                fireupdateGUI();

            }

            @Override
            protected void process(List<CNCCommand> chunks) {
                CNCCommand cmd=chunks.get(chunks.size()-1);

                jLCNCCommands.setSelectedValue(cmd,false);
 
                int index=jLCNCCommands.getSelectedIndex();
                layers.setSelectedRange(0, index);                
                
                if(jCheckBox1.isSelected())
                {
                    //Get cmd centerd:
                    int elementsvisual=jLCNCCommands.getLastVisibleIndex()-jLCNCCommands.getFirstVisibleIndex();
                    if (index==-1) index=0;
                    int sindex=index-elementsvisual/2;
                    if(sindex<0) sindex=0;
                    int eindex=sindex+elementsvisual;
                    if(eindex>jLCNCCommands.getModel().getSize()) eindex=jLCNCCommands.getModel().getSize()-1;
                    jLCNCCommands.scrollRectToVisible(jLCNCCommands.getCellBounds(sindex, eindex));
                }
                
                //Show new time:
                jPBar.setString("~"+Tools.formatDuration(maxTime-cmd.getSecounds()));
            }

        };

        cncworker.execute();

        fireupdateGUI();
    }//GEN-LAST:event_jBMillingActionPerformed

    private void jLoadFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jLoadFileActionPerformed
        JFileChooser fc= Database.getFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        
        if(fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)
            return;

        final File f= fc.getSelectedFile();

        if(!f.canRead())
        {
            JOptionPane.showMessageDialog(this, "File cannot be read!");
            return;
        }

        cncloadedfile=false;
        layers= new PrintableLayers();
        jLCNCCommands.setModel(new DefaultComboBoxModel()); //Clear Listbox
        jCBPerview.setModel(new DefaultComboBoxModel()); //Clear Layers

        cncworker = new PMySwingWorker<String, CNCCommand>() {

            DefaultListModel<CNCCommand> model=new DefaultListModel<>();
            long secounds=0;

            @Override
            protected String doInBackground() throws Exception {
                CNCCommand.Calchelper c= new CNCCommand.Calchelper();
                PrintableLayers layer= new PrintableLayers();

                String line;
                int warings=0;
                int errors=0;
                long countbytes=0;
                long length= f.length();

                if (length==0) length=1;
                
                model.addElement(CNCCommand.getStartCommand());
                
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {

                    while((line = br.readLine() )!=null && !this.isCancelled())
                    {
                        countbytes+=line.length()+1;
                        setProgress((int)(100*countbytes/length));

                        CNCCommand command= new CNCCommand(line);

                        CNCCommand.State t=command.calcCommand(c);

                        layer.processMoves(model.getSize(), command.getMoves());

                        if(t==CNCCommand.State.WARNING)
                        warings++;
                        if(t==CNCCommand.State.ERROR)
                        errors++;

                        model.addElement(command);

                        dopause();
                    }
                }
                JPanelCNCMilling.this.layers=layer;
                secounds=(long)c.secounds;
                return "File loaded with "+warings+" Warings and "+errors+" Errors!";
            }

            @Override
            protected void done(String rvalue, Exception ex, boolean canceled) {
                String message;

                if(canceled)
                {
                    message="Canceled!";
                }
                else
                {
                    try {
                        if(ex!=null)
                        throw ex;
                        message=rvalue;
                        cncloadedfile=true;
                        jPBar.setString("~"+Tools.formatDuration(secounds));
                        maxTime=secounds;
                        fireupdateGUI();
                    }
                    catch (CancellationException e){
                        message="Canceled!";
                    }
                    catch ( Exception e) {
                        message="Error loading File! ("+ex.getMessage()+")";
                        Logger.getLogger(JPanelCNCMilling.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                jLCNCCommands.setModel(model);
                jCBPerview.setModel(new DefaultComboBoxModel(layers.getLayers())); //Clear Layers

                JOptionPane.showMessageDialog(JPanelCNCMilling.this, message);

                fireupdateGUI();
            }

        };

        cncworker.execute();

        fireupdateGUI();
    }//GEN-LAST:event_jLoadFileActionPerformed

    private void jPPaintComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPPaintComponentResized
        painter.trigger();
    }//GEN-LAST:event_jPPaintComponentResized

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBAbrote;
    private javax.swing.JButton jBMilling;
    private javax.swing.JButton jBPause;
    private javax.swing.JComboBox jCBPerview;
    private javax.swing.JCheckBox jCBmirroX;
    private javax.swing.JCheckBox jCBmirroY;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JList jLCNCCommands;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JButton jLoadFile;
    private javax.swing.JProgressBar jPBar;
    private cnc.gcode.controller.JPPaintable jPPaint;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JSlider jSZoom;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTextField jTFmoveX;
    private javax.swing.JTextField jTFmoveY;
    // End of variables declaration//GEN-END:variables
}
