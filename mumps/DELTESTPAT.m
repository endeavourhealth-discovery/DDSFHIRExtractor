DELTESTPAT ; ; 8/18/20 9:27am
 S ^token=$$STT^KNOW2()
 
 S ZE=^DSYSTEM("FHIR","FHIRENDPOINT")
 
 ;S GUID="d5d81988-fdb0-4f83-95d6-10461a63f8a0"
 W !,"PATIENT GUID: "
 R GUID
 
 ; DISPLAY PATIENT BEFORE DELETING
 D DISPLAY^KNOW3(GUID,ZE)
 
STT W !!,"DELETE PATIENT FROM VCFR? "
 R YN#1
 I YN="N" QUIT
 I YN'="Y" W !,"?" G STT
 
 D DEVPAT(GUID)
 D DELETE
 QUIT
 
DELETE ;
 S A=""
 F  S A=$O(^T($J,A)) Q:A=""  D
 .S R=^T($J,A)
 .D DELINDEV(R,A)
 .Q
 S A=""
 F  S A=$O(^T($J,A)) Q:A=""  W !,A," ",^(A)
 QUIT
 
DELINDEV(r,id) 
 S ZTOKEN=$$STT^KNOW2()
 
 ;
 Set CURL="curl -X DELETE -i -H ""Authorization: Bearer "_ZTOKEN_""" "_ZE_r_"/"_id_" > /tmp/AZURE-"_$j_".TXT"
  
 ;Set ST=$zf(-1,CURL)
 zsystem CURL
 Set JSON=$$READ^KNOW2("/tmp/AZURE-"_$J_".TXT")
 
 W JSON
 
 QUIT
 
DEVPAT(PATGUID) 
 K ^T
 F R="MedicationStatement","AllergyIntolerance","Observation" D DEV(R,PATGUID)
 
 S A="",T=0
 F  S A=$O(^T($J,A)) Q:A=""  S T=T+1
 W !,"TOT=",T
 QUIT
 
DEV(R,PATGUID) 
 ;
 ;
 S URI=ZE_R_"?subject="_PATGUID
 I R="AllergyIntolerance" S URI=ZE_R_"?patient="_PATGUID
 D GET^KNOW3(URI),IDS(R)
 S next=""
 f i=1:1 do  q:next'="next"
 .s next=$GET(B("link",1,"relation"))
 .w !,next
 .I next="next" S url=$GET(B("link",1,"url")) D GET^KNOW3(url),IDS(R)
 .quit
 QUIT
 
IDS(R) ;
 s z=""
 f  s z=$o(B("entry",z)) q:z=""  s zid=$g(B("entry",z,"resource","id")) w !,R," * ",zid S ^T($J,zid)=R
 QUIT
