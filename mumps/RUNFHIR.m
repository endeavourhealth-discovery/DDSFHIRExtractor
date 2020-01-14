RUNFHIR ; ; 1/3/20 2:21pm
 ;
 ; KILLJOB:-
 ; $ydb_dist/MUPIP stop $job
 ;
 N I,ht,prac,pth,before
 
 ; 21600 = 6 am
 Set before=$g(^KRUNNING("BEFORE"),21600)
 
 LOCK ^KRUNNING("FHIR"):1 I '$T QUIT
 S ^KRUNNING("FHIR-JOB")=$J
 F I=1:1 Do  Q:$D(^DSYSTEM("FHIR-STOP"))
 .; if it's < 6am run the procs
 .S ht=$p($h,",",2)
 .I ht<before,'$D(^DSYSTEM("RUNPROCS",+$H)) D RUNPROCS
 .I ht>before do
 ..s prac=""
 ..f  s prac=$O(^DSYSTEM("FHIR-PRAC",prac)) Q:prac=""  do
 ...I $D(^DSYSTEM("RUNFHIR",+$H,prac)) QUIT
 ...s pth=^DSYSTEM("FHIR-PRAC",prac)
 ...i $p($h,",",2)>pth S ^DSYSTEM("RUNFHIR",+$H,prac)="" J SH^RUNFHIR(prac)
 ...quit
 ..quit
 .Hang 10
 .quit
 LOCK
 QUIT
 
