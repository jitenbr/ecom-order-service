package com.secor.restroservice;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "restros")
@Getter @Setter
public class Restro {

    @Id
    private String restroid;
    private String name;
    private String owner;

}
