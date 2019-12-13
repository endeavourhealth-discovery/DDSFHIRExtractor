KNOW2 ; ; 12/13/19 2:01pm
 ;
 
STT() 
 n endpoint
 
 set clientid=^DSYSTEM("FHIR","CLIENTID")
 
 set secret=^DSYSTEM("FHIR","SECRET")
 
 set scope=^DSYSTEM("FHIR","SCOPE")
 
 set endpoint=^DSYSTEM("FHIR","ENDPOINT")
 
 set cmd="curl -s -X POST -i -H ""Content-Type: application/x-www-form-urlencoded"""
 set cmd=cmd_" -d ""client_id="_clientid_""" -d ""client_secret="_secret_""" -d ""scope="_scope_""" -d ""grant_type=client_credentials"" "
 set cmd=cmd_endpoint_" > /tmp/b"_$job_".txt"
 
 ;w cmd
 
 ;Set ST=$zf(-1,cmd)
 zsystem cmd
 Set JSON(1)=$$TEMP("/tmp/b"_$J_".txt")
 
 ;W JSON
 
 KILL B
 ; VALIDATE THE ACCESS TOKEN
 D DECODE^VPRJSON($NAME(JSON(1)),$NAME(B),$NAME(E))
 
 QUIT B("access_token")
 
TEMP(file) 
 new z,ret,i,start
 ;break
 K data
 set data=""
 close file
 ;o file:("R"):0
 o file:(readonly):0
 ;i '$t break
 for i=1:1 use file read z q:$zeof  s data(i)=z
 close file
 
 zsystem "rm "_file
 
 f i=1:1 q:data(i)=""!(data(i)=$c(13))  s start=(i+1)
 
 S ret=""
 F i=start:1 q:$g(data(i))=""  S ret=ret_data(i) 
 q ret
 
dumpref ;
 ;S ^token=$$ST()
 ; run the java code to build dumpref.txt from data_extracts.references
 
 zsystem "rm /tmp/dumprefs.txt"
 
 w !,"running java code to create /tmp/dumprefs.txt"
 zsystem "java -jar /tmp/DatasetExtractor/mysql-exporter/target/mysql-exporter-1.0-SNAPSHOT.jar dumprefs"
 
 w !,"reading dump file"
 
 s file="/tmp/dumprefs.txt"
 o file:(readonly):0
 k ^AXIN("NOR")
 for i=1:1 use file read rec q:$zeof  d
 .u 0 w !,rec
 .s anid=$p(rec,"~",1),strid=$p(rec,"~",2)
 .s location=$p(rec,"~",3),resource=$p(rec,"~",4)
 .s nor=$p(rec,"~",5)
 .i nor=0 q
 .s idx=$order(^AXIN("NOR",nor,""),-1)+1
 .S ^AXIN("NOR",nor,idx)=anid_"~"_strid_"~"_location_"~"_resource
 .quit
 close file
 
 q
 
DATES(E,DATES) 
 N START
 S START=$GET(^B("entry",E,"resource","effectivePeriod","start"))
 S DATES(E)=START
 QUIT
 
GETDSEQTY(E,DDSE) 
 NEW N
 S N=""
 F  S N=$O(^B("entry",E,"resource","dosage",N)) Q:N=""  D
 .S UNIT=$GET(^B("entry",E,"resource","dosage",N,"doseQuantity","unit"))
 .S VALUE=$GET(^B("entry",E,"resource","dosage",N,"doseQuantity","value"))
 .S TEXT=$GET(^B("entry",E,"resource","dosage",N,"text"))
 .S DDSE(E)=UNIT_","_VALUE_","_TEXT
 .Q
 QUIT
 
REF(E,REFS) 
 S REF=^B("entry",E,"resource","medicationReference","reference")
 BREAK:REF="0b58d9b1-b6fc-48ee-b85f-7c71ade1b082"
 S REFS(E)=REF
 Q
 
