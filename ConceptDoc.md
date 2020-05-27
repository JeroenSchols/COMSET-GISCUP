# GISCUP 2020 Concept Design
## Background
The GISCUP 2020 builds upon GISCUP 2019.
Specifically GISCUP 2020 adds to the original, several new twists:

* Management of an entire fleet of taxis, i.e. a user provided **`Fleet Manager`**
* Dynamic Travel Times. The travel time through each segment is modeled based on an internal traffic model
* Failures. Taxis may fail at any time.

For more details, see [GISCUP 2020 Problem Definition](https://docs.google.com/document/d/e/2PACX-1vQ6PL6krQtLjtWs8pI3UKI_NhNuFr_Ecl_Kfk77Yt3ZLzrf2lWt6A1UUCgAbf3JMgnXR9VhfWXJCtab/pub)

This document outlines GISCUP 2020's conceptual differences from GISCUP 2019.
We do not intend to cover all of 2019's functionality.
We supply only enough details to highlight the key differences.

## Notes
Here we list some of the things we would like to do the 
GISCUP 2019 code in order to implement
the new features proposed in GISCUP 2020.
* Fleet Manager
* Dynamic Travel Time
* Failures
## Changes for Classes `Agent` and `AgentEvent`
There are only two types of agent events, handled in the
class `AgentEvent`.

1. **`DROPPING_OFF`** This is the initial state of an Agent and
the state when the agent drops off a resource (customer).
The handler in `AgentEvent` tries to search for a new resource
and
assign to the agent.
If the search fails, the agent then goes into cruising mode.
2. **`INTERSECTION_REACHED`** The handler for this event asks
the
agent for the next intersection and travels there.

When a resource becomes available, it searches for the best agent/agentEvent.
Removes it from the event queue and then set it to DROP_OFF state.
Then the DROP_OFF handler will match it up with the resource.

### Problems
There is no explicit state for *cruising mode*.
Instead it is represented by the agent have a route.
It seems the route is abandoned once if a resource is assigned
to an agent.

### Proosal
**A clearer separation of concerns is needed for `AgentEvent`
and `Agent`.**
This is so that we can have the Fleet Manager do more of the
work.
First, the fleet manager performs resource assignment, and
route planning.
So what are the hooks for fleet manager.
It should be called to assign a resource.
It should be called to find a route.
The `AgentEvent` should just execute the moves.
#### Questions
##### What should the states be?
* Agent has no customer, and is cruising
* Agent has no customer, and on its way to pick up customer
* Agent has a customer, and is on the way to drop off customer

##### Where should the state resides?
* In the `AgentEvent` class? with calls into Agent?
* Agent should then call the Fleet Manager?  But why?
Maybe we should just have AgentEvent and no Agent Class.

##### What should the AgentEvent class do?
* It should just do the movement?
* Maybe it should just be the same as the agent

## Change Plan
Note each step has to be protected with tests.
* Introduce a Fleet Manager that will take care of all\
the existing state changes. Dpn't really need agent class anymore
* Add new states
* That's it.
