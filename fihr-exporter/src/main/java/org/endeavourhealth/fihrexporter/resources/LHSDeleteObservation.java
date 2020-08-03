package org.endeavourhealth.fihrexporter.resources;

import org.endeavourhealth.fihrexporter.repository.Repository;
import org.endeavourhealth.fihrexporter.send.LHShttpSend;

import java.sql.SQLException;
import java.util.List;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

public class LHSDeleteObservation {
    public String Run(Repository repository, String baseURL)  throws SQLException {
        String ret = "";
        String id;
        Integer responseCode=0;
        String result = ""; String ok = "";

        List<List<String>> ids = repository.getDeleteRowsObservation();

        LHShttpSend send = new LHShttpSend();

        for(List<String> rec : ids)
        {
            if (isTrue(repository.Stop())) {
                System.out.println("STOPPING DELETE OBSERVATION");
                return "1";
            }

            if(!rec.isEmpty()) {

                System.out.print(rec.get(0));

                id = rec.get(0);

                // already deleted from queue as part of composite observation?
                ok = repository.CheckObsInQ(id);
                if (ok.equals("0")) {continue;}

                String loc = repository.getLocationObsWithCheckingDeleted(id);
                if (loc.isEmpty()) {
                    System.out.println("No obs location for id (cannot delete)? "+id);
                    return ret;
                }

                String zids = repository.getIdsForLocation(loc);

                if (zids.isEmpty()) {
                    System.out.println("No zids for obs location (cannot delete)? "+loc);
                    return ret;
                }

                // check child obs DON'T exist in the clinical db before deleting
                String[] ss = zids.split("~");
                String q = "";
                for (int i = 0; i < ss.length; i++) {
                    q = ss[i];
                    ret = repository.getPatientIdAndOrg(q, "observation");
                    if (!ret.equals("~")) {
                        System.out.println("Child obs still exists in the db (cannot delete)? "+ q);
                        return ret;
                    }
                }

                ok = repository.AllObsInQ(zids);
                // patient -2, observation - 11, allergy - 4, medication - 10
                if (ok.equals("1")) {
                    responseCode=send.Delete(repository, id, "Observation", "0", 11);
                    if (!responseCode.equals(204)) {return "?";}
                    // purge the zids (child obs) from the delete queue
                    ss = zids.split("~");
                    for (int i = 0; i < ss.length; i++) {
                        q = ss[i];
                        repository.PurgeTheDeleteQueue(q, "Observation");
                    }
                }
            }
        }

        return ret;
    }
}