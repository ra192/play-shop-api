package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Status;
import akka.japi.pf.ReceiveBuilder;
import db.MyConnectionPool;
import dto.PropertyValueDto;

import java.util.Arrays;

/**
 * Created by yakov_000 on 10.02.2015.
 */
public class GetPropertyValueByNameActor extends AbstractActor {

    public GetPropertyValueByNameActor() {

        receive(ReceiveBuilder.match(String.class, propertyValueName -> {
            final ActorRef self = self();
            final ActorRef sender = sender();
            MyConnectionPool.db.query("select * from property_value where name=$1", Arrays.asList(propertyValueName),
                    result -> {
                        if (result.size() > 0) {
                            sender.tell(new PropertyValueDto(result.row(0).getLong("id"), result.row(0).getString("name"),
                                    result.row(0).getString("displayName"), result.row(0).getLong("property_id")), self);
                        } else {
                            sender.tell(new Status.Failure(new Exception("Property value with specified name doesn't exist")), self);
                        }
                    },
                    error -> sender.tell(new Status.Failure(error), self));
        }).build());
    }
}
