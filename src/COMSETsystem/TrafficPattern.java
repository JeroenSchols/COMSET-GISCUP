package COMSETsystem;

import COMSETsystem.DistanceLocationOnLink;
import COMSETsystem.Link;
import COMSETsystem.Road;
import COMSETsystem.Vertex;

import java.util.ArrayList;

public class TrafficPattern {
    public long epoch;
    public long step;

    private ArrayList<TrafficPatternItem> trafficPattern;

    public TrafficPattern(long epoch, long step) {
        this.epoch = epoch;
        this.step = step;
        this.trafficPattern = new ArrayList<TrafficPatternItem>();
    }

    public void addTrafficPatternItem(long epochBeginTime, double speedFactor) {
        //TrafficPatternItem trafficPatternItem = new TrafficPatternItem(epochBeginTime, speedFactor);
        TrafficPatternItem trafficPatternItem = new TrafficPatternItem(epochBeginTime, 0.25);
        trafficPattern.add(trafficPatternItem);
    }

    // get the speed factor of a given time
    public double getSpeedFactor(double time) {
        return 1;
//        if (time < trafficPattern.get(0).epochBeginTime) {
//            return trafficPattern.get(0).speed_factor;
//        } else if (time >= trafficPattern.get(trafficPattern.size()-1).epochBeginTime) {
//            return trafficPattern.get(trafficPattern.size()-1).speed_factor;
//        } else {
//            // compute the index of time
//            int patternIndex = (int)((time - trafficPattern.get(0).epochBeginTime) / step);
//            assert(time >= trafficPattern.get(patternIndex).epochBeginTime && time <= trafficPattern.get(patternIndex).epochBeginTime + step);
//            return trafficPattern.get(patternIndex).speed_factor;
//        }
    }

    public void printOut() {
        for (TrafficPatternItem trafficPatternItem : trafficPattern) {
            System.out.println(trafficPatternItem.epochBeginTime + "," + trafficPatternItem.speed_factor);
        }
    }

    class TrafficPatternItem {
        public long epochBeginTime;
        public double speed_factor;
        public TrafficPatternItem(long epochBeginTime, double speed_factor) {
            this.epochBeginTime = epochBeginTime;
            this.speed_factor = speed_factor;
        }
        public String toString() {
            return String.valueOf(this.epochBeginTime)+","+String.valueOf(this.speed_factor);
        }
    }

    // compute the dynamic travel time to travel a certain distance of a link starting at a certain time
    public double dynamicForwardTravelTime(double time, double unadjustedSpeed, double distance) {
        double totalDistance = 0.0;
        double totalTime = 0.0;
        double currentTime = time;

        double speedFactor;
        double adjustedSpeed;

        while (true) {
            double stepTime = -1.0;
            // travel for one step
            if (currentTime >= this.trafficPattern.get(this.trafficPattern.size()-1).epochBeginTime) {
                speedFactor = this.trafficPattern.get(this.trafficPattern.size()-1).speed_factor;
                adjustedSpeed = unadjustedSpeed * speedFactor;
                stepTime = (distance - totalDistance) / adjustedSpeed;
                totalTime += stepTime;
                break;
            } else {
                if (currentTime < this.trafficPattern.get(0).epochBeginTime) {
                    stepTime = this.trafficPattern.get(0).epochBeginTime - currentTime;
                    speedFactor = this.trafficPattern.get(0).speed_factor;
                } else {
                    int patternIndex = (int) ((currentTime - trafficPattern.get(0).epochBeginTime) / step);
                    stepTime = trafficPattern.get(patternIndex).epochBeginTime + step - currentTime;
                    speedFactor = this.trafficPattern.get(patternIndex).speed_factor;
                }
                adjustedSpeed = unadjustedSpeed * speedFactor;
                double stepDistance = adjustedSpeed * stepTime;
                if (totalDistance + stepDistance < distance) {
                    // finish a full step
                    totalDistance += stepDistance;
                    totalTime += stepTime;
                    currentTime += stepTime;
                } else {
                    // finish a partial step
                    double remainingDistance = distance - totalDistance;
                    double remainingTime = remainingDistance / adjustedSpeed;
                    totalTime += remainingTime;
                    break;
                }
            }
        }
        return totalTime;
    }

    // how long
    public double linkForwardTravelTime(double time, Link link, double distance) {
        double travelTime = dynamicForwardTravelTime(time, link.speed, distance);
        return travelTime;
    }

