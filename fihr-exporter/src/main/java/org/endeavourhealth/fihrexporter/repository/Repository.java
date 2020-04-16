package org.endeavourhealth.fihrexporter.repository;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.apache.commons.lang3.ObjectUtils;
import org.endeavourhealth.common.config.ConfigManager;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import sun.dc.pr.PRError;

import java.io.File;
import java.sql.*;
import java.time.LocalTime;
import java.util.*;

import static org.apache.commons.lang3.BooleanUtils.isFalse;

import org.endeavourhealth.common.utility.MetricsHelper;

public class Repository {

    private MysqlDataSource dataSource;

    private Connection connection;

    private String baseURL;

    public String outputFHIR; public String dbschema; public String clientid;
    public String clientsecret; public String scope; public String granttype;
    public String tokenurl; public String token; public String runguid;
    public Integer scaletotal; public Integer counting;
    public String config; public String dbreferences;
    public String organization; public String procrun;

    public String testobs;
    public String resendpats;
    public String deletesdone;

    public String testpatget;

    public Repository(Properties properties) throws SQLException {
        init( properties );
    }

    public String TestConnection()  throws SQLException {

        try {
            System.out.println("testing connection");

            //dataSource.setURL("jdbc:mysql://localhost:3306/data_extracts");
            //dataSource.setUser("root");
            //dataSource.setPassword("1qaz1qaz");

            Scanner sc = new Scanner(System.in);
            System.out.println("Enter table");
            String table = sc.next();

            System.out.println("Enter field");
            String field = sc.next();

            //String q = "SELECT * FROM config.config";
            String q = "SELECT * FROM "+table;
            PreparedStatement preparedStatement = connection.prepareStatement(q);
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getString(field));
            }

