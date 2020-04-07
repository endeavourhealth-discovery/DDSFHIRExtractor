package org.endeavourhealth.fihrexporter.resources;

import org.endeavourhealth.fihrexporter.repository.Repository;
import org.endeavourhealth.fihrexporter.send.LHShttpSend;

import java.sql.SQLException;
import java.util.List;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

public class LHSDelete {
    public String Run(Repository repository, String baseURL)  throws SQLException {
        String ret = "";
        String id; String tableid; String nor; String tablename="";
        Integer responseCode=0; String resource=""; String deducted = "";
        String result = "";

        List<List<String>> ids = repository.getDeleteRows();

        LHShttpSend send = new LHShttpSend();

        for(List<String> rec : ids)
        {
            if (isTrue(repository.Stop())) {
                System.out.println("STOPPING DELETE");
                return "1";
            }

            if(!rec.isEmpty()) {

                System.out.print(rec.get(0));
                System.out.print(rec.get(1));
                id = rec.get(0); tableid = rec.get(1); nor = rec.get(2); tablename=rec.get(3);
                resource=rec.get(4);

                if (tablename.length()==0) continue;

                if (tableid.equals("2")) {
                    /*
                    deducted = repository.Deducted(Integer.parseInt(id), "Patient");
                    if (deducted.equals("1")) {
                        String eocdata = repository.DeductedData(Integer.parseInt(id));
                        if (!eocdata.isEmpty()) {
                            // send an episode of care resource
                            String[] ss = eocdata.split("\\~", -1);
                            Integer eocid = Integer.parseInt(ss[0]); String StartDate = ss[1]; String EndDate = ss[2];
                            LHSEpisodeOfCare eoc = new LHSEpisodeOfCare();
                            result = eoc.Run(repository, eocid, Integer.parseInt(id), StartDate, EndDate, baseURL);
                            if (result.equals("1")) {return "1";}
                            repository.PurgeTheDeleteQueue(Integer.parseInt(id),"Patient");
                        }
                    }
                    */

                    // double check that the patient is deducted?
                    /*
                    deducted = repository.Deducted(Integer.parseInt(id), "Patient");
                    if (deducted.equals("1")) {
                        LHSPatient patient = new LHSPatient();
                        patient.RunSinglePatient(repository, Integer.parseInt(nor), baseURL, "1"); // 1 is deducted
                        repository.PurgeTheDeleteQueue(Integer.parseInt(id), "Patient");
                    }
                    continue;
                    */

                    deducted = repository.InCohort(Integer.parseInt(id));
                    if (deducted.equals("0")) {
                        LHSPatient patient = new LHSPatient();
                        patient.RunSinglePatient(repository, Integer.parseInt(nor), baseURL, "1"); // 1 is deducted
                        repository.PurgeTheDeleteQueue(Integer.parseInt(id), "Patient");
                        repository.PurgetheQueue(Integer.parseInt(id), "Patient");
                        continue;
                    }
                }

                // patient -2, observation - 11, allergy - 4, medication - 10
                /*
                if (tableid.equals("11")) {
                    responseCode=send.DeleteObservation(repository,Integer.parseInt(id),"Observation",Integer.parseInt(nor),11);
                    continue;
                }

                responseCode=send.Delete(repository, Integer.parseInt(id), resource, Integer.parseInt(nor), Integer.parseInt(tableid));
                 */
            }
        }

        return ret;
    }
}