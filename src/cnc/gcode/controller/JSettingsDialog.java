/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.Component;
import java.awt.GridLayout;
import java.text.ParseException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 *
 * @author patrick
 */
public class JSettingsDialog extends javax.swing.JDialog {

    public static abstract class Setting{
        private String label;
        private Setting(String label){
            this.label=label;
        }

        public String getLabel() {
            return label;
        }
        
        public abstract Component getElement();
        
        public abstract boolean isValid();
        
    }

    public static class STextArea extends Setting{
        JTextArea textArea; 
        JScrollPane scrollArea; 

        public STextArea(String label, String text) {
            super(label);
            textArea      = new JTextArea(text); 
            scrollArea  = new JScrollPane(textArea); 
        }

        @Override
        public Component getElement() {
            return scrollArea;
        }

        @Override
        public boolean isValid() {
            return true;
        }
        
        public String getText(){
            return textArea.getText().trim();
        }
        
    }
    
    public static class STextField extends Setting{
        JTextField jTF;
        public STextField(String label, String value) {
            super(label);
            jTF= new JTextField(value);
        }
        
        public void setValue(String value){
            jTF.setText(value);
        }
        
        public String getText(){
            return jTF.getText();
        }

        public JTextField getjTF() {
            return jTF;
        }

        @Override
        public Component getElement() {
            return jTF;
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }

    public static class SDouble extends STextField{
        NumberFieldManipulator m;
        Double dmin;
        SDouble smin;
        Double dmax;
        SDouble smax;

        public SDouble(String label, double value) {
            this(label,value,1.0);
        }
        
        public SDouble(String label, double value, double delta) {
            super(label, "");
            
            m=new NumberFieldManipulator(super.jTF, (NumberFieldManipulator axis) -> {
                String message=null;
                double v;
                try {
                    v = axis.getd();
                    message=getMessage(v);
                    if(message==null){   
                        m.set(v);
                    }
                } catch (ParseException ex) {
                    message=ex.toString();
                }
                if(message!=null){
                    axis.popUpToolTip(message);
                    axis.setFocus();
                }
            },delta);
            m.set(value);
        }

        public void setDmax(Double dmax) {
            this.dmax = dmax;
        }

        public void setDmin(Double dmin) {
            this.dmin = dmin;
        }

        public void setSmax(SDouble smax) {
            this.smax = smax;
        }

        public void setSmin(SDouble smin) {
            this.smin = smin;
        }
        
        @Override
        public boolean isValid() {  
            try{
                double v=m.getd();
                return getMessage(v)==null;
                
            }
            catch(Exception e){
                return false;
            }
        }
               
        private String getMessage(double v){
            if(dmin!=null && v<dmin){
                return "Must be bigger then "+dmin;
            }
            else if(smin!=null && v<smin.getValue()){
                return "Must be bigger then "+smin.getText();
            }
            else if(dmax!=null && v>dmax){
                return "Must be smaler then "+dmin;
            }
            else if(smax!=null && v>smax.getValue()){
                return "Must be smaler then "+smax.getText();
            }
            else{
                return null;
            }
        }
        
        public double getValue(){
            return m.getdsave();
        }
    }    

    public static class SEnum<E extends  Enum<E>> extends Setting{
        JComboBox<E> jCB;
        public SEnum(String label, E e) {
            super(label);
            jCB= new JComboBox<>();
            jCB.setModel(new DefaultComboBoxModel<>(((Class<E>)e.getClass()).getEnumConstants()));
            jCB.setSelectedItem(e);
        }

        @Override
        public Component getElement() {
            return jCB;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        public E getValue(){
            return (E)jCB.getSelectedItem();
        }
    }
    
    public static boolean showSettingsDialog(String lable,Setting setting){
        return showSettingsDialog(lable, new Setting[]{setting});
    } 

    public static boolean showSettingsDialog(String lable,Setting[] settings){
        return new JSettingsDialog(lable, settings).ok;        
    } 
    
    private final Setting[] settings;
    private boolean ok;
    
    /**
     * Creates new form JSettingsDialog2
     */
    private JSettingsDialog(String lable, Setting[] settings) {
        super();
        this.settings=settings;
        this.setModal(true);
        initComponents();
        this.setTitle(lable);
        jSettingsPanel.setLayout(new GridLayout(0,1));
        for(Setting s:settings){    
            JTextArea description = new JTextArea(s.getLabel());
            description.setEditable(false);
            jSettingsPanel.add(description);
            jSettingsPanel.add(s.getElement());
        }
        this.pack();
        this.setVisible(true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSettingsPanel = new javax.swing.JPanel();
        jOkButton = new javax.swing.JButton();
        jCancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        javax.swing.GroupLayout jSettingsPanelLayout = new javax.swing.GroupLayout(jSettingsPanel);
        jSettingsPanel.setLayout(jSettingsPanelLayout);
        jSettingsPanelLayout.setHorizontalGroup(
            jSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jSettingsPanelLayout.setVerticalGroup(
            jSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 256, Short.MAX_VALUE)
        );

        jOkButton.setText("OK");
        jOkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jOkButtonActionPerformed(evt);
            }
        });

        jCancelButton.setLabel("Cancel");
        jCancelButton.setMaximumSize(new java.awt.Dimension(47, 23));
        jCancelButton.setMinimumSize(new java.awt.Dimension(47, 23));
        jCancelButton.setPreferredSize(new java.awt.Dimension(47, 23));
        jCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addComponent(jOkButton, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 280, Short.MAX_VALUE)
                .addComponent(jCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(34, 34, 34))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(jSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jOkButton)
                    .addComponent(jCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jOkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jOkButtonActionPerformed
        for(Setting s:settings)
            if(s.isValid()==false){
                s.getElement().requestFocus();
                return;        
            }
        ok=true;
        this.setVisible(false);
    }//GEN-LAST:event_jOkButtonActionPerformed

    private void jCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCancelButtonActionPerformed
        this.setVisible(false);
    }//GEN-LAST:event_jCancelButtonActionPerformed

 
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jCancelButton;
    private javax.swing.JButton jOkButton;
    private javax.swing.JPanel jSettingsPanel;
    // End of variables declaration//GEN-END:variables
}
