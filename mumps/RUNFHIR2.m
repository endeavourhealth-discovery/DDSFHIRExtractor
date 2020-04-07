RUNFHIR2 ; ; 4/2/20 3:43pm
 ;
STOPANDCHK() 
 n hm
 D STOP
 HANG 5
 S hm=$$HOWMANY()
 QUIT hm
 
HOWMANY() ;
 QUIT $$CHK("runit:")
 ;
ORGCHK(org) ; is this organization running?
 S jarstr="organization:"_org
 S chk=$$CHK(jarstr)
 quit chk
 
 ; procrun:
 ; runit:
CHK(jarstr) 
 new file,q,str
 ;zsystem "ps aux | grep "_jarstr_" > /tmp/grep.txt"
 ; full list
 L ^KRUNNING("GREP"):3
 I '$T Q 0
 zsystem "ps aux > /tmp/grep.txt"
 set file="/tmp/grep.txt"
 open file:(readonly)
 set q=0
 f  u file r str q:$zeof  d
 .i str[jarstr s q=q+1 u 0 w !,str
 .quit
 close file
 L -^KRUNNING("GREP")
 quit q
STOP 
 ; stop fhirexctractors
 zsystem "touch /tmp/stop.txt" 
 QUIT
FREE ;
 zsystem "rm /tmp/stop.txt"
 QUIT
 
MYSQL(ORG) 
 N F
 S F="/tmp/mysql.sh"
 C F
 O F:(newversion)
 U F W "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64",!
 U F W "export CONFIG_JDBC_USERNAME=",^DSYSTEM("JDBC_USERNAME"),!
 U F W "export CONFIG_JDBC_PASSWORD=",^DSYSTEM("JDBC_PASSWORD"),!
 U F W "export CONFIG_JDBC_URL=""",^DSYSTEM("JDBC_URL"),"""",!
 U F W "export CONFIG_JDBC_CLASS=",^DSYSTEM("JDBC_CLASS"),!
 U F W "java -Xmx1024m -jar /tmp/mysql-exporter-1.0-SNAPSHOT-jar-with-dependencies.jar organization:"_ORG,!
 C F
 zsystem "chmod +x /tmp/mysql.sh"
 zsystem "/tmp/mysql.sh"
 QUIT
