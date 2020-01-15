/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ulb.lisa.infoh400.labs2020.controller;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.StoredFilePathStrategy;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.UniqueIdentifierAttribute;
import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.FindSOPClassSCU;
import com.pixelmed.network.IdentifierHandler;
import com.pixelmed.network.MoveSOPClassSCU;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.network.StorageSOPClassSCPDispatcher;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ulb.lisa.infoh400.labs2020.GlobalConfig;
import ulb.lisa.infoh400.labs2020.model.Patient;
import ulb.lisa.infoh400.labs2020.view.AddPatientWindow;
import ulb.lisa.infoh400.labs2020.view.MainWindow;

/**
 *
 * @author Adrien Foucart
 */
public class DICOMServices {
    
    private boolean listening = false;
    private Thread t = null;
    private StorageSOPClassSCPDispatcher dispatcher = null;
    
    public DICOMServices(){
        try {
            dispatcher = new StorageSOPClassSCPDispatcher(
                    GlobalConfig.STORESCP_PORT,
                    GlobalConfig.STORESCP_AET,
                    new File(GlobalConfig.LOCAL_DICOM_REPOSITORY),
                    StoredFilePathStrategy.BYSOPINSTANCEUIDINSINGLEFOLDER,
                    new StoreObjectHandler());
        } catch (IOException ex) {
            Logger.getLogger(DICOMServices.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public boolean isListening(){
        return listening;
    }
    
    public void start(){
        t = new Thread(dispatcher);
        t.start();
        listening = true;
    }
    
    public void stop(){
        dispatcher.shutdown();
        listening = false;
    }

    public void doCMove(String studyUID) {
        try {
            AttributeList identifier = new AttributeList();
            { AttributeTag t = TagFromName.QueryRetrieveLevel; Attribute a = new CodeStringAttribute(t); a.addValue("STUDY"); identifier.put(t,a); }
            { AttributeTag t = TagFromName.StudyInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); a.addValue(studyUID); identifier.put(t,a); }
            new MoveSOPClassSCU(GlobalConfig.ORTHANC_HOST,GlobalConfig.ORTHANC_PORT,GlobalConfig.ORTHANC_AET,"MOVESCU","STORESCP",SOPClass.StudyRootQueryRetrieveInformationModelMove,identifier);
        }
        catch (DicomException | NullPointerException | ClassCastException | DicomNetworkException | IOException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private class StoreObjectHandler extends ReceivedObjectHandler {
        @Override
        public void sendReceivedObjectIndication(String dcmFilename, String transferSyntax, String callingAET) throws DicomNetworkException, DicomException, IOException {
            System.out.println("Received file " + dcmFilename + " from " + callingAET);
        }
    }
    
    public static void doCFind(Patient p, AddPatientWindow.CFindResultDisplayer displayer){
        try {
            AttributeList identifier = new AttributeList(); //specify attributes to retrieve and pass in any search criteria
            identifier.putNewAttribute(TagFromName.QueryRetrieveLevel).addValue("STUDY");   // query root of "study" to retrieve studies
            identifier.putNewAttribute(TagFromName.PatientName).addValue(p.getIdperson().getFamilyname() + "*");
            identifier.putNewAttribute(TagFromName.PatientBirthDate).addValue(GlobalConfig.dcmDateFmt.format(p.getIdperson().getDateofbirth()));
            // add all the fields you want to retrieve to the identifier
            identifier.putNewAttribute(TagFromName.PatientID);
            identifier.putNewAttribute(TagFromName.StudyInstanceUID);
            identifier.putNewAttribute(TagFromName.SOPInstanceUID);
            identifier.putNewAttribute(TagFromName.StudyDate);
            //retrieve all studies belonging to patient
            new FindSOPClassSCU(GlobalConfig.ORTHANC_HOST, 
                                GlobalConfig.ORTHANC_PORT, 
                                GlobalConfig.ORTHANC_AET, 
                                "FINDSCU",
                                SOPClass.StudyRootQueryRetrieveInformationModelFind, 
                                identifier, 
                                new CFindIdentifierHandler(displayer));
        } catch (DicomException | DicomNetworkException | IOException ex) {
            Logger.getLogger(DICOMServices.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static class CFindIdentifierHandler extends IdentifierHandler {
        
        private AddPatientWindow.CFindResultDisplayer displayer = null;
        
        public CFindIdentifierHandler(AddPatientWindow.CFindResultDisplayer displayer){
            super();
            
            this.displayer = displayer;
        }
        
        @Override
        public void doSomethingWithIdentifier(AttributeList identifier){
            displayer.addResult(identifier.get(TagFromName.StudyDate).getDelimitedStringValuesOrEmptyString());
        }
        
    }

    
}
