/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ulb.lisa.infoh400.labs2020.controller;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.app.HL7Service;
import ca.uhn.hl7v2.app.Initiator;
import ca.uhn.hl7v2.llp.LLPException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v23.message.ACK;
import ca.uhn.hl7v2.model.v23.message.ADT_A01;
import ca.uhn.hl7v2.model.v23.segment.MSH;
import ca.uhn.hl7v2.model.v23.segment.PID;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationException;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import ulb.lisa.infoh400.labs2020.GlobalConfig;
import ulb.lisa.infoh400.labs2020.model.Patient;
import ulb.lisa.infoh400.labs2020.model.Person;

/**
 *
 * @author Adrien Foucart
 */
public class HL7Services {
    
    private HL7Service server = null;
    private final HapiContext ctxt = new DefaultHapiContext();
    
    public HL7Services(){
        startServer();
    }

    public boolean isListening() {
        return (server != null && server.isRunning());
    }
    
    public void stopServer(){
        if(isListening()){
            server.stop();
        }
    }
    
    public void startServer(){
        if(!isListening()){
            System.out.println("Starting HL7 Server listening on port " + GlobalConfig.HL7_LISTENING_PORT);
            server = ctxt.newServer(GlobalConfig.HL7_LISTENING_PORT, GlobalConfig.HL7_TLS);

            ReceivingApplication<ADT_A01> handler = new ADTReceiverApplication();
            server.registerApplication("ADT", "A01", handler);
            try {
                server.startAndWait();
            } catch (InterruptedException ex) {
                Logger.getLogger(HL7Services.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            System.out.println("Server started.");
        }
    }
    
    
    
    public static boolean sendADT_A01(Patient patient, String host, int port){
        try {
            ADT_A01 adt = new ADT_A01();
            adt.initQuickstart("ADT", "A01", "P");
            
            MSH msh = adt.getMSH();
            msh.getSendingApplication().getNamespaceID().setValue("HIS");
            msh.getMessageControlID().setValue(String.valueOf(GlobalConfig.getNextADTMessageID()));
            
            PID pid = adt.getPID();
            pid.getPatientName(0).getFamilyName().setValue(patient.getIdperson().getFamilyname());
            pid.getPatientName(0).getGivenName().setValue(patient.getIdperson().getFirstname());
            pid.getDateOfBirth().getTimeOfAnEvent().setValue(patient.getIdperson().getDateofbirth());
            pid.getPhoneNumberHome(0).getAnyText().setValue(patient.getPhonenumber());
            
            HapiContext ctxt = new DefaultHapiContext();
            Parser parser = ctxt.getXMLParser();
            String encoded = parser.encode(adt);
            
            Connection conn = ctxt.newClient(host, port, GlobalConfig.HL7_TLS);
            Initiator initiator = conn.getInitiator();
            ACK response = (ACK) initiator.sendAndReceive(adt);
            
            return response.getMSA().getAcknowledgementCode().getValue().equalsIgnoreCase("AA");
        } catch (HL7Exception | IOException | LLPException ex) {
            Logger.getLogger(HL7Services.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return false;
    }

    private class ADTReceiverApplication implements ReceivingApplication<ADT_A01> {
        
        private final EntityManagerFactory emfac = Persistence.createEntityManagerFactory("infoh400_PU");
        private final PatientJpaController patientCtrl = new PatientJpaController(emfac);
        private final PersonJpaController personCtrl = new PersonJpaController(emfac);
        
        public ADTReceiverApplication() {
        }

        @Override
        public Message processMessage(ADT_A01 t, Map<String, Object> map) throws ReceivingApplicationException, HL7Exception {
            String encodedMessage = ctxt.getPipeParser().encode(t);
            System.out.println("Received:");
            System.out.println(encodedMessage);
            
            Person person = new Person();
            person.setFamilyname(t.getPID().getPatientName(0).getFamilyName().getValue());
            person.setFirstname(t.getPID().getPatientName(0).getGivenName().getValue());
            person.setDateofbirth(t.getPID().getDateOfBirth().getTimeOfAnEvent().getValueAsDate());
            
            Person duplicate = personCtrl.findDuplicate(person);
            
            if( duplicate == null ){
                // No duplicate found: create new patient & person
                Patient patient = new Patient();
                patient.setIdperson(person);
                patient.setPhonenumber(t.getPID().getPhoneNumberHome(0).getAnyText().getValue());
                patient.setStatus("active");
                
                personCtrl.create(person);
                patientCtrl.create(patient);
            }
            else {
                if( duplicate.getPatient() == null ){
                    System.out.println("Person already exists. Creating new patient");
                    Patient patient = new Patient();
                    patient.setIdperson(duplicate);
                    patient.setPhonenumber(t.getPID().getPhoneNumberHome(0).getAnyText().getValue());
                    patient.setStatus("active");
                    patientCtrl.create(patient);
                }
                else {
                    System.out.println("Person already exists. Updating patient.");
                    Patient patient = duplicate.getPatient();
                    patient.setPhonenumber(t.getPID().getPhoneNumberHome(0).getAnyText().getValue());

                    try {
                        patientCtrl.edit(patient);
                    } catch (Exception ex) {
                        Logger.getLogger(HL7Services.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            
            try {
                return t.generateACK();
            } catch (IOException e) {
                throw new HL7Exception(e);
            }
        }

        @Override
        public boolean canProcess(ADT_A01 t) {
            return true;
        }
    }
    
}
