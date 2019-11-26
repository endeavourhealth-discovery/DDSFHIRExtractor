package org.endeavourhealth.fihrexporter.resources;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.mysql.cj.jdbc.MysqlDataSource;
import com.sun.org.apache.xml.internal.serialize.LineSeparator;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.fihrexporter.repository.Repository;
import org.hl7.fhir.dstu3.model.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.endeavourhealth.fihrexporter.send.*;
import org.json.*;

import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import java.net.URL;
//import java.security.cert.X509Certificate;
//import java.security.cert.CertificateExpiredException;
import java.security.cert.Certificate;

public class LHSTest {

    public void getConfig()
    {
        String conStr = ConfigManager.getConfiguration("database","knowdiabetes");
        //String conStr = ConfigManager.getConfiguration("global","slack");
        System.out.println(conStr);
    }

    public void TestDelete(Repository repository, Integer anid, String resource, Integer patientid, Integer type)
    {
        LHShttpSend send = new LHShttpSend();
        String token = send.GetToken(repository);
        repository.token = token;
        // send.DeleteTLS(repository, 22232, "Organization");
        send.Delete(repository, anid, resource, patientid, type);
    }

    public void DeleteObservation(Repository repository)
    {
        LHShttpSend send = new LHShttpSend();
        send.DeleteObservation(repository,133722,"Observation",133675,11);
    }

    public String TestCert(String token, String url)
    {
        // test that the cert is valid (only done once at the start of the process)
        String response = GetTLS(url, token, true);
        return response;
    }

    public String GetToken(Repository repository)
    {
        LHShttpSend send = new LHShttpSend();
        String token = send.GetToken(repository);
        System.out.print(token);
        return token;
    }

    public void GetPatients(Repository repository)
    {
        String token = GetToken(repository);
        // String url = "https://dhs-fhir-test.azurehealthcareapis.com/Patient/bb1acdff-d9d9-4927-9769-1701ec9812e5";
        String url = "https://dhs-fhir-test.azurehealthcareapis.com/Patient";
        String response = GetTLS(url, token, false);
        // System.out.println(response);

        FhirContext ctx = FhirContext.forDstu3();
        IParser parser = ctx.newJsonParser();

        try {
            JSONObject jsonObj = new JSONObject(response);
            System.out.println("test");

            String zurl = ""; String rel = ""; String o = "";
            Integer s = 0;

            if (jsonObj.has("entry")==false) {
                System.out.println("its not a bundle");
                o = jsonObj.toString();
                Patient patient = parser.parseResource(Patient.class, o);
                List<Address> addresses = patient.getAddress();
                Address address = addresses.get(0);
                System.out.println(address.getLine().size());
                s = address.getLine().size()-1;
                for ( int i=0 ; i<=s; i++) {
                    System.out.println(address.getLine().get(i).getValue());
                }
                return;
            }

            if (jsonObj.has("link")==true) {
                JSONArray link = jsonObj.getJSONArray("link");
                for (int it = 0; it < link.length(); it++) {
                    String jstuff = link.get(it).toString();
                    System.out.println(jstuff);
                    JSONObject jsonObject = new JSONObject(jstuff);
                    System.out.println(jsonObject.get("url"));
                    System.out.println(jsonObject.get("relation"));
                }
            }
            JSONArray resources = jsonObj.getJSONArray("entry");
            for (int it = 0; it < resources.length(); it++) {
                JSONObject resource = resources.getJSONObject(it).getJSONObject("resource");
                o = resource.toString();
                Patient patient = parser.parseResource(Patient.class, o);
                System.out.println("Patient Last Name: " + patient.getName().get(0).getFamily());
                System.out.println("Patient First Name: " + patient.getName().get(0).getGiven().get(0));

                //SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                //String dateString = format.format( new Date()   );
                //Date   date       = format.parse (patient.getBirthDate().toString());
                //System.out.println(date.toString());


                //format.parse(patient.getBirthDate().toString());
                //System.out.println(patient.getBirthDateElement().toHumanDisplayLocalTimezone());

                String dob = patient.getBirthDate().toString();
                DateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
                Date date;
                date = (Date)formatter.parse(dob);

                //System.out.println(date);

                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                String formatedDate = cal.get(Calendar.DATE) + "/" +
                        (cal.get(Calendar.MONTH) + 1) +
                        "/" +         cal.get(Calendar.YEAR);
                System.out.println("formatedDate : " + formatedDate);;

                List<Address> addresses = patient.getAddress();
                Address address = addresses.get(0);
                System.out.println(address.getLine().size());

                System.out.println(patient.getId());

                //for ( int i=0 ; i<=(address.getLine().size()-1); i++) {
                //    System.out.println(address.getLine().get(i).getValue());
                //}

                s = address.getLine().size()-1;
                for ( int i=0 ; i<=s; i++) {
                    System.out.println(address.getLine().get(i).getValue());
                }

                //    Address address = addresses.get(i);
                //    System.out.println(address.getLine().get(i).getValue());
                    //System.out.println(address.getLine().get(1).getValue());
                //}

                //System.out.println(patient.getBirthDate());
                //patient.getAddress().get(0).getLine().
                // patient.address
            }

        }catch(Exception e){
            System.out.println(e);
        }
    }

