package org.endeavourhealth.fihrexporter;

import org.endeavourhealth.fihrexporter.repository.Repository;
import org.endeavourhealth.fihrexporter.resources.*;
import org.endeavourhealth.fihrexporter.resources.LHSMedicationStatement;
import org.endeavourhealth.fihrexporter.send.LHShttpSend;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Scanner;
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
            //Process p = Runtime.getRuntime().exec("ps -few");
            //so we can detect organization
            Process p = Runtime.getRuntime().exec("ps -ewo pid,cmd:160,etime");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((process = input.readLine()) != null) {
                //System.out.println(process);
                //if (process.indexOf("FihrExporter-") >=0) {runcount=runcount+1;}
                if (process.indexOf("organization:"+repository.organization) >=0) {runcount=runcount+1;}
            }
            input.close();
        } catch (Exception err) {
            err.printStackTrace();
        }
        return runcount;
    }

    public String export(String finished) throws Exception {

        if (!this.repository.testobs.isEmpty()) {
            LHSObservation observation = new LHSObservation();
            observation.TestObs(repository,repository.testobs);
            return "1111";
        }

        //Scanner scan = new Scanner(System.in);
        //System.out.print("Should have written out FHIR! . . . ");
        //scan.nextLine();

        repository.counting = 0;
        
        Integer runcount = IsRunning();
        if (runcount>1) {System.out.println("already running"); return "1111";}

        if (Integer.parseInt(this.repository.procrun)>0) {return "1111";}

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
                return "1111";
            }
        }

        if (!this.repository.resendpats.isEmpty()) {
            // read a file of ids
            System.out.println("Re-sending patients!!");
            String deducted = ""; String result = "";
            LHSPatient patient = new LHSPatient();

            String fileName = "//tmp//resendpats.txt";
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String nor;
            while((nor = bufferedReader.readLine()) != null)
            {
                System.out.println(nor);
                // has the patient been deducted?
                deducted = repository.Deducted(Integer.parseInt(nor),"Patient");
                result = patient.RunSinglePatient(repository, Integer.parseInt(nor), baseURL, deducted);
            }
            fileReader.close();
            return "1111";
        }

        this.repository.Audit(0,"","Start",0,"dum","",0,0);

        // ** TO DO put this back in
        this.repository.DeleteTracker();

        // ** TO DO put this back in
        // causing problems when doing stress testing - running two or more organizations at a time
        // this.repository.DeleteFileReferences();

        // perform any deletions
        // !! ONLY NEEDS TO BE RUN ONCE !!
        LHSDelete delete = new LHSDelete();
        delete.Run(this.repository, baseURL);

        // index 1, till index 2
        String pfin=finished.substring(1,2);
        if (pfin.equals("0")) {
            LHSPatient patient = new LHSPatient();
            pfin = patient.Run(this.repository, baseURL);
        }

        String rxfin=finished.substring(2,3);
        if (rxfin.equals("0")) {
            LHSMedicationStatement medicationStatement = new LHSMedicationStatement();
            rxfin = medicationStatement.Run(this.repository, baseURL);
        }

        String afin=finished.substring(3,4);
        if (afin.equals("0")) {
            LHSAllergyIntolerance allergyIntolerance = new LHSAllergyIntolerance();
            afin = allergyIntolerance.Run(this.repository, baseURL);
        }

        //gfg.gc();

        String ofin=finished.substring(0,1);
        if (ofin.equals("0")) {
            LHSObservation observation = new LHSObservation();
            ofin = observation.Run(this.repository, baseURL);
        }

        this.repository.Audit(0,"","End",0,"dum","",0,0);

        // 1111 process has completed (no need to loop round again!)
        String ret = ofin+pfin+rxfin+afin;

        return ret;

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
