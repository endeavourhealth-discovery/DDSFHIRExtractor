package org.endeavourhealth.fihrexporter;

import org.endeavourhealth.fihrexporter.repository.Repository;
import org.endeavourhealth.fihrexporter.resources.*;
import org.endeavourhealth.fihrexporter.resources.LHSMedicationStatement;
import org.endeavourhealth.fihrexporter.send.LHShttpSend;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.UUID;

public class FihrExporter implements AutoCloseable {

    private final Repository repository;

    public FihrExporter(final Properties properties) throws Exception {
        this(properties, new Repository(properties));
    }

    public FihrExporter(final Properties properties, final Repository repository) {
        this.repository = repository;
    }

    private Integer IsRunning()
    {
        Integer runcount = 0;
        // if windows then return false
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.indexOf("win") >= 0) {return 0;}
        try {
            String process;
            Process p = Runtime.getRuntime().exec("ps -few");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((process = input.readLine()) != null) {
                //System.out.println(process);
                if (process.indexOf("FihrExporter-") >=0) {runcount=runcount+1;}
            }
            input.close();
        } catch (Exception err) {
            err.printStackTrace();
        }
        return runcount;
    }

    public void export() throws Exception {

        Integer runcount = IsRunning();
        if (runcount>1) {System.out.println("already running"); return;}

        if (Integer.parseInt(this.repository.procrun)>0) {return;}

        String baseURL = this.repository.getBaseURL();

        UUID uuid = UUID.randomUUID();
        String uuidStr = uuid.toString();
        this.repository.runguid = uuidStr;

        if (baseURL.contains("https:")) {
            LHShttpSend send = new LHShttpSend();
            repository.token = send.GetToken(this.repository);
            LHSTest test = new LHSTest();
            String response = test.TestCert(repository.token, baseURL + "Patient/");
            if (response == "invalid-cert") {
                return;
            }
        }

        this.repository.Audit(0,"","Start",0,"dum","",0,0);

        // ** TO DO put this back in
        this.repository.DeleteTracker();

        // ** TO DO put this back in
        this.repository.DeleteFileReferences();

        // perform any deletions
        LHSDelete delete = new LHSDelete();
        delete.Run(this.repository);

        LHSPatient patient = new LHSPatient();
        patient.Run(this.repository, baseURL);

        LHSMedicationStatement medicationStatement = new LHSMedicationStatement();
        medicationStatement.Run(this.repository, baseURL);

        LHSAllergyIntolerance allergyIntolerance = new LHSAllergyIntolerance();
        allergyIntolerance.Run(this.repository, baseURL);

        //gfg.gc();

        LHSObservation observation = new LHSObservation();
        observation.Run(this.repository, baseURL);

        this.repository.Audit(0,"","End",0,"dum","",0,0);

        //LHSTest test = new LHSTest();
        //test.Run(this.repository);
        //test.TestObsNotFound(this.repository);
        //test.ReconcileObservations(this.repository);
        //test.ReconcileOtherTables(this.repository);
        //test.GetPatients(this.repository);
        //String token = test.GetToken(this.repository);
        //String response = test.TestCert(this.repository.token, "https://dhs-fhir-test.azurehealthcareapis.com/Patient/");

        //test.TestDelete(this.repository, 22232, "Organization", 0, 0);
        //test.getConfig();
        //test.DeleteObservation(this.repository);

        //this.repository.TestConnection();

        //this.repository.getTerms();

    }

    @Override
    public void close() throws Exception {
        repository.close();
    }
}
