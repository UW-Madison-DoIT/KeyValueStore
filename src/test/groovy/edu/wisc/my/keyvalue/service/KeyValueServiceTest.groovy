package edu.wisc.my.keyvalue.service

import edu.wisc.my.keyvalue.model.KeyValue
import edu.wisc.my.keyvalue.repository.KeyValueRepository
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.internal.stubbing.answers.ThrowsException
import org.mockito.invocation.InvocationOnMock
import org.mockito.runners.MockitoJUnitRunner
import org.mockito.stubbing.Answer
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.http.HttpMethod
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockHttpServletRequest

import static org.junit.Assert.fail
import static org.mockito.Mockito.when
import static org.mockito.Mockito.any
import static org.junit.Assert.assertEquals

@RunWith(MockitoJUnitRunner.class)
class KeyValueServiceTest {
  protected final Logger logger = LoggerFactory.getLogger(getClass());


  @InjectMocks KeyValueServiceImpl keyValueService = new KeyValueServiceImpl();

  @Mock KeyValueRepository keyValueRepository;
  private MockEnvironment env = new MockEnvironment();

  final String usernameAttribute = "uid";
  final String additionalAttributes = "sysid,dummy,tertiary";

  @Before()
  void setup() {

    env.setProperty("usernameAttribute","uid");
    env.setProperty("additionalAttributes","sysid,dummy,tertiary");
    env.setProperty("groupHeaderAttribute", "ismemberof");

    env.setProperty("scope.global.byUser", "false");
    env.setProperty("scope.global.admin.group", "uw:domain:my.wisc.edu:my_uw_administrators");

    env.setProperty("scope.myuw.byUser", "true");
    env.setProperty("scope.myuw.prefixAttribute", "uid");

    keyValueService.setKeyValueRepository(keyValueRepository);
    keyValueService.setUsernameAttr(usernameAttribute);
    keyValueService.setAdditionalAttributes(additionalAttributes);
    keyValueService.setEnv(env);
  }



  @Test
  void test_global_get_valid() {
    final String scope = "global", key = "someKey", url = "/" + scope + "/" + key;
    final KeyValue ret = new KeyValue(key);
    ret.setValue("{ why : 42 }");
    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.toString(),url);
    request.addHeader("uid", "levett");
    when(keyValueRepository.findByKey("global:someKey")).thenReturn(ret);
    String response = keyValueService.getValue(request, "global", "someKey");
    assertEquals(ret.getValue(), response);
  }
  
  @Test(expected=SecurityException.class)
  void test_no_user_name_in_header_get(){ 
    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.toString(), "")
    keyValueService.getValue(request, "global", "someKey")
  }
  
  @Test(expected=SecurityException.class)
  void test_blank_user_name_in_header_get(){ 
    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.toString(), "")
    request.addHeader("uid", "  ")
    keyValueService.getValue(request, "global", "someKey")
  }
  
    @Test(expected=SecurityException.class)
  void test_no_user_name_in_header_put(){ 
    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.PUT.toString(), "")
    keyValueService.getValue(request, "global", "someKey")
  }
  
  @Test(expected=SecurityException.class)
  void test_blank_user_name_in_header_put(){ 
    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.PUT.toString(), "")
    request.addHeader("uid", "  ")
    keyValueService.getValue(request, "global", "someKey")
  }

  @Test(expected=SecurityException.class)
  void test_global_put_invalid() {
    final String scope = "global", key = "someKey", url = "/" + scope + "/" + key, value = "{ why : 42 }";

    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.PUT.toString(),url);
    request.addHeader("uid", "levett");

    keyValueService.setValue(request, scope, key, value);
    fail("Should not get here")
  }

  @Test
  void test_additional_attributes(){
    keyValueService.setUsernameAttr("NONE");
      MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.toString(), "")
    request.addHeader("dummy", "reed")
    keyValueService.getValue(request, "global", "someKey")
  }

    @Test
  void test_null_additional_attributes(){
    keyValueService.setAdditionalAttributes(null);
    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.toString(), "")
    request.addHeader("uid", "reed")
    keyValueService.getValue(request, "global", "someKey")
  }

  @Test()
  void test_global_put_valid() {
    final String scope = "global", key = "someKey", url = "/" + scope + "/" + key, value = "{ why : 42 }";

    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.PUT.toString(),url);
    request.addHeader("uid", "levett");
    request.addHeader("ismemberof","uw:domain:my.wisc.edu:my_uw_administrators")

    keyValueService.setValue(request, scope, key, value);
    assertEquals(true, true);
  }

  @Test
  void test_put_value(){
    final String key = "someKey", url = "/" + key, value = "{ why : 42 }", user = "levett";
    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.toString(),url);
    KeyValue kv = new KeyValue(user + ":"+ key);
    kv.setValue(value);
    request.addHeader("uid", user);
    request.setContent(value.bytes);

    when(keyValueRepository.save(any(KeyValue.class))).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        KeyValue thePassedInObject = args[0];
        //validate that the key is user:key
        assertEquals(thePassedInObject.getKey(),user + ":" + key);
        JSONAssert.assertEquals(thePassedInObject.getValue(),value, false)
        return thePassedInObject;
      }
    });

    keyValueService.setValue(request, null, key, value);
  }

  @Test
  void test_delete_value(){
    final String key = "someKey", url = "/" + key, value = "{ why : 42 }", user = "levett";
    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.toString(),url);
    KeyValue kv = new KeyValue(user + ":"+ key);
    kv.setValue(value);
    request.addHeader("uid", user);
    request.setContent(value.bytes);

    when(keyValueRepository.delete(any(KeyValue.class))).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        KeyValue thePassedInObject = args[0];
        //validate that the key is user:key
        assertEquals(thePassedInObject.getKey(),user + ":" + key);
        return thePassedInObject;
      }
    });

    keyValueService.delete(request, null, key);
  }

  @Test(expected = SecurityException.class)
  void test_delete_global_value_without_access(){
    final String scope = "global", key = "someKey", url = "/" + scope + "/" + key

    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.DELETE.toString(),url);
    request.addHeader("uid", "levett");

    keyValueService.delete(request, scope, key);
  }

  @Test()
  void test_delete_global_value_with_access(){
    final String scope = "global", key = "someKey", url = "/" + scope + "/" + key

    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.DELETE.toString(),url);
    request.addHeader("uid", "levett");
    request.addHeader("ismemberof","uw:domain:my.wisc.edu:my_uw_administrators")

    keyValueService.delete(request, scope, key);
  }
}
