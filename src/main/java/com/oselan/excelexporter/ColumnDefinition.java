package com.oselan.excelexporter;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
/***
 * A simple class to define columns in Excel other properties may be added in
 * the future like column format
 * 
 * @author Ahmad Hamid
 *
 */
@Getter
@Setter
public class ColumnDefinition {
  private Integer index;

  private String header;

  private String property;

  private String format;
  
  
  /**
   * Creates a list builder implementation 
   * @return
   */
  public static ListBuilder  listBuilder() {
    return new ListBuilder ();
  }
   
  public final static class ListBuilder 
  {   
      private List<ColumnDefinition> columnsDef = new ArrayList<ColumnDefinition>(); 
      public ListBuilder withColumn(String header,String property)
      {
        columnsDef.add(ColumnDefinition.builder().index(columnsDef.size()).header(header).property(property).build());
        return this; 
      }
      public ListBuilder withColumn(String header,String property, Integer index)
      {
        columnsDef.add(ColumnDefinition.builder().index(index).header(header).property(property).build());
        return this; 
      }
      public List<ColumnDefinition> build()
      {
        return columnsDef;
      }
  }

   

}