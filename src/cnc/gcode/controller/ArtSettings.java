/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.Color;
import java.awt.Image;

/**
 *
 * @author patrick
 */
public class ArtSettings {
    public enum EScale{
        AREA_AVERAGING(Image.SCALE_AREA_AVERAGING),
        DEFAULT(Image.SCALE_DEFAULT),
        FAST(Image.SCALE_FAST),
        REPLICATE(Image.SCALE_REPLICATE),
        SMOOTH(Image.SCALE_SMOOTH);

        private final int value;
        private EScale(int value){
            this.value=value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum EPathtype{
        YLINES("Go Y min to Y max then Increase X"),
        XLINES("Go X min to X max then Increase Y"),
        DIAGONAL1("Go Diagonally +45°"),
        DIAGONAL2("Go Diagonally -45°");
        private final String description;
        private EPathtype(String description) {
            this.description=description;
        }

        public String getDescription() {
            return description;
        }
    }

    //Image prosessing
    public int ires=20; //Internal resulution for calculation
    public Color bgc=Color.WHITE; //Background Color
    public boolean fembossing=true; //Filters
    public boolean fedge=false;
    public boolean flow=false;
    public EScale iscale=EScale.SMOOTH; //Scale algorithem

    //Gcode Parameters
    public float bit_size= 1; // Diameter of your bit.
    public float pline=50;    // Line distance in % of bit size
    public float psegment=50; // Sigment length in % of bit size
    public float zmin=0;      // milling deth
    public float zmax=-2;      
    public float zsave=10;    // save moving hight

    public float ftravel=500; //Trefel speed
    public float fmill=50;    //Milling speed

    public EPathtype pathtype=EPathtype.XLINES;   //possible Path algorithems
    public boolean sweep=false;
    public boolean mdirx=false;
    public boolean mdiry=true;
            
    public boolean showDialog(){
        JSettingsDialog.Setting[] settings= new JSettingsDialog.Setting[18];
        settings[0]= new JSettingsDialog.SInteger("Internal resulution for calculation pixel/mm", ires);
        ((JSettingsDialog.SInteger)settings[0]).setImin(0);
        settings[1]= new JSettingsDialog.Scolor("Background Color", bgc);
        settings[2]= new JSettingsDialog.SBoolean("Use Imagefilter embossing", fembossing);
        settings[3]= new JSettingsDialog.SBoolean("Use Imagefilter edge", fedge);
        settings[4]= new JSettingsDialog.SBoolean("Use Imagefilter low", flow);
        settings[5]= new JSettingsDialog.SEnum<>("Scale algorithem", iscale);
        settings[6]= new JSettingsDialog.SDouble("Diameter of your bit", bit_size);
        ((JSettingsDialog.SDouble)settings[6]).setDmin((double)Float.MIN_VALUE);
        settings[7]= new JSettingsDialog.SDouble("Line distance in % of bit size", pline);
        ((JSettingsDialog.SDouble)settings[7]).setDmin((double)Float.MIN_VALUE);
        ((JSettingsDialog.SDouble)settings[7]).setDmax(100.0);
        settings[8]= new JSettingsDialog.SDouble("Sigment length in % of bit size", psegment);
        ((JSettingsDialog.SDouble)settings[8]).setDmin((double)Float.MIN_VALUE);
        ((JSettingsDialog.SDouble)settings[8]).setDmax(100.0);
        settings[9]= new JSettingsDialog.SDouble("Minimal milling deth", zmin);
        settings[10]= new JSettingsDialog.SDouble("Maximal milling deth", zmax);
        settings[11]= new JSettingsDialog.SDouble("Save moving Hight", zsave);
        settings[12]= new JSettingsDialog.SDouble("Trefel speed", ftravel);
        ((JSettingsDialog.SDouble)settings[12]).setDmin((double)Float.MIN_VALUE);
        settings[13]= new JSettingsDialog.SDouble("Milling speed", fmill);
        ((JSettingsDialog.SDouble)settings[13]).setDmin((double)Float.MIN_VALUE);
        settings[14]= new JSettingsDialog.SEnum<>("Possible Path algorithems", pathtype);
        settings[15]= new JSettingsDialog.SBoolean("Sweep lines", sweep);
        settings[16]= new JSettingsDialog.SBoolean("Invert X milling direction", mdirx);
        settings[17]= new JSettingsDialog.SBoolean("Invert Y milling direction", mdiry);
        
        
        if(JSettingsDialog.showSettingsDialog("Art Settings", settings)){
            ires=((JSettingsDialog.SInteger)settings[0]).getValue();
            bgc=((JSettingsDialog.Scolor)settings[1]).getValue();
            fembossing=((JSettingsDialog.SBoolean)settings[2]).getValue();
            fedge=((JSettingsDialog.SBoolean)settings[3]).getValue();
            flow=((JSettingsDialog.SBoolean)settings[4]).getValue();
            iscale=((JSettingsDialog.SEnum<EScale>)settings[5]).getValue();
            bit_size=(float)((JSettingsDialog.SDouble)settings[6]).getValue();
            pline=(float)((JSettingsDialog.SDouble)settings[7]).getValue();
            psegment=(float)((JSettingsDialog.SDouble)settings[8]).getValue();
            zmin=(float)((JSettingsDialog.SDouble)settings[9]).getValue();
            zmax=(float)((JSettingsDialog.SDouble)settings[10]).getValue();
            zsave=(float)((JSettingsDialog.SDouble)settings[11]).getValue();
            ftravel=(float)((JSettingsDialog.SDouble)settings[12]).getValue();
            fmill=(float)((JSettingsDialog.SDouble)settings[13]).getValue();
            pathtype=((JSettingsDialog.SEnum<EPathtype>)settings[14]).getValue();
            sweep=((JSettingsDialog.SBoolean)settings[15]).getValue();
            mdirx=((JSettingsDialog.SBoolean)settings[16]).getValue();
            mdiry=((JSettingsDialog.SBoolean)settings[17]).getValue();
            return true;
        }
        return false;
        
    }

        
    public void fromString(String s){
        try{
            String[] data= s.split("\\|");
            ires=Integer.parseInt(data[0]);
            bgc=new Color(Integer.parseInt(data[1]));
            fembossing=Boolean.parseBoolean(data[2]);
            fedge=Boolean.parseBoolean(data[3]);
            flow=Boolean.parseBoolean(data[4]);
            iscale=EScale.valueOf(data[5]);
            bit_size=Float.parseFloat(data[6]);
            pline=Float.parseFloat(data[7]);
            psegment=Float.parseFloat(data[8]);
            zmin=Float.parseFloat(data[9]);
            zmax=Float.parseFloat(data[10]);
            zsave=Float.parseFloat(data[11]);
            ftravel=Float.parseFloat(data[12]);
            fmill=Float.parseFloat(data[13]);
            pathtype=EPathtype.valueOf(data[14]);
            sweep=Boolean.parseBoolean(data[15]);
            mdirx=Boolean.parseBoolean(data[16]);
            mdiry=Boolean.parseBoolean(data[17]);            
        }
        catch(Exception e){
            e.printStackTrace();
            throw e;
        }
    }    

    @Override
    public String toString() {
        String[] data=new String[18];
        data[0]=String.valueOf(ires);
        data[1]=String.valueOf(bgc.getRGB());
        data[2]=String.valueOf(fembossing);
        data[3]=String.valueOf(fedge);
        data[4]=String.valueOf(flow);
        data[5]=iscale.name();
        data[6]=String.valueOf(bit_size);
        data[7]=String.valueOf(pline);
        data[8]=String.valueOf(psegment);
        data[9]=String.valueOf(zmin);
        data[10]=String.valueOf(zmax);
        data[11]=String.valueOf(zsave);
        data[12]=String.valueOf(ftravel);
        data[13]=String.valueOf(fmill);
        data[14]=pathtype.name();
        data[15]=String.valueOf(sweep);
        data[16]=String.valueOf(mdirx);
        data[17]=String.valueOf(mdiry);
        return String.join("|", data);
    }

}
