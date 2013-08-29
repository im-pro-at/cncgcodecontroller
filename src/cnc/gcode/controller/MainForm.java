/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import gnu.io.NRSerialPort;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author patrick
 */
public final class MainForm extends javax.swing.JFrame implements ActionListener, SerialInterface.IEvent{

    private class AxesManipulator
    {
        Object element;
        
        public AxesManipulator(Object element)
        {
            this.element=element;
        }
        
        public void setFocus()
        {
            if(element instanceof JTextField)
                ((JTextField)element).grabFocus();
            else if(element instanceof JComboBox)
                ((JComboBox)element).grabFocus();
            else
                throw new UnsupportedOperationException("Not yet implemented");
        }

        public void set(String text)
        {
            if(element instanceof JTextField)
                ((JTextField)element).setText(text);
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
        
        public boolean isObject(Object element)
        {
            return this.element==element;
        }
        
    }
    
    String[] axesName={"X","Y","Z"};
    
    AxesManipulator[][] axes;
    SerialInterface serial= new SerialInterface(this);
    String lastserialstring="";
    Timer serialtimer;
    boolean pharsnextserial=false;

    
    
 
    /**
     * Creates new form MainForm
     */
    public MainForm() {
        initComponents();
        
        //Init Fileds:
        axes= new AxesManipulator[][]   {
                                        /*0*/    {new AxesManipulator(jTFXa),new AxesManipulator(jTFXd),new AxesManipulator(jTFXn)},
                                        /*1*/    {new AxesManipulator(jTFYa),new AxesManipulator(jTFYd),new AxesManipulator(jTFYn)},
                                        /*2*/    {new AxesManipulator(jTFZa),new AxesManipulator(jTFZd),new AxesManipulator(jCBZn)},
                                        /*3*/    {new AxesManipulator(jCBarcI),new AxesManipulator(jCBarcJ)}, // I,J
                                        /*4*/    {new AxesManipulator(jCBdiameter)}, //Diameter
                                        /*5*/    {new AxesManipulator(jCBfeedrate)} //Feedrate
                                        };
        for(AxesManipulator[] axe:axes)
            for(AxesManipulator field:axe)
                field.set(0.0);
        
        
                
        //Load Database
        if(!Database.getDatabase().load())
            JOptionPane.showMessageDialog(null,"Could not load Settings!");
        
        jCBSpeed.setSelectedIndex(
                Integer.parseInt(
                Database.getDatabase().get("Speed","9")));
        
        //update Settings
        jBSettingsActionPerformed(new ActionEvent(this, 0, "init"));

        //Show Comports avilable
        Set<String> ports=NRSerialPort.getAvailableSerialPorts();
        if(ports.isEmpty())
        {
            //No Ports Found
            jCBPort.setEnabled(false);
            jCBSpeed.setEnabled(false);
            jBConnect.setEnabled(false);
            jLStatus.setText("No Serialport found!");
        }
        else
        {
            jCBPort.setModel(new DefaultComboBoxModel(ports.toArray(new String[0])));
            int index=0;
            for (String port:ports) {
                if(port.equals(Database.getDatabase().get("Port","")))
                {
                    jCBPort.setSelectedIndex(index);
                    break;
                }
                index++;
            }
        }
        serialtimer= new Timer(1000, this);
        serialtimer.setRepeats(true);
        serialtimer.start();
        
        //Update Comport Status
        actionSerialStatusChanged();
        
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource()==serialtimer)
        {
            if(serial.isConnect())
            {
                String input = serial.get();
                ArrayList<String> inputs= new ArrayList<>();
                inputs.add("");
                for(char c:input.toCharArray())
                {
                    if(c=='\n')
                        inputs.add("");
                    else
                        inputs.set(inputs.size()-1, inputs.get(inputs.size()-1)+c);
                }
                
                inputs.set(0,lastserialstring+inputs.get(0));
                lastserialstring= inputs.get(inputs.size()-1);
                
                for(int i=0; i < inputs.size()-1;i++)
                {
                    ((DefaultComboBoxModel<SendListElement>)jLCInOut.getModel()).addElement(new SendListElement(inputs.get(i), SendListElement.EType.IN));
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            jLCInOut.ensureIndexIsVisible(jLCInOut.getModel().getSize()-1);
                        }
                    });
                    if(pharsnextserial)
                    {
                        pharsnextserial=false;
                        String in=inputs.get(i);
                        Double[] values= new Double[3];
                        for(int j=0;j<3;j++)
                        {
                            int pos =in.indexOf(axesName[j]+":");
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
                                axes[j][0].set(values[j]);  
                            JOptionPane.showMessageDialog(this, "Update done!");
                        }
                        else
                        {
                            JOptionPane.showMessageDialog(this, "Error reading Position");
                       }
                    }
                }
            }
        }
    }
    
    private void send(String command)
    {
        if(serial.isConnect())
        {
            serial.send(command + "\n");

            //Add to list 
            ((DefaultComboBoxModel<SendListElement>) jLCInOut.getModel()).addElement(new SendListElement(command, SendListElement.EType.OUT));
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    jLCInOut.ensureIndexIsVisible(jLCInOut.getModel().getSize() - 1);
                }
            });
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

        jTabbedPane2 = new javax.swing.JTabbedPane();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        jBSetPos = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jTFXd = new javax.swing.JTextField();
        jTFYd = new javax.swing.JTextField();
        jPanel6 = new javax.swing.JPanel();
        jTFXn = new javax.swing.JTextField();
        jTFYn = new javax.swing.JTextField();
        jScrollPane4 = new javax.swing.JScrollPane();
        jLSave = new javax.swing.JList();
        jLabel7 = new javax.swing.JLabel();
        jBPosSave = new javax.swing.JButton();
        jBPosLoad = new javax.swing.JButton();
        jBPosRem = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jTFZa = new javax.swing.JTextField();
        jPanel10 = new javax.swing.JPanel();
        jTFZd = new javax.swing.JTextField();
        jBGetPos = new javax.swing.JButton();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jPanel13 = new javax.swing.JPanel();
        jCBdiameter = new javax.swing.JComboBox();
        jPanel14 = new javax.swing.JPanel();
        jBTXfp = new javax.swing.JButton();
        jBTXhp = new javax.swing.JButton();
        jBTXfm = new javax.swing.JButton();
        jBTXhm = new javax.swing.JButton();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        jBTYfp = new javax.swing.JButton();
        jBTYhp = new javax.swing.JButton();
        jBTYfm = new javax.swing.JButton();
        jBTYhm = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jBMove = new javax.swing.JButton();
        jBHoming = new javax.swing.JButton();
        jBPowerON = new javax.swing.JButton();
        jBPowerOFF = new javax.swing.JButton();
        jPanel15 = new javax.swing.JPanel();
        jCBarc = new javax.swing.JCheckBox();
        jLabel25 = new javax.swing.JLabel();
        jCBarcCC = new javax.swing.JCheckBox();
        jLabel26 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jCBFastMode = new javax.swing.JCheckBox();
        jCBarcI = new javax.swing.JComboBox();
        jCBarcJ = new javax.swing.JComboBox();
        jCBfeedrate = new javax.swing.JComboBox();
        jPanel16 = new javax.swing.JPanel();
        jTFXa = new javax.swing.JTextField();
        jTFYa = new javax.swing.JTextField();
        jPanel5 = new javax.swing.JPanel();
        jCBZn = new javax.swing.JComboBox();
        jPPaint = new cnc.gcode.controller.JPPaintable();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jLCInOut = new javax.swing.JList();
        jTFSend = new javax.swing.JTextField();
        jBSend = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel4 = new javax.swing.JPanel();
        jBSHomeing = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLSHomeing = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jBSFastFeedrate = new javax.swing.JButton();
        jLSFastFeedrate = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jBSWorkSpace = new javax.swing.JButton();
        jLSWorkSpace = new javax.swing.JLabel();
        jBConnect = new javax.swing.JButton();
        jLStatus = new javax.swing.JLabel();
        jCBPort = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        jCBSpeed = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("CNC-GCode-Controller");
        setName("jframe"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jSplitPane1.setDividerLocation(300);

        jBSetPos.setText("Set Position");
        jBSetPos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSetPosActionPerformed(evt);
            }
        });

        jPanel3.setToolTipText("");
        jPanel3.setName(""); // NOI18N

        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel8.setText("X");

        jLabel9.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel9.setText("Y");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel8)
            .addComponent(jLabel9)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel9)
                .addGap(28, 28, 28))
        );

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Delta"));
        jPanel7.setToolTipText("");
        jPanel7.setName(""); // NOI18N

        jTFXd.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                axesLostFocus(evt);
            }
        });

        jTFYd.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                axesLostFocus(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTFXd, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jTFYd, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addComponent(jTFXd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTFYd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("New Position"));
        jPanel6.setToolTipText("");
        jPanel6.setName(""); // NOI18N

        jTFXn.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                axesLostFocus(evt);
            }
        });

        jTFYn.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                axesLostFocus(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTFXn, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jTFYn, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jTFXn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTFYn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jScrollPane4.setViewportView(jLSave);

        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel7.setText("Save:");

        jBPosSave.setText("Save Akt Position");

        jBPosLoad.setText("Load Position");

        jBPosRem.setText("Remove Position");

        jLabel11.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel11.setText("Z:");

        jPanel8.setToolTipText("");
        jPanel8.setName(""); // NOI18N

        jLabel12.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel12.setText("Z");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(jLabel12)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel8Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jLabel12)
                .addGap(56, 56, 56))
        );

        jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder("Akt Position"));
        jPanel9.setToolTipText("");
        jPanel9.setName(""); // NOI18N

        jTFZa.setEnabled(false);
        jTFZa.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                axesLostFocus(evt);
            }
        });

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTFZa, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTFZa, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder("Delta"));
        jPanel10.setToolTipText("");
        jPanel10.setName(""); // NOI18N

        jTFZd.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                axesLostFocus(evt);
            }
        });

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTFZd, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTFZd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        jBGetPos.setText("Get Position");
        jBGetPos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBGetPosActionPerformed(evt);
            }
        });

        jLabel13.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel13.setText("X,Y:");

        jLabel14.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel14.setText("Tool:");

        jPanel13.setBorder(javax.swing.BorderFactory.createTitledBorder("Diameter"));
        jPanel13.setToolTipText("");
        jPanel13.setName(""); // NOI18N

        jCBdiameter.setEditable(true);
        jCBdiameter.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                axesLostFocus(evt);
            }
        });

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jCBdiameter, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jCBdiameter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel14.setBorder(javax.swing.BorderFactory.createTitledBorder("X"));
        jPanel14.setToolTipText("");
        jPanel14.setName(""); // NOI18N

        jBTXfp.setText("+");
        jBTXfp.setPreferredSize(new java.awt.Dimension(6, 23));

        jBTXhp.setText("+");
        jBTXhp.setPreferredSize(new java.awt.Dimension(6, 23));

        jBTXfm.setText("-");
        jBTXfm.setPreferredSize(new java.awt.Dimension(6, 23));

        jBTXhm.setText("-");
        jBTXhm.setPreferredSize(new java.awt.Dimension(6, 23));

        jLabel16.setText("1");

        jLabel17.setText("1/2");

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel14Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jLabel16)
                        .addGap(6, 6, 6)
                        .addComponent(jBTXfp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel14Layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(jLabel17)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBTXhp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jBTXfm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jBTXhm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBTXfp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBTXfm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBTXhp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBTXhm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17)))
        );

        jPanel12.setBorder(javax.swing.BorderFactory.createTitledBorder("Y"));
        jPanel12.setToolTipText("");
        jPanel12.setName(""); // NOI18N

        jBTYfp.setText("+");
        jBTYfp.setPreferredSize(new java.awt.Dimension(6, 23));

        jBTYhp.setText("+");
        jBTYhp.setPreferredSize(new java.awt.Dimension(6, 23));

        jBTYfm.setText("-");
        jBTYfm.setPreferredSize(new java.awt.Dimension(6, 23));

        jBTYhm.setText("-");
        jBTYhm.setPreferredSize(new java.awt.Dimension(6, 23));

        jLabel10.setText("1");

        jLabel15.setText("1/2");

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel12Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jLabel10)
                        .addGap(6, 6, 6)
                        .addComponent(jBTYfp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel12Layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(jLabel15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBTYhp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jBTYfm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jBTYhm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBTYfp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBTYfm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBTYhp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBTYhm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15)))
        );

        jLabel18.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel18.setText("Global:");

        jLabel21.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel21.setText("Move:");

        jBMove.setFont(new java.awt.Font("Tahoma", 1, 48)); // NOI18N
        jBMove.setText("Move");

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

        jPanel15.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jCBarc.setText("ARC");

        jLabel25.setText("I:");

        jCBarcCC.setText("cc");

        jLabel26.setText("J:");

        jLabel22.setText("Feedrate:");

        jCBFastMode.setText("Fast Moves");

        jCBarcI.setEditable(true);
        jCBarcI.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                axesLostFocus(evt);
            }
        });

        jCBarcJ.setEditable(true);
        jCBarcJ.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                axesLostFocus(evt);
            }
        });

        jCBfeedrate.setEditable(true);
        jCBfeedrate.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                axesLostFocus(evt);
            }
        });

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel15Layout.createSequentialGroup()
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
                        .addComponent(jCBarcCC)
                        .addGap(0, 0, 0))
                    .addGroup(jPanel15Layout.createSequentialGroup()
                        .addComponent(jLabel22)
                        .addGap(18, 18, 18)
                        .addComponent(jCBfeedrate, 0, 1, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jCBFastMode))))
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCBarc)
                    .addComponent(jLabel25)
                    .addComponent(jLabel26)
                    .addComponent(jCBarcCC)
                    .addComponent(jCBarcI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCBarcJ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(jCBFastMode)
                    .addComponent(jCBfeedrate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel16.setBorder(javax.swing.BorderFactory.createTitledBorder("Akt Position"));

        jTFXa.setEditable(false);
        jTFXa.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                axesLostFocus(evt);
            }
        });

        jTFYa.setEditable(false);
        jTFYa.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                axesLostFocus(evt);
            }
        });

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTFXa)
            .addComponent(jTFYa)
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addComponent(jTFXa, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTFYa, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("New Position"));

        jCBZn.setEditable(true);
        jCBZn.setMinimumSize(new java.awt.Dimension(6, 20));
        jCBZn.setPreferredSize(new java.awt.Dimension(6, 20));
        jCBZn.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                axesLostFocus(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jCBZn, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jCBZn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

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
                        .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                        .addComponent(jBHoming, javax.swing.GroupLayout.DEFAULT_SIZE, 92, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBPowerON, javax.swing.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBPowerOFF, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBSetPos, javax.swing.GroupLayout.DEFAULT_SIZE, 103, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBGetPos, javax.swing.GroupLayout.DEFAULT_SIZE, 103, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jPanel16, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel13)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jBPosSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBPosLoad)
                        .addGap(7, 7, 7)
                        .addComponent(jBPosRem))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel8, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 7, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jLabel14))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 14, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jBMove, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jLabel21))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jScrollPane2.setViewportView(jPanel1);

        jSplitPane1.setRightComponent(jScrollPane2);

        javax.swing.GroupLayout jPPaintLayout = new javax.swing.GroupLayout(jPPaint);
        jPPaint.setLayout(jPPaintLayout);
        jPPaintLayout.setHorizontalGroup(
            jPPaintLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 299, Short.MAX_VALUE)
        );
        jPPaintLayout.setVerticalGroup(
            jPPaintLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 498, Short.MAX_VALUE)
        );

        jSplitPane1.setLeftComponent(jPPaint);

        jTabbedPane2.addTab("Control", jSplitPane1);

        jLCInOut.setModel(new javax.swing.DefaultComboBoxModel(new SendListElement[0]));
        jLCInOut.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jLCInOutValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(jLCInOut);

        jTFSend.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTFSendKeyReleased(evt);
            }
        });

        jBSend.setText("Send");
        jBSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSendActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 949, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jTFSend)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBSend))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 471, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTFSend, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBSend)))
        );

        jTabbedPane2.addTab("Communication", jPanel2);

        jBSHomeing.setText("Change");
        jBSHomeing.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLabel2.setText("Homeing:");

        jLSHomeing.setText("Settings Text");

        jLabel19.setText("Fast Move Feedrate:");

        jBSFastFeedrate.setText("Change");
        jBSFastFeedrate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSFastFeedrate.setText("Settings Text");

        jLabel20.setText("Size of Workingspace");

        jBSWorkSpace.setText("Change");
        jBSWorkSpace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBSettingsActionPerformed(evt);
            }
        });

        jLSWorkSpace.setText("Settings Text");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(119, 119, 119)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jBSHomeing)
                            .addComponent(jBSFastFeedrate))
                        .addGap(44, 44, 44)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLSFastFeedrate)
                            .addComponent(jLSHomeing)))
                    .addComponent(jLabel19)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(164, 164, 164)
                        .addComponent(jBSWorkSpace)
                        .addGap(44, 44, 44)
                        .addComponent(jLSWorkSpace))
                    .addComponent(jLabel20))
                .addContainerGap(661, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBSHomeing)
                    .addComponent(jLabel2)
                    .addComponent(jLSHomeing))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel19)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jBSFastFeedrate)
                        .addComponent(jLSFastFeedrate)))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel20)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jBSWorkSpace)
                        .addComponent(jLSWorkSpace)))
                .addContainerGap(499, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(jPanel4);

        jTabbedPane2.addTab("Settings", jScrollPane1);

        jBConnect.setText("Connect");
        jBConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBConnectActionPerformed(evt);
            }
        });

        jLStatus.setText("Not connected");

        jLabel6.setText("@");

        jCBSpeed.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "2400", "4800", "9600", "14400", "19200", "28800", "38400", "57600", "76800", "115200", "230400" }));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jCBPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCBSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBConnect)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jTabbedPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 954, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 528, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBConnect)
                    .addComponent(jLStatus)
                    .addComponent(jCBPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(jCBSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        //close Connection
        if(serial.isConnect())
            serial.disconnect();
        
        //Save Database
        if(!Database.getDatabase().save())
            JOptionPane.showMessageDialog(this,"Could not Save Settings!");
        
        System.exit(0);
    }//GEN-LAST:event_formWindowClosing

    @Override
    public void actionSerialStatusChanged() {
        //Change Status 
        if(serial.isConnect())
        {
            jCBPort.setEnabled(false);
            jCBSpeed.setEnabled(false);
            jBConnect.setText("Disconnect");
            jBSend.setEnabled(true);
            jBHoming.setEnabled(true);
            jBPowerON.setEnabled(true);
            jBPowerOFF.setEnabled(true);
            jBSetPos.setEnabled(true);
            jBGetPos.setEnabled(true);
            jBMove.setEnabled(true);
        }
        else
        {
            lastserialstring="";
            jCBPort.setEnabled(true);
            jCBSpeed.setEnabled(true);            
            jBConnect.setText("Connect");
            jBSend.setEnabled(false);
            jBHoming.setEnabled(false);
            jBPowerON.setEnabled(false);
            jBPowerOFF.setEnabled(false);
            jBSetPos.setEnabled(false);
            jBGetPos.setEnabled(false);
            jBMove.setEnabled(false);
        }
        
        jLStatus.setText(serial.getStatus());
    }

    
    
    private void jBConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBConnectActionPerformed
        if(serial.isConnect())
        {
            serial.disconnect();
        }
        else
        {
            serial.connect((String)jCBPort.getModel().getSelectedItem(), Integer.parseInt((String)jCBSpeed.getSelectedItem()));
            //Save config
            Database.getDatabase().set("Port", (String)jCBPort.getModel().getSelectedItem());
            Database.getDatabase().set("Speed", ((Integer)jCBSpeed.getSelectedIndex()).toString());
            
            send("G90");
        }
        
        actionSerialStatusChanged();
    }//GEN-LAST:event_jBConnectActionPerformed

    private void jBSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBSendActionPerformed
        if(!jTFSend.getText().equals(""))
        {
            send(jTFSend.getText());
        }
    }//GEN-LAST:event_jBSendActionPerformed

    private void jTFSendKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTFSendKeyReleased
        if(evt.getKeyCode()== KeyEvent.VK_ENTER)
            jBSendActionPerformed(new ActionEvent(evt.getSource(),evt.getID(),evt.toString()));
    }//GEN-LAST:event_jTFSendKeyReleased

    private void jLCInOutValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jLCInOutValueChanged
        if(jLCInOut.getSelectedIndex()!=-1)
            jTFSend.setText(((SendListElement)jLCInOut.getModel().getElementAt(jLCInOut.getSelectedIndex())).getText());
    }//GEN-LAST:event_jLCInOutValueChanged

    private void jBSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBSettingsActionPerformed
        String[] homeing= {"upper left","upper right","lower left","lower right" }; //0= upper left; 1= upper right; 2= lower left; 3= lower right;
        
        //HOMEING
        if(evt.getSource()==jBSHomeing)
        {
           int options= JOptionPane.showOptionDialog(this, "Select homeing corner", "Homeing", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, homeing,0); 
           if(options!=JOptionPane.CLOSED_OPTION)
               Database.getDatabase().set("HOMEING", ""+options);
        }
        
        //MAXFEEDRATE
        if(evt.getSource()==jBSFastFeedrate)
        {
            Double[] d=Tools.getValues(new String[]{"Set the Feetrate for the fast move:"}, new Double[]{Tools.strtodsave(Database.getDatabase().get("MAXFEEDRATE", Tools.dtostr(600.0)))}, new Double[]{Double.MAX_VALUE}, new Double[]{0.0});

            if(d!= null)
                Database.getDatabase().set("MAXFEEDRATE", Tools.dtostr(d[0]));
        }
        
        //WORKINGSPACE
        if(evt.getSource()==jBSWorkSpace)
        {
            Double[] values = new Double[3];
            String[] messages= new String[3];
            
            for(int i=0; i<3;i++ )
            {
                values[i] = Tools.strtodsave(Database.getDatabase().get("WORKSPACE"+i, Tools.dtostr(200.0)));
                messages[i] = "Set Size for the "+axesName[i]+" axis";
            }
            values= Tools.getValues(messages, values, new Double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE}, new Double[]{0.0,0.0,0.0});
            
            if(values!= null)
                for(int i=0; i<3;i++ )
                    Database.getDatabase().set("WORKSPACE"+i, Tools.dtostr(values[i]));
        }
        
        //Akt Text
        jLSHomeing.setText(homeing[Integer.parseInt(Database.getDatabase().get("HOMEING", "0"))]); //Homeing
        jLSFastFeedrate.setText(Database.getDatabase().get("MAXFEEDRATE", Tools.dtostr(600.0)));
        jLSWorkSpace.setText("");
        for(int i=0; i<3;i++ )
            jLSWorkSpace.setText(jLSWorkSpace.getText() +axesName[i]+" = "+ Database.getDatabase().get("WORKSPACE"+i, Tools.dtostr(200.0))+"   ");
        
    }//GEN-LAST:event_jBSettingsActionPerformed

    private void jBHomingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBHomingActionPerformed
        send("G28 X Y Z");
        for(int i=0;i<3;i++)
            axes[i][0].set(0.0);
    }//GEN-LAST:event_jBHomingActionPerformed

    private void jBPowerONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBPowerONActionPerformed
        send("M80");
    }//GEN-LAST:event_jBPowerONActionPerformed

    private void jBPowerOFFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBPowerOFFActionPerformed
        send("M81");
    }//GEN-LAST:event_jBPowerOFFActionPerformed

    private void jBSetPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBSetPosActionPerformed
        Double[] values=new Double[3];
        Double[] max=new Double[3];
        String[] messages= new String[3];
        for(int i=0;i<3;i++)
        {
           messages[i]="Set the Value for the "+axesName[i]+" Axis";
           values[i]= axes[i][0].getdsave();
           max[i]= Tools.strtodsave(Database.getDatabase().get("WORKSPACE"+i, Tools.dtostr(200.0))); 
        }
        
        values = Tools.getValues(messages, values, max, new Double[]{0.0,0.0,0.0});

        if(values!= null)
        {
            String cmd= "G92";
            for(int i=0;i<3;i++)
            {
                axes[i][0].set(values[i]);  
                cmd+=" "+axesName[i]+Tools.dtostr(values[i]);
            }
            send(cmd);
        }
        

    }//GEN-LAST:event_jBSetPosActionPerformed

    private void jBGetPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBGetPosActionPerformed
        pharsnextserial=true;
        send("M114");
    }//GEN-LAST:event_jBGetPosActionPerformed

    private void axesLostFocus(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_axesLostFocus
        //Find Position
        int cat=-1,num=-1;
        for(int i=0;i<axes.length;i++)
            for(int j=0;j<axes[i].length;j++)
                if(axes[i][j].isObject(evt.getSource()))
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
            JOptionPane.showMessageDialog(this, ex.toString());
            axes[cat][num].setFocus();
            return;
        }
        
        //Write back Value
        axes[cat][num].set(value);
        
        //Calc other Fields
        if(cat<=2)
            switch(num)
            {
                case 0: //a
                case 1: //d
                    break;
                case 2: //n
                    break;
            }
        

        //Test Range
        switch(cat)
        {
            case 0: //X
            case 1: //Y
            case 2: //Z
                break;
            case 3: //I,J
                break;
            case 4: //Diameter
                break;
            case 5: //Feedrate
                break;
        }
        
        
        
    }//GEN-LAST:event_axesLostFocus

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBConnect;
    private javax.swing.JButton jBGetPos;
    private javax.swing.JButton jBHoming;
    private javax.swing.JButton jBMove;
    private javax.swing.JButton jBPosLoad;
    private javax.swing.JButton jBPosRem;
    private javax.swing.JButton jBPosSave;
    private javax.swing.JButton jBPowerOFF;
    private javax.swing.JButton jBPowerON;
    private javax.swing.JButton jBSFastFeedrate;
    private javax.swing.JButton jBSHomeing;
    private javax.swing.JButton jBSWorkSpace;
    private javax.swing.JButton jBSend;
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
    private javax.swing.JComboBox jCBPort;
    private javax.swing.JComboBox jCBSpeed;
    private javax.swing.JComboBox jCBZn;
    private javax.swing.JCheckBox jCBarc;
    private javax.swing.JCheckBox jCBarcCC;
    private javax.swing.JComboBox jCBarcI;
    private javax.swing.JComboBox jCBarcJ;
    private javax.swing.JComboBox jCBdiameter;
    private javax.swing.JComboBox jCBfeedrate;
    private javax.swing.JList jLCInOut;
    private javax.swing.JLabel jLSFastFeedrate;
    private javax.swing.JLabel jLSHomeing;
    private javax.swing.JLabel jLSWorkSpace;
    private javax.swing.JList jLSave;
    private javax.swing.JLabel jLStatus;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private cnc.gcode.controller.JPPaintable jPPaint;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTextField jTFSend;
    private javax.swing.JTextField jTFXa;
    private javax.swing.JTextField jTFXd;
    private javax.swing.JTextField jTFXn;
    private javax.swing.JTextField jTFYa;
    private javax.swing.JTextField jTFYd;
    private javax.swing.JTextField jTFYn;
    private javax.swing.JTextField jTFZa;
    private javax.swing.JTextField jTFZd;
    private javax.swing.JTabbedPane jTabbedPane2;
    // End of variables declaration//GEN-END:variables

    

}
