package controllers;

import actors.GetProductsByCategoryAndFilterActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.JsonNode;
import dto.ErrorrResponseDto;
import dto.ListResponseDto;
import dto.ProductDto;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yakov_000 on 09.02.2015.
 */
public class ProductController extends Controller {

    public static Promise<Result>listByCategoryAndFilter(String categoryName) {

        final JsonNode bodyAsJson = request().body().asJson();

        final ActorRef actorRef = Akka.system().actorOf(Props.create(GetProductsByCategoryAndFilterActor.class));

        final ArrayList<String> propertyValues = new ArrayList<>();
        bodyAsJson.get("propertyValues").forEach(itm->propertyValues.add(itm.asText()));

        final Promise<Result> promiseResult = Promise.wrap(Patterns.ask(actorRef, new GetProductsByCategoryAndFilterActor.Message(categoryName, propertyValues), 5000)).map(res -> {
            List<ProductDto> products = (List<ProductDto>) res;
            return ok(Json.toJson(new ListResponseDto<>(products)));
        });
        return promiseResult.recover(error -> ok(Json.toJson(new ErrorrResponseDto(error.getMessage()))));
    }
}
