package org.endeavourhealth.fihrexporter.resources;

import ca.uhn.fhir.context.FhirContext;
import org.endeavourhealth.fihrexporter.send.LHShttpSend;
import org.hl7.fhir.dstu3.model.*;

import org.endeavourhealth.fihrexporter.repository.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

public class LHSAllergyIntolerance {

	private String getAllergyResource(String patientid, String clineffdate, String allergyname, String snomedcode, String PatientRef, String ddsid, String putloc)
	{
		//AllergyIntolerance allergy = null;

		FhirContext ctx = FhirContext.forDstu3();

		AllergyIntolerance allergy = new AllergyIntolerance();

		if (putloc.length()>0) {
			allergy.setId(putloc);
		}

		allergy.addIdentifier()
				.setSystem("https://discoverydataservice.net")
				.setValue(ddsid.toString());

		allergy.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-AllergyIntolerance-1");
		allergy.setClinicalStatus(AllergyIntolerance.AllergyIntoleranceClinicalStatus.ACTIVE);
		allergy.setVerificationStatus(AllergyIntolerance.AllergyIntoleranceVerificationStatus.CONFIRMED);

		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			allergy.setAssertedDate(format.parse(clineffdate));
		} catch (Exception e) {
		}

		// manifestation or codeable concept?
		CodeableConcept code = new CodeableConcept();
		code.addCoding()
				.setCode(snomedcode)
				.setSystem("http://snomed.info/sct")
				.setDisplay(allergyname);

		allergy.setCode(code);

		allergy.setPatient(new Reference("Patient/" + PatientRef));

		String encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(allergy);

		return encoded;
	}

	public String Run(Repository repository, String baseURL)  throws SQLException
	{
		String encoded = ""; String result ="";

		//List<Integer> ids = repository.getRows("filteredallergies");
        List<Long> ids = repository.getRows("filteredAllergiesDelta");

        if (ids.isEmpty()) {return "1";}

		String url = baseURL + "AllergyIntolerance";

		ResultSet rs;
		Long id = 0L; Integer j = 0;

		//Integer nor = 0;
		String nor = "0";
		String allergyname =""; String snomedcode=""; String clineffdate="";
		String location=""; Integer typeid = 4; String putloc=""; String deducted="";
		String deceased="";

		while (ids.size() > j) {

			if (isTrue(repository.Stop())) {
				System.out.println("STOPPING ALLERGY");
				return "1";
			}

			id = ids.get(j);

			//rs = repository.getAllergyIntoleranceRS(id);
			result = repository.getAllergyIntoleranceRS(Long.toString(id));

			if (result.length()>0)
			{
				String[] ss = result.split("\\~");
				//nor = Integer.parseInt(ss[0]);
				nor = ss[0];
				clineffdate=ss[1];
				allergyname=ss[2];
				snomedcode=ss[3];

				/*
				deceased = repository.Deceased(nor,"Allergy");
				deducted = repository.Deducted(nor,"Allergy");
				if (deducted.equals("1") || deceased.equals("1")) {
					System.out.println("Allergy - Patient has died or has been deducted " + nor);
					repository.PurgetheQueue(id, "AllergyIntolerance");
					j++;
					continue;
				}
				*/

				deducted = repository.InCohort(nor);
				if (deducted.equals("0")) {
					System.out.println("Allergy - Patient not in cohort (probably deducted)");
					repository.PurgetheQueue(id.toString(), "AllergyIntolerance");
					j++;
					continue;
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

				putloc = repository.getLocation(Long.toString(id), "AllergyIntolerance");

				encoded = getAllergyResource(nor,clineffdate,allergyname,snomedcode,location,Long.toString(id),putloc);

				LHShttpSend send = new LHShttpSend();
				Integer httpResponse = send.Post(repository,id.toString(), "", url, encoded, "AllergyIntolerance", nor, typeid);
				//if (httpResponse == 401) {return "401, aborting";}
				if (!repository.outputFHIR.isEmpty()) {j++; continue;}
				if (httpResponse == 401 || httpResponse == 0) {return "1";}
			}

			j++;
		}
		return "0";
	}
}