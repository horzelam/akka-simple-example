package example1;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.japi.JavaPartialFunction;
import akka.util.Timeout;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.util.Try;

import static akka.pattern.Patterns.ask;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class App {

    private static final Timeout timeout = new Timeout(Duration.create(10, TimeUnit.SECONDS));
    private static final FiniteDuration duration = Duration.create(10, TimeUnit.SECONDS);

    public static void main(String[] args) throws Exception {

        // starting the AKKA system
        ActorSystem system = ActorSystem.create("some_system");

        // creating 1st actor
        ActorRef master = system.actorOf(Props.create(Master.class), "master");


        // sending initial msg - using tell to start jobs
        master.tell(new Master.JobMessage(1, "hi"), null);
        master.tell(new Master.JobMessage(1, "there"), null);
        master.tell(new Master.JobMessage(100, "hello"), null);
        master.tell(new Master.JobMessage(100, "world"), null);
        sendLotOfMessages(master);

        // using ASK instead of TELL - as a way to get the result out of the actor system
        // ----- finishing 1st round
        Future<Object> future1 = ask(master, new Master.FinishAll(), new Timeout(duration));
        //future1.onComplete();


        // ----- 2nd round
        master.tell(new Master.JobMessage(4, "hi"), null);
        master.tell(new Master.JobMessage(4, "there"), null);
        Future<Object> future2 = ask(master, new Master.FinishAll(), new Timeout(duration));


        // just define printing the results - if received:
        future1.onSuccess(printResultsFromMaster("future1"), system.dispatcher());
        future2.onSuccess(printResultsFromMaster("future2"), system.dispatcher());

        // how to wait for single result :
        //        String result1 = (String) Await.result(future1, duration);

        // lets wait for both and then SHUTDOWN...
        final List<Future<Object>> futures = Arrays.asList(future1, future2);
        Futures.sequence(futures, system.dispatcher())
                .andThen(handleSysShutdown(system), system.dispatcher());
    }

    private static JavaPartialFunction<Try<Iterable<Object>>, Object> handleSysShutdown(final ActorSystem system) {
        return new JavaPartialFunction<Try<Iterable<Object>>, Object>() {
            @Override
            public Object apply(Try<Iterable<Object>> x, boolean isCheck) throws Exception {
                System.out.println("shutdown");
                system.shutdown();
                return null;
            }
        };
    }

    private static JavaPartialFunction<Object, Object> printResultsFromMaster(String round) {

        return new JavaPartialFunction<Object, Object>() {

            @Override
            public Object apply(Object result, boolean isCheck) throws Exception {
                System.out.println(round + " - all msgs received from master: " + result);
                return null;
            }
        };
    }

    private static void sendLotOfMessages(ActorRef master) {
        //   master.tell(new example1.Master.JobMessage(100, "world"), null);
    }
}
