package example1;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActorWithStash;
import akka.dispatch.sysmsg.Terminate;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;
import scala.Option;
import scala.collection.Iterator;


public class MasterTraditionalVersion extends UntypedActorWithStash {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private ActorRef senderAwaitingResult;

    private String allResults = "";

    @Override
    public void preStart() {
        // Here we can also create child actor:
        //      // create the greeter actor
        //      final ActorRef child = getContext().actorOf(Props.create(example1.Child.class), "child");
        //      // tell it to perform the greeting
        //      greeter.tell("msgFromParent", getSelf());
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof JobMessage) {
            JobMessage jobMsg = (JobMessage) msg;

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
            childRef.tell(msg, getSelf());


        } else if (msg instanceof FinishAll) {

            //ActorSelection childrenSelection = context().actorSelection("/user/master/child_*");
            Iterator<ActorRef> iterator = getContext().children().iterator();
            while (iterator.hasNext()) {
                iterator.next().tell(msg, self());
            }

            // State change example:
            getContext().become(awaitingResults);
            this.senderAwaitingResult = sender();


        } else if (msg instanceof Terminate) {
            logger.info("we have to finish...");
        } else {
            unhandled(msg);
        }
    }


    Procedure<Object> awaitingResults = new Procedure<Object>() {
        @Override
        public void apply(Object msg) {
            if (msg instanceof JobDoneMsg) {

                String result = sender().path().name() + ": <" + ((JobDoneMsg) msg).getResult() + ">";
                if (allResults.length() > 0) {
                    allResults = allResults.concat(" " + result);
                } else {
                    allResults = allResults.concat(result);

                }

                // Terminate the sender = child
                // there are also other ways : poisonPill , gracefulStop waiting for termination
                logger.info("RESULT from " + sender().path() + " is " + ((JobDoneMsg) msg).getResult());
                getContext().stop(sender());

            } else if (msg instanceof Terminated) {
                logger.info("Got msg about child termination: " + sender().path());
                if (!getContext().children().nonEmpty()) {
                    // all children terminated -> we can finish completely
                    senderAwaitingResult.tell(allResults, self());
                    unstashAll();
                    getContext().unbecome();

                } else {
                    logger.info("children still exists");
                }


            } else {

                // put aside all other messages
                stash();
            }
        }
    };


    private ActorRef createChild(String childName) {
        return context().actorOf(Props.create(Child.class), childName);
    }

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

    public static class CheckCompletion {
    }



}