SH(PRAC) ;
 N F
 S ^KRUNNING("FHIR-JOB")=$J
 S F="/tmp/runfhir-"_PRAC_".sh"
 C F
 O F:(newversion)
 U F W "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64",!
 U F W "export CONFIG_JDBC_USERNAME=",^DSYSTEM("JDBC_USERNAME"),!
 U F W "export CONFIG_JDBC_PASSWORD=",^DSYSTEM("JDBC_PASSWORD"),!
 U F W "export CONFIG_JDBC_URL=""",^DSYSTEM("JDBC_URL"),"""",!
 U F W "export CONFIG_JDBC_CLASS=",^DSYSTEM("JDBC_CLASS"),!
 U F W "java -jar /tmp/FihrExporter-1.0-SNAPSHOT-jar-with-dependencies.jar organization:",PRAC," dbschema:",^DSYSTEM("dbschema")," dbreferences:",^DSYSTEM("dbreference")," config:",^DSYSTEM("config")," runit:50",!
 C F
 S ^DSYSTEM("FHIR-AUDIT",$$I())="RUNFHIR~"_PRAC_"~START~"_$H
 zsystem "chmod +x /tmp/runfhir-"_PRAC_".sh"
 zsystem "/tmp/runfhir-"_PRAC_".sh"
 S ^DSYSTEM("FHIR-AUDIT",$$I())="RUNFHIR~"_PRAC_"~END~"_$H
 QUIT
 ;
 
SETUP ;
 ;S ^DSYSTEM("FHIR-PRAC",22232)=22500 ; 6:15am
 ;S ^DSYSTEM("JDBC_USERNAME")="?"
 ;S ^DSYSTEM("JDBC_PASSWORD")="?"
 ;S ^DSYSTEM("JDBC_URL")="?"
 ;S ^DSYSTEM("JDBC_CLASS")="?"
 ;S ^DSYSTEM("dbschema")="?"
 ;S ^DSYSTEM("dbreference")="?"
 ;S ^DSYSTEM("config")="?"
 QUIT
 
I() Q $I(^DSYSTEM("FHIR-AUDIT"))
 
RUNQUEUE ;
 N F,Q,file,i,STR,ID,ODSCODE,NAME,TYPE,ORG
 
 LOCK ^RUNQ
 
 S Q=$I(^RUNQ)
 S ^KRUNNING("FHIRQ-JOB")=$J
 S F="/tmp/runqueue.sh"
 C F
 O F:(newversion)
 U F W "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64",!
 U F W "export CONFIG_JDBC_USERNAME=",^DSYSTEM("JDBC_USERNAME"),!
 U F W "export CONFIG_JDBC_PASSWORD=",^DSYSTEM("JDBC_PASSWORD"),!
 U F W "export CONFIG_JDBC_URL=""",^DSYSTEM("JDBC_URL"),"""",!
 U F W "export CONFIG_JDBC_CLASS=",^DSYSTEM("JDBC_CLASS"),!
 U F W "java -Xmx1024m -jar /tmp/mysql-exporter-1.0-SNAPSHOT-jar-with-dependencies.jar queueinfo",!
 C F
 zsystem "chmod +x /tmp/runqueue.sh"
 zsystem "/tmp/runqueue.sh"
 
 zsystem "cp /tmp/qdata.txt /tmp/qdata"_Q_".txt"
 
 LOCK -^RUNQ
 
 ; create html/csv from /tmp/qdata.txt
 
 k ^REP($J),^ORG($J)
 
 s file="/tmp/qdata"_Q_".txt"
 close file
 
 o file:(readonly):0
 f i=1:1 use file read STR Q:$zeof  do
 .I $P(STR,"~",1)="org" DO  q
 ..S ID=$P(STR,"~",2),ODSCODE=$P(STR,"~",3),NAME=$$TR^LIB($P(STR,"~",4),"""","")
 ..S ^ORGS($j,ID)=NAME_"~"_ODSCODE
 ..QUIT
 .S TYPE=$P(STR,"~",1)
 .S ID=$P(STR,"~",2)
 .S ORG=$P(STR,"~",3)
 .S ^REP($j,TYPE,ORG)=$GET(^REP($j,TYPE,ORG))+1
 .quit
 
 close file
 
 K ^TEMP($J)
 S ORG="",COL=0
 F NODE="allergy","nor","obs","rx" do
 .S COL=COL+1
 .F  S ORG=$O(^REP($J,NODE,ORG)) Q:ORG=""  DO
 ..S $P(^TEMP($J,ORG),"~",COL)=^REP($J,NODE,ORG)
 ..Q
 .Q
 
 S ORG=""
 W "<html>",!
 W "<table BORDER=1>",!
 W "<td>org</td><td>allergies</td><td>patients</td><td>observations</td><td>medication</td><tr>",!
 F  S ORG=$O(^TEMP($J,ORG)) Q:ORG=""  D
 .s rec=^(ORG)
 .s all=$p(rec,"~",1),nor=$p(rec,"~",2),obs=$p(rec,"~",3),rx=$p(rec,"~",4)
 .I '$DATA(^ORGS($J,ORG)) QUIT
 .s orgname=$p(^ORGS($J,ORG),"~",1)
 .S odscode=$P(^ORGS($J,ORG),"~",2)
 .W "<td><b>",ORG,"</b>:",orgname," (",odscode,")</td>"
 .W "<td>",all,"</td>"
 .w "<td>",nor,"</td>"
 .w "<td>",obs,"</td>"
 .w "<td>",rx,"</td>"
 .w "<tr>",!
 .Q
 w "</table>",!
 w "</html>",!
 
 QUIT
 
RUNPROCS ;
 N F
 S ^KRUNNING("FHIRPROC-JOB")=$J
 S F="/tmp/runprocs.sh"
 C F
 O F:(newversion)
 U F W "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64",!
 U F W "export CONFIG_JDBC_USERNAME=",^DSYSTEM("JDBC_USERNAME"),!
 U F W "export CONFIG_JDBC_PASSWORD=",^DSYSTEM("JDBC_PASSWORD"),!
 U F W "export CONFIG_JDBC_URL=""",^DSYSTEM("JDBC_URL"),"""",!
 U F W "export CONFIG_JDBC_CLASS=",^DSYSTEM("JDBC_CLASS"),!
 U F W "java -jar /tmp/FihrExporter-1.0-SNAPSHOT-jar-with-dependencies.jar dbschema:",^DSYSTEM("dbschema")," dbreferences:",^DSYSTEM("dbreference")," config:",^DSYSTEM("config")," procrun:1",!
 C F
 S ^DSYSTEM("FHIR-AUDIT",$$I())="RUNPROCS~START~"_$H
 zsystem "chmod +x /tmp/runprocs.sh"
 zsystem "/tmp/runprocs.sh"
 S ^DSYSTEM("FHIR-AUDIT",$$I())="RUNPROCS~END~"_$H
 S ^DSYSTEM("RUNPROCS",+$H)=""
 QUIT
