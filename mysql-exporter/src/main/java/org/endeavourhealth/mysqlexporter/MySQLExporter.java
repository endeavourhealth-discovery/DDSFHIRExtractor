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

        if (repository.params.indexOf("dumprefs") >=0)
        {
            repository.DumpRefs();
            return;
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