package actors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Status;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import dao.ProductDao;
import dto.ListResponseWithCountDto;
import dto.ProductDto;
import model.Category;
import model.Product;
import model.PropertyValue;
import scala.Tuple2;
import scala.concurrent.Future;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by yakov_000 on 09.02.2015.
 */
public class ListProductsByCategoryAndFilterActor extends CategoryAndPropertyValuesBaseActor {

    public ListProductsByCategoryAndFilterActor() {

        super(Message.class);
    }

    @Override
    protected void onReceive(Object message, Category category, Map<Long, List<Long>> propertyValueIds,
                             ActorRef self, ActorRef sender, ActorContext context) {

        Message inputParams = (Message) message;

        final Future<List<Product>> productsFuture = ProductDao.listByCategoryIdAndPropertyValues(category.getId(), propertyValueIds, inputParams.getFirst(),
                inputParams.getMax(), inputParams.getOrderProperty(), inputParams.getIsAsc());

        final Future<Iterable<ProductDto>> productsDtoFuture = productsFuture.flatMap(new Mapper<List<Product>, Future<Iterable<ProductDto>>>() {
            @Override
            public Future<Iterable<ProductDto>> apply(List<Product> products) {
                return Futures.sequence(products.stream().map(product -> ProductDao.listPropertyValuesById(product.getId())
                        .map(new Mapper<List<PropertyValue>, ProductDto>() {
                            @Override
                            public ProductDto apply(List<PropertyValue> propertyValues) {
                                return new ProductDto(product, category, propertyValues);
                            }
                        }, context.dispatcher())).collect(Collectors.toList()), context.dispatcher());
            }
        }, context.dispatcher());

        final Future<ListResponseWithCountDto> resultFuture = productsDtoFuture
                .zip(ProductDao.countByCategoryIdAndPropertyValues(category.getId(), propertyValueIds))
                .map(new Mapper<Tuple2<Iterable<ProductDto>, Long>, ListResponseWithCountDto>() {
                    @Override
                    public ListResponseWithCountDto apply(Tuple2<Iterable<ProductDto>, Long> res) {
                        return new ListResponseWithCountDto(StreamSupport.stream(res._1().spliterator(), false).collect(Collectors.toList()), res._2());
                    }
                }, context.dispatcher());

        resultFuture.onComplete(new OnComplete<ListResponseWithCountDto>() {

            @Override
            public void onComplete(Throwable failure, ListResponseWithCountDto success) throws Throwable {
                if (failure != null)
                    sender.tell(new Status.Failure(failure), self);
                else
                    sender.tell(success, self);
            }
        }, context.dispatcher());
    }

    public static class Message extends BaseMessage {
        private Integer first;
        private Integer max;
        private String orderProperty;
        private Boolean isAsc;

        public Message(String categoryName, List<String> propertyValueNames, Integer first, Integer max, String orderProperty, Boolean isAsc) {
            super(categoryName, propertyValueNames);
            this.first = first;
            this.max = max;
            this.orderProperty = orderProperty;
            this.isAsc = isAsc;
        }

        public Integer getFirst() {
            return first;
        }

        public Integer getMax() {
            return max;
        }

        public String getOrderProperty() {
            return orderProperty;
        }

        public Boolean getIsAsc() {
            return isAsc;
        }
    }
}
