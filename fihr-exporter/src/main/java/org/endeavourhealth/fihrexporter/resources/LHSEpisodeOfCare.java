package org.endeavourhealth.fihrexporter.resources;

import ca.uhn.fhir.context.FhirContext;
import org.endeavourhealth.fihrexporter.send.LHShttpSend;
import org.hl7.fhir.dstu3.model.*;
import java.sql.SQLException;
import org.endeavourhealth.fihrexporter.repository.Repository;

import java.text.SimpleDateFormat;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

public class LHSEpisodeOfCare {
	private static String GetEpisodeOfCareResource(Integer id, Integer nor, String StartDate, String EndDate, String putloc, String PatientRef) {
		FhirContext ctx = FhirContext.forDstu3();
		EpisodeOfCare eoc = new EpisodeOfCare();

		if (putloc.length() > 0) {
			eoc.setId(putloc);
		}

		eoc.addIdentifier()
				.setSystem("https://discoverydataservice.net")
				.setValue(id.toString());

		eoc.setStatus(EpisodeOfCare.EpisodeOfCareStatus.FINISHED);

		Period period = new Period();
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			period.setStart(format.parse(StartDate));
			period.setEnd(format.parse(EndDate));
		} catch (Exception e) {
		}

		eoc.setPeriod(period);

		eoc.setPatient(new Reference("Patient/" + PatientRef));

		String encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(eoc);
		return encoded;
	}

	public String Run(Repository repository, Integer id, Integer nor, String StartDate, String EndDate, String baseURL) throws SQLException {
		if (isTrue(repository.Stop())) {
			System.out.println("STOPPING EOC");
			return "1";
		}

		/*
		boolean prev = repository.PreviouslyPostedId(nor, "Patient");
		if (prev == false) {
			LHSPatient patient = new LHSPatient();
			patient.RunSinglePatient(repository, nor, baseURL);
		}
		 */

		String location = repository.getLocation(nor, "Patient");
		if (location.length() == 0) {
			System.out.println("Unable to find patient " + nor);
			return "0";
		}

		String putloc = repository.getLocation(id, "EpisodeOfCare");
		String encoded = GetEpisodeOfCareResource(id, nor, StartDate, EndDate, putloc, location);
		System.out.println(encoded);

		LHShttpSend send = new LHShttpSend();

		String url = baseURL + "EpisodeOfCare";
		Integer typeid=6; // see EpisodeOfCare trigger
		Integer httpResponse = send.Post(repository, id, "", url, encoded, "EpisodeOfCare", nor, typeid);
		if (httpResponse == 401 || httpResponse == 0) {return "1";}

		return "0";
	}
}