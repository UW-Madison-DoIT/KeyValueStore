package edu.wisc.my.keyvalue.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import edu.wisc.my.keyvalue.model.KeyValue;
import edu.wisc.my.keyvalue.repository.KeyValueRepository;

import javax.servlet.http.HttpServletRequest;

@Service
public class KeyValueServiceImpl implements IKeyValueService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private KeyValueRepository keyValueRepository;
    private Environment env;
    private String usernameAttribute;
    private String additionalAttributes;
    private String[] allAttributes;

    @Value("${usernameAttribute}")
    public void setUsernameAttr(String attr) {
        usernameAttribute = attr;
    }

    @Value("${additionalAttributes}")
    public void setAdditionalAttributes(String attr) {
        //Additional attributes are stored in a comma delimited string.
        //If additional attributes are present, this method constructs an array with the usernameAttr as the first element,
        // and additional elements following in order. 

        //If no additional elements are present, this method will constuct a one-element array 
        //consisting of the usernameAttr.
        if(StringUtils.isNotBlank(attr)){
            additionalAttributes = attr;
            String[] tempArray = additionalAttributes.split(",");
            allAttributes = new String[tempArray.length +1];
            allAttributes[0] = usernameAttribute;
            for(int x=1;x<tempArray.length+1;x++){
                allAttributes[x] = tempArray[x-1].trim();
            }
        }else{
            allAttributes = new String[1];
            allAttributes[0] = usernameAttribute;
        }
    }

    private String[] getAllAttributes(){
        if(this.allAttributes==null){
            this.allAttributes=new String[1];
            this.allAttributes[0]=usernameAttribute;
        }

        return this.allAttributes;
    }

    private String getAttribute(HttpServletRequest request){
        for(String attribute:getAllAttributes()){
            if(StringUtils.isNotBlank(request.getHeader(attribute))){
                return attribute;
            }
        }

        return null;
    }

    @Autowired
    public void setEnv(Environment env) { this.env = env; }

    @Autowired
    public void setKeyValueRepository(KeyValueRepository keyValueRepository){
        this.keyValueRepository = keyValueRepository;
    }

    @Override
    public String getValue(HttpServletRequest request, String scope, String key) {
        if(isAuthorized(scope, request, METHOD.GET)) {
            KeyValue keyValue = keyValueRepository.findByKey(getPrefix(request, scope) + ":" + key);
            return keyValue != null ? keyValue.getValue() : "";
        } else {
            throw new SecurityException();
        }
    }

    @Override
    public void setValue(HttpServletRequest request, String scope, String key, String value) {
        if(isAuthorized(scope, request, METHOD.PUT)){
            KeyValue keyValue = new KeyValue();
            keyValue.setKey(getPrefix(request, scope)+":"+key);
            keyValue.setValue(value);
            keyValueRepository.save(keyValue);
        } else {
            throw new SecurityException();
        }
    }

    @Override
    public void delete(HttpServletRequest request, String scope, String key) {
        if(isAuthorized(scope, request, METHOD.DELETE)){
            keyValueRepository.delete(new KeyValue(getPrefix(request, scope)+":"+key));
        } else {
            throw new SecurityException();
        }
    }

    @Override
    public boolean isByUser(String scope) {
      String byUserString = "";
      if(scope != null) {
        byUserString = env.getRequiredProperty("scope." + scope + ".byUser");
      } else {
        byUserString = "true";
      }
        logger.trace("scope {} byUser? {}.", scope, byUserString);
        return byUserString != null && Boolean.parseBoolean(byUserString);
    }

    @Override
    public boolean isAuthorized(String scope, HttpServletRequest request, METHOD method) {
        if(getAttribute(request)==null){
            return false;
        }

        if(scope == null) {
          //all base keys are authorized, and are all username scoped.
          return true;
        } else if(isByUser(scope)){
            String prefixAttr = env.getRequiredProperty("scope." + scope + ".prefixAttribute");
            String filterHeaderValue = request.getHeader(prefixAttr);
            return filterHeaderValue != null;
        }
        else if(!METHOD.GET.equals(method)) {//global, and method != GET

            String adminGroup = env.getRequiredProperty("scope." + scope + ".admin.group");
            String groupHeader = env.getRequiredProperty("groupHeaderAttribute");
            String groups = request.getHeader(groupHeader);
            return groups !=null && groups.contains(adminGroup);
        } else {
            //global hit on a GET method
            return true;
        }
    }

    private String getPrefix (HttpServletRequest request, String scope) {
      if(isByUser(scope)) {
        String username = request.getHeader(getAttribute(request));
        return scope != null ? scope +":"+ username : username;
      } else {
        return scope;
      }
    }

}
