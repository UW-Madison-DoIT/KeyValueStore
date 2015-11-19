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
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
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
    private Environment env;

    @Autowired
    public void setKeyValueService(IKeyValueService keyValueService){
        this.keyValueService = keyValueService;
    }

    @Value("${usernameAttribute}")
    public void setUsernameAttr(String attr) {
      usernameAttribute = attr;
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
    public @ResponseBody void putScopedKeyValue(HttpServletRequest request, HttpServletResponse response, @PathVariable String scope, @PathVariable String key, @RequestBody String valueJson) throws IOException {
      String byUser = env.getRequiredProperty("scope." + scope + ".byUser");
      boolean scopeUserBased = Boolean.parseBoolean(byUser);
      logger.trace("scope " + scope + " byUser? " +byUser + " : " + scopeUserBased);

      String username = request.getHeader(usernameAttribute);

      String prefix = scope;
      boolean authorized = false;

      if(!scopeUserBased) {
        //we are editing a global scoped thing, so we must be an admin
        String adminGroup = env.getRequiredProperty("scope." + scope + ".admin.group");
        String groupHeader = env.getRequiredProperty("groupHeaderAttribute");

        String header = request.getHeader(groupHeader);

        authorized = header !=null && header.contains(adminGroup);
      } else {
        //a user is PUTing on a scoped key that is per attribute
        String prefixAttr = env.getRequiredProperty("scope." + scope + ".prefixAttribute");
        String filterHeaderValue = request.getHeader(prefixAttr);
        authorized = filterHeaderValue != null;
        prefix += ":" + filterHeaderValue;
      }

      if(authorized) {
        //security check success

        if(!isJSONValid(valueJson)) {
          logger.error("Invalid request, json not valid");
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          return;
        }

        logger.trace("Setting prefix: " + prefix + " key: " + key);
        keyValueService.setValue(prefix, key, valueJson);

        // write response
        try {
          response.getWriter().write(valueJson);
          response.setContentType("application/json");
          response.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e) {
          logger.error("Issues happened while trying to write json", e);
          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        logger.info("user " + username + " wrote to scope " + scope + " and key " + key + " with value " + valueJson);
      } else {
        logger.error("User " + username + " attempted to PUT to " + scope + " but doesn't have access, shame!");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      }

    }

    @RequestMapping(value="/{scope}/{key}", method=RequestMethod.GET)
    public @ResponseBody void getScopedKeyValue(HttpServletRequest request, HttpServletResponse response, @PathVariable String scope, @PathVariable String key) throws IOException {
      boolean scopeUserBased = Boolean.parseBoolean(env.getRequiredProperty("scope." + scope + ".byUser"));

      if(scopeUserBased) {
        String property = env.getProperty("scope." + scope + ".prefixAttribute");
        String propHeaderValue = request.getHeader(property);
        if(propHeaderValue != null) {
          scope += ":" + propHeaderValue;
        } else {
          //there was a property for this scope, but the proper header was not set
          logger.error(ACCESS_ERROR);
          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
          return;
        }
      } else {
        logger.trace("global hit");
      }
      logger.trace("Searching for prefix " + scope + " key: " + key);
      String value = keyValueService.getValue(scope, key);
      try {
          if(isJSONValid(value)) {
            logger.trace("Got something for scope : " + scope + ", key : " + key + ", value : " + value);
            //valid json, cool, write it
            response.getWriter().write(value);
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
          }
          else {
            logger.trace("Got nothing for scope : " + scope + ", key : " + key);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
          }

      } catch (IOException e) {
          logger.error("Issues happened while trying to write json", e);
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