            //this.connection.close();
        }
        catch(Exception e) {
            System.out.println(e);
        }

        return "test";
    }

    public boolean PreviouslyPostedCode(String code, String resource) throws SQLException {

        //String q = "SELECT * FROM "+dbreferences+".references WHERE strid='" + code + "' AND resource='" + resource +" '";

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return false;}

        String q = "SELECT * FROM "+dbreferences+".references WHERE strid=? AND resource=?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,code);
        preparedStatement.setString(2,resource);

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            preparedStatement.close();
            return true;
        }
        preparedStatement.close();
        return false;
    }

    // select * from information_schema.tables where TABLE_SCHEMA=? and TABLE_NAME=?
    // check if the table exists in a particular schema?
    private boolean ValidateTable(String Schema, String Table)  throws SQLException {
        //return true;

        String q="select * from information_schema.tables where TABLE_SCHEMA=? and TABLE_NAME=?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        preparedStatement.setString(1,Schema);
        preparedStatement.setString(2,Table);

        ResultSet rs = preparedStatement.executeQuery();

        boolean ret=true;
        if (!rs.next()) {System.out.println("Table "+Table+" does not exist"); ret=false;}
        preparedStatement.close();
        return ret;
    }

    private boolean ValidateSchema(String Schema) throws SQLException {
        // return true;

        String q="select * from information_schema.schemata where schema_name=?";
        PreparedStatement preparedStatement = connection.prepareStatement(q);
        preparedStatement.setString(1,Schema);

        ResultSet rs = preparedStatement.executeQuery();

        boolean ret=true;
        if (!rs.next()) {System.out.println("Schema "+Schema+" does not exist"); ret=false;}
        preparedStatement.close();
        return ret;
    }

    public boolean PreviouslyPostedId(Integer id, String resource) throws SQLException {

        //String q = "SELECT * FROM "+dbreferences+".references WHERE an_id='" + id.toString() + "' AND resource='" + resource + " '";

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return false;}

        String q = "SELECT * FROM "+dbreferences+".references WHERE an_id=? AND resource=?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,id.toString());
        preparedStatement.setString(2,resource);

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            preparedStatement.close();
            return true;
        }

        preparedStatement.close();

        return false;
    }

    public String getIdsForLocation(String location)  throws SQLException {
        String ids ="";

        //String q = "SELECT * FROM "+dbreferences+".references WHERE location='" + location + "'";

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "";}

        String q = "SELECT * FROM "+dbreferences+".references WHERE location=?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        preparedStatement.setString(1,location);

        ResultSet rs = preparedStatement.executeQuery();

        while (rs.next()) {
            ids=ids+rs.getInt("an_id")+"~";
        }

        preparedStatement.close();
        return ids;
    }

    public void InsertBackIntoObsQueue(Integer id) throws SQLException {
        // does the id already exist in filteredobservationsdelta?
        // String q ="select id from "+dbreferences+".filteredObservationsDelta where id="+id;

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return;}

        String q ="select id from "+dbreferences+".filteredObservationsDelta where id=?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,id.toString());

        ResultSet rs = preparedStatement.executeQuery();

        Boolean alreadyinq = false;
        if (rs.next()) {
            alreadyinq = true;
        }

        preparedStatement.close();

        if (alreadyinq==true) return;

        //q ="insert into "+dbreferences+".filteredObservationsDelta (id) values(?)";

        q ="insert into "+dbreferences+".filteredObservationsDelta (id) values(?)";

        System.out.println("back into q "+q);

        PreparedStatement preparedStmt = connection.prepareStatement(q);

        //preparedStmt.setInt(1, id);

        preparedStmt.setString(1,id.toString());

        preparedStmt.execute();
        preparedStmt.close();
    }

    public String getLocationObsWithCheckingDeleted(Integer anid) throws SQLException {
        String location = "";

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "";}

        //String q = "SELECT * FROM "+dbreferences+".references WHERE an_id='" + anid + "' AND resource='Observation'";
        String q = "SELECT * FROM "+dbreferences+".references WHERE an_id=? AND resource=?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,anid.toString());
        preparedStatement.setString(2,"Observation");

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) { location =  rs.getString("location"); }

        preparedStatement.close();
        return location;
    }

    public String getLocation(Integer anid, String resource) throws SQLException {
        String location = "";

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "";}

        //String q = "SELECT * FROM "+dbreferences+".references WHERE an_id='" + anid + "' AND resource='" + resource + "'";
        String q = "SELECT * FROM "+dbreferences+".references WHERE an_id=? AND resource=?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,anid.toString());
        preparedStatement.setString(2,resource);

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) { location =  rs.getString("location"); }

        preparedStatement.close();

        // Has the resource been deleted?
        if (location.length()>0) {
            //q = "SELECT * FROM "+dbreferences+".references WHERE an_id='" + anid + "' AND resource='DEL:" + resource + "'";
            q = "SELECT * FROM "+dbreferences+".references WHERE an_id=? AND resource=?";

            preparedStatement = connection.prepareStatement(q);
            preparedStatement.setString(1,anid.toString());
            preparedStatement.setString(2,"DEL:"+resource);


            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                location = "";
            }
            preparedStatement.close();
        }

        return location;
    }

    public String GetMedicationReference(String snomedcode) throws SQLException {
        String location = "";

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "";}

        //String q = "SELECT * FROM "+dbreferences+".references WHERE strid='" + snomedcode + "' AND resource='Medication'";
        String q = "SELECT * FROM "+dbreferences+".references WHERE strid=? AND resource=?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,snomedcode);
        preparedStatement.setString(2,"Medication");

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) { location =  rs.getString("location"); }

        preparedStatement.close();
        return location;
    }

    public void DeleteTracker() throws SQLException
    {
        //String q ="DELETE FROM "+dbreferences+".references where resource ='Tracker'";
        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return;}

        v = ValidateSchema(dbschema);
        if (isFalse(v)) {return;}

        // so that we can stream more than one organization at a time
        //String q ="DELETE FROM "+dbreferences+".references where resource =?";

        String q="DELETE r from "+dbreferences+".references r ";
        q = q+"left outer join "+dbschema+".observation j on r.an_id=j.id ";
        q = q+"where resource='Tracker' and j.organization_id=?";

        PreparedStatement preparedStmt = connection.prepareStatement(q);

        //preparedStmt.setString(1,"Tracker");
        preparedStmt.setString(1,organization);
        preparedStmt.execute();

        preparedStmt.close();
    }

    public void DeleteFileReferences() throws SQLException
    {
        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return;}

        String q ="DELETE FROM "+dbreferences+".references where response = 123";

        PreparedStatement preparedStmt = connection.prepareStatement(q);
        preparedStmt.execute();
    }

    public void PurgetheQueue(Integer anId, String resource) throws SQLException
    {
        // purge the queues
        String table = ""; String q = "";

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return;}

        if (resource=="Patient") {table=dbreferences+".filteredPatientsDelta";}
        //if (resource=="Observation") {table="data_extracts.filteredobservationsdelta";}
        if (resource=="Observation") {table=dbreferences+".filteredObservationsDelta";}
        if (resource=="MedicationStatement") {table=dbreferences+".filteredMedicationsDelta";}
        if (resource== "AllergyIntolerance") {table=dbreferences+".filteredAllergiesDelta";};

        if (table.length()>0) {
            //q = "DELETE FROM " + table + " where id='" + anId + "'";
            q = "DELETE FROM "+table+" where id=?";

            PreparedStatement preparedStmt = connection.prepareStatement(q);

            preparedStmt.setString(1,anId.toString());

            preparedStmt.execute();
            preparedStmt.close();
        }
    }

    public void PurgeTheDeleteQueue(Integer anId, String resource) throws SQLException
    {
        //filteredDeletionsDelta (id, type)
        //patient - 2, observation - 11, allergy - 4, medication - 10

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return;}
        Integer type=0; String q="";

        if (resource.equals("Patient")) {type=2;}
        if (resource.equals("Observation")) {type=11;}
        if (resource.equals("MedicationStatement")) {type=10;}
        if (resource.equals("AllergyIntolerance")) {type=4;}

        if (!type.equals(0)) {
            //q = "DELETE FROM filteredDeletionsDelta where record_id='"+anId+"' AND table_id='"+type+"'";
            q = "DELETE FROM "+dbreferences+".filteredDeletionsDelta where record_id=? AND table_id=?";

            PreparedStatement preparedStmt = connection.prepareStatement(q);

            preparedStmt.setString(1,anId.toString());
            preparedStmt.setString(2,type.toString());

            preparedStmt.execute();
            preparedStmt.close();
        }
    }

    public boolean UpdateAudit(Integer anId, String strid, String encoded, Integer responseCode, String resource) throws SQLException
    {
        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return false;}

        long timeNow = Calendar.getInstance().getTimeInMillis();
        java.sql.Timestamp ts = new java.sql.Timestamp(timeNow);
        String str = ts.toString();

        String q = "";

        encoded = encoded.replaceAll("'","''");

        if (anId != 0) {
            //q = "update "+dbreferences+".references set response = " + responseCode + ", datesent = '"+str+"', json = '"+encoded+"' where an_id = '"+anId+"' and resource='"+resource+"' and response<>'1234'";
            q = "update "+dbreferences+".references set response = ?, datesent = ?, json = ? where an_id = ? and resource=? and response<>'1234'";
            PurgetheQueue(anId, resource);
        }

        if (strid.length() > 0) {
            //q = "update "+dbreferences+".references set response = " + responseCode + ", datesent = '" + str + "', json = '" + encoded + "' where strid = '"+strid+"' and resource='"+resource+"' and response<>'1234'";
            q = "update "+dbreferences+".references set response = ?, datesent = ?, json = ? where strid = ? and resource=? and response<>'1234'";
        }

        //System.out.println(q);

        PreparedStatement preparedStmt = connection.prepareStatement(q);

        preparedStmt.setString(1,responseCode.toString());
        preparedStmt.setString(2,str);
        preparedStmt.setString(3,encoded);
        preparedStmt.setString(5,resource);

        if (anId !=0) {preparedStmt.setString(4,anId.toString());}

        if (strid.length() > 0) {preparedStmt.setString(4,strid);}

        preparedStmt.execute();

        preparedStmt.close();

        return true;
    }

    public boolean AuditDeducted(Integer nor, String reason, String resource) throws SQLException {
        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return false;}

        // already set for this run?
        String q = "SELECT * FROM "+dbreferences+".deducted where patient_id = ? and resource=?";
        PreparedStatement preparedStatement = connection.prepareStatement(q);
        preparedStatement.setInt(1,nor);
        preparedStatement.setString(2,resource);
        ResultSet rs = preparedStatement.executeQuery();
        if (rs.next()) {
            preparedStatement.close();
            return true;
        }
        preparedStatement.close();

        q="insert into "+dbreferences+".deducted (patient_id, reason, resource) values (?,?,?)";
        PreparedStatement preparedStmt = connection.prepareStatement(q);
        preparedStmt.setInt(1, nor);
        preparedStmt.setString(2, reason);
        preparedStmt.setString(3,resource);
        preparedStmt.execute();
        preparedStmt.close();

        return true;
    }

    public boolean Audit(Integer anId, String strid, String resource, Integer responseCode, String location, String encoded, Integer patientid, Integer typeid) throws SQLException
    {

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return false;}

        //String q = "insert into "+dbreferences+".references (an_id,strid,resource,response,location,datesent,json,patient_id,type_id,runguid) values(?,?,?,?,?,?,?,?,?,?)";
        String q = "insert into "+dbreferences+".references (an_id,strid,resource,response,location,datesent,json,patient_id,type_id,runguid) values(?,?,?,?,?,?,?,?,?,?)";

        // connection.setSchema(dbreferences);

        PreparedStatement preparedStmt = connection.prepareStatement(q);

        preparedStmt.setInt(1, anId);
        preparedStmt.setString(2, strid);

        preparedStmt.setString(3, resource);

        preparedStmt.setString(4, responseCode.toString());
        preparedStmt.setString(5, location);

        long timeNow = Calendar.getInstance().getTimeInMillis();
        java.sql.Timestamp ts = new java.sql.Timestamp(timeNow);
        preparedStmt.setTimestamp(6, ts);

        preparedStmt.setString(7, encoded);

        preparedStmt.setInt(8, patientid);

        preparedStmt.setInt(9, typeid);

        preparedStmt.setString(10, this.runguid);

        preparedStmt.execute();

        preparedStmt.close();

        // if (this.outputFHIR==null && anId != 0) {
        if (anId!=0) {
            PurgetheQueue(anId, resource);
        }

        return true;
    }

    public String getOrganizationRS(Integer organization_id) throws SQLException {

        String result = "";
        //String q = "SELECT * FROM subscriber_pi.organization where id = '" + organization_id + "'";
        //String q = "SELECT * FROM "+dbschema+".organization where id = '" + organization_id + "'";

        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        String q = "SELECT * FROM "+dbschema+".organization where id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,organization_id.toString());

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            String odscode = rs.getString("ods_code");
            String name = rs.getString("name");
            String postcode = rs.getString("postcode");
            Integer id = rs.getInt("id");

            result = odscode + "~" + name + "~" + postcode + "~" + id;
        }

        preparedStatement.close();

        return result;

    }

    public String GetOtherAddresses(Integer patientid, String curraddid) throws SQLException {

        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        String addresses="";

        //String q = "select * from "+dbschema+".patient_address where id <> "+curraddid+" AND patient_id="+patientid.toString();
        String q = "select * from "+dbschema+".patient_address where id <> ? AND patient_id=?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,curraddid);
        preparedStatement.setString(2,patientid.toString());

        ResultSet rs = preparedStatement.executeQuery();

        while(rs.next())
        {
            String add1=""; String add2=""; String add3=""; String add4="";
            String city=""; String postcode=""; String useconceptid="";

            if (rs.getString("address_line_1")!=null) {add1 = rs.getString("address_line_1");}
            if (rs.getString("address_line_2")!=null) {add2 = rs.getString("address_line_2");}
            if (rs.getString("address_line_3")!=null) {add3 = rs.getString("address_line_3");}
            if (rs.getString("address_line_4")!=null) {add4 = rs.getString("address_line_4");}
            if (rs.getString("city")!=null) {city = rs.getString("city");}
            if (rs.getString("postcode")!=null) {postcode = rs.getString("postcode");}
            if (rs.getString("use_concept_id")!=null) {useconceptid=rs.getString("use_concept_id");}

            addresses=addresses+add1+"`"+add2+"`"+add3+"`";
            addresses=addresses+add4+"`"+city+"`"+postcode+"`"+useconceptid+"|";
        }

        preparedStatement.close();

        return addresses;
    }

    public String GetTelecom(Integer patientid) throws SQLException {
        String telecom ="";

        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        //String q = "select pc.value, cctype.name as contact_type, ccuse.name as contact_use ";
        //q = q + "from subscriber_pi.patient_contact pc " + "left outer join subscriber_pi.concept ccuse on ccuse.dbid = pc.use_concept_id "
        //        + "left outer join subscriber_pi.concept cctype on cctype.dbid = pc.type_concept_id where pc.patient_id = '"+patientid.toString()+"'";

        /*
        String q = "select pc.value, cctype.name as contact_type, ccuse.name as contact_use ";
        q = q + "from "+dbschema+".patient_contact pc " + "left outer join "+dbschema+".concept ccuse on ccuse.dbid = pc.use_concept_id "
                + "left outer join "+dbschema+".concept cctype on cctype.dbid = pc.type_concept_id where pc.patient_id = '"+patientid.toString()+"'";
        */

        String q = "select pc.value, cctype.name as contact_type, ccuse.name as contact_use ";
        q = q + "from "+dbschema+".patient_contact pc " + "left outer join "+dbschema+".concept ccuse on ccuse.dbid = pc.use_concept_id "
                + "left outer join "+dbschema+".concept cctype on cctype.dbid = pc.type_concept_id where pc.patient_id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,patientid.toString());

        ResultSet rs = preparedStatement.executeQuery();

        while(rs.next()) {
            if (rs.getString(3) != null) {
                telecom = telecom + rs.getString(1) + "`" + rs.getString(2) + "`" + rs.getString(3) + "|";
            }
        }

        preparedStatement.close();

        return telecom;
    }

    public String getMedicationStatementRSOld(Integer record_id) throws SQLException {
        String q = ""; String result = "";

        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        /*
        q = "select " + "ms.id," + "ms.patient_id," + "ms.dose," + "ms.quantity_value," + "ms.quantity_unit," + "ms.clinical_effective_date,"
                + "c.name as medication_name," + "c.code as snomed_code, c.name as drugname "
                + "from "+dbschema+".medication_statement ms "
                // + "join "+dbschema+".concept_map cm on cm.legacy = ms.non_core_concept_id "
                // + "join "+dbschema+".concept c on c.dbid = cm.core "
                + "join "+dbschema+".concept c on c.dbid = ms.non_core_concept_id "
                + "where ms.id = '" + record_id + "'";
         */

        q = "select " + "ms.id," + "ms.patient_id," + "ms.dose," + "ms.quantity_value," + "ms.quantity_unit," + "ms.clinical_effective_date,"
                + "c.name as medication_name," + "c.code as snomed_code, c.name as drugname "
                + "from "+dbschema+".medication_statement ms "
                + "join "+dbschema+".concept c on c.dbid = ms.non_core_concept_id "
                + "where ms.id = ?";

        //System.out.println(q);
        //Scanner scan = new Scanner(System.in);
        //System.out.print("Press any key to continue . . . ");
        //scan.nextLine();

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,record_id.toString());

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            Integer nor = rs.getInt("patient_id");
            String snomedcode = rs.getString("snomed_code");
            String drugname = rs.getString("drugname");
            String dose = rs.getString("dose"); String quantityvalue = rs.getString("quantity_value");
            String quantityunit = rs.getString("quantity_unit"); String clinicaleffdate = rs.getString("clinical_effective_date");
            Integer id = rs.getInt(1);

            if (rs.getString("dose")==null) {dose="";}
            if (rs.getString("quantity_value")==null) {quantityvalue="";}
            if (rs.getString("quantity_unit")==null) {quantityunit="";}

            // dose contained a ~!
            String newdose = dose.replaceAll("`","");
            result = nor+"`"+snomedcode+"`"+drugname+"`"+newdose+"`"+quantityvalue+"`"+quantityunit+"`"+clinicaleffdate+"`"+id;
        }
        preparedStatement.close();

        return result;
    }

    public String getMedicationStatementRS(Integer record_id) throws SQLException {
        String q = ""; String result = "";

        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        /*
        q = "select "
                + "ms.id,\r\n"
                + "ms.patient_id,\r\n"
                + "ms.dose,\r\n"
                + "ms.quantity_value,\r\n"
                + "ms.quantity_unit,\r\n"
                + "ms.clinical_effective_date,\r\n"
                + "c.name as medication_name,\r\n"
                + "c.code as snomed_code,\r\n"
                + "c.name as drugname\r\n"
                + "from "+dbschema+".medication_statement ms\r\n"
                + "join "+dbschema+".concept_map cm on cm.legacy = ms.non_core_concept_id\r\n"
                + "join "+dbschema+".concept c on c.dbid = cm.core\r\n"
                ////+ "join "+dbschema+".concept c on c.dbid = ms.non_core_concept_id\r\n"
                + "where ms.id = '" + record_id + "'";
         */

        q = "select "
                + "ms.id,\r\n"
                + "ms.patient_id,\r\n"
                + "ms.dose,\r\n"
                + "ms.quantity_value,\r\n"
                + "ms.quantity_unit,\r\n"
                + "ms.clinical_effective_date,\r\n"
                + "c.name as medication_name,\r\n"
                + "c.code as snomed_code,\r\n"
                + "c.name as drugname\r\n"
                + "from "+dbschema+".medication_statement ms\r\n"
                + "join "+dbschema+".concept_map cm on cm.legacy = ms.non_core_concept_id\r\n"
                + "join "+dbschema+".concept c on c.dbid = cm.core\r\n"
                + "where ms.id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,record_id.toString());

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            Integer nor = rs.getInt("patient_id");
            String snomedcode = rs.getString("snomed_code");
            String drugname = rs.getString("drugname");
            String dose = rs.getString("dose"); String quantityvalue = rs.getString("quantity_value");
            String quantityunit = rs.getString("quantity_unit"); String clinicaleffdate = rs.getString("clinical_effective_date");
            Integer id = rs.getInt(1);

            if (rs.getString("dose")==null) {dose="";}
            if (rs.getString("quantity_value")==null) {quantityvalue="";}
            if (rs.getString("quantity_unit")==null) {quantityunit="";}

            // dose contained a ~!
            String newdose = dose.replaceAll("`","");
            result = nor+"`"+snomedcode+"`"+drugname+"`"+newdose+"`"+quantityvalue+"`"+quantityunit+"`"+clinicaleffdate+"`"+id;
        }

        preparedStatement.close();

        if (result.length()==0) {
            result = getMedicationStatementRSOld(record_id);
        }

        return result;
    }

    public String getObservationRecordNew(String id) throws SQLException {

        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "";}

        String obsrec = ""; String snomedcode = ""; String orginalterm = "";
        String result_value = ""; String clineffdate = ""; String resultvalunits = "";

        Integer noncoreconceptid = 0;

        /*
        String q = "select ";
        q = q + "o.id,\n\r"
                + "o.patient_id,\n\r"
                + "c.code as snomed_code,\n\r"
                + "c.name as original_term,\n\r"
                + "o.result_value,\n\r"
                + "o.clinical_effective_date,\n\r"
                + "o.parent_observation_id,\n\r"
                + "o.result_value_units,\n\r"
                + "o.non_core_concept_id \n\r"
                + "from "+dbschema+".observation o \n\r"
                + "join "+dbschema+".concept_map cm on cm.legacy = o.non_core_concept_id \n\r"
                + "join "+dbschema+".concept c on c.dbid = cm.core \n\r"
                + "join "+dbreferences+".snomed_code_set_codes scs on scs.snomedCode = c.code \n\r"
                + "where o.id = '"+id+"'";
        */

        String q = "select ";
        q = q + "o.id,\n\r"
                + "o.patient_id,\n\r"
                + "c.code as snomed_code,\n\r"
                + "c.name as original_term,\n\r"
                + "o.result_value,\n\r"
                + "o.clinical_effective_date,\n\r"
                + "o.parent_observation_id,\n\r"
                + "o.result_value_units,\n\r"
                + "o.non_core_concept_id \n\r"
                + "from "+dbschema+".observation o \n\r"
                + "join "+dbschema+".concept_map cm on cm.legacy = o.non_core_concept_id \n\r"
                + "join "+dbschema+".concept c on c.dbid = cm.core \n\r"
                + "join "+dbreferences+".snomed_code_set_codes scs on scs.snomedCode = c.code \n\r"
                + "where o.id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,id);

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            snomedcode = rs.getString(3); orginalterm = rs.getString(4);
            result_value = rs.getString(5); clineffdate = rs.getString(6); resultvalunits = rs.getString(8);
            noncoreconceptid = rs.getInt("non_core_concept_id");

            if (result_value == null) {result_value="";}
            if (resultvalunits == null) { resultvalunits="";}

            obsrec = snomedcode + "~" + orginalterm + "~" + result_value + "~" + clineffdate + "~" + resultvalunits + "~" + noncoreconceptid;
        }

        preparedStatement.close();

        if (obsrec.length()==0) {

            // q = "select * from "+dbschema+".observation where id = "+id;

            /*
            q = "select ";
            q = q + "o.id,\n\r"
                    + "o.patient_id,\n\r"
                    + "c.code as snomed_code,\n\r"
                    + "c.name as original_term,\n\r"
                    + "o.result_value,\n\r"
                    + "o.clinical_effective_date,\n\r"
                    + "o.parent_observation_id,\n\r"
                    + "o.result_value_units,\n\r"
                    + "o.non_core_concept_id \n\r"
                    + "from "+dbschema+".observation o \n\r"
                    + "join  "+dbschema+".concept c on c.dbid = o.non_core_concept_id "
                    + "where o.id = '"+id+"'";
             */

            q = "select ";
            q = q + "o.id,\n\r"
                    + "o.patient_id,\n\r"
                    + "c.code as snomed_code,\n\r"
                    + "c.name as original_term,\n\r"
                    + "o.result_value,\n\r"
                    + "o.clinical_effective_date,\n\r"
                    + "o.parent_observation_id,\n\r"
                    + "o.result_value_units,\n\r"
                    + "o.non_core_concept_id \n\r"
                    + "from "+dbschema+".observation o \n\r"
                    + "join "+dbschema+".concept c on c.dbid = o.non_core_concept_id "
                    + "where o.id = ?";

            preparedStatement = connection.prepareStatement(q);

            preparedStatement.setString(1,id);

            rs = preparedStatement.executeQuery();
            if (rs.next()) { ;
                result_value = rs.getString("result_value"); clineffdate = rs.getString("clinical_effective_date"); resultvalunits = rs.getString("result_value_units");
                noncoreconceptid = rs.getInt("non_core_concept_id"); orginalterm=rs.getString("original_term");
                snomedcode = rs.getString("snomed_code");

                if (result_value == null) {result_value="";}
                if (resultvalunits == null) { resultvalunits="";}

                obsrec = snomedcode+"~"+orginalterm+"~"+result_value+"~"+clineffdate+"~"+resultvalunits+"~"+noncoreconceptid;
            }
            preparedStatement.close();
        }

        return obsrec;
    }

    public String getObservationRecord(String id) throws SQLException {

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "";}

        v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        String obsrec = ""; String snomedcode = ""; String orginalterm = "";
        String result_value = ""; String clineffdate = ""; String resultvalunits = "";

        Integer noncoreconceptid = 0;

        /*
        String q = "select ";
        q = q + "o.id,"
                + "o.patient_id,"
                + "c.code as snomed_code,"
                + "c.name as original_term,"
                + "o.result_value,"
                + "o.clinical_effective_date,"
                + "o.parent_observation_id,"
                + "o.result_value_units,"
                + "o.non_core_concept_id "
                + "from "+dbschema+".observation o "
                + "join "+dbschema+".concept_map cm on cm.legacy = o.non_core_concept_id "
                + "join "+dbschema+".concept c on c.dbid = cm.core "
                + "join "+dbreferences+".snomed_code_set_codes scs on scs.snomedCode = c.code "
                + "where o.id = '"+id+"'";
         */

        String q = "select ";
        q = q + "o.id,"
                + "o.patient_id,"
                + "c.code as snomed_code,"
                + "c.name as original_term,"
                + "o.result_value,"
                + "o.clinical_effective_date,"
                + "o.parent_observation_id,"
                + "o.result_value_units,"
                + "o.non_core_concept_id "
                + "from "+dbschema+".observation o "
                + "join "+dbschema+".concept_map cm on cm.legacy = o.non_core_concept_id "
                + "join "+dbschema+".concept c on c.dbid = cm.core "
                + "join "+dbreferences+".snomed_code_set_codes scs on scs.snomedCode = c.code "
                + "where o.id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,id);

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            snomedcode = rs.getString(3); orginalterm = rs.getString(4);
            result_value = rs.getString(5); clineffdate = rs.getString(6); resultvalunits = rs.getString(8);
            noncoreconceptid = rs.getInt("non_core_concept_id");
            obsrec = snomedcode + "~" + orginalterm + "~" + result_value + "~" + clineffdate + "~" + resultvalunits + "~" + noncoreconceptid;
        }

        preparedStatement.close();

        if (obsrec.length()==0) {
            //q = "select * from subscriber_pi.observation where id = "+id;
            q = "select * from "+dbschema+".observation where id = "+id;
            preparedStatement = connection.prepareStatement(q);
            rs = preparedStatement.executeQuery();
            if (rs.next()) { ;
                result_value = rs.getString("result_value"); clineffdate = rs.getString("clinical_effective_date"); resultvalunits = rs.getString("result_value_units");
                noncoreconceptid = rs.getInt("non_core_concept_id");
                obsrec = "~~"+result_value+"~"+clineffdate+"~"+resultvalunits+"~"+noncoreconceptid;
            }
            preparedStatement.close();
        }

        //preparedStatement.close();

        return obsrec;
    }

    public String getIdsFromParent(Integer parentid) throws SQLException {
        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        String ids = "";


        //String q = "SELECT id FROM subscriber_pi.observation WHERE parent_observation_id="+parentid;
        //String q = "SELECT id FROM "+dbschema+".observation WHERE parent_observation_id="+parentid;
        String q = "SELECT id FROM "+dbschema+".observation WHERE parent_observation_id=?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,parentid.toString());

        ResultSet rs = preparedStatement.executeQuery();

        while(rs.next()) {
            ids = ids + rs.getString(1) + "~";
        }

        preparedStatement.close();

        return ids;
    }

    /*
    public String GenerateDateTime(String zeffdate, String znor, String zcode, String record_id) throws SQLException {
        String sTime = "";

        String q = "SELECT id, patient_id, clinical_effective_date, non_core_concept_id FROM "+dbschema+".observation\n" +
                "where clinical_effective_date = ? and patient_id=? and non_core_concept_id=? order by id";

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        preparedStatement.setString(1,zeffdate);
        preparedStatement.setString(2,znor);
        preparedStatement.setString(3,zcode);
        ResultSet rs = preparedStatement.executeQuery();

        Integer secs=0;

        while (rs.next())
        {
            String zid = rs.getString("id");
            secs = secs + 10;
            if (Integer.parseInt(zid) == Integer.parseInt(record_id)) {break;}
        }
        preparedStatement.close();

        sTime = zeffdate + " " + LocalTime.ofSecondOfDay(secs);
        return sTime;
    }
    */

    public String GetIdsForNOR(String nor) throws SQLException
    {
        String q = "select ";
        q = q + "o.id,\n\r"
                + "o.patient_id,\n\r"
                + "c.code as snomed_code,\n\r"
                + "c.name as original_term,\n\r"
                + "o.result_value,\n\r"
                + "o.clinical_effective_date,\n\r"
                + "o.parent_observation_id\n\r,"
                + "o.result_value_units \n\r"
                + "from "+dbschema+".observation o \n\r"
                + "join "+dbschema+".concept_map cm on cm.legacy = o.non_core_concept_id \n\r"
                + "join "+dbschema+".concept c on c.dbid = cm.core \n\r"
                + "join "+dbreferences+".snomed_code_set_codes scs on scs.snomedCode = c.code \n\r"
                + "where scs.codeSetId = 2 and o.patient_id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        preparedStatement.setString(1,nor);
        ResultSet rs = preparedStatement.executeQuery();

        String ids="";
        while (rs.next()) {
            ids=ids+rs.getString("id")+"~";
        }

        preparedStatement.close();

        return ids;
    }

    public boolean Stop() {
        File tempFile = new File("/tmp/stop.txt");
        boolean exists = tempFile.exists();
        return exists;
    }

    public String getObservationRSNew(Integer record_id) throws SQLException {

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "";}

        v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        String result = "";

        /*
        String q = "select ";
        q = q + "o.id,\n\r"
                + "o.patient_id,\n\r"
                + "c.code as snomed_code,\n\r"
                + "c.name as original_term,\n\r"
                + "o.result_value,\n\r"
                + "o.clinical_effective_date,\n\r"
                + "o.parent_observation_id\n\r,"
                + "o.result_value_units \n\r"
                + "from "+dbschema+".observation o \n\r"
                + "join "+dbschema+".concept_map cm on cm.legacy = o.non_core_concept_id \n\r"
                + "join "+dbschema+".concept c on c.dbid = cm.core \n\r"
                + "join "+dbreferences+".snomed_code_set_codes scs on scs.snomedCode = c.code \n\r"
                ////+ "join "+dbschema+".concept c on c.dbid = o.non_core_concept_id " // <= returns read codes
                + "where scs.codeSetId = 2 and o.id = '"+record_id+"'";
        ////+ "where o.id = '"+record_id+"'";
        */

        // count the number of observations for this code and effective_date and apply a timestamp
        // SELECT id, patient_id, clinical_effective_date, non_core_concept_id FROM subscriber_pi.observation
        // where clinical_effective_date = '2014-12-23' and patient_id=23608 and non_core_concept_id=1050706 order by id

        /*
        String q = "SELECT non_core_concept_id, patient_id, clinical_effective_date from "+dbschema+".observation where id=?";
        PreparedStatement preparedStatement = connection.prepareStatement(q);
        preparedStatement.setString(1,record_id.toString());
        ResultSet rs = preparedStatement.executeQuery();

        String ztime = "";
        if (rs.next()) {
            String zeffdate = rs.getString("clinical_effective_date");
            String znor = rs.getString("patient_id");
            String zcode = rs.getString("non_core_concept_id");
            ztime = GenerateDateTime(zeffdate, znor, zcode, record_id.toString());
        }

        preparedStatement.close();
        */

        String q = "select ";
        q = q + "o.id,\n\r"
                + "o.patient_id,\n\r"
                + "c.code as snomed_code,\n\r"
                + "c.name as original_term,\n\r"
                + "o.result_value,\n\r"
                + "o.clinical_effective_date,\n\r"
                + "o.parent_observation_id\n\r,"
                + "o.result_value_units \n\r"
                + "from "+dbschema+".observation o \n\r"
                + "join "+dbschema+".concept_map cm on cm.legacy = o.non_core_concept_id \n\r"
                + "join "+dbschema+".concept c on c.dbid = cm.core \n\r"
                + "join "+dbreferences+".snomed_code_set_codes scs on scs.snomedCode = c.code \n\r"
                + "where scs.codeSetId = 2 and o.id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,record_id.toString());

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            Integer nor = rs.getInt("patient_id"); String snomedcode = rs.getString("snomed_code"); String orginalterm = rs.getString("original_term");
            String result_value = rs.getString("result_value"); String clineffdate = rs.getString("clinical_effective_date"); String resultvalunits = rs.getString("result_value_units");

            if (rs.getString("result_value") == null) {result_value="";}
            if (rs.getString("result_value_units") == null) {resultvalunits="";}

            result = nor.toString()+"~"+snomedcode+"~"+orginalterm+"~"+result_value+"~"+clineffdate+"~"+resultvalunits+"~"+rs.getInt("parent_observation_id");
            // result = nor.toString()+"~"+snomedcode+"~"+orginalterm+"~"+result_value+"~"+ztime+"~"+resultvalunits+"~"+rs.getInt("parent_observation_id");
        }

        if (result.length()==0) {
            System.out.println(q);
            System.out.println(">>> "+record_id.toString());
            //Scanner scan = new Scanner(System.in);
            //System.out.print("Press any key to continue . . . ");
            //scan.nextLine();
        }

        preparedStatement.close();

        return result;
    }

    public String getObservationRS(Integer record_id) throws SQLException {

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "";}

        v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        String result = "";

        /*
        String q = "select ";
        q = q + "o.id,"
                + "o.patient_id,"
                + "c.code as snomed_code,"
                + "c.name as original_term,"
                + "o.result_value,"
                + "o.clinical_effective_date,"
                + "o.parent_observation_id,"
                + "o.result_value_units "
                + "from "+dbschema+".observation o "
                + "join "+dbschema+".concept_map cm on cm.legacy = o.non_core_concept_id "
                + "join "+dbschema+".concept c on c.dbid = cm.core "
                + "join "+dbreferences+".snomed_code_set_codes scs on scs.snomedCode = c.code "
                + "where scs.codeSetId = 2 and o.id = '"+record_id+"'";
         */

        String q = "select ";
        q = q + "o.id,"
                + "o.patient_id,"
                + "c.code as snomed_code,"
                + "c.name as original_term,"
                + "o.result_value,"
                + "o.clinical_effective_date,"
                + "o.parent_observation_id,"
                + "o.result_value_units "
                + "from "+dbschema+".observation o "
                + "join "+dbschema+".concept_map cm on cm.legacy = o.non_core_concept_id "
                + "join "+dbschema+".concept c on c.dbid = cm.core "
                + "join "+dbreferences+".snomed_code_set_codes scs on scs.snomedCode = c.code "
                + "where scs.codeSetId = 2 and o.id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,record_id.toString());

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            Integer nor = rs.getInt("patient_id"); String snomedcode = rs.getString("snomed_code"); String orginalterm = rs.getString("original_term");
            String result_value = rs.getString("result_value"); String clineffdate = rs.getString("clinical_effective_date"); String resultvalunits = rs.getString("result_value_units");

            if (rs.getString("result_value") == null) {result_value="";}
            if (rs.getString("result_value_units") == null) {resultvalunits="";}

            result = nor.toString()+"~"+snomedcode+"~"+orginalterm+"~"+result_value+"~"+clineffdate+"~"+resultvalunits+"~"+rs.getInt("parent_observation_id");
        }

        preparedStatement.close();

        return result;
    }

    public String getAllergyIntoleranceRSOld(Integer record_id) throws SQLException {

        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        String q = "select ";
        String result = "";

        /*
        q = q + "ai.id,"
                + "ai.patient_id,"
                + "ai.clinical_effective_date,"
                + "c.name as allergy_name,"
                + "c.code as snomed_code "
                + "from " + dbschema + ".allergy_intolerance ai "
                //+ "join "+dbschema+".concept_map cm on cm.legacy = ai.non_core_concept_id "
                //+ "join "+dbschema+".concept c on c.dbid = cm.core "
                + "join " + dbschema + ".concept c on c.dbid = ai.non_core_concept_id "
                + "where ai.id = '" + record_id + "'";
         */

        q = q + "ai.id,"
                + "ai.patient_id,"
                + "ai.clinical_effective_date,"
                + "c.name as allergy_name,"
                + "c.code as snomed_code "
                + "from "+dbschema+".allergy_intolerance ai "
                + "join "+dbschema+".concept c on c.dbid = ai.non_core_concept_id "
                + "where ai.id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,record_id.toString());

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            Integer nor = rs.getInt("patient_id");
            String clineffdate = rs.getString(3);
            String allergyname = rs.getString(4);
            String snomedcode = rs.getString(5);
            result = nor + "~" + clineffdate + "~" + allergyname + "~" + snomedcode;
        }

        preparedStatement.close();

        if (result.length() == 0) {
            System.out.println("?" + record_id);
            System.out.println(q);
        }

        return result;
    }

    public String getAllergyIntoleranceRS(Integer record_id) throws SQLException {

        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        /*
        String q = "select "; String result = "";
        q =q + "ai.id,\n\r"
                + "ai.patient_id,\n\r"
                + "ai.clinical_effective_date,\n\r"
                + "c.name as allergy_name,\n\r"
                + "c.code as snomed_code \n\r"
                + "from "+dbschema+".allergy_intolerance ai \n\r"
                // commented out (start)
                + "join "+dbschema+".concept_map cm on cm.legacy = ai.non_core_concept_id \n\r"
                + "join "+dbschema+".concept c on c.dbid = cm.core \n\r"
                // (end)
                ////+ "join "+dbschema+".concept c on c.dbid = ai.non_core_concept_id "
                + "where ai.id = '"+record_id+"'";
         */

        String q = "select "; String result = "";
        q =q + "ai.id,\n\r"
                + "ai.patient_id,\n\r"
                + "ai.clinical_effective_date,\n\r"
                + "c.name as allergy_name,\n\r"
                + "c.code as snomed_code \n\r"
                + "from "+dbschema+".allergy_intolerance ai \n\r"
                + "join "+dbschema+".concept_map cm on cm.legacy = ai.non_core_concept_id \n\r"
                + "join "+dbschema+".concept c on c.dbid = cm.core \n\r"
                + "where ai.id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,record_id.toString());

        //System.out.println(q);

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            Integer nor = rs.getInt("patient_id");
            String clineffdate = rs.getString(3);
            String allergyname = rs.getString(4);
            String snomedcode = rs.getString(5);
            result = nor+"~"+clineffdate+"~"+allergyname+"~"+snomedcode;
        }

        preparedStatement.close();

        if (result.length()==0)
        {
            result = getAllergyIntoleranceRSOld(record_id);
        }

        return result;
    }

    public String DeductedData(Integer nor) throws SQLException {
        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "1";}

        v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "1";}

        String result="";

        String q="SELECT p.id, e.date_registered, e.date_registered_end ";
        q=q+"FROM "+dbschema+".patient p ";
        q=q+"join "+dbschema+".episode_of_care e on e.patient_id = p.id ";
        q=q+"join "+dbschema+".concept c on c.dbid = e.registration_type_concept_id ";
        q=q+"where c.code='R' and p.id=? ";
        q=q+"and e.organization_id=? ";
        q=q+"order by e.id desc";

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        preparedStatement.setString(1,nor.toString());
        preparedStatement.setString(2,organization);

        ResultSet rs = preparedStatement.executeQuery();
        result="";
        if (rs.next()) {
            String StartDate = rs.getString("date_registered");
            String EndDate = rs.getString("e.date_registered_end");
            String id = rs.getString("id");
            result = id+"~"+StartDate+"~"+EndDate;
        }

        preparedStatement.close();

        return result;
    }

    public String InCohort(Integer nor) throws SQLException {
        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "0";}

        v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "0";}

        String q = "SELECT patientId FROM "+dbreferences+".subscriber_cohort WHERE patientId=? and needsDelete=0";

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        preparedStatement.setString(1,nor.toString());

        ResultSet rs = preparedStatement.executeQuery();

        String result = "0";
        if (rs.next()) { result = "1";}

        preparedStatement.close();

        return result;
    }

    public String Deducted(Integer nor, String resource) throws SQLException {
        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "1";}

        v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "1";}

        String q="SELECT p.id ";
        q=q+"FROM "+dbschema+".patient p ";
        q=q+"join "+dbschema+".episode_of_care e on e.patient_id = p.id ";
        q=q+"join "+dbschema+".concept c on c.dbid = e.registration_type_concept_id ";
        q=q+"where c.code = 'R' and p.id=? ";
        q=q+"and p.date_of_death IS NULL ";
        q=q+"and e.date_registered <= now() ";
        q=q+"and (e.date_registered_end > now() or e.date_registered_end IS NULL) ";
        q=q+"and e.organization_id=? ";
        q=q+"order by e.id desc"; // might have re-registered with the practice?

        //System.out.println(nor + " >> " +q);

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        preparedStatement.setString(1,nor.toString());
        preparedStatement.setString(2,organization);

        ResultSet rs = preparedStatement.executeQuery();

        String result="1"; // deducted
        if (rs.next()) {
            String id = rs.getString("id");
            if (!id.isEmpty() || id !=null) {result="0";} // not deducted
        }

        preparedStatement.close();

        if (result.equals("1")) {boolean ret = AuditDeducted(nor, "Deducted", resource);}

        return result;
    }

    public String Deceased(Integer nor, String resource) throws SQLException {

        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "1";}

        v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "1";}

        String q= "select date_of_death from "+dbschema+".patient where id=?";

        //System.out.println(nor + " >> " +q);

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        preparedStatement.setString(1,nor.toString());

        ResultSet rs = preparedStatement.executeQuery();

        String result="0";
        if (rs.next()) {
            String dod = rs.getString("date_of_death");
            if (dod!=null) {
                result="1";
            }
        }

        preparedStatement.close();

        if (result.equals("1")) {boolean ret = AuditDeducted(nor, "Deceased", resource);};

        return result;
    }

    public String getPatientRS(Integer patient_id) throws SQLException {

        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "";}

        /*
        String q = "select distinct ";
        q = q + "p.id as patient_id,\r\n"
                + "p.nhs_number,\r\n"
                + "p.title,\r\n"
                + "p.first_names,\r\n"
                + "p.last_name,\r\n"
                + "gc.name as gender,\r\n"
                + "p.date_of_birth,\r\n"
                + "p.date_of_death,\r\n"
                + "pa.address_line_1,\r\n"
                + "pa.address_line_2,\r\n"
                + "pa.address_line_3,\r\n"
                + "pa.address_line_4,\r\n"
                + "pa.postcode,\r\n"
                + "pa.city,\r\n"
                + "pa.start_date,\r\n"
                + "pa.end_date,\r\n"
                + "pa.use_concept_id,\r\n" // change
                + "cctype.name as contact_type,\r\n"
                + "ccuse.name as contact_use,\r\n"
                + "pc.value as contact_value,\r\n"
                + "p.organization_id,\r\n"
                + "p.current_address_id,\r\n" // change
                + "org.ods_code,\r\n"
                + "org.name as org_name,\r\n"
                + "org.postcode as org_postcode\r\n "
                + "from "+dbschema+".patient p \r\n"
                + "left outer join "+dbschema+".patient_address pa on pa.id = p.current_address_id \r\n"
                + "left outer join "+dbschema+".patient_contact pc on pc.patient_id = p.id \r\n"
                + "left outer join "+dbschema+".concept ccuse on ccuse.dbid = pc.use_concept_id \r\n"
                + "left outer join "+dbschema+".concept cctype on cctype.dbid = pc.type_concept_id \r\n"
                + "left outer join "+dbschema+".concept gc on gc.dbid = p.gender_concept_id \r\n"
                + "left outer join "+dbschema+".organization org on org.id = p.organization_id \r\n"
                + "join "+dbschema+".observation o on o.patient_id = p.id \r\n"
                + "join "+dbschema+".concept_map cm on cm.legacy = o.non_core_concept_id \r\n"
                + "join "+dbschema+".concept c on c.dbid = cm.core \r\n"
                + "join "+dbreferences+".snomed_code_set_codes scs on scs.snomedCode = c.code \r\n"
                + "where scs.codeSetId = 1 and p.id ='" + patient_id.toString() + "'";
         */

        String q = "select distinct ";
        q = q + "p.id as patient_id,\r\n"
                + "p.nhs_number,\r\n"
                + "p.title,\r\n"
                + "p.first_names,\r\n"
                + "p.last_name,\r\n"
                + "gc.name as gender,\r\n"
                + "p.date_of_birth,\r\n"
                + "p.date_of_death,\r\n"
                + "pa.address_line_1,\r\n"
                + "pa.address_line_2,\r\n"
                + "pa.address_line_3,\r\n"
                + "pa.address_line_4,\r\n"
                + "pa.postcode,\r\n"
                + "pa.city,\r\n"
                + "pa.start_date,\r\n"
                + "pa.end_date,\r\n"
                + "pa.use_concept_id,\r\n" // change
                + "cctype.name as contact_type,\r\n"
                + "ccuse.name as contact_use,\r\n"
                + "pc.value as contact_value,\r\n"
                + "p.organization_id,\r\n"
                + "p.current_address_id,\r\n" // change
                + "org.ods_code,\r\n"
                + "org.name as org_name,\r\n"
                + "org.postcode as org_postcode\r\n "
                + "from "+dbschema+".patient p \r\n"
                + "left outer join "+dbschema+".patient_address pa on pa.id = p.current_address_id \r\n"
                + "left outer join "+dbschema+".patient_contact pc on pc.patient_id = p.id \r\n"
                + "left outer join "+dbschema+".concept ccuse on ccuse.dbid = pc.use_concept_id \r\n"
                + "left outer join "+dbschema+".concept cctype on cctype.dbid = pc.type_concept_id \r\n"
                + "left outer join "+dbschema+".concept gc on gc.dbid = p.gender_concept_id \r\n"
                + "left outer join "+dbschema+".organization org on org.id = p.organization_id \r\n"
                + "join "+dbschema+".observation o on o.patient_id = p.id \r\n"
                + "join "+dbschema+".concept_map cm on cm.legacy = o.non_core_concept_id \r\n"
                + "join "+dbschema+".concept c on c.dbid = cm.core \r\n"
                + "join "+dbreferences+".snomed_code_set_codes scs on scs.snomedCode = c.code \r\n"
                + "where scs.codeSetId = 1 and p.id =?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,patient_id.toString());

        ResultSet rs = preparedStatement.executeQuery();

        String result="";
        if (rs.next()) {
            String nhsno = rs.getString("nhs_number");
            String dob = rs.getString("date_of_birth");
            String odscode = rs.getString("ods_code");
            String orgname = rs.getString("org_name");
            String orgpostcode = rs.getString("org_postcode");

            String telecom = GetTelecom(patient_id);

            String dod = rs.getString("date_of_death");

            String add1="";
            if (rs.getString("address_line_1")!=null) {add1 = rs.getString("address_line_1");}

            String add2="";
            // test
            if (rs.getString("address_line_2")!=null) add2 = rs.getString("address_line_2");

            String add3="";
            if (rs.getString("address_line_3")!=null) add3 = rs.getString("address_line_3");

            String add4="";
            if (rs.getString("address_line_4")!=null) add4 = rs.getString("address_line_4");

            String city="";
            if (rs.getString("city")!=null) city = rs.getString("city");

            String postcode="";
            if (rs.getString("postcode")!=null) postcode = rs.getString("postcode");

            String gender = rs.getString("gender");
            String contacttype = rs.getString("contact_type");
            String contactuse = rs.getString("contact_use");
            String contactvalue = rs.getString("contact_value");
            String title = rs.getString("title");
            String firstname = rs.getString("first_names");
            String lastname = rs.getString("last_name");

            String startdate = rs.getString("start_date"); // date added to the cohort?
            Integer orgid = rs.getInt("organization_id");

            String useconceptid = rs.getString("use_concept_id");
            String curraddid = rs.getString("current_address_id");

            String addresses = GetOtherAddresses(patient_id, curraddid);

            result = nhsno + "~" + odscode + "~" + orgname + "~" + orgpostcode + "~" + telecom + "~" + dod + "~" + add1 + "~" + add2 + "~" + add3 + "~" + add4 + "~" + city + "~";
            result = result + gender + "~" + contacttype + "~" + contactuse + "~" + contactvalue + "~" + title + "~" + firstname + "~" + lastname + "~" + startdate + "~" + orgid + "~" + dob + "~" + postcode + "~";
            result = result + useconceptid + "~" + curraddid + "~" + addresses + "~";
        }

        preparedStatement.close();

        return result;
    }

    private String getPatientIdAndOrg(String id, String tablename) throws SQLException {

        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "0";}

        v = ValidateTable(dbschema,tablename);
        if (isFalse(v)) {return "0";}

        String nor=""; String orgid="";

        //if (tablename.equals("patient")) {return Integer.parseInt(id);}

        if (tablename.length()==0) return "0";

        //String preparedSql = "select patient_id from "+dbschema+"."+tablename+" where id="+id;
        String preparedSql = "select patient_id, organization_id from "+dbschema+"."+tablename+" where id=?";

        if (tablename.equals("patient")) {
            preparedSql = "select id, organization_id from "+dbschema+"."+tablename+" where id=?";
        }

        PreparedStatement preparedStatement = connection.prepareStatement( preparedSql );
        preparedStatement.setString(1,id);

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {

            if (tablename.equals("patient")) { nor = rs.getString("id");}
            else
            {nor = rs.getString("patient_id");}

            orgid = rs.getString("organization_id");
        }

        preparedStatement.close();

        return nor+"~"+orgid;
    }

    public List<List<String>> getDeleteRows() throws SQLException {

        List<List<String>> result = new ArrayList<>();

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return result;}

        String preparedSql = "select * from "+dbreferences+".filteredDeletionsDelta";

        PreparedStatement preparedStatement = connection.prepareStatement( preparedSql );

        ResultSet rs = preparedStatement.executeQuery();

        Integer recid=0; Integer tableid=0; String nor=""; String resource="";
        String tablename=""; String ret=""; String orgid="";

        while (rs.next()) {
            recid = rs.getInt("record_id");
            tableid = rs.getInt("table_id");

            // patient - 2, observation - 11, allergy - 4, medication - 10
            tablename="";
            if (tableid.equals(2)) {tablename="patient"; resource="Patient";}
            if (tableid.equals(11)) {tablename="observation"; resource="Observation";}
            if (tableid.equals(4)) {tablename="allergy_intolerance"; resource="AllergyIntolerance";}
            if (tableid.equals(10)) {tablename="medication_statement"; resource="MedicationStatement";}

            if (tablename.length()==0) continue;

            ret = getPatientIdAndOrg(recid.toString(), tablename);
            // nor~org
            String[] ss = ret.split("\\~",-1);
            nor = ss[0]; orgid=ss[1];

            // we don't want to transmit a delete for a record that exists in the system
            if (!tablename.equals("patient") && !ret.equals("~")) {continue;}

            if (!orgid.equals(organization)) {continue;}

            List<String> row = new ArrayList<>();
            row.add(recid.toString());
            row.add(tableid.toString());
            row.add(nor);
            row.add(tablename);
            row.add(resource);
            result.add(row);

            this.counting = this.counting + 1;
            if (this.counting > this.scaletotal) break;
        }

        preparedStatement.close();

        return result;
    }

    public List<Integer> getRows(String table) throws SQLException {
        List<Integer> result = new ArrayList<>();

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return result;}

        v = ValidateSchema(dbschema);
        if (isFalse(v)) {return result;}

        v = ValidateTable(dbreferences, table);
        if (isFalse(v)) {return result;}

        String preparedSql = "select * from "+ dbreferences+"."+table;

        //String prepareOrgSQL = "select t.id, j.organization_id from "+dbreferences+"."+table+ "t ";

        String j ="";

        if (table.equals("filteredObservationsDelta")) {j=" join "+dbschema+".observation j on t.id=j.id";}
        if (table.equals("filteredMedicationsDelta")) {j=" join "+dbschema+".medication_statement j on t.id=j.id";}
        if (table.equals("filteredAllergiesDelta")) {j=" join "+dbschema+".allergy_intolerance j on t.id=j.id";}

        if (!organization.isEmpty()) {
            //preparedSql = "select t.id, j.organization_id from "+dbreferences+"."+table+ " t";
            //preparedSql = preparedSql + j + " WHERE j.organization_id="+organization;
            preparedSql = "select t.id, j.organization_id from "+dbreferences+"."+table+" t";
            preparedSql = preparedSql + j + " WHERE j.organization_id=? limit "+scaletotal;

            if (table.equals("filteredObservationsDelta")) {
                preparedSql = "select id from "+dbreferences+".filteredObservationsDelta where organization_id=? limit "+scaletotal;
            }
        }

        System.out.println(preparedSql);

        // preparedSql = preparedSql + " where id>14189471 order by id asc";

        PreparedStatement preparedStatement = connection.prepareStatement( preparedSql );

        if (!organization.isEmpty()) {preparedStatement.setString(1,organization);}


        ResultSet rs = preparedStatement.executeQuery();

        Integer id = 0; Integer count = 0;

        while (rs.next()) {
            //this.counting = this.counting + 1;
            //if (this.counting > this.scaletotal) break;

            id = rs.getInt("id");

            List<Integer> row = new ArrayList<>();

            // 10k testing!
            //for (int i = 0; i < 14; i++) {
            //    List<Integer> row = new ArrayList<>();
            //    result.add(id);
            //}

            result.add(id);

            this.counting = this.counting + 1;
            if (this.counting > this.scaletotal) break;
        }
        preparedStatement.close();

        //List<Integer> result = new ArrayList<>();
        //result.add(56229);

        return result;
    }

    public List<Integer> getPatientRows() throws SQLException {
        List<Integer> result = new ArrayList<>();

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return result;}

        // String preparedSql = "select * from data_extracts.cohort";
        String preparedSql = "select * from "+dbreferences+".filteredPatientsDelta";

        if (!organization.isEmpty()) {
            preparedSql ="SELECT p.id, p.organization_id FROM "+dbreferences+".filteredPatientsDelta f join "+dbschema+".patient p on p.id = f.id where p.organization_id=?";
        }

        PreparedStatement preparedStatement = connection.prepareStatement( preparedSql );

        if (!organization.isEmpty()) {
            preparedStatement.setString(1,organization);
        }

        ResultSet rs = preparedStatement.executeQuery();

        Integer patient_id = 0;

        while (rs.next()) {

            //this.counting = this.counting + 1;
            //if (this.counting > this.scaletotal) break;

            //patient_id = rs.getInt("patient_id");
            patient_id = rs.getInt("id");

            List<Integer> row = new ArrayList<>();

            result.add(patient_id);

            this.counting = this.counting + 1;
            if (this.counting > this.scaletotal) break;

        }

        preparedStatement.close();


        // Testing
        //List<Integer> result = new ArrayList<>();
        //result.add(28844);

        return result;
    }

    /*
    public List<List<String>> getRows(int offset, int pageSize) throws SQLException {

        String preparedSql = "select * from table";

        PreparedStatement preparedStatement = connection.prepareStatement( preparedSql );

        ResultSet rs = preparedStatement.executeQuery();

        List<List<String>> result = new ArrayList<>();

        while (rs.next()) {

        }

        preparedStatement.close();

        return result;
    }
    */

    public String getBaseURL()
    {
        return baseURL;
    }

    public String getConfig()
    {
        //String conStr = ConfigManager.getConfiguration("database","knowdiabetes");
        String conStr = ConfigManager.getConfiguration("database",config);
        System.out.println(conStr);
        return conStr;
    }

    public boolean CreateFilteredTables() throws SQLException {

        // #1
        String q = "call initialiseSnomedCodeSetTablesDelta();";
        PreparedStatement preparedStatement = connection.prepareStatement(q);
        ResultSet rs = preparedStatement.executeQuery();
        preparedStatement.close();

        System.out.println("initialiseSnomedCodeSetTablesDelta "+rs);

        // Don't need to run #2 and #3 again
        // #2
        //q = "call buildCohortCodeSetDelta();";
        //preparedStatement = connection.prepareStatement(q);
        //rs = preparedStatement.executeQuery();
        //preparedStatement.close();

        //System.out.println("buildCohortCodeSetDelta "+rs);

        // #3
        //q = "call buildKnowDiabetesObservationCodeSetDelta();";
        //preparedStatement = connection.prepareStatement(q);
        //rs = preparedStatement.executeQuery();
        //preparedStatement.close();

        //System.out.println("buildKnowDiabetesObservationCodeSetDelta "+rs);

        // #4
        q = "call createCohortKnowDiabetesDelta();";
        preparedStatement = connection.prepareStatement(q);
        rs = preparedStatement.executeQuery();
        preparedStatement.close();

        System.out.println("createCohortKnowDiabetesDelta" +rs);

        // #5
        q = "call getKnowDiabetesPatientDelta();";
        preparedStatement = connection.prepareStatement(q);
        rs = preparedStatement.executeQuery();
        preparedStatement.close();

        System.out.println("getKnowDiabetesPatientDelta "+rs);

        // #6
        q = "call getKnowDiabetesObservationsDelta();";
        preparedStatement = connection.prepareStatement(q);
        rs = preparedStatement.executeQuery();
        preparedStatement.close();

        System.out.println("getKnowDiabetesObservationsDelta "+rs);

        // #7
        q = "call getKnowDiabetesAllergiesDelta();";
        preparedStatement = connection.prepareStatement(q);
        rs = preparedStatement.executeQuery();
        preparedStatement.close();

        System.out.println("getKnowDiabetesAllergiesDelta "+rs);

        // #8
        q = "call getKnowDiabetesMedicationsDelta();";
        preparedStatement = connection.prepareStatement(q);
        rs = preparedStatement.executeQuery();
        preparedStatement.close();

        System.out.println("getKnowDiabetesMedicationsDelta "+rs);

        // #9
        q = "call getKnowDiabetesDeletionsDelta();";
        preparedStatement = connection.prepareStatement(q);
        rs = preparedStatement.executeQuery();
        preparedStatement.close();

        System.out.println("getKnowDiabetesDeletionsDelta "+rs);

        // #10
        q = "call finaliseExtract();";
        preparedStatement = connection.prepareStatement(q);
        rs = preparedStatement.executeQuery();
        preparedStatement.close();

        System.out.println("finaliseExtract "+rs);

        return true;
    }

    private void init(Properties props) throws SQLException {

        try {
            System.out.println("initializing properties");

            config = props.getProperty("config");

            String conStr = getConfig();
            String[] ss = conStr.split("\\`");

            //ConfigManager.Initialize("fhirExtractor");

            //MetricsHelper.recordEvent("Starting fhirextractor");

            // sqlurl~username~password~clientid~clientsecret~scope~tokenurl~baseurl
            //String zsqlurl=ss[0]; String zsqlusername=ss[1]; String zsqlpass=ss[2];
            //String zclientid=ss[3]; String zclientsecret= ss[4]; String zscope=ss[5];
            //String ztokenurl =ss[6]; String zbaseurl = ss[7];

            //baseURL = props.getProperty("baseurl");

            baseURL = ss[7];
            outputFHIR = props.getProperty("outputFHIR");
            dbschema = props.getProperty("dbschema");
            //clientid = props.getProperty("clientid");
            clientid = ss[3];
            //clientsecret = props.getProperty("clientsecret");
            clientsecret = ss[4];
            //scope = props.getProperty("scope");
            scope = ss[5];
            granttype = props.getProperty("granttype");
            //tokenurl = props.getProperty("tokenurl");
            tokenurl = ss[6];
            token = props.getProperty("token");
            runguid = props.getProperty("runguid");

            scaletotal = Integer.parseInt(props.getProperty("scaletotal"));

            dbreferences = props.getProperty("dbreferences");

            organization = props.getProperty("organization");

            procrun = props.getProperty("procrun");

            testobs = props.getProperty("testobs");

            resendpats = props.getProperty("resendpats");

            deletesdone = props.getProperty("deletesdone");

            testpatget = props.getProperty("testpatget");

            System.out.println("mysql url: "+ss[0]);
            System.out.println("mysql user: "+ss[1]);
            System.out.println("mysql pass: "+ss[2]);
            System.out.println("mysql db: "+dbschema);
            System.out.println("baseurl: "+baseURL);
            System.out.println("scale tot: "+scaletotal);
            System.out.println("disk: "+outputFHIR);
            System.out.println("dbreferences: "+dbreferences);
            System.out.println("config: "+config);
            System.out.println("organization: "+organization);
            System.out.println("testobs: "+testobs);
            System.out.println("resendpats: "+resendpats);

            Integer procruntimes = 0;
            if (!procrun.isEmpty()) {
                System.out.println("RUNNING SP'S "+procrun+" times!");
                procruntimes = Integer.parseInt(procrun);
            }

            dataSource = new MysqlDataSource();

            System.out.println(">> " + outputFHIR);

            //dataSource.setURL(props.getProperty("url"));
            //dataSource.setUser(props.getProperty("user"));
            //dataSource.setPassword(props.getProperty("password"));

            dataSource.setURL(ss[0]);
            dataSource.setUser(ss[1]);
            dataSource.setPassword(ss[2]);

            /* test
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter url");
            String url = sc.next();

            System.out.println("username");
            String username = sc.next();

            System.out.println("password");
            String pass = sc.next();

            dataSource.setURL(url);
            System.out.println("1");
            dataSource.setUser(username);
            System.out.println("2");
            dataSource.setPassword(pass);
            System.out.println("3");
            */

            dataSource.setReadOnlyPropagatesToServer(true);

            connection = dataSource.getConnection();

            // validate schemas
            boolean v = ValidateSchema(dbschema);
            if (isFalse(v)) {return;}

            v = ValidateSchema(dbreferences);
            if (isFalse(v)) {return;}

            // boolean ok = CreateFilteredTables();

            if (!outputFHIR.isEmpty() || !resendpats.isEmpty()) {
                Scanner scan = new Scanner(System.in);
                System.out.print("Press any key to continue . . . ");
                scan.nextLine();
            }

            if (procruntimes>0) {
                for (int i=0; i <(procruntimes); i++) {
                    System.out.println("Run: "+i);
                    boolean ok = CreateFilteredTables();
                }
                return;
            }

            // connection.setReadOnly(true);
            counting =0;
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
    }


    public void close() throws SQLException {
        connection.close();
    }
}
