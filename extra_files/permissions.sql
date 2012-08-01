declare
permId krim_perm_t.perm_id%TYPE;
begin
insert into KRIM_PERM_T (PERM_ID,OBJ_ID,VER_NBR,PERM_TMPL_ID,NMSPC_CD,NM,DESC_TXT,ACTV_IND)
values (krim_perm_id_s.nextval,sys_guid(),1,(select perm_tmpl_id from krim_perm_tmpl_t where nmspc_cd='KFS-SYS' and nm='Modify Accounting Lines'),'KFS-FP','Modify Accounting Lines','Allows users to modify the invoice number of Source accounting lines on a Disbursement Voucher document that is at the Travel Node of routing.','Y')
returning PERM_ID into permId;

insert into KRIM_PERM_ATTR_DATA_T (ATTR_DATA_ID,OBJ_ID,VER_NBR,PERM_ID,KIM_TYP_ID,KIM_ATTR_DEFN_ID,ATTR_VAL)
values (krim_attr_data_id_s.nextval,sys_guid(),1,permId,(select kim_typ_id from krim_typ_t where nmspc_cd = 'KR-SYS' and nm like '%Document Type, Routing Node%'),(select kim_attr_defn_id from krim_attr_defn_t where nm='documentTypeName' and nmspc_cd='KR-WKFLW'),'DV');

insert into KRIM_PERM_ATTR_DATA_T (ATTR_DATA_ID,OBJ_ID,VER_NBR,PERM_ID,KIM_TYP_ID,KIM_ATTR_DEFN_ID,ATTR_VAL)
values (krim_attr_data_id_s.nextval,sys_guid(),1,permId,(select kim_typ_id from krim_typ_t where nmspc_cd = 'KR-SYS' and nm like '%Document Type, Routing Node%'),(select kim_attr_defn_id from krim_attr_defn_t where nm='routeNodeName' and nmspc_cd='KR-WKFLW'),'Travel');

insert into KRIM_PERM_ATTR_DATA_T (ATTR_DATA_ID,OBJ_ID,VER_NBR,PERM_ID,KIM_TYP_ID,KIM_ATTR_DEFN_ID,ATTR_VAL)
values (krim_attr_data_id_s.nextval,sys_guid(),1,permId,(select kim_typ_id from krim_typ_t where nmspc_cd = 'KR-SYS' and nm like '%Document Type, Routing Node%'),(select kim_attr_defn_id from krim_attr_defn_t where nm='propertyName' and nmspc_cd='KR-NS'),'sourceAccountingLines.extension.invoiceNumber');

insert into KRIM_ROLE_PERM_T (ROLE_PERM_ID,OBJ_ID,VER_NBR,ROLE_ID,PERM_ID,ACTV_IND)
values (krim_role_perm_id_s.nextval,sys_guid(),1,(select role_id from krim_role_t where role_nm='Document Editor' and nmspc_cd='KR-NS'),permId,'Y');
end; 
