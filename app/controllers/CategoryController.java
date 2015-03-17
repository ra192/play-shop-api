package controllers;

import actors.ListCategoriesByParentActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.JsonNode;
import dao.CategoryDao;
import dao.PropertyDao;
import dto.CategoryDto;
import dto.ErrorResponseDto;
import dto.ListResponseDto;
import model.Category;
import model.Property;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import scala.concurrent.Future;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Created by yakov_000 on 29.01.2015.
 */
@With(CorsAction.class)
public class CategoryController extends Controller {

    public static Promise<Result> listRoots() {

        final ActorRef actorRef = Akka.system().actorOf(Props.create(ListCategoriesByParentActor.class));

        return Promise.wrap(Patterns.ask(actorRef, new Long(0), 5000)).map(res -> {
            List<CategoryDto> categories = (List<CategoryDto>) res;
            return ok(Json.toJson(new ListResponseDto<>(categories)));
        });
    }

    public static Promise<Result> create() {

        final JsonNode jsonNode = request().body().asJson();

        final Promise<Category> parentPromise;
        if (jsonNode.hasNonNull("parent"))
            parentPromise = Promise.wrap(CategoryDao.getByName(jsonNode.get("parent").asText()));
        else parentPromise = Promise.pure(null);

        List<Promise<Property>> propertyPromises = new ArrayList<>();
        jsonNode.get("properties").forEach(propVal -> propertyPromises.add(Promise.wrap(PropertyDao.getByName(propVal.asText()))));

        final Promise<Result> result = parentPromise.zip(Promise.sequence(propertyPromises)).flatMap(res -> {
            final Long parentId = (res._1 != null) ? res._1.getId() : null;
            final Category category = new Category(null, jsonNode.get("name").asText(), jsonNode.get("displayName").asText(), parentId);

            return Promise.wrap(CategoryDao.create(category, res._2.stream().map(Property::getId).collect(Collectors.toSet())));
        }).map(res -> ok(Json.toJson("created")));

        return result.recover(error -> ok(Json.toJson(new ErrorResponseDto(error.getMessage()))));
    }

    public static Promise<Result> update() {
        final JsonNode jsonNode = request().body().asJson();

        final Promise<Category> categoryPromise = Promise.wrap(CategoryDao.getByName("name"));

        final Promise<Category> parentPromise;
        if (jsonNode.has("parent"))
            parentPromise = Promise.wrap(CategoryDao.getByName(jsonNode.get("parent").asText()));
        else parentPromise = Promise.pure(null);

        List<Promise<Property>> propertyPromises = new ArrayList<>();
        jsonNode.get("properties").forEach(propVal -> propertyPromises.add(Promise.wrap(PropertyDao.getByName(propVal.asText()))));

        final Promise<Result> result = categoryPromise.zip(parentPromise.zip(Promise.sequence(propertyPromises))).flatMap(res -> {
            final Long parentId = (res._2._1 != null)?res._2._1.getId():null;
            final Category category = res._1;
            category.setDisplayName(jsonNode.get("displayName").asText());
            category.setParentId(parentId);

            return Promise.wrap(CategoryDao.update(category, res._2._2.stream().map(Property::getId).collect(Collectors.toSet())));
        }).map(res -> ok(Json.toJson("created")));

        return result.recover(error -> ok(Json.toJson(new ErrorResponseDto(error.getMessage()))));
    }
}
