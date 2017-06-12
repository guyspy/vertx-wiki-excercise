package io.vertx.starter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.mongodb.ServerAddress;
import com.mongodb.async.client.*;
import com.mongodb.connection.ClusterSettings;
import eu.dozd.mongo.MongoMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;
import io.vertx.starter.models.Page;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import static java.util.Collections.singletonList;

public class MainVerticle extends AbstractVerticle {

  private JDBCClient dbClient;

  private MongoDatabase database;

  private MongoCollection<Page> collection;

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Future<Void> steps = prepareDatabase().compose(aVoid -> startHttpServer());
    steps.setHandler(startFuture.completer());
  }

  private Future<Void> prepareDatabase(){
    Future<Void> future = Future.future();

    try{
      CodecRegistry codecRegistry = CodecRegistries.fromProviders(MongoMapper.getProviders());
      ClusterSettings clusterSettings = ClusterSettings.builder().hosts(singletonList(new ServerAddress("localhost", 27017))).build();
      MongoClientSettings settings = MongoClientSettings.builder().codecRegistry(codecRegistry)
        .clusterSettings(clusterSettings).build();
      MongoClient mongoClient = MongoClients.create(settings);
      database = mongoClient.getDatabase("vertxwiki");
      collection = database.getCollection("page", Page.class);

      LOGGER.info("prepareDatabase completed ");
      future.complete();
    }
    catch (Exception e){
      future.fail(e);
    }

    // TODO
    return future;
  }

  private Future<Void> startHttpServer(){
    Future<Void> future = Future.future();
    HttpServer httpServer = vertx.createHttpServer();
    Router router = Router.router(vertx);
    router.get("/").handler(this::indexHandler);
    httpServer
      .requestHandler(router::accept)
      .listen(9000, ar -> {
        if (ar.succeeded()){
          LOGGER.info("HTTP server running on port 9000");
          future.complete();
        }
        else {
          LOGGER.error("Could not start a HTTP server", ar.cause());
          future.fail(ar.cause());
        }
      });

    /*HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);
    router.get("/").handler(this::indexHandler);
    router.get("/wiki/:page").handler(this::pageRenderingHandler);
    router.post().handler(BodyHandler.create());
    router.post("/save").handler(this::pageUpdateHandler);
    router.post("/create").handler(this::pageCreateHandler);
    router.post("/delete").handler(this::pageDeletionHandler);
    server
      .requestHandler(router::accept).listen(8080, ar -> {
      if (ar.succeeded()) {
        LOGGER.info("HTTP server running on port 8080"); future.complete();
      } else {
        LOGGER.error("Could not start a HTTP server", ar.cause()); future.fail(ar.cause());
      } });*/
    return future;
  }


  private final HandlebarsTemplateEngine handlebarsTemplateEngine = HandlebarsTemplateEngine.create();

  private void indexHandler(RoutingContext routingContext){
    _toList(collection.find()).setHandler(listAsyncResult -> {
      if (listAsyncResult.succeeded()){
        routingContext.put("pages", listAsyncResult.result());
        routingContext.put("title", "wiki home");
        handlebarsTemplateEngine.render(routingContext, "templates/index.hbs", ar -> {
          if (ar.succeeded()){
            routingContext.response().putHeader("Content-Type", "text/html");
            routingContext.response().end(ar.result());
          }
          else {
            routingContext.fail(ar.cause());
          }
        });
      }
      else {
        routingContext.fail(listAsyncResult.cause());
      }
    });
  }

  private static Future<List<Page>> _toList(FindIterable<Page> findIterable){
    Future<List<Page>> future = Future.future();
    List<Page> pages = new ArrayList<>();
    findIterable.forEach(
      pages::add,
      (aVoid, throwable) -> {
        if (throwable != null){
          future.fail(throwable);
        }
        else {
          future.complete(pages);
        }
      }
    );
    return future;
  }



  private static final String SQL_CREATE_PAGES_TABLE = "create table if not exists Pages (Id integer identity primary key, Name varchar(255) unique, Content clob)";
  private static final String SQL_GET_PAGE = "select Id, Content from Pages where Name = ?";
  private static final String SQL_CREATE_PAGE = "insert into Pages values (NULL, ?, ?)";
  private static final String SQL_SAVE_PAGE = "update Pages set Content = ? where Id = ?";
  private static final String SQL_ALL_PAGES = "select Name from Pages";
  private static final String SQL_DELETE_PAGE = "delete from Pages where Id = ?";



}
