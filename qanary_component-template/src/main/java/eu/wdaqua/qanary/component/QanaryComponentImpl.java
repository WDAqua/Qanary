package eu.wdaqua.qanary.component;

import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.message.QanaryMessage;

/**
 * represents a trivial Qanary component, not doing anything (can be removed at
 * the end)
 * 
 * @author AnBo
 *
 */
@Component
public class QanaryComponentImpl extends QanaryComponent {

	/**
	 * default processor of a QanaryMessage
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) {
		return myQanaryMessage;
	}

}
