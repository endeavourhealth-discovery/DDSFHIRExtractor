package org.endeavourhealth.fihrexporter.repository;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.endeavourhealth.common.config.ConfigManager;

import java.sql.*;
import java.util.*;

public class Repository {

    private MysqlDataSource dataSource;

    private Connection connection;

    private String baseURL;

    public String outputFHIR; public String dbschema; public String clientid;
    public String clientsecret; public String scope; public String granttype;
    public String tokenurl; public String token; public String runguid;
    public Integer scaletotal; public Integer counting;
    public String config; public String dbreferences;
    public String organization;

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
        String q = "SELECT * FROM "+dbreferences+".references WHERE strid='" + code + "' AND resource='" + resource +" '";

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            preparedStatement.close();
            return true;
        }
        preparedStatement.close();
        return false;
    }

    public boolean PreviouslyPostedId(Integer id, String resource) throws SQLException {

        String q = "SELECT * FROM "+dbreferences+".references WHERE an_id='" + id.toString() + "' AND resource='" + resource + " '";

        PreparedStatement preparedStatement = connection.prepareStatement(q);
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

        String q = "SELECT * FROM "+dbreferences+".references WHERE location='" + location + "'";

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        ResultSet rs = preparedStatement.executeQuery();

        while (rs.next()) {
            ids=ids+rs.getInt("an_id")+"~";
        }

        preparedStatement.close();
        return ids;
    }

    public void InsertBackIntoObsQueue(Integer id) throws SQLException {
        // does the id already exist in filteredobservationsdelta?
        String q ="select id from "+dbreferences+".filteredObservationsDelta where id="+id;

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        ResultSet rs = preparedStatement.executeQuery();

        Boolean alreadyinq = false;
        if (rs.next()) {
            alreadyinq = true;
        }

        preparedStatement.close();

        if (alreadyinq==true) return;

        q ="insert into "+dbreferences+".filteredObservationsDelta (id) values(?)";
        System.out.println("back into q "+q);

        PreparedStatement preparedStmt = connection.prepareStatement(q);
        preparedStmt.setInt(1, id);
        preparedStmt.execute();
        preparedStmt.close();
    }

    public String getLocationObsWithCheckingDeleted(Integer anid) throws SQLException {
        String location = "";

        String q = "SELECT * FROM "+dbreferences+".references WHERE an_id='" + anid + "' AND resource='Observation'";

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) { location =  rs.getString("location"); }

        preparedStatement.close();
        return location;
    }

    public String getLocation(Integer anid, String resource) throws SQLException {
        String location = "";

        String q = "SELECT * FROM "+dbreferences+".references WHERE an_id='" + anid + "' AND resource='" + resource + "'";

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) { location =  rs.getString("location"); }

        preparedStatement.close();

        // Has the resource been deleted?
        if (location.length()>0) {
            q = "SELECT * FROM "+dbreferences+".references WHERE an_id='" + anid + "' AND resource='DEL:" + resource + "'";
            preparedStatement = connection.prepareStatement(q);
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

        String q = "SELECT * FROM "+dbreferences+".references WHERE strid='" + snomedcode + "' AND resource='Medication'";

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) { location =  rs.getString("location"); }

        preparedStatement.close();
        return location;
    }

    public void DeleteTracker() throws SQLException
    {
        String q ="DELETE FROM "+dbreferences+".references where resource ='Tracker'";

        PreparedStatement preparedStmt = connection.prepareStatement(q);
        preparedStmt.execute();

        preparedStmt.close();
    }

    public void DeleteFileReferences() throws SQLException
    {
        String q ="DELETE FROM "+dbreferences+".references where response = 123";

        PreparedStatement preparedStmt = connection.prepareStatement(q);
        preparedStmt.execute();
    }

    private String getMap(String dbid)  throws SQLException
    {
        Integer legacy=0; Integer core=0; String r2="";

        String q = "SELECT legacy,core FROM subscriber_pi.concept_map WHERE core=?";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        preparedStatement.setString(1,dbid);

        ResultSet rs = preparedStatement.executeQuery();

        r2 = "";
        while(rs.next()) {
            legacy=rs.getInt("legacy");
            core=rs.getInt("core");
            if (legacy.intValue()==core.intValue()) { continue; }
            r2=legacy.toString();
            break;
        }
        return r2;
    }

    private String getConcept(String zcode) throws SQLException
    {
        String legacy=""; String code=""; String r2 =""; String dbid="";
        String q = "SELECT dbid FROM subscriber_pi.concept WHERE code="+zcode;
        PreparedStatement preparedStatement = connection.prepareStatement(q);
        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            dbid = rs.getString("dbid");
            r2 = getMap(dbid);
        }

        preparedStatement.close();

        return r2;
    }

    private String getCode(String r2) throws SQLException
    {
        String code =""; String desc="";
        String q = "SELECT code,description FROM subscriber_pi.concept where dbid="+r2;
        PreparedStatement preparedStatement = connection.prepareStatement(q);

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            code = rs.getString("code");
            desc = rs.getString("description");
        }

        preparedStatement.close();

        return code+"~"+desc;
    }

    public void getTerms() throws SQLException
    {
        String code=""; String dbid=""; String r2=""; String str="";
        String q ="SELECT * FROM "+dbreferences+".snomed_code_set_codes";
        PreparedStatement preparedStatement = connection.prepareStatement(q);

        ResultSet rs = preparedStatement.executeQuery();

        while(rs.next()) {
            code = rs.getString("snomedCode");
            r2=getConcept(code);
            if (r2!="") {
                str = getCode(r2);
                System.out.println(str);
            }
            if (r2=="") {
                System.out.println("? "+code+" "+r2);
            }
        }
        preparedStatement.close();
    }

    private void PurgetheQueue(Integer anId, String resource) throws SQLException
    {
        // purge the queues
        String table = ""; String q = "";

        if (resource=="Patient") {table=dbreferences+".filteredPatientsDelta";}
        //if (resource=="Observation") {table="data_extracts.filteredobservationsdelta";}
        if (resource=="Observation") {table=dbreferences+".filteredObservationsDelta";}
        if (resource=="MedicationStatement") {table=dbreferences+".filteredMedicationsDelta";}
        if (resource== "AllergyIntolerance") {table=dbreferences+".filteredAllergiesDelta";};

        if (table.length()>0) {
            q = "DELETE FROM " + table + " where id='" + anId + "'";
            PreparedStatement preparedStmt = connection.prepareStatement(q);
            preparedStmt.execute();
            preparedStmt.close();
        }
    }

    public void PurgeTheDeleteQueue(Integer anId, String resource) throws SQLException
    {
        //filteredDeletionsDelta (id, type)
        //patient - 2, observation - 11, allergy - 4, medication - 10

        Integer type=0; String q="";

        if (resource=="Patient") {type=2;}
        if (resource=="Observation") {type=11;}
        if (resource=="MedicationStatement") {type=10;}
        if (resource=="AllergyIntolerance") {type=4;}

        if (type !=0) {
            q = "DELETE FROM filteredDeletionsDelta where record_id='"+anId+"' AND table_id='"+type+"'";
            PreparedStatement preparedStmt = connection.prepareStatement(q);
            preparedStmt.execute();
            preparedStmt.close();
        }
    }

    public boolean UpdateAudit(Integer anId, String strid, String encoded, Integer responseCode, String resource) throws SQLException
    {
        long timeNow = Calendar.getInstance().getTimeInMillis();
        java.sql.Timestamp ts = new java.sql.Timestamp(timeNow);
        String str = ts.toString();

        String q = "";

        encoded = encoded.replaceAll("'","''");

        if (anId != 0) {
            q = "update "+dbreferences+".references set response = " + responseCode + ", datesent = '"+str+"', json = '"+encoded+"' where an_id = '"+anId+"' and resource='"+resource+"' and response<>'1234'";
            PurgetheQueue(anId, resource);
        }

        if (strid.length() > 0) {
            q = "update "+dbreferences+".references set response = " + responseCode + ", datesent = '" + str + "', json = '" + encoded + "' where strid = '"+strid+"' and resource='"+resource+"' and response<>'1234'";
        }

        //System.out.println(q);

        PreparedStatement preparedStmt = connection.prepareStatement(q);
        preparedStmt.execute();

        preparedStmt.close();

        return true;
    }

    public boolean Audit(Integer anId, String strid, String resource, Integer responseCode, String location, String encoded, Integer patientid, Integer typeid) throws SQLException
    {

        String q = "insert into "+dbreferences+".references (an_id,strid,resource,response,location,datesent,json,patient_id,type_id,runguid) values(?,?,?,?,?,?,?,?,?,?)";

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

        if (this.outputFHIR==null && anId != 0) {
            PurgetheQueue(anId, resource);
        }

        return true;
    }

    public String getOrganizationRS(Integer organization_id) throws SQLException {

        String result = "";
        //String q = "SELECT * FROM subscriber_pi.organization where id = '" + organization_id + "'";
        String q = "SELECT * FROM "+dbschema+".organization where id = '" + organization_id + "'";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

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
        String addresses="";

        String q = "select * from "+dbschema+".patient_address where id <> "+curraddid+" AND patient_id="+patientid.toString();

        PreparedStatement preparedStatement = connection.prepareStatement(q);
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

        //String q = "select pc.value, cctype.name as contact_type, ccuse.name as contact_use ";
        //q = q + "from subscriber_pi.patient_contact pc " + "left outer join subscriber_pi.concept ccuse on ccuse.dbid = pc.use_concept_id "
        //        + "left outer join subscriber_pi.concept cctype on cctype.dbid = pc.type_concept_id where pc.patient_id = '"+patientid.toString()+"'";

        String q = "select pc.value, cctype.name as contact_type, ccuse.name as contact_use ";
        q = q + "from "+dbschema+".patient_contact pc " + "left outer join "+dbschema+".concept ccuse on ccuse.dbid = pc.use_concept_id "
                + "left outer join "+dbschema+".concept cctype on cctype.dbid = pc.type_concept_id where pc.patient_id = '"+patientid.toString()+"'";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

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


        q = "select " + "ms.id," + "ms.patient_id," + "ms.dose," + "ms.quantity_value," + "ms.quantity_unit," + "ms.clinical_effective_date,"
                + "c.name as medication_name," + "c.code as snomed_code, c.name as drugname "
                + "from "+dbschema+".medication_statement ms "
                // + "join "+dbschema+".concept_map cm on cm.legacy = ms.non_core_concept_id "
                // + "join "+dbschema+".concept c on c.dbid = cm.core "
                + "join "+dbschema+".concept c on c.dbid = ms.non_core_concept_id "
                + "where ms.id = '" + record_id + "'";

        //System.out.println(q);
        //Scanner scan = new Scanner(System.in);
        //System.out.print("Press any key to continue . . . ");
        //scan.nextLine();

        PreparedStatement preparedStatement = connection.prepareStatement(q);

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
                ////+ "join "+dbschema+".concept c on c.dbid = ms.non_core_concept_id\r\n"
                + "where ms.id = '" + record_id + "'";

        PreparedStatement preparedStatement = connection.prepareStatement(q);

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

    public String getObservationRecordNew(String id) throws SQLException {

        String obsrec = ""; String snomedcode = ""; String orginalterm = "";
        String result_value = ""; String clineffdate = ""; String resultvalunits = "";

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
                + "where o.id = '"+id+"'";

        PreparedStatement preparedStatement = connection.prepareStatement(q);
        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            snomedcode = rs.getString(3); orginalterm = rs.getString(4);
            result_value = rs.getString(5); clineffdate = rs.getString(6); resultvalunits = rs.getString(8);
            noncoreconceptid = rs.getInt("non_core_concept_id");
            obsrec = snomedcode + "~" + orginalterm + "~" + result_value + "~" + clineffdate + "~" + resultvalunits + "~" + noncoreconceptid;
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
                    + "where o.id = '"+id+"'";

            preparedStatement = connection.prepareStatement(q);
            rs = preparedStatement.executeQuery();
            if (rs.next()) { ;
                result_value = rs.getString("result_value"); clineffdate = rs.getString("clinical_effective_date"); resultvalunits = rs.getString("result_value_units");
                noncoreconceptid = rs.getInt("non_core_concept_id"); orginalterm=rs.getString("original_term");
                snomedcode = rs.getString("snomed_code");
                obsrec = snomedcode+"~"+orginalterm+"~"+result_value+"~"+clineffdate+"~"+resultvalunits+"~"+noncoreconceptid;
            }
            preparedStatement.close();
        }

        return obsrec;
    }

    public String getObservationRecord(String id) throws SQLException {

        String obsrec = ""; String snomedcode = ""; String orginalterm = "";
        String result_value = ""; String clineffdate = ""; String resultvalunits = "";

        Integer noncoreconceptid = 0;

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

        PreparedStatement preparedStatement = connection.prepareStatement(q);
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
        String ids = "";

        //String q = "SELECT id FROM subscriber_pi.observation WHERE parent_observation_id="+parentid;
        String q = "SELECT id FROM "+dbschema+".observation WHERE parent_observation_id="+parentid;

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        ResultSet rs = preparedStatement.executeQuery();

        while(rs.next()) {
            ids = ids + rs.getString(1) + "~";
        }

        preparedStatement.close();

        return ids;
    }

    public String getObservationRSNew(Integer record_id) throws SQLException {
        String result = "";

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

        PreparedStatement preparedStatement = connection.prepareStatement(q);

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            Integer nor = rs.getInt("patient_id"); String snomedcode = rs.getString("snomed_code"); String orginalterm = rs.getString("original_term");
            String result_value = rs.getString("result_value"); String clineffdate = rs.getString("clinical_effective_date"); String resultvalunits = rs.getString("result_value_units");

            if (rs.getString("result_value") == null) {result_value="";}
            if (rs.getString("result_value_units") == null) {resultvalunits="";}

            result = nor.toString()+"~"+snomedcode+"~"+orginalterm+"~"+result_value+"~"+clineffdate+"~"+resultvalunits+"~"+rs.getInt("parent_observation_id");
        }

        if (result.length()==0) {
            System.out.println(q);
        }

        preparedStatement.close();

        return result;
    }

    public String getObservationRS(Integer record_id) throws SQLException {
        String result = "";

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

        PreparedStatement preparedStatement = connection.prepareStatement(q);

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
        String q = "select ";
        String result = "";

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

        PreparedStatement preparedStatement = connection.prepareStatement(q);

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
        PreparedStatement preparedStatement = connection.prepareStatement(q);

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

    public String getPatientRS(Integer patient_id) throws SQLException {

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

        PreparedStatement preparedStatement = connection.prepareStatement(q);

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

    private Integer getPatientId(String id, String tablename) throws SQLException {
        Integer nor=0;

        if (tablename.equals("patient")) {return Integer.parseInt(id);}

        if (tablename.length()==0) return 0;

        String preparedSql = "select patient_id from "+dbschema+"."+tablename+" where id="+id;

        PreparedStatement preparedStatement = connection.prepareStatement( preparedSql );
        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            nor = rs.getInt("patient_id");
        }

        preparedStatement.close();

        return nor;
    }

    public List<List<String>> getDeleteRows() throws SQLException {
        String preparedSql = "select * from "+dbreferences+".filteredDeletionsDelta";
        PreparedStatement preparedStatement = connection.prepareStatement( preparedSql );
        ResultSet rs = preparedStatement.executeQuery();
        List<List<String>> result = new ArrayList<>();

        Integer recid=0; Integer tableid=0; Integer nor=0; String resource="";
        String tablename="";

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

            nor = getPatientId(recid.toString(), tablename);

            List<String> row = new ArrayList<>();
            row.add(recid.toString());
            row.add(tableid.toString());
            row.add(nor.toString());
            row.add(tablename);
            row.add(resource);
            result.add(row);
        }

        preparedStatement.close();

        return result;
    }

    public List<Integer> getRows(String table) throws SQLException {
        String preparedSql = "select * from "+ dbreferences+"."+table;

        //String prepareOrgSQL = "select t.id, j.organization_id from "+dbreferences+"."+table+ "t ";

        String j ="";
        if (table.equals("filteredObservationsDelta")) {j=" join "+dbschema+".observation j on t.id=j.id";}
        if (table.equals("filteredMedicationsDelta")) {j=" join "+dbschema+".medication_statement j on t.id=j.id";}
        if (table.equals("filteredAllergiesDelta")) {j=" join "+dbschema+".allergy_intolerance j on t.id=j.id";}

        if (!organization.isEmpty()) {
            preparedSql = "select t.id, j.organization_id from "+dbreferences+"."+table+ " t";
            preparedSql = preparedSql + j + " WHERE j.organization_id="+organization;
        }

        System.out.println(preparedSql);

        // preparedSql = preparedSql + " where id>14189471 order by id asc";

        PreparedStatement preparedStatement = connection.prepareStatement( preparedSql );
        ResultSet rs = preparedStatement.executeQuery();
        List<Integer> result = new ArrayList<>();
        Integer id = 0; Integer count = 0;

        while (rs.next()) {
            this.counting = this.counting + 1;
            if (this.counting > this.scaletotal) break;

            id = rs.getInt("id");

            List<Integer> row = new ArrayList<>();

            // 10k testing!
            //for (int i = 0; i < 14; i++) {
            //    List<Integer> row = new ArrayList<>();
            //    result.add(id);
            //}

            result.add(id);
        }
        preparedStatement.close();

        //List<Integer> result = new ArrayList<>();
        //result.add(56229);

        return result;
    }

    public List<Integer> getPatientRows() throws SQLException {

        // String preparedSql = "select * from data_extracts.cohort";
        String preparedSql = "select * from "+dbreferences+".filteredPatientsDelta";

        if (!organization.isEmpty()) {preparedSql ="SELECT p.id, p.organization_id FROM data_extracts.filteredPatientsDelta f join subscriber_pi.patient p on p.id = f.id where p.organization_id="+organization;}

        PreparedStatement preparedStatement = connection.prepareStatement( preparedSql );

        ResultSet rs = preparedStatement.executeQuery();

        List<Integer> result = new ArrayList<>();

        Integer patient_id = 0;

        while (rs.next()) {

            this.counting = this.counting + 1;
            if (this.counting > this.scaletotal) break;

            //patient_id = rs.getInt("patient_id");
            patient_id = rs.getInt("id");

            List<Integer> row = new ArrayList<>();

            result.add(patient_id);

        }

        preparedStatement.close();


        // Testing
        //List<Integer> result = new ArrayList<>();
        //result.add(28844);

        return result;
    }

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

        // #2
        q = "call buildCohortCodeSetDelta();";
        preparedStatement = connection.prepareStatement(q);
        rs = preparedStatement.executeQuery();
        preparedStatement.close();

        System.out.println("buildCohortCodeSetDelta "+rs);

        // #3
        q = "call buildKnowDiabetesObservationCodeSetDelta();";
        preparedStatement = connection.prepareStatement(q);
        rs = preparedStatement.executeQuery();
        preparedStatement.close();

        System.out.println("buildKnowDiabetesObservationCodeSetDelta "+rs);

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

            boolean ok = CreateFilteredTables();

            Scanner scan = new Scanner(System.in);
            System.out.print("Press any key to continue . . . ");
            scan.nextLine();

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
