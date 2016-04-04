package eu.wdaqua.qanary.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class QanaryWebHelperController {

	/**
	 * get some information about your business using the fancy HTML template
	 * description.html
	 * 
	 * @return
	 */
	@RequestMapping("/inputtextquestion")
	public String inputtextquestion() {
		return "inputtextquestion";
	}

}
