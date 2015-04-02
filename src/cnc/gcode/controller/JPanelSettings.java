/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import cnc.gcode.controller.communication.Communication;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.LinkedList;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author patrick
 */
public class JPanelSettings extends javax.swing.JPanel implements IGUIEvent{
    
    public static final String[] homing = {"upper left","upper right","lower left","lower right" }; //0= upper left; 1= upper right; 2= lower left; 3= lower right;

    private IEvent GUIEvent = null;

    /**
     * Creates new form JPanelSettings
     */
    public JPanelSettings() {
        initComponents();
    }
    
    @Override
    public void setGUIEvent(IEvent event)
    {
        GUIEvent = event;
    }

    @Override
    public void updateGUI(boolean serial, boolean isworking)
    {
        jBImport.setEnabled(!isworking);
        //Akt Text
        jLSHomeing.setText(homing[Integer.parseInt(DatabaseV2.HOMING.get())]); //Homeing
        jLSFastFeedrate.setText(DatabaseV2.MAXFEEDRATE.get());
        jLSWorkSpace.setText("");
        for(int i = 0; i < 3;i++ )
        {
            jLSWorkSpace.setText(jLSWorkSpace.getText() + CommandParsing.axesName[i] + " = " + DatabaseV2.getWorkspace(i)+"   ");        
        }
        jLSCNCStart.setText(Tools.convertToMultiline(DatabaseV2.STARTCODE.get()));
        jLSCNCToolChange.setText(Tools.convertToMultiline(DatabaseV2.TOOLCHANGE.get()));
        jLSCNCSpindleON.setText(Tools.convertToMultiline(DatabaseV2.SPINDLEON.get()));
        jLSCNCSpindleOFF.setText(Tools.convertToMultiline(DatabaseV2.SPINDLEOFF.get()));
        jLSCNCG0Feedrate.setText(DatabaseV2.GOFEEDRATE.get());
        jLSCNCToolSize.setText(DatabaseV2.TOOLSIZE.get());
        jLSCNCOptimiserTime.setText(DatabaseV2.OPTIMISATIONTIMEOUT.get());
        jLSALOptions.setText(Tools.convertToMultiline("Zero height: " + DatabaseV2.ALZERO +
                                                      "\nMax depth: " + DatabaseV2.ALMAXPROBDEPTH +
                                                      "\nSafe height: " + DatabaseV2.ALSAVEHEIGHT +
                                                      "\nClearence: " + DatabaseV2.ALCLEARANCE +
                                                       "\nFeedrate: " + DatabaseV2.ALFEEDRATE));
        jLSALDistance.setText(Tools.convertToMultiline("Distance: " + DatabaseV2.ALDISTANCE +       
                                                        "\nMax XY Move Length: " + DatabaseV2.ALMAXMOVELENGTH));
        jLSALStart.setText(Tools.convertToMultiline(DatabaseV2.ALSTARTCODE.get()));
        jLSARC.setText(DatabaseV2.ARCSEGMENTLENGTH.get());
        jLSBacklash.setText("");
        for(int i=0; i<3;i++ )
        {
            jLSBacklash.setText(jLSBacklash.getText() + CommandParsing.axesName[i] + " = " + DatabaseV2.getBacklash(i) + "   ");       
        }
        jLSmodalG1.setText(DatabaseV2.G1MODAL.get().equals("0")?"OFF":"ON");
        jLSmodalG0.setText(DatabaseV2.G0MODAL.get().equals("0")?"OFF":"ON");
        jLSComType.setText(DatabaseV2.COMTYPE.get());
        jLSCBack.setBackground(new Color(Integer.parseInt(DatabaseV2.CBACKGROUND.get())));
        jLSCG0.setBackground(new Color(Integer.parseInt(DatabaseV2.CG0.get())));
        jLSCG1.setBackground(new Color(Integer.parseInt(DatabaseV2.CG1.get())));
    }
    
