package controllers;

import play.mvc.Controller;
import play.mvc.Result;

/**
 * Created by yakov_000 on 10.02.2015.
 */
public class OptionsRequestController extends Controller {

    public static Result options(String path) {

        response().setHeader("Access-Control-Allow-Headers","Content-Type, x-xsrf-token, accessToken");
        return ok();
    }
}
