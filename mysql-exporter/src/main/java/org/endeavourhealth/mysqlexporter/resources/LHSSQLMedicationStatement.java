package org.endeavourhealth.mysqlexporter.resources;

import org.endeavourhealth.mysqlexporter.repository.Repository;

import java.io.File;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class LHSSQLMedicationStatement {
	public String Run(Repository repository)  throws SQLException
	{
		String result="";

		try {
			String OS = System.getProperty("os.name").toLowerCase();
			String file="//tmp//rx.txt";
			if (OS.indexOf("win") >= 0) {file="D:\\TEMP\\rx.txt";}
			PrintStream o = new PrintStream(new File(file));
			System.setOut(o);
		} catch (Exception e) {
			System.out.println(e);
		}

		List<Integer> ids = new ArrayList<>();
		if (repository.organization.isEmpty()) {
			ids = repository.getRows("MedicationStatement","filteredMedicationsDelta");
		}

		if (!repository.organization.isEmpty()) {
			ids = repository.getRowsFromReferences("MedicationStatement",repository.organization,"medication_statement");
		}

		Integer id = 0; Integer j = 0;

		String headings="dds_id,patient_id,qty_unit,qty_value,dose_txt,date,code,drugname";
		System.out.println(headings);

		while (ids.size() > j) {
			id = ids.get(j);

			result = repository.getMedicationStatementRS(id);

            String[] ss = result.split("\\`");
            String nor = ss[0]; String code=ss[1]; String drugname=ss[2]; String dosetxt=ss[3];
            String qtyval=ss[4]; String qtyunit=ss[5]; String date=ss[6];

            String out = id+","+nor+","+qtyunit+","+qtyval+","+dosetxt+","+date+","+code+","+drugname;

			System.out.println(out);

			j++;
		}

		return "stuff";
	}
}