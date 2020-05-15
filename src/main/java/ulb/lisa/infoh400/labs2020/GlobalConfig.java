/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ulb.lisa.infoh400.labs2020;

import be.fedict.commons.eid.client.BeIDCards;
import java.text.SimpleDateFormat;

/**
 *
 * @author Adrien Foucart
 */
public class GlobalConfig {
    
    public static String ORTHANC_HOST = "localhost";
    public static int ORTHANC_PORT = 4242;
    public static String ORTHANC_AET = "ORTHANC";
    public static String LOCAL_DICOM_REPOSITORY = "C:\\Users\\Administrateur\\infoh400-labs2020\\src\\main\\resources\\localpacs";
    public static int STORESCP_PORT = 11112;
    public static String STORESCP_AET = "STORESCP";
    public static SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
    public static SimpleDateFormat timeFmt = new SimpleDateFormat("H:mm");
    public static SimpleDateFormat dateTimeFmt = new SimpleDateFormat("yyyy-MM-dd H:mm");
    public static SimpleDateFormat dcmDateFmt = new SimpleDateFormat("yyyyMMdd");

    private static int nextADTMessageID = 0;
    public static boolean HL7_TLS = false;
    
    public static int getNextADTMessageID() {
        nextADTMessageID += 1;
        return nextADTMessageID;
    }
    
    public static int HL7_LISTENING_PORT = 54321;
    
    public static BeIDCards cards = new BeIDCards();
    public static String CONSENT_FORM = "I, [PATIENT], give permission to the Hospital to store my personal data according to the data usage agreement.";
    public static String CONSENT_FORM_DIRECTORY = "C:\\Users\\Administrateur\\infoh400-labs2020\\src\\main\\resources\\consent";
}
