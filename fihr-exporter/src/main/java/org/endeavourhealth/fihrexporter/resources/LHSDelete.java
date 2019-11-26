package org.endeavourhealth.fihrexporter.resources;

import org.endeavourhealth.fihrexporter.repository.Repository;
import org.endeavourhealth.fihrexporter.send.LHShttpSend;

import java.sql.SQLException;
import java.util.List;

public class LHSDelete {
    public String Run(Repository repository)  throws SQLException {
        String ret = "";
        String id; String tableid; String nor; String tablename="";
        Integer responseCode=0; String resource="";

        List<List<String>> ids = repository.getDeleteRows();

        LHShttpSend send = new LHShttpSend();

        for(List<String> rec : ids)
        {
            if(!rec.isEmpty()) {

                System.out.print(rec.get(0));
                System.out.print(rec.get(1));
                id = rec.get(0); tableid = rec.get(1); nor = rec.get(2); tablename=rec.get(3);
                resource=rec.get(4);

                if (tablename.length()==0) continue;

                // patient -2, observation - 11, allergy - 4, medication - 10
                if (tableid.equals("11")) {
                    responseCode=send.DeleteObservation(repository,Integer.parseInt(id),"Observation",Integer.parseInt(nor),11);
                    continue;
                }

                responseCode=send.Delete(repository, Integer.parseInt(id), resource, Integer.parseInt(nor), Integer.parseInt(tableid));
            }
        }

        return ret;
    }
}