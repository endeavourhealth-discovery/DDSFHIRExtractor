package org.endeavourhealth.mysqlexporter.repository;

import com.mysql.cj.jdbc.MysqlDataSource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.sql.*;
import org.endeavourhealth.common.config.ConfigManager;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.apache.commons.lang3.BooleanUtils.isFalse;

public class Repository {

    private Connection connection;
    private MysqlDataSource dataSource;
    public String dbschema; public String params; public String dbreferences;
    public String organization; public String oneoff;

    public String a_patient;
    public String delq;
    public String refdate;

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

    public void DeleteUnwanted(Integer recid, Integer tableid) throws SQLException {
        boolean v = ValidateSchema(dbreferences);
        if (isFalse(v)) {return;}

        String q ="DELETE FROM "+dbreferences+".filteredDeletionsDelta where record_id="+recid.toString()+" and table_id="+tableid.toString();

        PreparedStatement preparedStmt = connection.prepareStatement(q);
        preparedStmt.execute();

        preparedStmt.close();
    }

    public void CheckFilteredDeletions() throws SQLException {
        try {
            String preparedSql = "select * from " + dbreferences + ".filteredDeletionsDelta";

            System.out.println(preparedSql);

            PreparedStatement preparedStatement = connection.prepareStatement(preparedSql);

            ResultSet rs = preparedStatement.executeQuery();

            String file = "//tmp//checkdeletions.txt";

            File zfile = new File(file);
            FileWriter fr = null;
            BufferedWriter br = null;
            fr = new FileWriter(zfile);
            br = new BufferedWriter(fr);

            Integer recid = 0;
            Integer tableid = 0;
            String nor = "";
            String resource = "";
            String tablename = "";
            String ret = "";
            String orgid = ""; String dataWithNewLine = "";

            while (rs.next()) {
                recid = rs.getInt("record_id");
                tableid = rs.getInt("table_id");

                // patient - 2, observation - 11, allergy - 4, medication - 10
                tablename = "";
                if (tableid.equals(2)) {
                    tablename = "patient";
                    resource = "Patient";
                }
                if (tableid.equals(11)) {
                    tablename = "observation";
                    resource = "Observation";
                }
                if (tableid.equals(4)) {
                    tablename = "allergy_intolerance";
                    resource = "AllergyIntolerance";
                }
                if (tableid.equals(10)) {
                    tablename = "medication_statement";
                    resource = "MedicationStatement";
                }

                if (tablename.length() == 0) continue;

                ret = getPatientIdAndOrg(recid.toString(), tablename);
                // nor~org
                String[] ss = ret.split("\\~", -1);
                nor = ss[0];
                orgid = ss[1];

                if (!tablename.equals("patient") && !ret.equals("~")) {DeleteUnwanted(recid, tableid);}

                // we don't want to transmit a delete for a record that exists in the system
                // if (!tablename.equals("patient") && !ret.equals("~")) {continue;}

                dataWithNewLine = tablename+","+recid+","+ret+System.getProperty("line.separator");
                //System.out.println(tablename + "," + recid + "," + ret);
                br.write(dataWithNewLine);
            }

            preparedStatement.close();
            br.close();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    public void DELQ()  throws SQLException {
        String zid = ""; String ztype = ""; String q = ""; String table = "";
        System.out.println("delq");
        try {
            File f = new File("/tmp/DELQ.txt");
            Scanner r = new Scanner(f);
            while (r.hasNextLine()) {
                String data = r.nextLine();
                String[] ss = data.split("\\~");
                ztype = ss[0]; zid = ss[1];
                table = "";
                if (ztype.equals("obs")) {table="filteredObservationsDelta";}
                if (ztype.equals("allergy")) {table="filteredAllergiesDelta";}
                if (ztype.equals("rx")) {table="filteredMedicationsDelta";}
                if (table.isEmpty()) continue;
                q ="DELETE FROM data_extracts."+table+" WHERE id="+zid;

                System.out.println(q);

                PreparedStatement preparedStmt = connection.prepareStatement(q);
                preparedStmt.execute();
                preparedStmt.close();
            }
            r.close();
        } catch (Exception e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void OneOff()  throws SQLException {
        String q = "call data_extracts.getKnowDiabetesObservationsDeltaOneOff('"+oneoff+"');";
        PreparedStatement preparedStatement = connection.prepareStatement(q);
        ResultSet rs = preparedStatement.executeQuery();
        preparedStatement.close();

        System.out.println("getKnowDiabetesObservationsDeltaOneOff "+rs);
    }

    public void getReferencesSimple()
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
            String file = "//tmp//references" + UUID.randomUUID().toString().replace("-", "") + ".txt";
            if (OS.indexOf("win") >= 0) {
                file = "D:\\TEMP\\references.txt";
            }

            File zfile = new File(file);

            FileWriter fr = null;
            BufferedWriter br = null;

            fr = new FileWriter(zfile);
            br = new BufferedWriter(fr);

            String q = "SELECT response, an_id, p.organization_id, resource, r.patient_id, datesent ";
            q = q + "from " + dbreferences + ".references r ";
            q = q + "join " + dbschema + ".patient p on p.id = r.patient_id ";

            PreparedStatement preparedStatement = connection.prepareStatement(q);
            ResultSet rs = preparedStatement.executeQuery();

            String lastid = "0";
            String org_id=""; String resource = ""; String response="";
            String dataWithNewLine = ""; String nor =""; String result = ""; String dead = "";
            String datesent = "";

            while (rs.next()) {
                lastid = rs.getString("an_id");
                org_id = rs.getString("organization_id");
                resource = rs.getString("resource");
                response = rs.getString("response");
                datesent = rs.getString("datesent");

                result = "";

                dataWithNewLine=lastid+","+response+","+org_id+","+resource+","+result+","+dead+","+datesent+System.getProperty("line.separator");

                br.write(dataWithNewLine);
            }

            preparedStatement.close();
            br.close();

        } catch (Exception e) {
                System.out.println(e);
            }
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
            //String file = "//tmp//references"+UUID.randomUUID().toString().replace("-", "")+".txt";
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
            String dataWithNewLine = ""; String nor =""; String result = ""; String dead = "";
            String datesent = refdate;

            // get the max value at this point in time
            q = "SELECT MAX(an_id) as zmax FROM "+dbreferences+".references";
            PreparedStatement zpreparedStatement = connection.prepareStatement(q);
            ResultSet zrs = zpreparedStatement.executeQuery();
            String max = "0";
            if (zrs.next()) { max = zrs.getString("zmax"); }
            zpreparedStatement.close();

            for (int i=1; i <(40000); i++) {
                /*
                q = "SELECT response, an_id, p.organization_id, resource, r.patient_id, datesent ";
                q = q + "from " + dbreferences + ".references r ";
                q = q + "join " + dbschema + ".patient p on p.id = r.patient_id where r.an_id >" + lastid + " limit 2000";
                */

                //q = "SELECT sc.patientId, response, an_id, p.organization_id, resource, r.patient_id, datesent ";
                q = "SELECT response, an_id, p.organization_id, resource, r.patient_id, datesent ";
                q = q + "from " + dbreferences + ".references r ";
                q = q + "join " + dbschema + ".patient p on p.id = r.patient_id ";
                //q = q + " left join "+dbreferences+".subscriber_cohort sc on sc.patientId=p.id ";
                //q = q + " where r.an_id >" + lastid + " and datesent > '"+refdate+"' order by r.an_id  limit 80000";
                //q = q + " where r.an_id > " + lastid + " and r.an_id <= "+max+" and datesent > '"+refdate+"' order by r.an_id limit 1000";
                q = q + " where r.an_id > " + lastid + " and r.an_id <= "+max+" order by r.an_id limit 20000";
                //q = q + " where datesent > DATE_SUB('"+datesent+"', INTERVAL 1 SECOND) order by r.datesent  limit 2000";

                //System.out.println(q);

                //dataWithNewLine = q + System.getProperty("line.separator");
                //br.write(dataWithNewLine);

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
                    //nor = rs.getString("patient_id");
                    datesent = rs.getString("datesent");

                    //result = rs.getString("patientId");
                    result = "";

                    /*
                    result = ""; dead = "";
                    if (!nor.equals("0")) {
                        //result = Deducted(Integer.parseInt(nor));
                        result = InCohort(Integer.parseInt(nor));
                        //dead = Deceased(Integer.parseInt(nor));
                    }
                     */

                    dataWithNewLine=lastid+","+response+","+org_id+","+resource+","+result+","+dead+","+datesent+System.getProperty("line.separator");

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

    private void UpdateObsFilteredDelta(String id, String orgid) throws SQLException {
        String q ="update "+dbreferences+".filteredObservationsDelta set organization_id=? where id=?";
        PreparedStatement preparedStmt = connection.prepareStatement(q);
        preparedStmt.setString(1,orgid);
        preparedStmt.setString(2,id);
        preparedStmt.execute();
        preparedStmt.close();
    }

    private void UpdateRxFilteredDelta(String id, String orgid) throws SQLException {
        String q ="update "+dbreferences+".filteredMedicationsDelta set organization_id=? where id=?";
        PreparedStatement preparedStmt = connection.prepareStatement(q);
        preparedStmt.setString(1,orgid);
        preparedStmt.setString(2,id);
        preparedStmt.execute();
        preparedStmt.close();
    }

    public String Deceased(Integer nor) throws SQLException {

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

        return result;
    }

    public String Deducted(Integer nor) throws SQLException {
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
        //q=q+"and e.organization_id=? ";
        q=q+"order by e.id desc"; // might have re-registered with the practice?

        //System.out.println(nor + " >> " +q);

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        preparedStatement.setString(1,nor.toString());
        //preparedStatement.setString(2,organization);

        ResultSet rs = preparedStatement.executeQuery();

        String result="1"; // deducted
        if (rs.next()) {
            String id = rs.getString("id");
            if (!id.isEmpty() || id !=null) {result="0";} // not deducted
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

        // spun the logic for output
        String result = "1"; // deducted
        if (rs.next()) { result = "0";}

        preparedStatement.close();

        return result;
    }

    public String checkPatient(String id) throws SQLException {

        //select distinct
        //p.id as patient_id,
        //        p.firstname,
        //        p.last_name
        //join "+dbschema+".observation o on o.patient_id = p.id
        //left join subscriber_pi_dev.concept_map cm on cm.legacy = o.non_core_concept_id
        //left join subscriber_pi_dev.concept c on c.dbid = cm.core
        //left join data_extracts.snomed_code_set_codes z on z.snomedCode = c.code
        //where patient_id=zid and organization_id=organziation and z.codeSetId=1

        String q = "select distinct p.id as patient_id,  p.first_names, p.last_name, c.name, z.codeSetId, c.dbid, c.code ";
        q = q + "from "+dbschema+".patient p ";
        q = q + "join "+dbschema+".observation o on o.patient_id = p.id ";
        q = q + "left join "+dbschema+".concept_map cm on cm.legacy = o.non_core_concept_id ";
        q = q + "left join "+dbschema+".concept c on c.dbid = cm.core ";
        q = q + "left join "+dbreferences+".snomed_code_set_codes z on z.snomedCode = c.code ";
        q = q + "where patient_id="+id+" and z.codeSetId=1";

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        ResultSet rs = preparedStatement.executeQuery();

        String ret = id+",";
        if (rs.next()) {
            String patient_id = rs.getString("patient_id");
            String last_name = rs.getString("last_name");
            String firstname = rs.getString("first_names");
            String dbid = rs.getString("dbid");
            String name = rs.getString("name");
            String code = rs.getString("code");
            String codeset = rs.getString("codeSetId");
            ret = ret + patient_id+","+last_name+","+dbid+","+name+","+code+","+codeset;
        }

        preparedStatement.close();

        return ret;
    }

    public String CheckObs(String id)  throws SQLException {
        //SELECT o.patient_id, o.id, c.dbid, name, c.code
        //FROM subscriber_pi_dev.observation o
        //left join subscriber_pi_dev.concept_map cm on cm.legacy = o.non_core_concept_id
        //left join subscriber_pi_dev.concept c on c.dbid = cm.core
        //left join data_extracts.snomed_code_set_codes z on z.snomedCode = c.code
        //where o.id=id

        String q = "SELECT o.patient_id, o.id, c.dbid, z.codeSetId, name, c.code, o.organization_id ";
        q = q + "FROM "+dbschema+".observation o ";
        q = q + "left join "+dbschema+".concept_map cm on cm.legacy = o.non_core_concept_id ";
        q = q + "left join "+dbschema+".concept c on c.dbid = cm.core ";
        q = q + "left join "+dbreferences+".snomed_code_set_codes z on z.snomedCode = c.code ";
        q = q + "where o.id=" + id;

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        ResultSet rs = preparedStatement.executeQuery();

        String ret = id+",";
        if (rs.next()) {
            String nor = rs.getString("patient_id");
            String deducted = "1";
            if (InCohort(Integer.parseInt(nor)).equals("0")) { // not deducted
                deducted = "0";
                String dbid = rs.getString("dbid");
                String name = rs.getString("name");
                String code = rs.getString("code");
                String codeset = rs.getString("codeSetId");
                String org = rs.getString("organization_id");
                ret = ret + nor + "," + dbid + "," + name + "," + code + "," + org + "," + codeset + ",";
            }
            ret = ret + deducted;
        }

        preparedStatement.close();

        return ret;
    }

    public void CheckPatients() throws SQLException {
        String file = "//tmp//checkpatients.txt";
        // String file = "c:\\temp\\checkpatients.txt";

        try {
            PrintStream o = new PrintStream(new File(file));
            System.setOut(o);

            String q = "SELECT f.id, j.organization_id, j.id as patient_id ";
            q = q+"FROM "+dbreferences+".filteredPatientsDelta f ";
            q = q+"left join "+dbschema+".patient j on j.id = f.id";

            PreparedStatement preparedStatement = connection.prepareStatement(q);
            ResultSet rs = preparedStatement.executeQuery();

            String ret = ""; String id = ""; String nor="";
            while (rs.next()) {
                nor = rs.getString("patient_id");
                // if (InCohort(Integer.parseInt(nor)).equals("1")) {continue;} // 1 is deducted
                id = rs.getString("id");
                ret = checkPatient(id);
                System.out.println(ret);
            }

            preparedStatement.close();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    public void CheckConcepts() throws SQLException {

        String file = "//tmp//all-checkconcepts.txt";
        // String file = "c:\\temp\\checkconcepts.txt";

        try {
            PrintStream o = new PrintStream(new File(file));
            System.setOut(o);

            String q = "";
            String lastid = "0";
            String ret = "";
            for (int i = 1; i < (90000); i++) {

                q = "SELECT f.id ";
                q = q + "from " + dbreferences + ".filteredObservationsDelta f ";
                //q = q + "where organization_id=" + organization + " and f.id >" + lastid + " order by f.id limit 2000";
                q = q + "where f.id >" + lastid + " order by f.id limit 2000";

                PreparedStatement preparedStatement = connection.prepareStatement(q);
                ResultSet rs = preparedStatement.executeQuery();

                if (!rs.isBeforeFirst()) {
                    preparedStatement.close();
                    break;
                }

                while (rs.next()) {
                    ret = CheckObs(rs.getString("id"));
                    System.out.println(ret);
                    lastid = rs.getString("id");
                }

                preparedStatement.close();
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    public void GetQDataSimple()
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

            //SELECT ods_code, organization_id, name, count(distinct f.id) from data_extracts.filteredObservationsDelta f
            //-- join nwl_subscriber_pid.observation j on j.id = f.id
            //join nwl_subscriber_pid.organization o on o.id = f.organization_id
            //group by f.organization_id
            //order by name

            String q = "SELECT f.id, organization_id, count(distinct f.id) as zcount from "+dbreferences+".filteredObservationsDelta f ";
            q = q + "group by f.organization_id";

            PreparedStatement preparedStatement = connection.prepareStatement(q);
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                System.out.println("obs~" + rs.getString("id") + "~" + rs.getString("organization_id") + "~" + rs.getString("zcount"));
            }

            preparedStatement.close();

            //select f.id, j.organization_id
            //from data_extracts.filteredPatientsDelta f
            //left join nwl_subscriber_pid.patient j on j.id = f.id

            q = "SELECT f.id, j.organization_id ";
            q = q+"FROM "+dbreferences+".filteredPatientsDelta f ";
            q = q+"left join "+dbschema+".patient j on j.id = f.id ";

            preparedStatement = connection.prepareStatement(q);
            rs = preparedStatement.executeQuery();

            while (rs.next()) {
                System.out.println("nor~" + rs.getString("id") + "~" + rs.getString("organization_id"));
            }

            preparedStatement.close();

            //select f.id, j.organization_id
            //from data_extracts.filteredMedicationsDelta f
            //left join nwl_subscriber_pid.medication_statement j on j.id = f.id

            q = "select f.id, j.organization_id ";
            q = q+"FROM "+dbreferences+".filteredMedicationsDelta f ";
            q = q+"left join "+dbschema+".medication_statement j on j.id = f.id ";

            preparedStatement = connection.prepareStatement(q);
            rs = preparedStatement.executeQuery();

            while (rs.next()) {
                System.out.println("rx~"+rs.getString("id")+"~"+rs.getString("organization_id"));
            }

            preparedStatement.close();

            //select f.id, j.organization_id
            //from data_extracts.filteredAllergiesDelta f
            //left join nwl_subscriber_pid.allergy_intolerance j on j.id = f.id
            q = "select f.id, j.organization_id ";
            q = q+"FROM "+dbreferences+".filteredAllergiesDelta f ";
            q = q+"left join "+dbschema+".allergy_intolerance j on j.id = f.id ";

            preparedStatement = connection.prepareStatement(q);
            rs = preparedStatement.executeQuery();

            while (rs.next()) {
                System.out.println("allergy~"+rs.getString("id")+"~"+rs.getString("organization_id"));
            }

            preparedStatement.close();

            // organizations
            q = "SELECT * ";
            q = q+"FROM "+dbschema+".organization";

            preparedStatement = connection.prepareStatement(q);
            rs = preparedStatement.executeQuery();

            while (rs.next()) {
                System.out.println("org~"+rs.getString("id")+"~"+rs.getString("ods_code")+"~"+rs.getString("name"));
            }

            preparedStatement.close();
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

            String q = ""; String lastid = "0"; String result = ""; String nor =""; String dead = "";
            // obs
            for (int i=1; i <(90000); i++) {

                /*
                q = "SELECT f.id, j.organization_id, j.patient_id ";
                q = q + "from " + dbreferences + ".filteredObservationsDelta f ";
                q = q + "join " + dbschema + ".observation j on j.id = f.id where f.id >"+lastid+" order by f.id limit 2000";
                */

                //q = "SELECT id, organization_id ";
                //q = q + "FROM "+dbreferences+".filteredObservationsDelta ";
                //q = q + "WHERE id >"+lastid+ " order by id limit 2000";

                q = "SELECT sc.patientId, f.id, f.organization_id, j.patient_id ";
                q = q + "from " + dbreferences + ".filteredObservationsDelta f ";
                q = q + "join " + dbschema + ".observation j on j.id = f.id ";
                q = q + "left join " + dbreferences + ".subscriber_cohort sc on sc.patientId=j.patient_id ";
                q = q +	" where f.id >"+lastid+" order by f.id limit 50000";

                //System.out.println(q);

                PreparedStatement preparedStatement = connection.prepareStatement(q);
                //preparedStatement.setString(1,organization);
                ResultSet rs = preparedStatement.executeQuery();

                if (!rs.isBeforeFirst()) {
                    preparedStatement.close();
                    break;
                }

                while (rs.next()) {
                    //nor = rs.getString("patient_id");
                    //result = Deducted(Integer.parseInt(nor));
                    //dead = Deceased(Integer.parseInt(nor));

                    dead = "";
                    // result = InCohort(Integer.parseInt(nor));

                    result = rs.getString("patient_Id");

                    System.out.println("obs~" + rs.getString("id") + "~" + rs.getString("organization_id") + "~" + result + "~" + dead);

                    // one-off update (code needs removing after updates have been run)
                    //UpdateObsFilteredDelta(rs.getString("id"),rs.getString("organization_id"));

                    lastid = rs.getString("id");
                }

                preparedStatement.close();
            }

            // deleted
            q = "SELECT record_id, table_id ";
            q = q + "FROM "+dbreferences+".filteredDeletionsDelta";

            PreparedStatement zpreparedStatement = connection.prepareStatement(q);
            ResultSet zrs = zpreparedStatement.executeQuery();
            while (zrs.next()) {
                System.out.println("del~"+zrs.getString("record_id")+"~"+zrs.getString("table_id"));
            }

            zpreparedStatement.close();

            // cohort
            q = "SELECT * FROM "+dbreferences+".subscriber_cohort";
            zpreparedStatement = connection.prepareStatement(q);
            zrs = zpreparedStatement.executeQuery();
            String out = "";
            while (zrs.next()) {
                out = "cohort~"+zrs.getString("extractId")+"~"+zrs.getString("patientId")+"~"+zrs.getString("isBulked")+"~"+zrs.getString("needsDelete");
                System.out.println(out);
            }

            zpreparedStatement.close();

            // nor
            /*
            q = "SELECT f.id, j.organization_id ";
            q = q+"FROM "+dbreferences+".filteredPatientsDelta f ";
            q = q+"left join "+dbschema+".patient j on j.id = f.id"; // where j.organization_id=?";
            */

            q = "SELECT sc.patientId, f.id, j.organization_id, j.id ";
            q = q+"FROM "+dbreferences+".filteredPatientsDelta f ";
            q = q+"left join "+dbschema+".patient j on j.id = f.id ";
            q = q + "left join "+dbreferences+".subscriber_cohort sc on sc.patientId=j.id ";

            //System.out.println(q);

            zpreparedStatement = connection.prepareStatement(q);
            //preparedStatement.setString(1,organization);
            zrs = zpreparedStatement.executeQuery();

            while (zrs.next()) {
                nor = zrs.getString("id");
                // result = Deducted(Integer.parseInt(nor));
                // dead = Deceased(Integer.parseInt(nor));

                dead = "";
                //result = InCohort(Integer.parseInt(nor));
                result = zrs.getString("patientId");

                System.out.println("nor~"+zrs.getString("id")+"~"+zrs.getString("organization_id")+"~"+result+"~"+dead);
            }

            zpreparedStatement.close();

            // rx (left join returns nulls)
            /*
            q = "SELECT f.id, j.organization_id, j.patient_id ";
            q = q+"FROM "+dbreferences+".filteredMedicationsDelta f ";
            //q = q+"left join "+dbschema+".medication_statement j on j.id = f.id"; // where organization_id=?";
            q = q+"join "+dbschema+".medication_statement j on j.id = f.id";
            */

            q = "SELECT sc.patientId, f.id, j.organization_id, j.patient_id ";
            q = q+"FROM "+dbreferences+".filteredMedicationsDelta f ";
            q = q+"join "+dbschema+".medication_statement j on j.id = f.id ";
            q = q + "left join "+dbreferences+".subscriber_cohort sc on sc.patientId=j.patient_id ";

            //System.out.println(q);

            zpreparedStatement = connection.prepareStatement(q);
            //preparedStatement.setString(1,organization);
            zrs = zpreparedStatement.executeQuery();

            while (zrs.next()) {
                nor = zrs.getString("patient_id");
                // result = Deducted(Integer.parseInt(nor));
                // dead = Deceased(Integer.parseInt(nor));

                dead = "";
                // result = InCohort(Integer.parseInt(nor));
                result = zrs.getString("patientId");

                // one-off update (code needs removing after updates have been run)
                // UpdateRxFilteredDelta(zrs.getString("id"),zrs.getString("organization_id"));

                System.out.println("rx~"+zrs.getString("id")+"~"+zrs.getString("organization_id")+"~"+result+"~"+dead);
            }

            zpreparedStatement.close();

            // allergy
            /*
            q = "SELECT f.id, j.organization_id, j.patient_id ";
            q = q+"FROM "+dbreferences+".filteredAllergiesDelta f ";
            //q = q+"left join "+dbschema+".allergy_intolerance j on j.id = f.id"; // where j.organization_id=?";
            q = q+"join "+dbschema+".allergy_intolerance j on j.id = f.id";
            */

            q = "SELECT sc.patientId, f.id, j.organization_id, j.patient_id ";
            q = q+"FROM "+dbreferences+".filteredAllergiesDelta f ";
            q = q+"join "+dbschema+".allergy_intolerance j on j.id = f.id ";
            q = q + "left join "+dbreferences+".subscriber_cohort sc on sc.patientId=j.patient_id ";

            //System.out.println(q);

            zpreparedStatement = connection.prepareStatement(q);
            //preparedStatement.setString(1,organization);
            zrs = zpreparedStatement.executeQuery();

            while (zrs.next()) {
                nor = zrs.getString("patient_id");

                // result = Deducted(Integer.parseInt(nor));
                // dead = Deceased(Integer.parseInt(nor));

                dead = "";
                // result = InCohort(Integer.parseInt(nor));
                result = zrs.getString("patientId");

                System.out.println("allergy~"+zrs.getString("id")+"~"+zrs.getString("organization_id")+"~"+result+"~"+dead);
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

        System.out.println(preparedSql);

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

            refdate = props.getProperty("refdate");

            System.out.println("mysql url: "+ss[0]);
            System.out.println("mysql user: "+ss[1]);
            System.out.println("mysql pass: "+ss[2]);
            System.out.println("mysql db: "+dbschema);
            System.out.println("references db: "+dbreferences);
            System.out.println("redate: "+refdate);

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