    private String GetTLS(String url, String token, boolean testCert)
    {
        try {
            URL obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            con.setRequestMethod("GET");

            con.setRequestProperty("Content-Type","application/json");
            con.setRequestProperty("Authorization","Bearer "+token);

            int responseCode = con.getResponseCode();

            if (testCert == true) {
                LHShttpSend send = new LHShttpSend();
                Certificate[] certs = con.getServerCertificates();
                boolean valid = send.CheckCert(certs);
                if (valid == false) {return "invalid-cert";}
            }

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

    private void ReconcileTables(Repository repository, String table, String resource) throws SQLException {
        // tables:
        // filteredMedicationsDelta
        // filteredAllergiesDelta
        // filteredPatientsDelta
        List<Integer> ids = repository.getRows(table);
        Integer j = 0; Integer id = 0;
        String location="";
        while (ids.size() > j) {
            id = ids.get(j);
            location = repository.getLocation(id, resource);
            if (location.length()==0) {
                System.out.println(id+" "+resource);
            }
            j++;
        }
    }

    public void ReconcileOtherTables(Repository repository)  throws SQLException {
        ReconcileTables(repository,"filteredMedicationsDelta","MedicationStatement");
        ReconcileTables(repository,"filteredAllergiesDelta","AllergyIntolerance");
        ReconcileTables(repository,"filteredPatientsDelta","Patient");
    }

    public void ReconcileObservations(Repository repository) throws SQLException {
        List<Integer> ids = repository.getRows("filteredObservationsDelta");
        Integer j = 0; Integer id = 0;
        String location="";
        while (ids.size() > j) {
            id = ids.get(j);
            location = repository.getLocation(id, "Observation");
            if (location.length()==0) {
                // is it a Tracker observation? (systolic for diastolic?)
                location = repository.getLocation(id, "Tracker");
                if (location.length()==0) {
                    System.out.println(id+" Obs");
                }
            }
            j++;
        }
    }

    public void TestObsNotFound(Repository repository) throws SQLException {
        List<Integer> ids = repository.getRows("filteredObservationsDelta");
        Integer j = 0; Integer id = 0;
        String result = "";
        while (ids.size() > j) {
            id = ids.get(j);

            result = repository.getObservationRS(id);
            System.out.println(result);

            if (result.length()==0) {
                System.out.println(id);
                Scanner scan = new Scanner(System.in);
                System.out.print("Press any key to continue . . . ");
                scan.nextLine();
            }

            j++;
        }
    }

	public void Run(Repository repository) throws SQLException {
		String result="";

		Integer nor = 0;
		String snomedcode = ""; String drugname = "";

		String dose = ""; String quantityvalue; String quantityunit = "";
		String clinicaleffdate = ""; String location = ""; Integer typeid = 10;
		Integer id = 0;

		result = repository.getMedicationStatementRS(14189472);

		if (result.length()>0) {

			System.out.println(result);

			String[] ss = result.split("\\`");
			nor = Integer.parseInt(ss[0]);
			snomedcode = ss[1];
			drugname = ss[2];

			boolean prev = repository.PreviouslyPostedId(nor, "Patient");

			prev = repository.PreviouslyPostedCode(snomedcode,"Medication");

			location = repository.getLocation(nor, "Patient");

			String rxref = repository.GetMedicationReference(snomedcode);

			dose=ss[3]; quantityvalue=ss[4]; quantityunit=ss[5]; clinicaleffdate=ss[6]; id= Integer.parseInt(ss[7]);

		}

	}
}