package eu.knowledge.engine.admin.api.impl;

import eu.knowledge.engine.admin.AdminUI;
import eu.knowledge.engine.admin.Util;
import io.swagger.annotations.ApiParam;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Set;
import eu.knowledge.engine.rest.model.SmartConnector;

@Path("/admin")
public class AdminApiServiceImpl {

	private static final Logger LOG = LoggerFactory.getLogger(AdminApiServiceImpl.class);

	private AdminUI admin;

	private Model model;

	//todo: Add TKE runtimes + Smart connectors per runtime in JSON response
	//todo: add active=true|false (show historical SCs, even after lease is expired. Missing or lost SCs can also be valuable information.)
	//todo: add registered knowledge interactions
	//todo: add log with timestamps of all instances of knowledge interaction (in client/GUI we can show time since last interaction)
	//todo: Select smart connector or knowledge interactions based on Knowledge-Base-Id (re-use get route in SmartConnectorLifeCycleApiServiceImpl.java)
	//todo: make route which only gets updated info since <timestamp> (for longpolling)
	//todo: test if suitable for long polling
	//@io.swagger.annotations.ApiOperation(value = "Get all available Knowledge Engine Runtimes and Smart Connectors in the network.", notes = "", response = KnowledgeEngineRuntimeConnectionDetails.class, responseContainer = "List", tags = {"smart connector life cycle",})
	//@Path("{TKE_ID}/kbs/overview")
	//TODO: create response = model.class return type of routes

	@GET
	@Path("/sc/overview")
	@Produces({"application/json; charset=UTF-8"})
	@io.swagger.annotations.ApiOperation(value = "Get all smart connectors in the network.", notes = "", response = SmartConnector.class, responseContainer = "List", tags = {"admin UI API",})
	@io.swagger.annotations.ApiResponses(value = {
			@io.swagger.annotations.ApiResponse(code = 200, message = "A list of smart connectors.", response = SmartConnector.class, responseContainer = "List"),
			@io.swagger.annotations.ApiResponse(code = 404, message = "If there are no smart connectors (at all, or given the knowledgeBaseId) .", response = String.class),
			@io.swagger.annotations.ApiResponse(code = 500, message = "If a problem occurred.", response = String.class)
	})
	public void getSCOverview(
			@ApiParam(value = "The knowledgebase for which you want to retrieve the smart connector information.") @HeaderParam("Knowledge-base-Id") String knowledgeBaseId,
			@Suspended final AsyncResponse asyncResponse,
			@Context SecurityContext securityContext
	) throws NotFoundException {
		admin = AdminUI.newInstance(); //or start when init/start API route is called?
		if (knowledgeBaseId == null) {
			model = this.admin.getModel();
			if (model != null && !model.isEmpty()) {
				Set<Resource> kbs = Util.getKnowledgeBaseURIs(model);

				asyncResponse.resume(Response.ok().entity(convertToModel(kbs, model)).build());

				int i = 0;
				for (Resource kbRes : kbs) {
					i++;

					if (i > 1) {
						LOG.info("");
					}
					LOG.info("Knowledge Base <{}>", kbRes);

					LOG.info("\t* Name: {}", Util.getName(model, kbRes));
					LOG.info("\t* Description: {}", Util.getDescription(model, kbRes));
				}
				return;
			} else {
				throw new NotFoundException();
			}
		}
		//get smart connector belonging to KB with id knowledgeBaseId
	}

	//ADAPT THIS
	private eu.knowledge.engine.rest.model.SmartConnector[] convertToModel(
			Set<Resource> kbs, Model model) {
		return kbs.stream().map((kbRes) -> {
			return new eu.knowledge.engine.rest.model.SmartConnector()
					.knowledgeBaseId(kbRes.toString())
					.knowledgeBaseName(Util.getName(model, kbRes))
					.knowledgeBaseDescription(Util.getDescription(model, kbRes));
		}).toArray(eu.knowledge.engine.rest.model.SmartConnector[]::new);
	}
	//.leaseRenewalTime(1337)
}