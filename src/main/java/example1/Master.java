package example1;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import scala.Option;
import scala.PartialFunction;
import scala.collection.Iterator;
import scala.runtime.BoxedUnit;


public class Master extends AbstractActorWithStash {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private ActorRef senderAwaitingResult;

    private String allResults = "";

    public Master() {
        // ------------------ INITIAL ACTOR STATE ----------------------
        receive(ReceiveBuilder
                .match(JobMessage.class, msg -> handleJobMsg(msg))
                .match(FinishAll.class, msg -> handleFinishAllMsg(msg))
                .matchAny(msg-> System.out.println("any other msg in master:" + msg))
                .build());
    }

    private void handleFinishAllMsg(FinishAll msg) {
        ActorSelection childrenSelection = context().actorSelection("/user/master/child_*");
        childrenSelection.tell(msg, self());

         // State change example:
        getContext().become(awaitingResultsFunction());
        this.senderAwaitingResult = sender();
    }

    private void handleJobMsg(JobMessage msg) {
        JobMessage jobMsg = msg;
        logger.info(" new job msg: " + jobMsg);

        // one child per id
        String childName = "child_" + jobMsg.getId();

        // get existing or create new one:
        ActorRef childRef;
        Option<ActorRef> childRefOptional = getContext().child(childName);
        if (childRefOptional.isEmpty()) {
            childRef = createChild(childName);
            getContext().watch(childRef);
            logger.info("new child created: " + childRef.path());
        } else {
            childRef = childRefOptional.get();
        }

        // propagate msg to child actor
        childRef.tell(msg, self());
    }

    // ------------------ ANOTHER STATE ----------------------
    final PartialFunction<Object, BoxedUnit> awaitingResultsFunction() {
        return ReceiveBuilder
                .match(JobDoneMsg.class, msg -> handleJobDoneMsg(msg))
                .match(Terminated.class, msg -> handleTerminatedMsg())
                .matchAny(anyMsg -> {
                    logger.info("Putting aside msg:" + anyMsg);
                    stash();
                })
                .build();
    }

    private void handleTerminatedMsg() {
        logger.info("Got msg about child termination: " + sender().path());
        if (!getContext().children().nonEmpty()) {
            // all children terminated -> we can finish completely
            senderAwaitingResult.tell(allResults, self());
            allResults = "";
            unstashAll();
            getContext().unbecome();

        } else {
            logger.info("children still exists");
        }
    }

    private void handleJobDoneMsg(JobDoneMsg msg) {
        String result = sender().path().name() + ": <" + msg.getResult() + ">";
        if (allResults.length() > 0) {
            allResults = allResults.concat(" " + result);
        } else {
            allResults = allResults.concat(result);

        }

        // Terminate the sender = child
        // there are also other ways : poisonPill , gracefulStop waiting for termination
        logger.info("RESULT from " + sender().path() + " is " + msg.getResult());
        getContext().stop(sender());
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

        @Override
        public String toString() {
            return id + " - " + word;
        }
    }

    public static class FinishAll {
    }



}