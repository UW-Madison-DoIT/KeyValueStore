package edu.wisc.my.keyvalue.service;

import org.springframework.stereotype.Service;

@Service
public interface IKeyValueService{

    /**
     * Returns the value for a given key and prefix
     * @param prefix
     * @param key
     * @return may return empty string if nothing exists
     */
    public String getValue(String prefix, String key);
    
    /**
     * 
     * @param username
     * @param key
     * @param value
     * @return
     */
    public String setValue(String prefix, String key, String value);
    
    public void delete(String prefix, String key);
    
}