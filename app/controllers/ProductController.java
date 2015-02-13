package controllers;

import actors.GetProductsByCategoryAndFilterActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.JsonNode;
import dto.ErrorrResponseDto;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.ArrayList;

/**
 * Created by yakov_000 on 09.02.2015.
 */
public class ProductController extends Controller {

    public static Promise<Result> listByCategoryAndFilter(String categoryName) {

        final JsonNode bodyAsJson = request().body().asJson();

        final ActorRef actorRef = Akka.system().actorOf(Props.create(GetProductsByCategoryAndFilterActor.class));

        final ArrayList<String> propertyValues = new ArrayList<>();
        bodyAsJson.get("propertyValues").forEach(itm -> propertyValues.add(itm.asText()));

        final Integer first = (bodyAsJson.get("first") != null) ? bodyAsJson.get("first").asInt() : 0;
        final Integer max = (bodyAsJson.get("max") != null) ? bodyAsJson.get("max").asInt() : 100;
        final String orderProperty = (bodyAsJson.get("orderProperty") != null) ? bodyAsJson.get("max").asText() : "displayName";
        final Boolean isAsk = (bodyAsJson.get("isAsk") != null) ? bodyAsJson.get("max").asBoolean() : true;

        final Promise<Result> promiseResult = Promise.wrap(Patterns.ask(actorRef,
                new GetProductsByCategoryAndFilterActor.Message(categoryName, propertyValues, first, max, orderProperty, isAsk), 5000)).map(res -> ok(Json.toJson(res)));
        return promiseResult.recover(error -> ok(Json.toJson(new ErrorrResponseDto(error.getMessage()))));
    }
}
