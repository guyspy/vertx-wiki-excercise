package io.vertx.starter.models;

import eu.dozd.mongo.annotation.Entity;
import eu.dozd.mongo.annotation.Id;

/**
 * mapping mongo
 */
@Entity
public class Page {

  @Id
  String id;

  String name;

  String content;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
