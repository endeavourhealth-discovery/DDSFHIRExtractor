package org.endeavourhealth.fihrexporter.resources;

import ca.uhn.fhir.parser.IParser;
import com.mysql.cj.protocol.Resultset;
import org.endeavourhealth.fihrexporter.repository.Repository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.security.cert.Certificate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.context.FhirContext;
import org.endeavourhealth.fihrexporter.send.LHShttpSend;
import org.hl7.fhir.dstu3.model.*;

import org.endeavourhealth.fihrexporter.resources.LHSOrganization;
import org.hl7.fhir.dstu3.model.codesystems.AddressUse;

import javax.net.ssl.HttpsURLConnection;

import java.io.File;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

public class LHSPatient {

	private static String getPatientResource(String PatId, String nhsNumber, String dob, String dod, String add1, String add2, String add3, String add4, String city, String startdate, String gender, String title, String firstname, String lastname, String telecom, String orglocation, String postcode, String putloc, String adduse, String curraddid, String otheraddresses, String deducted, String vids)
	{
		FhirContext ctx = FhirContext.forDstu3();

		Patient patient = new Patient();

		if (putloc.length()>0) {
			patient.setId(putloc);
		}

		patient.addIdentifier()
				.setSystem("https://discoverydataservice.net")
				.setValue(PatId.toString());

		patient.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-Patient-1");

		// nhsNumber
		Identifier nhs = patient.addIdentifier()
				.setSystem("https://fhir.nhs.uk/Id/nhs-number")
				.setValue(nhsNumber);
		CodeableConcept code = new CodeableConcept();
		code.addCoding()
				.setCode("01")
				.setSystem("https://fhir.hl7.org.uk/STU3/CodeSystem/CareConnect-NHSNumberVerificationStatus-1");

		nhs.addExtension()
				.setUrl("https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-NHSNumberVerificationStatus-1")
				.setValue(code);

		if (gender.equals("Other")) {
			patient.setGender(Enumerations.AdministrativeGender.OTHER);
		}
		if (gender.equals("Male")) {
			patient.setGender(Enumerations.AdministrativeGender.MALE);
		}
		if (gender.equals("Female")) {
			patient.setGender(Enumerations.AdministrativeGender.FEMALE);
		}
		if (gender.equals("Unknown")) {
			patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
		}

		patient.addName()
				.setFamily(lastname)
				.addPrefix(title)
				.addGiven(firstname)
				.setUse(HumanName.NameUse.OFFICIAL);

		// virtucare.com ids
		/*
		if (vids.length()>0) {
			String[] ss = vids.split("\\~");
			String z = ""; String vid = "";
			for (int i = 0; i < ss.length; i++) {
				vid = ss[i];
				patient.addIdentifier()
						.setSystem("http://vitrucare.com/Id/aadb2c_id")
						.setValue(vid);
			}
		}
		*/

		if (vids.length()>0) {
			String[] ss = vids.split("\\~");
			String vid = ss[0];
			Identifier aadb2c = patient.addIdentifier()
					.setSystem("http://vitrucare.com/Id/aadb2c_id")
					.setValue(vid);
			CodeableConcept b2cCode = new CodeableConcept();
			b2cCode.addCoding()
					.setCode("3")
					.setSystem("http://vitrucare.com/StructureDefinition/Extension-aadb2cLevelStatus");
			aadb2c.addExtension()
					.setUrl("http://vitrucare.com/StructureDefinition/Extension-aadb2cLevelStatus")
					.setValue(b2cCode);
		}

		// contact_type`contact_use`contact_value|
		if (telecom.length()>0) {
			String[] ss = telecom.split("\\|");
			String z = "";
			for (int i = 0; i < ss.length; i++) {
				z = ss[i];
				String[] contact = z.split("\\`");
				ContactPoint t = new ContactPoint();

				t.setValue(contact[0]);

				if (contact[2].equals("Mobile")) t.setUse(ContactPoint.ContactPointUse.MOBILE);
				if (contact[2].equals("Home")) t.setUse(ContactPoint.ContactPointUse.HOME);

				if (contact[1].equals("Email")) t.setSystem(ContactPoint.ContactPointSystem.EMAIL);
				if (contact[1].equals("Phone")) t.setSystem(ContactPoint.ContactPointSystem.PHONE);

				patient.addTelecom(t);
			}
		}

		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			patient.setBirthDate(format.parse(dob));
		} catch (Exception e) {
		}

		try {
			patient.setDeceased(new DateTimeType(dod));
		} catch (Exception e) {
		}

		// current address
		Address address = new Address();

