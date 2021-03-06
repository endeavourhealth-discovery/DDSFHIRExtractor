package org.endeavourhealth.fihrexporter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

public class FihrExporterRunner {

    public static boolean Stop() {
        File tempFile = new File("/tmp/stop.txt");
        boolean exists = tempFile.exists();
        return exists;
    }

    public static void main(String... args) throws IOException, SQLException {

        Integer runit = 1;
        
        Properties properties = loadProperties( args );

        for (String s: args) {
            //System.out.println(s);

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

            if (ss[0].equals("resendpats")) {properties.setProperty("resendpats", ss[1]);}

            // ss[1] contains patient guid
            if (ss[0].equals("testpatget")) {properties.setProperty("testpatget", ss[1]);}
        }

        //System.out.println("fhirexporter will run "+runit+" times");

        String finished="0000";

        try (  FihrExporter csvExporter = new FihrExporter( properties  ) ) {
            for (int i=1; i <(runit+1); i++) {
                if (isTrue(Stop())) {
                    System.out.println("STOPPING RUNNER");
                    break;
                }
                finished = csvExporter.export(finished);
                if (finished.equals("1111")) {break;}
            }
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private static Properties loadProperties(String[] args) throws IOException {

        Properties properties = new Properties();

        InputStream inputStream = FihrExporterRunner.class.getClassLoader().getResourceAsStream("fihr.exporter.properties");

        properties.load( inputStream );

        return properties;
    }

}
