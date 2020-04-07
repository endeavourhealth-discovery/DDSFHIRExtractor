package org.endeavourhealth.fihrexporter.resources;

import ca.uhn.fhir.context.FhirContext;
import org.endeavourhealth.fihrexporter.send.LHShttpSend;
import org.hl7.fhir.dstu3.model.*;

import org.endeavourhealth.fihrexporter.repository.Repository;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

public class LHSObservation {

	private List<Integer> zresult = new ArrayList<>();

	private CodeableConcept addCodeableConcept(String snomed, String term, String parent)
	{
		CodeableConcept code = new CodeableConcept();
		code.addCoding()
				.setCode(snomed)
				.setDisplay(term)
				.setSystem("http://snomed.info/sct")
                .setId(parent);

		return code;
	}

	private Observation.ObservationComponentComponent ObsCompComp(String coreconceptid, String term, String resultvalue, String resultvalueunits, String zid)
	{
		Observation.ObservationComponentComponent occ= new Observation.ObservationComponentComponent();
		CodeableConcept codecc = new CodeableConcept();
		codecc.addCoding()
				.setCode(coreconceptid)
				.setSystem("http://snomed.info/sct")
				.setDisplay(term)
				.setId(zid);
		occ.setCode(codecc);

		if (!resultvalue.isEmpty()) {
            Quantity q = new Quantity();
            q.setValue(Double.parseDouble(resultvalue));
            q.setSystem("http://unitsofmeasure.org");
            // if (resultvalueunits != null) {
            if (!resultvalueunits.isEmpty()) {
                q.setCode(resultvalueunits);
            }
            occ.setValue(q);
        }

		return occ;
	}

	private String getObervationResource(Repository repository, Integer patientid, String snomedcode, String orginalterm, String resultvalue, String clineffdate, String resultvalunits, String PatientRef, String ids, Integer parent, Integer ddsid, String putloc)
	{
		String id = "";

		//Observation observation = null;

		FhirContext ctx = FhirContext.forDstu3();

		Observation observation = new Observation();

		if (putloc.length()>0) {
			observation.setId(putloc);
		}
		observation.setStatus(Observation.ObservationStatus.FINAL);

        observation.addIdentifier()
                .setSystem("https://discoverydataservice.net/ddsid")
                .setValue(ddsid.toString());

        // for reporting
        if (parent!=0) {
			observation.addIdentifier()
					.setSystem("https://discoverydataservice.net/ddsparentid")
					.setValue(parent.toString());
		}

		String ObsRec = ""; String noncoreconceptid = "";

		// use parent code if necessary
		if (parent !=0) {
			try {

				ObsRec= repository.getObservationRecordNew(Integer.toString(parent));

				String[] ss = ObsRec.split("\\~");

				noncoreconceptid = ss[0]; orginalterm = ss[1];
				if (noncoreconceptid.length()==0) noncoreconceptid = ss[5];

				CodeableConcept code = addCodeableConcept(noncoreconceptid, orginalterm, parent.toString());
				observation.setCode(code);

				//System.out.println(ObsRec);
			} catch (Exception e) {
			}
		}

		if (parent == 0) {
			CodeableConcept code = addCodeableConcept(snomedcode, orginalterm, "");
			observation.setCode(code);
		}

		// http://hl7.org/fhir/stu3/valueset-observation-category.html
        // social-history, vital-signs, imaging, laboratory, procedure, survey, exam, therapy

        /*
		CodeableConcept vital = new CodeableConcept();
		vital.addCoding()
				.setCode("vital-signs");

		// might be a lab result, or something else?
		observation.addCategory(vital);
        */

		observation.setSubject(new Reference("/Patient/" + PatientRef));

		Period period = new Period();
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			// SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			period.setStart(format.parse(clineffdate));
		} catch (Exception e) {
		}
		observation.setEffective(period);

		// nests codeable concepts
		String encoded = "";
		ArrayList occs=new ArrayList();

