package org.endeavourhealth.mysqlexporter;
import org.endeavourhealth.mysqlexporter.repository.Repository;
import org.endeavourhealth.mysqlexporter.resources.LHSSQLAllergyIntolerance;
import org.endeavourhealth.mysqlexporter.resources.LHSSQLMedicationStatement;
import org.endeavourhealth.mysqlexporter.resources.LHSSQLObservation;
import org.endeavourhealth.mysqlexporter.resources.LHSSQLPatient;

import java.util.Properties;

public class MySQLExporter implements AutoCloseable {

    private final Repository repository;

    public MySQLExporter(final Properties properties) throws Exception {
        this(properties, new Repository(properties));
    }

    public MySQLExporter(final Properties properties, final Repository repository) {
        this.repository = repository;
    }

   public void export() throws Exception {

        repository.delq="";
        if (repository.params.indexOf("delq") >=0)
        {
            repository.DELQ();
            return;
        }

        repository.oneoff="";
        if (repository.params.indexOf("oneoff") >=0)
        {
            String[] ss = repository.params.split("\\:");
            repository.oneoff=ss[1];
            ss = new String[0];
            ss = repository.oneoff.split("\\~");
            repository.oneoff = ss[0];
            repository.OneOff();
            return;
        }

       if (repository.params.indexOf("references") >=0)
       {
           repository.getReferences();
           return;
       }

        if (repository.params.indexOf("queueinfo") >=0)
        {
            //String[] ss = repository.params.split("\\:");
            //repository.organization = ss[1];
            repository.GetQData();
            return;
        }

        if (repository.params.indexOf("dumprefs") >=0)
        {
            repository.DumpRefs();
            return;
        }

        repository.a_patient = "";
        if (repository.params.indexOf("a_patient") >=0) {
            String[] ss = repository.params.split("\\:");
            repository.a_patient=ss[1];
            ss = new String[0];
            ss = repository.a_patient.split("\\~");
            repository.a_patient = ss[0];
        }

        repository.organization = "";
        if (repository.params.indexOf("organization") >=0)
        {
            String[] ss = repository.params.split("\\:");
            repository.organization = ss[1];
            ss = new String[0];
            ss = repository.organization.split("\\~");
            repository.organization = ss[0];
        }

        // create the allergy csv data from the reference table
        LHSSQLAllergyIntolerance AllergySQL = new LHSSQLAllergyIntolerance();
        AllergySQL.Run(this.repository);

        LHSSQLMedicationStatement rx = new LHSSQLMedicationStatement();
        rx.Run(this.repository);

        LHSSQLPatient patient = new LHSSQLPatient();
        patient.Run(this.repository);

        LHSSQLObservation observation = new LHSSQLObservation();
        observation.Run(this.repository);
   }

    @Override
    public void close() throws Exception {
        repository.close();
    }
}