MEDS(MEDS) ;
 ;W !
 ;ZW ^B
 S E=""
 F  S E=$O(^B("entry",E)) Q:E=""  D
 .S ID=^B("entry",E,"resource","id")
 .S CODE=^B("entry",E,"resource","code","coding",1,"code")
 .W !,CODE
 .;BREAK:CODE="13D-1"
 .S DISPLAY=^B("entry",E,"resource","code","coding",1,"display")
 .S MEDS(ID)=CODE_","_DISPLAY
 .Q
 S ID=""
 F  S ID=$O(MEDS(ID)) Q:ID=""  D
 .S CODE=$P(MEDS(ID),",",1)
 .S ^OUT("MEDS",CODE)=MEDS(ID)
 .S ^OUT("MEDS",CODE,ID)=""
 .QUIT
 QUIT
 
RX ;
 N IDS,DDSE,DATES,REFS,PATS
 S E=""
 F  S E=$O(^B("entry",E)) Q:E=""  D
 .D GETDDSID(E,.IDS)
 .D OBSPAT(E,.PATS)
 .D GETDSEQTY(E,.DDSE)
 .D DATES(E,.DATES)
 .D REF(E,.REFS)
 .S GUIDS(E)=^B("entry",E,"resource","id")
 .QUIT
 
 ;W !
 
 ;ZW IDS
 ;ZW DDSE
 ;ZW DATES
 ;ZW REFS ; should maybe bring all the medication resources back in one hit?
 
 S ID="",C=$O(^OUT("RX",""),-1)+1
 F  S ID=$O(IDS(ID)) Q:ID=""  D
 .S ZID=IDS(ID)
 .S DDSE=$GET(DDSE(ID))
 .S DATE=$P(DATES(ID),"T",1)
 .S REF=$P(REFS(ID),"/",2)
 .S NOR=$GET(PATS(ID))
 .W !,ZID,",",NOR,",",DDSE,",",DATE,",",$GET(MEDS(REF),"?")
 .S ^OUT("RX",C)=ZID_","_NOR_","_DDSE_","_DATE_","_$GET(MEDS(REF),"?")_","_REF_","_GUIDS(ID)
 .S C=C+1
 .QUIT
 
 QUIT
 
NEXT() ;
 S A="",NEXT=""
 F  S A=$O(^B("link",A)) Q:A=""  D  Q:NEXT'=""
 .I ^B("link",A,"relation")="next" S NEXT=^B("link",A,"url") Q
 .QUIT
 QUIT NEXT
 
