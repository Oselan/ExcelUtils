package com.oselan.commons.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.context.annotation.Configuration;

/***
* Maps names to database snake case naming and quotes identifiers
* @author Ahmad Hamid
*
*/
@Configuration
//@Slf4j 
public class SnakeCaseNamingStrategy implements PhysicalNamingStrategy{


 @Override
 public Identifier toPhysicalCatalogName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
     return convertToSnakeCase(identifier);
 }

 @Override
 public Identifier toPhysicalColumnName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
     return convertToSnakeCase(identifier);
 }

 @Override
 public Identifier toPhysicalSchemaName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
     return convertToSnakeCase(identifier);
 }

 @Override
 public Identifier toPhysicalSequenceName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
     return convertToSnakeCase(identifier);
 }

 @Override
 public Identifier toPhysicalTableName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
     return convertToSnakeCase(identifier);
 }
 
   private Identifier convertToSnakeCase(final Identifier identifier) {
     if (identifier==null) return identifier;
     final String regex = "([a-z])([A-Z])";
     final String replacement = "$1_$2";
     final String newName = identifier.getText()
       .replaceAll(regex, replacement)
       .toLowerCase();
     
     Identifier   id =  Identifier.toIdentifier(newName,  identifier.isQuoted());
     //log.info("Translated identifier {}",id);
     return id;
 }
}