    // dynamic travel time from a location on a road to the end of the road
    public long roadTravelTimeToEndIntersection(long time, DistanceLocationOnLink loc) {
        return (long)roadTravelTimeToEndIntersection((double)time, loc);
    }

    // dynamic travel time from a location on a road to the end of the road
    public double roadTravelTimeToEndIntersection(double time, DistanceLocationOnLink loc) {
        Road road = loc.link.road;
        DistanceLocationOnLink endIntersection = DistanceLocationOnLink.createFromRoadEnd(road);
        return roadForwardTravelTime(time, loc, endIntersection);
    }

    public long roadTravelTimeFromStartIntersection(long time, DistanceLocationOnLink loc) {
        return (long)roadTravelTimeFromStartIntersection((double)time, loc);
    }

    public double roadTravelTimeFromStartIntersection(double time, DistanceLocationOnLink loc) {
        Road road = loc.link.road;
        DistanceLocationOnLink startIntersection = DistanceLocationOnLink.createFromRoadStart(road);
        return roadForwardTravelTime(time, startIntersection, loc);
    }

    public long roadForwardTravelTime(long time, DistanceLocationOnLink loc1, DistanceLocationOnLink loc2) {
        return (long)roadForwardTravelTime((double)time, loc1, loc2);
    }

    // TODO: to be tested
    public double roadForwardTravelTime(double time, DistanceLocationOnLink loc1, DistanceLocationOnLink loc2) {
        assert loc1.upstreamTo(loc2) : "loc1 must be upstream to loc2";

        Road road = loc1.link.road;
        Link link1 = loc1.link;
        Link link2 = loc2.link;
        double distanceFromStartVertexOfLink1 = loc1.distanceFromStartVertex;
        double distanceFromStartVertexOfLink2 = loc2.distanceFromStartVertex;

        // finish link1
        double currentTime = time;
        double travelTime = linkForwardTravelTime(time, link1, link1.length - distanceFromStartVertexOfLink1);
        currentTime += travelTime;

        // finish the links between link1 and link2
        int linkOrderOfLink1 = road.links.indexOf(link1);
        int linkOrderOfLink2 = road.links.indexOf(link2);
        for (int linkOrder = linkOrderOfLink1 + 1; linkOrder < linkOrderOfLink2; linkOrder++) {
            Link link = road.links.get(linkOrder);
            travelTime = linkForwardTravelTime(currentTime, link, link.length);
            currentTime += travelTime;
        }

        // finish link2
        travelTime = linkForwardTravelTime(currentTime, link2, distanceFromStartVertexOfLink2);
        currentTime += travelTime;

        return currentTime - time;
    }

    // compute the travel distance along a link for a certain time starting at a certain time
    public double[] dynamicTravelDistance(double time, double unadjustedSpeed, double travelTime, double maxDistance) {
        double totalDistance = 0.0;
        double totalTime = 0.0;
        double currentTime = time;

        double speedFactor;
        double adjustedSpeed;
        double distance;

        while (true) {
            double stepTime = -1.0;
            // travel for one step
            if (currentTime >= this.trafficPattern.get(this.trafficPattern.size()-1).epochBeginTime) {
                speedFactor = this.trafficPattern.get(this.trafficPattern.size()-1).speed_factor;
                adjustedSpeed = unadjustedSpeed * speedFactor;
                distance = (travelTime - totalTime) * adjustedSpeed;
                if (totalDistance + distance > maxDistance) {
                    double remainingDistance = maxDistance - totalDistance;
                    totalTime += remainingDistance / adjustedSpeed;
                    totalDistance = maxDistance;
                } else {
                    totalDistance += distance;
                    totalTime = travelTime;
                }
                break;
            } else {
                if (currentTime < this.trafficPattern.get(0).epochBeginTime) {
                    stepTime = this.trafficPattern.get(0).epochBeginTime - currentTime;
                    speedFactor = this.trafficPattern.get(0).speed_factor;
                } else {
                    int patternIndex = (int) ((currentTime - trafficPattern.get(0).epochBeginTime) / step);
                    stepTime = trafficPattern.get(patternIndex).epochBeginTime + step - currentTime;
                    speedFactor = this.trafficPattern.get(patternIndex).speed_factor;
                }
                adjustedSpeed = unadjustedSpeed * speedFactor;
                if (totalTime + stepTime > travelTime) {
                    double remainingTime = travelTime - totalTime;
                    distance = adjustedSpeed * remainingTime;
                    totalDistance += distance;
                    totalTime = travelTime;
                    break;
                } else {
                    distance = adjustedSpeed * stepTime;
                    if (totalDistance + distance > maxDistance) {
                        double remainingDistance = maxDistance - totalDistance;
                        totalTime += remainingDistance / adjustedSpeed;
                        totalDistance = maxDistance;
                        break;
                    } else {
                        totalDistance += distance;
                        totalTime += stepTime;
                        currentTime += stepTime;
                    }
                }
            }
        }
        double[] returnValue = {totalDistance, totalTime};
        return returnValue;
    }

