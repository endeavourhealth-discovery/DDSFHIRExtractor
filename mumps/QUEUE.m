QUEUE ; ; 11/11/19 6:50pm
 K ^T,^FRED
 F R="Patient","MedicationStatement","Medication","Organization","AllergyIntolerance","Observation" D PURGE(R)
 D Q
 S q=""
 f  s q=$o(^Q(q)) q:q=""  W !,"jobbing ",q JOB DELQ(q)	
 Q
 
DELQ(q) ;
 ;W !!,"DELETING!!"
 ;R *Y
 S ^FRED(q,"start")=""
 S ZTOKEN=$$STT^CLRREPO()
 S zid=""
 f  s zid=$o(^Q(q,zid)) q:zid=""  DO
 .S R=^(zid)
 .S REC=$$DEL(ZTOKEN,R,zid)
 .S ^Q(q,zid,"response")=$p(JSON,$c(13),1)
 .S ^FRED(q,"last")=zid
 .Q
 s ^FRED(q,"end")=""
 QUIT
 
MONITOR ;
 F I=1:1:$O(^Q(""),-1) DO
 .S zid=""
 .s (ok,nok,stp)=0
 .f  s zid=$O(^Q(I,zid)) q:zid=""  d
 ..i $get(^Q(I,zid,"response"))["204" S ok=ok+1 q
 ..i $get(^Q(I,zid,"response"))["429" s nok=nok+1 q
 ..i $get(^Q(I,zid,"response"))="" s stp=stp+1
 ..quit
 .s tot=ok+nok
 .w !,"queue: ",I," *not* processed=",stp," ok=",ok," nok=",nok," tot=",tot," %fail= ",(nok/tot)*100
 .QUIT
 Q
 
DEL(token,r,id) 
 Set CURL="curl -X DELETE -i -H ""Authorization: Bearer "_token_""" "_^DSYSTEM("FHIR","FHIRENDPOINT")_r_"/"_id_" > /tmp/AZURE-"_$j_".TXT"
 zsystem CURL
 Set JSON=$$READ^CLRREPO("/tmp/AZURE-"_$J_".TXT")
 quit JSON
 
Q ;
 K ^Q
 set zid="",c=1,q=0
 f  s zid=$o(^T($j,zid)) q:zid=""  d
 .i c#500=0 s q=q+1
 .s ^Q(q,zid)=^(zid)
 .s c=c+1
 .q
 QUIT
 
IDS(R) ;
 s z=""
 f  s z=$o(B("entry",z)) q:z=""  s zid=$g(B("entry",z,"resource","id")) w !,zid S ^T($J,zid)=R
 QUIT
 
PURGE(R) ;
 S ^token=$$STT^CLRREPO()
 S ZTOKEN=^token
 s fhirendpoint=^DSYSTEM("FHIR","FHIRENDPOINT")
 D GETRES^CLRREPO(fhirendpoint_R)
 D IDS(R)
 S next=""
 f i=1:1 do  q:next'="next"
 .s next=$GET(B("link",1,"relation"))
 .I next'="" S url=$GET(B("link",1,"url")) D GETRES^CLRREPO(url) D IDS(R)
 .Q
 Q
