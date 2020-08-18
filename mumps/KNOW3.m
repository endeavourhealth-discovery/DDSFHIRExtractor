KNOW3 ; ; 8/18/20 9:29am
DISPLAY(GUID,ZE) ;
 
 s ^token=$$STT^KNOW2()
 
 ;
 ;
 ;
 ;
 
 ; Patient resource
 ;
 ;
 D GET(ZE_"Patient/"_GUID)
 K ^B M ^B=B
 W !,"family: ",^B("name",1,"family")
 W !,"given: ",^B("name",1,"given",1)
 W !,"dob: ",^B("birthDate")
 W !,"gender: ",^B("gender")
 S DDSID=^B("identifier",1,"value")
 W !,"ddsid: ",DDSID
 
 ;Observation
 ;BREAK
 
 K ^TEMP($J,"OBS"),^tdone($j)
 S ZC=1
 ;
 ;
 D GET(ZE_"Observation?subject="_GUID)
 K ^B M ^B=B
 D OBS
 F  S NEXT=$$NEXT^KNOW2() Q:NEXT=""  D GET(NEXT) K ^B M ^B=B D OBS
 
 S A="" F  S A=$O(^TEMP($J,"OBS",A)) Q:A=""  D
 .;W !,^(A)
 .S REC=^(A)
 .W !,$P(REC,",",1)," ",$P(REC,",",2)," ",$P(REC,",",4)," ",$P(REC,",",5)
 .QUIT
 
 ;
 ;
 D GET(ZE_"MedicationStatement?subject="_GUID)
 K ^B M ^B=B
 D RX
 F  S NEXT=$$NEXT^KNOW2() Q:NEXT=""  D GET(NEXT) K ^B M ^B=B D RX
 ;ZW ^B
 
 ;
 ;
 D GET(ZE_"AllergyIntolerance?patient="_GUID)
 K ^B M ^B=B
 D ALL
 F  S NEXT=$$NEXT^KNOW2() Q:NEXT=""  D GET(NEXT) K ^B M ^B=B D ALL
 
 QUIT
 
ALL ;
 N DDSID,CODES,IDS,ASSDATES,GUIDS
 KILL DDSID
 S E=""
 F  S E=$O(^B("entry",E)) Q:E=""  D
 .D GETDDSID^KNOW2(E,.IDS)
 .I $GET(IDS(E))="" S IDS(E)="?"
 .D GETASSDATE^KNOW2(E,.ASSDATES)
 .S CODE=^B("entry",E,"resource","code","coding",1,"code")
 .S TERM=^B("entry",E,"resource","code","coding",1,"display")
 .S CODES(E)=CODE_"~"_TERM
 .S GUIDS(E)=^B("entry",E,"resource","id")
 .QUIT
 
 S ZID=""
 F  S ZID=$O(IDS(ZID)) Q:ZID=""  D
 .S ID=IDS(ZID)
 .S ASSDATE=ASSDATES(ZID)
 .S CODE=$P(CODES(ZID),"~"),TERM=$P(CODES(ZID),"~",2)
 .W !,ID,",",",",ASSDATE,",",CODE,",",TERM
 .;S ^OUT("ALL",C)=ID_","_DDSID_","_$P(ASSDATE,"T")_","_CODE_","_TERM_","_GUIDS(ZID)
 .;S C=C+1
 .QUIT
 QUIT
  
RX ;
 N IDS,DDSE,DATES,REFS,PATS
 S E=""
 F  S E=$O(^B("entry",E)) Q:E=""  D
 .D GETDDSID^KNOW2(E,.IDS)
 .I '$D(IDS(E)) S IDS(E)="?"
 .;D OBSPAT(E,.PATS)
 .D GETDSEQTY^KNOW2(E,.DDSE)
 .D DATES^KNOW2(E,.DATES)
 .D REF^KNOW2(E,.REFS)
 .;^B("entry",1,"resource","medicationReference","display")
 .S MEDS(E)=$GET(^B("entry",1,"resource","medicationReference","display"))
 .;S GUIDS(E)=^B("entry",E,"resource","id")
 .QUIT
 
 S ID=""
 F  S ID=$O(IDS(ID)) Q:ID=""  D
 .S ZID=IDS(ID)
 .S DDSE=$GET(DDSE(ID))
 .;BREAK
 .S DATE=$P(DATES(ID),"T",1)
 .S REF=$P(REFS(ID),"/",2)
 .S NOR=$GET(PATS(ID))
 .W !,ZID,",",DDSE,",",DATE,",",$GET(MEDS(ID),"?")
 .;S ^OUT("RX",C)=ZID_","_NOR_","_DDSE_","_DATE_","_$GET(MEDS(REF),"?")_","_REF_","_GUIDS(ID)
 .;S C=C+1
 .QUIT
 
 QUIT
 
OBS N DDSID,IDS,OBSDATS,PARENTS,COMPS,OPATS,COMPIDS
 KILL DDSID
 S E=""
 ;BREAK
 F  S E=$O(^B("entry",E)) Q:E=""  D
 .D GETDDSID^KNOW2(E,.IDS)
 .I $G(IDS(E))="" S IDS(E)="?"
 .d OBSDAT^KNOW2(E,.OBSDATS)
 .D PARENT^KNOW2(E,.PARENTS)
 .D COMPONENT^KNOW2(E,.COMPS,.COMPIDS)
 .D OBSPAT^KNOW2(E,.OPATS)
 .S DATE=$P(OBSDATS(E),"T")
 .S CODE=$P($G(PARENTS(E)),"~",1)
 .S DISPLAY=$P($G(PARENTS(E)),"~",2)
 .S DDSID=IDS(E)
 .;BREAK:DDSID=131622
 .i $d(^tdone($j,DDSID)) Q
 .s ^tdone($j,DDSID)=""
 .I '$D(COMPS) S ^TEMP($J,"OBS",ZC)=DDSID_","_DATE_","_CODE_","_DISPLAY_","_$G(^B("entry",E,"resource","id")),ZC=ZC+1
 .; get the component codes
 .S N=""
 .F  S N=$O(COMPS(E,N)) Q:N=""  D
 ..s zid=$GET(COMPIDS(E,N))
 ..s:zid="" zid="?"
 ..s ^tdone($j,zid)=""
 ..S ^TEMP($J,"OBS",ZC)=$GET(COMPIDS(E,N))_","_DATE_","_COMPS(E,N)_","_$GET(IDS(E,"P")),ZC=ZC+1
 ..Q
 .QUIT
 
 QUIT
 
GET(endpoint) 
 ;W !,endpoint R *Y
 set cmd="curl -s -X GET -i -H ""Authorization: Bearer "_^token_""" """_endpoint_""" > /tmp/a"_$job_".txt"
 
 ;Set ST=$zf(-1,cmd)
 zsystem cmd
 Set JSON(1)=$$TEMP^KNOW2("/tmp/a"_$J_".txt")
 
 KILL B
 ; VALIDATE THE ACCESS TOKEN
 D DECODE^VPRJSON($NAME(JSON(1)),$NAME(B),$NAME(E))
 
 QUIT
