package eu.wdaqua.qanary.component;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

//import eu.wdaqua.qanary.message.QanaryMessage;
import eu.wdaqua.qanary.component.config.QanaryConfiguration;

@Controller
public class QanaryServiceController {
	/*
	 * private static final Logger logger =
	 * LoggerFactory.getLogger(QanaryServiceController.class);
	 * 
	 * QanaryComponent qanaryComponent;
	 * 
	 * @Inject public QanaryServiceController(QanaryComponent qanaryComponent) {
	 * this.qanaryComponent = qanaryComponent; logger.info("qanaryComponent: {}"
	 * , this.qanaryComponent); }
	 */

	/**
	 * provides a description HTML page of the component, replace
	 * description.html to custom page
	 * 
	 * @return
	 */
	@RequestMapping(value = QanaryConfiguration.description, produces = { "text/html" })
	public String description() {
		return "description";
	}

	/**
	 * example: curl -X POST -d
	 * 'message={"http://qanary/#endpoint":"http://x.y"}'
	 * http://localhost:8080/annotatequestion | python -m json.tool
	 * 
	 * @param myQanaryMessage
	 * @return
	 */
	/*
	 * @RequestMapping(value = QanaryConfiguration.annotatequestion, consumes =
	 * "application/json", produces = "application/json", method =
	 * RequestMethod.POST)
	 * 
	 * @ResponseBody public QanaryMessage annotatequestion(@RequestBody
	 * QanaryMessage myQanaryMessage) {
	 * 
	 * this.qanaryComponent.process(myQanaryMessage);
	 * 
	 * return myQanaryMessage; }
	 */

}