    private void fireupdateGUI()
    {
        if(GUIEvent == null)
        {
            throw new RuntimeException("GUI EVENT NOT USED!");
        }
        GUIEvent.fired();
    }
    
    
    private void HandleBacklashSettings()
    {
        Double[] values = new Double[3];
        String[] messages = new String[3];

        for(int i = 0; i < 3;i++ )
        {
            messages[i] = "Set the backlash for the " + CommandParsing.axesName[i] + " axis:";
        }

        LinkedList<ISettingFeedback> updatedValues = JSettingsDialog.DisplaySettingPanel("Backlash correction",
                                                                         new DatabaseV2[]{ //value
                                                                                            DatabaseV2.BL0,
                                                                                            DatabaseV2.BL1,
                                                                                            DatabaseV2.BL2
                                                                        },
                                                                        new double[]{ //value
                                                                                        DatabaseV2.getBacklash(0).getsaved(),
                                                                                        DatabaseV2.getBacklash(1).getsaved(),
                                                                                        DatabaseV2.getBacklash(2).getsaved()
                                                                                    },
                                                                        new double[]{ //min 
                                                                                    0,
                                                                                    0,
                                                                                    0
                                                                        },
                                                                        new double[]{ //max
                                                                                        300,
                                                                                        300,
                                                                                        300
                                                                                    },
                                                                        new String[]{ //message
                                                                                    messages[0],
                                                                                    messages[1],
                                                                                    messages[2]
                                                                                });
        if(updatedValues != null)
        {
            for(int i = 0; i < 3;i++ )
            {
                DatabaseV2.getBacklash(i).set(Tools.dtostr(updatedValues.get(i).getSettingValue()));
            }
        }
    }
    
    private void HandleWorkingSpaceSettings()
    {
        LinkedList<ISettingFeedback> updatedValues = JSettingsDialog.DisplaySettingPanel("Workspace size",
                                                                             new DatabaseV2[]{ //value
                                                                                DatabaseV2.WORKSPACE0,
                                                                                DatabaseV2.WORKSPACE1,
                                                                                DatabaseV2.WORKSPACE2
                                                                            },
                                                                            new double[]{ //value
                                                                                            DatabaseV2.getWorkspace(0).getsaved(),
                                                                                            DatabaseV2.getWorkspace(1).getsaved(),
                                                                                            DatabaseV2.getWorkspace(2).getsaved()
                                                                                        },
                                                                            new double[]{ //min 
                                                                                /*ALZERO*/          0,
                                                                                /*ALMAXPROBDEPTH*/  0,
                                                                                /*ALSAVEHEIGHT*/    0
                                                                            },
                                                                            new double[]{ //max
                                                                                            300,
                                                                                            300,
                                                                                            300
                                                                                        },
                                                                            new String[]{ //message
                                                                                        "Set Size for the " + CommandParsing.axesName[0] + " axis",
                                                                                        "Set Size for the " + CommandParsing.axesName[1] + " axis",
                                                                                        "Set Size for the " + CommandParsing.axesName[2] + " axis"
                                                                                    });
            if(updatedValues != null)
            {
                for(int i = 0; i < 3;i++ )
                {
                    DatabaseV2.getWorkspace(i).set(Tools.dtostr(updatedValues.get(i).getSettingValue()));
                }
            }
    }
    
