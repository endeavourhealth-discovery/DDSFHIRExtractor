package org.endeavourhealth.fihrexporter.resources;

import ca.uhn.fhir.context.FhirContext;
import org.endeavourhealth.fihrexporter.send.LHShttpSend;
import org.hl7.fhir.dstu3.model.*;

import org.endeavourhealth.fihrexporter.repository.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LHSMedication {

	// should really be in a different class
	private static CodeableConcept addCodeableConcept(String snomed, String term)
	{
		CodeableConcept code = new CodeableConcept();
		code.addCoding()
				.setCode(snomed)
				.setDisplay(term)
				.setSystem("http://snomed.info/sct");

		return code;
	}

	private String getMedicationResource(String snomedcode, String term)
	{
		FhirContext ctx = FhirContext.forDstu3();

		//Medication medication = null;

		Medication medication = new Medication();

		medication.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-Medication-1");

		CodeableConcept code = addCodeableConcept(snomedcode, term);
		medication.setCode(code);

		String encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(medication);

		return encoded;
	}

	public String Run(Repository repository, String snomedcode, String drugname, String baseURL)  throws SQLException
	{
		String encoded = "";
		//String url = "http://apidemo.discoverydataservice.net:8080/fhir/STU3/Medication";
		String url = baseURL+"Medication";

		encoded = getMedicationResource(snomedcode, drugname);

		// post
		LHShttpSend send = new LHShttpSend();
		Integer httpResponse = send.Post(repository, 0, snomedcode, url, encoded, "Medication", 0, 0);

		return encoded;
	}
}