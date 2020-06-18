DUPS ; ; 6/17/20 12:54pm
 S org=""
 F  S org=$order(^QF("Q",99,org)) q:org=""  do
 .D RUN(org)
 .D PROCESS(org)
 .quit
 q
 
DEL ;
 ; create filteredobs.csv
 S F2="/tmp/DELQ.txt"
 C F2
 O F2:(newversion)
 ; create DELQ.txt from ^DELME
 S (ORG,ID)="",C=0,SQL=""
 F  S ORG=$O(^DELME(ORG)) Q:ORG=""  D
 .F  S ID=$O(^DELME(ORG,ID)) Q:ID=""  D
 ..I C#2000=0 D
 ...I SQL'="" U F2 W SQL,")",!
 ...S SQL="DELETE FROM data_extracts.filteredObservationsDelta where id in ("_ID
 ...QUIT
 ..I C#2000>0 S SQL=SQL_","_ID
 ..S C=C+1
 ..QUIT
 I SQL'="" U F2 W SQL,")",!
 C F2
 QUIT
PROCESS(org) 
 ;
 s file="/tmp/OBSDATA.txt"
 close file
 o file:(readonly):0
 F  U file R STR Q:$ZEOF  DO
 .S JSON=$P(STR,"~",10)
 .KILL B,JSON(1)
 .S JSON(1)=JSON
 .D DECODE^VPRJSON($NAME(JSON(1)),$NAME(B),$NAME(E))
 .S ID=$P(STR,"~",1)
 .S ITE=$P(STR,"~",6)
 .S DISPLAY=$P(STR,"~",8)
 .S DATE=$P(STR,"~",4)
 .S VALUE=$P(STR,"~",5)
 .S ORG=$P(STR,"~",7)
 .I VALUE="null" S VALUE=""
 .S ^OBSREC(ORG,ID)=ITE_"~"_DISPLAY_"~"_DATE_"~"_VALUE
 .; just a single obs record
 .I '$D(B) QUIT
 .I '$D(B("component")) D SINGLE(ORG)
 .I $D(B("component")) D COMPONENT(ORG)
 .Q
 .quit
 close file
 
delme 
 ; SEE WHATS THE SAME
 K ^DELME(org)
 S (ORG,ID)=""
 F  S ORG=$O(^OBSREC(ORG)) Q:ORG=""  DO
 .F  S ID=$O(^OBSREC(ORG,ID)) Q:ID=""  DO
 ..S REC=^OBSREC(ORG,ID)
 ..I $P(REC,"~",4)'="" S $P(REC,"~",4)=+$P(REC,"~",4)
 ..I REC'=$GET(^JSONREC(ORG,ID)) W !!,ID,"* ",REC,!,$GET(^JSONREC(ORG,ID)) Q ; R *Y QUIT
 ..S ^DELME(ORG,ID)=""
 ..QUIT
 .QUIT
 QUIT
  
RUN(org) ;
 ;W !,"Running getobsdata for org:",org
 S F="/tmp/rungetobsdata.sh"
 C F
 O F:(newversion)
 U F W "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64",!
 U F W "export CONFIG_JDBC_USERNAME=",^DSYSTEM("JDBC_USERNAME"),!
 U F W "export CONFIG_JDBC_PASSWORD=",^DSYSTEM("JDBC_PASSWORD"),!
 U F W "export CONFIG_JDBC_URL=""",^DSYSTEM("JDBC_URL"),"""",!
 U F W "export CONFIG_JDBC_CLASS=",^DSYSTEM("JDBC_CLASS"),!
 U F W "java -Xmx1024m -jar /tmp/mysql-exporter-1.0-SNAPSHOT-jar-with-dependencies.jar getobsdata organziation:",org,!
 C F
 zsystem "chmod +x /tmp/rungetobsdata.sh"
 zsystem "/tmp/rungetobsdata.sh"	
 quit
 
SINGLE(ORG) ;
 S ITE=B("code","coding",1,"code")
 S DISPLAY=B("code","coding",1,"display")
 S DATE=$GET(B("effectivePeriod","start"))
 S ID=B("identifier",1,"value")
 S VALUE=""
 S ^JSONREC(ORG,ID)=ITE_"~"_DISPLAY_"~"_$P(DATE,"T")_"~"_VALUE
 QUIT
 
COMPONENT(ORG) ;
 S (C1,C2)=""
 F  S C1=$O(B("component",C1)) Q:C1=""  DO
 .F  S C2=$O(B("component",C1,"code","coding",C2)) Q:C2=""  DO
 ..S ID=B("component",C1,"code","coding",C2,"id")
 ..S ITE=B("component",C1,"code","coding",C2,"code")
 ..S DISPLAY=B("component",C1,"code","coding",C2,"display")
 ..S VALUE=$GET(B("component",C1,"valueQuantity","value"))
 ..S ^JSONREC(ORG,ID)=ITE_"~"_DISPLAY_"~"_$P(DATE,"T")_"~"_VALUE
 ..QUIT
 .Q
 QUIT
