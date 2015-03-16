package controllers;

import actors.ListCategoriesByParentActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.JsonNode;
import dao.CategoryDao;
import dao.PropertyDao;
import dto.CategoryDto;
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

        return Promise.wrap(Patterns.ask(actorRef, new Long(0), 5000)).map(res->{
            List<CategoryDto>categories= (List<CategoryDto>) res;
            return ok(Json.toJson(new ListResponseDto<>(categories)));
        });
    }

    public static Promise<Result>create() {

        final JsonNode jsonNode = request().body().asJson();

        final Promise<Category> parentPromise = Promise.wrap(CategoryDao.getByName(jsonNode.get("parent").asText()));

        List<Promise<Property>>propertyPromises=new ArrayList<>();
        jsonNode.get("properties").forEach(propVal->propertyPromises.add(Promise.wrap(PropertyDao.getByName(propVal.asText()))));

        final Promise<Result> result = parentPromise.zip(Promise.sequence(propertyPromises)).flatMap(res -> {
            final Category category = new Category(null, jsonNode.get("name").asText(), jsonNode.get("displayName").asText(), res._1.getId());
            return Promise.wrap(CategoryDao.create(category, res._2.stream().map(Property::getId).collect(Collectors.toSet())));
        }).map(res -> ok(Json.toJson("created")));

        return result.recover(error->ok(Json.toJson(error.getMessage())));
    }

    public static Promise<Result>  update() {
        final JsonNode jsonNode = request().body().asJson();

        final Promise<Category> parentPromise = Promise.wrap(CategoryDao.getByName(jsonNode.get("parent").asText()));

        List<Promise<Property>>propertyPromises=new ArrayList<>();
        jsonNode.get("properties").forEach(propVal->propertyPromises.add(Promise.wrap(PropertyDao.getByName(propVal.asText()))));

        final Promise<Result> result = parentPromise.zip(Promise.sequence(propertyPromises)).flatMap(res -> {
            final Category category = new Category(jsonNode.get("id").asLong(), jsonNode.get("name").asText(), jsonNode.get("displayName").asText(), res._1.getId());
            return Promise.wrap(CategoryDao.update(category, res._2.stream().map(Property::getId).collect(Collectors.toSet())));
        }).map(res -> ok(Json.toJson("created")));

        return result.recover(error->ok(Json.toJson(error.getMessage())));
    }
}
