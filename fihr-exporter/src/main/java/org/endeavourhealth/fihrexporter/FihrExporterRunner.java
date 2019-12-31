package org.endeavourhealth.fihrexporter;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

public class FihrExporterRunner {

    public static void main(String... args) throws IOException, SQLException {

        Integer runit = 1;

        Properties properties = loadProperties( args );

        for (String s: args) {
            System.out.println(s);

            String[] ss = s.split("\\:");

            if (ss[0].equals("dbschema")) {
                properties.setProperty("dbschema", ss[1]);
            }
            if (ss[0].equals("dbreferences")) {properties.setProperty("dbreferences", ss[1]);}
            if (ss[0].equals("config")) {properties.setProperty("config", ss[1]);}
            if (ss[0].equals("organization")) {properties.setProperty("organization", ss[1]);}
            if (ss[0].equals("procrun")) {properties.setProperty("procrun", ss[1]);}

            // How many times do we run the export method?
            if (ss[0].equals("runit")) {runit=Integer.parseInt(ss[1]);}
        }

        System.out.println("fhirexporter will run "+runit+" times");

        try (  FihrExporter csvExporter = new FihrExporter( properties  ) ) {
            for (int i=1; i <(runit+1); i++) {
                csvExporter.export();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private static Properties loadProperties(String[] args) throws IOException {

        Properties properties = new Properties();

        InputStream inputStream = FihrExporterRunner.class.getClassLoader().getResourceAsStream("fihr.exporter.properties");

        properties.load( inputStream );

        return properties;
    }

}
