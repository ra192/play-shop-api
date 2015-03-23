package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import dao.PropertyDao;
import dao.PropertyValueDao;
import dto.ErrorResponseDto;
import model.Property;
import model.PropertyValue;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yakov_000 on 17.03.2015.
 */
public class PropertyController extends Controller {

    public static Promise<Result> create() {

        final JsonNode jsonNode = request().body().asJson();

        final Property property = new Property(null, jsonNode.get("name").asText(), jsonNode.get("displayName").asText());

        final Promise<Long> propertyPromise = Promise.wrap(PropertyDao.create(property));
        final Promise<Result> result = propertyPromise.flatMap(propertyId -> {
            final List<Promise<Long>> propertyValuePromises = new ArrayList<>();
            jsonNode.get("propertyValues").forEach(itm -> {
                final PropertyValue propertyValue = new PropertyValue(null, itm.get("name").asText(), itm.get("displayName").asText(), propertyId);
                propertyValuePromises.add(Promise.wrap(PropertyValueDao.create(propertyValue)));
            });
            return Promise.sequence(propertyValuePromises);
        }).map(res -> ok(Json.toJson("created")));

        return result.recover(error->ok(Json.toJson(new ErrorResponseDto(error.getMessage()))));
    }

    public static Promise<Result>update() {

        final JsonNode jsonNode = request().body().asJson();

        final Promise<Result> result = Promise.wrap(PropertyDao.getByName(jsonNode.get("name").asText())).flatMap(property -> {
            property.setDisplayName(jsonNode.get("displayName").asText());
            return Promise.wrap(PropertyDao.update(property));
        }).map(res -> ok(Json.toJson("created")));

        return result.recover(error->ok(Json.toJson(new ErrorResponseDto(error.getMessage()))));
    }
}
