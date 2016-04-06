package eu.wdaqua.qanary.web;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * controller for processing
 * 
 * @author AnBo
 *
 */

@Controller
public class QanaryQuestionAnsweringController {

	/**
	 * start a configured process
	 * 
	 * @return
	 */
	@RequestMapping(value = "/questionanswering", method = RequestMethod.POST)
	@ResponseBody
	public String questionanswering() {

		UUID runID = UUID.randomUUID();

		// TODO: create a new graph with this UUID

		// TODO: load ontology into graph

		// TODO: call all defined components

		return runID.toString();
	}

}
