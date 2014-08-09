/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

/**
 * Old Data base. Use to load old DataFiles without errors!
 * @author patrick
 */
 enum Database {
    PORT(DatabaseV2.PORT), 
    SPEED(DatabaseV2.SPEED), 
    HOMEING(DatabaseV2.HOMING), 
    MAXFEEDRATE(DatabaseV2.MAXFEEDRATE), 
    WORKSPACE0(DatabaseV2.WORKSPACE0), 
    WORKSPACE1(DatabaseV2.WORKSPACE1), 
    WORKSPACE2(DatabaseV2.WORKSPACE2), 
    FILEDIRECTORY(DatabaseV2.FILEDIRECTORY), 
    STARTCODE(DatabaseV2.STARTCODE), 
    TOOLCHANGE(DatabaseV2.TOOLCHANGE), 
    SPINDLEON(DatabaseV2.SPINDLEON), 
    SPINDLEOFF(DatabaseV2.SPINDLEOFF), 
    GOFEEDRATE(DatabaseV2.GOFEEDRATE), 
    TOOLSIZE(DatabaseV2.TOOLSIZE), 
    OPTIMISATIONTIMEOUT(DatabaseV2.OPTIMISATIONTIMEOUT), 
    ALZERO(DatabaseV2.ALZERO), 
    ALMAXPROPDEPTH(DatabaseV2.ALMAXPROBDEPTH), 
    ALSAVEHEIGHT(DatabaseV2.ALSAVEHEIGHT), 
    ALCLEARENCE(DatabaseV2.ALCLEARANCE), 
    ALFEEDRATE(DatabaseV2.ALFEEDRATE), 
    ALDISTANACE(DatabaseV2.ALDISTANCE), 
    ALMAXMOVELENGTH(DatabaseV2.ALMAXMOVELENGTH), 
    ALSTARTCODE(DatabaseV2.ALSTARTCODE), 
    ARCSEGMENTLENGTH(DatabaseV2.ARCSEGMENTLENGTH), 
    BL0(DatabaseV2.BL0), 
    BL1(DatabaseV2.BL1), 
    BL2(DatabaseV2.BL2), 
    G1MODAL(DatabaseV2.G1MODAL), 
    COMTYPE(DatabaseV2.COMTYPE);
    private final DatabaseV2 link;

    private Database(DatabaseV2 link) {
        this.link = link;
    }

    public DatabaseV2 getLink() {
        return link;
    }
    
}
