Q ; ; 7/16/20 6:50am
 K ^REP($J)
 D RUNQUEUE^RUNFHIR
 S file="/tmp/qdata.txt"
 close file
 o file:(readonly):0
 f i=1:1 use file read STR Q:$zeof  do
 .I $P(STR,"~",1)="org" quit
 .S TYPE=$P(STR,"~",1)
 .S ORG=$P(STR,"~",3)
 .I TYPE="obs" S ^REP($J,ORG,TYPE)=$P(STR,"~",4) Q
 .S ^REP($j,ORG,TYPE)=$GET(^REP($j,ORG,TYPE))+1
 .quit
 close file
 k orgs
 D GREP(.orgs)
 S q="",org="",C=1
 S (OTOT,RXTOT,PATTOT)=0
 f  s q=$o(^QF("Q",q)) q:q=""  do
 .f  s org=$o(^QF("Q",q,org)) q:org=""  do
 ..s pointer=""
 ..S OBS=$GET(^REP($J,org,"obs"))
 ..S RX=$G(^REP($J,org,"rx"))
 ..S PATS=$G(^REP($J,org,"nor"))
 ..i $d(orgs(org)) s pointer="<<<==="
 ..w !,C," ",q," ",org," o:",OBS," rx:",RX," nor:",PATS," ",pointer
 ..S OTOT=OTOT+OBS,RXTOT=RXTOT+RX,PATTOT=PATTOT+PATS
 ..S C=C+1
 ..quit
 .quit
 ; ORDER BY TOT
 S (org,type)=""
 k ^TSORT($j)
 f  s org=$o(^REP($J,org)) q:org=""  d
 .f  s type=$o(^REP($J,org,type)) q:type=""  d
 ..s tot=+^(type)
 ..s ^TSORT($j,tot,type,org)=""
 ..q
 .quit
 W !,"OTOT=",OTOT," RXTOT=",RXTOT," PATTOT=",PATTOT
 QUIT
 
CHK ;
 S (TOT,TYPE,ORG)="",T=0
 F  S TOT=$O(^TSORT($J,TOT)) Q:TOT=""  DO
 .;
 .F  S TYPE=$O(^TSORT($J,TOT,TYPE)) Q:TYPE=""  DO
 ..F  S ORG=$O(^TSORT($J,TOT,TYPE,ORG)) Q:ORG=""  D
 ...I '$D(^TSORT($J,TOT,"obs",ORG)) q
 ...w !,ORG," ",TOT
 ...S T=T+TOT
 ...I '$D(^QF("Q",1,ORG)) W !,"? ",ORG
 ...Q
 ..Q
 .QUIT
 W !,T
 QUIT
 
GREP(orgs) ; 
 zsystem "ps aux | grep FihrExporter > /tmp/grep"_$J_".txt"
 set file="/tmp/grep"_$J_".txt"
 open file:(readonly)
 f  u file r str q:$zeof  d
 .i str["organization:" s orgs($P($P(str,"organization:",2)," "))=""
 .quit
 close file
 QUIT
 
 S C=0
 S ORGID="",Q=1
 F  S ORGID=$O(^CDB(ORGID)) Q:ORGID=""  DO
 .;S ^QF("Q",Q,ORGID)=""
 .;S C=C+1
 .I Q>4 S Q=1
 .S ^QF("Q",Q,ORGID)=""
 .S Q=Q+1
 .QUIT
 QUIT
