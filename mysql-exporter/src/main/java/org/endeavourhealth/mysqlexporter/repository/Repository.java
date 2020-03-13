package org.endeavourhealth.mysqlexporter.repository;

import com.mysql.cj.jdbc.MysqlDataSource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.sql.*;
import org.endeavourhealth.common.config.ConfigManager;

import java.time.LocalTime;
import java.util.*;

import static org.apache.commons.lang3.BooleanUtils.isFalse;

public class Repository {

    private Connection connection;
    private MysqlDataSource dataSource;
    public String dbschema; public String params; public String dbreferences;
    public String organization; public String oneoff;

    public String a_patient;

    public Repository(Properties properties) throws SQLException {
        init( properties );
    }

    public String getConfig()
    {
        //String conStr = ConfigManager.getConfiguration("database","knowdiabetesdev");
        String conStr = ConfigManager.getConfiguration("database","knowdiabetes");
        System.out.println(conStr);
        return conStr;
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

    public String getLocation(Integer anid) throws SQLException {
        String location = "";

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "";}

        //String q = "SELECT * FROM "+dbreferences+".references WHERE an_id='" + anid + "' AND resource='ReportTracker'";
        String q = "SELECT * FROM "+dbreferences+".references WHERE an_id=? AND resource='ReportTracker'";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,anid.toString());

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) { location =  rs.getString("location"); }

        preparedStatement.close();

        return location;
    }

    public boolean Audit(Integer anId, String strid, String resource, Integer responseCode, String location, String encoded, Integer patientid, Integer typeid) throws SQLException
    {

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return false;}

        String q = "insert into "+dbreferences+".references (an_id,strid,resource,response,location,datesent,json,patient_id,type_id,runguid) values(?,?,?,?,?,?,?,?,?,?)";

        PreparedStatement preparedStmt = connection.prepareStatement(q);

        preparedStmt.setInt(1, anId);
        preparedStmt.setString(2, "");

        preparedStmt.setString(3, resource);

        preparedStmt.setString(4, "");
        preparedStmt.setString(5, "?");

        long timeNow = Calendar.getInstance().getTimeInMillis();
        java.sql.Timestamp ts = new java.sql.Timestamp(timeNow);
        preparedStmt.setTimestamp(6, ts);

        preparedStmt.setString(7, "");

        preparedStmt.setInt(8, 0);

        preparedStmt.setInt(9, 0);

        preparedStmt.setString(10, "");

        preparedStmt.execute();

        preparedStmt.close();

        return true;
    }

    private void ObsAudit(Repository repository, String ids, Integer patientid, String location) throws SQLException
    {
        String[] ss = ids.split("\\~");
        String id = "";
        for (int i = 0; i < ss.length; i++) {
            id = ss[i];
            Audit(Integer.parseInt(id), "", "ReportTracker", 0, "dum", "", patientid, 0);
        }
    }

    public void DeleteReportTracker() throws SQLException
    {
        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return;}

        String q ="DELETE FROM "+dbreferences+".references where resource ='ReportTracker'";

        PreparedStatement preparedStmt = connection.prepareStatement(q);
        preparedStmt.execute();

        preparedStmt.close();
    }

    public String getObservationRecord(String id) throws SQLException {

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "";}

        v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        String obsrec = ""; String snomedcode = ""; String orginalterm = "";
        String result_value = ""; String clineffdate = ""; String resultvalunits = "";

        /*
        String q = "SELECT non_core_concept_id, patient_id, clinical_effective_date from "+dbschema+".observation where id=?";
        PreparedStatement preparedStatement = connection.prepareStatement(q);
        preparedStatement.setString(1,id);
        ResultSet rs = preparedStatement.executeQuery();

        String ztime=""; String zcode="";
        if (rs.next()) {
            String zeffdate = rs.getString("clinical_effective_date");
            String znor = rs.getString("patient_id");
            zcode = rs.getString("non_core_concept_id");
            ztime = GenerateDateTime(zeffdate, znor, zcode, id);
        }

        preparedStatement.close();
         */

        Integer noncoreconceptid = 0;

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
            obsrec = snomedcode + "~" + orginalterm + "~" + result_value + "~" + clineffdate + "~" + resultvalunits + "~" + noncoreconceptid;
            // obsrec = snomedcode + "~" + orginalterm + "~" + result_value + "~" + ztime + "~" + resultvalunits + "~" + noncoreconceptid;
        }

        preparedStatement.close();

        if (obsrec.length()==0) {

            // q = "select * from "+dbschema+".observation where id = "+id;

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
                    + "where o.id = ?";

            preparedStatement = connection.prepareStatement(q);

            preparedStatement.setString(1,id);

            rs = preparedStatement.executeQuery();
            if (rs.next()) { ;
                result_value = rs.getString("result_value"); clineffdate = rs.getString("clinical_effective_date"); resultvalunits = rs.getString("result_value_units");
                noncoreconceptid = rs.getInt("non_core_concept_id"); orginalterm=rs.getString("original_term");
                snomedcode = rs.getString("snomed_code");
                obsrec = snomedcode+"~"+orginalterm+"~"+result_value+"~"+clineffdate+"~"+resultvalunits+"~"+noncoreconceptid;
                // obsrec = snomedcode+"~"+orginalterm+"~"+result_value+"~"+ztime+"~"+resultvalunits+"~"+noncoreconceptid;
            }
            preparedStatement.close();
        }

        return obsrec;
    }

    public String getIdsFromParent(Integer parentid) throws SQLException {

        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        String ids = "";

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
            if (Integer.parseInt(zid) == Integer.parseInt(record_id)) {
                break;
            }
        }
        preparedStatement.close();

        sTime = zeffdate + " " + LocalTime.ofSecondOfDay(secs);
        return sTime;
    }
     */

    public String getObservationRS(Integer record_id) throws SQLException {

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "";}

        v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        String result = "";

        /*
        String q = "SELECT non_core_concept_id, patient_id, clinical_effective_date from "+dbschema+".observation where id=?";
        PreparedStatement preparedStatement = connection.prepareStatement(q);
        preparedStatement.setString(1,record_id.toString());
        ResultSet rs = preparedStatement.executeQuery();

        String ztime = ""; String zcode = "";
        if (rs.next()) {
            String zeffdate = rs.getString("clinical_effective_date");
            String znor = rs.getString("patient_id");
            zcode = rs.getString("non_core_concept_id");
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
                + "o.parent_observation_id,\n\r"
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
            // result = nor.toString()+"~"+snomedcode+"~"+orginalterm+"~"+result_value+"~"+ztime+"~"+resultvalunits+"~"+rs.getInt("parent_observation_id")+"~"+zcode;
        }

        if (result.length()==0) {
            System.out.println(q);
        }

        preparedStatement.close();

        return result;
    }

    public String GetTelecom(Integer patientid) throws SQLException {


        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        String telecom = "";

        String q = "select pc.value, cctype.name as contact_type, ccuse.name as contact_use ";
        q = q + "from " + dbschema + ".patient_contact pc " + "left outer join " + dbschema + ".concept ccuse on ccuse.dbid = pc.use_concept_id "
                + "left outer join " + dbschema + ".concept cctype on cctype.dbid = pc.type_concept_id where pc.patient_id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,patientid.toString());

        ResultSet rs = preparedStatement.executeQuery();

        while (rs.next()) {
            if (rs.getString(3) != null) {
                telecom = telecom + rs.getString(1) + "`" + rs.getString(2) + "`" + rs.getString(3) + "|";
            }
        }
        preparedStatement.close();

        return telecom;
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

    public String getPatientRS(Integer patient_id) throws SQLException {

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return "";}

        v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

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
                + "where scs.codeSetId = 1 and p.id = ?";

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
            String curraddid = rs.getString("current_address_id");

            String addresses = GetOtherAddresses(patient_id, curraddid);

            result = nhsno + "~" + odscode + "~" + orgname + "~" + orgpostcode + "~" + telecom + "~" + dod + "~" + add1 + "~" + add2 + "~" + add3 + "~" + add4 + "~" + city + "~";
            result = result + gender + "~" + contacttype + "~" + contactuse + "~" + contactvalue + "~" + title + "~" + firstname + "~" + lastname + "~" + startdate + "~" + orgid + "~" + dob + "~" + postcode + "~";
            result = result + addresses + "~";
        }

        preparedStatement.close();

        return result;
    }

    public String getMedicationStatementRSOld(Integer record_id) throws SQLException {


        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        String q = ""; String result = "";

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
            result = nor+"`"+snomedcode+"`"+drugname+"`"+dose+"`"+quantityvalue+"`"+quantityunit+"`"+clinicaleffdate+"`"+id;
        }
        preparedStatement.close();

        return result;
    }

    public String getMedicationStatementRS(Integer record_id) throws SQLException {

        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        String q = ""; String result = "";

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
            result = nor+"`"+snomedcode+"`"+drugname+"`"+dose+"`"+quantityvalue+"`"+quantityunit+"`"+clinicaleffdate+"`"+id;
        }

        preparedStatement.close();

        if (result.length()==0) {
            result = getMedicationStatementRSOld(record_id);
        }

        return result;
    }

    public String getAllergyIntoleranceRSOld(Integer record_id) throws SQLException {

        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

        String q = "select "; String result = "";

        q =q + "ai.id,"
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
            result = nor+"~"+clineffdate+"~"+allergyname+"~"+snomedcode;
        }

        preparedStatement.close();

        if (result.length()==0) {
            System.out.println("?"+record_id);
            System.out.println(q);
        }

        return result;
    }

    public String getAllergyIntoleranceRS(Integer record_id) throws SQLException {

        boolean v = ValidateSchema(dbschema);
        if (isFalse(v)) {return "";}

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

    public void OneOff()  throws SQLException {
        String q = "call data_extracts.getKnowDiabetesObservationsDeltaOneOff('"+oneoff+"');";
        PreparedStatement preparedStatement = connection.prepareStatement(q);
        ResultSet rs = preparedStatement.executeQuery();
        preparedStatement.close();

        System.out.println("getKnowDiabetesObservationsDeltaOneOff "+rs);
    }

    public void getReferences() {
        try {
            boolean v = ValidateSchema(dbreferences);
            if (isFalse(v)) {
                return;
            }

            v = ValidateSchema(dbschema);
            if (isFalse(v)) {
                return;
            }

            String OS = System.getProperty("os.name").toLowerCase();
            String file = "//tmp//references.txt";
            if (OS.indexOf("win") >= 0) {
                file = "D:\\TEMP\\references.txt";
            }

            File zfile = new File(file);

            FileWriter fr = null;
            BufferedWriter br = null;

            fr = new FileWriter(zfile);
            br = new BufferedWriter(fr);

            String q =""; String lastid = "0";
            String org_id=""; String resource = ""; String response="";
            String dataWithNewLine = "";

            for (int i=1; i <(40000); i++) {
                q = "SELECT response, an_id, p.organization_id, resource ";
                q = q + "from " + dbreferences + ".references r ";
                q = q + "join " + dbschema + ".patient p on p.id = r.patient_id where r.an_id >" + lastid + " limit 2000";

                PreparedStatement preparedStatement = connection.prepareStatement(q);
                ResultSet rs = preparedStatement.executeQuery();

                if (!rs.next()) {
                    preparedStatement.close();
                    break;
                }

                while (rs.next()) {
                    lastid = rs.getString("an_id");
                    org_id = rs.getString("organization_id");
                    resource = rs.getString("resource");
                    response = rs.getString("response");

                    dataWithNewLine=lastid+","+response+","+org_id+","+resource+System.getProperty("line.separator");

                    br.write(dataWithNewLine);
                }
                preparedStatement.close();
            }
            br.close();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    public void GetQData()
    {
        try {
            boolean v = ValidateSchema(dbreferences);
            if (isFalse(v)) {
                return;
            }

            v = ValidateSchema(dbschema);
            if (isFalse(v)) {
                return;
            }

            String OS = System.getProperty("os.name").toLowerCase();
            String file = "//tmp//qdata.txt";
            if (OS.indexOf("win") >= 0) {
                file = "D:\\TEMP\\qdata.txt";
            }
            PrintStream o = new PrintStream(new File(file));
            System.setOut(o);

            String q = ""; String lastid = "0";
            // obs
            for (int i=1; i <(40000); i++) {
                q = "SELECT f.id, j.organization_id ";
                q = q + "from " + dbreferences + ".filteredObservationsDelta f ";
                //q = q+"left join "+dbschema+".observation j on j.id = f.id"; // where j.organization_id=?";
                q = q + "join " + dbschema + ".observation j on j.id = f.id where f.id >"+lastid+" order by f.id limit 2000";

                //System.out.println(q);

                PreparedStatement preparedStatement = connection.prepareStatement(q);
                //preparedStatement.setString(1,organization);
                ResultSet rs = preparedStatement.executeQuery();

                if (!rs.next()) {
                    preparedStatement.close();
                    break;
                }

                while (rs.next()) {
                    System.out.println("obs~" + rs.getString("id") + "~" + rs.getString("organization_id"));
                    lastid = rs.getString("id");
                }

                preparedStatement.close();
            }

            // nor
            q = "SELECT f.id, j.organization_id, j.date_of_birth ";
            q = q+"FROM "+dbreferences+".filteredPatientsDelta f ";
            q = q+"left join "+dbschema+".patient j on j.id = f.id"; // where j.organization_id=?";

            PreparedStatement zpreparedStatement = connection.prepareStatement(q);
            //preparedStatement.setString(1,organization);
            ResultSet zrs = zpreparedStatement.executeQuery();

            while (zrs.next()) {
                System.out.println("nor~"+zrs.getString("id")+"~"+zrs.getString("organization_id"));
            }

            zpreparedStatement.close();

            // rx
            q = "SELECT f.id, j.organization_id ";
            q = q+"FROM "+dbreferences+".filteredMedicationsDelta f ";
            q = q+"left join "+dbschema+".medication_statement j on j.id = f.id"; // where organization_id=?";

            zpreparedStatement = connection.prepareStatement(q);
            //preparedStatement.setString(1,organization);
            zrs = zpreparedStatement.executeQuery();

            while (zrs.next()) {
                System.out.println("rx~"+zrs.getString("id")+"~"+zrs.getString("organization_id"));
            }

            zpreparedStatement.close();

            // allergy
            q = "SELECT f.id, j.organization_id ";
            q = q+"FROM "+dbreferences+".filteredAllergiesDelta f ";
            q = q+"left join "+dbschema+".allergy_intolerance j on j.id = f.id"; // where j.organization_id=?";

            zpreparedStatement = connection.prepareStatement(q);
            //preparedStatement.setString(1,organization);
            zrs = zpreparedStatement.executeQuery();

            while (zrs.next()) {
                System.out.println("allergy~"+zrs.getString("id")+"~"+zrs.getString("organization_id"));
            }

            zpreparedStatement.close();

            // organizations
            q = "SELECT * ";
            q = q+"FROM "+dbschema+".organization";

            zpreparedStatement = connection.prepareStatement(q);
            zrs = zpreparedStatement.executeQuery();

            while (zrs.next()) {
                System.out.println("org~"+zrs.getString("id")+"~"+zrs.getString("ods_code")+"~"+zrs.getString("name"));
            }

            zpreparedStatement.close();
        }
        catch (Exception e) {
            System.out.println(e);
        }

    }

    public void DumpRefs()
    {
        try
        {

            boolean v = ValidateSchema(dbreferences);
            if (isFalse(v)) {return;}

            String OS = System.getProperty("os.name").toLowerCase();
            String file="//tmp//dumprefs.txt";
            if (OS.indexOf("win") >= 0) {file="D:\\TEMP\\dumprefs.txt";}
            PrintStream o = new PrintStream(new File(file));
            System.setOut(o);

            String preparedSql = "select * from "+dbreferences+".references";
            PreparedStatement preparedStatement = connection.prepareStatement( preparedSql );
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                System.out.print(rs.getInt("an_id")+"~");
                System.out.print(rs.getString("strid")+"~");
                System.out.print(rs.getString("location")+"~");
                System.out.print(rs.getString("resource")+"~");
                System.out.println(rs.getString("patient_id")+"~");
                System.out.println(rs.getString("json")+"~");
                //System.out.println(rs.getInt("patient_id"));
            }

            preparedStatement.close();

            }
        catch (Exception e) {
            System.out.println(e);
        }
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

    public List<Integer> getRowsForPatient(String patientid, String table) throws SQLException {
        Integer id = 0;

        List<Integer> result = new ArrayList<>();

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return result;}

        v = ValidateTable(dbreferences, table);
        if (isFalse(v)) {return result;}

        String q="";

        if (table=="obs") {
            q ="select distinct o.id from data_extracts.subscriber_cohort coh ";
            q = q + "join "+dbschema+".observation o on o.patient_id = ? ";
            q = q + "join "+dbschema+".concept_map cm on cm.legacy = o.non_core_concept_id ";
            q = q + "join "+dbschema+".concept c on c.dbid = cm.core ";
            q = q + "join "+dbreferences+".snomed_code_set_codes scs on scs.snomedCode = c.code";
        }

        if (table=="allergy") {
            q = "select distinct ";
            q= q + "ai.id ";
            q = q + "from "+dbreferences+".subscriber_cohort coh ";
            q =q + "join "+dbschema+".allergy_intolerance ai on ai.patient_id = ?";
        }

        if (table=="medicationstatement") {
            q = "select distinct ";
            q = q + "ms.id ";
            q = q + "from "+dbreferences+".subscriber_cohort coh ";
            q = q + "join "+dbschema+".medication_statement ms on ms.patient_id = ? ";
            q = q + "where ms.cancellation_date is null";
        }

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,patientid);

        ResultSet rs = preparedStatement.executeQuery();

        while (rs.next()) {
            id = rs.getInt("id");
            System.out.println(id.toString());
            List<Integer> row = new ArrayList<>();
            result.add(id);
        }

        preparedStatement.close();

        return result;
    }

    //select j.organization_id, dbref.json from data_extracts.references dbref
    //join nwl_subscriber_pid.medication_statement j on j.id=dbref.an_id
    //where j.organization_id=16327386 and resource='MedicationStatement'

    public List<Integer> getRowsFromReferences(String resource, String org_id, String subtable) throws SQLException {
        Integer id;

        List<Integer> result = new ArrayList<>();

        String q ="select an_id, j.organization_id, dbref.json from "+dbreferences+".references dbref ";
        q = q + "join "+dbschema+"."+subtable+" j on j.id=dbref.an_id ";
        q = q + "where organization_id=? and resource=? and response < 202";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,org_id);
        preparedStatement.setString(2,resource);

        ResultSet rs = preparedStatement.executeQuery();

        while (rs.next()) {
            id = rs.getInt("an_id");
            List<Integer> row = new ArrayList<>();
            result.add(id);
        }

        preparedStatement.close();

        return result;
    }

    public List<Integer> getRows(String resource, String table) throws SQLException {

        List<Integer> result = new ArrayList<>();

        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return result;}

        v = ValidateTable(dbreferences, table);
        if (isFalse(v)) {return result;}

        String zid = ""; Integer count = 0; Integer id=0;
        String[] type;

        // String preparedSql = "select distinct an_id from data_extracts.references where resource='"+resource+"'";

        // a list of specific ids the we want to report on ...
        if (params.length() >0 && a_patient.isEmpty())
        {
            String[] ss = params.split("\\~");
            for (int i = 0; i < ss.length; i++) {
                zid = ss[i]; type = zid.split("\\:");
                if (type[0].equals("o") && table.equals("filteredObservationsDelta")) {
                    List<Integer> row = new ArrayList<>(); result.add(Integer.parseInt(type[1]));
                }
                if (type[0]=="m" && table.equals("filteredMedicationsDelta")) {
                    List<Integer> row = new ArrayList<>(); result.add(Integer.parseInt(type[1]));
                }
                if (type[0]=="a" && table.equals("filteredAllergiesDelta")) {
                    List<Integer> row = new ArrayList<>(); result.add(Integer.parseInt(type[1]));
                }
                if (type[0]=="p" && table.equals("filteredPatientsDelta")) {
                    List<Integer> row = new ArrayList<>(); result.add(Integer.parseInt(type[1]));
                }
            }
            return result;
        }


        String preparedSql = "select * from "+dbreferences+"."+table; // +" LIMIT 10000";

        if (!a_patient.isEmpty()) {
            // join
            //SELECT * FROM data_extracts.filteredAllergiesDelta fa
            //join subscriber_pi.allergy_intolerance ai on ai.id = fa.id
            //where patient_id=23608
            if (table.equals("filteredObservationsDelta")) {
                preparedSql = "SELECT * FROM "+dbreferences+".filteredObservationsDelta fo ";
                preparedSql = preparedSql+"join "+dbschema+".observation oi on oi.id=fo.id ";
                preparedSql = preparedSql+"where patient_id="+a_patient;
            }
            if (table.equals("filteredMedicationsDelta")) {
                preparedSql = "SELECT * FROM "+dbreferences+".filteredMedicationsDelta fm ";
                preparedSql = preparedSql+"join "+dbschema+".medication_statement mi on mi.id=fm.id ";
                preparedSql = preparedSql+"where patient_id="+a_patient;
            }
            if (table.equals("filteredAllergiesDelta")) {
                preparedSql = "SELECT * FROM "+dbreferences+".filteredAllergiesDelta fa ";
                preparedSql = preparedSql+"join "+dbschema+".allergy_intolerance ai on ai.id=fa.id ";
                preparedSql = preparedSql+"where patient_id="+a_patient;
            }
            if (table.equals("filteredPatientsDelta")) {
                preparedSql = "SELECT * FROM "+dbreferences+".filteredPatientsDelta fp ";
                preparedSql = preparedSql+"join "+dbschema+".patient pi on pi.id=fp.id ";
                preparedSql = preparedSql+"where pi.id="+a_patient;
            }
        }

        //System.out.println(preparedSql);

        PreparedStatement preparedStatement = connection.prepareStatement( preparedSql );
        ResultSet rs = preparedStatement.executeQuery();

        while (rs.next()) {
            //id = rs.getInt("an_id");
            id = rs.getInt("id");
            List<Integer> row = new ArrayList<>();
            result.add(id);
        }

        preparedStatement.close();

        return result;
    }

    private void init(Properties props) throws SQLException {

        try {
            System.out.println("initializing properties");

            String conStr = getConfig();
            String[] ss = conStr.split("\\`");

            dbschema = props.getProperty("dbschema");
            params = props.getProperty("params");
            dbreferences = props.getProperty("dbreferences");

            System.out.println("mysql url: "+ss[0]);
            System.out.println("mysql user: "+ss[1]);
            System.out.println("mysql pass: "+ss[2]);
            System.out.println("mysql db: "+dbschema);
            System.out.println("references db: "+dbreferences);

            Scanner scan = new Scanner(System.in);
            System.out.print("Press any key to continue . . . ");
            scan.nextLine();

            dataSource = new MysqlDataSource();

            dataSource.setURL(ss[0]);
            dataSource.setUser(ss[1]);
            dataSource.setPassword(ss[2]);

            dataSource.setReadOnlyPropagatesToServer(true);

            connection = dataSource.getConnection();

            System.out.println("end initializing properties");
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