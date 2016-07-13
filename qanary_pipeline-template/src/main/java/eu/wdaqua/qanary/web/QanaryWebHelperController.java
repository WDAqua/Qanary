package eu.wdaqua.qanary.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * collection of helpers implemented as web UI to provide convenient access to Qanary
 * implementation
 *
 * @author AnBo
 */
@Controller
public class QanaryWebHelperController {

    /**
     * get some information about your Qanary implementation using the fancy HTML template
     * description.html
     */
    @RequestMapping("/description")
    public String description() {
        // TODO: take from config "1st Qanary test service"
        return "description";
    }

    /**
     * web form for input a question as defined in inputtextquestion.html
     */
    @RequestMapping("/inputtextquestion")
    public String inputtextquestion() {
        return "inputtextquestion";
    }

    /**
     * start a question answering process by using this web form as defined in
     * startquestionanswering.html
     */
    @RequestMapping("/startquestionanswering")
    public String startquestionanswering() {
        return "startquestionanswering";
    }

}
