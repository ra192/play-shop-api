package controllers;

import actors.GetCategoriesByParentActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import dto.CategoryDto;
import dto.ListResponseDto;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import java.util.List;


/**
 * Created by yakov_000 on 29.01.2015.
 */
@With(CorsAction.class)
public class CategoryController extends Controller {

    public static Promise<Result> listRoots() {

        final ActorRef actorRef = Akka.system().actorOf(Props.create(GetCategoriesByParentActor.class));

        return Promise.wrap(Patterns.ask(actorRef, new Long(0), 5000)).map(res->{
            List<CategoryDto>categories= (List<CategoryDto>) res;
            return ok(Json.toJson(new ListResponseDto<>(categories)));
        });
    }
}
