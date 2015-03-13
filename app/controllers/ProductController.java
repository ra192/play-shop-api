package controllers;

import actors.CountProductPropertyValueActor;
import actors.ListProductsByCategoryAndFilterActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.JsonNode;
import dao.CategoryDao;
import dao.ProductDao;
import dao.PropertyDao;
import dto.ErrorResponseDto;
import model.Category;
import model.Product;
import model.PropertyValue;
import play.api.*;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.HttpExecution;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import scala.concurrent.Future;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by yakov_000 on 09.02.2015.
 */
public class ProductController extends Controller {

    public static Promise<Result> listByCategoryAndFilter(String categoryName) {

        final JsonNode bodyAsJson = request().body().asJson();

        final ActorRef actorRef = Akka.system().actorOf(Props.create(ListProductsByCategoryAndFilterActor.class));

        final ArrayList<String> propertyValues = new ArrayList<>();
        bodyAsJson.get("propertyValues").forEach(itm -> propertyValues.add(itm.asText()));

        final Integer first = (bodyAsJson.get("first") != null) ? bodyAsJson.get("first").asInt() : 0;
        final Integer max = (bodyAsJson.get("max") != null) ? bodyAsJson.get("max").asInt() : 100;
        final String orderProperty = (bodyAsJson.get("orderProperty") != null) ? bodyAsJson.get("max").asText() : "displayName";
        final Boolean isAsk = (bodyAsJson.get("isAsk") != null) ? bodyAsJson.get("max").asBoolean() : true;

        final Promise<Result> promiseResult = Promise.wrap(Patterns.ask(actorRef,
                new ListProductsByCategoryAndFilterActor.Message(categoryName, propertyValues, first, max, orderProperty, isAsk), 5000)).map(res -> ok(Json.toJson(res)));
        return promiseResult.recover(error -> ok(Json.toJson(new ErrorResponseDto(error.getMessage()))));
    }

    public static Promise<Result> countPropertiesByCategoryAndFilter(String categoryName) {

        final JsonNode bodyAsJson = request().body().asJson();

        final ActorRef actorRef = Akka.system().actorOf(Props.create(CountProductPropertyValueActor.class));

        final ArrayList<String> propertyValues = new ArrayList<>();
        bodyAsJson.get("propertyValues").forEach(itm -> propertyValues.add(itm.asText()));

        final Promise<Result> promiseResult = Promise.wrap(Patterns.ask(actorRef,
                new CountProductPropertyValueActor.Message(categoryName, propertyValues), 5000)).map(res -> ok(Json.toJson(res)));
        return promiseResult.recover(error -> ok(Json.toJson(new ErrorResponseDto(error.getMessage()))));
    }

    public static Promise<Result> create() {

        final JsonNode jsonNode = request().body().asJson();

        final Promise<Category> categoryPromise = Promise.wrap(CategoryDao.getByName(jsonNode.get("category").asText()));

        List<Promise<PropertyValue>> propertyValuePromises = new ArrayList<>();
        jsonNode.get("propertyValues").forEach(propVal -> propertyValuePromises.add(Promise.wrap(PropertyDao.getPropertyValueByName(propVal.asText()))));

        final Promise<Result> result = categoryPromise.zip(Promise.sequence(propertyValuePromises)).flatMap(res -> {

            Product product = new Product(null, jsonNode.get("code").asText(), jsonNode.get("displayName").asText(),
                    jsonNode.get("price").asDouble(), jsonNode.get("description").asText(), jsonNode.get("imageUrl").asText(),
                    res._1.getId());

            return Promise.wrap(ProductDao.create(product, res._2.stream().map(PropertyValue::getId).collect(Collectors.toSet())));
        }).map(res -> ok(Json.toJson("created")));

        return result.recover(error -> ok(Json.toJson(new ErrorResponseDto(error.getMessage()))));
    }
}
