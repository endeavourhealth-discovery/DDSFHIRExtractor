package org.endeavourhealth.fihrexporter.send;

import org.apache.commons.io.FileUtils;
import org.endeavourhealth.fihrexporter.repository.Repository;

import javax.net.ssl.HttpsURLConnection;
import java.security.cert.Certificate;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.*;
import java.nio.file.StandardOpenOption;
import org.json.*;

import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.Scanner;
import java.util.UUID;

public class LHShttpSend {

	private static String location = "";

	public boolean CheckCert(Certificate[] certs)
    {
        try {
        for (Certificate cert : certs) {
            //System.out.println("Certificate is: " + cert);
            if(cert instanceof X509Certificate) {
                try {
                    ( (X509Certificate) cert).checkValidity();
                    System.out.println("Certificate is active for current date");
                    //System.out.println(((X509Certificate) cert).getSigAlgName());
                    //System.out.println(((X509Certificate) cert).getVersion());
                    //System.out.println(((X509Certificate) cert).getSigAlgOID());
                    //System.out.println(cert);
                    return true;
                } catch(CertificateExpiredException cee) {
                    System.out.println("Certificate is expired");
                    return false;
                }
            }
        }
        }catch(Exception e) {
            System.out.println(e);
        }
        return false;
    }

	public String GetToken(Repository repository)
	{
		try {

			String tokenurl = repository.tokenurl;
			String clientid = repository.clientid;
			String clientsecret = repository.clientsecret;
			String scope = repository.scope;
			String granttype = repository.granttype;

			String token = "";

			String encoded = "client_id="+clientid+"&client_secret="+clientsecret+"&scope="+scope+"&grant_type="+granttype;

			URL obj = new URL(tokenurl);
			HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type","application/x-www-form-urlencoded");

			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(encoded);

			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			String output="";

			StringBuffer response = new StringBuffer();
			BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));

			while ((output = in.readLine()) != null) {
				response.append(output);
			}
			in.close();

			//printing result from response
			//System.out.println(response.toString());
			System.out.println(response.toString());

			JSONObject json = new JSONObject(response.toString());
			System.out.println(json.getString("access_token"));

			token = json.getString("access_token");

