package edu.wisc.my.keyvalue.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.wisc.my.keyvalue.service.IKeyValueService;


@Controller
public class KeyValueStoreController{
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    private IKeyValueService keyValueService;
    
    private String usernameAttribute;
    
    private static final String ACCESS_ERROR="No username set in header, entity manager set properly?";
    
    @Autowired
    public void setKeyValueService(IKeyValueService keyValueService){
        this.keyValueService = keyValueService;
    }
    
    @Value("${usernameAttribute}")
    public void setUsernameAttr(String attr) {
      usernameAttribute = attr;
    }
    
    @RequestMapping("/")
    public @ResponseBody void index(HttpServletRequest request, HttpServletResponse response){
        try {
          JSONObject responseObj = new JSONObject();
          responseObj.put("status", "up");
          response.getWriter().write(responseObj.toString());
          response.setContentType("application/json");
          response.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e) {
            logger.error("Issues happened while trying to write Status", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    
    @RequestMapping(value="/{key}", method=RequestMethod.GET)
    public @ResponseBody void getKeyValue(HttpServletRequest request, HttpServletResponse response, @PathVariable String key){
        String username = request.getHeader(usernameAttribute);
        if(username == null) {
          logger.error(ACCESS_ERROR);
          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
          return;
        }
        String value = keyValueService.getValue(username, key);
        try {
            if(isJSONValid(value)) {
              //valid json, cool, write it
              response.getWriter().write(value);
            }
            else {
              //if its not valid JSON (backwards compatible, wrap in a value object
              JSONObject responseObj = new JSONObject();
              responseObj.put("value", value);
              response.getWriter().write(responseObj.toString());
            }
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e) {
            logger.error("Issues happened while trying to write json", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    @RequestMapping(value="/{key}", method=RequestMethod.PUT)
    public @ResponseBody void setKeyValue(HttpServletRequest request, HttpServletResponse response, @PathVariable String key, @RequestBody String valueJson){
        //security check
        String username = request.getHeader(usernameAttribute);
        if(username == null) {
          logger.error(ACCESS_ERROR);
          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
          return;
        }
        
        //validation of request
        if(!isJSONValid(valueJson)) {
          logger.error("Invalid request, json not valid");
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          return;
        }
        
        //save
        keyValueService.setValue(username, key, valueJson);

        //write response
        try {
          response.getWriter().write(valueJson);
          response.setContentType("application/json");
          response.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e) {
          logger.error("Issues happened while trying to write json", e);
          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    @RequestMapping(value="/{key}", method=RequestMethod.DELETE)
    public @ResponseBody void delete(HttpServletRequest request, HttpServletResponse response, @PathVariable String key){
        String username = request.getHeader(usernameAttribute);
        keyValueService.delete(username, key);
        response.setStatus(HttpServletResponse.SC_OK);
    }
    
    private boolean isJSONValid(String test) {
      try {
          new JSONObject(test);
      } catch (JSONException ex) {
          try {
              new JSONArray(test);
          } catch (JSONException ex1) {
              return false;
          }
      }
      return true;
  }
    
}
