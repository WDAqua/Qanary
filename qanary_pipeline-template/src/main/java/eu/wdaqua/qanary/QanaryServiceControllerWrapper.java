package eu.wdaqua.qanary;

import com.complexible.common.inject.OverrideModule;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryServiceController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class QanaryServiceControllerWrapper extends QanaryServiceController {


    public QanaryServiceControllerWrapper(QanaryComponent qanaryComponent) {
        super(qanaryComponent);
    }

    @Override
    public String showDescriptionOnGetRequestOnRoot(HttpServletResponse response, Model model, HttpSession session) {
        return null;
    }
}
