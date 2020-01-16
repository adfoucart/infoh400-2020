/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ulb.lisa.infoh400.labs2020.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.dstu3.model.Bundle;
import ulb.lisa.infoh400.labs2020.model.Patient;
import ulb.lisa.infoh400.labs2020.model.Person;

/**
 *
 * @author Adrien Foucart
 */
public class FHIRServices {
    
    private static final FhirContext ctxt = FhirContext.forDstu3();
    private IGenericClient client = null;
    
    public FHIRServices(){
    }
    
    public List<Patient> searchPatientsByName(String name, String serverBase){
        client = ctxt.newRestfulGenericClient(serverBase);
        
        Bundle results = client.search()
                .forResource(org.hl7.fhir.dstu3.model.Patient.class)
                .where(org.hl7.fhir.dstu3.model.Patient.FAMILY.matches().value(name))
                .returnBundle(Bundle.class)
                .execute();
        
        ArrayList<Patient> searchResponse = new ArrayList();
        
        for( Bundle.BundleEntryComponent p : results.getEntry() ){
            searchResponse.add(getPatient((org.hl7.fhir.dstu3.model.Patient) p.getResource()));
        }
        
        return searchResponse;
    }
    
    /*public List<org.hl7.fhir.dstu3.model.Patient> searchPatientsByName(String name){
        
    }*/
    
    public static Patient getPatient(org.hl7.fhir.dstu3.model.Patient patientFhir){
        Patient patientHIS = new Patient();
        Person personHIS = new Person();
        personHIS.setFamilyname(patientFhir.getNameFirstRep().getFamily());
        personHIS.setFirstname(patientFhir.getNameFirstRep().getGivenAsSingleString());
        personHIS.setDateofbirth(patientFhir.getBirthDate());
        patientHIS.setIdperson(personHIS);
        patientHIS.setPhonenumber(patientFhir.getTelecomFirstRep().getValue());
        patientHIS.setStatus("active");
        
        return patientHIS;
    }
    
    public static org.hl7.fhir.dstu3.model.Patient getPatient(Patient patientHIS){
        org.hl7.fhir.dstu3.model.Patient patientFHIR = new org.hl7.fhir.dstu3.model.Patient();
        
        patientFHIR.addName().setFamily(patientHIS.getIdperson().getFamilyname());
        patientFHIR.getNameFirstRep().addGiven(patientHIS.getIdperson().getFirstname());
        patientFHIR.setBirthDate(patientHIS.getIdperson().getDateofbirth());
        patientFHIR.addTelecom().setValue(patientHIS.getPhonenumber());
        
        return patientFHIR;
    }
}
