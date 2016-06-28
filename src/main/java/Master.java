import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.sysmsg.Terminate;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jdk.nashorn.internal.scripts.JO;
import scala.Option;
import scala.collection.Iterable;
import scala.collection.Iterator;

import java.util.Collection;


public class Master extends UntypedActor {
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    @Override
    public void preStart() {
        // Here we can also create child actor:
        //      // create the greeter actor
        //      final ActorRef child = getContext().actorOf(Props.create(Child.class), "child");
        //      // tell it to perform the greeting
        //      greeter.tell("msgFromParent", getSelf());
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof JobMessage) {
            JobMessage jobMsg = (JobMessage) msg;

            String childName = "child_" + jobMsg.getId();

            // get existing or create new one:
            ActorRef childRef ;
            Option<ActorRef> childCandidate = context().child(childName);
            if(childCandidate.isEmpty()){
                childRef = createChild(childName);
                logger.info("new child created: " + childRef.path());
            } else{
                childRef = childCandidate.get();
            }

            // propagate msg to child actor
            childRef.tell(msg, getSelf());

        } else if (msg instanceof FinishAll) {
            Iterator<ActorRef> iterator = context().children().iterator();
            while(iterator.hasNext()){
                iterator.next().tell(msg, self());
            }
            // TODO: become awaiting the JobDoneMsg
            // this.senderAwaitingResult = sender();
            // if all children finished - return the response to original sender
        } else if (msg instanceof JobDoneMsg) {
            // brutally stop the sender - child
            // here are also other ways : poisonPill , gracefulStop waiting for termination
            logger.info("RESULT from " + sender().path() +" is " + ((JobDoneMsg) msg).getResult());
            context().stop(sender());

        } else if (msg instanceof Terminate) {
            logger.info("whe have to finish");
        } else if (msg instanceof StopMessage) {
            getContext().stop(getSelf());
        } else {
            unhandled(msg);
        }
    }


    private ActorRef createChild(String childName) {
        return context().actorOf(Props.create(Child.class), childName);
    }


    // ---------------- MESSAGES -------------
    public static class JobDoneMsg {
        private String result;

        public JobDoneMsg(String result) {
            this.result = result;
        }

        public String getResult() {
            return result;
        }
    }

    public static class JobMessage {

        private final long id;
        private String word;

        public JobMessage(long id, String word) {
            this.id = id;
            this.word = word;
        }

        public long getId() {
            return id;
        }

        public String getWord() {
            return word;
        }
    }

    public static class FinishAll {
    }
}