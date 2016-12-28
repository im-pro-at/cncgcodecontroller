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

    public enum EMode{
        LASER,
        MILLING;
    }
    
    
    
    //Image prosessing
    public int ires=20; //Internal resulution for calculation
    public Color bgc=Color.WHITE; //Background Color
    public boolean fembossing=true; //Filters
    public boolean fedge=false;
    public boolean flow=false;
    public EScale iscale=EScale.SMOOTH; //Scale algorithem

    //Gcode Parameters
    public double bit_size= 1; // Diameter of your bit.
    public double pline=50;    // Line distance in % of bit size
    public double psegment=50; // Sigment length in % of bit size

    public double ftravel=500; //Trefel speed

    public EPathtype pathtype=EPathtype.XLINES;   //possible Path algorithems
    public boolean sweep=false;
    public boolean mdirx=false;
    public boolean mdiry=true;

    public EMode mode= EMode.MILLING;
    
    //Mill spesific
    public double zmin=0;      // milling deth
    public double zmax=-2;      
    public double zignor=-2;      
    public double zsave=10;    // save moving hight
    public double fmill=50;    //Milling speed

    //Laser spesific
    public double zlaser=0;
    public double amin=0;      // milling deth
    public double amax=100;      
    public double aignor=5;    // save moving hight
    public double faon=10000;    //Milling speed
    
    
    public boolean showDialog(){
        JSettingsDialog.Setting[] settings= new JSettingsDialog.Setting[27];
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
        settings[9]= new JSettingsDialog.SDouble("Trefel speed", ftravel);
        ((JSettingsDialog.SDouble)settings[9]).setDmin((double)Float.MIN_VALUE);
        settings[10]= new JSettingsDialog.SEnum<>("Possible Path algorithems", pathtype);
        settings[11]= new JSettingsDialog.SBoolean("Sweep lines", sweep);
        settings[12]= new JSettingsDialog.SBoolean("Invert X milling direction", mdirx);
        settings[13]= new JSettingsDialog.SBoolean("Invert Y milling direction", mdiry);
        
        settings[14]= new JSettingsDialog.SEnum<>("Mode",mode);
        
        settings[15]= new JSettingsDialog.STitel("Milling Mode specific settings:");
        
        settings[16]= new JSettingsDialog.SDouble("Minimal milling deth", zmin);
        settings[17]= new JSettingsDialog.SDouble("Maximal milling deth", zmax);
        settings[18]= new JSettingsDialog.SDouble("Go to save Hight over", zignor);
        settings[19]= new JSettingsDialog.SDouble("Save moving Hight", zsave);
        settings[20]= new JSettingsDialog.SDouble("Milling speed", fmill);
        ((JSettingsDialog.SDouble)settings[19]).setDmin((double)Float.MIN_VALUE);

        settings[21]= new JSettingsDialog.STitel("Laser Mode specific settings:");
        settings[22]= new JSettingsDialog.SDouble("Operating height", zlaser);        
        settings[23]= new JSettingsDialog.SDouble("Minimal A/mm", amin);
        ((JSettingsDialog.SDouble)settings[22]).setDmin(0.0);
        settings[24]= new JSettingsDialog.SDouble("Maximal A/mm", amax);
        ((JSettingsDialog.SDouble)settings[23]).setDmin(0.0);
        settings[25]= new JSettingsDialog.SDouble("Ignor A/mm under", aignor);
        ((JSettingsDialog.SDouble)settings[24]).setDmin(0.0);
        settings[26]= new JSettingsDialog.SDouble("Engrave speed", faon);
        ((JSettingsDialog.SDouble)settings[25]).setDmin((double)Float.MIN_VALUE);
        
        
        
        if(JSettingsDialog.showSettingsDialog("Art Settings", settings)){
            ires=((JSettingsDialog.SInteger)settings[0]).getValue();
            bgc=((JSettingsDialog.Scolor)settings[1]).getValue();
            fembossing=((JSettingsDialog.SBoolean)settings[2]).getValue();
            fedge=((JSettingsDialog.SBoolean)settings[3]).getValue();
            flow=((JSettingsDialog.SBoolean)settings[4]).getValue();
            iscale=((JSettingsDialog.SEnum<EScale>)settings[5]).getValue();
            bit_size=((JSettingsDialog.SDouble)settings[6]).getValue();
            DatabaseV2.TOOLSIZE.set(Tools.dtostr(bit_size));
            pline=((JSettingsDialog.SDouble)settings[7]).getValue();
            psegment=((JSettingsDialog.SDouble)settings[8]).getValue();
            ftravel=((JSettingsDialog.SDouble)settings[9]).getValue();
            pathtype=((JSettingsDialog.SEnum<EPathtype>)settings[10]).getValue();
            sweep=((JSettingsDialog.SBoolean)settings[11]).getValue();
            mdirx=((JSettingsDialog.SBoolean)settings[12]).getValue();
            mdiry=((JSettingsDialog.SBoolean)settings[13]).getValue();
            mode=((JSettingsDialog.SEnum<EMode>)settings[14]).getValue();
            zmin=((JSettingsDialog.SDouble)settings[16]).getValue();
            zmax=((JSettingsDialog.SDouble)settings[17]).getValue();
            zignor=((JSettingsDialog.SDouble)settings[18]).getValue();
            zsave=((JSettingsDialog.SDouble)settings[19]).getValue();
            fmill=((JSettingsDialog.SDouble)settings[20]).getValue();
            zlaser=((JSettingsDialog.SDouble)settings[22]).getValue();
            amin=((JSettingsDialog.SDouble)settings[23]).getValue();
            amax=((JSettingsDialog.SDouble)settings[24]).getValue();
            aignor=((JSettingsDialog.SDouble)settings[25]).getValue();
            faon=((JSettingsDialog.SDouble)settings[26]).getValue();
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
            bit_size=DatabaseV2.TOOLSIZE.getsaved();
            pline=Double.parseDouble(data[6]);
            psegment=Double.parseDouble(data[7]);
            ftravel=Double.parseDouble(data[8]);
            pathtype=EPathtype.valueOf(data[9]);
            sweep=Boolean.parseBoolean(data[10]);
            mdirx=Boolean.parseBoolean(data[11]);
            mdiry=Boolean.parseBoolean(data[12]);
            mode=EMode.valueOf(data[13]);
            zmin=Double.parseDouble(data[14]);
            zmax=Double.parseDouble(data[15]);
            zignor=Double.parseDouble(data[16]);
            zsave=Double.parseDouble(data[17]);
            fmill=Double.parseDouble(data[18]);
            zlaser=Double.parseDouble(data[19]);
            amin=Double.parseDouble(data[20]);
            amax=Double.parseDouble(data[21]);
            aignor=Double.parseDouble(data[22]);
            faon=Double.parseDouble(data[23]);
        }
        catch(Exception e){
            e.printStackTrace();
            throw e;
        }
    }    

    @Override
    public String toString() {
        String[] data=new String[24];
        data[0]=String.valueOf(ires);
        data[1]=String.valueOf(bgc.getRGB());
        data[2]=String.valueOf(fembossing);
        data[3]=String.valueOf(fedge);
        data[4]=String.valueOf(flow);
        data[5]=iscale.name();
        data[6]=String.valueOf(pline);
        data[7]=String.valueOf(psegment);
        data[8]=String.valueOf(ftravel);
        data[9]=pathtype.name();
        data[10]=String.valueOf(sweep);
        data[11]=String.valueOf(mdirx);
        data[12]=String.valueOf(mdiry);
        data[13]=String.valueOf(mode);
        data[14]=String.valueOf(zmin);
        data[15]=String.valueOf(zmax);
        data[16]=String.valueOf(zignor);
        data[17]=String.valueOf(zsave);
        data[18]=String.valueOf(fmill);
        data[19]=String.valueOf(zlaser);        
        data[20]=String.valueOf(amin);
        data[21]=String.valueOf(amax);
        data[22]=String.valueOf(aignor);
        data[23]=String.valueOf(faon);
        return String.join("|", data);
    }

}
