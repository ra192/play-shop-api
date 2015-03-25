package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import dao.CategoryDao;
import dao.ProductDao;
import dao.PropertyDao;
import dao.PropertyValueDao;
import dto.*;
import model.Category;
import model.Product;
import model.Property;
import model.PropertyValue;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by yakov_000 on 09.02.2015.
 */
@With(CorsAction.class)
public class ProductController extends Controller {

    public static Promise<Result> listByCategoryAndFilter(String categoryName, List<String> propertyValues, String orderProperty,
                                                          Boolean isAsk, Integer first, Integer max) {

        final Promise<Category> categoryPromise = Promise.wrap(CategoryDao.getByName(categoryName));

        final Promise<List<PropertyValue>> propertyValuesPromise = Promise.sequence(propertyValues.stream()
                .map(PropertyValueDao::getByName).map(Promise::wrap).collect(Collectors.toList()));

        final Promise<Result> promiseResult = categoryPromise.zip(propertyValuesPromise)
                .flatMap(catAndPropVals -> {
                    final Map<Long, List<PropertyValue>> propertyValuesMap = groupPropertyValues(catAndPropVals._2);

                    final Map<Long, List<Long>> propertyValueIds = propertyValuesMap.keySet().stream().collect(
                            Collectors.toMap(key -> key, key -> propertyValuesMap.get(key).stream()
                                    .map(PropertyValue::getId).collect(Collectors.toList())));

                    final Promise<List<Product>> productsPromise = Promise.wrap(ProductDao.listByCategoryIdAndPropertyValues(
                            catAndPropVals._1.getId(), propertyValueIds, first, max, orderProperty, isAsk));

                    final Promise<List<ProductDto>> productsDtoPromise = productsPromise.flatMap(products ->
                            Promise.sequence(products.stream().map(product -> Promise.wrap(
                                    ProductDao.listPropertyValuesById(product.getId())).map(propVals ->
                                    new ProductDto(product, catAndPropVals._1, propVals))).collect(Collectors.toList())));

                    final Promise<Long> countPromise = Promise.wrap(ProductDao.countByCategoryIdAndPropertyValues(
                            catAndPropVals._1.getId(), propertyValueIds));

                    return productsDtoPromise.zip(countPromise).map(res->new ListResponseWithCountDto(res._1,res._2));
                }).map(res -> ok(Json.toJson(res)));

        return promiseResult.recover(error -> ok(Json.toJson(new ErrorResponseDto(error.getMessage()))));
    }

    public static Promise<Result> countPropertiesByCategoryAndFilter(String categoryName, List<String> propertyValues) {

        final Promise<Category> categoryPromise = Promise.wrap(CategoryDao.getByName(categoryName));

        final Promise<List<PropertyValue>> propertyValuesPromise = Promise.sequence(propertyValues.stream()
                .map(PropertyValueDao::getByName).map(Promise::wrap).collect(Collectors.toList()));

        final Promise<Result> promiseResult = categoryPromise.zip(propertyValuesPromise)
                .flatMap(catAndPropVals -> {
                    final Map<Long, List<PropertyValue>> propertyValuesMap = groupPropertyValues(catAndPropVals._2);

                    final Map<Long, List<Long>> propertyValueIds = propertyValuesMap.keySet().stream().collect(
                            Collectors.toMap(key -> key, key -> propertyValuesMap.get(key).stream()
                                    .map(PropertyValue::getId).collect(Collectors.toList())));

                    final Promise<List<PropertyDto>> countPromise = Promise.wrap(ProductDao
                            .countPropertyValuesByCategoryIdAndFilter(catAndPropVals._1.getId(), null, propertyValueIds));

                    final Promise<List<List<PropertyDto>>> additionalCountsPromise = Promise.sequence(
                            propertyValueIds.keySet().stream().map(propertyId -> Promise.wrap(ProductDao
                                    .countPropertyValuesByCategoryIdAndFilter(catAndPropVals._1.getId(), propertyId, propertyValueIds)))
                                    .collect(Collectors.toList()));

                    final Promise<List<Property>> propertiesPromise = Promise.sequence(propertyValueIds.keySet().stream()
                            .map(propertyId -> Promise.wrap(PropertyDao.get(propertyId))).collect(Collectors.toList()));

                    return countPromise.zip(additionalCountsPromise).zip(propertiesPromise)
                            .map(res -> createCountPropertiesResponse(res._1._1, res._1._2, res._2, propertyValuesMap));
                }).map(res -> ok(Json.toJson(res)));

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

    private static Map<Long, List<PropertyValue>> groupPropertyValues(List<PropertyValue> propertyValues) {

        final Map<Long, List<PropertyValue>> resultMap = new HashMap<>();

        propertyValues.forEach(propertyValue -> {
            final List<PropertyValue> resultItem;
            if (resultMap.containsKey(propertyValue.getPropertyId()))
                resultItem = resultMap.get(propertyValue.getPropertyId());
            else {
                resultItem = new ArrayList<>();
                resultMap.put(propertyValue.getPropertyId(), resultItem);
            }
            resultItem.add(propertyValue);
        });

        return resultMap;
    }

    private static CountPropertiesResponse createCountPropertiesResponse(
            List<PropertyDto> properrtiesCount, List<List<PropertyDto>> additionalProperrtiesCount, List<Property> properties,
            Map<Long, List<PropertyValue>> propertyValuesMap) {

        final Set<CountPropertiesResponseItem> propertiesSet = new TreeSet<>((o1, o2) -> o1.getDisplayName().compareTo(o2.getDisplayName()));
        properrtiesCount.forEach(itm -> propertiesSet.add(new CountPropertiesResponseItem(itm, false)));
        additionalProperrtiesCount.forEach(list -> list.forEach(itm -> propertiesSet.add(new CountPropertiesResponseItem(itm, true))));

        final Set<PropertyDto> selectedPropertiesSet = new TreeSet<>((o1, o2) -> o1.getDisplayName().compareTo(o2.getDisplayName()));
        properties.forEach(itm -> {
            final PropertyDto propertyDto = new PropertyDto(itm.getName(), itm.getDisplayName());
            propertyValuesMap.get(itm.getId()).forEach(propertyValue -> propertyDto.getPropertyValues()
                    .add(new PropertyValueDto(propertyValue.getName(), propertyValue.getDisplayName())));
            selectedPropertiesSet.add(propertyDto);
        });

        return new CountPropertiesResponse(new ArrayList<>(propertiesSet), new ArrayList<>(selectedPropertiesSet));
    }

    public static class CountPropertiesResponse extends ListResponseDto<CountPropertiesResponseItem> {

        public final List<PropertyDto> selectedProperties;

        public CountPropertiesResponse(List<CountPropertiesResponseItem> data, List<PropertyDto> selectedProperties) {
            super(data);
            this.selectedProperties = selectedProperties;
        }
    }

    public static class CountPropertiesResponseItem extends PropertyDto {

        public final Boolean isAdditional;

        public CountPropertiesResponseItem(PropertyDto propertyDto, Boolean isAdditional) {
            super(propertyDto.getName(), propertyDto.getDisplayName());
            getPropertyValues().addAll(propertyDto.getPropertyValues());
            this.isAdditional = isAdditional;
        }
    }
}