		if (ids.length() > 0) {
			String[] ss = ids.split("\\~");
			for (int i = 0; i < ss.length; i++) {
				id = ss[i];
				try {

					ObsRec = repository.getObservationRecordNew(id);

					if (ObsRec.length() == 0) {continue;}
					String obs[] = ObsRec.split("\\~");
					snomedcode = obs[0]; orginalterm = obs[1]; resultvalue = obs[2]; clineffdate = obs[3]; resultvalunits = obs[4];
					if (snomedcode.length() == 0) snomedcode = obs[5];

					//if (resultvalue.length() > 0 || resultvalunits.length() > 0) {
                        Observation.ObservationComponentComponent ocs = ObsCompComp(snomedcode, orginalterm, resultvalue, resultvalunits, id);
                        occs.add(ocs);
                        observation.setComponent(occs);
                    //}
				} catch (Exception e) {
				}
			}

			encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(observation);
			return encoded;
		}

        //System.out.println(resultvalue.length());

		if (resultvalue.length()>0) {
            Observation.ObservationComponentComponent ocs = ObsCompComp(snomedcode, orginalterm, resultvalue, resultvalunits, ddsid.toString());
            occs.add(ocs);
            observation.setComponent(occs);
        }

		encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(observation);

		return encoded;
	}

	private void ObsAudit(Repository repository, String ids, Integer patientid, String location) throws SQLException
	{
		String[] ss = ids.split("\\~");
		String id = "";
		for (int i = 0; i < ss.length; i++) {
			id = ss[i];
			repository.Audit(Integer.parseInt(id), "", "Tracker", 0, "dum", "", patientid, 0);
			repository.Audit(Integer.parseInt(id), "", "Observation", 1234, location, "", patientid, 11);
		}
	}

	public void DT(String prefix) {
        long timeNow = Calendar.getInstance().getTimeInMillis();
        java.sql.Timestamp ts = new java.sql.Timestamp(timeNow);
        String str = ts.toString();
        System.out.println(prefix+" "+str);
	}

	private String IdProcessed(Integer id)
	{
		String yn="n"; Integer j = 0;
		Integer zid;
		while (zresult.size() > j) {
			zid = zresult.get(j);
			//System.out.println(zid+"="+id+"?");
			if (zid.equals(id)) {yn="y"; break;}
			j++;
		}
		return yn;
	}

	// Test stub (be careful when running in production environments)
	public void TestObs(Repository repository, String patient_id) throws SQLException
	{
		String snomedcode =""; String orginalterm=""; String result_value="";
		String clineffdate = ""; String resultvalunits = ""; String location="";
		Integer typeid = 11; String t = ""; Integer parent =0; String parentids = "";
		String id; Integer nor; String yn="";

		// get all the ids for a patient
		String ids = repository.GetIdsForNOR(patient_id);
		if (ids.length()==0) {System.out.println("empty result set"); return;};

		String[] ssids = ids.split("\\~");

		// windows
		// String dir = "c:\\temp\\TestObs\\";
		// linux
		String dir = "//tmp//TestObs//";

		String file = "";

		for (int i = 0; i < ssids.length; i++) {
			id = ssids[i];
			yn = IdProcessed(Integer.parseInt(id));
			if (yn=="y") {continue;}

			zresult.add(Integer.parseInt(id));

			String result = repository.getObservationRSNew(Integer.parseInt(id));

			if (result.length()>0) {

				//System.out.println(result);

				String[] ss = result.split("\\~");
				nor = Integer.parseInt(ss[0]);
				snomedcode = ss[1];
				orginalterm = ss[2];
				result_value = ss[3];
				clineffdate = ss[4];
				resultvalunits = ss[5];

				parent = Integer.parseInt(ss[6]);
				parentids = "";
				if (parent != 0) {
					// should really be child_ids
					parentids = repository.getIdsFromParent(parent);
				}
				location = repository.getLocation(nor, "Patient");
				String putloc = repository.getLocation(Integer.parseInt(id), "Observation");

				String encoded = getObervationResource(repository, nor, snomedcode, orginalterm, result_value, clineffdate, resultvalunits, location, parentids, parent, Integer.parseInt(id), putloc);
				System.out.println(encoded);

				file = dir+"obs_"+id+".txt";
				try {
					FileWriter fr = new FileWriter(file);
					fr.write(encoded);
					fr.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				// so, the child_id's don't get processed again
				if (parentids.length() > 0 ) {
					String[] ssp = parentids.split("\\~");
					for (i = 0; i < ssp.length; i++) {
						zresult.add(Integer.parseInt(ssp[i]));
					}
				}
			}
		}
	}

	public String Run(Repository repository, String baseURL) throws SQLException
	{
		String encoded = ""; Integer j = 0; Integer id = 0;

		if (isTrue(repository.Stop())) {
			System.out.println("STOPPING OBS");
			return "1";
		}

		//List<Integer> ids = repository.getRows("filteredobservations");
        List<Integer> ids = repository.getRows("filteredObservationsDelta");

        if (ids.isEmpty()) {
        	return "1";
		}

		Integer nor =0; // patientid
		String snomedcode =""; String orginalterm=""; String result_value="";
		String clineffdate = ""; String resultvalunits = ""; String location="";
		Integer typeid = 11; String t = ""; Integer parent =0; String parentids = "";

        String url = baseURL + "Observation"; String putloc="";

		ResultSet rs; String result = ""; String deducted = ""; String deceased = "";

        Runtime gfg = Runtime.getRuntime();
        long memory1, memory2;
        Integer integer[] = new Integer[1000];

        while (ids.size() > j) {

        	if (isTrue(repository.Stop())) {
				System.out.println("STOPPING OBS");
        		return "1";
        	}

			id = ids.get(j);

            //System.out.println(id);

			if (id == 23185) {
				System.out.println("test");
			}

            result = repository.getObservationRSNew(id);

            if (result.length()>0) {

                String[] ss = result.split("\\~");
                nor = Integer.parseInt(ss[0]); snomedcode=ss[1]; orginalterm=ss[2]; result_value=ss[3]; clineffdate=ss[4]; resultvalunits=ss[5];

				// obs id sent in this run?  might have already been sent in a bp?
				t = repository.getLocation(id,"Tracker");
				if (t.length() > 0) {
					System.out.println("Obs" + id + " has been processed");
					j++;
					continue;
				}

				/*
				deceased = repository.Deceased(nor, "Observation");
				deducted = repository.Deducted(nor, "Observation");
				if (deducted.equals("1") || deceased.equals("1")) {
					System.out.println("Obs - Patient has died or has been deducted " + nor);
					repository.PurgetheQueue(id, "Observation");
					j++;
					continue;
				}
				*/

				deducted = repository.InCohort(nor);
				if (deducted.equals("0")) {
					System.out.println("Observation - Patient not in cohort (probably deducted)");
					repository.PurgetheQueue(id, "Observation");
					j++;
					continue;
				}

				// parent = rs.getInt("parent_observation_id");
                parent = Integer.parseInt(ss[6]); parentids = "";
				if (parent != 0) {
					// find the other event with the same parent id
					parentids = repository.getIdsFromParent(parent);
					//System.out.println(ids);
				}

				boolean prev = repository.PreviouslyPostedId(nor, "Patient");
				if (prev==false) {
					LHSPatient patient = new LHSPatient();
					patient.RunSinglePatient(repository, nor, baseURL, deducted);
				}

				location = repository.getLocation(nor, "Patient");
				if (location.length() == 0)
				{
					System.out.println("Unable to find patient " + nor);
					j++;
					continue;
				}

				putloc = repository.getLocation(id, "Observation");

				encoded = getObervationResource(repository, nor, snomedcode, orginalterm, result_value, clineffdate, resultvalunits, location, parentids, parent, id, putloc);

				// post
				Integer httpResponse;
				LHShttpSend send = new LHShttpSend();

				httpResponse = send.Post(repository, id, "", url, encoded, "Observation", nor, typeid);

				//if (httpResponse == 401) {return "401, aborting";}
				if (repository.outputFHIR.isEmpty()) {
					if (httpResponse == 401 || httpResponse == 0) {
						return "1";
					}
				}

				if (parentids.length() > 0) {
					location = repository.getLocation(id, "Observation");
					// location added so that we can delete a composite group
					ObsAudit(repository, parentids, nor, location);
				}

				System.out.println(httpResponse.toString());

				// if getObservationRSNew does not return anything, then software loops
				// j++;
			}
            j++;
		}
		return "0";
	}
}