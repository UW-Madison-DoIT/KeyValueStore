package edu.wisc.my.keyvalue.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.wisc.my.keyvalue.service.IKeyValueService;

@Controller
public class KeyValueStoreController{
    
    private IKeyValueService keyValueService;
    
    
    @Autowired
    public void setKeyValueService(IKeyValueService keyValueService){
        this.keyValueService = keyValueService;
    }
    
    @RequestMapping("/")
    public @ResponseBody void index(HttpServletRequest request, HttpServletResponse response){
        try {
            response.getWriter().write("Hello World");
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    
    @RequestMapping(value="/getValue", method=RequestMethod.GET)
    public @ResponseBody void getKeyValue(HttpServletRequest request, HttpServletResponse response){
        String username = "vertein";
        String key="notification";
        String value = keyValueService.getValue(username, key);
        try {
            response.getWriter().write("{value:"+value+"}");
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @RequestMapping(value="/setValue", method=RequestMethod.GET)
    public @ResponseBody void setKeyValue(HttpServletRequest request, HttpServletResponse response){
        String username="vertein";
        String key = "notification";
        String value = "awesome";
        String returnValue = keyValueService.setValue(username, key, value);
        try {
            response.getWriter().write("{retunValue:"+returnValue+"}");
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}