package actors;

import akka.actor.*;
import akka.dispatch.Futures;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import com.github.pgasync.ResultSet;
import db.MyConnectionPool;
import dto.CategoryDto;
import scala.concurrent.Future;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by yakov_000 on 29.01.2015.
 */
public class GetCategoriesByParentActor extends AbstractActor {

    final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public GetCategoriesByParentActor() {

        receive(ReceiveBuilder.match(Long.class,parentId->{
            log.info("Recieved: ".concat(parentId.toString()));

            final ActorRef sender =sender();
            final ActorRef self = self();
            final ActorSystem system = getContext().system();

            final Consumer<ResultSet> resultConsumer = result -> {
                final List<CategoryDto> categories = new ArrayList<>();
                final List<Future<Object>>futures=new ArrayList<>();
                result.forEach(row -> {
                    final CategoryDto category = new CategoryDto(row.getLong("id"), row.getString("name"),
                            row.getString("displayname"));
                    final Future<Object> future = Patterns.ask(self, category.getId(), 5000);
                    future.onSuccess(new OnSuccess<Object>(){

                        @Override
                        public void onSuccess(Object result) throws Throwable {
                            List<CategoryDto>childCategories= (List<CategoryDto>) result;
                            category.getChildren().addAll(childCategories);
                        }
                    },system.dispatcher());
                    futures.add(future);

                    categories.add(category);
                });

                final Future<Iterable<Object>> aggregate = Futures.sequence(futures, system.dispatcher());

                aggregate.onSuccess(new OnSuccess<Iterable<Object>>() {

                    @Override
                    public void onSuccess(Iterable<Object> result) throws Throwable {
                        sender.tell(categories, self);
                    }
                }, system.dispatcher());
            };
            final Consumer<Throwable> errorConsumer = error -> {
                sender.tell(new Status.Failure(error), self);

            };

            if (parentId == 0)
                MyConnectionPool.db.query("select * from category where parent_id is null",
                        resultConsumer,
                        errorConsumer);
            else
                MyConnectionPool.db.query("select * from category where parent_id = $1", Arrays.asList(parentId),
                        resultConsumer,
                        errorConsumer);
        }).build());
    }
}