    private void HandleAutoLevelingSettings()
    {
        LinkedList<ISettingFeedback> updatedValues = JSettingsDialog.DisplaySettingPanel("Auto leveling settings",
                                                                            new DatabaseV2[]{ 
                                                                                DatabaseV2.ALZERO,
                                                                                DatabaseV2.ALMAXPROBDEPTH,
                                                                                DatabaseV2.ALSAVEHEIGHT,
                                                                                DatabaseV2.ALCLEARANCE,
                                                                                DatabaseV2.ALFEEDRATE,
                                                                            },
                                                                            new double[]{ //value
                                                                                            DatabaseV2.ALZERO.getsaved(),
                                                                                            DatabaseV2.ALMAXPROBDEPTH.getsaved(),
                                                                                            DatabaseV2.ALSAVEHEIGHT.getsaved(),
                                                                                            DatabaseV2.ALCLEARANCE.getsaved(),
                                                                                            DatabaseV2.ALFEEDRATE.getsaved(),
                                                                                        },
                                                                            new double[]{ //min 
                                                                                /*ALZERO*/          -Double.MAX_VALUE,
                                                                                /*ALMAXPROBDEPTH*/  -1.0,
                                                                                /*ALSAVEHEIGHT*/    0.0-DatabaseV2.WORKSPACE2.getsaved(),                    
                                                                                /*ALCLEARANCE*/     0.0,
                                                                                /*ALFEEDRATE*/      0.0,
                                                                            },
                                                                            new double[]{ //max
                                                                                            /*ALZERO*/          Double.MAX_VALUE,
                                                                                            /*ALMAXPROBDEPTH*/  DatabaseV2.WORKSPACE2.getsaved(),
                                                                                            /*ALSAVEHEIGHT*/    DatabaseV2.WORKSPACE2.getsaved(),
                                                                                            /*ALCLEARANCE*/     DatabaseV2.WORKSPACE2.getsaved(),    
                                                                                            /*ALFEEDRATE*/      Double.MAX_VALUE,
                                                                                        },
                                                                            new String[]{ //message
                                                                                        /*ALZERO*/          "The absolute position where the Autoleveling is correcting. \nSo after level correction, this Z value will have the probed value. (Normally it is 0)",
                                                                                        /*ALMAXPROBDEPTH*/  "How deep the system tries to probe. (You should home your system before Autoleveling.) \nIn absolute position it is \"Zero height\" - this value.",
                                                                                        /*ALSAVEHEIGHT*/    "Absolute height where the CNC can move safely without problems. \nThe first probing will also start from this position!",
                                                                                        /*ALCLEARANCE*/     "The clearance to the object between two probes.",
                                                                                        /*ALFEEDRATE*/      "The feedrate used for the probing",
                                                                                    });
                                                                                            

            
            if(updatedValues != null)
            {
                    DatabaseV2.ALZERO.set(Tools.dtostr(updatedValues.get(0).getSettingValue()));
                    DatabaseV2.ALMAXPROBDEPTH.set(Tools.dtostr(updatedValues.get(1).getSettingValue()));
                    DatabaseV2.ALSAVEHEIGHT.set(Tools.dtostr(updatedValues.get(2).getSettingValue()));
                    DatabaseV2.ALCLEARANCE.set(Tools.dtostr(updatedValues.get(3).getSettingValue()));
                    DatabaseV2.ALFEEDRATE.set(Tools.dtostr(updatedValues.get(4).getSettingValue()));
            }
    }
    
    
    private void HandleAutoLevelingDistanceSettings()
    {
        LinkedList<ISettingFeedback> updatedValues = JSettingsDialog.DisplaySettingPanel("Workspace size",
                                                                             new DatabaseV2[]{ 
                                                                                DatabaseV2.ALDISTANCE,
                                                                                DatabaseV2.ALMAXMOVELENGTH
                                                                            },
                                                                            new double[]{ //value
                                                                                DatabaseV2.ALDISTANCE.getsaved(),
                                                                                DatabaseV2.ALMAXMOVELENGTH.getsaved(),
                                                                            },
                                                                            new double[]{ //min 
                                                                                Double.MIN_VALUE,
                                                                                Double.MIN_VALUE
                                                                            },
                                                                            new double[]{ //max
                                                                                            300,
                                                                                            300
                                                                                        },
                                                                            new String[]{ //message
                                                                                          "The maximum distance between two probs",
                                                                                          "The maximum Length of a XY move before it gets split",
                                                                                        });
            if(updatedValues != null)
            {
                DatabaseV2.ALDISTANCE.set(Tools.dtostr(updatedValues.get(0).getSettingValue()));
                DatabaseV2.ALMAXMOVELENGTH.set(Tools.dtostr(updatedValues.get(1).getSettingValue()));
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

        jLabel2 = new javax.swing.JLabel();
        jBSHoming = new javax.swing.JButton();
        jLSHomeing = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jBSFastFeedrate = new javax.swing.JButton();
        jLSFastFeedrate = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jBSWorkSpace = new javax.swing.JButton();
        jLSWorkSpace = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jBSCNCStart = new javax.swing.JButton();
        jLSCNCStart = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        jBSCNCToolChange = new javax.swing.JButton();
        jLSCNCToolChange = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        jBSCNCSpindleON = new javax.swing.JButton();
        jLSCNCSpindleON = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        jBSCNCSpindleOFF = new javax.swing.JButton();
        jLSCNCSpindleOFF = new javax.swing.JLabel();
        jLabel33 = new javax.swing.JLabel();
        jBSCNCG0Feedrate = new javax.swing.JButton();
        jLSCNCG0Feedrate = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        jBSCNCToolSize = new javax.swing.JButton();
        jLSCNCToolSize = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        jBSCNCOptimiserTime = new javax.swing.JButton();
        jLSCNCOptimiserTime = new javax.swing.JLabel();
        jLabel37 = new javax.swing.JLabel();
        jBSALOptions = new javax.swing.JButton();
        jLSALOptions = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        jBSALDistance = new javax.swing.JButton();
        jLSALDistance = new javax.swing.JLabel();
        jLabel38 = new javax.swing.JLabel();
        jBSALStart = new javax.swing.JButton();
        jLSALStart = new javax.swing.JLabel();
        jLabel40 = new javax.swing.JLabel();
        jBSARC = new javax.swing.JButton();
        jLSARC = new javax.swing.JLabel();
        jLabel42 = new javax.swing.JLabel();
        jBSBacklash = new javax.swing.JButton();
        jLSBacklash = new javax.swing.JLabel();
        jLabel43 = new javax.swing.JLabel();
        jBSmodalG1 = new javax.swing.JButton();
        jLSmodalG1 = new javax.swing.JLabel();
        jLabel45 = new javax.swing.JLabel();
        jBSmodalG0 = new javax.swing.JButton();
        jLSmodalG0 = new javax.swing.JLabel();
        jLabel44 = new javax.swing.JLabel();
        jBSComType = new javax.swing.JButton();
        jLSComType = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        jBSCBack = new javax.swing.JButton();
        jLSCBack = new javax.swing.JLabel();
        jLabel47 = new javax.swing.JLabel();
        jBSCG0 = new javax.swing.JButton();
        jLSCG0 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jBSCG1 = new javax.swing.JButton();
        jLSCG1 = new javax.swing.JLabel();
        jBexport = new javax.swing.JButton();
        jBImport = new javax.swing.JButton();

        jLabel2.setText("Homing:");

        jBSHoming.setText("Change");
        jBSHoming.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSHomeing.setText("Settings Text");

        jLabel19.setText("Fast Move Feedrate:");

        jBSFastFeedrate.setText("Change");
        jBSFastFeedrate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSFastFeedrate.setText("Settings Text");

        jLabel20.setText("Size of Workingspace:");

        jBSWorkSpace.setText("Change");
        jBSWorkSpace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSWorkSpace.setText("Settings Text");

        jLabel21.setText("CNC/StartGCode");

        jBSCNCStart.setText("Change");
        jBSCNCStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSCNCStart.setText("Settings Text");

        jLabel32.setText("CNC/Tool Change:");

        jBSCNCToolChange.setText("Change");
        jBSCNCToolChange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSCNCToolChange.setText("Settings Text");

        jLabel35.setText("CNC/Spindle ON:");

        jBSCNCSpindleON.setText("Change");
        jBSCNCSpindleON.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSCNCSpindleON.setText("Settings Text");

        jLabel36.setText("CNC/Spindle OFF:");

        jBSCNCSpindleOFF.setText("Change");
        jBSCNCSpindleOFF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSCNCSpindleOFF.setText("Settings Text");

        jLabel33.setText("CNC/G0 Feedrate:");

        jBSCNCG0Feedrate.setText("Change");
        jBSCNCG0Feedrate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSCNCG0Feedrate.setText("Settings Text");

        jLabel34.setText("CNC/Paint Tool Size:");

        jBSCNCToolSize.setText("Change");
        jBSCNCToolSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSCNCToolSize.setText("Settings Text");

        jLabel39.setText("CNC/Optimize Time:");

        jBSCNCOptimiserTime.setText("Change");
        jBSCNCOptimiserTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSCNCOptimiserTime.setText("Settings Text");

        jLabel37.setText("Autolevel/Options:");

        jBSALOptions.setText("Change");
        jBSALOptions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSALOptions.setText("Settings Text");

        jLabel41.setText("Autolevel/Distance:");

        jBSALDistance.setText("Change");
        jBSALDistance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSALDistance.setText("Settings Text");

        jLabel38.setText("Autolevel/Start GCode:");

        jBSALStart.setText("Change");
        jBSALStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSALStart.setText("Settings Text");

        jLabel40.setText("ARC/ Max Segment Length:");

        jBSARC.setText("Change");
        jBSARC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSARC.setText("Settings Text");

        jLabel42.setText("Backlash Correction:");

        jBSBacklash.setText("Change");
        jBSBacklash.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSBacklash.setText("Settings Text");

        jLabel43.setText("Allow modal G1:");

        jBSmodalG1.setText("Change");
        jBSmodalG1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSmodalG1.setText("Settings Text");

        jLabel45.setText("Allow modal G0:");

        jBSmodalG0.setText("Change");
        jBSmodalG0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSmodalG0.setText("Settings Text");

        jLabel44.setText("Device connected:");

        jBSComType.setText("Change");
        jBSComType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSComType.setText("Settings Text");

        jLabel46.setText("Background color:");

        jBSCBack.setText("Change");
        jBSCBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSCBack.setText("                   ");
        jLSCBack.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jLSCBack.setOpaque(true);

        jLabel47.setText("G0 color:");

        jBSCG0.setText("Change");
        jBSCG0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSCG0.setText("                   ");
        jLSCG0.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jLSCG0.setOpaque(true);

        jLabel48.setText("G1 color:");

        jBSCG1.setText("Change");
        jBSCG1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSCG1.setText("                   ");
        jLSCG1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jLSCG1.setOpaque(true);

        jBexport.setText("Export");
        jBexport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBexportActionPerformed(evt);
            }
        });

        jBImport.setText("Import");
        jBImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBImportActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel43))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel39)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel19)
                                    .addComponent(jLabel20)
                                    .addComponent(jLabel32)
                                    .addComponent(jLabel35)
                                    .addComponent(jLabel36)
                                    .addComponent(jLabel33)
                                    .addComponent(jLabel34)
                                    .addComponent(jLabel21)
                                    .addComponent(jLabel37)
                                    .addComponent(jLabel38)
                                    .addComponent(jLabel41)
                                    .addComponent(jLabel40)
                                    .addComponent(jLabel42))
                                .addGap(50, 50, 50)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jBSWorkSpace)
                                    .addComponent(jBSFastFeedrate)
                                    .addComponent(jBSHoming)
                                    .addComponent(jBSCNCStart)
                                    .addComponent(jBSCNCToolChange)
                                    .addComponent(jBSCNCSpindleON)
                                    .addComponent(jBSCNCSpindleOFF)
                                    .addComponent(jBSCNCG0Feedrate)
                                    .addComponent(jBSCNCToolSize)
                                    .addComponent(jBSCNCOptimiserTime)
                                    .addComponent(jBSALOptions)
                                    .addComponent(jBSALDistance)
                                    .addComponent(jBSALStart)
                                    .addComponent(jBSARC)
                                    .addComponent(jBSBacklash)
                                    .addComponent(jBSmodalG1)
                                    .addComponent(jBSmodalG0)
                                    .addComponent(jBSComType)
                                    .addComponent(jBSCBack)
                                    .addComponent(jBSCG0)
                                    .addComponent(jBSCG1)))
                            .addComponent(jLabel45)
                            .addComponent(jLabel44)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jBexport)
                                .addGap(18, 18, 18)
                                .addComponent(jBImport))
                            .addComponent(jLabel46)
                            .addComponent(jLabel47)
                            .addComponent(jLabel48))
                        .addGap(50, 50, 50)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLSCBack)
                            .addComponent(jLSHomeing)
                            .addComponent(jLSFastFeedrate)
                            .addComponent(jLSWorkSpace)
                            .addComponent(jLSCNCStart)
                            .addComponent(jLSCNCToolChange)
                            .addComponent(jLSCNCSpindleON)
                            .addComponent(jLSCNCSpindleOFF)
                            .addComponent(jLSCNCG0Feedrate)
                            .addComponent(jLSCNCToolSize)
                            .addComponent(jLSCNCOptimiserTime)
                            .addComponent(jLSALOptions)
                            .addComponent(jLSALDistance)
                            .addComponent(jLSALStart)
                            .addComponent(jLSARC)
                            .addComponent(jLSBacklash)
                            .addComponent(jLSmodalG1)
                            .addComponent(jLSmodalG0)
                            .addComponent(jLSComType)
                            .addComponent(jLSCG0)
                            .addComponent(jLSCG1))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBSHoming)
                    .addComponent(jLabel2)
                    .addComponent(jLSHomeing))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(jBSFastFeedrate)
                    .addComponent(jLSFastFeedrate))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(jBSWorkSpace)
                    .addComponent(jLSWorkSpace))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(jBSCNCStart)
                    .addComponent(jLSCNCStart))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel32)
                    .addComponent(jBSCNCToolChange)
                    .addComponent(jLSCNCToolChange))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel35)
                    .addComponent(jBSCNCSpindleON)
                    .addComponent(jLSCNCSpindleON))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel36)
                    .addComponent(jBSCNCSpindleOFF)
                    .addComponent(jLSCNCSpindleOFF))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel33)
                    .addComponent(jBSCNCG0Feedrate)
                    .addComponent(jLSCNCG0Feedrate))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel34)
                    .addComponent(jBSCNCToolSize)
                    .addComponent(jLSCNCToolSize))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel39)
                    .addComponent(jBSCNCOptimiserTime)
                    .addComponent(jLSCNCOptimiserTime))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel37)
                    .addComponent(jBSALOptions)
                    .addComponent(jLSALOptions))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBSALDistance)
                    .addComponent(jLSALDistance)
                    .addComponent(jLabel41))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel38)
                    .addComponent(jLSALStart)
                    .addComponent(jBSALStart))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel40)
                    .addComponent(jLSARC)
                    .addComponent(jBSARC))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel42)
                    .addComponent(jLSBacklash)
                    .addComponent(jBSBacklash))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel43)
                    .addComponent(jLSmodalG1)
                    .addComponent(jBSmodalG1))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel45)
                    .addComponent(jBSmodalG0)
                    .addComponent(jLSmodalG0))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel44)
                    .addComponent(jBSComType)
                    .addComponent(jLSComType))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel46)
                    .addComponent(jBSCBack)
                    .addComponent(jLSCBack))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel47)
                    .addComponent(jBSCG0)
                    .addComponent(jLSCG0))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel48)
                    .addComponent(jBSCG1)
                    .addComponent(jLSCG1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBexport)
                    .addComponent(jBImport))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jBSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBSettingsActionPerformed

        //HOMING
        if(evt.getSource() == jBSHoming)
        {
            int options = JOptionPane.showOptionDialog(this,
                                                       "Select homing corner",
                                                       "Homing",
                                                       JOptionPane.YES_NO_CANCEL_OPTION,
                                                       JOptionPane.INFORMATION_MESSAGE,
                                                       null,
                                                       homing,
                                                       0);
            if(options != JOptionPane.CLOSED_OPTION)
            {
                DatabaseV2.HOMING.set("" + options);
            }
        }

        //MAXFEEDRATE
        if(evt.getSource() == jBSFastFeedrate)
        {
            Double[] feedrates = Tools.getValues(new String[]{"Set the feedrate for the fast move:"},
                                        new Double[]{DatabaseV2.MAXFEEDRATE.getsaved()},
                                        new Double[]{Double.MAX_VALUE},
                                        new Double[]{0.0});
            if(feedrates != null)
            {
                DatabaseV2.MAXFEEDRATE.set(Tools.dtostr(feedrates[0]));
            }
        }

        //WORKINGSPACE
        if(evt.getSource() == jBSWorkSpace)
        {
            HandleWorkingSpaceSettings();
        }
        
        //StartCode
        if(evt.getSource() == jBSCNCStart)
        {
            JTextArea textArea      = new JTextArea(DatabaseV2.STARTCODE.get()); 
            JScrollPane scrollArea  = new JScrollPane(textArea); 
            scrollArea.setPreferredSize(new Dimension(100, 100));

            if(JOptionPane.showConfirmDialog
                                            (
                                            this,
                                            new Object[]{"Enter the command that will be executed when milling starts:",
                                                        scrollArea},
                                            "Tool change:",
                                            JOptionPane.OK_CANCEL_OPTION
                                            )
                                            == JOptionPane.OK_OPTION)
                DatabaseV2.STARTCODE.set(textArea.getText().trim());
        }

        //Toolchange
        if(evt.getSource() == jBSCNCToolChange)
        {
            JTextArea textArea      = new JTextArea(DatabaseV2.TOOLCHANGE.get()); 
            JScrollPane scrollArea  = new JScrollPane(textArea); 
            scrollArea.setPreferredSize(new Dimension(100, 100));

            if(JOptionPane.showConfirmDialog
                                            (
                                            this,
                                            new Object[]{"Enter the command to change the tool:",
                                                        new JScrollPane(textArea),
                                                        "Hint: '?' will be replaced with the tool number"},
                                            "Tool change:",
                                            JOptionPane.OK_CANCEL_OPTION
                                            )
                                            == JOptionPane.OK_OPTION)
                DatabaseV2.TOOLCHANGE.set(textArea.getText().trim());
        }
        
        //Spindle ON
        if(evt.getSource() == jBSCNCSpindleON)
        {
            JTextArea textArea      = new JTextArea(DatabaseV2.SPINDLEON.get()); 
            JScrollPane scrollArea  = new JScrollPane(textArea); 
            scrollArea.setPreferredSize(new Dimension(100, 100));

            if(JOptionPane.showConfirmDialog
                                            (
                                            this,
                                            new Object[]{"Enter the command to turn the spindle on:",
                                                        scrollArea,
                                                        "Hint: '?' will be replaced with the original command number!"},
                                            "Spindle ON:",
                                            JOptionPane.OK_CANCEL_OPTION
                                            )
                                            == JOptionPane.OK_OPTION)
                DatabaseV2.SPINDLEON.set(textArea.getText().trim());
        }

        //Spindle OFF
        if(evt.getSource() == jBSCNCSpindleOFF)
        {
            JTextArea textArea          = new JTextArea(DatabaseV2.SPINDLEOFF.get()); 
            JScrollPane scrollArea      = new JScrollPane(textArea); 
            scrollArea.setPreferredSize(new Dimension(100, 100));

            if(JOptionPane.showConfirmDialog
                                            (
                                            this,
                                            new Object[]{"Enter the command to turn the spindle off:", scrollArea},
                                            "Spindle OFF:",
                                            JOptionPane.OK_CANCEL_OPTION
                                            )
                                            == JOptionPane.OK_OPTION)
                DatabaseV2.SPINDLEOFF.set(textArea.getText().trim());
        }
        
        
        //G0Feedrate
        if(evt.getSource() == jBSCNCG0Feedrate)
        {
            Double[] d = Tools.getValues(new String[]{"Set the feedrate for the G0 move:"},
                                        new Double[]{DatabaseV2.GOFEEDRATE.getsaved()},
                                        new Double[]{DatabaseV2.MAXFEEDRATE.getsaved()},
                                        new Double[]{0.0});
            if(d != null) 
            {
                DatabaseV2.GOFEEDRATE.set(Tools.dtostr(d[0]));
            }
        }
        
        //Tooldiameter
        if(evt.getSource() == jBSCNCToolSize)
        {
            Double[] d = Tools.getValues(new String[]{"Set the toolsize for CNC milling simulation:"},
                                        new Double[]{DatabaseV2.TOOLSIZE.getsaved()},
                                        new Double[]{Double.MAX_VALUE},
                                        new Double[]{0.0});
            if(d != null) 
            {
                DatabaseV2.TOOLSIZE.set(Tools.dtostr(d[0]));
            }
        }
        
        //Tooldiameter
        if(evt.getSource() == jBSCNCOptimiserTime)
        {
            Double[] d = Tools.getValues(new String[]{"Set the timeout in seconds for optimizing:"},
                                        new Double[]{DatabaseV2.OPTIMISATIONTIMEOUT.getsaved()},
                                        new Double[]{Double.MAX_VALUE},
                                        new Double[]{0.0});
            if(d != null)
            {
                DatabaseV2.OPTIMISATIONTIMEOUT.set(Tools.dtostr(d[0]));
            }
        }
        
        //AL Options
        if(evt.getSource() == jBSALOptions)
        {
            HandleAutoLevelingSettings();
        }

        //AL Distance
        if(evt.getSource() == jBSALDistance)
        {
            HandleAutoLevelingDistanceSettings();
        }
        
        
        //AutoLavel StartCode
        if(evt.getSource( )== jBSALStart)
        {
            JTextArea textArea      = new JTextArea(DatabaseV2.ALSTARTCODE.get()); 
            JScrollPane scrollArea  = new JScrollPane(textArea); 
            scrollArea.setPreferredSize(new Dimension(100, 100));

            if(JOptionPane.showConfirmDialog
                    (
                    this,
                    new Object[]{"Enter the commands that will be executed when autoleveling starts:",
                                scrollArea},
                    "Tool change:",
                    JOptionPane.OK_CANCEL_OPTION
                    )
                    == JOptionPane.OK_OPTION)
                DatabaseV2.ALSTARTCODE.set(textArea.getText().trim());
        }
        
        //ARC
        if(evt.getSource() == jBSARC)
        {
            Double[] d = Tools.getValues(new String[]{"Set the maximum segment length for ARC to linear move conversion \n The lower the value the more communication is needed:"},
                                        new Double[]{DatabaseV2.ARCSEGMENTLENGTH.getsaved()},
                                        new Double[]{Double.MAX_VALUE},
                                        new Double[]{Double.MIN_VALUE});
            if(d != null)
            {
                DatabaseV2.ARCSEGMENTLENGTH.set(Tools.dtostr(d[0]));
            }
        }

        //Backlash
        if(evt.getSource() == jBSBacklash)
        {
            HandleBacklashSettings();
        }

        //Modal G1
        if(evt.getSource() == jBSmodalG1)
        {
            int options = JOptionPane.showOptionDialog(this,
                                                       "Select G1 modal mode:",
                                                       "Modal G1",
                                                       JOptionPane.YES_NO_CANCEL_OPTION,
                                                       JOptionPane.INFORMATION_MESSAGE,
                                                       null,
                                                       new String[] {"OFF", "ON"},
                                                       0);
            if(options != JOptionPane.CLOSED_OPTION)
            {
                DatabaseV2.G1MODAL.set("" + options);
            }
        }

        //Modal G1
        if(evt.getSource() == jBSmodalG0)
        {
            int options = JOptionPane.showOptionDialog(this,
                                                       "Select G0 modal mode:",
                                                       "Modal G0",
                                                       JOptionPane.YES_NO_CANCEL_OPTION,
                                                       JOptionPane.INFORMATION_MESSAGE,
                                                       null,
                                                       new String[] {"OFF", "ON"},
                                                       0);
            if(options != JOptionPane.CLOSED_OPTION)
            {
                DatabaseV2.G0MODAL.set("" + options);
            }
        }
        
        //ComType
        if(evt.getSource() == jBSComType)
        {
            int options = JOptionPane.showOptionDialog(this,
                                                        "Select type of communication",
                                                        "Device Type",
                                                        JOptionPane.YES_NO_CANCEL_OPTION,
                                                        JOptionPane.INFORMATION_MESSAGE,
                                                        null,
                                                        Communication.values(),
                                                        0);
            if(options != JOptionPane.CLOSED_OPTION)
            {
                DatabaseV2.COMTYPE.set(Communication.values()[options].toString());
            }
        }
        
        //Colors
        if(evt.getSource() == jBSCBack)
        {
            Color c=JColorChooser.showDialog(this, "Set the background color for the visualisation", new Color(Integer.parseInt(DatabaseV2.CBACKGROUND.get())));
            if(c!=null)
                DatabaseV2.CBACKGROUND.set(""+c.getRGB());
        }
        if(evt.getSource() == jBSCG0)
        {
            Color c=JColorChooser.showDialog(this, "Set the color for G0", new Color(Integer.parseInt(DatabaseV2.CG0.get())));
            if(c!=null)
                DatabaseV2.CG0.set(""+c.getRGB());
        }
        if(evt.getSource() == jBSCG1)
        {
            Color c=JColorChooser.showDialog(this, "Set the color for G1", new Color(Integer.parseInt(DatabaseV2.CG1.get())));
            if(c!=null)
                DatabaseV2.CG1.set(""+c.getRGB());
        }
        
        fireupdateGUI();
    }//GEN-LAST:event_jBSettingsActionPerformed

    private void jBexportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBexportActionPerformed
        JFileChooser fc = DatabaseV2.getFileChooser();
        fc.setFileFilter(new FileFilter() 
        {
            @Override
            public boolean accept(File f) 
            {
                return f.getName().toLowerCase().endsWith(".ois")||f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Settings files (*.ois)";
            }
        });
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);

        if(fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        
        File f = fc.getSelectedFile();
        if(f.getName().lastIndexOf('.') == -1)
        {
            f = new File(f.getPath()+".ois");
        }

        if(DatabaseV2.save(f) == false)
        {
            JOptionPane.showMessageDialog(this, "Cannot export Settings!");
        }
    }//GEN-LAST:event_jBexportActionPerformed

    private void jBImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBImportActionPerformed
        JFileChooser fc = DatabaseV2.getFileChooser();
        fc.setFileFilter(new FileFilter() 
        {
            @Override
            public boolean accept(File f) 
            {
                return f.getName().toLowerCase().endsWith(".ois")||f.isDirectory();
            }

            @Override
            public String getDescription() 
            {
                return "Settings files (*.ois)";
            }
        });
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);

        if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        
        if(DatabaseV2.load(fc.getSelectedFile()) == false)
        {
            JOptionPane.showMessageDialog(this, "Cannot import settings!");
        }
        
        fireupdateGUI();
        
    }//GEN-LAST:event_jBImportActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBImport;
    private javax.swing.JButton jBSALDistance;
    private javax.swing.JButton jBSALOptions;
    private javax.swing.JButton jBSALStart;
    private javax.swing.JButton jBSARC;
    private javax.swing.JButton jBSBacklash;
    private javax.swing.JButton jBSCBack;
    private javax.swing.JButton jBSCG0;
    private javax.swing.JButton jBSCG1;
    private javax.swing.JButton jBSCNCG0Feedrate;
    private javax.swing.JButton jBSCNCOptimiserTime;
    private javax.swing.JButton jBSCNCSpindleOFF;
    private javax.swing.JButton jBSCNCSpindleON;
    private javax.swing.JButton jBSCNCStart;
    private javax.swing.JButton jBSCNCToolChange;
    private javax.swing.JButton jBSCNCToolSize;
    private javax.swing.JButton jBSComType;
    private javax.swing.JButton jBSFastFeedrate;
    private javax.swing.JButton jBSHoming;
    private javax.swing.JButton jBSWorkSpace;
    private javax.swing.JButton jBSmodalG0;
    private javax.swing.JButton jBSmodalG1;
    private javax.swing.JButton jBexport;
    private javax.swing.JLabel jLSALDistance;
    private javax.swing.JLabel jLSALOptions;
    private javax.swing.JLabel jLSALStart;
    private javax.swing.JLabel jLSARC;
    private javax.swing.JLabel jLSBacklash;
    private javax.swing.JLabel jLSCBack;
    private javax.swing.JLabel jLSCG0;
    private javax.swing.JLabel jLSCG1;
    private javax.swing.JLabel jLSCNCG0Feedrate;
    private javax.swing.JLabel jLSCNCOptimiserTime;
    private javax.swing.JLabel jLSCNCSpindleOFF;
    private javax.swing.JLabel jLSCNCSpindleON;
    private javax.swing.JLabel jLSCNCStart;
    private javax.swing.JLabel jLSCNCToolChange;
    private javax.swing.JLabel jLSCNCToolSize;
    private javax.swing.JLabel jLSComType;
    private javax.swing.JLabel jLSFastFeedrate;
    private javax.swing.JLabel jLSHomeing;
    private javax.swing.JLabel jLSWorkSpace;
    private javax.swing.JLabel jLSmodalG0;
    private javax.swing.JLabel jLSmodalG1;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    // End of variables declaration//GEN-END:variables
}
