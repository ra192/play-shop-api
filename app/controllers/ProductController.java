package controllers;

import actors.CountProductPropertyValueActor;
import actors.ListProductsByCategoryAndFilterActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.JsonNode;
import dao.CategoryDao;
import dao.ProductDao;
import dao.PropertyValueDao;
import dto.ErrorResponseDto;
import model.Category;
import model.Product;
import model.PropertyValue;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by yakov_000 on 09.02.2015.
 */
@With(CorsAction.class)
public class ProductController extends Controller {

    public static Promise<Result> listByCategoryAndFilter(String categoryName, List<String> propertyValues, String orderProperty,
                                                          Boolean isAsk, Integer first, Integer max) {

        final ActorRef actorRef = Akka.system().actorOf(Props.create(ListProductsByCategoryAndFilterActor.class));

        final Promise<Result> promiseResult = Promise.wrap(Patterns.ask(actorRef,
                new ListProductsByCategoryAndFilterActor.Message(categoryName, propertyValues, first, max, orderProperty, isAsk), 5000)).map(res -> ok(Json.toJson(res)));

        return promiseResult.recover(error -> ok(Json.toJson(new ErrorResponseDto(error.getMessage()))));
    }

    public static Promise<Result> countPropertiesByCategoryAndFilter(String categoryName, List<String> propertyValues) {

        final ActorRef actorRef = Akka.system().actorOf(Props.create(CountProductPropertyValueActor.class));

        final Promise<Result> promiseResult = Promise.wrap(Patterns.ask(actorRef,
                new CountProductPropertyValueActor.Message(categoryName, propertyValues), 5000)).map(res -> ok(Json.toJson(res)));
        return promiseResult.recover(error -> ok(Json.toJson(new ErrorResponseDto(error.getMessage()))));
    }

    public static Promise<Result> create() {

        final JsonNode jsonNode = request().body().asJson();

        final Promise<Category> categoryPromise = Promise.wrap(CategoryDao.getByName(jsonNode.get("category").asText()));

        List<Promise<PropertyValue>> propertyValuePromises = new ArrayList<>();
        jsonNode.get("propertyValues").forEach(propVal -> propertyValuePromises.add(Promise.wrap(PropertyValueDao.getByName(propVal.asText()))));

        final Promise<Result> result = categoryPromise.zip(Promise.sequence(propertyValuePromises)).flatMap(res -> {

            Product product = new Product(null, jsonNode.get("code").asText(), jsonNode.get("displayName").asText(),
                    jsonNode.get("price").asDouble(), jsonNode.get("description").asText(), jsonNode.get("imageUrl").asText(),
                    res._1.getId());

            return Promise.wrap(ProductDao.create(product, res._2.stream().map(PropertyValue::getId).collect(Collectors.toSet())));
        }).map(res -> ok(Json.toJson("created")));

        return result.recover(error -> ok(Json.toJson(new ErrorResponseDto(error.getMessage()))));
    }

    public static Promise<Result> update() {

        final JsonNode jsonNode = request().body().asJson();

        final Promise<Product> productPromise = Promise.wrap(ProductDao.getByCode(jsonNode.get("code").asText()));

        final Promise<Category> categoryPromise = Promise.wrap(CategoryDao.getByName(jsonNode.get("category").asText()));

        List<Promise<PropertyValue>> propertyValuePromises = new ArrayList<>();
        jsonNode.get("propertyValues").forEach(propVal -> propertyValuePromises.add(Promise.wrap(PropertyValueDao.getByName(propVal.asText()))));

        final Promise<Result> result = productPromise.zip(categoryPromise.zip(Promise.sequence(propertyValuePromises))).flatMap(res -> {
            Product product = res._1;
            product.setDisplayName(jsonNode.get("displayName").asText());
            product.setPrice(jsonNode.get("price").asDouble());
            product.setDescription(jsonNode.get("description").asText());
            product.setImageUrl(jsonNode.get("imageUrl").asText());
            product.setCategoryId(res._2._1.getId());

            return Promise.wrap(ProductDao.update(product, res._2._2.stream().map(PropertyValue::getId).collect(Collectors.toSet())));
        }).map(res -> ok(Json.toJson("updated")));

        return result.recover(error -> ok(Json.toJson(new ErrorResponseDto(error.getMessage()))));
    }
}
