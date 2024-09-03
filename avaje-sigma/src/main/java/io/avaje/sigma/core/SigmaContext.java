package io.avaje.sigma.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;

import io.avaje.sigma.HttpContext;
import io.avaje.sigma.Routing.HttpMethod;
import io.avaje.sigma.aws.events.AWSHttpResponse;
import io.avaje.sigma.aws.events.AWSRequest;

class SigmaContext implements HttpContext {

  public static final String CONTENT_TYPE = "Content-Type";

  private final ServiceManager mgr;
  protected final AWSRequest req;
  private final Context ctx;
  private final Map<String, String> pathParams;
  private final Map<String, Object> attributes = new HashMap<>();
  private final String matchedPath;
  private final boolean multiValue;
  private final Map<String, String> responseHeaders;
  private final Map<String, List<String>> multiValueResponseHeaders;
  private int status = 200;

  private String body;
  private boolean base64Encoded;

  SigmaContext(ServiceManager mgr, AWSRequest req, Context ctx, String matchedPath) {
    this(mgr, req, ctx, matchedPath, Map.of());
  }

  SigmaContext(
      ServiceManager mgr,
      AWSRequest req,
      Context ctx,
      String matchedPath,
      Map<String, String> pathParams) {
    this.mgr = mgr;
    this.req = req;
    this.ctx = ctx;
    this.matchedPath = matchedPath;
    this.pathParams = pathParams;
    this.multiValue = req.hasMultiValueParams();
    multiValueResponseHeaders = multiValue ? new HashMap<>() : null;
    responseHeaders = !multiValue ? new HashMap<>() : null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends AWSRequest> T awsRequest() {
    return (T) req;
  }

  @Override
  public Context awsContext() {
    return ctx;
  }

  @Override
  public HttpContext attribute(String key, Object value) {
    attributes.put(key, value);
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T attribute(String key) {
    return (T) attributes.get(key);
  }

  @Override
  public String matchedPath() {
    return matchedPath;
  }

  @Override
  public <T> T bodyAsClass(Class<T> clazz) {
    return mgr.jsonRead(clazz, this.body);
  }

  @Override
  public String body() {
    return req.body();
  }

  @Override
  public Map<String, String> pathParamMap() {
    return pathParams;
  }

  @Override
  public String pathParam(String name) {
    return pathParams.get(name);
  }

  @Override
  public String queryParam(String name) {
    return req.queryParam(name);
  }

  @Override
  public List<String> queryParams(String name) {

    return req.queryParams(name);
  }

  @Override
  public SigmaContext status(int statusCode) {
    this.status = statusCode;
    return this;
  }

  @Override
  public int status() {
    return status;
  }

  @Override
  public String contentType() {
    return req.contentType();
  }

  @Override
  public SigmaContext contentType(String contentType) {

    header(CONTENT_TYPE, contentType);
    return this;
  }

  @Override
  public String header(String key) {
    return req.header(key);
  }

  @Override
  public SigmaContext header(String key, String value) {
    if (multiValue) {

      multiValueResponseHeaders.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    } else {

      responseHeaders.put(key, value);
    }

    return this;
  }

  @Override
  public HttpMethod method() {
    return req.httpMethod();
  }

  @Override
  public String path() {
    return req.path();
  }

  @Override
  public HttpContext json(Object bean) {
    contentType("application/json");
    var content = mgr.jsonWrite(bean);
    return writeBody(content);
  }

  /** Write plain text content to the response. */
  @Override
  public HttpContext text(String content) {
    contentType("text/plain");
    return writeBody(content);
  }

  /** Write html content to the response. */
  @Override
  public HttpContext html(String content) {
    contentType("text/html");
    return writeBody(content);
  }

  @Override
  public HttpContext writeBody(String content) {
    this.body = content;
    return this;
  }

  @Override
  public HttpContext base64EncodedBody(String content) {
    this.body = content;
    this.base64Encoded = true;
    return this;
  }

  public void resetResponse() {
    if (multiValue) {
      multiValueResponseHeaders.clear();
    } else {
      responseHeaders.clear();
    }
    base64Encoded = false;
    body = null;
    status = 200;
  }

  public AWSHttpResponse createResponse() {

    return new AWSHttpResponse(
        status, responseHeaders, multiValueResponseHeaders, body, base64Encoded);
  }
}
