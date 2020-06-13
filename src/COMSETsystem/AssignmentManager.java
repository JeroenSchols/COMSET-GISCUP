package COMSETsystem;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

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
        switch (agentAction.type) {
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
            System.out.println("Invalid Assignment");
            return;
        }

        if (!agentEvent.isCruising()) {
            // TODO: Check if we need to do re-assignment
        }

        agentEvent.assignTo(resEvent);
    }

    private void processAbort(AgentAction agentAction, long currentTime) {
        long agentId = agentAction.agentId;
        AgentEvent agentEvent = agents.get(agentId);

        if (agentEvent == null || agentEvent.isCruising()) {
            System.out.println("Invalid Abort");
            return;
        }

        // TODO: Relocate the assigned resource

        agentEvent.assignTo(null);
    }
}
