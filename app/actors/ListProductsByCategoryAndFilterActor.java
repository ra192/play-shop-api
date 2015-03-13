package actors;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Status;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import dao.ProductDao;
import dto.ListResponseWithCountDto;
import dto.ProductDto;
import model.Category;
import model.Product;
import scala.Tuple2;
import scala.concurrent.Future;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        final Future<ListResponseWithCountDto> resultFuture =
                ProductDao.listByCategoryIdAndPropertyValues(category.getId(), propertyValueIds, inputParams.getFirst(),
                        inputParams.getMax(), inputParams.getOrderProperty(), inputParams.getIsAsc())
                        .zip(ProductDao.countByCategoryIdAndPropertyValues(category.getId(), propertyValueIds))
                        .map(new Mapper<Tuple2<List<Product>, Long>, ListResponseWithCountDto>() {
                            @Override
                            public ListResponseWithCountDto apply(Tuple2<List<Product>, Long> parameter) {
                                return new ListResponseWithCountDto(parameter._1().stream().map(product ->
                                        new ProductDto(product,category))
                                        .collect(Collectors.toList()), parameter._2());
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
