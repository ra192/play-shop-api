package controllers;

import actors.GetProductsByCategoryAndFilter;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import dto.ListResponseDto;
import dto.ProductDto;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;

/**
 * Created by yakov_000 on 09.02.2015.
 */
public class ProductController extends Controller {

    public static Promise<Result>listByCategoryAndFilter(String categoryName) {

        final ActorRef actorRef = Akka.system().actorOf(Props.create(GetProductsByCategoryAndFilter.class));

        return Promise.wrap(Patterns.ask(actorRef,new GetProductsByCategoryAndFilter.Message(categoryName,"filterStub"),5000)).map(res-> {
            List<ProductDto>products=(List<ProductDto>)res;
            return ok(Json.toJson(new ListResponseDto<>(products)));
        });
    }
}
