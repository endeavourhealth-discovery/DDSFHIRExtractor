package org.endeavourhealth.certchecker;

import org.endeavourhealth.certchecker.repository.Repository;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import org.json.*;

public class CertChecker implements AutoCloseable {

    private final Repository repository;

    public CertChecker(final Properties properties) throws Exception {
        this(properties, new Repository(properties));
    }

    public CertChecker(final Properties properties, final Repository repository) {
        this.repository = repository;
    }

   public void export() throws Exception {

       this.repository.token = GetToken(this.repository);

       String baseURL = this.repository.baseURL;

       String response = TestCert(repository.token, baseURL + "Patient/");

       if (response == "invalid-cert" || response == "?") {
           System.out.println("invalid-cert or no response from FHIR endpoint");
       }
   }

    public boolean CheckCert(Certificate[] certs)
    {
        try {
            for (Certificate cert : certs) {
                //System.out.println("Certificate is: " + cert);
                if(cert instanceof X509Certificate) {
                    try {
                        ( (X509Certificate) cert).checkValidity();
                        System.out.println("Certificate is active for current date");

                        /*
                        System.out.println(((X509Certificate) cert).getSigAlgName());
                        System.out.println(((X509Certificate) cert).getVersion());
                        System.out.println(((X509Certificate) cert).getSigAlgOID());
                        System.out.println(cert);
                         */

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
    public String GetTLS(String url, String token)
    {
        try {
            URL obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            con.setRequestMethod("GET");

            con.setRequestProperty("Content-Type","application/json");
            con.setRequestProperty("Authorization","Bearer "+token);

            int responseCode = con.getResponseCode();

            Certificate[] certs = con.getServerCertificates();
            boolean valid = CheckCert(certs);

            if (valid == false) {return "invalid-cert";}

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

    private String TestCert(String token, String url)
    {
        // test that the cert is valid (only done once at the start of the process)
        String response = GetTLS(url, token);
        return response;
    }

    private String GetToken(org.endeavourhealth.certchecker.repository.Repository repository)
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

            JSONObject json = new JSONObject(response.toString());
            //System.out.println(json.getString("access_token"));

            token = json.getString("access_token");

            return token;
        }catch(Exception e){
            System.out.println(e);
            return "?";

        }
    }
    @Override
    public void close() throws Exception {
        repository.close();
    }
}