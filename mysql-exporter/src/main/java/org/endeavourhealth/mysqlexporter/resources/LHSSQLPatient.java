package org.endeavourhealth.mysqlexporter.resources;

import org.endeavourhealth.mysqlexporter.repository.Repository;

import java.io.File;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class LHSSQLPatient {
	public String Run(Repository repository)  throws SQLException
	{
		String result="";

		try {
			String OS = System.getProperty("os.name").toLowerCase();
			String file="//tmp//patient.txt";
			if (OS.indexOf("win") >= 0) {file="D:\\TEMP\\patient.txt";}
			PrintStream o = new PrintStream(new File(file));
			System.setOut(o);
		} catch (Exception e) {
			System.out.println(e);
		}

		List<Integer> ids = new ArrayList<>();
		if (repository.organization.isEmpty()) {
			ids = repository.getRows("Patient", "filteredPatientsDelta");
		}

		if (!repository.organization.isEmpty()) {
			ids = repository.getRowsFromReferences("Patient",repository.organization,"patient");
		}

		Integer id = 0; Integer j = 0;

		String headings="dds_id,add1,add2,add3,add4,city,postcode,dob,last,first,title,gender,nhsno,telecom";
		System.out.println(headings);

		while (ids.size() > j) {
			id = ids.get(j);

			result = repository.getPatientRS(id);
			if (result.length()==0) {continue;}

			String[] ss = result.split("~",-1);
			String nhsno=ss[0]; String odscode=ss[1]; String orgname=ss[2]; String orgpostcode=ss[3]; String telecom=ss[4]; String dod=ss[5];
			String add1=ss[6]; String add2=ss[7]; String add3=ss[8]; String add4=ss[9]; String city=ss[10]; String gender=ss[11]; String contype=ss[12]; String conuse=ss[13];
			String title=ss[15]; String first=ss[16]; String last=ss[17]; String start=ss[18]; String orgid=ss[19]; String dob=ss[20]; String postcode=ss[21];

			String out = id+","+add1+","+add2+","+add3+","+add4+","+city+","+postcode+","+dob+","+last+","+first+","+title+","+gender+","+nhsno+","+telecom;

			System.out.println(out);
			j++;
		}

		return "stuff";
	}
}