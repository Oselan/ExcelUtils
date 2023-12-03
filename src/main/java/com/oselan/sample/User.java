package com.oselan.sample;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity 
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data 
@Accessors(chain = true) 
public class User  {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) 
	private Long id;
	 
	private String firstName; 
	private String lastName;
}
