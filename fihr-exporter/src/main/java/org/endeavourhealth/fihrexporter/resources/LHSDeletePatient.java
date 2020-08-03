package org.endeavourhealth.fihrexporter.resources;

import org.endeavourhealth.fihrexporter.repository.Repository;
import org.endeavourhealth.fihrexporter.send.LHShttpSend;

import java.sql.SQLException;
import java.util.List;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

public class LHSDeletePatient {
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