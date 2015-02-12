package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Status;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import db.MyConnectionPool;
import dto.CategoryDto;

import java.util.Arrays;

/**
 * Created by yakov_000 on 09.02.2015.
 */
public class GetCategoryByNameActor extends AbstractActor {

    final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public GetCategoryByNameActor() {

        receive(ReceiveBuilder.match(String.class, categoryName -> {
            final ActorRef self = self();
            final ActorRef sender = sender();

            MyConnectionPool.db.query("select * from category where name=$1", Arrays.asList(categoryName),
                    result -> {
                        if (result.size() > 0) {
                            final CategoryDto category = new CategoryDto(result.row(0).getLong("id"),
                                    result.row(0).getString("name"), result.row(0).getString("displayName"));

                            sender.tell(category, self);
                        } else {
                            sender.tell(new Status.Failure(new Exception("Category with specified name doesn't exist")), self);
                        }
                    },
                    error -> sender.tell(new Status.Failure(error), self)
            );
        }).build());
    }
}
