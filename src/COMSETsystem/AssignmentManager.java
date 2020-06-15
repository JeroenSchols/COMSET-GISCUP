package COMSETsystem;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AssignmentManager {

    Map<Long, AgentEvent> agents = new HashMap<>();
    Map<Long, ResourceEvent> resources = new HashMap<>();
//    PriorityQueue<Event> events;

    public void addNewEvent(Event event) {
        if (event instanceof AgentEvent) {
            agents.put(event.id, (AgentEvent) event);
        } else {
            resources.put(event.id, (ResourceEvent) event);
        }
    }

    public void processAgentAction(AgentAction agentAction, long currentTime) {
        switch (agentAction.getType()) {
            case ASSIGN:
                processAssignment(agentAction, currentTime);
                break;
            case ABORT:
                processAbort(agentAction, currentTime);
                break;
            case NONE:
            default:
                break;
        }
    }

    private void processAssignment(AgentAction agentAction, long currentTime) {
        long agentId = agentAction.agentId;
        long resId = agentAction.resId;

        AgentEvent agentEvent = agents.get(agentId);
        ResourceEvent resEvent = resources.get(resId);

        if (agentEvent == null || resEvent == null) {
            return;
        }

        if (agentEvent.hasPickupRes()) {
            return;
        }

        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Agent " + agentId + " pick up Res " + resId);
        agentEvent.assignTo(resEvent, currentTime);
    }

    private void processAbort(AgentAction agentAction, long currentTime) {
        // TODO: Check if we allow contestant to this.
        long agentId = agentAction.agentId;
        AgentEvent agentEvent = agents.get(agentId);

        if (agentEvent == null) {
            System.out.println("Invalid Abort");
            return;
        }

        agentEvent.abortResource();
    }
}
