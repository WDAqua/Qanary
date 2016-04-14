package eu.wdaqua.qanary.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * represents a trivial Qanary component, not doing anything (can be removed at
 * the end)
 * 
 * @author AnBo
 *
 */
@Component
public class QanaryComponentImpl extends QanaryComponent {

	private static final Logger logger = LoggerFactory.getLogger(QanaryComponentImpl.class);

	/**
	 * default processor of a QanaryMessage
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) {
		logger.warn("QanaryComponentImpl was just for test and presentation not for actual use in pipeline.");
		return myQanaryMessage;
	}

}
