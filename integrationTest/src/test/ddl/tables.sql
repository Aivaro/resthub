--------------------------------------------------------
--  DDL for Table CUSTOMER
--------------------------------------------------------

CREATE TABLE "CUSTOMER" (
    "CUS_ID" NUMBER, 
    "CUS_LNAME" VARCHAR2(255 BYTE), 
    "CUS_CITY" VARCHAR2(128 BYTE), 
    "CUS_GENDER" CHAR(1 BYTE), 
    "CUS_FNAME" VARCHAR2(128 BYTE), 
    "CUS_COUNTRY" VARCHAR2(128 BYTE)
);

ALTER TABLE CUSTOMER MODIFY (CUS_ID NOT NULL);

ALTER TABLE CUSTOMER ADD CONSTRAINT CUSTOMER_PK PRIMARY KEY (CUS_ID) ENABLE;

--------------------------------------------------------
--  DDL for Table PRODUCT
--------------------------------------------------------

CREATE TABLE "PRODUCT" (
    "PRO_ID" NUMBER, 
    "PRO_BRAND" VARCHAR2(255 BYTE), 
    "PRO_NAME" VARCHAR2(255 BYTE), 
    "PRO_DESC" CLOB, 
    "PRO_IMAGE" BLOB
);

ALTER TABLE PRODUCT MODIFY (PRO_ID NOT NULL);

ALTER TABLE PRODUCT ADD CONSTRAINT PRODUCT_PK PRIMARY KEY (PRO_ID) ENABLE;

--------------------------------------------------------
--  DDL for Table SALES
--------------------------------------------------------

CREATE TABLE "SALES" (
    "SAL_PRO_ID" NUMBER, 
    "SAL_TIME" TIMESTAMP (6), 
    "SAL_CUS_ID" NUMBER, 
    "SAL_COST" NUMBER, 
    "SAL_UNITS" NUMBER, 
    "SAL_ID" NUMBER
);

ALTER TABLE SALES MODIFY (SAL_ID NOT NULL);
ALTER TABLE SALES MODIFY (SAL_PRO_ID NOT NULL);
ALTER TABLE SALES MODIFY (SAL_TIME NOT NULL);
ALTER TABLE SALES MODIFY (SAL_CUS_ID NOT NULL);
ALTER TABLE SALES MODIFY (SAL_COST NOT NULL);
ALTER TABLE SALES MODIFY (SAL_UNITS NOT NULL);

ALTER TABLE SALES ADD CONSTRAINT SALES_PK PRIMARY KEY (SAL_ID) ENABLE;
ALTER TABLE SALES ADD CONSTRAINT SAL_CUS_FK FOREIGN KEY (SAL_CUS_ID) REFERENCES CUSTOMER (CUS_ID) ENABLE;
ALTER TABLE SALES ADD CONSTRAINT SAL_PRO_FK FOREIGN KEY (SAL_PRO_ID) REFERENCES PRODUCT (PRO_ID) ENABLE;