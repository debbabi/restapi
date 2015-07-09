package org.cristalise.restapi;
import java.util.LinkedHashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

@Path("item")
public class ItemData extends ItemUtils {

	
	public ItemData() {
	}

	@GET
    @Produces(MediaType.APPLICATION_JSON)
	@Path("{uuid}/data")
	public Response getSchemas(@PathParam("uuid") String uuid,
			@Context UriInfo uri) {
		ItemProxy item = ItemSummary.getProxy(uuid);
		return toJSON(enumerate(item, ClusterStorage.VIEWPOINT, "data", uri));
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{uuid}/data/{schema}")
	public Response getViewNames(@PathParam("uuid") String uuid,
			@PathParam("schema") String schema,
			@Context UriInfo uri) {
		ItemProxy item = ItemSummary.getProxy(uuid);
		return toJSON(enumerate(item, ClusterStorage.VIEWPOINT+"/"+schema, "data/"+schema, uri));
	}
	
	@GET
	@Produces(MediaType.TEXT_XML)
	@Path("{uuid}/data/{schema}/{viewName}")
	public Response queryData(@PathParam("uuid") String uuid,
			@PathParam("schema") String schema,
			@PathParam("viewName") String viewName,
			@Context UriInfo uri) {
		ItemProxy item = ItemSummary.getProxy(uuid);
		Viewpoint view;
		try {
			view = item.getViewpoint(schema, viewName);
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw new WebApplicationException("Database error loading view "+viewName+" of schema "+schema);
		}
		Outcome oc;
		try {
			oc = view.getOutcome();
		} catch (ObjectNotFoundException | PersistencyException e) {
			Logger.error(e);
			throw new WebApplicationException("Database error loading outcome for view "+viewName+" of schema "+schema);
		}
		Event ev;
		try {
			ev = view.getEvent();
		} catch (InvalidDataException | PersistencyException | ObjectNotFoundException e) {
			Logger.error(e);
			throw new WebApplicationException("Database error loading event data for view "+viewName+" of schema "+schema);
		}
		
		return getOutcomeResponse(oc, ev);
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{uuid}/data/{schema}/{viewName}/event")
	public Response getViewEvent(@PathParam("uuid") String uuid,
			@PathParam("schema") String schema,
			@PathParam("viewName") String viewName,
			@Context UriInfo uri) {
		ItemProxy item = ItemSummary.getProxy(uuid);
		Viewpoint view;
		try {
			view = item.getViewpoint(schema, viewName);
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw new WebApplicationException("Database error loading view "+viewName+" of schema "+schema);
		}
		Event ev;
		try {
			ev = view.getEvent();
		} catch (InvalidDataException | PersistencyException | ObjectNotFoundException e) {
			Logger.error(e);
			throw new WebApplicationException("Database error loading event data for view "+viewName+" of schema "+schema);
		}
		
		return toJSON(jsonEvent(ev));
	}
	
	protected LinkedHashMap<String, Object> jsonEvent(Event ev) {
		LinkedHashMap<String, Object> eventData = new LinkedHashMap<String, Object>();
		eventData.put("ID", ev.getID());
		eventData.put("Timestamp", ev.getTimeString());
		eventData.put("Agent", ev.getAgentPath().getAgentName());
		eventData.put("Role", ev.getAgentRole());
		
		if (ev.getSchemaName() != null && ev.getSchemaName().length()>0) { // add outcome info
			LinkedHashMap<String, Object> outcomeData = new LinkedHashMap<String, Object>();
			outcomeData.put("Schema", ev.getSchemaName()+" v"+ev.getSchemaVersion());
			outcomeData.put("Name", ev.getViewName());
			eventData.put("Data", outcomeData);
		}
		
		// activity data
		LinkedHashMap<String, Object> activityData = new LinkedHashMap<String, Object>();
		activityData.put("Name", ev.getStepName());
		activityData.put("Path", ev.getStepPath());
		activityData.put("Type", ev.getStepType());
		eventData.put("Activity", activityData);
		
		// state data
		LinkedHashMap<String, Object> stateData = new LinkedHashMap<String, Object>();
		try {
			StateMachine sm = LocalObjectLoader.getStateMachine(ev.getStateMachineName(), ev.getStateMachineVersion());
			stateData.put("Name", sm.getState(ev.getTransition()).getName());
			stateData.put("OriginState", sm.getState(ev.getOriginState()).getName());
			stateData.put("TargetState", sm.getState(ev.getTargetState()).getName());
			stateData.put("StateMachine", ev.getStateMachineName()+" v"+ev.getStateMachineVersion());
			eventData.put("Transition", stateData);
		} catch (ObjectNotFoundException e) {
			eventData.put("Transition", "ERROR: State Machine "+ev.getStateMachineName()+" v"+ev.getStateMachineVersion()+" not found!");
		} catch (InvalidDataException e) {
			eventData.put("Transition", "ERROR: State Machine definition "+ev.getStateMachineName()+" v"+ev.getStateMachineVersion()+" not valid!");
		}
		
		return eventData;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{uuid}/data/{schema}/{viewName}/history")
	public Response getAllEventsForView(@PathParam("uuid") String uuid,
			@PathParam("schema") String schema,
			@PathParam("viewName") String viewName,
			@Context UriInfo uri) {
		ItemProxy item = ItemSummary.getProxy(uuid);
		History history;
		try {
			history = (History)item.getObject(ClusterStorage.HISTORY);
		} catch (ObjectNotFoundException e) {
			throw new WebApplicationException("Could not load History");
		}
		LinkedHashMap<String, Object> eventList = new LinkedHashMap<String, Object>();
		for (int i=0; i<=history.getLastId(); i++) {
			Event ev = history.get(i);
			if (schema.equals(ev.getSchemaName()) && viewName.equals(ev.getViewName())) {
				String evId = String.valueOf(i);
				LinkedHashMap<String, Object> eventDetails = new LinkedHashMap<String, Object>();
				eventDetails.put("Timestamp", ev.getTimeString());
				eventDetails.put("Data", uri.getAbsolutePathBuilder().path(evId).build());
				eventList.put(evId, eventDetails);
			}
		}
		return toJSON(eventList);
	}
	
	@GET
	@Produces(MediaType.TEXT_XML)
	@Path("{uuid}/data/{schema}/{viewName}/history/{event}")
	public Response getOutcomeForEvent(@PathParam("uuid") String uuid,
			@PathParam("schema") String schema,
			@PathParam("viewName") String viewName,
			@PathParam("event") Integer eventId,
			@Context UriInfo uri) {
		ItemProxy item = ItemSummary.getProxy(uuid);
		Event ev;
		try {
			ev = (Event)item.getObject(ClusterStorage.HISTORY+"/"+eventId);
		} catch (ObjectNotFoundException e) {
			throw new WebApplicationException("Event "+eventId+" was not found", 404);
		}
		if (!schema.equals(ev.getSchemaName()) || !viewName.equals(ev.getViewName())) {
			throw new WebApplicationException("Event does not belong to this data");
		}
		Outcome oc;
		try {
			oc = (Outcome)item.getObject(ClusterStorage.OUTCOME+"/"+schema+"/"+ev.getSchemaVersion()+"/"+eventId);
		} catch (ObjectNotFoundException e) {
			throw new WebApplicationException("Outcome "+eventId+" was not found", 404);
		}
		return getOutcomeResponse(oc, ev);
	}
}
