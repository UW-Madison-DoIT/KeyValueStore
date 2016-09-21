package edu.wisc.my.keyvalue.service;

import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public interface IKeyValueService {

    public enum METHOD {
        PUT,
        GET,
        DELETE
    }

    /**
     * Returns the value for a given key and prefix
     * @param request
     * @param scope
     * @param key
     * @return may return empty string if nothing exists
     */
    public String getValue(HttpServletRequest request, String scope, String key);
    
    /**
     * 

     * @param key
     * @param value
     * @return
     */
    public void setValue(HttpServletRequest request, String scope, String key, String value);

    /**
     * Deletes a given key with prefix
     * @param request
     * @param scope
     * @param key
     */
    public void delete(HttpServletRequest request, String scope, String key);

    /**
     *
     * @param scope
     * @return
     */
    boolean isByUser(String scope);

    /**
     *
     * @param scope
     * @param request
     * @return
     */
    public boolean isAuthorized(String scope, HttpServletRequest request, METHOD method);
    
}