    public DistanceLocationOnLink travelRoadForTime(long time, DistanceLocationOnLink distanceLocationOnLink, long travelTime) {
        return travelRoadForTime((double)time, distanceLocationOnLink, (double)travelTime);

    }
    public DistanceLocationOnLink travelRoadForTime(double time, DistanceLocationOnLink distanceLocationOnLink, double travelTime) {
        double currentTime = time;

        double remainingTime = travelTime;
        double distanceFromStartVertex = distanceLocationOnLink.distanceFromStartVertex;
        int linkOrder = distanceLocationOnLink.link.road.links.indexOf(distanceLocationOnLink.link);
        for (int currentLinkOrder = linkOrder; currentLinkOrder < distanceLocationOnLink.link.road.links.size(); currentLinkOrder++) {
            Link currentLink = distanceLocationOnLink.link.road.links.get(currentLinkOrder);
            double maxDistance = currentLink.length - distanceFromStartVertex;
            double[] distanceTimePair = dynamicTravelDistance(currentTime, currentLink.speed, remainingTime, maxDistance);
            double traveledDistance = distanceTimePair[0];
            double traveledTime = distanceTimePair[1];
            if (traveledTime < remainingTime) {
                // reached the end of link before travel time is used out
                // start the next link
                remainingTime -= traveledTime;
                distanceFromStartVertex = 0;
                currentTime += traveledTime;
            } else {
                // finished, return location
                return new DistanceLocationOnLink(currentLink, distanceFromStartVertex + traveledDistance);
            }
        }
        return DistanceLocationOnLink.createFromRoadEnd(distanceLocationOnLink.link.road);
    }

    public static void roadTravelTimeTest_case_1() {
        // configure a traffic pattern
        TrafficPattern trafficPattern = new TrafficPattern(6L, 6L);
        trafficPattern.addTrafficPatternItem(0L, 0.5);
        trafficPattern.addTrafficPatternItem(6L, 1);
        trafficPattern.addTrafficPatternItem(12L, 2);

        // configure links
        Vertex vertex1 = new Vertex(0, 0, 0, 0, 0);
        Vertex vertex2 = new Vertex(0, 0, 10, 0, 1);
        Vertex vertex3 = new Vertex(0, 0, 22, 0, 2);
        Vertex vertex4 = new Vertex(0, 0, 28, 0, 3);
        Link link1 = new Link(vertex1, vertex2, 10, 4);
        Link link2 = new Link(vertex2, vertex3, 12, 2);
        Link link3 = new Link(vertex3, vertex4, 6, 1);

        ArrayList<Link> links = new ArrayList<Link>();
        links.add(link1);
        links.add(link2);
        links.add(link3);
        // configure a road
        Road road = new Road();
        road.links = links;
        link1.road = road;
        link2.road = road;
        link3.road = road;

        //double travelTime = trafficPattern.roadTravelTimeToEndIntersection(3, link1, 2);
        DistanceLocationOnLink loc1 = new DistanceLocationOnLink(link1, 2);
        double travelTime1 = trafficPattern.roadTravelTimeToEndIntersection(3.0, loc1);
        System.out.println("calculated travel time = " + travelTime1 + ", correct travel time = " + 12.25);
        DistanceLocationOnLink anEnd = DistanceLocationOnLink.createFromRoadEnd(road);
        DistanceLocationOnLink anEnd_ = trafficPattern.travelRoadForTime(3.0, loc1, 12.25);
        System.out.println("calculated location = " + anEnd_ + ", correct location = " + anEnd);

        DistanceLocationOnLink loc2 = new DistanceLocationOnLink(link3, link3.length);
        double travelTime2 = trafficPattern.roadTravelTimeFromStartIntersection(3.0, loc2);
        System.out.println("calculated travel Time = " + travelTime2 + ", correct travel time = " + 12.5);
        DistanceLocationOnLink aStart = DistanceLocationOnLink.createFromRoadStart(road);
        DistanceLocationOnLink loc2_ = trafficPattern.travelRoadForTime(3.0, aStart, 12.5);
        System.out.println("calculated location = " + loc2_ + ", correct location = " + loc2);

        DistanceLocationOnLink loc3 = new DistanceLocationOnLink(link2, 2);
        DistanceLocationOnLink loc4 = new DistanceLocationOnLink(link3, 5);
        double travelTime3 = trafficPattern.roadForwardTravelTime(2.0, loc3, loc4);
        System.out.println("calculated travel time = " + travelTime3 + ", correct travel time = " + 11.0);
        DistanceLocationOnLink loc4_ = trafficPattern.travelRoadForTime(2, loc3, 11);
        System.out.println("calculated location = " + loc4_ + ", correct location = " + loc4);
    }

