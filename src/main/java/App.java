import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Awaitable;
import scala.concurrent.CanAwait;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import static akka.pattern.Patterns.ask;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class App {
    public static void main(String[] args) throws Exception {

        // starting the AKKA system
        ActorSystem system = ActorSystem.create("some_system");

        // creating 1st actor
        ActorRef ref = system.actorOf(Props.create(Master.class), "master");

        // sending initial msg - using tell to start jobs
        ref.tell(new Master.JobMessage(1, "hi"), null);
        ref.tell(new Master.JobMessage(100, "hello"), null);
        ref.tell(new Master.JobMessage(100, "world"), null);

        // using ASK instead of tell - as a way to get the result out of the actor system
        Timeout timeout = new Timeout(Duration.create(10, TimeUnit.SECONDS));
        Future<Object> future = ask(ref, new Master.FinishAll(), timeout);
        String result = (String) Await.result(future, timeout.duration());

        System.out.println("All msgs received from master: " + result);

        system.terminate();
        // sync way to communicate with actors:
        //        Future future = ask(counter, "get", 5000);
        //
        //        future.onSuccess(new OnSuccess<Integer>() {
        //            public void onSuccess(Integer count) {
        //                System.out.println("Count is " + count);
        //            }
        //        });

    }
}
