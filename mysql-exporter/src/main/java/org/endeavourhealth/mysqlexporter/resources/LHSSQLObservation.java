package org.endeavourhealth.mysqlexporter.resources;

import org.endeavourhealth.mysqlexporter.repository.Repository;

import java.io.File;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class LHSSQLObservation {
	public String Run(Repository repository)  throws SQLException
	{

		repository.DeleteReportTracker();

		String result=""; Integer parent=0; String parentids=""; String ObsRec="";
		String noncoreconceptid=""; String ss[]; String orginalterm;
		Integer nor=0; String snomedcode=""; String result_value="";
		String clineffdate=""; String resultvalunits=""; String location="";
		String zid =""; String resultvalue=""; String t="";

        List<Integer> ids = new ArrayList<>();
        if (repository.organization.isEmpty()) {
            ids = repository.getRows("Observation", "filteredObservationsDelta");
        }
        if (!repository.organization.isEmpty()) {
            ids = repository.getRowsFromReferences("Observation",repository.organization,"observation");
        }

		Integer id = 0; Integer j = 0;

		try {
            String OS = System.getProperty("os.name").toLowerCase();
            String file="//tmp//obs.txt";
			if (OS.indexOf("win") >= 0) {file="D:\\TEMP\\obs.txt";}
			PrintStream o = new PrintStream(new File(file));
			System.setOut(o);
		} catch (Exception e) {
			System.out.println(e);
		}

		String headings="dds_id,patient_id,date,code,term,result_value,result_unints,dds_parent_id,dds_child_id";
		System.out.println(headings);

		while (ids.size() > j) {
			id = ids.get(j);

			// has this record already been processed?
			location = repository.getLocation(id);
			if (location.length()>0) {j++; continue;}

			result = repository.getObservationRS(id);

			if (result.length()==0) {
				System.out.println("getObservationRS: "+id+" no return");
				j++; continue;
			}

			ss = result.split("\\~");
			parent=Integer.parseInt(ss[6]);

			nor = Integer.parseInt(ss[0]); snomedcode=ss[1]; orginalterm=ss[2]; resultvalue=ss[3]; clineffdate=ss[4]; resultvalunits=ss[5];

			if (parent==0) {
                System.out.println(id + "," + nor + "," + clineffdate + "," + snomedcode + "," + orginalterm + "," + resultvalue + "," + resultvalunits + "," + parent + "," + id);
                repository.Audit(id, "", "ReportTracker", 0, "dum", "", nor, 0);
            }

			//repository.Audit(id, "", "ReportTracker", 0, "dum", "", nor, 0);

			if (parent != 0) {
                parentids = repository.getIdsFromParent(parent);

                ObsRec = repository.getObservationRecord(Integer.toString(parent));
                ss = ObsRec.split("\\~");
                snomedcode = ss[0];
                orginalterm = ss[1];
                resultvalue = ss[2];
                clineffdate = ss[3];
                resultvalunits = ss[4];

                // System.out.println(id + "," + nor + "," + clineffdate + "," + snomedcode + "," + orginalterm + "," + resultvalue + "," + resultvalunits + "," + parent);

                if (parentids.length() > 0) {
                    ss = parentids.split("\\~");
                    for (int i = 0; i < ss.length; i++) {
                        zid = ss[i];

                        // obs id sent in this run?  might have already been sent in a bp?
                        //t = repository.getLocation(Integer.parseInt(zid));
                        //if (t.length() > 0) {
                        //    System.out.println("Obs" + id + " has been processed");
                        //    j++;
                        //    continue;
                        //}

                        try {

                            ObsRec = repository.getObservationRecord(zid);

                            if (ObsRec.length() == 0) {
                                continue;
                            }

                            String obs[] = ObsRec.split("\\~");

                            snomedcode = obs[0];
                            orginalterm = obs[1];
                            resultvalue = obs[2];
                            clineffdate = obs[3];
                            resultvalunits = obs[4];
                            if (snomedcode.length() == 0) snomedcode = obs[5];

                            System.out.println(zid + "," + nor + "," + clineffdate + "," + snomedcode + "," + orginalterm + "," + resultvalue + "," + resultvalunits + "," + parent + "," +id);

                            repository.Audit(Integer.parseInt(zid), "", "ReportTracker", 0, "dum", "", nor, 0);

                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                }
            }
			j++;
		}

		return "stuff";
	}
}