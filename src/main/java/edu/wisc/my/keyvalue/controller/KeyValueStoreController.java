package edu.wisc.my.keyvalue.controller;

import edu.wisc.my.keyvalue.service.IKeyValueService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Controller
public class KeyValueStoreController{

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private IKeyValueService keyValueService;

    private String usernameAttribute;

    private static final String ACCESS_ERROR="No username set in header, entity manager set properly?";

    @Autowired
    private Environment env;

    @Autowired
    public void setKeyValueService(IKeyValueService keyValueService){
        this.keyValueService = keyValueService;
    }

    /**
     * Status page
     * @param response the thing to write to
     */
    @RequestMapping("/")
    public @ResponseBody void index(HttpServletResponse response){
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

    @RequestMapping(value="/{scope}/{key}", method=RequestMethod.PUT)
    public @ResponseBody void putScopedKeyValue(HttpServletRequest request,
                                                HttpServletResponse response,
                                                @PathVariable String scope,
                                                @PathVariable String key,
                                                @RequestBody String valueJson) throws IOException {
        putInternal(request, response, scope, key, valueJson);
    }

    @RequestMapping(value="/{key}", method=RequestMethod.PUT)
    public @ResponseBody void setKeyValue(HttpServletRequest request, HttpServletResponse response, @PathVariable String key, @RequestBody String valueJson){
        putInternal(request, response, null, key, valueJson);
    }

    private void putInternal(HttpServletRequest request, HttpServletResponse response, String scope, String key,  String value) {
        //validation of request
        if(!isJSONValid(value)) {
            logger.error("Invalid request, json not valid");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        //save
        try {
            keyValueService.setValue(request, scope, key, value);
            //write response
            writeResponse(response, value);
        } catch(SecurityException se) {
            logger.error("Access denied", se);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @RequestMapping(value="/{scope}/{key}", method=RequestMethod.GET)
    public @ResponseBody void getScopedKeyValue(HttpServletRequest request,
                                                HttpServletResponse response,
                                                @PathVariable String scope,
                                                @PathVariable String key) throws IOException {
      getInternal(request, response, scope, key);
    }

    @RequestMapping(value="/{key}", method=RequestMethod.GET)
    public @ResponseBody void getKeyValue(HttpServletRequest request, HttpServletResponse response, @PathVariable String key){
        getInternal(request, response, null, key);
    }

    private void getInternal(HttpServletRequest request,
                             HttpServletResponse response,
                             String scope,
                             String key) {
        try {
            String value = keyValueService.getValue(request, scope, key);
            if (isJSONValid(value)) {
                logger.trace("Got something for scope : {}, key : {}, value : {}", scope, key, value);
                //valid json, cool, write it
                writeResponse(response, value);
            } else {
                logger.trace("Got nothing for scope : {}, key : {}", scope, key);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SecurityException se) {
            logger.error("Access denied", se);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private void writeResponse(HttpServletResponse response, String json) {
        try {
            response.getWriter().write(json);
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e) {
            logger.error("Issues happened while trying to write json", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value="/{key}", method=RequestMethod.DELETE)
    public @ResponseBody void delete(HttpServletRequest request, HttpServletResponse response, @PathVariable String key){
        internalDelete(request, response, null, key);
    }

    @RequestMapping(value="/{scope}/{key}", method=RequestMethod.DELETE)
    public @ResponseBody void deleteScoped(HttpServletRequest request,
                                           HttpServletResponse response,
                                           @PathVariable String scope,
                                           @PathVariable String key){
        internalDelete(request, response, scope, key);
    }

    private void internalDelete(HttpServletRequest request, HttpServletResponse response, String scope, String key) {
        try {
            keyValueService.delete(request, scope, key);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch(SecurityException se) {
            logger.error("Access denied", se);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
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
