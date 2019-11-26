CLRREPO ; ; 10/22/19 11:58am
 ;
TEMP(file) 
       new z,data,ret,i
       ;break
       set data=""
       close file
       ;o file:("R"):0
       o file:(readonly):0
       ;i '$t break
       for i=1:1 use file read z q:$zeof  s data(i)=z
       close file
 
       zsystem "rm "_file
 
       S ret=$get(data(i-1))
       q ret
 
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

CONFIG
	W !,"CLIENT ID: "
	R CLIENTID
	W !,"SECRET: "
	R SECRET
	W !,"SCOPE: "
	R SCOPE
	W !,"ENDPOINT: "
	R ENDPOINT
	W !,"FHIR ENDPOINT: "
	R FHIRENDPOINT
	W !,"CONTINUE?"
	R X
	
	S ^DSYSTEM("FHIR","CLIENTID")=CLIENTID
	S ^DSYSTEM("FHIR","SECRET")=SECRET
	S ^DSYSTEM("FHIR","SCOPE")=SCOPE
	S ^DSYSTEM("FHIR","ENDPOINT")=ENDPOINT
	S ^DSYSTEM("FHIR","FHIRENDPOINT")=FHIRENDPOINT
	
	QUIT
 
STT() 
 n endpoint
 
 set clientid=^DSYSTEM("FHIR","CLIENTID")
 
 set secret=^DSYSTEM("FHIR","SECRET")
 
 set scope=^DSYSTEM("FHIR","SCOPE")
 
 set endpoint=^DSYSTEM("FHIR","ENDPOINT")
 
 set cmd="curl -s -X POST -i -H ""Content-Type: application/x-www-form-urlencoded"""
 set cmd=cmd_" -d ""client_id="_clientid_""" -d ""client_secret="_secret_""" -d ""scope="_scope_""" -d ""grant_type=client_credentials"" "
 set cmd=cmd_endpoint_" > /tmp/b"_$job_".txt"
 
 zsystem cmd
 Set JSON(1)=$$TEMP("/tmp/b"_$J_".txt")
 
 KILL B
 ; VALIDATE THE ACCESS TOKEN
 D DECODE^VPRJSON($NAME(JSON(1)),$NAME(B),$NAME(E))
 
 QUIT B("access_token")
 
WRITE ;
 w $$STT()
 s f="/tmp/token.txt"
 o f:(write):0
 u f W B("access_token")
 c f
 QUIT
 
DEL(token,r,id) ;
 
 
 Set CURL="curl -X DELETE -i -H ""Authorization: Bearer "_token_""" "_^DSYSTEM("FHIR","FHIRENDPOINT")_r_"/"_id_" > /tmp/AZURE-"_$j_".TXT"
  
 ;Set ST=$zf(-1,CURL)
 
 zsystem CURL
 
 Set JSON=$$READ("/tmp/AZURE-"_$J_".TXT")
 W JSON
 Q
 
CLEARDOWN ;
 k ^T($J)
 FOR R="Patient","MedicationStatement","Medication","Organization","AllergyIntolerance","Observation" D PURGE(R)
 ;d PURGE("Organization")
 QUIT
 
IDS(R) s z=""
 f  s z=$o(B("entry",z)) q:z=""  s zid=$g(B("entry",z,"resource","id")) w !,zid s t=t+1 S ^T($J,zid)=R
 QUIT
 
PURGE(R) ;
 k ^T($J)
 s ^token=$$STT()
 s ZTOKEN=^token
 s fhirendpoint=^DSYSTEM("FHIR","FHIRENDPOINT")
 
 D GETRES(fhirendpoint_R)
 
 s t=0
 D IDS(R)
 
 S next=""
 f i=1:1 do  q:next'="next"
 .s next=$GET(B("link",1,"relation"))
 .I next'="" S url=$GET(B("link",1,"url")) D GETRES(url) D IDS(R)
 .q
 
D k ^T($j,"10bd4cf14918ab408176cfc819ec247d") s zid=""
 f  s zid=$o(^T($J,zid)) q:zid=""  D DEL(ZTOKEN,R,zid)
 
 QUIT
 
GETRES(endpoint) ;
 set cmd="curl -s -X GET -i -H ""Authorization: Bearer "_^token_""" "_endpoint_" > /tmp/a"_$job_".txt"
 
 zsystem cmd
 Set JSON(1)=$$TEMP("/tmp/a"_$J_".txt")
 
 KILL B
 ; VALIDATE THE ACCESS TOKEN
 D DECODE^VPRJSON($NAME(JSON(1)),$NAME(B),$NAME(E))
 
 QUIT