GETDDSID(E,IDS) 
 NEW N,SYS
 S N=""
 F  S N=$O(^B("entry",E,"resource","identifier",N)) Q:N=""  D
 .S SYS=^B("entry",E,"resource","identifier",N,"system")
 .S VALUE=^B("entry",E,"resource","identifier",N,"value")
 .I SYS'["discovery" Q
 .I SYS["ddsparentid" S IDS(E,"P")=VALUE QUIT
 .S IDS(E)=VALUE
 .Q
 QUIT
 
GETADD(E,ADD) 
 NEW CITY
 S CITY=$GET(^B("entry",E,"resource","address",1,"city"))
 S ADD1=$GET(^B("entry",E,"resource","address",1,"line",1))
 S ADD2=$GET(^B("entry",E,"resource","address",1,"line",2))
 S ADD3=$GET(^B("entry",E,"resource","address",1,"line",3))
 S ADD4=$GET(^B("entry",E,"resource","address",1,"line",4))
 S POSTCODE=$GET(^B("entry",E,"resource","address",1,"postalCode"))
 S ADD(E)=ADD1_","_ADD2_","_ADD3_","_ADD4_","_CITY_","_POSTCODE
 QUIT
 
GETDOB(E,DOB) 
 S D=$GET(^B("entry",E,"resource","birthDate"))
 S DOB(E)=D
 QUIT
 
GETNAME(E,NAME) 
 ;break
 S F=$GET(^B("entry",E,"resource","name",1,"family"))
 S G=$GET(^B("entry",E,"resource","name",1,"given",1))
 S P=$GET(^B("entry",E,"resource","name",1,"prefix",1))
 S NAME(E)=F_","_G_","_P
 Q
 
GETGENDER(E,GENDER) 
 S G=^B("entry",E,"resource","gender")
 S GENDER(E)=G
 QUIT
 
GETTELECOM(E) 
 N N,REC
 S N="",REC=""
 F  S N=$O(^B("entry",E,"resource","telecom",N)) Q:N=""  DO
 .S USE=$GET(^B("entry",E,"resource","telecom",N,"use"))
 .S VALUE=$GET(^B("entry",E,"resource","telecom",N,"value"))
 .S SYSTEM=$GET(^B("entry",E,"resource","telecom",N,"system"))
 .S REC=REC_VALUE_"`"_SYSTEM_"`"_USE_"|"
 .QUIT
 QUIT REC
 
PAT ;
 N E,PATREF,IDS,ADD,DOBS,NAMES,GENDERS,NHSNO,TELECOM
 S E=""
 F  S E=$O(^B("entry",E)) Q:E=""  D
 .D GETDDSID(E,.IDS)
 .I $G(IDS(E))="" S IDS(E)="?"
 .D GETADD(E,.ADD)
 .D GETDOB(E,.DOBS)
 .D GETNAME(E,.NAMES)
 .D GETGENDER(E,.GENDERS)
 .S TELECOM=$$GETTELECOM(E)
 .S TELECOM(E)=TELECOM
 .;S ^DDS
 .S PATREF=^B("entry",E,"resource","id")
 .S ^DDS("PAT",PATREF)=IDS(E)
 .S PATREF(E)=PATREF
 .S NHSNO(E)=$GET(^B("entry",E,"resource","identifier",2,"value"))
 .Q
 
 S ZID="",C=$O(^OUT("PAT",""),-1)+1
 F  S ZID=$O(IDS(ZID)) Q:ZID=""  D
 .S ID=IDS(ZID)
 .S ADD=ADD(ZID)
 .S DOB=DOBS(ZID)
 .S NAME=NAMES(ZID)
 .S GENDER=GENDERS(ZID)
 .S TELECOM=TELECOM(ZID)
 .W !,ID,",",ADD,",",DOB,",",NAME,",",GENDER,",",NHSNO(ZID),",",TELECOM
 .S ^OUT("PAT",C)=ID_","_ADD_","_DOB_","_NAME_","_GENDER_","_NHSNO(ZID)_","_TELECOM_","_PATREF(ZID)
 .S C=C+1
 .Q
 Q
 
GETASSDATE(E,ASSDATE) 
 S D=$GET(^B("entry",E,"resource","assertedDate"))
 S ASSDATE(E)=D
 QUIT
 
PARENT(E,PARENT) ;
 S CODE=$G(^B("entry",E,"resource","code","coding",1,"code"))
 S PARENTS(E)=CODE_"~"_$G(^B("entry",E,"resource","code","coding",1,"display"))
 Q
 
COMPONENT(E,COMP,COMPIDS) 
 N N,COMP
 S N=""
 F  S N=$ORDER(^B("entry",E,"resource","component",N)) Q:N=""  D
 .S CODE=$GET(^B("entry",E,"resource","component",N,"code","coding",1,"code"))
 .S DISP=$GET(^B("entry",E,"resource","component",N,"code","coding",1,"display"))
 .S VCODE=$get(^B("entry",E,"resource","component",N,"valueQuantity","code"))
 .S VVALUE=$get(^B("entry",E,"resource","component",N,"valueQuantity","value"))
 .S ZID=$get(^B("entry",E,"resource","component",N,"code","coding",1,"id"))
 .S COMPIDS(E,N)=ZID
 .S COMPS(E,N)=CODE_","_DISP_","_VVALUE_","_VCODE
 .QUIT
 Q
 
OBSDAT(E,OBSDAT) 
 S OBSDAT(E)=$GET(^B("entry",E,"resource","effectivePeriod","start"))
 QUIT
 
OBSPAT(E,OPATS) 
 S REF=$P(^B("entry",E,"resource","subject","reference"),"/",2)
 S OPATS(E)=$GET(^DDS("PAT",REF))
 QUIT
 
OBS N DDSID,IDS,OBSDATS,PARENTS,COMPS,OPATS,COMPIDS
 KILL DDSID
 S E="",C=$O(^OUT("OBS",""),-1)+1
 F  S E=$O(^B("entry",E)) Q:E=""  D
 .D GETDDSID(E,.IDS)
 .I $G(IDS(E))="" S IDS(E)="?"
 .d OBSDAT(E,.OBSDATS)
 .D PARENT(E,.PARENTS)
 .D COMPONENT(E,.COMPS,.COMPIDS)
 .D OBSPAT(E,.OPATS)
 .S NOR=OPATS(E)
 .S DATE=$P(OBSDATS(E),"T")
 .S CODE=$P($G(PARENTS(E)),"~",1)
 .S DISPLAY=$P($G(PARENTS(E)),"~",2)
 .S DDSID=IDS(E)
 .;i $d(^tdone(DDSID)) Q
 .s ^tdone(DDSID)=""
 .I '$DATA(COMPS(E)) S ^OUT("OBS",C)=DDSID_","_NOR_","_DATE_","_CODE_","_DISPLAY_","_$G(^B("entry",E,"resource","id"))
 .S ^OBS(DDSID,C)=NOR_","_DATE_","_CODE_","_DISPLAY
 .W !,DDSID
 .S C=C+1
 .; get the component codes
 .S N=""
 .F  S N=$O(COMPS(E,N)) Q:N=""  D
 ..s zid=$GET(COMPIDS(E,N))
 ..;I zid'="",$d(^tdone(zid)) quit
 ..s ^tdone(zid)=""
 ..S ^OUT("OBS",C)=$GET(COMPIDS(E,N))_","_NOR_","_DATE_","_COMPS(E,N)_","_$GET(IDS(E,"P"))
 ..;
 ..S C=C+1
 ..Q
 .;I IDS(E)=237366 BREAK
 .QUIT
 
 ;ZW IDS
 ;ZW OBSDAT
 ;ZW PARENTS
 ;ZW COMPS
 ;ZW OPATS
 
 QUIT
 
ALL ;
 N DDSID,CODES,IDS,ASSDATES,GUIDS
 KILL DDSID
 S E=""
 F  S E=$O(^B("entry",E)) Q:E=""  D
 .D GETDDSID(E,.IDS)
 .I $GET(IDS(E))="" S IDS(E)="?"
 .D GETASSDATE(E,.ASSDATES)
 .S PATREF=$P($GET(^B("entry",E,"resource","patient","reference")),"/",2)
 .S DDSID="?"
 .I $DATA(^DDS("PAT",PATREF)) S DDSID=^DDS("PAT",PATREF)
 .S DDSID(E)=DDSID
 .S CODE=^B("entry",E,"resource","code","coding",1,"code")
 .S TERM=^B("entry",E,"resource","code","coding",1,"display")
 .S CODES(E)=CODE_"~"_TERM
 .S GUIDS(E)=^B("entry",E,"resource","id")
 .QUIT
 
 S ZID="",C=$O(^OUT("ALL",""),-1)+1
 F  S ZID=$O(IDS(ZID)) Q:ZID=""  D
 .S ID=IDS(ZID)
 .S ASSDATE=ASSDATES(ZID)
 .S CODE=$P(CODES(ZID),"~"),TERM=$P(CODES(ZID),"~",2)
 .S DDSID=DDSID(ZID)
 .W !,ID,",",DDSID,",",ASSDATE,",",CODE,",",TERM
 .S ^OUT("ALL",C)=ID_","_DDSID_","_$P(ASSDATE,"T")_","_CODE_","_TERM_","_GUIDS(ZID)
 .S C=C+1
 .QUIT
 QUIT
  
GO ;
 n token,MEDS
 
 k ^token,^DDS,^OUT,^tdone
 
 s ^token=$$STT()
 
 D GET(^DSYSTEM("FHIR","FHIRENDPOINT")_"Patient?_count=100")
 K ^B M ^B=B
 D PAT
 F  S NEXT=$$NEXT() Q:NEXT=""  D GET(NEXT) K ^B M ^B=B D PAT
 
 ;ZWR ^B
 ;QUIT
 
 D GET(^DSYSTEM("FHIR","FHIRENDPOINT")_"Medication")
 K ^B M ^B=B
 D MEDS(.MEDS)
 F  S NEXT=$$NEXT() Q:NEXT=""  D GET(NEXT) K ^B M ^B=B D MEDS(.MEDS)
 
 ;ZW MEDS
 
 D GET(^DSYSTEM("FHIR","FHIRENDPOINT")_"MedicationStatement")
 K ^B M ^B=B
 D RX
 F  S NEXT=$$NEXT() Q:NEXT=""  D GET(NEXT) K ^B M ^B=B D RX
 zwr ^B
 
 ;S F="D:\TEMP\OUT.TXT"
 ;O F:"WNS"
 ;S A=""
 ;F  S A=$O(^OUT("RX",A)) Q:A=""  U F W ^(A),!
 ;CLOSE F
 
 D GET(^DSYSTEM("FHIR","FHIRENDPOINT")_"AllergyIntolerance")
 K ^B M ^B=B
 D ALL
 F  S NEXT=$$NEXT() Q:NEXT=""  D GET(NEXT) K ^B M ^B=B D ALL
 
 D GET(^DSYSTEM("FHIR","FHIRENDPOINT")_"Observation")
 K ^B M ^B=B
 D OBS
 F  S NEXT=$$NEXT() Q:NEXT=""  D GET(NEXT) K ^B M ^B=B D OBS
 
X S F="/tmp/dyn-OBS.TXT"
 ;O F:"WNS"
 O F:(newversion):0
 S A=""
 F  S A=$O(^OUT("OBS",A)) Q:A=""  U F W ^(A),!
 CLOSE F
 
P S F="/tmp/dyn-PATS.TXT"
 O F:(newversion):0
 S A=""
 F  S A=$O(^OUT("PAT",A)) Q:A=""  U F W ^(A),!
 CLOSE F
 
A S F="/tmp/dyn-ALL.TXT"
 O F:(newversion)
 S A=""
 F  S A=$O(^OUT("ALL",A)) Q:A=""  U F W ^(A),!
 CLOSE F
 
M S F="/tmp/dyn-MEDS.TXT"
 O F:(newversion)
 S A=""
 F  S A=$O(^OUT("MEDS",A)) Q:A=""  U F W ^(A),!
 C F
  
R S F="/tmp/dyn-RX.TXT"
 O F:(newversion)
 S A=""
 F  S A=$O(^OUT("RX",A)) Q:A=""  U F W ^(A),!
 C F
 Q
  
GET(endpoint) ;
 set cmd="curl -s -X GET -i -H ""Authorization: Bearer "_^token_""" """_endpoint_""" > /tmp/a"_$job_".txt"
 
 ;Set ST=$zf(-1,cmd)
 zsystem cmd
 Set JSON(1)=$$TEMP("/tmp/a"_$J_".txt")
 
 KILL B
 ; VALIDATE THE ACCESS TOKEN
 D DECODE^VPRJSON($NAME(JSON(1)),$NAME(B),$NAME(E))
 
 QUIT
 
READ(pFile) 
 New C,JSON,STR
 ;Do $system.Process.SetZEOF(1)
 Close pFile
 Open pFile:(readonly):0
 Set C=0,JSON=""
 For  Use pFile Read STR Q:$ZEOF  D
 .Set JSON=JSON_STR_$C(13,10)
 .Q
 Close pFile
 Q JSON
