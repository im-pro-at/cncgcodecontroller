/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cnc.gcode.controller;

/**
 *
 * @author n.rambaud
 */
public class JSettingFeedback implements ISettingFeedback{
    public JSettingEnum id;
    public double value;
    public double minValue;
    public double maxValue;
    public String message;
    public JSettingFeedback(JSettingEnum settingId,
                            double settingValue,
                            String description)
    {
        id      = settingId;
        value   = settingValue;
        message = description; 
    }
    public JSettingFeedback(JSettingEnum settingId,
                            double settingValue,
                            double minValue,
                            double maxValue,
                            String description)
    {
        id      = settingId;
        value   = settingValue;
        message = description; 
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public JSettingEnum getSettingId() {
        return id;
    }

    @Override
    public double getSettingValue() {
        return value;
    }

    @Override
    public String getSettingDescription() {
        return message;
    }

    @Override
    public void setSettingValue(double value) {
        this.value = value;
    }

    @Override
    public double getSettingMinValue() {
        return this.minValue;
    }

    @Override
    public double getSettingMaxValue() {
        return this.maxValue;
    }
}
