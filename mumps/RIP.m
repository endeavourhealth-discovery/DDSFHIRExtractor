RIP(QID) ; ; 6/8/20 8:54am
 ; J ^RIP(1) process q-1
 ; J ^RIP(2) process q-2
 D START
 F I=1:1 Q:$data(^DSYSTEM("STOP-SENDING"))  D STT(QID)
 QUIT
 
NOW(QID) ; old code (run in foreground)
 S ORGID=""
 F  S ORGID=$ORDER(^QF("Q",QID,ORGID)) Q:ORGID=""  D
 .I $$ORGCHK^RUNFHIR2(ORGID)>0 W !,"Org already running" QUIT
 .i $data(^DSYSTEM("STOP-SENDING")) QUIT
 .D SH^RUNFHIR(ORGID)
 .QUIT
 QUIT
 
STT(QID) 
 S ORGID=""
 ;
 F  S ORGID=$ORDER(^QF("Q",QID,ORGID),-1) Q:ORGID=""  D
 .I $$ORGCHK^RUNFHIR2(ORGID)>0 W !,"Org already running" QUIT
 .W !!,ORGID,!!
 .i $data(^DSYSTEM("STOP-SENDING")) QUIT
 .D SH^RUNFHIR(ORGID)
 .QUIT
 QUIT
STATUS ;
 S X=$$CHK^RUNFHIR2("FihrExporter-1.0-SNAPSHOT-jar-with-dependencies.jar")
 ;ZWR ^TEMP($J,*)
 QUIT
 
STOP D STOP^RUNFHIR2
 S ^DSYSTEM("STOP-SENDING")=1
 QUIT
 
START D FREE^RUNFHIR2
 K ^DSYSTEM("STOP-SENDING")
 QUIT
 
PROCS ; J PROCS^RIP
 ; cohort proc gets run at midnight
 S ^KRUNNING("RUNNING-PROCS")=$JOB
 f i=1:1 do  q:$data(^DSYSTEM("STOP-PROCS"))
 .H 5
 .S ht=$Piece($H,",",2)
 .;W !,ht
 .i ht<1800 D ; between midnight and 12:30 am
 ..I $data(^DSYSTEM("COHORT",+$H)) QUIT
 ..S ^DSYSTEM("COHORT",+$H)=$H
 ..D RUNCOHORT
 ..QUIT
 .I ht>32400,ht<34200 DO ; between 9 and 9:30 am
 ..I $data(^DSYSTEM("DELTAS",+$H,9)) QUIT
 ..S ^DSYSTEM("DELTAS",+$H,9)=$H
 ..D RUNDELTAS
 ..QUIT
 .I ht>61200,ht<63000 DO ; between 5 and 5:30 pm
 ..I $data(^DSYSTEM("DELTAS",+$H,1700)) QUIT
 ..S ^DSYSTEM("DELTAS",+$H,1700)=$H
 ..D RUNDELTAS
 ..QUIT
 .quit
 S ^KRUNNING("EXITING-PROCS")=$J
 QUIT
 ;
RUNCOHORT ;
 W !,"RUNNING COHORT",!
 S F="/tmp/runcohortproc.sh"
 C F
 O F:(newversion)
 U F W "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64",!
 U F W "export CONFIG_JDBC_USERNAME=",^DSYSTEM("JDBC_USERNAME"),!
 U F W "export CONFIG_JDBC_PASSWORD=",^DSYSTEM("JDBC_PASSWORD"),!
 U F W "export CONFIG_JDBC_URL=""",^DSYSTEM("JDBC_URL"),"""",!
 U F W "export CONFIG_JDBC_CLASS=",^DSYSTEM("JDBC_CLASS"),!
 U F W "java -Xmx1024m -jar /tmp/mysql-exporter-1.0-SNAPSHOT-jar-with-dependencies.jar runcohortproc",!
 C F
 zsystem "chmod +x /tmp/runcohortproc.sh"
 zsystem "/tmp/runcohortproc.sh"
 QUIT
RUNDELTAS ;
 W !,"RUNNING DELTAS",!
 S F="/tmp/rundeltas.sh"
 C F
 O F:(newversion)
 U F W "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64",!
 U F W "export CONFIG_JDBC_USERNAME=",^DSYSTEM("JDBC_USERNAME"),!
 U F W "export CONFIG_JDBC_PASSWORD=",^DSYSTEM("JDBC_PASSWORD"),!
 U F W "export CONFIG_JDBC_URL=""",^DSYSTEM("JDBC_URL"),"""",!
 U F W "export CONFIG_JDBC_CLASS=",^DSYSTEM("JDBC_CLASS"),!
 U F W "java -Xmx1024m -jar /tmp/mysql-exporter-1.0-SNAPSHOT-jar-with-dependencies.jar rundeltaprocs",!
 CLOSE F
 zsystem "chmod +x /tmp/rundeltas.sh"
 zsystem "/tmp/rundeltas.sh"
 QUIT
