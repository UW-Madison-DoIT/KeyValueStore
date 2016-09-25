package edu.wisc.my.keyvalue.controller

import edu.wisc.my.keyvalue.service.IKeyValueService
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.http.HttpStatus
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.util.StringUtils

import static junit.framework.TestCase.assertTrue
import static org.junit.Assert.assertEquals
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
class KeyValueStoreControllerTest  {

  private MockEnvironment env = new MockEnvironment();

  @InjectMocks private KeyValueStoreController controller = new KeyValueStoreController();

  @Mock IKeyValueService keyValueService;

  @Before
  void setUp() {
    controller.setEnv(env);
    controller.setKeyValueService(keyValueService);
  }

  @After
  void tearDown() {

  }

  @Test
  void testIndex() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    controller.index(response);

    assertEquals(response.getContentAsString(), "{\"status\":\"up\"}")
  }

  @Test
  void testGetKeyValue_noContent() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setPathInfo("doesnotexist");
    request.setMethod("GET");
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(keyValueService.getValue(request,null,"doesnotexist")).thenReturn("");

    controller.getKeyValue(request, response, "doesnotexist");

    assertTrue(204 == response.status)
    assertTrue(StringUtils.isEmpty(response.getContentAsString()));
  }

  @Test
  void testGetKeyValue_content() {
    final String content = "{ \"taco\" : \"tuesday\" }";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setPathInfo("exists");
    request.setMethod("GET");
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(keyValueService.getValue(request,null,"exists")).thenReturn(content);

    controller.getKeyValue(request, response, "exists");

    assertTrue(200 == response.status)
    JSONAssert.assertEquals(content, response.getContentAsString(), false);
  }

  @Test
  void testGetScopedKeyValue() {
    final String content = "{ \"taco\" : \"tuesday\", \"myuw\" : \"badger\" }";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setPathInfo("/myuw/exists");
    request.setMethod("GET");
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(keyValueService.getValue(request,"myuw","exists")).thenReturn(content);

    controller.getScopedKeyValue(request, response, "myuw", "exists");

    assertTrue(HttpStatus.OK.value() == response.status)
    JSONAssert.assertEquals(content, response.getContentAsString(), false);
  }

  @Test
  void testPutScopedKeyValue_valid_json() {
    final String content = "{ \"taco\" : \"tuesday\", \"myuw\" : \"badger\" }";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setPathInfo("/myuw/placement");
    request.setMethod("PUT");
    request.setContent(content.bytes);

    MockHttpServletResponse response = new MockHttpServletResponse();
    controller.putScopedKeyValue(request, response, "myuw", "placement", content);
    assertTrue(response.getStatus() == HttpStatus.OK.value());
    JSONAssert.assertEquals(response.getContentAsString(),content, false);
  }

  @Test
  void testPutScopedKeyValue_INvalid_json() {
    final String content = "{ \"taco\" : \"tuesday\", myuw : \"badger\" ";//missing }
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setPathInfo("/myuw/placement");
    request.setMethod("PUT");
    request.setContent(content.bytes);

    MockHttpServletResponse response = new MockHttpServletResponse();
    controller.putScopedKeyValue(request, response, "myuw", "placement", content);
    assertTrue(response.getStatus() == HttpStatus.BAD_REQUEST.value());
    assertTrue(StringUtils.isEmpty(response.getContentAsString()));
  }


  @Test
  void testSetKeyValue_invalid_json() {
    final String content = "{ \"taco\" : \"tuesday\" ";//missing }
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setPathInfo("/placement");
    request.setMethod("PUT");
    request.setContent(content.bytes);

    MockHttpServletResponse response = new MockHttpServletResponse();
    controller.setKeyValue(request, response, "placement",content);
    assertTrue(response.getStatus() == HttpStatus.BAD_REQUEST.value());
    assertTrue(StringUtils.isEmpty(response.getContentAsString()));
  }

  @Test
  void testSetKeyValue_valid_json() {
    final String content = "{ \"taco\" : \"tuesday\" }";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setPathInfo("/placement");
    request.setMethod("PUT");
    request.setContent(content.bytes);

    MockHttpServletResponse response = new MockHttpServletResponse();

    controller.setKeyValue(request, response, "placement",content);
    assertTrue(response.getStatus() == HttpStatus.OK.value());
    JSONAssert.assertEquals(response.getContentAsString(), content, false);
  }

  @Test
  void testDelete_okay() {
    final String key = "placement";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setPathInfo("/" + key);
    request.setMethod("DELETE");

    MockHttpServletResponse response = new MockHttpServletResponse();

    controller.delete(request, response, key);

    assertTrue(response.getStatus() == HttpStatus.OK.value());
  }

  @Test
  void testDelete_accessDenied() {
    final String key = "placement";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setPathInfo("/" + key);
    request.setMethod("DELETE");

    MockHttpServletResponse response = new MockHttpServletResponse();
    when(keyValueService.delete(request, null, key)).thenThrow(new SecurityException("You got no access to that!?!?!"))

    controller.delete(request, response, key);

    assertTrue(response.getStatus() == HttpStatus.FORBIDDEN.value());
  }

  @Test
  void testDelete_scope_okay() {
    final String key = "placement", scope = "myuw";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setPathInfo("/"+scope+"/" + key);
    request.setMethod("DELETE");

    MockHttpServletResponse response = new MockHttpServletResponse();

    controller.deleteScoped(request, response, scope, key);

    assertTrue(response.getStatus() == HttpStatus.OK.value());
  }

  @Test
  void testDelete_scope_accessDenied() {
    final String key = "placement", scope = "myuw";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setPathInfo("/"+scope+"/" + key);
    request.setMethod("DELETE");

    MockHttpServletResponse response = new MockHttpServletResponse();
    when(keyValueService.delete(request, scope, key)).thenThrow(new SecurityException("You got no access to that!?!?!"))

    controller.deleteScoped(request, response, scope, key);

    assertTrue(response.getStatus() == HttpStatus.FORBIDDEN.value());
  }
}