			return token;
		}catch(Exception e){
			System.out.println(e);
			return "?";
		}
	}

	private Integer SendTLS(String url, String method, String encoded, String token)
	{
		try {
			URL obj = new URL(url);
			HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

			con.setRequestMethod(method);

			con.setRequestProperty("Content-Type","application/json");
			con.setRequestProperty("Authorization","Bearer "+token);

			// Send request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(encoded);
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();

			//System.out.println("Response Code : " + responseCode);
			if (responseCode == 401) {return 401;}

			BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream()));
			String output;
			StringBuffer response = new StringBuffer();

			while ((output = in.readLine()) != null) {
				response.append(output);
			}
			in.close();

			//printing result from response
			//System.out.println(response.toString());
			System.out.println(url);
			System.out.println(responseCode);

			if (method == "POST") {location = con.getHeaderField("location");}

			return responseCode;
		}catch(Exception e){
			System.out.println(e);
			return 0;
		}
	}

	private Integer SendHttp(String url, String method, String encoded)
	{
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setRequestMethod(method);

			con.setRequestProperty("Content-Type","application/json");

			// Send request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(encoded);
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();

			System.out.println("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
			String output;
			StringBuffer response = new StringBuffer();

			while ((output = in.readLine()) != null) {
				response.append(output);
			}
			in.close();

			//printing result from response
			System.out.println(response.toString());

			if (method == "POST") {location = con.getHeaderField("location");}

			return responseCode;
		}catch(Exception e){
			System.out.println(e);
			return 0;
		}
	}

	public Integer DeleteTLS(Repository repository, String anId, String resource, String patientid, Integer typeid, String location)
    {
        try {

            URL obj = new URL(repository.getBaseURL()+resource+"/"+location);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            con.setRequestMethod("DELETE");

            con.setRequestProperty("Content-Type","application/json");
            con.setRequestProperty("Authorization","Bearer "+repository.token);

            int responseCode = con.getResponseCode();

            System.out.println("Response Code : " + responseCode);

            return responseCode;
        }catch(Exception e){
            System.out.println(e);
        }
        return 1;
    }

    public Integer DeleteObservation(Repository repository, String anId, String resource, String patientid, Integer typeid)
	{
		// delete the observation in question
		// check parents:
		// get the location
		// get the observation id's for the location
		// add the ids back into the filtered table queue
		try
		{
			Integer responseCode = 0;
			responseCode = Delete(repository, anId, resource, patientid, typeid);

			String ids = "";
			String id = "";
			String loc = repository.getLocationObsWithCheckingDeleted(anId);

			if (loc.length() == 0) {
				return 0;
			}

			if (responseCode.equals(204)) {
				ids = repository.getIdsForLocation(loc);

				String[] ss = ids.split("\\~");
				for (int i = 0; i < ss.length; i++) {
					id = ss[i];
					if (id == anId) continue;
					// has the observation been deleted previously?
					if (repository.getLocation(id, "Observation").length() == 0) continue;
					repository.InsertBackIntoObsQueue(Integer.parseInt(id));
				}
				return responseCode;
			}
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
		return 0;
	}

    public Integer Delete(Repository repository, String anId, String resource, String patientid, Integer typeid)
    {
        Integer responseCode = 0;
        try
        {
            String loc = repository.getLocation(anId, resource);
            if (loc.length() == 0) {
                System.out.println("Unable to find location");
                return 0;
            }

        responseCode = DeleteTLS(repository, anId, resource, patientid, typeid, loc);

        if (responseCode.equals(401))  {
            repository.token = GetToken(repository);
            responseCode = DeleteTLS(repository, anId, resource, patientid, typeid, loc);
        }

        if (responseCode.equals(204)) {
			repository.Audit(anId, "", "DEL:"+resource, responseCode, loc, "", patientid, typeid);
			repository.PurgeTheDeleteQueue(anId, resource);
		}

		return responseCode;
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
        return responseCode;
    }

	public Integer Post(Repository repository, String anId, String strid, String url, String encoded, String resource, String patientid, Integer typeid)
	{
		try {

			String location = ""; String method = "POST";

			//Scanner scan = new Scanner(System.in);
			//System.out.print("Press any key to continue . . . ");
			//scan.nextLine();

			//System.out.println(repository.outputFHIR);

			if (!repository.outputFHIR.isEmpty()) {
				String folder = repository.outputFHIR;

				UUID uuid = UUID.randomUUID();
                String uuidStr = uuid.toString();

				//String file = folder+resource+"-"+anId+strid+".json";
                String file = folder+anId+"-"+resource+"-"+uuidStr+".json";

				boolean FileExists = false;
				Path path = Paths.get(file);
				if (Files.notExists(path)) {FileExists=true;}

				// don't write the files to disk, but use the references table instead
				Files.write(Paths.get(file), encoded.getBytes());

				//location = resource+"-"+anId+strid+".json";
                location = anId+"-"+resource+"-"+uuidStr+".json";

				//if (FileExists==false) repository.Audit(anId, strid, resource, 123, location, encoded, patientid, typeid);
				repository.Audit(anId, strid, resource, 123, location, encoded, patientid, typeid);

				return 0;
			}

			// decide if it's a post or a put?
			if (anId != "0") {location = repository.getLocation(anId, resource);}

			// 10k test
            // location = "";

			// snomed reference?
			if ( (location.length() == 0) & (strid.length()>0) ) {
				location = repository.GetMedicationReference(strid);
			}

			if (location.length() > 0) {
				url = url + "/" + location;
				method = "PUT";
			}

			int responseCode = 0;

			if (url.contains("http:")) {
				responseCode = SendHttp(url, method, encoded);
			}
			if (url.contains("https:")) {
				responseCode = SendTLS(url,method, encoded, repository.token);
				// assume 401 is token expiration (try once again)
				if (responseCode == 401) {
					repository.token = GetToken(repository);
					responseCode = SendTLS(url,method, encoded, repository.token);
				}
			}

			if (method == "PUT") {repository.UpdateAudit(anId, strid, encoded, responseCode, resource);}

			if (method == "POST") {

				System.out.println(LHShttpSend.location);

				String[] ss = LHShttpSend.location.split("/");

				if (LHShttpSend.location.contains("/_history/")) {
					location = ss[ss.length - 3];
				} else {
					location = ss[ss.length - 1];
				}

				repository.Audit(anId, strid, resource, responseCode, location, encoded, patientid, typeid);
			}

			return responseCode;
		}catch(Exception e){
			System.out.println(e);
			return 0;
		}
	}
}