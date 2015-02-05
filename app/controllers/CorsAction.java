package controllers;

import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

/**
 * Created by yakov_000 on 05.02.2015.
 */
public class CorsAction extends Action.Simple {
    @Override
    public F.Promise<Result> call(Http.Context context) throws Throwable {
        context.response().setHeader("Access-Control-Allow-Origin","*");
        return delegate.call(context);
    }
}
