package edu.wisc.my.keyvalue.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.wisc.my.keyvalue.model.KeyValue;
import edu.wisc.my.keyvalue.repository.KeyValueRepository;

@Service
public class KeyValueServiceImpl implements IKeyValueService{
    
    private KeyValueRepository keyValueRepository;
    
    @Autowired
    public void setKeyValueRepository(KeyValueRepository keyValueRepository){
        this.keyValueRepository = keyValueRepository;
    }

    @Override
    public String getValue(String username, String key) {
        KeyValue keyValue = keyValueRepository.findByKey(username+":"+key);
        return keyValue!=null ? keyValue.getValue() : "";
    }

    @Override
    public String setValue(String username, String key, String value) {
        KeyValue keyValue = new KeyValue();
        keyValue.setKey(username+":"+key);
        keyValue.setValue(value);
        keyValueRepository.save(keyValue);
        return "Saved?";
    }

    @Override
    public void delete(String username, String key) {
      keyValueRepository.delete(new KeyValue(username+":"+key));
    }
    
}