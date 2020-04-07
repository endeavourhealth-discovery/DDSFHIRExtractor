package org.endeavourhealth.fihrexporter.resources;

import ca.uhn.fhir.context.FhirContext;
import org.endeavourhealth.fihrexporter.send.LHShttpSend;
import org.hl7.fhir.dstu3.model.*;

import org.endeavourhealth.fihrexporter.repository.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

public class LHSMedicationStatement {
	private Dosage addDosage(String dosagetext, String qtyvalue, String qtyunit)
	{
		Dosage dose = null;

		dose = new Dosage();
		dose.setText(dosagetext);

		if ( (!qtyvalue.isEmpty()) & (!qtyunit.isEmpty()) ) {
			dose.setDose(new SimpleQuantity()
					//.setValue(Integer.parseInt(qtyvalue))
					.setValue(Double.parseDouble(qtyvalue))
					.setUnit(qtyunit)
			);
		}

		return dose;
	}

	private String GetMedicationStatementResource(Integer patientid, String dose, String quantityvalue, String quantityunit, String clinicaleffdate, String medicationname, String snomedcode, String PatientRef, String rxref, Integer ddsid, String putloc)
	{
		FhirContext ctx = FhirContext.forDstu3();

		// MedicationStatement rxstatement = null;

		MedicationStatement rxstatement = new MedicationStatement();

		if (putloc.length()>0) {
			rxstatement.setId(putloc);
		}

		rxstatement.addIdentifier()
				.setSystem("https://discoverydataservice.net")
				.setValue(ddsid.toString());

		rxstatement.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-MedicationStatement-1");

		// this needs to be a switch statement using ?
		rxstatement.setStatus(MedicationStatement.MedicationStatementStatus.ACTIVE);

		//rxstatement.setSubject(new Reference("/api/Patient/33"));

		Period period = new Period();
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			period.setStart(format.parse(clinicaleffdate));
		} catch (Exception e) {
		}
		rxstatement.setEffective(period);

		rxstatement.setTaken(MedicationStatement.MedicationStatementTaken.UNK);

		rxstatement.setSubject(new Reference("Patient/" + PatientRef));

		rxstatement.setMedication(new Reference("Medication/" + rxref)
				.setDisplay(medicationname));

		ArrayList dosages=new ArrayList();

		Dosage doseage = addDosage(dose, quantityvalue, quantityunit);
		dosages.add(doseage);
		rxstatement.setDosage(dosages);

		String encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(rxstatement);

		return encoded;
	}

	public String Run(Repository repository, String baseURL)  throws SQLException
	{
		String encoded = "";
		Integer id = 0;

		//List<Integer> ids = repository.getRows("filteredmedications");
        List<Integer> ids = repository.getRows("filteredMedicationsDelta");

        if (ids.isEmpty()) {return "1";}

		ResultSet rs; String result;

		Integer nor = 0;
		String snomedcode = ""; String drugname = ""; String deceased = "";

		String dose = ""; String quantityvalue; String quantityunit = "";
		String clinicaleffdate = ""; String location = ""; Integer typeid = 10;

		String url = baseURL + "MedicationStatement"; String putloc=""; String deducted="";

		Integer j = 0;

		while (ids.size() > j) {

			if (isTrue(repository.Stop())) {
				System.out.println("STOPPING RX");
				return "1";
			}

			id = ids.get(j);
			System.out.println(id);

			result = repository.getMedicationStatementRS(id);

			if (result.length()>0) {
				String[] ss = result.split("\\`");
				nor = Integer.parseInt(ss[0]);
				snomedcode = ss[1];
				drugname = ss[2];

				/*
                deceased = repository.Deceased(nor,"MedicationStatement");
				deducted = repository.Deducted(nor,"MedicationStatement");

				if (deducted.equals("1") || deceased.equals("1")) {
					System.out.println("Rx - Patient has died or been deducted" + nor);
					repository.PurgetheQueue(id, "MedicationStatement");
					j++;
					continue;
				}
				*/

				deducted = repository.InCohort(nor);
				if (deducted.equals("0")) {
					System.out.println("Rx - Patient not in cohort (probably deducted)");
					repository.PurgetheQueue(id, "MedicationStatement");
					j++;
					continue;
				}

				boolean prev = repository.PreviouslyPostedId(nor, "Patient");
				if (prev==false) {
					LHSPatient patient = new LHSPatient();
					patient.RunSinglePatient(repository, nor, baseURL, deducted);
				}

				prev = repository.PreviouslyPostedCode(snomedcode,"Medication");
				if (prev == false) {
					LHSMedication medication = new LHSMedication();
					medication.Run(repository, snomedcode, drugname, baseURL);
				}

				location = repository.getLocation(nor, "Patient");
				if (location.length() == 0)
				{
					System.out.println("Unable to find patient " + nor);
					j++;
					continue;
				}

				String rxref = repository.GetMedicationReference(snomedcode);
				if (rxref.length() == 0)
				{
					System.out.println("Unable to find medication reference" + snomedcode);
					j++;
					continue;
				}

				dose=ss[3]; quantityvalue=ss[4]; quantityunit=ss[5]; clinicaleffdate=ss[6]; id= Integer.parseInt(ss[7]);

				putloc = repository.getLocation(id, "MedicationStatement");

				encoded = GetMedicationStatementResource(nor, dose, quantityvalue, quantityunit, clinicaleffdate, drugname, snomedcode, location, rxref, id, putloc);
				System.out.println(encoded);

				LHShttpSend send = new LHShttpSend();
				Integer httpResponse = send.Post(repository, id, "", url, encoded, "MedicationStatement", nor, typeid);
                //if (httpResponse == 401) {return "401, aborting";}
				if (!repository.outputFHIR.isEmpty()) {j++; continue;}
                if (httpResponse == 401 || httpResponse == 0) {return "1";}
			}

			j++;

			System.out.println(id);
		}

		return "0";
	}
}