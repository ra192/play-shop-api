package actors;

import akka.actor.ActorRef;
import akka.actor.Status;
import akka.actor.UntypedActor;
import db.MyConnectionPool;
import dto.PropertyValueDto;

import java.util.Arrays;

/**
 * Created by yakov_000 on 10.02.2015.
 */
public class GetPropertyValueByNameActor extends UntypedActor {

    @Override
    public void onReceive(Object message) throws Exception {

        if(message instanceof String) {
            String propertyValueName=(String)message;
            final ActorRef self = getSelf();
            final ActorRef sender = getSender();
            MyConnectionPool.db.query("select * from property_value where name=$1", Arrays.asList(propertyValueName),
                    result->{
                        if(result.size()>0) {
                            sender.tell(new PropertyValueDto(result.row(0).getLong("id"),result.row(0).getString("name"),
                                    result.row(0).getString("displayName"),result.row(0).getLong("property_id")),self);
                        } else {
                            sender.tell(new Status.Failure(new Exception("Property value with specified name doesn't exist")),self);
                        }
                    },
                    error->sender.tell(new Status.Failure(error),self));
        } else {
            unhandled(message);
        }
    }
}
