package edu.wisc.my.keyvalue.repository;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import edu.wisc.my.keyvalue.model.KeyValue;

public interface KeyValueRepository extends Repository<KeyValue, Long>{
    
    public KeyValue findByKey(@Param("key") String Key);
    
    public KeyValue save(KeyValue keyValue);
    
    public KeyValue delete(KeyValue Key);
    
}