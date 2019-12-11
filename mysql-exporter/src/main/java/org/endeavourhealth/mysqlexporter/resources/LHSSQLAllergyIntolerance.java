package org.endeavourhealth.mysqlexporter.resources;

import org.endeavourhealth.mysqlexporter.repository.Repository;

import java.io.File;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class LHSSQLAllergyIntolerance {
	public String Run(Repository repository)  throws SQLException
	{
		String result="";

		try {
			String OS = System.getProperty("os.name").toLowerCase();
			String file="//tmp//allery.txt";
			if (OS.indexOf("win") >= 0) {file="D:\\TEMP\\allergy.txt";}
			PrintStream o = new PrintStream(new File(file));
			System.setOut(o);
		} catch (Exception e) {
			System.out.println(e);
		}

		List<Integer> ids = new ArrayList<>();
		if (repository.organization.isEmpty()) {
			ids = repository.getRows("AllergyIntolerance","filteredAllergiesDelta");
		}

		if (!repository.organization.isEmpty()) {
			ids = repository.getRowsFromReferences("AllergyIntolerance",repository.organization,"allergy_intolerance");
		}

		Integer id = 0; Integer j = 0;

		String headings="dds_id,patient_id,date,code,term";
		System.out.println(headings);

		while (ids.size() > j) {
			id = ids.get(j);

			result = repository.getAllergyIntoleranceRS(id);

			String[] ss = result.split("\\~");
			String nor=ss[0]; String date=ss[1]; String term=ss[2]; String code=ss[3];
			String out=id+","+nor+","+date+","+code+","+term;

			System.out.println(out);
			j++;
		}

		return "stuff";
	}
}