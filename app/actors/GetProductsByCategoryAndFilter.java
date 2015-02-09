package actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Status;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;
import db.MyConnectionPool;
import dto.CategoryDto;
import dto.ProductDto;
import scala.concurrent.Future;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by yakov_000 on 09.02.2015.
 */
public class GetProductsByCategoryAndFilter extends UntypedActor {

    private final ActorRef categoryByNameActorRef;

    public GetProductsByCategoryAndFilter() {

        categoryByNameActorRef = getContext().actorOf(Props.create(GetCategoryByNameActor.class));
    }

    @Override
    public void onReceive(Object message) throws Exception {

        if(message instanceof Message) {
            Message inputParams= (Message) message;

            final ActorRef self = getSelf();
            final ActorRef sender = getSender();

            final Future<Object> categoryByNameFuture = Patterns.ask(categoryByNameActorRef, inputParams.getCategoryName(), 5000);
            categoryByNameFuture.onSuccess(new OnSuccess<Object>(){

                @Override
                public void onSuccess(Object result) throws Throwable {

                    CategoryDto categoryDto= (CategoryDto) result;

                    MyConnectionPool.db.query("select * from product where category_id=$1", Arrays.asList(categoryDto.getId()),
                            queryRes->{
                                List<ProductDto>products=new ArrayList<>();
                                queryRes.forEach(row->products.add(new ProductDto(row.getLong("id"),
                                        row.getString("code"),row.getString("displayName"),
                                        row.getBigDecimal("price").doubleValue(),row.getString("description"),
                                        row.getString("imageUrl"))));
                                sender.tell(products,self);
                            },
                            error->sender.tell(new Status.Failure(error),self));
                }
            },getContext().dispatcher());
        } else {
            unhandled(message);
        }
    }

    public static class Message {
        private String categoryName;
        private String filter;

        public Message(String name, String filter) {
            this.categoryName = name;
            this.filter = filter;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public String getFilter() {
            return filter;
        }
    }
}
