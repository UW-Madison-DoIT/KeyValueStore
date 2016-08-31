package edu.wisc.my.keyvalue.service;

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
    private String usernameAttribute;

    @Value("${usernameAttribute}")
    public void setUsernameAttr(String attr) {
        usernameAttribute = attr;
    }

    @Autowired
    private Environment env;
    
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
    public boolean isByUser(String scope){
        String byUserString = env.getRequiredProperty("scope." + scope + ".byUser");
        logger.trace("scope {} byUser? {}.", scope, byUserString);
        return byUserString != null && Boolean.parseBoolean(byUserString);
    }

    @Override
    public boolean isAuthorized(String scope, HttpServletRequest request, METHOD method) {
        if(request.getHeader(usernameAttribute) == null) {

            return false;
        }

        if(isByUser(scope)){
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
        return isByUser(scope) ? scope + request.getHeader(usernameAttribute) : scope;
    }

}