		if (adduse.equals("1335358")) {address.setUse(Address.AddressUse.HOME);}
		if (adduse.equals("1335360")) {address.setUse(Address.AddressUse.TEMP);}
		if (adduse.equals("1335361")) {address.setUse(Address.AddressUse.OLD);}

		address.addLine(add1);
		address.addLine(add2);
		address.addLine(add3);
		address.addLine(add4);
		address.setPostalCode(postcode);
		address.setCity(city);

		patient.addAddress(address);

		// add1`add2`add3`add4`city`postcode`useconceptid| <= alternative addresses
		if (otheraddresses.length()>0) {
			String[] ss = otheraddresses.split("\\|");
			String z = "";
			for (int i = 0; i < ss.length; i++) {
				z = ss[i];
				String[] zaddress = z.split("\\`");
				Address t = new Address();
				if (zaddress[6].equals("1335358")) {t.setUse(Address.AddressUse.HOME);}
				if (zaddress[6].equals("1335360")) {t.setUse(Address.AddressUse.TEMP);}
				if (zaddress[6].equals("1335361")) {t.setUse(Address.AddressUse.OLD);}
				t.addLine(zaddress[0]);
				t.addLine(zaddress[1]);
				t.addLine(zaddress[2]);
				t.addLine(zaddress[3]);
				t.setPostalCode(zaddress[5]);
				t.setCity(zaddress[4]);
				patient.addAddress(t);
			}
		}
		/*
		patient.addAddress()
				.addLine(add1)
				.addLine(add2)
				.addLine(add3)
				.addLine(add4)
				.setPostalCode(postcode)
				.setCity(city);
		*/

		Extension registration = patient.addExtension();
		registration.setUrl("https://fhir.nhs.uk/STU3/StructureDefinition/Extension-CareConnect-GPC-RegistrationDetails-1");

