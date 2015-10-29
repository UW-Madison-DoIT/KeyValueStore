package edu.wisc.my.keyvalue.service;

import org.springframework.stereotype.Service;

@Service
public interface IKeyValueService{

    /**
     * Returns the value for a given key and username
     * @param username
     * @param key
     * @return may return empty string if nothing exists
     */
    public String getValue(String username, String key);
    
    /**
     * 
     * @param username
     * @param key
     * @param value
     * @return
     */
    public String setValue(String username, String key, String value);
    
    public void delete(String username, String key);
    
}