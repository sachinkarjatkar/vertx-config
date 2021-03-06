package io.vertx.config.impl.spi;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class HttpConfigStoreTest extends ConfigStoreTestBase {

  public static final String JSON = "{\n" +
      "  \"key\": \"value\",\n" +
      "  \"sub\": {\n" +
      "    \"foo\": \"bar\"\n" +
      "  },\n" +
      "  \"array\": [\n" +
      "    1,\n" +
      "    2,\n" +
      "    3\n" +
      "  ],\n" +
      "  \"int\": 5,\n" +
      "  \"float\": 25.3,\n" +
      "  \"true\": true,\n" +
      "  \"false\": false\n" +
      "}";

  @Before
  public void init() {
    factory = new HttpConfigStoreFactory();

    AtomicBoolean done = new AtomicBoolean();
    vertx.createHttpServer()
        .requestHandler(request -> {
          if (request.path().endsWith("/A")) {
            request.response().end(new JsonObject(JSON).encodePrettily());
          }
          if (request.path().endsWith("/B")) {
            request.response().end(new JsonObject().put("key", "http-value").encodePrettily());
          }
          if (request.path().endsWith("/C")) {
            // Properties
            request.response().end("#some properties\nfoo=bar\nkey=value");
          }
        })
        .listen(8080, s -> {
          done.set(true);
        });

    await().untilAtomic(done, is(true));
  }


  @Test
  public void testJsonConf(TestContext tc) {
    Async async = tc.async();
    store = factory.create(vertx, new JsonObject()
        .put("host", "localhost")
        .put("port", 8080)
        .put("path", "/A")
    );

    getJsonConfiguration(vertx, store, ar -> {
      ConfigChecker.check(ar);
      async.complete();
    });
  }

  @Test
  public void testPropertiesConf(TestContext tc) {
    Async async = tc.async();
    store = factory.create(vertx, new JsonObject()
        .put("host", "localhost")
        .put("port", 8080)
        .put("path", "/C")
    );

    getPropertiesConfiguration(vertx, store, ar -> {
      assertThat(ar.succeeded()).isTrue();
      assertThat(ar.result().getString("key")).isEqualTo("value");
      assertThat(ar.result().getString("foo")).isEqualTo("bar");
      async.complete();
    });
  }

  @Test
  public void testName() {
    assertThat(factory.name()).isNotNull().isEqualTo("http");
  }


  @Test
  public void testWrongServer(TestContext tc) {
    Async async = tc.async();
    store = factory.create(vertx, new JsonObject()
        .put("host", "localhost")
        .put("port", 8085)
        .put("path", "/B")
    );

    getJsonConfiguration(vertx, store, ar -> {
      assertThat(ar.failed()).isTrue();
      async.complete();
    });
  }

}