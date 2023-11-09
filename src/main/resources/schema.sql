CREATE SCHEMA IF NOT EXISTS testdb;
SET SCHEMA testdb;
CREATE TABLE user (
  id int NOT NULL AUTO_INCREMENT, 
  first_name varchar(100) DEFAULT NULL,
  last_name varchar(100) DEFAULT NULL, 
  PRIMARY KEY (id) ,
  CONSTRAINT unique_name 
    UNIQUE (first_name,last_name)
);