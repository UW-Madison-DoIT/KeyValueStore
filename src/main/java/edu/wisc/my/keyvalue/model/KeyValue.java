package edu.wisc.my.keyvalue.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;


@Entity
public class KeyValue{
  
    public KeyValue() {}
    
    public KeyValue(String key) {
      this.key = key;
    }
    
    @Id
    private String key;
    
    @Lob
    @Column(length=6000)
    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
}