		Period period = new Period();
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			period.setStart(format.parse(startdate));
		} catch (Exception e) {
		}

		patient.setManagingOrganization(new Reference("Organization/" + orglocation));

		if (deducted.equals("1")) {patient.setActive(false);}
		else {patient.setActive(true);}

		String encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);

		return encoded;
	}

	public String GetTLS(String url, String token)
	{
		try {
			URL obj = new URL(url);
			HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

			con.setRequestMethod("GET");

			con.setRequestProperty("Content-Type","application/json");
			con.setRequestProperty("Authorization","Bearer "+token);

			int responseCode = con.getResponseCode();

			System.out.println("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream()));

			String output;
			// StringBuffer response = new StringBuffer();
			String response = "";

			while ((output = in.readLine()) != null) {
				// response.append(output);
				response = response + output;
			}
			in.close();

			return response;
		}catch(Exception e){
			System.out.println(e);
			return "?";
		}
	}

	public String GetVirtuCareIds(Repository repository, String baseURL, String location)
	{
		String url = baseURL + "Patient/"+location;
		String response = GetTLS(url, repository.token);

		if (response.equals("?")) {return "?";}

		FhirContext ctx = FhirContext.forDstu3();
		IParser parser = ctx.newJsonParser();
		Patient patient = parser.parseResource(Patient.class, response);

		Integer s = 0; String vids = "";

		List<Identifier> identifiers = patient.getIdentifier();
		s = identifiers.size()-1; String system = "";
		for ( int i=0 ; i<=s; i++) {

			//System.out.println(identifiers.get(i).getValue());
			//System.out.println(identifiers.get(i).getSystem());

			system = identifiers.get(i).getSystem();
			if (system.contains("vitrucare.com")) {
				vids = vids + identifiers.get(i).getValue() + "~";
			}

		}

		return vids;
	}

	public String RunSinglePatient(Repository repository, String nor, String baseURL, String deducted)  throws SQLException {
		ResultSet rs; String result;

		result = repository.getPatientRS(nor);

		String url = baseURL + "Patient";

		int j = 0;
		List row;

		String nhsno = ""; String title  = ""; String dob = ""; String dod = ""; String add1 = ""; String add2 = "";
		String add3 = ""; String add4 = ""; String city = ""; String startdate = ""; String gender = "";
		String contacttype = ""; String contactuse = ""; String contactvalue = "";
		String firstname =""; String lastname = ""; String telecom = ""; String query = "";
		String odscode = ""; String orgname = ""; String orgpostcode = ""; String orgid = "0";
		Integer typeid = 2; String encoded = ""; String postcode = ""; String putloc="";
		String adduse = ""; String curraddid = ""; String otheraddresses = "";

		String vids = "";

		boolean prev; String orglocation;

        // System.out.println(result);

		if (result.length()>0) {

			String[] ss = result.split("¬", -1);

			//orgid = rs.getInt("organization_id");
			orgid = ss[19];

			// has the organization been sent for this patient?
			prev = repository.PreviouslyPostedId(orgid, "Organization");

			if (prev == false) {
				LHSOrganization organization = new LHSOrganization();
				organization.Run(repository, orgid, baseURL);
			}

			// get the http location of the organization_id
			orglocation = repository.getLocation(orgid, "Organization");
			if (orglocation.length() == 0) {
				System.out.println("Cannot find patients " + nor + " "+ orgid + "organization?");
				System.out.println(result);
				return "1";
			}

			nhsno=ss[0]; dob=ss[20]; odscode=ss[1]; orgname=ss[2]; orgpostcode=ss[3]; telecom=ss[4];
			dod=ss[5]; add1=ss[6]; add2=ss[7]; add3=ss[8]; add4=ss[9]; city=ss[10]; gender=ss[11];
			contacttype=ss[12]; contactuse=ss[13]; contactvalue=ss[14]; title=ss[15]; firstname=ss[16];
			lastname=ss[17]; startdate=ss[18]; postcode=ss[21]; adduse=ss[22]; curraddid=ss[23];
			otheraddresses=ss[24];

			if (nhsno.isEmpty()) {
				System.out.println("null nhs number: " + nor);
				return "0";
			}

			putloc = repository.getLocation(nor, "Patient");

			if (!putloc.isEmpty()) {
				vids = GetVirtuCareIds(repository, baseURL, putloc);
				if (vids.equals("?")) {
					System.out.println("patient "+putloc+" does not exist in the FHIR repository");
					String OS = System.getProperty("os.name").toLowerCase();
					if (OS.indexOf("win") >= 0) return "1";
					// stop the fhir extractors!
					try {
						File file = new File("/tmp/stop.txt");
						file.createNewFile();
					}catch(Exception e)
					{ System.out.println(e);}
					return "1";
				}
			}

			encoded = getPatientResource(nor, nhsno, dob, dod, add1, add2, add3, add4, city, startdate, gender, title, firstname, lastname, telecom, orglocation, postcode, putloc, adduse, curraddid, otheraddresses, deducted, vids);

			LHShttpSend send = new LHShttpSend();
			Integer httpResponse = send.Post(repository, nor, "", url, encoded, "Patient", nor, typeid);
            // if (httpResponse == 401) {
			if (!repository.outputFHIR.isEmpty()) {return "0";}
            if (httpResponse == 401 || httpResponse == 0) {return "1";}
		}
		return "0";
	}

	public String Run(Repository repository, String baseURL) {
		try {
			// List<List<String>> patient = repository.getPatientRows();

			List<Long> patient = repository.getPatientRows();

			//String deducted = "";
			//deducted = repository.Deducted(23123,"Patient");

			if (patient.isEmpty()) {return "1";}

			int j = 0;
			List row;

			Long nor = 0L;
			//Integer nor;
			String deducted = ""; String deceased = ""; String result = "";
			String incohort = "";

			String url = baseURL + "Patient";

			while (patient.size() > j) {

				if (isTrue(repository.Stop())) {
					System.out.println("STOPPING PATIENT");
					return "1";
				}

				// nor = patient.get(j).get(1);
				nor = patient.get(j);
				System.out.println(nor);

				// has the patient been deducted?
				// deducted = repository.Deducted(nor,"Patient");

                incohort = repository.InCohort(Long.toString(nor));
                deducted = "1";
                if (incohort.equals("1")) {deducted = "0";}

				result = RunSinglePatient(repository, Long.toString(nor), baseURL, deducted);
                if (result.equals("1")) {return "1";}

                // so deceased it makes it into the logs (still need to send Patient resource)
				// deceased = repository.Deceased(nor, "Patient");

                /*
                if (deducted.equals("1")) {

                    System.out.println("Patient - Patient has been deducted!");

                    String eocdata = repository.DeductedData(nor);

                    if (!eocdata.isEmpty()) {
                        // send an episode of care resource
                        String[] ss = eocdata.split("\\~", -1);
                        Integer id = Integer.parseInt(ss[0]); String StartDate = ss[1]; String EndDate = ss[2];
                        LHSEpisodeOfCare eoc = new LHSEpisodeOfCare();
                        result = eoc.Run(repository, id, nor, StartDate, EndDate, baseURL);
                        if (result.equals("1")) {return "1";}
                    }

                    repository.PurgetheQueue(nor, "Patient");
                    j ++;
                    continue;
                }
                 */

				j++;
			}
		}catch(Exception e){ System.out.println(e);}
		return "0";
	}
}