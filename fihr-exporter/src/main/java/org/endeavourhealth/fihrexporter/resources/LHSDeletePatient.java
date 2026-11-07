package org.endeavourhealth.fihrexporter.resources;

import org.endeavourhealth.fihrexporter.repository.Repository;
import org.endeavourhealth.fihrexporter.send.LHShttpSend;

import java.sql.SQLException;
import java.util.List;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

public class LHSDeletePatient {
    public String RunContacts(Repository repository, String baseURL)  throws SQLException {
        String ret = "";
        String nor = "";

        String deducted = ""; String deceased = ""; String result = "";
        String incohort = "";

        List<List<String>> ids = repository.getDeleteRowsContacts();

        LHSPatient patient = new LHSPatient();

        for(List<String> rec : ids) {
            if (isTrue(repository.Stop())) {
                System.out.println("STOPPING RESEND OF PATIENT RESOURCES WHERE CONTACTS HAVE BEEN DELETED");
                return "1";
            }

            nor = rec.get(0);

            incohort = repository.InCohort(nor);
            deducted = "1";
            if (incohort.equals("1")) {deducted = "0";}

            System.out.println("re-sending patient "+nor+" because one of the contacts was deleted");

            result = patient.RunSinglePatient(repository, nor, baseURL, deducted);
            if (result.equals("1")) {return "?";}

            // purge filteredContactsDelta (the contacts queue)
            String anIds = repository.getContactIdsFromReferencesTableForPatient(nor);

            String[] ss = anIds.split("\\~");
            for (int i = 0; i < ss.length; i++) {
                String anId = ss[i];

                System.out.println("purging the contact delete queue for "+anId);

                repository.PurgeTheDeleteQueue(anId, "Patient-Contact");
            }
        }

        return ret;
    }

    public String Run(Repository repository, String baseURL)  throws SQLException {
        String ret = "";
        String id;
        Integer responseCode=0;
        String result = "";

        List<List<String>> ids = repository.getDeleteRowsPatient();

        LHShttpSend send = new LHShttpSend();

        for(List<String> rec : ids)
        {
            if (isTrue(repository.Stop())) {
                System.out.println("STOPPING DELETE PATIENT");
                return "1";
            }

            if(!rec.isEmpty()) {

                System.out.print(rec.get(0));

                id = rec.get(0);

                // patient -2, observation - 11, allergy - 4, medication - 10
                responseCode=send.Delete(repository, id, "Patient", "0", 2);
                if (!responseCode.equals(204)) {return "?";}
            }
        }

        return ret;
    }
}