    public static void roadTravelTimeTest_case_2() {
        // configure a traffic pattern
        TrafficPattern trafficPattern = new TrafficPattern(4L, 4L);
        trafficPattern.addTrafficPatternItem(0L, 2);
        trafficPattern.addTrafficPatternItem(4L, 1);
        trafficPattern.addTrafficPatternItem(8L, 3);
        trafficPattern.addTrafficPatternItem(12L, 4);

        // configure links
        Vertex vertex1 = new Vertex(0, 0, 0, 0, 0);
        Vertex vertex2 = new Vertex(0, 0, 10, 0, 1);
        Vertex vertex3 = new Vertex(0, 0, 22, 0, 2);
        Vertex vertex4 = new Vertex(0, 0, 28, 0, 3);
        Link link1 = new Link(vertex1, vertex2, 10, 4);
        Link link2 = new Link(vertex2, vertex3, 12, 2);
        Link link3 = new Link(vertex3, vertex4, 6, 1);

        ArrayList<Link> links = new ArrayList<Link>();
        links.add(link1);
        links.add(link2);
        links.add(link3);
        // configure a road
        Road road = new Road();
        road.links = links;
        link1.road = road;
        link2.road = road;
        link3.road = road;

        //double travelTime = trafficPattern.roadForwardTravelTime(3, road, link1, 2);
        //System.out.println("calculated travel time = " + travelTime + ", correct travel time = " + 7.67);
        //double travelTime = trafficPattern.roadTravelTimeToEndIntersection(5, link2, 3);
        DistanceLocationOnLink loc = new DistanceLocationOnLink(link2, 3);
        double travelTime = trafficPattern.roadTravelTimeToEndIntersection(5.0, loc);
        System.out.println("calculated travel time = " + travelTime + ", correct travel time = " + 5.5);
    }

    // test code
    public static void main(String[] args) {
        roadTravelTimeTest_case_1();
        //roadTravelTimeTest_case_2();
    }

    public static void travelTimeTest() {
        TrafficPattern trafficPattern = new TrafficPattern(10L, 3L);
        trafficPattern.addTrafficPatternItem(100L, 0.5);
        trafficPattern.addTrafficPatternItem(103L, 1);
        trafficPattern.addTrafficPatternItem(106L, 2);
        double travelTime1 = trafficPattern.dynamicForwardTravelTime(102L, 2, 13);
        // true travel time = 5.5 because 1*0.5*2 + 3*1*2 + 1.5*2*2 = 13
        System.out.println("calculated travel time = " + travelTime1 + ", " + "correct travel time = " + 5.5);
        double travelTime2 = trafficPattern.dynamicForwardTravelTime(99L, 2, 28);
        // true travel time = 11.5 because 1*0.5*2 + 3*0.5*2 + 3*1*2 + 3*2*2 + 1.5*2*2 = 28
        System.out.println("calculated travel time = " + travelTime2 + ", " + "correct travel time = " + 11.5);
        double travelTime3 = trafficPattern.dynamicForwardTravelTime(120, 2, 38);
        // travel travel time = 9.5 because 9.5*2*2 = 38
        System.out.println("calculated travel time = " + travelTime3 + ", " + "correct travel time = " + 9.5);
    }


}

